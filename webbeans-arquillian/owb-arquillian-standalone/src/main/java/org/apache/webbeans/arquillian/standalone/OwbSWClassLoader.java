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
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class OwbSWClassLoader extends URLClassLoader implements Closeable
{
    private final List<InputStream> openedStreams = new ArrayList<InputStream>();
    private final String prefix;
    private final boolean useOnlyArchiveResources;
    private final Archive<?> archive;

    public OwbSWClassLoader(final ClassLoader parent, final Archive<?> archive, final boolean useOnlyArchiveResources)
    {
        super(new URL[0], parent);

        this.useOnlyArchiveResources = useOnlyArchiveResources;
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
        catch (final MalformedURLException e)
        {
            throw new RuntimeException("Could not create URL for archive: " + archive.getName(), e);
        }
    }

    @Override
    public URL getResource(final String name)
    {
        if (useOnlyArchiveResources)
        {
            final URL url = findResource(name);
            if (url != null)
            {
                return url;
            } // else user probably used the fact the test is embedded
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException
    {
        if (useOnlyArchiveResources)
        {
            final Enumeration<URL> urls = findResources(name);
            if (urls.hasMoreElements())
            {
                return urls;
            } // else user probably used the fact the test is embedded
        }
        return super.getResources(name);
    }

    @Override
    public URL findResource(final String name)
    {
        final Node node = findNode(name);
        if (node != null)
        {
            try
            {
                return new URL(null, "archive:" + archive.getName() + "/" + name, new ArchiveStreamHandler());
            }
            catch (final MalformedURLException e)
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
    public Enumeration<URL> findResources(final String name) throws IOException
    {
        final Node node = findNode(name);
        if (node != null)
        {
            return Collections.enumeration(Collections.singleton(new URL(null, "archive:" + archive.getName() + "/" + name, new ArchiveStreamHandler())));
        }
        if (useOnlyArchiveResources)
        {
            return Collections.emptyEnumeration();
        }

        return super.findResources(name);
    }

    private Node findNode(final String name)
    {
        ArchivePath path = ArchivePaths.create(path(prefix, name));
        Node node = archive.get(path);
        if (node == null)
        {
            path = ArchivePaths.create(name);
            node = archive.get(path);


        }
        return node;
    }

    private String path(final String... parts)
    {
        final StringBuilder builder = new StringBuilder(parts[0]);
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
            for (final InputStream stream : openedStreams)
            {
                try
                {
                    stream.close();
                }
                catch (final Exception e)
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
        protected URLConnection openConnection(final URL u) throws IOException
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
                    final ArchivePath path = convertToArchivePath(u);
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

                    final Asset asset = node.getAsset();
                    if (asset == null)
                    {
                        return null;
                    }

                    final InputStream input = asset.openStream();
                    synchronized (this)
                    {
                        openedStreams.add(input);
                    }
                    return input;

                }

                private ArchivePath convertToArchivePath(final URL url)
                {
                    return ArchivePaths.create(url.getPath().replace(archive.getName(), ""));
                }
            };
        }
    }
}
