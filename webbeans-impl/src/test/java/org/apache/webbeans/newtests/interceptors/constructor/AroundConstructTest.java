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
package org.apache.webbeans.newtests.interceptors.constructor;

import org.apache.webbeans.intercept.AroundConstruct;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.junit.Ignore;
import org.junit.Test;

import javax.inject.Inject;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class AroundConstructTest extends AbstractUnitTest
{
    @Inject
    private IAmBuiltWithMyConstructor bean;

    @Test @Ignore("needs CDI 1.1")
    public void checkBeanWasBuiltWithItsConstructorAndIntercepted()
    {
        addInterceptor(IllGetYourConstructorInvocation.class);

        final Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(IAmBuiltWithMyConstructor.class);
        beanClasses.add(Foo.class);

        startContainer(beanClasses, Collections.<String>emptyList(), true);
        assertEquals(1, IllGetYourConstructorInvocation.count);
        shutDownContainer();

    }

    @ConstructorInterceptorBinding
    public static class IAmBuiltWithMyConstructor
    {
        private final Foo foo;

        @Inject
        public IAmBuiltWithMyConstructor(final Foo foo)
        {
            if (foo == null) {
                throw new NullPointerException();
            }

            this.foo = foo;
        }
    }

    public static class Foo
    {
    }

    @ConstructorInterceptorBinding
    @Interceptor
    public static class IllGetYourConstructorInvocation
    {
        public static int count = 0;

        @AroundConstruct
        public Object around(final InvocationContext ic) throws Exception
        {
            count++;
            return ic.proceed();
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target( { ElementType.TYPE, ElementType.METHOD })
    public @interface ConstructorInterceptorBinding
    {
    }
}
