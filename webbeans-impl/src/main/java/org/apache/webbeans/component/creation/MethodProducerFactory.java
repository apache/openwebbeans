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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Inject;
import javax.inject.Named;

import javax.enterprise.inject.spi.ProducerFactory;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.portable.ProducerMethodProducer;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.GenericsUtil;

public class MethodProducerFactory<P> implements ProducerFactory<P>
{

    private AnnotatedMethod<? super P> producerMethod;
    private AnnotatedMethod<? super P> disposalMethod;
    private Bean<P> parent;
    private WebBeansContext webBeansContext;

    public MethodProducerFactory(AnnotatedMethod<? super P> producerMethod, Bean<P> parent, WebBeansContext webBeansContext)
    {
        Asserts.assertNotNull(producerMethod, "producer method may not be null");
        Asserts.assertNotNull(webBeansContext, "WebBeansContext may not be null");
        this.producerMethod = producerMethod;
        this.parent = parent;
        this.webBeansContext = webBeansContext;
        defineDisposalMethod();
    }

    @Override
    public <T> Producer<T> createProducer(Bean<T> bean)
    {
        Set<InjectionPoint> disposalIPs = null;
        if (disposalMethod != null)
        {
            disposalIPs = new HashSet<InjectionPoint>(webBeansContext.getInjectionPointFactory().buildInjectionPoints(bean, disposalMethod));
        }

        Producer<T> producer = new ProducerMethodProducer<T, P>(parent, producerMethod, disposalMethod, createInjectionPoints(bean), disposalIPs, webBeansContext);
        return webBeansContext.getWebBeansUtil().fireProcessProducerEvent(producer, producerMethod);
    }

    private void defineDisposalMethod()
    {
        Set<Annotation> producerQualifiers = webBeansContext.getAnnotationManager().getQualifierAnnotations(producerMethod.getAnnotations());
        if (producerQualifiers.size() == 1 && producerQualifiers.iterator().next().annotationType().equals(Default.class))
        {
            producerQualifiers = Collections.emptySet();
        }
        Set<Annotation> producerQualifiersWithoutNamed = new HashSet<Annotation>();
        for (Annotation qualifier: producerQualifiers)
        {
            if (!qualifier.annotationType().equals(Named.class))
            {
                producerQualifiersWithoutNamed.add(qualifier);
            }
        }
        Set<AnnotatedMethod<? super P>> annotatedMethods
            = (Set<AnnotatedMethod<? super P>>)(Set<?>)webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(producerMethod.getDeclaringType());        
        for (AnnotatedMethod<? super P> annotatedMethod : annotatedMethods)
        {            
            if (annotatedMethod.getDeclaringType().equals(producerMethod.getDeclaringType()))
            {
                for (AnnotatedParameter<? super P> annotatedParameter : annotatedMethod.getParameters())
                {
                    if (annotatedParameter.isAnnotationPresent(Disposes.class))
                    {
                        if (!GenericsUtil.satisfiesDependency(false, producerMethod.getBaseType(), annotatedParameter.getBaseType()))
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
                        for (Annotation disposalQualifier: disposalQualifiers)
                        {
                            boolean found = false;
                            for (Annotation producerQualifier: producerQualifiers)
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
                            continue;
                        }
                        if (disposalMethod != null)
                        {
                            throw new WebBeansConfigurationException("There are multiple disposal method for the producer method : "
                                    + disposalMethod.getJavaMember().getName() + " in class : "
                                    + annotatedMethod.getDeclaringType().getJavaClass());
                        }
                        if (!annotatedMethod.getDeclaringType().equals(producerMethod.getDeclaringType()))
                        {
                            throw new WebBeansConfigurationException("Producer method component of the disposal method : "
                                    + annotatedMethod.getJavaMember().getName() + " in class : "
                                    + annotatedMethod.getDeclaringType().getJavaClass() + " must be in the same class!");
                        }
                        checkDisposalMethod(annotatedMethod);
                        disposalMethod = (AnnotatedMethod<P>)annotatedMethod;
                    }
                }
            }
        }
    }    

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
                || annotatedMethod.isAnnotationPresent(Produces.class))
        {
            throw new WebBeansConfigurationException("Error in definining disposal method of annotated method : " + annotatedMethod
                    + ". Disposal methods  can not be annotated with" + " @Initializer/@Destructor/@Produces annotation or has a parameter annotated with @Observes.");
        }        
    }

    protected Set<InjectionPoint> createInjectionPoints(Bean<?> bean)
    {
        return new HashSet<InjectionPoint>(webBeansContext.getInjectionPointFactory().buildInjectionPoints(bean, producerMethod));
    }
}
