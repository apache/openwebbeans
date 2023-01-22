/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.instance;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * As reported by John Ament in OWB-1207
 *
 * Instance x = CDI.current().select(SomeClass, someQualifiers);
 * Instance y = CDI.current().select(SomeClass).select(someQualifiers)
 * did behave different.
 */
public class CdiCurrentTest extends AbstractUnitTest
{

    @Test
    public void testCdiCurrentSelect()
    {
        startContainer(SomeBean.class, OtherOwner.class);

        {
            SomeBean sb = CDI.current().select(SomeBean.class).select(new AnnotationLiteral<Qualifier1>()
            {
            }).get();
            Assert.assertNotNull(sb);
            Assert.assertEquals("other", sb.getX());
        }

        {
            SomeBean sb = CDI.current().select(SomeBean.class, new AnnotationLiteral<Qualifier1>()
            {
            }).get();
            Assert.assertNotNull(sb);
            Assert.assertEquals("other", sb.getX());
        }

    }

    public static class SomeBean
    {
        private String x = "default";

        public String getX()
        {
            return this.x;
        }

        public void setX(String x)
        {
            this.x = x;
        }
    }

    public static class OtherOwner
    {
        @Produces
        @Qualifier1
        public SomeBean createOther()
        {
            SomeBean sb = new SomeBean();
            sb.setX("other");
            return sb;
        }
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Qualifier1
    {
    }

}
