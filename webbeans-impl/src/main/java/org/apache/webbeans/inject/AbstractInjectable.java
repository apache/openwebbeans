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

import javax.context.CreationalContext;
import javax.context.Dependent;
import javax.event.Fires;
import javax.inject.Instance;
import javax.inject.New;
import javax.inject.Obtains;
import javax.inject.manager.Bean;
import javax.inject.manager.InjectionPoint;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.event.EventImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.spi.JPAService;
import org.apache.webbeans.spi.ServiceLoader;
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
            return null;
        }
        
        if(!ContextFactory.checkDependentContextActive())
        {
            ContextFactory.activateDependentContext();
            dependentContext = true;
        }       
        
        try
        {
            if (isResource(annotations))
            {
                //X TODO do we need the args too?
                return injectResource(type, annotations);
            }
            
            if (isNewBinding(annotations))
            {
                return injectForNew(type, annotations);
            }

            if (isObservableBinding(annotations))
            {
                return injectForObservable(args, annotations);
            }
            
            if(isObtainsBinding(annotations))
            {
                return injectForObtains(type, args, annotations);
            }
            
            InjectionPoint injectionPoint = InjectionPointFactory.getPartialInjectionPoint(this.injectionOwnerComponent, type, this.injectionMember, this.injectionAnnotations, annotations);                        
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
     * check if any of the given resources is a resource annotation
     * @see AnnotationUtil#isResourceAnnotation(Class)
     */
    protected boolean isResource(Annotation... annotations)
    {
        return AnnotationUtil.hasResourceAnnotation(annotations);
    }

    private boolean isNewBinding(Annotation... annotations)
    {
        if (annotations.length == 1)
        {
            if (annotations[0].annotationType().equals(New.class))
            {
                return true;
            }
        }

        return false;
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
    
    private boolean isObtainsBinding(Annotation... annotations)
    {
        for (Annotation ann : annotations)
        {
            if (ann.annotationType().equals(Obtains.class))
            {
                return true;
            }
        }

        return false;
    }
    

    /**
     * create the instance for injecting web beans resources.
     * @see AnnotationUtil#isResourceAnnotation(Class)
     * @param type the class type which should be created
     * @param annotations which has been defined in the web bean
     * @return the instance linked with the annotation
     * @see WebBeansUtil#checkForValidResources(Type, Class, String, Annotation[])
     */
    private Object injectResource(Type type, Annotation... annotations)
    {
        Object ret = null;
        Annotation annot = AnnotationUtil.getAnnotation(annotations, PersistenceUnit.class);
        if (annot != null)
        {
            PersistenceUnit pu = (PersistenceUnit) annot;
            String unitName = pu.unitName();
            
            //X TODO what if the EntityManagerFactory is null?
            return getJPAService().getPersistenceUnit(unitName);
        }
        
        annot = AnnotationUtil.getAnnotation(annotations, PersistenceContext.class);
        if (annot != null)
        {
            PersistenceContext pc = (PersistenceContext) annot;
            String unitName = pc.unitName();
            String name = pc.name();
            
            //X TODO what if the EntityManager is null?
            return getJPAService().getPersistenceContext(unitName, name);
        }

        return ret;
    }
    
    private Object injectForNew(Type type, Annotation... annotations)
    {
        Class<?> clazz = null;

        if (type instanceof ParameterizedType)
        {
            clazz = (Class<?>) ((ParameterizedType) type).getRawType();
        }
        else if (type instanceof Class)
        {
            clazz = (Class<?>) type;
        }

        return injectForDependent(WebBeansUtil.createNewComponent(clazz),null);
    }

    private Object injectForObservable(Type[] args, Annotation... annotations)
    {
        Class<?> eventType = (Class<?>) args[0];

        return injectForDependent(WebBeansUtil.createObservableImplicitComponent(EventImpl.class, eventType, annotations),null);
    }
    
    private <T> Object injectForObtains(Class<T> instanceType, Type[] args, Annotation...annotations)
    {   
        @SuppressWarnings("unchecked")
        Class<Instance<T>> clazz = (Class<Instance<T>>)instanceType;
        return injectForDependent(WebBeansUtil.createInstanceComponent(clazz, args[0] , annotations),null);
        
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

    protected JPAService getJPAService()
    {
        return ServiceLoader.getService(JPAService.class);
    }
}