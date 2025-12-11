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
 * Three-parameter generic with mixed variance:
 * {@code TripleStore<? extends HeadlineFoo, ?, ? extends HeadlineBar>}.
 */
public class ThreeParameterMixedVarianceExtension implements Extension
{
    public static boolean CALLED = false;

    void processClasses(@Observes ProcessAnnotatedType<TripleStore<? extends HeadlineFoo, ?, ? extends HeadlineBar>> event)
    {
        CALLED = true;
    }

    public interface Baz3
    {
    }

    public static class BazImpl3 implements Baz3
    {
    }

    public static class OtherBazImpl3 implements Baz3
    {
    }

    public interface Bar3<T extends Baz3>
    {
    }

    public static class HeadlineBar implements Bar3<BazImpl3>
    {
    }

    public static class OtherBar implements Bar3<OtherBazImpl3>
    {
    }

    public interface Foo3
    {
    }

    public static class HeadlineFoo implements Foo3
    {
    }

    public static class OtherFoo implements Foo3
    {
    }

    public interface TripleStore<A extends Foo3, B, C extends Bar3<?>>
    {
        void store(A a, B b, C c);
    }

    /**
     * Matches the observer: uses HeadlineFoo and HeadlineBar.
     */
    public static class TripleStoreImpl implements TripleStore<HeadlineFoo, String, HeadlineBar>
    {
        @Override
        public void store(HeadlineFoo a, String b, HeadlineBar c)
        {
            // no-op
        }
    }

    /**
     * Does not match the observer: uses OtherBar instead of HeadlineBar.
     */
    public static class TripleStoreWrongImpl implements TripleStore<HeadlineFoo, String, OtherBar>
    {
        @Override
        public void store(HeadlineFoo a, String b, OtherBar c)
        {
            // no-op
        }
    }
}

