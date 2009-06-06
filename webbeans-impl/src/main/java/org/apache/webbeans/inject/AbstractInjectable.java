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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.event.Fires;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.plugins.OpenWebBeansPlugin;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation of the {@link Injectable} contract.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public abstract class AbstractInjectable implements Injectable
{
    /** Owner component */
    private AbstractComponent<?> injectionOwnerComponent;
    
    private CreationalContext<?> creationalContext;
    
    protected Member injectionMember;
    
    protected Annotation[] injectionAnnotations = new Annotation[0];

    protected AbstractInjectable(AbstractComponent<?> component, CreationalContext<?> creaitonalContext)
    {
        this.injectionOwnerComponent = component;
        this.creationalContext = creaitonalContext;
    }

    /**
     * Gets the injected component instance in its scoped context.
     * 
     * @param type type of the injection point, maybe parametrized type
     * @param annotations binding annotations at the injection point
     * @return current component instance in the resolved component scope
     */
    public <T> Object inject(Class<T> type, Type[] args, Annotation... annotations)
    {
        boolean dependentContext = false;
        
        if(type.equals(InjectionPoint.class))
        {
            //Try to inject dependent owner injection point
            //If this injection owner is dependent object then its
            //dependentOwnerInjectionPoint can not be null.
            return injectDependentOwnerInjectionPoint();
        }
        
        if(!ContextFactory.checkDependentContextActive())
        {
            ContextFactory.activateDependentContext();
            dependentContext = true;
        }       
        
        try
        {
            if (isResource(this.injectionAnnotations))
            {
                return injectResource(type, this.injectionAnnotations);
            }
                        
            if (isObservableBinding(annotations))
            {
                return injectForObservable(args, annotations);
            }
            
            //Find injection point for injecting instance
            InjectionPoint injectionPoint = InjectionPointFactory.getPartialInjectionPoint(this.injectionOwnerComponent, type, this.injectionMember, this.injectionAnnotations, annotations);                        
            
            //Get injection point Bean component
            Bean<?> component = InjectionResolver.getInstance().getInjectionPointBean(injectionPoint);
            

            if (component.getScopeType().equals(Dependent.class))
            {
                if(WebBeansUtil.isSimpleWebBeans(this.injectionOwnerComponent))
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
        finally
        {
            if(dependentContext)
            {
                ContextFactory.passivateDependentContext();
            }
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
        AbstractComponent<?> dependentComponent = this.injectionOwnerComponent;
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

    private boolean isObservableBinding(Annotation... annotations)
    {
        for (Annotation ann : annotations)
        {
            if (ann.annotationType().equals(Fires.class))
            {
                return true;
            }
        }

        return false;
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
    
    private Object injectForObservable(Type[] args, Annotation... annotations)
    {
        Bean<?> bean = InjectionResolver.getInstance().implResolveByType(Event.class, args, annotations).iterator().next();
        
        return injectForDependent(bean,null);
    }
    
    private Object injectForDependent(Bean<?> component, InjectionPoint injectionPoint)
    {
        Object object = null;
        object = this.injectionOwnerComponent.getDependent(component,injectionPoint);

        return object;
    }

    private <T> Object injectForComponent(InjectionPoint injectionPoint)
    {
        Object object = ManagerImpl.getManager().getInstanceToInject(injectionPoint,this.creationalContext);
                
        return object;
    }

    protected void checkParametrizedTypeForInjectionPoint(ParameterizedType pType)
    {
        if (!ClassUtil.checkParametrizedType(pType))
        {
            throw new WebBeansConfigurationException("Injection point with parametrized type : " + pType + " can not define Type variable or Wildcard type");
        }
    }

    /**
     * Gets the component.
     * 
     * @return the component
     */
    public AbstractComponent<?> getInjectionOwnerComponent()
    {
        return injectionOwnerComponent;
    }

}