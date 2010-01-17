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

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.component.EventBean;
import org.apache.webbeans.component.InstanceBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.util.AnnotationUtil;
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
    /** Owner bean of the injection point*/
    protected AbstractBean<?> injectionOwnerComponent;
    
    /**Creational context instance that is passed to bean's create*/
    protected CreationalContext<?> creationalContext;
    
    /**Field, method or constructor injection*/
    protected Member injectionMember;

    /**
     * Creates a new injectable.
     * 
     * @param bean owner bean
     * @param creaitonalContext creational context instance
     */
    protected AbstractInjectable(AbstractBean<?> bean, CreationalContext<?> creaitonalContext)
    {
        this.injectionOwnerComponent = bean;
        this.creationalContext = creaitonalContext;
    }

    /**
     * Gets the injected bean instance in its scoped context. 
     * @param injectionPoint injection point definition  
     * @return current bean instance in the resolved bean scope
     */
    @SuppressWarnings("unchecked")
    public <T> Object inject(InjectionPoint injectionPoint)
    {
        Object injected = null;
        
        if(injectionPoint.getType().equals(InjectionPoint.class))
        {
            //Try to inject dependent owner injection point
            //If this injection owner is dependent object then its
            //dependentOwnerInjectionPoint can not be null.
            return injectDependentOwnerInjectionPoint();
        }
        
        if(isInstanceProviderInjection(injectionPoint))
        {
            InstanceBean.local.set(injectionPoint);
        }
        
        else if(isEventProviderInjection(injectionPoint))
        {
            EventBean.local.set(injectionPoint);
        }
        
        //Get injection point Bean to look for @Dependent
        Bean<?> injectedBean = InjectionResolver.getInstance().getInjectionPointBean(injectionPoint);
        
        boolean dependent = false;
        
        //Managed @Dependence instances
        if (injectedBean.getScope().equals(Dependent.class))
        {
            dependent = true;
        }
        
        if(dependent && (WebBeansUtil.isManagedBean(this.injectionOwnerComponent) || 
                WebBeansUtil.isEnterpriseBean(this.injectionOwnerComponent)))
        {
            injected = injectForBeanDependent(injectedBean,injectionPoint);   
        }                 
        else
        {
            injected = injectForComponent(injectionPoint);
            
            if(dependent && WebBeansUtil.isProducerBean(this.injectionOwnerComponent))
            {
                if(this.creationalContext instanceof CreationalContext)
                {
                    CreationalContextImpl<?> cc = (CreationalContextImpl<?>)this.creationalContext;
                    CreationalContextImpl<Object> dependentCc = (CreationalContextImpl<Object>)BeanManagerImpl.getManager().createCreationalContext((Bean<Object>)injectedBean);
                    dependentCc.setOwnerCreational(cc);
                    
                    cc.addDependent((Bean<Object>)injectedBean, injected, dependentCc);                    
                }
            }
        }
         

        return injected;
    }
    
    /**
     * TODO Not Sure to correct!
     * Specification 5.6.1 is not explicit! 
     *
     * @return the injection point of the dependent owner
     */
    protected Object injectDependentOwnerInjectionPoint()
    {
        AbstractBean<?> dependentComponent = this.injectionOwnerComponent;
        InjectionPoint injectionPointOfOwner = dependentComponent.getDependentOwnerInjectionPoint();
        
        if(injectionPointOfOwner != null)
        {
            return injectionPointOfOwner;
        }
                
        return null;        
    }

    /**
     * check if any of the given resources is a resource annotation
     * @see AnnotationUtil#isResourceAnnotation(Class)
     */
    protected boolean isResource(Annotation... annotations)
    {
        return AnnotationUtil.hasResourceAnnotation(annotations); 
    }
    
    /**
     * Return dependent scoped injection point bean instance.
     * @param bean injected depedent scoped bean
     * @param injectionPoint injection point
     * @return injection point instance
     */
    private Object injectForBeanDependent(Bean<?> bean, InjectionPoint injectionPoint)
    {
        Object object = null;
        object = this.injectionOwnerComponent.getDependent(bean,injectionPoint, this.creationalContext);

        return object;
    }

    /**
     * Returns injection point instance.
     * @param injectionPoint injection point
     * @return injection point instance
     */
    private Object injectForComponent(InjectionPoint injectionPoint)
    {
        BeanManager manager = BeanManagerImpl.getManager();
        Object object = manager.getInjectableReference(injectionPoint, this.creationalContext);
                
        return object;
    }
    
    /**
     * Returns injection points related with given member type of the bean.
     * @param member java member
     * @return injection points related with given member type
     */
    protected List<InjectionPoint> getInjectedPoints(Member member)
    {
        List<InjectionPoint> injectedFields = this.injectionOwnerComponent.getInjectionPoint(member);
        
        return injectedFields;

    }

    private boolean isInstanceProviderInjection(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;            
            Class<?> clazz = (Class<?>) pt.getRawType();
            
            if(clazz.isAssignableFrom(Instance.class))
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
    public AbstractBean<?> getInjectionOwnerComponent()
    {
        return injectionOwnerComponent;
    }

}