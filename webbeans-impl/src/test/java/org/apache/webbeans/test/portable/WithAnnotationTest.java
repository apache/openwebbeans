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
package org.apache.webbeans.test.portable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.inject.Inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test &#064WithAnnotation; annotation
 */
public class WithAnnotationTest extends AbstractUnitTest
{

    @Test
    public void testWithAnnotation()
    {
        WithAnnotationExtension.scannedClasses = 0;
        WithAnnotationExtension.one = 0;

        addExtension(new WithAnnotationExtension());
        startContainer(WithConstructorAnnotatedClass.class, WithoutAnyAnnotation.class, WithAnnotatedClass.class, WithAnnotatedField.class, WithAnnotatedMethod.class);

        Assert.assertEquals(4, WithAnnotationExtension.scannedClasses);
        Assert.assertEquals(1, WithAnnotationExtension.one);
    }


    public static class WithAnnotationExtension implements Extension
    {
        public static int scannedClasses = 0;
        public static int one = 0;

        public void processClassess(@Observes @WithAnnotations(MyAnnoation.class) ProcessAnnotatedType pat)
        {
            scannedClasses += 1;
        }

        public void dontProcessClassess(@Observes @WithAnnotations(AnotherAnnoation.class) ProcessAnnotatedType pat)
        {
            throw new IllegalStateException("This observer must not get called by the container!");
        }

        public void noIssueWithGenericsOWB997(@Observes @WithAnnotations(MyAnnoation.class) ProcessAnnotatedType<WithAnnotatedClass> pat)
        {
            one++;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
    public static @interface MyAnnoation
    {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
    public static @interface AnotherAnnoation
    {
    }


    /**
     * This class should not get picked up by the {@link org.apache.webbeans.test.portable.WithAnnotationTest.WithAnnotationExtension}
     */
    @ApplicationScoped
    public static class WithoutAnyAnnotation
    {
        public int getMeanintOfLife()
        {
            return 42;
        }
    }


    @ApplicationScoped
    @MyAnnoation
    public static class WithAnnotatedClass
    {
        public int getMeanintOfLife()
        {
            return 42;
        }
    }

    @MyAnnoation
    public static class WithConstructorAnnotatedClass
    {
        @Inject
        public WithConstructorAnnotatedClass(@MyAnnoation WithAnnotatedClass wac) {
        }
    }

    @ApplicationScoped
    public static class WithAnnotatedField
    {
        @MyAnnoation
        private int x;

        public int getMeanintOfLife()
        {
            return 42;
        }
    }

    @ApplicationScoped
    public static class WithAnnotatedMethod
    {
        @MyAnnoation
        public int getMeanintOfLife()
        {
            return 42;
        }
    }

}
