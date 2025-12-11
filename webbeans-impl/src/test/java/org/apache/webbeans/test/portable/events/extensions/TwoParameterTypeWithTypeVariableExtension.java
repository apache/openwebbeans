/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.portable.events.extensions;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

/**
 * Extension that observes a {@link ProcessAnnotatedType} with a type that has
 * two bounded type parameters: {@code <K extends Foo, V extends Bar>}.
 */
public class TwoParameterTypeWithTypeVariableExtension implements Extension
{
    public static boolean CALLED = false;

    <K extends Foo, V extends Bar> void processClasses(@Observes ProcessAnnotatedType<KeyValueStore<K, V>> event)
    {
        CALLED = true;
    }

    public interface Foo
    {
    }

    public interface Bar
    {
    }

    public interface KeyValueStore<K extends Foo, V extends Bar>
    {
        void put(K key, V value);

        V get(K key);
    }

    public static class FooImpl implements Foo
    {
    }

    public static class BarImpl implements Bar
    {
    }

    public static class KeyValueStoreImpl implements KeyValueStore<FooImpl, BarImpl>
    {
        @Override
        public void put(FooImpl key, BarImpl value)
        {
            // no-op
        }

        @Override
        public BarImpl get(FooImpl key)
        {
            return null;
        }
    }
}

