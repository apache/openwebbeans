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
package org.apache.webbeans.proxy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;

import org.apache.webbeans.annotation.WebBeansAnnotation;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.DependentScopedBeanInterceptorHandler;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.intercept.NormalScopedBeanInterceptorHandler;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.util.ClassUtil;

public final class JavassistProxyFactory
{
    private JavassistProxyFactory()
    {

    }
    
    private static Map<OwbBean<?>, Class<?>> normalScopedBeanProxyClasses = new ConcurrentHashMap<OwbBean<?>, Class<?>>();
    
    private static Map<OwbBean<?>, Class<?>> dependentScopedBeanProxyClasses = new ConcurrentHashMap<OwbBean<?>, Class<?>>();

    public static Object createNormalScopedBeanProxy(OwbBean<?> bean, CreationalContext<?> creationalContext)
    {
        Object result = null;
        try
        {
            Class<?> proxyClass = normalScopedBeanProxyClasses.get(bean);
            if (proxyClass == null)
            {
                ProxyFactory fact = createProxyFactory(bean);

                proxyClass = fact.createClass();
                normalScopedBeanProxyClasses.put(bean, proxyClass);
            }
            
            result = proxyClass.newInstance();
            
            if (!(bean instanceof WebBeansDecorator<?>) && !(bean instanceof WebBeansInterceptor<?>))
            {
                ((ProxyObject)result).setHandler(new NormalScopedBeanInterceptorHandler(bean, creationalContext));
            }
        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }

        return result;
    }
    
    public static Object createDependentScopedBeanProxy(OwbBean<?> bean, Object actualInstance, CreationalContext<?> creastionalContext)
    {
        Object result = null;
        
        List<InterceptorData> interceptors =  null;
        List<Decorator<?>> decorators = null;
        InjectionTargetBean<?> injectionTargetBean = null;
        if(bean instanceof InjectionTargetBean<?>)
        {
            injectionTargetBean = (InjectionTargetBean<?>)bean;
            interceptors = injectionTargetBean.getInterceptorStack();
            decorators = injectionTargetBean.getDecoratorStack();
        }
        
        if(interceptors == null && decorators == null)
        {
            return actualInstance;
        }
        
        boolean notInInterceptorClassAndLifecycle = false;
        if(interceptors != null)
        {
            Iterator<InterceptorData> its = interceptors.iterator();
            while(its.hasNext())
            {
                InterceptorData id = its.next();
                if(!id.isDefinedInInterceptorClass() && id.isLifecycleInterceptor())
                {
                    continue;
                }
                notInInterceptorClassAndLifecycle = true;
                break;
            }
        }
        
        //No need to return proxy
        if(!notInInterceptorClassAndLifecycle && decorators.isEmpty())
        {
            return actualInstance;
        }
        
        try
        {
            Class<?> proxyClass = dependentScopedBeanProxyClasses.get(bean);
            if (proxyClass == null)
            {
                ProxyFactory fact = createProxyFactory(bean);
                proxyClass = fact.createClass();
                dependentScopedBeanProxyClasses.put(bean, proxyClass);
            }
            
            result = proxyClass.newInstance();
            if (!(bean instanceof WebBeansDecorator<?>) && !(bean instanceof WebBeansInterceptor<?>))
            {
                ((ProxyObject)result).setHandler(new DependentScopedBeanInterceptorHandler(bean, actualInstance, creastionalContext));
            }

        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }

        return result;
    }
    

    public static ProxyFactory createProxyFactory(Bean<?> bean) throws Exception
    {
        Set<Type> types = bean.getTypes();
        Set<Class<?>> interfaceList = new HashSet<Class<?>>();
        Class<?> superClass = null;
        for (Type generic : types)
        {
            Class<?> type = ClassUtil.getClazz(generic);
            
            if (type.isInterface())
            {
                interfaceList.add(type);
            }
            
            else if ((superClass == null) || (superClass.isAssignableFrom(type) && type != Object.class))
            {
                superClass = type;
            }

        }
        if (!interfaceList.contains(Serializable.class))
        {
            interfaceList.add(Serializable.class);
        }

        Class<?>[] interfaceArray = new Class<?>[interfaceList.size()];
        interfaceArray = interfaceList.toArray(interfaceArray);

        ProxyFactory fact = new ProxyFactory();
        fact.setInterfaces(interfaceArray);
        fact.setSuperclass(superClass);

        return fact;
        
    }
    
    public static WebBeansAnnotation createNewAnnotationProxy(Class<? extends Annotation> annotationType)
    {
        WebBeansAnnotation result = null;

        try
        {
            ProxyFactory pf = new ProxyFactory();
            pf.setInterfaces(new Class<?>[] { annotationType, Annotation.class });
            pf.setSuperclass(WebBeansAnnotation.class);
            pf.setHandler(new WebBeansAnnotation(annotationType));

            result = (WebBeansAnnotation) pf.create(new Class[] { Class.class }, new Object[] { annotationType });

        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }

        return result;
    }
    
    /**
     * @param o the object to check
     * @return <code>true</code> if the given object is a proxy
     */
    public static boolean isProxyInstance(Object o)
    {
        return o instanceof ProxyObject;
    }

}
