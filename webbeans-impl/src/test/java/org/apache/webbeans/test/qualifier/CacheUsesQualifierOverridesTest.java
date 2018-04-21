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
package org.apache.webbeans.test.qualifier;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

import org.apache.webbeans.config.OwbParametrizedTypeImpl;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class CacheUsesQualifierOverridesTest extends AbstractUnitTest
{
    @Test
    public void run()
    {
        addExtension(new Extension() {
            void changeQualifier(@Observes final BeforeBeanDiscovery beforeBeanDiscovery) {
                beforeBeanDiscovery.configureQualifier(TheQualifier.class)
                        .methods().forEach(m -> m.remove(it -> it.annotationType() == Nonbinding.class));
            }
        });
        startContainer(Impl1.class, Impl2.class);
        final OwbParametrizedTypeImpl type = new OwbParametrizedTypeImpl(null, Supplier.class, String.class);
        final Supplier<String> uno = getInstance(type, new TheQualifier.Literal("uno"));
        final Supplier<String> due = getInstance(type, new TheQualifier.Literal("due"));
        assertEquals("1", uno.get());
        assertEquals("2", due.get());
        // redundant but this is the real test here, previous are just nicer to read in the output
        assertNotEquals(uno.getClass(), due.getClass());
    }

    @Dependent
    @TheQualifier("uno")
    public static class Impl1 implements Supplier<String>
    {
        @Override
        public String get()
        {
            return "1";
        }
    }

    @Dependent
    @TheQualifier("due")
    public static class Impl2 implements Supplier<String>
    {
        @Override
        public String get()
        {
            return "2";
        }
    }

    @Qualifier
    @Target({FIELD, TYPE})
    @Retention(RUNTIME)
    public @interface TheQualifier
    {
        @Nonbinding
        String value();

        class Literal extends AnnotationLiteral<TheQualifier> implements TheQualifier
        {
            private final String value;

            public Literal(final String value)
            {
                this.value = value;
            }

            @Override
            public String value()
            {
                return value;
            }
        }
    }
}
