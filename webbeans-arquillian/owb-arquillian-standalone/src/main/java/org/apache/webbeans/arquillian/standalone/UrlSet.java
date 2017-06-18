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

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A {@link java.util.Set} which only takes the externalForm
 * as key instead the very expensive hashCode.
 * This is a copy of the impl from openwebbeans-impl since
 * this Arquillian adaptor also runs with older OWB versions
 * which do not yet have this class.
 */
public class UrlSet implements Set<URL>
{
    private Map<String, URL> urlMap = new HashMap<>();


    @Override
    public boolean add(URL url)
    {
        return urlMap.put(url.toExternalForm(), url) == null;
    }

    @Override
    public int size()
    {
        return urlMap.size();
    }

    @Override
    public boolean isEmpty()
    {
        return urlMap.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        if (o instanceof URL)
        {
            return urlMap.containsKey(((URL) o).toExternalForm());
        }

        return false;
    }

    @Override
    public Iterator<URL> iterator()
    {
        return urlMap.values().iterator();
    }

    @Override
    public Object[] toArray()
    {
        return urlMap.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return urlMap.values().toArray(a);
    }

    @Override
    public boolean remove(Object o)
    {
        if (o instanceof URL)
        {
            return urlMap.remove(((URL) o).toExternalForm()) != null;
        }

        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        // not implemented
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends URL> c)
    {
        // not implemented
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        // not implemented
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        // not implemented
        return false;
    }

    @Override
    public void clear()
    {
        urlMap.clear();
    }
}
