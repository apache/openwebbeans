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
package org.apache.webbeans.proxy;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.decorator.DelegateHandler;
import org.apache.webbeans.proxy.javassist.JavassistFactory;
import org.apache.webbeans.util.ClassUtil;



/**
 *  TODO remove old proxy handling. Only InterceptorDecoratorProxyFactory and NormalScopeProxyFactory shall remain.
 */
public final class ProxyFactory
{

    private ConcurrentMap<OwbBean<?>, Class<?>> normalScopedBeanProxyClassesRemove = new ConcurrentHashMap<OwbBean<?>, Class<?>>();
    private ConcurrentMap<OwbBean<?>, Class<?>> dependentScopedBeanProxyClassesRemove = new ConcurrentHashMap<OwbBean<?>, Class<?>>();
    private ConcurrentMap<OwbBean<?>, Class<?>> interceptorProxyClassesRemove = new ConcurrentHashMap<OwbBean<?>, Class<?>>();

    // second level map is indexed on local interface
    private ConcurrentMap<OwbBean<?>, ConcurrentMap<Class<?>, Class<?>>> ejbProxyClasses = new ConcurrentHashMap<OwbBean<?>, ConcurrentMap<Class<?>, Class<?>>>();
    private Factory factoryRemove = new JavassistFactory();


    public ProxyFactory(WebBeansContext webBeansContext)
    {
    }


    public void setHandler(Object proxy, MethodHandler handler)
    {
        factoryRemove.setHandler(proxy, handler);
    }


    private Map<OwbBean<?>, Class<?>> getInterceptorProxyClasses()
    {
        return interceptorProxyClassesRemove;
    }

    public void clear()
    {
        normalScopedBeanProxyClassesRemove.clear();
        dependentScopedBeanProxyClassesRemove.clear();
        interceptorProxyClassesRemove.clear();
        ejbProxyClasses.clear();
    }

    /**
     * TODO rework! Still uses old proxy
     */
    public Object createDecoratorDelegate(OwbBean<?> bean, DelegateHandler newDelegateHandler)
        throws Exception
    {

        Class<?> proxyClass = this.getInterceptorProxyClasses().get(bean);
        if (proxyClass == null)
        {
            proxyClass = createProxyClassRemove(bean);
            this.getInterceptorProxyClasses().put(bean, proxyClass);
        }

        final Object delegate = createProxyRemove(proxyClass);
        setHandler(delegate, newDelegateHandler);
        return delegate;
    }


    /**
     * @deprecated uses old proxy
     */
    private Object createProxyRemove(Class<?> proxyClass)
        throws InstantiationException, IllegalAccessException
    {
        return factoryRemove.createProxy(proxyClass);
    }

    /**
     * @deprecated uses old proxy. And will be obsolete anyway...
     */
    public  Object createDependentScopedBeanProxyRemove(OwbBean<?> bean, Object actualInstance, CreationalContext<?> creastionalContext)
    {

        throw new RuntimeException("Bloody Mary! this must not get used anymore!");

    }

    /**
     * @deprecated uses old proxy
     */
    private Class<?> createProxyClassRemove(OwbBean<?> bean)
    {
        final ProxyInfoRemove info = getProxyInfo(bean);
        return factoryRemove.getProxyClass(info.getSuperClass(), info.getInterfaces());
    }

    public Class<?> createAbstractDecoratorProxyClass(OwbBean<?> bean)
    {
        return createProxyClassRemove(bean);
    }


    /**
     * @deprecated uses old proxy
     */
    private static class ProxyInfoRemove
    {
        private final Class<?> superClass;
        private final Class<?>[] interfaces;

        private ProxyInfoRemove(Class<?> superClass, Class<?>[] interfaces)
        {
            this.superClass = superClass;
            this.interfaces = interfaces;
        }

        public Class<?> getSuperClass()
        {
            return superClass;
        }

        public Class<?>[] getInterfaces()
        {
            return interfaces;
        }
    }

    /**
     * @deprecated uses old proxy
     */
    private static ProxyInfoRemove getProxyInfo(Bean<?> bean)
    {
        final Set<Class<?>> interfaceList = new HashSet<Class<?>>();
        Class<?> superClass = null;
        for (Type generic : bean.getTypes())
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

        return new ProxyInfoRemove(superClass, interfaceArray);
    }
}
