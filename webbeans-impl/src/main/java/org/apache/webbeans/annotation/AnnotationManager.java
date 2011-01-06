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
package org.apache.webbeans.annotation;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ArrayUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.SecurityUtil;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;

import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages annotation usage by classes in this application.
 */
public final class AnnotationManager
{
    public static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private final XMLAnnotationTypeManager manager;

    private final BeanManagerImpl beanManagerImpl;

    // No instantiate

    public AnnotationManager(WebBeansContext context)
    {
        manager = context.getxMLAnnotationTypeManager();
        beanManagerImpl = context.getBeanManagerImpl();
    }


    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link javax.interceptor.InterceptorBinding} false otherwise.
     *
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link javax.interceptor.InterceptorBinding} false otherwise
     */
    public boolean isInterceptorBindingAnnotation(Class<? extends Annotation> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        if (manager.hasInterceptorBindingType(clazz))
        {
            return true;
        }
        else if (clazz.isAnnotationPresent(InterceptorBinding.class))
        {
            return true;
        }

        return false;
    }

    /**
     * If any Annotations in the input is an interceptor binding annotation type then return
     * true, false otherwise.
     *
     * @param anns array of Annotations to check
     * @return true if one or moe of the input annotations are an interceptor binding annotation
     *         type false otherwise
     */
    public boolean hasInterceptorBindingMetaAnnotation(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, "anns parameter can not be null");

        for (Annotation ann : anns)
        {
            if (isInterceptorBindingAnnotation(ann.annotationType()))
            {
                return true;
            }
            else
            {
                continue;
            }
        }

        return false;
    }

    /**
     * Collect the interceptor bindings from an array of annotations, including
     * transitively defined interceptor bindings.
     * @param anns An array of annotations
     * @return an array of interceptor binding annotations, including the input and any transitively declared annotations
     */
    public Annotation[] getInterceptorBindingMetaAnnotations(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, "anns parameter can not be null");
        List<Annotation> interAnns = new ArrayList<Annotation>();

        for (Annotation ann : anns)
        {
            if (isInterceptorBindingAnnotation(ann.annotationType()))
            {
                interAnns.add(ann);

                //check for transitive
                Annotation[] transitives = getTransitiveInterceptorBindings(ann.annotationType().getDeclaredAnnotations());

                for(Annotation transitive : transitives)
                {
                    interAnns.add(transitive);
                }

            }
        }

        Annotation[] ret = new Annotation[interAnns.size()];
        ret = interAnns.toArray(ret);

        return ret;
    }

    private Annotation[] getTransitiveInterceptorBindings(Annotation[] anns)
    {
        return getInterceptorBindingMetaAnnotations(anns);
    }

    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link javax.inject.Qualifier} false otherwise.
     *
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link javax.inject.Qualifier} false otherwise
     */
    public boolean isQualifierAnnotation(Class<? extends Annotation> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        if (manager.hasBindingType(clazz))
        {
            return true;
        }
        else if (clazz.isAnnotationPresent(Qualifier.class))
        {
            return true;
        }
        else if(beanManagerImpl.getAdditionalQualifiers().contains(clazz))
        {
            return true;
        }

        return false;
    }

    public <X> Annotation[] getAnnotatedMethodFirstParameterQualifierWithGivenAnnotation(
            AnnotatedMethod<X> annotatedMethod, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(annotatedMethod, "annotatedMethod argument can not be null");
        Asserts.nullCheckForClass(clazz);

        List<Annotation> list = new ArrayList<Annotation>();
        List<AnnotatedParameter<X>> parameters = annotatedMethod.getParameters();
        for(AnnotatedParameter<X> parameter : parameters)
        {
            if(parameter.isAnnotationPresent(clazz))
            {
                Annotation[] anns = AnnotationUtil.getAnnotationsFromSet(parameter.getAnnotations());
                for(Annotation ann : anns)
                {
                    if(isQualifierAnnotation(ann.annotationType()))
                    {
                        list.add(ann);
                    }
                }
            }
        }

        Annotation[] finalAnns = new Annotation[list.size()];
        finalAnns = list.toArray(finalAnns);

        return finalAnns;
    }


    /**
     * Gets the method first found parameter qualifiers.
     *
     * @param method method
     * @param clazz checking annotation
     * @return annotation array
     */
    public Annotation[] getMethodFirstParameterQualifierWithGivenAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.nullCheckForClass(clazz);

        Annotation[][] parameterAnns = method.getParameterAnnotations();
        List<Annotation> list = new ArrayList<Annotation>();
        Annotation[] result;

        for (Annotation[] parameters : parameterAnns)
        {
            boolean found = false;
            for (Annotation param : parameters)
            {
                Class<? extends Annotation> btype = param.annotationType();
                if (btype.equals(clazz))
                {
                    found = true;
                    continue;
                }

                if (isQualifierAnnotation(btype))
                {
                    list.add(param);
                }

            }

            if (found)
            {
                result = new Annotation[list.size()];
                result = list.toArray(result);
                return result;
            }
        }
        result = new Annotation[0];
        return result;
    }

    /**
     * Gets the array of qualifier annotations on the given array.
     *
     * @param annotations annotation array
     * @return array containing qualifier anns
     */
    public Annotation[] getQualifierAnnotations(Annotation... annotations)
    {
        Asserts.assertNotNull(annotations, "Annotations argument can not be null");

        Set<Annotation> set = new HashSet<Annotation>();

        for (Annotation annot : annotations)
        {
            if (isQualifierAnnotation(annot.annotationType()))
            {
                set.add(annot);
            }
        }

        //Add the default qualifier if no others exist.  Section 3.10, OWB-142///
        if(set.size() == 0)
        {
            set.add(new DefaultLiteral());
        }
        ////////////////////////////////////////////////////////////////////////

        Annotation[] a = new Annotation[set.size()];
        a = set.toArray(a);

        return a;
    }


    /**
     * If the bean extends generic class via Realizes
     * annotation, realized based producer methods, fields and observer
     * methods qualifier is
     *
     * <ul>
     *  <li>Qualifiers on the definitions</li>
     *  <li>Plus class qualifiers</li>
     *  <li>Minus generic class qualifiers</li>
     * </ul>
     *
     * @param clazz realized definition class
     * @param anns binding annotations array
     */
    public Annotation[] getRealizesGenericAnnotations(Class<?> clazz, Annotation[] anns)
    {
       Set<Annotation> setAnnots = new HashSet<Annotation>();

        for(Annotation definedAnn : anns)
        {
            setAnnots.add(definedAnn);
        }

        Annotation[] genericReliazesAnns = getQualifierAnnotations(clazz.getSuperclass().getDeclaredAnnotations());

        for(Annotation generic : genericReliazesAnns)
        {
            setAnnots.remove(generic);
        }

        genericReliazesAnns = getQualifierAnnotations(clazz.getDeclaredAnnotations());

        for(Annotation generic : genericReliazesAnns)
        {
            setAnnots.add(generic);
        }

        Annotation[] annots = new Annotation[setAnnots.size()];
        annots = setAnnots.toArray(annots);

        return annots;
    }

    public void checkQualifierConditions(Annotation... qualifierAnnots)
    {
        Set<Annotation> annSet = ArrayUtil.asSet(qualifierAnnots);

        //check for duplicate annotations
        if (qualifierAnnots.length != annSet.size())
        {
            throw new IllegalArgumentException("Qualifier annotations can not contain duplicate qualifiers:" + qualifierAnnots);
        }

        checkQualifierConditions(annSet);
    }

    /**
     * This function obviously cannot check for duplicate annotations.
     * So this must have been done before!
     * @param qualifierAnnots
     */
    public void checkQualifierConditions(Set<Annotation> qualifierAnnots)
    {
        for (Annotation ann : qualifierAnnots)
        {
            checkQualifierConditions(ann);
        }
    }

    private void checkQualifierConditions(Annotation ann)
    {
        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(ann.annotationType());

        for (Method method : methods)
        {
            Class<?> clazz = method.getReturnType();
            if (clazz.isArray() || clazz.isAnnotation())
            {
                if (!AnnotationUtil.hasAnnotation(method.getDeclaredAnnotations(), Nonbinding.class))
                {
                    throw new WebBeansConfigurationException("@Qualifier : " + ann.annotationType().getName()
                                                             + " must have @NonBinding valued members for its array-valued and annotation valued members");
                }
            }
        }

        if (!isQualifierAnnotation(ann.annotationType()))
        {
            throw new IllegalArgumentException("Qualifier annotations must be annotated with @Qualifier");
        }
    }

    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link javax.enterprise.inject.Stereotype} false otherwise.
     *
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link javax.enterprise.inject.Stereotype} false otherwise
     */
    public boolean isStereoTypeAnnotation(Class<? extends Annotation> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        if (manager.hasStereoType(clazz))
        {
            return true;
        }
        else if (clazz.isAnnotationPresent(Stereotype.class))
        {
            return true;
        }

        return false;
    }

    public boolean hasStereoTypeMetaAnnotation(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, "anns parameter can not be null");

        for (Annotation ann : anns)
        {
            if (isStereoTypeAnnotation(ann.annotationType()))
            {
                return true;
            }
            else
            {
                continue;
            }
        }

        return false;
    }

    public Annotation[] getStereotypeMetaAnnotations(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, "anns parameter can not be null");
        List<Annotation> interAnns = new ArrayList<Annotation>();

        for (Annotation ann : anns)
        {
            if (isStereoTypeAnnotation(ann.annotationType()))
            {
                interAnns.add(ann);

                //check for transitive
                Annotation[] transitives = getTransitiveStereoTypes(ann.annotationType().getDeclaredAnnotations());

                for(Annotation transitive : transitives)
                {
                    interAnns.add(transitive);
                }
            }
        }

        Annotation[] ret = new Annotation[interAnns.size()];
        ret = interAnns.toArray(ret);

        return ret;
    }

    private Annotation[] getTransitiveStereoTypes(Annotation[] anns)
    {
        return getStereotypeMetaAnnotations(anns);
    }

}
