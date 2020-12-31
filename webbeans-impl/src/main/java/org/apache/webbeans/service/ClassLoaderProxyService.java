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
package org.apache.webbeans.service;

import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.DefiningClassService;

public class ClassLoaderProxyService implements DefiningClassService
{
    private final ProxiesClassLoader loader;

    public ClassLoaderProxyService(final WebBeansContext context)
    {
        this.loader = new ProxiesClassLoader(
                context,
                Boolean.parseBoolean(context.getOpenWebBeansConfiguration()
                        .getProperty(getClass().getName() + ".skipPackages")));
    }

    protected ClassLoaderProxyService(final ProxiesClassLoader loader)
    {
        this.loader = loader;
    }

    @Override
    public ClassLoader getProxyClassLoader(final Class<?> forClass)
    {
        return loader;
    }

    @Override
    public <T> Class<T> defineAndLoad(final String name, final byte[] bytecode, final Class<T> proxiedClass)
    {
        return (Class<T>) loader.getOrRegister(
                name, bytecode, proxiedClass.getPackage(), proxiedClass.getProtectionDomain());
    }

    // for build tools - @Experimental
    public static class Spy extends ClassLoaderProxyService
    {
        private final Map<String, byte[]> proxies = new HashMap<>();

        public Spy(final WebBeansContext context)
        {
            super(new ProxiesClassLoader(context, true));
        }

        public Map<String, byte[]> getProxies()
        {
            return proxies;
        }

        @Override
        public <T> Class<T> defineAndLoad(final String name, final byte[] bytecode, final Class<T> proxiedClass)
        {
            proxies.put(name, bytecode);
            return super.defineAndLoad(name, bytecode, proxiedClass);
        }
    }

    // runtime companion of Spy - @Experimental
    public static class LoadFirst extends ClassLoaderProxyService
    {
        public LoadFirst(final WebBeansContext context)
        {
            super(context);
        }

        @Override
        public <T> Class<T> defineAndLoad(final String name, final byte[] bytecode, final Class<T> proxiedClass)
        {
            ClassLoader proxyClassLoader = getProxyClassLoader(proxiedClass);
            if (proxyClassLoader == null)
            {
                proxyClassLoader = Thread.currentThread().getContextClassLoader();
            }
            try
            {
                return (Class<T>) proxyClassLoader.loadClass(name);
            }
            catch (final ClassNotFoundException e)
            {
                WebBeansLoggerFacade.getLogger(getClass()).warning(e.getMessage());
                return super.defineAndLoad(name, bytecode, proxiedClass);
            }
        }
    }

    // strict load only impl, it changes LoadFirst by not creating a classloader at all (nice in graalvm) -@Experimental
    public static class LoadOnly implements DefiningClassService
    {
        @Override
        public ClassLoader getProxyClassLoader(final Class<?> forClass)
        {
            return Thread.currentThread().getContextClassLoader();
        }

        @Override
        public <T> Class<T> defineAndLoad(final String name, final byte[] bytecode, final Class<T> proxiedClass)
        {
            try
            {
                return (Class<T>) getProxyClassLoader(null).loadClass(name);
            }
            catch (final ClassNotFoundException e)
            {
                WebBeansLoggerFacade.getLogger(getClass()).warning(e.getMessage());
                throw new WebBeansException(e);
            }
        }
    }

    private static class ProxiesClassLoader extends ClassLoader
    {
        private final boolean skipPackages;
        private final ConcurrentMap<String, Class<?>> classes = new ConcurrentHashMap<>();

        private ProxiesClassLoader(final WebBeansContext context, boolean skipPackages)
        {
            super(context.getApplicationBoundaryService().getApplicationClassLoader());
            this.skipPackages = skipPackages;
        }


        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException
        {
            final Class<?> clazz = classes.get(name);
            if (clazz == null)
            {
                return getParent().loadClass(name);
            }
            return clazz;
        }

        private Class<?> getOrRegister(final String proxyClassName, final byte[] proxyBytes,
                                       final Package pck, final ProtectionDomain protectionDomain)
        {
            final String key = proxyClassName.replace('/', '.');
            Class<?> existing = classes.get(key);
            if (existing == null)
            {
                synchronized (this)
                {
                    existing = classes.get(key);
                    if (existing == null)
                    {
                        if (!skipPackages)
                        {
                            definePackageFor(pck, protectionDomain);
                        }
                        existing = super.defineClass(proxyClassName, proxyBytes, 0, proxyBytes.length);
                        resolveClass(existing);
                        classes.put(key, existing);
                    }
                }
            }
            return existing;
        }

        private void definePackageFor(final Package model, final ProtectionDomain protectionDomain)
        {
            if (model == null)
            {
                return;
            }
            if (getPackage(model.getName()) == null)
            {
                if (model.isSealed() && protectionDomain != null &&
                        protectionDomain.getCodeSource() != null &&
                        protectionDomain.getCodeSource().getLocation() != null)
                {
                    definePackage(
                            model.getName(),
                            model.getSpecificationTitle(),
                            model.getSpecificationVersion(),
                            model.getSpecificationVendor(),
                            model.getImplementationTitle(),
                            model.getImplementationVersion(),
                            model.getImplementationVendor(),
                            protectionDomain.getCodeSource().getLocation());
                }
                else
                {
                    definePackage(
                            model.getName(),
                            model.getSpecificationTitle(),
                            model.getSpecificationVersion(),
                            model.getSpecificationVendor(),
                            model.getImplementationTitle(),
                            model.getImplementationVersion(),
                            model.getImplementationVendor(),
                            null);
                }
            }
        }
    }
}
