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

import static org.apache.webbeans.util.InjectionExceptionUtils.throwUnsatisfiedResolutionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ObserverMethod;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.WebBeansAnnotatedTypeUtil;

/**
 * Abstract implementation of {@link InjectionTargetBeanCreator}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class type
 */
public abstract class AbstractInjecionTargetBeanCreator<T> extends AbstractBeanCreator<T> implements InjectionTargetBeanCreator<T>
{    
    
    private WebBeansContext webBeansContext;

    /**
     * Creates a new instance.
     * 
     * @param bean bean instance
     */
    public AbstractInjecionTargetBeanCreator(AbstractInjectionTargetBean<T> bean)
    {
        super(bean, bean.getAnnotatedType());
        webBeansContext = bean.getWebBeansContext();
    }
    
 
    /**
     * {@inheritDoc}
     */
    public void defineDisposalMethods()
    {
        final AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        Set<AnnotatedMethod<? super T>> annotatedMethods = getAnnotatedType().getMethods();    
        ProducerMethodBean<?> previous = null;
        for (AnnotatedMethod<? super T> annotatedMethod : annotatedMethods)
        {
            Method declaredMethod = annotatedMethod.getJavaMember();
            AnnotatedMethod<T> annt = (AnnotatedMethod<T>)annotatedMethod;
            List<AnnotatedParameter<T>> parameters = annt.getParameters();
            boolean found = false;
            for(AnnotatedParameter<T> parameter : parameters)
            {
                if(parameter.isAnnotationPresent(Disposes.class))
                {
                    found = true;
                    break;
                }
            }
            
            if(found)
            {
                WebBeansAnnotatedTypeUtil.checkProducerMethodDisposal(annotatedMethod);
                Type type = AnnotationUtil.getAnnotatedMethodFirstParameterWithAnnotation(annotatedMethod, Disposes.class);
                Annotation[] annot = annotationManager.getAnnotatedMethodFirstParameterQualifierWithGivenAnnotation(annotatedMethod, Disposes.class);

                InjectionResolver injectionResolver = webBeansContext.getBeanManagerImpl().getInjectionResolver();

                Set<Bean<?>> set = injectionResolver.implResolveByType(type, annot);
                if (set.isEmpty())
                {
                    throwUnsatisfiedResolutionException(type, declaredMethod, annot);
                }
                
                Bean<?> foundBean = set.iterator().next();
                ProducerMethodBean<?> pr = null;

                if (foundBean == null || !(foundBean instanceof ProducerMethodBean))
                {
                    throwUnsatisfiedResolutionException(annotatedMethod.getDeclaringType().getJavaClass(), declaredMethod, annot);
                }

                pr = (ProducerMethodBean<?>) foundBean;

                if (previous == null)
                {
                    previous = pr;
                }
                else
                {
                    // multiple same producer
                    if (previous.equals(pr))
                    {
                        throw new WebBeansConfigurationException("There are multiple disposal method for the producer method : " + pr.getCreatorMethod().getName() + " in class : "
                                                                 + annotatedMethod.getDeclaringType().getJavaClass());
                    }
                }

                Method producerMethod = pr.getCreatorMethod();
                //Disposer methods and producer methods must be in the same class
                if(!producerMethod.getDeclaringClass().getName().equals(declaredMethod.getDeclaringClass().getName()))
                {
                    throw new WebBeansConfigurationException("Producer method component of the disposal method : " + declaredMethod.getName() + " in class : "
                                                             + annotatedMethod.getDeclaringType().getJavaClass() + " must be in the same class!");
                }
                
                pr.setDisposalMethod(declaredMethod);

                webBeansContext.getAnnotatedTypeUtil().addMethodInjectionPointMetaData(getBean(), annotatedMethod);
                
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void defineInjectedFields()
    {
        webBeansContext.getAnnotatedTypeUtil().defineInjectedFields(getBean(), getAnnotatedType());
    }

    /**
     * {@inheritDoc}
     */
    public void defineInjectedMethods()
    {
        webBeansContext.getAnnotatedTypeUtil().defineInjectedMethods(getBean(), getAnnotatedType());
    }

    /**
     * {@inheritDoc}
     */
    public Set<ObserverMethod<?>> defineObserverMethods()
    {   
        return webBeansContext.getAnnotatedTypeUtil().defineObserverMethods(getBean(), getAnnotatedType());
    }

    /**
     * {@inheritDoc}
     */
    public Set<ProducerFieldBean<?>> defineProducerFields()
    {
        return webBeansContext.getAnnotatedTypeUtil().defineProducerFields(getBean(), getAnnotatedType());
    }

    /**
     * {@inheritDoc}
     */
    public Set<ProducerMethodBean<?>> defineProducerMethods()
    {
        return webBeansContext.getAnnotatedTypeUtil().defineProducerMethods(getBean(), getAnnotatedType());
    }
    
    /**
     * Return type-safe bean instance.
     */
    public AbstractInjectionTargetBean<T> getBean()
    {
        return (AbstractInjectionTargetBean<T>)super.getBean();
    }
}
