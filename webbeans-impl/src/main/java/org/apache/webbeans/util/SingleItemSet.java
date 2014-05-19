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
package org.apache.webbeans.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * This is a special 'Set' which actually is no Collection but
 * only contains a very single item.
 * This way we keep mem low and also are pretty fast ;)
 */
public class SingleItemSet<T> implements Set<T>
{

    private T instance;


    public SingleItemSet(T instance)
    {
        this.instance = instance;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean contains(Object o)
    {
        return instance.equals(o);
    }

    @Override
    public Iterator<T> iterator()
    {
        return new SingleItemIterator(instance);
    }

    @Override
    public Object[] toArray()
    {
        Object[] array = new Object[1];
        array[0] = instance;
        return array;
    }

    @Override
    public <T1> T1[] toArray(T1[] a)
    {
        if (a.length > 0)
        {
            a[0] = (T1) instance;
            return a;
        }
        return (T1[]) toArray();
    }

    @Override
    public boolean add(T t)
    {
        throw new IllegalArgumentException("not supported operation");
    }

    @Override
    public boolean remove(Object o)
    {
        throw new IllegalArgumentException("not supported operation");
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        throw new IllegalArgumentException("not supported operation");
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new IllegalArgumentException("not supported operation");
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return false;
    }

    @Override
    public void clear()
    {
        instance = null;
    }

    public class SingleItemIterator implements Iterator<T>
    {
        private T instance;

        public SingleItemIterator(T instance)
        {
            this.instance = instance;
        }

        @Override
        public boolean hasNext()
        {
            return instance != null;
        }

        @Override
        public T next()
        {
            T oldInstance = instance;
            this.instance = null;
            return oldInstance;
        }

        @Override
        public void remove()
        {
            // do nothing...
        }
    }
}
