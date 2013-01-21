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
package org.apache.webbeans.inject;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.event.Event;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Provider;

import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.portable.EventProducer;
import org.apache.webbeans.portable.InjectionPointProducer;
import org.apache.webbeans.portable.InstanceProducer;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation of all components which are injectable.
 * 
 * <p>
 * Do actual injection via {@link AbstractInjectable#inject(InjectionPoint)}
 * </p>
 * 
 * @see InjectableField
 * @see InjectableConstructor
 * @see InjectableMethod
 */
public abstract class AbstractInjectable<T>
{

    private Producer<?> owner;
    
    protected final CreationalContextImpl<?> creationalContext;

    protected AbstractInjectable(Producer<?> owner, CreationalContextImpl<?> creationalContext)
    {
        this.owner = owner;
        this.creationalContext = creationalContext;
    }

    /**
     * Gets the injected bean instance in its scoped context. 
     * @param injectionPoint injection point definition  
     * @return current bean instance in the resolved bean scope
     */
    public T inject(InjectionPoint injectionPoint)
    {
        T injected;
        BeanManagerImpl beanManager = creationalContext.getWebBeansContext().getBeanManagerImpl();

        //Injected contextual beam
        InjectionResolver instance = beanManager.getInjectionResolver();

        Bean<?> injectedBean = instance.getInjectionPointBean(injectionPoint);
        if(isInstanceProviderInjection(injectionPoint))
        {
            InstanceProducer.local.set(injectionPoint);
        }
        
        else if(isEventProviderInjection(injectionPoint))
        {
            EventProducer.local.set(injectionPoint);
        }        
        
        boolean injectionPointBeanLocalSetOnStack = false;
        try 
        {
            //Injection for dependent instance InjectionPoint fields
            boolean dependentProducer = false;
            if(WebBeansUtil.isDependent(injectedBean))
            {
                if(!InjectionPoint.class.isAssignableFrom(ClassUtil.getClass(injectionPoint.getType())))
                {
                    injectionPointBeanLocalSetOnStack = InjectionPointProducer.setThreadLocal(injectionPoint);
                }

                if(!injectionPoint.isTransient())
                {
                    if(injectedBean instanceof AbstractProducerBean)
                    {
                        if((creationalContext.getBean() instanceof OwbBean) && ((OwbBean<?>) creationalContext.getBean()).isPassivationCapable())
                        {
                            dependentProducer = true;   
                        }
                    }
                }
            }        

            //Gets injectable reference for injected bean
            injected = (T) beanManager.getInjectableReference(injectionPoint, creationalContext);

            /*X TODO see spec issue CDI-140 */
            if(dependentProducer)
            {
                if(injected != null && !Serializable.class.isAssignableFrom(injected.getClass()))
                {
                    throw new IllegalProductException("If a producer method or field of scope @Dependent returns an serializable object for injection " +
                                                    "into an injection point "+ injectionPoint +" that requires a passivation capable dependency");
                }
            }
        } 
        finally
        {
            if (injectionPointBeanLocalSetOnStack)
            {
                InjectionPointProducer.unsetThreadLocal();
            }
        }
        

        return injected;
    }
    
    protected Contextual<?> getBean()
    {
        return creationalContext.getBean();
    }

    protected WebBeansContext getWebBeansContext()
    {
        return creationalContext.getWebBeansContext();
    }
        
    /**
     * Returns injection points related with given member type of the bean.
     * @param member java member
     * @return injection points related with given member type
     */
    protected List<InjectionPoint> getInjectionPoints(Member member)
    {
        List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();
        for (InjectionPoint injectionPoint : owner.getInjectionPoints())
        {
            if (injectionPoint.getMember().equals(member))
            {
                injectionPoints.add(injectionPoint);
            }
        }
        return injectionPoints;
    }

    private boolean isInstanceProviderInjection(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;            
            Class<?> clazz = (Class<?>) pt.getRawType();
            
            if(Provider.class.isAssignableFrom(clazz))
            {
                return true;
            }
        }
        
        return false;
    }
    
    
    private boolean isEventProviderInjection(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;            
            Class<?> clazz = (Class<?>) pt.getRawType();
            
            if(clazz.isAssignableFrom(Event.class))
            {
                return true;
            }
        }
        
        return false;
    }
}
