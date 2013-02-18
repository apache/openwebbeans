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

import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ClassesArchive;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// this delegate pattern is interesting
// because we will be able to add
// some exclusion config through it
// using Filter + FilterArchive
public class CdiArchive implements Archive
{
    private final Map<URL, Collection<String>> map = new HashMap<URL, Collection<String>>();
    private final Set<String> classes = new HashSet<String>();
    private final Archive delegate;

    public CdiArchive(final ClassLoader loader, final Iterable<URL> urls)
    {
        final Collection<Archive> archives = new ArrayList<Archive>();
        for (final URL url : urls)
        {
            final List<String> classes = new ArrayList<String>();
            final Archive archive = new FilteredArchive(ClasspathArchive.archive(loader, url), new Filter()
            {
                @Override
                public boolean accept(final String name)
                {
                    classes.add(name);
                    CdiArchive.this.classes.add(name);
                    return true;
                }
            });
            map.put(url, classes);
            archives.add(archive);
        }

        delegate = new CompositeArchive(archives);
    }

    public CdiArchive(final Collection<Class<?>> classList)
    {
        delegate = new FilteredArchive(new ClassesArchive(classList), new Filter()
        {
            @Override
            public boolean accept(final String name)
            {
                classes.add(name);
                return true;
            }
        });
    }

    public Set<String> getClasses()
    {
        return classes;
    }

    public Map<URL, Collection<String>> classesByUrl()
    {
        return map;
    }

    @Override
    public InputStream getBytecode(final String className) throws IOException, ClassNotFoundException
    {
        return delegate.getBytecode(className);
    }

    @Override
    public Class<?> loadClass(final String className) throws ClassNotFoundException
    {
        return delegate.loadClass(className);
    }

    @Override
    public Iterator<Entry> iterator()
    {
        return delegate.iterator();
    }
}
