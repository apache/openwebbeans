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

import java.util.Set;

import javax.enterprise.inject.spi.ObserverMethod;

import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.config.WebBeansContext;

/**
 * Abstract implementation of {@link InjectedTargetBeanCreator}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class type
 */
public abstract class AbstractInjecionTargetBeanCreator<T> extends AbstractBeanCreator<T> implements InjectedTargetBeanCreator<T>
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
        webBeansContext.getAnnotatedTypeUtil().defineDisposalMethods(getBean(), getAnnotatedType());
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
        AbstractInjectionTargetBean bean = getBean();
        if(isDefaultMetaDataProvider())
        {
            return bean.getWebBeansContext().getDefinitionUtil().defineProducerFields(bean);
        }
        else
        {
            return bean.getWebBeansContext().getAnnotatedTypeUtil().defineProducerFields(bean, getAnnotatedType());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<ProducerMethodBean<?>> defineProducerMethods()
    {
        AbstractInjectionTargetBean bean = getBean();
        if(isDefaultMetaDataProvider())
        {
            return bean.getWebBeansContext().getDefinitionUtil().defineProducerMethods(bean);
        }
        else
        {
            return bean.getWebBeansContext().getAnnotatedTypeUtil().defineProducerMethods(bean, getAnnotatedType());
        }
    }
    
    /**
     * Return type-safe bean instance.
     */
    public AbstractInjectionTargetBean<T> getBean()
    {
        return (AbstractInjectionTargetBean<T>)super.getBean();
    }
}
