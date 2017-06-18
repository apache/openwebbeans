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
package org.apache.webbeans.arquillian.standalone;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OwbSWClassLoader extends URLClassLoader
{
    // Collections.emptyEnumeration only exists in java7++
    private static final Enumeration EMPTY_ENUMERATION = new Enumeration()
    {
        @Override
        public boolean hasMoreElements()
        {
            return false;
        }

        @Override
        public Object nextElement()
        {
            return null;
        }
    };

    private final List<InputStream> openedStreams = new ArrayList<>();
    private final String prefix;
    private final boolean useOnlyArchiveResources;
    private final Archive<?> archive;
    private final Collection<String> useOnlyArchiveResourcesExcludes;

    public OwbSWClassLoader(ClassLoader parent, Archive<?> archive, boolean useOnlyArchiveResources, Collection<String> useOnlyArchiveResourcesExcludes)
    {
        super(new URL[0], parent);

        this.useOnlyArchiveResources = useOnlyArchiveResources;
        this.useOnlyArchiveResourcesExcludes = useOnlyArchiveResourcesExcludes;
        this.archive = archive;

        if (WebArchive.class.isInstance(archive))
        {
            prefix = "/WEB-INF/classes";
        }
        else
        {
            prefix = "";
        }

        try
        { // add it to find classes if used this way
            addURL(new URL(null, "archive:" + archive.getName() + "/", new ArchiveStreamHandler()));
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException("Could not create URL for archive: " + archive.getName(), e);
        }
    }

    @Override
    public URL getResource(String name)
    {
        if (useOnlyArchiveResources)
        {
            URL url = findResource(name);
            if (url != null)
            {
                return url;
            } // else user probably used the fact the test is embedded
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        if (useOnlyArchiveResources)
        {
            Enumeration<URL> urls = findResources(name);
            if (useOnlyArchiveResourcesExcludes.contains(name))
            {
                Collection<URL> returnValue = new ArrayList<>(Collections.list(urls));
                returnValue.addAll(Collections.list(super.getResources(name)));
                return Collections.enumeration(returnValue);
            }
            else if (urls.hasMoreElements())
            {
                return urls;
            } // else user probably used the fact the test is embedded
        }
        return super.getResources(name);
    }

    @Override
    public URL findResource(String name)
    {
        Set<String> nodes = findNodes(archive, name);
        if (!nodes.isEmpty())
        {
            try
            {
                return new URL(null, "archive:" + nodes.iterator().next());
            }
            catch (MalformedURLException e)
            {
                // no-op: let reuse parent method
            }
        }
        if (useOnlyArchiveResources)
        {
            return null;
        }
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException
    {
        Set<String> nodes = findNodes(archive, name);
        List<URL> urls = new ArrayList<>(nodes.size());
        for (String node : nodes)
        {
            urls.add(new URL(null, "archive:" + node, new ArchiveStreamHandler()));
        }

        if (!useOnlyArchiveResources)
        {
            Enumeration<URL> parentResources = getParent().getResources(name);
            while (parentResources.hasMoreElements())
            {
                urls.add(parentResources.nextElement());
            }
        }

        return Collections.enumeration(urls);
    }

    private Set<String> findNodes(Archive arch, String name)
    {
        Set<String> nodes = new HashSet<>();

        if (arch instanceof WebArchive)
        {
            // first check WEB-INF/classes
            ArchivePath path = ArchivePaths.create(path(prefix, name));
            Node node = arch.get(path);
            if (node != null)
            {
                nodes.add(path.get());
            }

            Map<ArchivePath, Node> jarLibs = ((WebArchive) arch).getContent(Filters.include("/WEB-INF/lib/.*\\.jar"));
            for (Node jarLib : jarLibs.values())
            {
                if (jarLib.getAsset() instanceof ArchiveAsset && ((ArchiveAsset) jarLib.getAsset()).getArchive() instanceof JavaArchive)
                {
                    Set<String> jarNodes = findNodes(((ArchiveAsset) jarLib.getAsset()).getArchive(), name);
                    for (String jarNode : jarNodes)
                    {
                        path = ArchivePaths.create(path("WEB-INF/lib", ((ArchiveAsset) jarLib.getAsset()).getArchive().getName(), jarNode));
                        nodes.add(path.get());
                    }

                }
            }
        }
        else
        {
            ArchivePath path = ArchivePaths.create(name);
            Node node = arch.get(path);
            if (node != null)
            {
                nodes.add(path.get());
            }
        }

        return nodes;
    }

    private String path(String... parts)
    {
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++)
        {
            if (!parts[i - 1].endsWith("/") && !parts[i].startsWith("/"))
            {
                builder.append("/");
            }
            builder.append(parts[i]);
        }
        return builder.toString();
    }


    public void close() throws IOException
    {
        synchronized (this)
        {
            for (InputStream stream : openedStreams)
            {
                try
                {
                    stream.close();
                }
                catch (Exception e)
                {
                    // no-op
                }
            }
            openedStreams.clear();
        }
    }

    protected class ArchiveStreamHandler extends URLStreamHandler
    {
        @Override
        protected URLConnection openConnection(URL u) throws IOException
        {
            return new URLConnection(u)
            {
                @Override
                public void connect() throws IOException
                {
                    // no-op
                }

                @Override
                public InputStream getInputStream() throws IOException
                {
                    ArchivePath path = convertToArchivePath(u);
                    Node node = archive.get(prefix + path.get());
                    if (node == null && !prefix.isEmpty())
                    { // WEB-INF/lib/x.jar!*
                        node = archive.get(path);
                    }

                    // SHRINKWRAP-308
                    if (node == null)
                    {
                        throw new FileNotFoundException("Requested path: " + path + " does not exist in " + archive.toString());
                    }

                    Asset asset = node.getAsset();
                    if (asset == null)
                    {
                        return null;
                    }

                    InputStream input = asset.openStream();
                    synchronized (this)
                    {
                        openedStreams.add(input);
                    }
                    return input;

                }

                private ArchivePath convertToArchivePath(URL url)
                {
                    return ArchivePaths.create(url.getPath().replace(archive.getName(), ""));
                }
            };
        }
    }
}
