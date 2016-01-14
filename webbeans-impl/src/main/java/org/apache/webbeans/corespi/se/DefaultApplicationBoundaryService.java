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
package org.apache.webbeans.corespi.se;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.spi.ApplicationBoundaryService;

/**
 * Really simple default impl of the ApplicationBoundaryService.
 * Assumes that there is a pretty easy ClassLoader structure in place.
 * If a proxy should get created for a class further outside of the
 * {@link #applicationClassLoader} then the {@link #applicationClassLoader}
 * itself will get used to prevent mem leaks.
 */
public class DefaultApplicationBoundaryService implements ApplicationBoundaryService, Closeable
{
    /**
     * The outermost ClassLoader of the appliction. E.g. the EAR classloader
     */
    private ClassLoader applicationClassLoader;

    /**
     * All ClassLoaders further outside of the {@link #applicationClassLoader}.
     */
    private Set<ClassLoader> parentClassLoaders;

    public DefaultApplicationBoundaryService()
    {
        init();
    }

    protected void init()
    {
        applicationClassLoader = BeanManagerImpl.class.getClassLoader();
        parentClassLoaders = new HashSet<ClassLoader>();
        ClassLoader cl = applicationClassLoader;
        while (cl.getParent() != null)
        {
            cl = cl.getParent();
            parentClassLoaders.add(cl);
        }

    }

    @Override
    public ClassLoader getApplicationClassLoader()
    {
        return applicationClassLoader;
    }

    @Override
    public ClassLoader getBoundaryClassLoader(Class classToProxy)
    {
        ClassLoader appCl = getApplicationClassLoader();

        ClassLoader classToProxyCl = classToProxy.getClassLoader();

        if (classToProxyCl == null)
        {
            // Some JVMs return null for the bootstrap CL
            return appCl;
        }

        if (classToProxyCl == appCl)
        {
            // this mainly happens in a 'flat CL' environment
            return appCl;
        }

        if (isOutsideOfApplicationClassLoader(classToProxyCl))
        {
            // note: this logic only works if you have a hieararchic ClassLoader scenario
            // It doesn't work for e.g. OSGi environments where the CLs are spread out
            // with very limited visibility
            return appCl;
        }

        return classToProxyCl;
    }

    protected boolean isOutsideOfApplicationClassLoader(ClassLoader classToProxyCl)
    {

        return parentClassLoaders.contains(classToProxyCl);
    }

    @Override
    public void close() throws IOException
    {
        // we store ClassLoaders so we MUST free them at shutdown!
        parentClassLoaders.clear();
    }
}
