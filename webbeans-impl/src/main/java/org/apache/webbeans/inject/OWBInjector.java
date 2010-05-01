/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.inject;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;

import org.apache.webbeans.component.InjectionPointBean;
import org.apache.webbeans.component.InjectionTargetWrapper;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.SecurityUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Injects dependencies of the given Java EE component
 * instance.
 * 
 * @version $Rev$ $Date$
 *
 */
public final class OWBInjector implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private CreationalContextImpl<?> ownerCreationalContext = null;
    
    private Object javaEEInstance;
    
    public OWBInjector()
    {
        
    }
    
    @SuppressWarnings("unchecked")
    public void destroy()
    {
        BeanManagerImpl beanManager = BeanManagerImpl.getManager();
        
        //Look for custom InjectionTarget
        InjectionTargetWrapper<Object> wrapper = beanManager.getInjectionTargetWrapper((Class<Object>)javaEEInstance.getClass());
        if(wrapper != null)
        {
           wrapper.dispose(javaEEInstance);
           this.javaEEInstance = null;
           this.ownerCreationalContext = null;
        }
        
        else
        {
            if(this.ownerCreationalContext != null)
            {
                this.ownerCreationalContext.release();
                this.ownerCreationalContext = null;
            }            
        }        
    }
    
    public  OWBInjector inject(Object javaEeComponentInstance) throws Exception
    {
        return inject(javaEeComponentInstance,null);
    }
    
    @SuppressWarnings("unchecked")
    public  OWBInjector inject(Object javaEeComponentInstance, CreationalContext<?> creationalContext) throws Exception
    {
        BeanManagerImpl beanManager = BeanManagerImpl.getManager();
        try
        {
            this.javaEEInstance = javaEeComponentInstance;
            if(creationalContext == null)
            {
                this.ownerCreationalContext = (CreationalContextImpl<?>) beanManager.createCreationalContext(null);   
            }

            Class<Object> injectableComponentClass = (Class<Object>)javaEeComponentInstance.getClass();
            InjectionTarget<Object> injectionTarget = null;
            
            //Look for custom InjectionTarget
            InjectionTargetWrapper<Object> wrapper = beanManager.getInjectionTargetWrapper(injectableComponentClass);
            if(wrapper != null)
            {
                wrapper.inject(javaEeComponentInstance, (CreationalContext<Object>)this.ownerCreationalContext);
                return this;
            }
            
            AnnotatedType<Object> annotated = (AnnotatedType<Object>) beanManager.createAnnotatedType(injectableComponentClass);
            injectionTarget = beanManager.createInjectionTarget(annotated);
            Set<InjectionPoint> injectionPoints = injectionTarget.getInjectionPoints();
            if(injectionPoints != null && injectionPoints.size() > 0)
            {
                for(InjectionPoint injectionPoint : injectionPoints)
                {
                    boolean injectionPointBeanSet = false;
                    try
                    {
                        if(injectionPoint.getMember() instanceof Field)
                        {
                            //Injected contextual beam
                            Bean<?> injectedBean = (Bean<?>)InjectionResolver.getInstance().getInjectionPointBean(injectionPoint);                
                            
                            if(WebBeansUtil.isDependent(injectedBean))
                            {
                                if(!InjectionPoint.class.isAssignableFrom(ClassUtil.getClass(injectionPoint.getType())))
                                {
                                    injectionPointBeanSet = true;
                                    InjectionPointBean.local.set(injectionPoint);   
                                }
                            }
                        }
                        
                        Object object = beanManager.getInjectableReference(injectionPoint, ownerCreationalContext);                    
                        
                        if(injectionPoint.getMember() instanceof Method)
                        {
                            Method method = (Method)injectionPoint.getMember();
                            ClassUtil.callInstanceMethod(method, javaEeComponentInstance, new Object[]{object});
                            
                        }
                        else if(injectionPoint.getMember() instanceof Field)
                        {
                            Field field = (Field)injectionPoint.getMember();
                            ClassUtil.setField(javaEeComponentInstance, field, object);
                        }                        
                    }finally
                    {
                        if(injectionPointBeanSet)
                        {
                            InjectionPointBean.local.remove();   
                        }  
                    }
                }
                
                return this;
            }
            
            
        }catch(Exception e)
        {
            throw e;
        }
        
        return null;
    }
    
    
    public static boolean checkInjectionPointForInterceptorPassivation(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        Field[] fields = SecurityUtil.doPrivilegedGetDeclaredFields(clazz);
        for(Field field : fields)
        {
            if(field.getAnnotation(Inject.class) != null)
            {
                Class<?> type = field.getType();
                if(!Serializable.class.isAssignableFrom(type))
                {
                    return false;
                }
            }
        }
        
        return true;
    }
}
