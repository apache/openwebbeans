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

import org.apache.webbeans.config.OwbParametrizedTypeImpl;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;

public class QualifierWithOptionalInjectTest extends AbstractUnitTest
{
    @Test
    public void run()
    {
        System.setProperty("org.apache.webbeans.application.supportsImplicitQualifierInjection", "true");
        startContainer(Producing.class, Injected.class);
        final OwbParametrizedTypeImpl type = new OwbParametrizedTypeImpl(null, Supplier.class, String.class);
        final Supplier<String> injected = getInstance(type);
        assertEquals("yes/no", injected.get());
        System.clearProperty("org.apache.webbeans.application.supportsImplicitQualifierInjection");
    }

    @Dependent
    public static class Producing
    {
        @Produces
        @TheQualifier("whatever")
        String yes(final InjectionPoint injectionPoint) {
            return new StringBuilder(
                    injectionPoint.getAnnotated().getAnnotation(TheQualifier.class).value()).reverse().toString();
        }
    }

    @Dependent
    public static class Injected implements Supplier<String>
    {
        @TheQualifier("sey")
        private String yes;

        @TheQualifier("on")
        private String no;

        @Override
        public String get()
        {
            return yes + '/' + no;
        }
    }

    @Qualifier
    @Target({FIELD, METHOD})
    @Retention(RUNTIME)
    public @interface TheQualifier
    {
        @Nonbinding
        String value();
    }
}
