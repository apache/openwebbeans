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
import java.lang.reflect.Type;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.plugins.OpenWebBeansPlugin;
import org.apache.webbeans.plugins.PluginLoader;
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
     * 
     * @param injectionPoint injection point definition 
     * 
     * @return current bean instance in the resolved bean scope
     */
    public <T> Object inject(InjectionPoint injectionPoint)
    {
        if(injectionPoint.getType().equals(InjectionPoint.class))
        {
            //Try to inject dependent owner injection point
            //If this injection owner is dependent object then its
            //dependentOwnerInjectionPoint can not be null.
            return injectDependentOwnerInjectionPoint();
        }
        
        Annotation[] injectionAnnotations = injectionPoint.getAnnotated().getAnnotations().toArray(new Annotation[0]);
        
        if (isResource(injectionAnnotations))
        {
            return injectResource(injectionPoint.getType(),injectionAnnotations);
        }
                    
        //Get injection point Bean component
        Bean<?> component = InjectionResolver.getInstance().getInjectionPointBean(injectionPoint);
        
        if (component.getScopeType().equals(Dependent.class))
        {
            if(WebBeansUtil.isManagedBean(this.injectionOwnerComponent))
            {
                return injectForDependent(component,injectionPoint);   
            }                
            
            else
            {
                return injectForComponent(injectionPoint);
            }
        }
        else
        {
            return injectForComponent(injectionPoint);
        }
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
     * If the annotation is a resource annotation, we create 
     * the instance for injecting web beans resources.
     * @param type the class type which should be created
     * @param annotations which has been defined in the web bean
     * @return the instance linked with the annotation
     * @see WebBeansUtil#checkForValidResources(Type, Class, String, Annotation[])
     */
    private Object injectResource(Type type, Annotation... annotations)
    {
        List<OpenWebBeansPlugin> plugins = PluginLoader.getInstance().getPlugins();
        for (OpenWebBeansPlugin plugin : plugins)
        {
            Object toInject = plugin.injectResource(type, annotations);
            if (toInject != null)
            {
                return toInject;
            }
        }
        
        return null;
    }
    
    private Object injectForDependent(Bean<?> component, InjectionPoint injectionPoint)
    {
        Object object = null;
        object = this.injectionOwnerComponent.getDependent(component,injectionPoint);

        return object;
    }

    private <T> Object injectForComponent(InjectionPoint injectionPoint)
    {
        Object object = BeanManagerImpl.getManager().getInstanceToInject(injectionPoint,this.creationalContext);
                
        return object;
    }
    
    protected List<InjectionPoint> getInjectedPoints(Member member)
    {
        List<InjectionPoint> injectedFields = this.injectionOwnerComponent.getInjectionPoint(member);
        
        return injectedFields;

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