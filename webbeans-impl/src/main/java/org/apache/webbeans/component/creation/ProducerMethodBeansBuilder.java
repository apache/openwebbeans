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

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.portable.events.ProcessBeanAttributesImpl;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.UnproxyableResolutionException;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @param <T> bean class type
 */
public class ProducerMethodBeansBuilder<T> extends AbstractBeanBuilder
{    
    
    protected final WebBeansContext webBeansContext;
    protected final AnnotatedType<T> annotatedType;

    /**
     * Creates a new instance.
     * 
     */
    public ProducerMethodBeansBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType)
    {
        Asserts.assertNotNull(webBeansContext, Asserts.PARAM_NAME_WEBBEANSCONTEXT);
        Asserts.assertNotNull(annotatedType, "annotated type");
        this.webBeansContext = webBeansContext;
        this.annotatedType = annotatedType;
    }

    /**
     * {@inheritDoc}
     */
    public Set<ProducerMethodBean<?>> defineProducerMethods(InjectionTargetBean<T> bean, Set<ProducerFieldBean<?>> producerFields)
    {
        Set<ProducerMethodBean<?>> producerBeans = new HashSet<>();
        Set<AnnotatedMethod<? super T>> annotatedMethods = webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(annotatedType);
        Collection<AnnotatedMethod<?>> skipMethods = new HashSet<>();

        for(AnnotatedMethod<? super T> annotatedMethod: annotatedMethods)
        {
            boolean isProducer = annotatedMethod.isAnnotationPresent(Produces.class);
            if(isProducer &&
                annotatedMethod.getJavaMember().getDeclaringClass().equals(annotatedType.getJavaClass()))
            {
                checkProducerMethodForDeployment(annotatedMethod);
                boolean specialize = false;
                if(annotatedMethod.isAnnotationPresent(Specializes.class))
                {
                    if (annotatedMethod.isStatic())
                    {
                        throw new WebBeansConfigurationException("Specializing annotated producer method : " + annotatedMethod + " can not be static");
                    }
                    
                    specialize = true;
                }

                AnnotatedMethod<T> method = (AnnotatedMethod<T>) annotatedMethod;
                ProcessBeanAttributesImpl<T> processBeanAttributes = webBeansContext.getWebBeansUtil().fireProcessBeanAttributes(
                        annotatedMethod, annotatedMethod.getJavaMember().getReturnType(),
                        BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(method).build());
                if (processBeanAttributes != null)
                {
                    ProducerMethodBeanBuilder<T> producerMethodBeanCreator = new ProducerMethodBeanBuilder<>(bean, annotatedMethod, processBeanAttributes.getAttributes());

                    ProducerMethodBean<T> producerMethodBean = producerMethodBeanCreator.getBean();

                    UnproxyableResolutionException lazyException = webBeansContext.getDeploymentValidationService()
                            .validateProxyable(producerMethodBean, processBeanAttributes.isIgnoreFinalMethods());
                    if (lazyException != null) // should we use UnproxyableBean there too? if not required by TCK, better to fail eagerly
                    {
                        throw lazyException;
                    }
                    producerMethodBeanCreator.validate();

                    if (specialize)
                    {
                        producerMethodBean.setSpecializedBean(true);
                    }
                    producerMethodBean.setCreatorMethod(annotatedMethod.getJavaMember());

                    webBeansContext.getWebBeansUtil().setBeanEnableFlagForProducerBean(bean,
                            producerMethodBean,
                            AnnotationUtil.asArray(annotatedMethod.getAnnotations()));
                    WebBeansUtil.checkProducerGenericType(producerMethodBean, annotatedMethod.getJavaMember());

                    producerBeans.add(producerMethodBean);
                }
            }
            else if (isProducer)
            {
                skipMethods.add(annotatedMethod);
            }
            
        }

        // valid all @Disposes have a @Produces
        validateNoDisposerWithoutProducer(annotatedMethods, producerBeans, producerFields, skipMethods);

        return producerBeans;
    }

    /**
     * Check producer method is ok for deployment.
     * 
     * @param annotatedMethod producer method
     */
    private void checkProducerMethodForDeployment(AnnotatedMethod<? super T> annotatedMethod)
    {
        Asserts.assertNotNull(annotatedMethod, "annotatedMethod argument");

        if (annotatedMethod.isAnnotationPresent(Inject.class) || 
                annotatedMethod.isAnnotationPresent(Disposes.class) ||  
                annotatedMethod.isAnnotationPresent(Observes.class) ||
                annotatedMethod.isAnnotationPresent(ObservesAsync.class))
        {
            throw new WebBeansConfigurationException("Producer annotated method : " + annotatedMethod + " can not be annotated with"
                                                     + " @Initializer/@Destructor annotation or has a parameter annotated with @Disposes/@Observes");
        }
    }
}
