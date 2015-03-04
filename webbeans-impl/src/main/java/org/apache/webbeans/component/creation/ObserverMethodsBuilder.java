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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.inject.Inject;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

/**
 *
 * @param <T> bean class type
 */
public class ObserverMethodsBuilder<T>
{    
    
    protected final WebBeansContext webBeansContext;
    protected final AnnotatedType<T> annotatedType;

    /**
     * Creates a new instance.
     * 
     */
    public ObserverMethodsBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType)
    {
        Asserts.assertNotNull(webBeansContext, "webBeansContext may not be null");
        Asserts.assertNotNull(annotatedType, "annotated type may not be null");
        this.webBeansContext = webBeansContext;
        this.annotatedType = annotatedType;
    }

    /**
     * {@inheritDoc}
     */
    public Set<ObserverMethod<?>> defineObserverMethods(AbstractOwbBean<T> bean)
    {   
        Set<ObserverMethod<?>> definedObservers = new HashSet<ObserverMethod<?>>();
        for (AnnotatedMethod<?> annotatedMethod : webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(annotatedType))
        {
            List<AnnotatedParameter<?>> parameters = (List<AnnotatedParameter<?>>)(List<?>)annotatedMethod.getParameters();
            AnnotatedParameter<?> observesParameter = null;
            for(AnnotatedParameter<?> parameter : parameters)
            {
                if(parameter.isAnnotationPresent(Observes.class))
                {
                    if (observesParameter != null)
                    {
                        throw new WebBeansConfigurationException("Observer method : " + annotatedMethod.getJavaMember().getName() 
                                + " in class : " + annotatedMethod.getJavaMember().getDeclaringClass().getName()
                                + " must not define two parameters that are annotated with @Observes");
                    }
                    observesParameter = parameter;
                }
            }            
            
            if(observesParameter != null)
            {
                checkObserverMethodConditions(bean, observesParameter);
                
                //Looking for ObserverMethod
                ObserverMethod<?> definedObserver = webBeansContext.getBeanManagerImpl().getNotificationManager().
                        getObservableMethodForAnnotatedMethod(annotatedMethod, observesParameter, bean);
                definedObservers.add(definedObserver);
            }
        }

        if (!definedObservers.isEmpty())
        {
            for (final InjectionPoint ip : bean.getInjectionPoints())
            {
                final Set<Annotation> qualifiers = ip.getQualifiers();
                if (EventMetadata.class == ip.getType()
                        && qualifiers != null && ip.getQualifiers().size() == 1
                        && Default.class == qualifiers.iterator().next().annotationType())
                {
                    throw new WebBeansConfigurationException(ip + " is not an observer parameter");
                }
            }
        }
        
        return definedObservers;
    }

    private void checkObserverMethodConditions(AbstractOwbBean<?> bean, AnnotatedParameter<?> annotatedParameter)
    {
        Asserts.assertNotNull(annotatedParameter, "annotatedParameter can not be null");
        
        AnnotatedMethod<?> annotatedMethod = (AnnotatedMethod<?>)annotatedParameter.getDeclaringCallable();

        if (annotatedMethod.isAnnotationPresent(Produces.class) 
                || annotatedMethod.isAnnotationPresent(Inject.class))
        {
            throw new WebBeansConfigurationException("Observer method : " + annotatedMethod.getJavaMember().getName() + " in class : " 
                                                     + annotatedMethod.getJavaMember().getDeclaringClass().getName()
                                                     + " can not annotated with annotation in the list {@Produces, @Initializer, @Destructor}");

        }

        if (AnnotationUtil.hasAnnotatedMethodParameterAnnotation(annotatedMethod, Disposes.class))
        {
            throw new WebBeansConfigurationException("Observer method : " + annotatedMethod.getJavaMember().getName() + " in class : "
                                                     + annotatedMethod.getJavaMember().getDeclaringClass().getName()
                                                     + " can not annotated with annotation @Disposes");
        }                

        if (bean.getScope().equals(Dependent.class))
        {
            //Check Reception
            Observes observes = annotatedParameter.getAnnotation(Observes.class);
            Reception reception = observes.notifyObserver();
            if(reception.equals(Reception.IF_EXISTS))
            {
                throw new WebBeansConfigurationException("Dependent Bean : " + annotatedType.getJavaClass() + " can not define observer method with @Receiver = IF_EXIST");
            }
        }
    }
}
