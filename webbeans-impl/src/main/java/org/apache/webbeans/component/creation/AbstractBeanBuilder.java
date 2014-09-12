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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.DefinitionException;
import java.util.Collection;
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
        for (final AnnotatedMethod<?> annotatedMethod : annotatedMethods)
        {
            for (final AnnotatedParameter<?> param : annotatedMethod.getParameters())
            {
                if (param.isAnnotationPresent(Disposes.class))
                {
                    boolean found = false;
                    for (final ProducerMethodBean<?> producer : producerBeans)
                    {
                        if (GenericsUtil.satisfiesDependency(false, producer.getCreatorMethod().getGenericReturnType(), param.getBaseType()))
                        {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        for (final ProducerFieldBean<?> field : producerFields)
                        {
                            if (GenericsUtil.satisfiesDependency(false, field.getCreatorField().getType(), param.getBaseType()))
                            {
                                found = true;
                                break;
                            }
                        }

                        if (!found)
                        {
                            // see if @Disposes should just be ignored as well - no inheritance
                            for (final AnnotatedMethod<?> producer : ignoredProducers)
                            {
                                if (GenericsUtil.satisfiesDependency(false, producer.getJavaMember().getGenericReturnType(), param.getBaseType()))
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
        for (final AnnotatedMethod<?> annotatedMethod : annotatedMethods)
        {
            if (annotatedMethod.isAnnotationPresent(Produces.class))
            {
                throw new DefinitionException("This class must not have a @Produces method" + annotatedMethod.getJavaMember());
            }

            for (AnnotatedParameter<?> parameter : annotatedMethod.getParameters())
            {
                if (parameter.isAnnotationPresent(Observes.class))
                {
                    throw new DefinitionException("This class must not have a @Observes method " + annotatedMethod.getJavaMember());
                }
            }
        }

        Set<AnnotatedField<? super T>> annotatedFields = annotatedType.getFields();
        for (final AnnotatedField<? super T> annotatedField : annotatedFields)
        {
            if (annotatedField.isAnnotationPresent(Produces.class))
            {
                throw new DefinitionException("This class must not have a @Produces field" + annotatedField.getJavaMember());
            }
        }
    }

}
