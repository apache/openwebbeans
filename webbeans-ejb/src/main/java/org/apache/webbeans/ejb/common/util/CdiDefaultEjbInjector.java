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
package org.apache.webbeans.ejb.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.ejb.common.component.BaseEjbBean;
import org.apache.webbeans.ejb.common.interceptor.OpenWebBeansEjbInterceptor;
import org.apache.webbeans.inject.AbstractInjectable;
import org.apache.webbeans.inject.OWBInjector;

@SuppressWarnings("unchecked")
public class CdiDefaultEjbInjector
{
    /**Ejb instances per creational context*/
    private final ConcurrentMap<Object, List<CreationalContextImpl<Object>>> injectedNonContextuals = 
        new ConcurrentHashMap<Object, List<CreationalContextImpl<Object>>>();
    
    /**Contextual ejb instance per key*/
    private final ConcurrentMap<Object, CreationalContextImpl<Object>> injectedContextuals = 
        new ConcurrentHashMap<Object, CreationalContextImpl<Object>>();
    
    private final BeanManagerImpl beanManager;
    
    /**
     * Default constructor.
     */
    public CdiDefaultEjbInjector()
    {
        super();
        this.beanManager = WebBeansContext.getInstance().getBeanManagerImpl();
    }
    
    /**
     * Inject dependencies of given ejb instance.
     * @param ejbInstance ejb instance
     * @throws Exception if any exception occurs
     */
    public void injectDependenciesOfEjbInstance(Object ejbInstance, Object mapKey) throws Exception
    {
        BaseEjbBean<Object> ejbBean = (BaseEjbBean<Object>)OpenWebBeansEjbInterceptor.getEjbBean();
        if(ejbBean != null)
        {
            CreationalContextImpl<Object> contextualCc = (CreationalContextImpl<Object>)OpenWebBeansEjbInterceptor.getThreadCreationalContext();
            injectDependenciesOfContextualEjb(ejbInstance,ejbBean, contextualCc);
            this.injectedContextuals.putIfAbsent(mapKey, contextualCc);
        }
        else
        {
            injectDependenciesOfNonContextualEjb(ejbInstance, mapKey);
        }
    }
    
    /**
     * Inject dependencies of given interceptor instance.
     * @param ejbInstance ejb instance
     * @param interceptorInstance interceptor instance
     * @throws Exception if any exception occurs
     */
    public void injectDependenciesOfEjbInterceptor(Object ejbInstance, Object interceptorInstance, Object mapKey) throws Exception
    {
        OWBInjector owbInjector = new OWBInjector();
        CreationalContextImpl<Object> cc = null;
        //Look for contextual ejb or not
        if(this.injectedContextuals.containsKey(mapKey))
        {
            cc = this.injectedContextuals.get(mapKey);
            
            //Add all dependencies of interceptor to ejb bean
            owbInjector.inject(interceptorInstance,cc);
        }                
        else
        {
            cc = (CreationalContextImpl<Object>)this.beanManager.createCreationalContext(null);
            owbInjector.inject(interceptorInstance,cc);
            this.injectedNonContextuals.get(mapKey).add(cc);
        }                    
    }
        
    
    /**
     * Release context for noncontextual ejb instance.
     * @param destroyInstance ejb instance
     * under destroy
     * @throws Exception if any exception occurs
     */
    public void releaseNonContextualDependents(Object mapKey) throws Exception
    {
        if(this.injectedContextuals.containsKey(mapKey))
        {
            this.injectedContextuals.remove(mapKey);
        }
        else
        {
            List<CreationalContextImpl<Object>> ccList = this.injectedNonContextuals.remove(mapKey);        
            if(ccList != null)
            {
                for(CreationalContextImpl<Object> cc : ccList)
                {
                    cc.release();
                }            
            }            
        }        
    }    
    
    /**
     * Inject dependencies of contextual ejb bean.
     * @param instance ejb instance
     * @param bean ejb bean
     * @param ctx creational context
     * @throws Exception if exception occurs
     */
    private void injectDependenciesOfContextualEjb(Object instance, BaseEjbBean<Object> bean, CreationalContext<Object> ctx) throws Exception
    {
        if(!(ctx instanceof CreationalContextImpl))
        {
            ctx = WebBeansContext.getInstance().getCreationalContextFactory().wrappedCreationalContext(ctx, bean);
        }
        
        Object oldInstanceUnderInjection = AbstractInjectable.instanceUnderInjection.get();
        boolean isInjectionToAnotherBean = false;
        try
        {
            Contextual<?> contextual = null;
            if(ctx instanceof CreationalContextImpl)
            {
                contextual = ((CreationalContextImpl<Object>)ctx).getBean();
                isInjectionToAnotherBean = contextual == bean ? false : true;
            }
            
            if(!isInjectionToAnotherBean)
            {
                AbstractInjectable.instanceUnderInjection.set(instance);   
            }
                        
            //Injection of @Inject
            bean.injectSuperFields(instance, ctx);
            bean.injectSuperMethods(instance, ctx);
            bean.injectFields(instance, ctx);
            bean.injectMethods(instance, ctx);            
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
     * Inject dependencies of non-contextual instance.
     * @param ejbInstance non contextual instance
     * @throws Exception if any exception occurs
     */
    private void injectDependenciesOfNonContextualEjb(Object instance, Object mapKey) throws Exception
    {
        OWBInjector owbInjector = new OWBInjector();        
        CreationalContextImpl<Object> cc = (CreationalContextImpl<Object>)this.beanManager.createCreationalContext(null);
        owbInjector.inject(instance,cc);
        
        List<CreationalContextImpl<Object>> ccList = this.injectedNonContextuals.get(mapKey);
        if(ccList == null)
        {
            ccList = new ArrayList<CreationalContextImpl<Object>>();
        }

        ccList.add(cc);
        this.injectedNonContextuals.putIfAbsent(mapKey, ccList);        
    }            
    
}
