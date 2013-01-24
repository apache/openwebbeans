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
package org.apache.webbeans.component.creation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;

public class ProducerMethodBeanBuilder<T> extends AbstractProducerBeanBuilder<T, AnnotatedMethod<?>, ProducerMethodBean<T>>
{

    private boolean specialized;

    public ProducerMethodBeanBuilder(InjectionTargetBean<T> parent, AnnotatedMethod<?> annotatedMethod, BeanAttributesImpl<T> beanAttributes)
    {
        super(parent, annotatedMethod, beanAttributes);
    }

    public void configureProducerSpecialization(ProducerMethodBean<T> bean, AnnotatedMethod<T> annotatedMethod)
    {
        List<AnnotatedParameter<T>> annotatedParameters = annotatedMethod.getParameters();
        List<Class<?>> parameters = new ArrayList<Class<?>>();
        for(AnnotatedParameter<T> annotatedParam : annotatedParameters)
        {
            parameters.add(ClassUtil.getClass(annotatedParam.getBaseType()));
        }
        
        Method superMethod = ClassUtil.getClassMethodWithTypes(annotatedMethod.getDeclaringType().getJavaClass().getSuperclass(), 
                annotatedMethod.getJavaMember().getName(), parameters);
        if (superMethod == null)
        {
            throw new WebBeansConfigurationException("Anontated producer method specialization is failed : " + annotatedMethod.getJavaMember().getName()
                                                     + " not found in super class : " + annotatedMethod.getDeclaringType().getJavaClass().getSuperclass().getName()
                                                     + " for annotated method : " + annotatedMethod);
        }
        
        if (!AnnotationUtil.hasAnnotation(superMethod.getAnnotations(), Produces.class))
        {
            throw new WebBeansConfigurationException("Anontated producer method specialization is failed : " + annotatedMethod.getJavaMember().getName()
                                                     + " found in super class : " + annotatedMethod.getDeclaringType().getJavaClass().getSuperclass().getName()
                                                     + " is not annotated with @Produces" + " for annotated method : " + annotatedMethod);
        }

        /* To avoid multiple invocations of setBeanName(), following code is delayed to
         * configSpecializedProducerMethodBeans() when checkSpecializations.
        Annotation[] anns = AnnotationUtil.getQualifierAnnotations(superMethod.getAnnotations());

        for (Annotation ann : anns)
        {
            bean.addQualifier(ann);
        }
        
        WebBeansUtil.configuredProducerSpecializedName(bean, annotatedMethod.getJavaMember(), superMethod);
        */
        
        bean.setSpecializedBean(true);        
    }
    
    protected AnnotatedMethod<?> getSuperAnnotated()
    {
        AnnotatedMethod<?> thisMethod = annotatedMember;
        for (AnnotatedMethod<?> superMethod: getSuperType().getMethods())
        {
            List<AnnotatedParameter<?>> thisParameters = (List<AnnotatedParameter<?>>)(List<?>)thisMethod.getParameters();
            if (thisMethod.getJavaMember().getName().equals(superMethod.getJavaMember().getName())
                && thisMethod.getBaseType().equals(superMethod.getBaseType())
                && thisParameters.size() == superMethod.getParameters().size())
            {
                List<AnnotatedParameter<?>> superParameters = (List<AnnotatedParameter<?>>)(List<?>)superMethod.getParameters();
                boolean match = true;
                for (int i = 0; i < thisParameters.size(); i++)
                {
                    if (!thisParameters.get(i).getBaseType().equals(superParameters.get(i).getBaseType()))
                    {
                        match = false;
                        break;
                    }
                }
                if (match)
                {
                    return superMethod;
                }
            }
        }
        return null;
    }

    @Override
    protected ProducerMethodBean<T> createBean(InjectionTargetBean<?> parent, Class<T> beanClass)
    {
        ProducerMethodBean<T> producerMethodBean = new ProducerMethodBean<T>(parent, beanAttributes, beanClass);
        producerMethodBean.setSpecializedBean(specialized);
        return producerMethodBean;
    }
    
    public ProducerMethodBean<T> getBean()
    {
        return createBean((Class<T>) annotatedMember.getJavaMember().getReturnType());
    }
}
