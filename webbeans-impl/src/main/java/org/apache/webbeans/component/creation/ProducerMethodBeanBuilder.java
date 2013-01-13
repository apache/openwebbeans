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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.inject.Named;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

public class ProducerMethodBeanBuilder<T> extends AbstractProducerBeanBuilder<T, AnnotatedMethod<?>, ProducerMethodBean<T>>
{

    private boolean specialized;

    public ProducerMethodBeanBuilder(InjectionTargetBean<T> parent, AnnotatedMethod<?> annotatedMethod)
    {
        super(parent, annotatedMethod);
    }

    public void configureProducerSpecialization(AnnotatedMethod<T> annotatedMethod)
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
        
        getBean().setSpecializedBean(true);        
    }
    
    /**
     * {@inheritDoc}
     */
    public void defineName()
    {
        if (getAnnotated().isAnnotationPresent(Specializes.class))
        {
            specialized = true;
            AnnotatedMethod<?> superAnnotated = getSuperAnnotated();
            defineName(superAnnotated, WebBeansUtil.getProducerDefaultName(superAnnotated.getJavaMember().getName()));
        }
        if (getName() == null)
        {
            defineName(getAnnotated(), WebBeansUtil.getProducerDefaultName(getAnnotated().getJavaMember().getName()));
        }
        else
        {
            // TODO XXX We have to check stereotypes here, too
            if (getAnnotated().isAnnotationPresent(Named.class))
            {
                throw new DefinitionException("@Specialized Producer method : " + getAnnotated().getJavaMember().getName()
                        + " may not explicitly declare a bean name");
            }
        }
    }

    protected AnnotatedMethod<?> getSuperAnnotated()
    {
        AnnotatedMethod<?> thisMethod = getAnnotated();
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
    protected Class<T> getBeanType()
    {
        return (Class<T>) getAnnotated().getJavaMember().getReturnType();
    }

    @Override
    protected ProducerMethodBean<T> createBean(InjectionTargetBean<?> parent,
                                               Set<Type> types,
                                               Set<Annotation> qualifiers,
                                               Class<? extends Annotation> scope,
                                               String name,
                                               boolean nullable,
                                               Class<T> beanClass,
                                               Set<Class<? extends Annotation>> stereotypes,
                                               boolean alternative)
    {
        ProducerMethodBean<T> producerMethodBean = new ProducerMethodBean<T>(parent, types, qualifiers, scope, name, nullable, beanClass, stereotypes, alternative);
        producerMethodBean.setSpecializedBean(specialized);
        return producerMethodBean;
    }
}
