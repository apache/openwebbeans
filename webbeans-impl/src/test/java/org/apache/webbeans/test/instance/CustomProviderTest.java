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

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test checks whether it is possible to add an own alternative implementation for
 * a Provider&lt;X&gt; without interfering with Instance&lt;X&gt;
 */
public class CustomProviderTest extends AbstractUnitTest
{

    private @Inject String defaultString;

    private @Inject Instance<String> defaultStringInstance;
    private @Inject @MyQualifier Instance<String> qualifiedStringInstance;

    private @Inject Provider<String> defaultStringProvider;
    private @Inject @MyQualifier Provider<String> qualifiedStringProvider;
    
    
    @Test
    public void testProviderOverwrite()
    {
        startContainerInnerClasses();

        Assert.assertNotNull(defaultString);
        Assert.assertNotNull(defaultStringInstance);
        Assert.assertNotNull(qualifiedStringInstance);
        Assert.assertNotNull(defaultStringProvider);
        Assert.assertNotNull(qualifiedStringProvider);

        Assert.assertFalse(qualifiedStringInstance.isResolvable());
        Assert.assertEquals("qualifiedString", qualifiedStringProvider.get());

        Assert.assertEquals("defaultString", defaultString);
        Assert.assertEquals("defaultString", defaultStringInstance.get());
        Assert.assertEquals("defaultString", defaultStringProvider.get());

    }

    @Qualifier
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MyQualifier
    {

    }

    @Alternative
    @Priority(1000)
    public static class StringProviderProducer
    {
        @Produces
        @Dependent
        @MyQualifier
        public Provider<String> myString()
        {
            return () -> "qualifiedString";
        }
    }

    public static class DefaultStringProducer
    {
        @Produces
        @Dependent
        public String myString()
        {
            return "defaultString";
        }
    }
    
    
}
