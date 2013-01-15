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
package org.apache.webbeans.portable.creation;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;

import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.AbstractInjectable;
import org.apache.webbeans.intercept.InvocationContextImplRemove;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.proxy.ProxyFactory;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * InjectionTargetProducer implementation.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean type info
 *
 * @deprecated replaced by {@link org.apache.webbeans.portable.InjectionTargetImpl}
 */
@SuppressWarnings("unchecked")
public class InjectionTargetProducerRemove<T> extends AbstractProducerRemove<T> implements InjectionTarget<T>
{
    private Logger logger;

    /**
     * Creates a new injection target producer.
     * @param bean injection target bean
     */
    public InjectionTargetProducerRemove(InjectionTargetBean<T> bean)
    {
        super(bean);
    }
        
    /**
     * {@inheritDoc}
     */
    public void inject(T instance, CreationalContext<T> ctx)
    {
        if(!(ctx instanceof CreationalContextImpl))
        {
            ctx = bean.getWebBeansContext().getCreationalContextFactory().wrappedCreationalContext(ctx, bean);
        }
        
        Object oldInstanceUnderInjection = AbstractInjectable.instanceUnderInjection.get();
        boolean isInjectionToAnotherBean = false;
        try
        {
            Contextual<?> contextual = null;
            if(ctx instanceof CreationalContextImpl)
            {
                contextual = ((CreationalContextImpl)ctx).getBean();
                isInjectionToAnotherBean = contextual != getBean(InjectionTargetBean.class);
            }
            
            if(!isInjectionToAnotherBean)
            {
                AbstractInjectable.instanceUnderInjection.set(instance);   
            }
                        
            InjectionTargetBean<T> bean = getBean(InjectionTargetBean.class);
            
            if(!(bean instanceof EnterpriseBeanMarker))
            {
                //GE: Currently we have a proxy for DependentScoped beans
                //that has an interceptor or decroator. This means that
                //injection will be occured on Proxy instances that are 
                //not correct. Injection must be on actual dependent
                //instance,so not necessary to inject on proxy
                final ProxyFactory proxyFactory = this.bean.getWebBeansContext().getProxyFactory();
                if(bean.getScope() == Dependent.class && proxyFactory.isProxyInstanceRemove(instance))
                {
                    return;
                }
                
                bean.injectResources(instance, ctx);
                bean.getInjectionTarget().inject(instance, ctx);
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
    public void postConstruct(T instance)
    {
        InjectionTargetBean<T> bean = getBean(InjectionTargetBean.class);    
        if(!(bean instanceof EnterpriseBeanMarker))
        {
            if(bean.getWebBeansType().equals(WebBeansType.MANAGED))
            {
                // Call Post Construct
                if (WebBeansUtil.isContainsInterceptorMethod(bean.getInterceptorStack(), InterceptionType.POST_CONSTRUCT))
                {
                    InvocationContextImplRemove impl = new InvocationContextImplRemove(bean.getWebBeansContext(), null, instance, null, null,
                            bean.getWebBeansContext().getInterceptorUtil().getInterceptorMethods(bean.getInterceptorStack(),
                                                                                            InterceptionType.POST_CONSTRUCT),
                                                                                            InterceptionType.POST_CONSTRUCT);
                    impl.setCreationalContext(creationalContext);
                    try
                    {
                        impl.proceed();
                    }

                    catch (Exception e)
                    {
                        getLogger().log(Level.SEVERE, WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0008, "@PostConstruct."), e);
                        throw new WebBeansException(e);
                    }
                }            
            }        
        }
    }

    /**
     * {@inheritDoc}
     */
    public void preDestroy(T instance)
    {
        InjectionTargetBean<T> bean = getBean(InjectionTargetBean.class);
        bean.destroyCreatedInstance(instance, creationalContext);
    }

    /**
     * The Logger should really only be used to log errors!
     */
    protected synchronized Logger getLogger()
    {
        if (logger == null)
        {
            logger = WebBeansLoggerFacade.getLogger(getClass());
        }
        return logger;
    }
}
