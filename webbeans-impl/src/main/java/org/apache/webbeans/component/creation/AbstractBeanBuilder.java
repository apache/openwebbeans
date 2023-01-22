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

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.GenericsUtil;

/**
 * Base class for all bean builders
 */
public abstract class AbstractBeanBuilder<T>
{

    /**
     * Make sure there is no disposer method without a corresponding producer method.
     * @param annotatedMethods of the given bean class
     * @param producerBeans or an empty Set
     */
    protected void validateNoDisposerWithoutProducer(Set<AnnotatedMethod<? super T>> annotatedMethods,
                                                     Set<ProducerMethodBean<?>> producerBeans,
                                                     Set<ProducerFieldBean<?>> producerFields,
                                                     Collection<AnnotatedMethod<?>> ignoredProducers)
    {
        for (AnnotatedMethod<?> annotatedMethod : annotatedMethods)
        {
            for (AnnotatedParameter<?> param : annotatedMethod.getParameters())
            {
                if (param.isAnnotationPresent(Disposes.class))
                {
                    boolean found = false;
                    for (ProducerMethodBean<?> producer : producerBeans)
                    {
                        if (GenericsUtil.satisfiesDependency(false, true, producer.getCreatorMethod().getGenericReturnType(), param.getBaseType(), new HashMap<>()))
                        {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        for (ProducerFieldBean<?> field : producerFields)
                        {
                            if (GenericsUtil.satisfiesDependency(false, true, field.getCreatorField().getType(), param.getBaseType(), new HashMap<>()))
                            {
                                found = true;
                                break;
                            }
                        }

                        if (!found)
                        {
                            // see if @Disposes should just be ignored as well - no inheritance
                            for (AnnotatedMethod<?> producer : ignoredProducers)
                            {
                                if (GenericsUtil.satisfiesDependency(false, true, producer.getJavaMember().getGenericReturnType(), param.getBaseType(), new HashMap<>()))
                                {
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (!found)
                        {
                            throw new WebBeansConfigurationException("@Disposes without @Produces " + annotatedMethod.getJavaMember());
                        }
                    }
                    break;
                }
            }
        }
    }


    /**
     * Certain beans like CDI Interceptors and Decorators
     * are not allowed to define producer methods.
     */
    protected void validateNoProducerOrObserverMethod(AnnotatedType<T> annotatedType)
    {
        Set<AnnotatedMethod<? super T>> annotatedMethods = annotatedType.getMethods();
        for (AnnotatedMethod<?> annotatedMethod : annotatedMethods)
        {
            if (annotatedMethod.isAnnotationPresent(Produces.class))
            {
                throw new WebBeansConfigurationException("This class must not have a @Produces method" + annotatedMethod.getJavaMember());
            }

            for (AnnotatedParameter<?> parameter : annotatedMethod.getParameters())
            {
                if (parameter.isAnnotationPresent(Observes.class) || parameter.isAnnotationPresent(ObservesAsync.class))
                {
                    throw new WebBeansConfigurationException("This class must not have an @Observes nor @ObservesAsync method " + annotatedMethod.getJavaMember());
                }
            }
        }

        Set<AnnotatedField<? super T>> annotatedFields = annotatedType.getFields();
        for (AnnotatedField<? super T> annotatedField : annotatedFields)
        {
            if (annotatedField.isAnnotationPresent(Produces.class))
            {
                throw new WebBeansConfigurationException("This class must not have a @Produces field" + annotatedField.getJavaMember());
            }
        }
    }

}
