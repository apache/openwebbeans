/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.deployment;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.NormalScope;
import javax.inject.Named;
import javax.inject.Scope;

import org.apache.webbeans.deployment.stereotype.IStereoTypeModel;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.definition.NonexistentTypeException;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.xml.XMLUtil;
import org.dom4j.Element;

@SuppressWarnings("unchecked")
public class XMLStereoTypeModel implements IStereoTypeModel
{
    private String name;

    private Annotation defaultDeploymentType;

    private Annotation defaultScopeType;

    private Set<Class<? extends Annotation>> supportedScopes = new HashSet<Class<? extends Annotation>>();

    private Set<Class<?>> restrictedTypes = new HashSet<Class<?>>();

    private Set<Annotation> interceptorBindingTypes = new HashSet<Annotation>();

    private Annotation defaultName = null;

    private Set<Annotation> inherits = new HashSet<Annotation>();

    public XMLStereoTypeModel(Element stereoTypeDecleration, String name, String errorMessage)
    {
        configure(stereoTypeDecleration, errorMessage);
        setName(name);
    }

    private void configure(Element stereoTypeDecleration, String errorMessage)
    {
        List<Element> childs = stereoTypeDecleration.elements();
        if (childs != null && childs.size() > 0)
        {
            boolean scopeTypeFound = false;

            for (Element child : childs)
            {
                Class<?> clazz = XMLUtil.getElementJavaType(child);
                if (clazz == null)
                {
                    throw new NonexistentTypeException(errorMessage + "Type is not exist with class name : " + XMLUtil.getElementJavaClassName(child) + " in namespace : " + XMLUtil.getElementNameSpace(child) );
                }

                Class<? extends Annotation> annClazz = null;
                if (!clazz.isAnnotation())
                {
                    throw new WebBeansConfigurationException(errorMessage + "Type is not annotation type with class name : " + XMLUtil.getElementJavaClassName(child) +  " in namespace : " + XMLUtil.getElementNameSpace(child));
                }

                annClazz = (Class<? extends Annotation>) clazz;
                Annotation defaultAnn = JavassistProxyFactory.createNewAnnotationProxy(annClazz);

                if (clazz.isAnnotationPresent(NormalScope.class) || clazz.isAnnotationPresent(Scope.class))
                {
                    if (scopeTypeFound)
                    {
                        throw new WebBeansConfigurationException(errorMessage + "@StereoType annotation can not contain more than one @Scope/@NormalScope annotation");
                    }

                    defaultScopeType = defaultAnn;
                    scopeTypeFound = true;
                }                
                else if (AnnotationUtil.isInterceptorBindingAnnotation(annClazz))
                {
                    Target target = clazz.getAnnotation(Target.class);
                    ElementType[] type = target.value();

                    if (type.length != 1 && !type[0].equals(ElementType.TYPE))
                    {
                        throw new WebBeansConfigurationException(errorMessage + "Stereotype with @InterceptorBinding must be defined as @Target{TYPE}");
                    }

                    interceptorBindingTypes.add(XMLUtil.getXMLDefinedAnnotationMember(child, annClazz, errorMessage));
                }
                else if (clazz.equals(Named.class))
                {
                    defaultName = defaultAnn;
                    Named name = (Named) defaultName;
                    if (!name.value().equals(""))
                    {
                        throw new WebBeansConfigurationException(errorMessage + "@StereoType annotation can not define @Named annotation with value");
                    }
                }
                else if (AnnotationUtil.isQualifierAnnotation(annClazz))
                {
                    throw new WebBeansConfigurationException(errorMessage + "@StereoType annotation can not define @Qualifier annotation");
                }

                else if (AnnotationUtil.isStereoTypeAnnotation(annClazz))
                {
                    Target innerStereo = clazz.getAnnotation(Target.class);
                    Class<?> outerStereoClass = XMLUtil.getElementJavaType(stereoTypeDecleration);
                    Target outerStereo = outerStereoClass.getAnnotation(Target.class);

                    ElementType[] innerValues = innerStereo.value();
                    ElementType[] outerValues = outerStereo.value();

                    for (ElementType innerValue : innerValues)
                    {
                        if (innerValue.equals(ElementType.METHOD) || innerValue.equals(ElementType.FIELD))
                        {
                            for (ElementType outerValue : outerValues)
                            {
                                if (outerValue.equals(ElementType.TYPE))
                                {
                                    throw new WebBeansConfigurationException(errorMessage + "Inherited StereoType with class name : " + clazz.getName() + " must have compatible @Target annotation with Stereotype class name : " + outerStereoClass.getName());
                                }
                            }
                        }
                        else if (innerValue.equals(ElementType.TYPE))
                        {
                            for (ElementType outerValue : outerValues)
                            {
                                if (outerValue.equals(ElementType.METHOD) || outerValue.equals(ElementType.FIELD))
                                {
                                    throw new WebBeansConfigurationException(errorMessage + "Inherited StereoType with class name : " + clazz.getName() + " must have compatible @Target annotation with Stereotype class name : " + outerStereoClass.getName());
                                }
                            }
                        }
                    }

                    this.inherits.add(defaultAnn);

                }

                else
                {
                    throw new WebBeansConfigurationException(errorMessage + "Type with class name : " + XMLUtil.getElementJavaClassName(child) + " is not applicable for stereotype");
                }
            }
        }
    }

    public Annotation getDefaultDeploymentType()
    {
        return this.defaultDeploymentType;
    }

    public Annotation getDefaultScopeType()
    {
        return this.defaultScopeType;
    }

    public Set<Annotation> getInterceptorBindingTypes()
    {
        return this.interceptorBindingTypes;
    }

    public Set<Annotation> getInheritedStereoTypes()
    {
        return this.inherits;
    }

    public Annotation getDefaultNamed()
    {
        return this.defaultName;
    }

    public String getName()
    {
        return this.name;
    }

    public Set<Class<?>> getRestrictedTypes()
    {
        return this.restrictedTypes;
    }

    public Set<Class<? extends Annotation>> getSupportedScopes()
    {
        return this.supportedScopes;
    }

    public void setName(String name)
    {
        this.name = name;

    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (!(obj instanceof XMLStereoTypeModel))
            return false;

        XMLStereoTypeModel model = (XMLStereoTypeModel) obj;

        return model.name.equals(this.name);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }

}
