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
package org.apache.webbeans.portable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.AnnotatedTypeWrapper;
import org.junit.Test;

public class AnnotatedTypeImplTest
{
    @Test
    public void equalsEvaluation()
    {
        final AnnotatedType<Foo> annotatedType = new AnnotatedTypeImpl<>(
                new WebBeansContext(), Foo.class, null);
        assertEquals(annotatedType, annotatedType); // obvious but does not cost much and is required

        final AnnotatedType<Foo> wrapped = new AnnotatedTypeWrapper<>(null, annotatedType, "");
        assertEquals(annotatedType, wrapped);
        assertEquals(wrapped, annotatedType);

        final AnnotatedType<AnnotatedTypeImplTest> anotherAnnotatedType = new AnnotatedTypeImpl<>(
                new WebBeansContext(), AnnotatedTypeImplTest.class, null);
        assertNotEquals(wrapped, anotherAnnotatedType);
        assertNotEquals(anotherAnnotatedType, wrapped);
    }

    @Test
    public void hashCodeComputation()
    {
        final AnnotatedType<Foo> annotatedType = new AnnotatedTypeImpl<>(
                new WebBeansContext(), Foo.class, null);
        assertEquals(annotatedType.hashCode(), annotatedType.hashCode());

        final AnnotatedType<Foo> wrapped = new AnnotatedTypeWrapper<>(null, annotatedType, "");
        assertEquals(annotatedType.hashCode(), wrapped.hashCode());
    }

    public static class Foo
    {
    }
}
