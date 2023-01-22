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
package org.apache.webbeans.test.interceptors.constructor;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InterceptionOfBeanWithConstructorInjectionTest extends AbstractUnitTest
{
    @Inject
    private BuildMeWithMyConstructor bean;

    @Test
    public void checkBeanWasBuiltWithItsConstructorAndIntercepted()
    {
        addInterceptor(ConstructorIsNotAnIssueForMe.class);

        final Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(BuildMeWithMyConstructor.class);
        beanClasses.add(Injected.class);

        startContainer(beanClasses, Collections.<String>emptyList(), true);
        assertNotNull(bean);
        assertNotNull(bean.getInjected());
        assertEquals(1, ConstructorIsNotAnIssueForMe.count);
        shutDownContainer();

    }

    @ConstructorInterceptorBindingType
    public static class BuildMeWithMyConstructor
    {
        private final Injected injected;

        @Inject
        public BuildMeWithMyConstructor(final Injected injected)
        {
            this.injected = injected;
        }

        public Injected getInjected()
        {
            return injected;
        }
    }

    public static class Injected
    {
    }

    @ConstructorInterceptorBindingType @Interceptor
    public static class ConstructorIsNotAnIssueForMe
    {
        public static int count = 0;

        @AroundInvoke
        public Object around(final InvocationContext ic) throws Throwable
        {
            count++;
            return ic.proceed();
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target( { ElementType.TYPE, ElementType.METHOD })
    public @interface ConstructorInterceptorBindingType
    {
    }

}
