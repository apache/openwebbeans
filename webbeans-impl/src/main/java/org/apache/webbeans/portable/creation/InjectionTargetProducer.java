/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.portable.creation;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.AbstractInjectable;

/**
 * InjectionTargetProducer implementation.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean type info
 */
@SuppressWarnings("unchecked")
public class InjectionTargetProducer<T> extends AbstractProducer<T> implements InjectionTarget<T>
{
    /**
     * Creates a new injection target producer.
     * @param bean injection target bean
     */
    public InjectionTargetProducer(InjectionTargetBean<T> bean)
    {
        super(bean);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void inject(T instance, CreationalContext<T> ctx)
    {
        if(!(ctx instanceof CreationalContextImpl))
        {
            ctx = CreationalContextFactory.getInstance().wrappedCreationalContext(ctx, this.bean);
        }
        
        Object oldInstanceUnderInjection = AbstractInjectable.instanceUnderInjection.get();
        boolean isInjectionToAnotherBean = false;
        try
        {
            Contextual<?> contextual = null;
            if(ctx instanceof CreationalContextImpl)
            {
                contextual = ((CreationalContextImpl)ctx).getBean();
                isInjectionToAnotherBean = contextual == getBean(InjectionTargetBean.class) ? false : true;
            }
            
            if(!isInjectionToAnotherBean)
            {
                AbstractInjectable.instanceUnderInjection.set(instance);   
            }
                        
            InjectionTargetBean<T> bean = getBean(InjectionTargetBean.class);
            
            if(!(bean instanceof EnterpriseBeanMarker))
            {
                bean.injectResources(instance, ctx);
                bean.injectSuperFields(instance, ctx);
                bean.injectSuperMethods(instance, ctx);
                bean.injectFields(instance, ctx);
                bean.injectMethods(instance, ctx);            
            }                    
        }
        finally
        {
            if(oldInstanceUnderInjection != null)
            {
                AbstractInjectable.instanceUnderInjection.set(oldInstanceUnderInjection);   
            }
            else
            {
                AbstractInjectable.instanceUnderInjection.set(null);
                AbstractInjectable.instanceUnderInjection.remove();
            }
        }
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void postConstruct(T instance)
    {
        InjectionTargetBean<T> bean = getBean(InjectionTargetBean.class);    
        if(!(bean instanceof EnterpriseBeanMarker))
        {
            bean.postConstruct(instance,this.creationalContext);   
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preDestroy(T instance)
    {
        InjectionTargetBean<T> bean = getBean(InjectionTargetBean.class);
        bean.destroyCreatedInstance(instance, this.creationalContext);
    }

}