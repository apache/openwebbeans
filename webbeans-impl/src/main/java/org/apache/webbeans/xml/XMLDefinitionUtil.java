/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.decorator.Decorator;
import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Specializes;
import javax.enterprise.util.Nonbinding;
import javax.inject.Named;
import javax.inject.Scope;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.SecurityUtil;
import org.w3c.dom.Element;

//X TODO think we can drop that class
@Deprecated
public final class XMLDefinitionUtil
{
    private XMLDefinitionUtil()
    {

    }

    public static void checkTypeMetaDataClasses(List<Class<? extends Annotation>> typeSet, String errorMessage)
    {
        if (typeSet != null && !typeSet.isEmpty())
        {
            Iterator<Class<? extends Annotation>> it = typeSet.iterator();
            while (it.hasNext())
            {
                Class<? extends Annotation> clazz = it.next();
                if (clazz.isAnnotationPresent(NormalScope.class)
                    || clazz.isAnnotationPresent(Scope.class)
                    || AnnotationUtil.isQualifierAnnotation(clazz)
                    || AnnotationUtil.isInterceptorBindingAnnotation(clazz)
                    || AnnotationUtil.isStereoTypeAnnotation(clazz)
                    || clazz.equals(Named.class)
                    || clazz.equals(Specializes.class) || clazz.equals(javax.interceptor.Interceptor.class)
                    || clazz.equals(Decorator.class))
                {
                    continue;
                }
                else
                {
                    throw new WebBeansConfigurationException(
                            errorMessage + " TypeLevelMeta data configuration is failed because of the class : " + clazz.getName() + " is not applicable type");
                }
            }
        }

    }

    /**
     * Gets applicable annotation class for given defineType parameter from the
     * given annotation set.
     *
     * @param component     webbeans component
     * @param annotationSet type-level metadata annotation set
     * @param defineType    annotation type class
     * @param errorMessage  error message for the operation
     * @return applicable annotation class for given defineType parameter from
     *         the given set
     */
    public static <T> Class<? extends Annotation> defineXMLTypeMetaData(AbstractOwbBean<T> component, List<Class<? extends Annotation>> annotationSet,
                                                                        Class<? extends Annotation> defineType, String errorMessage)
    {
        // Found annotation for given defineType parameter
        Class<? extends Annotation> metaType = null;

        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        boolean found = false;
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (temp.isAnnotationPresent(defineType))
            {
                if (found)
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }
                else
                {
                    metaType = temp;
                    found = true;
                }
            }
        }

        return metaType;
    }

    public static <T> boolean defineXMLBindingType(AbstractOwbBean<T> component, List<Class<? extends Annotation>> annotationSet,
                                                   List<Element> annotationElementList, String errorMessage)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        boolean found = false;
        int i = 0;
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (component.getWebBeansContext().getAnnotationManager().isQualifierAnnotation(temp))
            {
                Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(temp);

                for (Method method : methods)
                {
                    Class<?> clazz = method.getReturnType();
                    if (clazz.isArray() || clazz.isAnnotation())
                    {
                        if (!AnnotationUtil.hasAnnotation(method.getAnnotations(), Nonbinding.class))
                        {
                            throw new WebBeansConfigurationException(
                                    errorMessage + "WebBeans definition class : " + component.getReturnType().getName() + " @Qualifier : " + temp.getName() +
                                    " must have @NonBinding valued members for its array-valued and annotation valued members");
                        }
                    }
                }

                if (!found)
                {
                    found = true;
                }

                component.addQualifier(XMLUtil.getXMLDefinedAnnotationMember(annotationElementList.get(i), temp, errorMessage));
            }

            i++;
        }

        return found;
    }

    /**
     * Configures the webbeans component stereotype.
     *
     * @param component     webbeans component
     * @param annotationSet set of type-level metadata annotation set
     */
    public static <T> void defineXMLStereoType(AbstractOwbBean<T> component, List<Class<? extends Annotation>> annotationSet)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (AnnotationUtil.isStereoTypeAnnotation(temp))
            {
                component.addStereoType(JavassistProxyFactory.createNewAnnotationProxy(temp));
            }
        }
    }

    public static <T> boolean defineXMLName(AbstractOwbBean<T> component, List<Class<? extends Annotation>> annotationSet)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (temp.equals(Named.class))
            {
                return true;
            }
        }

        return false;
    }


}