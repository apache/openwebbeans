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
package org.apache.openwebbeans.se;

import org.apache.webbeans.corespi.scanner.AbstractMetaDataDiscovery;
import org.apache.webbeans.corespi.scanner.xbean.OwbAnnotationFinder;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.apache.xbean.finder.archive.FileArchive;
import org.apache.xbean.finder.archive.FilteredArchive;
import org.apache.xbean.finder.archive.JarArchive;
import org.apache.xbean.finder.filter.Filter;
import org.apache.xbean.finder.util.Files;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;

public class CDISeScannerService extends AbstractMetaDataDiscovery
{
    private boolean autoScanning = true;
    private final Collection<Class<?>> classes = new ArrayList<>();

    public OwbAnnotationFinder getFinder()
    {
        return finder;
    }

    public void loader(ClassLoader loader)
    {
        this.loader = loader;
    }

    public void classes(Class<?>[] classes)
    {
        this.classes.addAll(asList(classes));
    }

    public void packages(boolean recursive, Class<?>[] markers)
    {
        Stream.of(markers)
                .forEach(c -> this.addPackages(recursive, c.getName().replace('.', '/') + ".class", c.getPackage().getName()));
    }

    public void packages(boolean recursive, Package[] packages)
    {
        Stream.of(packages)
                .forEach(p -> this.addPackages(recursive, p.getName().replace('.', '/'), p.getName()));
    }

    public void disableAutoScanning()
    {
        autoScanning = false;
    }

    @Override
    protected void configure()
    {
        if (autoScanning)
        {
            registerBeanArchives(loader);
        }

        if (!classes.isEmpty())
        {
            addClassesDeploymentUrl();
        }
    }

    protected void addClassesDeploymentUrl()
    {
        try
        {
            addDeploymentUrl(CDISeBeanArchiveService.EMBEDDED_URL, new URL("openwebbeans", null, 0, "cdise", new URLStreamHandler()
            {
                @Override
                protected URLConnection openConnection(URL u) throws IOException
                {
                    return null;
                }
            }));
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException(e); // quite unlikely
        }
    }

    public Collection<Class<?>> configuredClasses()
    {
        return classes;
    }

    @Override
    protected Archive getAdditionalArchive()
    {
        return classes.isEmpty() ? null : new ClassesArchive(classes);
    }

    // not sure why it is in the spec, no way to make it portable
    private void addPackages(boolean recursive, String resource, String pack)
    {
        try
        {
            Enumeration<URL> urls = loader.getResources(resource);
            if (!urls.hasMoreElements())
            {
                throw new IllegalArgumentException("No matching jar for '" + resource + "'");
            }
            while (urls.hasMoreElements())
            {
                URL url = urls.nextElement();
                CaptureClasses capturedClasses = new CaptureClasses(pack, classes, recursive, loader);
                switch (url.getProtocol())
                {
                    case "jar":
                        consume(new FilteredArchive(new JarArchive(loader, url), capturedClasses));
                        break;
                    case "file":
                        File file = Files.toFile(url);
                        if (!file.exists())
                        {
                            throw new IllegalArgumentException(file + " doesn't exist (from url" + url + ")");
                        }

                        for (int i = 0; i < pack.chars().filter(c -> c == '.').count(); i++)
                        {
                            file = file.getParentFile();
                        }
                        if (resource.contains("/"))
                        {
                            file = file.getParentFile();
                        }
                        if (resource.endsWith(".class"))
                        {
                            file = file.getParentFile();
                        }

                        consume(new FilteredArchive(new FileArchive(loader, file), capturedClasses));
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported resource: " + url + " for resource '" + resource + "'");
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    private void consume(FilteredArchive entries)
    {
        StreamSupport.stream(entries.spliterator(), false).forEach(e ->
        {
        });
    }

    private static final class CaptureClasses implements Filter
    {
        private final Collection<Class<?>> classes;
        private final String prefix;
        private final boolean recursive;
        private final long prefixSegments;
        private final ClassLoader loader;

        private CaptureClasses(String prefix, Collection<Class<?>> classes, boolean recursive, ClassLoader loader)
        {
            this.prefix = prefix == null ? "" : prefix;
            this.prefixSegments = this.prefix.chars().filter(c -> c == '.').count();
            this.classes = classes;
            this.recursive = recursive;
            this.loader = loader;
        }

        @Override
        public boolean accept(String name)
        {
            boolean accepts = name.startsWith(prefix) && (recursive || name.chars().filter(c -> c == '.').count() == prefixSegments + 1);
            if (accepts)
            {
                try
                {
                    classes.add(loader.loadClass(name));
                }
                catch (ClassNotFoundException e)
                {
                    logger.warning(e.getMessage());
                }
            }
            return accepts;
        }
    }
}
