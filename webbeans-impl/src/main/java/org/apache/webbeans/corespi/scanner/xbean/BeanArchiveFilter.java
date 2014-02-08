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
package org.apache.webbeans.corespi.scanner.xbean;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.spi.BeanArchiveService.BeanArchiveInformation;
import org.apache.webbeans.spi.BeanArchiveService.BeanDiscoveryMode;
import org.apache.xbean.finder.filter.Filter;

/**
 * Filter which knows about BeanArchive scan modes
 */
public class BeanArchiveFilter implements Filter
{
    private final ClassLoader loader;
    private final BeanManagerImpl beanManager;
    private final BeanArchiveInformation beanArchiveInfo;
    private final boolean scanAll;
    private final boolean scanNone;
    private final boolean scanAnnotated;

    private List<String> urlClasses;
    private Set<String> allClasses;

    public BeanArchiveFilter(ClassLoader loader, BeanManagerImpl beanManager, BeanArchiveInformation beanArchiveInfo, List<String> urlClasses, Set<String> allClasses)
    {
        this.loader = loader;
        this.beanManager = beanManager;
        this.beanArchiveInfo = beanArchiveInfo;
        this.urlClasses = urlClasses;
        this.allClasses = allClasses;
        BeanDiscoveryMode discoveryMode = beanArchiveInfo.getBeanDiscoveryMode();

        scanAll = BeanDiscoveryMode.ALL.equals(discoveryMode);
        scanNone = BeanDiscoveryMode.NONE.equals(discoveryMode);
        scanAnnotated = BeanDiscoveryMode.ANNOTATED.equals(discoveryMode);
    }

    @Override
    public boolean accept(String name)
    {
        if (scanNone)
        {
            return false;
        }

        if (beanArchiveInfo.isClassExcluded(name))
        {
            return false;
        }

        if (scanAll)
        {
            urlClasses.add(name);
            allClasses.add(name);
            return true;
        }

        if (scanAnnotated)
        {
            try
            {
                Class clazz = Class.forName(name, false, loader);
                if (!hasScopeAnnotation(clazz.getAnnotations()))
                {
                    return false;
                }
            }
            catch (ClassNotFoundException cnfe)
            {
                // not a problem, just ignore this class
                return false;
            }
            catch (NoClassDefFoundError ncdf)
            {
                // not a problem, just ignore this class
                return false;
            }

            urlClasses.add(name);
            allClasses.add(name);
            return true;
        }

        return false;
    }

    private boolean hasScopeAnnotation(Annotation[] annotations)
    {
        for (Annotation annotation : annotations)
        {
            if (beanManager.isScope(annotation.annotationType()))
            {
                return true;
            }
        }
        return false;
    }
}
