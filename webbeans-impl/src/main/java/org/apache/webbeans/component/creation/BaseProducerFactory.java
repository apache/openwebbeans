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

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.GenericsUtil;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProducerFactory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseProducerFactory<P> implements ProducerFactory<P>
{

    protected AnnotatedMethod<? super P> disposalMethod;
    protected Bean<P> parent;
    protected WebBeansContext webBeansContext;

    public BaseProducerFactory(Bean<P> parent, WebBeansContext webBeansContext)
    {
        Asserts.assertNotNull(webBeansContext, Asserts.PARAM_NAME_WEBBEANSCONTEXT);
        this.parent = parent;
        this.webBeansContext = webBeansContext;
    }

    protected <T> Set<InjectionPoint> getInjectionPoints(Bean<T> bean)
    {
        Set<InjectionPoint> disposalIPs = null;
        if (disposalMethod != null)
        {
            disposalIPs = new HashSet<>(webBeansContext.getInjectionPointFactory().buildInjectionPoints(bean, disposalMethod));
        }
        return disposalIPs;
    }

    protected void defineDisposalMethod()
    {
        AnnotatedMember<? super P> producer = producerType();
        Set<Annotation> producerQualifiers = webBeansContext.getAnnotationManager().getQualifierAnnotations(producer.getAnnotations());
        if (producerQualifiers.size() == 1 && producerQualifiers.iterator().next().annotationType().equals(Default.class))
        {
            producerQualifiers = Collections.emptySet();
        }
        Set<Annotation> producerQualifiersWithoutNamed = new HashSet<>();
        for (Annotation qualifier: producerQualifiers)
        {
            if (!qualifier.annotationType().equals(Named.class))
            {
                producerQualifiersWithoutNamed.add(qualifier);
            }
        }

        AnnotatedType declaringType = producer.getDeclaringType();
        Type producerBaseType = producerType().getBaseType();
        Set<AnnotatedMethod<? super P>> annotatedMethods =
                webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(declaringType);

        AnnotatedMethod<? super P> anyDisposal = null;

        for (AnnotatedMethod<? super P> annotatedMethod : annotatedMethods)
        {
            if (annotatedMethod.getDeclaringType().equals(declaringType))
            {
                for (AnnotatedParameter<? super P> annotatedParameter : annotatedMethod.getParameters())
                {
                    if (annotatedParameter.isAnnotationPresent(Disposes.class))
                    {
                        if (!GenericsUtil.satisfiesDependency(false, true, producerBaseType, annotatedParameter.getBaseType(), new HashMap<>()))
                        {
                            continue;
                        }

                        Set<Annotation> producerQualifiersToCompare = producerQualifiers;
                        Set<Annotation> disposalQualifiers = webBeansContext.getAnnotationManager().getQualifierAnnotations(annotatedParameter.getAnnotations());
                        if (disposalQualifiers.size() == 1 && disposalQualifiers.iterator().next().annotationType().equals(Default.class))
                        {
                            disposalQualifiers = Collections.emptySet();
                        }
                        if (disposalQualifiers.size() == producerQualifiersToCompare.size() - 1)
                        {
                            // when @Named is present at the producer it may be ignored at the disposal
                            producerQualifiersToCompare = producerQualifiersWithoutNamed;
                        }
                        if (disposalQualifiers.size() != producerQualifiersToCompare.size())
                        {
                            continue;
                        }
                        boolean same = true;
                        for (Annotation disposalQualifier : disposalQualifiers)
                        {
                            boolean found = false;
                            for (Annotation producerQualifier : producerQualifiers)
                            {
                                if (AnnotationUtil.isCdiAnnotationEqual(producerQualifier, disposalQualifier))
                                {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found)
                            {
                                same = false;
                                break;
                            }
                        }
                        if (!same)
                        {
                            if (disposalQualifiers.size() == 1 && AnyLiteral.INSTANCE.equals(disposalQualifiers.iterator().next()))
                            {
                                anyDisposal = annotatedMethod;
                            }
                            continue;
                        }
                        if (disposalMethod != null)
                        {
                            throw new WebBeansConfigurationException("There are multiple disposal method for the producer method : "
                                    + disposalMethod.getJavaMember().getName() + " in class : "
                                    + annotatedMethod.getDeclaringType().getJavaClass());
                        }
                        validateDisposalMethod(declaringType, annotatedMethod);
                        disposalMethod = annotatedMethod;
                    }
                }
            }
        }
        if (disposalMethod == null && anyDisposal != null)
        {
            validateDisposalMethod(declaringType, anyDisposal);
            disposalMethod = anyDisposal;
        }
    }

    private void validateDisposalMethod(AnnotatedType declaringType, AnnotatedMethod<? super P> annotatedMethod)
    {
        if (!annotatedMethod.getDeclaringType().equals(declaringType))
        {
            throw new WebBeansConfigurationException("Producer method component of the disposal method : "
                    + annotatedMethod.getJavaMember().getName() + " in class : "
                    + annotatedMethod.getDeclaringType().getJavaClass() + " must be in the same class!");
        }
        checkDisposalMethod(annotatedMethod);
    }

    protected abstract AnnotatedMember<? super P> producerType();

    private void checkDisposalMethod(AnnotatedMethod<? super P> annotatedMethod)
    {
        boolean found = false;
        for (AnnotatedParameter<?> parameter : annotatedMethod.getParameters())
        {
            if(parameter.isAnnotationPresent(Disposes.class))
            {
                if(found)
                {
                    throw new WebBeansConfigurationException("Error in definining disposal method of annotated method : " + annotatedMethod
                            + ". Multiple disposes annotation.");
                }
                found = true;
            }
        }
        
        if(annotatedMethod.isAnnotationPresent(Inject.class) 
            || AnnotationUtil.hasAnnotatedMethodParameterAnnotation(annotatedMethod, Observes.class)
            || AnnotationUtil.hasAnnotatedMethodParameterAnnotation(annotatedMethod, ObservesAsync.class)
            || annotatedMethod.isAnnotationPresent(Produces.class))
        {
            throw new WebBeansConfigurationException("Error in definining disposal method of annotated method : " + annotatedMethod
                    + ". Disposal methods  can not be annotated with" + " @Initializer/@Destructor/@Produces annotation or has a parameter annotated with @Observes.");
        }

        for (AnnotatedParameter param : annotatedMethod.getParameters())
        {
            Type type = param.getBaseType();
            if (type.equals(InjectionPoint.class))
            {

                throw new WebBeansConfigurationException("Error in definining disposal method of annotated method : " + annotatedMethod
                    + ". Disposal methods must not have an InjectionPoint.");
            }
            else if (Bean.class.isAssignableFrom(ClassUtil.getClass(type)))
            {
                throw new WebBeansConfigurationException("Error in defining disposal method of annoted method: " + annotatedMethod +
                        ". Disposal methods must not have a Bean parameter.");
            }
        }
    }
}
