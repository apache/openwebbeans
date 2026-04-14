/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.specalization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * A subclass {@code @Specializes} producer must not disable a superclass producer that only shares
 * the same Java method name but has a different parameter list (CDI 3.3.3 / Java overriding).
 */
public class ProducerMethodSpecializationOverloadTest extends AbstractUnitTest
{
    @Test
    public void testSpecializingNoArgDoesNotDisableParameterOverload()
    {
        startContainer(BaseOverload.class, SubSpecializesNoArgOnly.class, ProducerDep.class);

        Set<Bean<?>> noArg = getBeanManager().getBeans(String.class, new AnnotationLiteral<QNoArg>()
        {
        });
        Assert.assertEquals(1, noArg.size());

        Set<Bean<?>> depOverload = getBeanManager().getBeans(String.class, new AnnotationLiteral<QDepOverload>()
        {
        });
        Assert.assertEquals("overloaded producer (same name, different parameters) must remain enabled", 1, depOverload.size());
        Assert.assertEquals(BaseOverload.class, depOverload.iterator().next().getBeanClass());
    }

    public static class BaseOverload
    {
        @Produces
        @QNoArg
        public String m()
        {
            return "base-m";
        }

        @Produces
        @QDepOverload
        public String m(ProducerDep ignored)
        {
            return "base-m-dep";
        }
    }

    /** Parameter type for the overloaded producer. */
    public static class ProducerDep
    {
    }

    public static class SubSpecializesNoArgOnly extends BaseOverload
    {
        @Produces
        @Specializes
        @QNoArg
        public String m()
        {
            return "sub-m";
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
    @Qualifier
    public @interface QNoArg
    {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
    @Qualifier
    public @interface QDepOverload
    {
    }
}
