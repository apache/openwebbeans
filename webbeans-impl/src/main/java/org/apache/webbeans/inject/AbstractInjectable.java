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
package org.apache.webbeans.inject;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Provider;

import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.EventBean;
import org.apache.webbeans.component.InjectionPointBean;
import org.apache.webbeans.component.InstanceBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation of the {@link Injectable} contract.
 * 
 * <p>
 * Do actual injection via {@link AbstractInjectable#inject(InjectionPoint)}
 * </p>
 * 
 * @see InjectableField
 * @see InjectableConstructor
 * @see InjectableMethods
 */
public abstract class AbstractInjectable implements Injectable
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(AbstractInjectable.class);
    
    /** Owner bean of the injection point*/
    protected OwbBean<?> injectionOwnerBean;
    
    /**Creational context instance that is passed to bean's create*/
    protected CreationalContext<?> injectionOwnerCreationalContext;
    
    /**Field, method or constructor injection*/
    protected Member injectionMember;

    /**
     * Creates a new injectable.
     * 
     * @param bean owner bean
     * @param creaitonalContext creational context instance
     */
    protected AbstractInjectable(OwbBean<?> injectionOwnerBean, CreationalContext<?> injectionOwnerCreationalContext)
    {
        this.injectionOwnerBean = injectionOwnerBean;
        this.injectionOwnerCreationalContext = injectionOwnerCreationalContext;
    }

    /**
     * Gets the injected bean instance in its scoped context. 
     * @param injectionPoint injection point definition  
     * @return current bean instance in the resolved bean scope
     */
    public <T> Object inject(InjectionPoint injectionPoint)
    {
        logger.debug("Injected into bean : " + this.injectionOwnerBean.toString() + " with injection point : " + injectionPoint);
        Object injected = null;
        Bean<?> injectedBean = (Bean<?>)InjectionResolver.getInstance().getInjectionPointBean(injectionPoint);        
        
        if(isInstanceProviderInjection(injectionPoint))
        {
            InstanceBean.local.set(injectionPoint);
        }
        
        else if(isEventProviderInjection(injectionPoint))
        {
            EventBean.local.set(injectionPoint);
        }        
        
        //Injection for dependent instance InjectionPoint fields
        boolean dependentProducer = false;
        if(WebBeansUtil.isDependent(injectedBean))
        {
            if(!InjectionPoint.class.isAssignableFrom(ClassUtil.getClass(injectionPoint.getType())))
            {
                InjectionPointBean.local.set(injectionPoint);   
            }
            
            if(!injectionPoint.isTransient())
            {
                if(injectedBean instanceof AbstractProducerBean)
                {
                    if(this.injectionOwnerBean.isPassivationCapable())
                    {
                        dependentProducer = true;   
                    }
                }
            }
        }        
        
        injected = BeanManagerImpl.getManager().getInjectableReference(injectionPoint, this.injectionOwnerCreationalContext);
        
        if(dependentProducer)
        {
            if(!Serializable.class.isAssignableFrom(injected.getClass()))
            {
                throw new IllegalProductException("If a producer method or field of scope @Dependent returns an serializable object for injection " +
                		                                "into an injection point "+ injectionPoint +" that requires a passivation capable dependency");
            }
        }
        

        return injected;
    }
    
        
    /**
     * Returns injection points related with given member type of the bean.
     * @param member java member
     * @return injection points related with given member type
     */
    protected List<InjectionPoint> getInjectedPoints(Member member)
    {
        List<InjectionPoint> injectedFields = this.injectionOwnerBean.getInjectionPoint(member);
        
        return injectedFields;

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
    
    /**
     * Gets the component.
     * 
     * @return the component
     */
    public OwbBean<?> getInjectionOwnerComponent()
    {
        return this.injectionOwnerBean;
    }

}