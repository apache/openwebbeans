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

import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.BeanArchiveService.BeanArchiveInformation;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ClasspathArchive;
import org.apache.xbean.finder.archive.CompositeArchive;
import org.apache.xbean.finder.archive.FilteredArchive;
import org.apache.xbean.finder.filter.Filter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * this delegate pattern is interesting
 * because we will be able to add
 * some exclusion config through it
 * using Filter + FilterArchive
 */
public class CdiArchive implements Archive
{
    /**
     * key: URL#toExternalForm of the scanned classpath entry
     * value: small data container with URL and class names
     */
    private final Map<String, FoundClasses> classesByUrl = new HashMap<>();

    private final Archive delegate;

    public CdiArchive(BeanArchiveService beanArchiveService, ClassLoader loader, Map<String, URL> urls,
                      Filter userFilter, Archive customArchive)
    {
        Collection<Archive> archives = new ArrayList<>();
        boolean customAdded = false;
        for (URL url : urls.values())
        {
            List<String> urlClasses = new ArrayList<>();

            BeanArchiveInformation beanArchiveInfo = beanArchiveService.getBeanArchiveInformation(url);
            final boolean custom = "openwebbeans".equals(url.getProtocol());
            Archive archive = new FilteredArchive(
                    custom ? customArchive : ClasspathArchive.archive(loader, url),
                    new BeanArchiveFilter(beanArchiveInfo, urlClasses, userFilter));
            if (!customAdded && custom)
            {
                customAdded = true;
            }

            classesByUrl.put(url.toExternalForm(), new FoundClasses(url, urlClasses, beanArchiveInfo));
            archives.add(archive);
        }
        if (!customAdded && customArchive != null)
        {
            archives.add(userFilter != null ? new FilteredArchive(customArchive, userFilter) : customArchive);
        }
        delegate = new CompositeArchive(archives);
    }

    public Map<String, FoundClasses> classesByUrl()
    {
        return classesByUrl;
    }

    @Override
    public InputStream getBytecode(String className) throws IOException, ClassNotFoundException
    {
        return delegate.getBytecode(className);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        return delegate.loadClass(className);
    }

    @Override
    public Iterator<Entry> iterator()
    {
        return delegate.iterator();
    }

    public static final class FoundClasses
    {
        private URL url;
        private Collection<String> classNames;
        private BeanArchiveInformation beanArchiveInfo;

        public FoundClasses(URL url, Collection<String> classNames, BeanArchiveInformation beanArchiveInfo)
        {
            this.url = url;
            this.classNames = classNames;
            this.beanArchiveInfo = beanArchiveInfo;
        }

        public URL getUrl()
        {
            return url;
        }

        public BeanArchiveInformation getBeanArchiveInfo()
        {
            return beanArchiveInfo;
        }

        public Collection<String> getClassNames()
        {
            return classNames;
        }
    }
}
