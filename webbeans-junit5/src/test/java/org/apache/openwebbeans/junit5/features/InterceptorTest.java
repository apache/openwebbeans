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
package org.apache.openwebbeans.junit5.features;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Cdi(disableDiscovery = true, classes = {
        InterceptorTest.Service.class, InterceptorTest.MyInterceptor.class, InterceptorTest.Wrap.class
}, interceptors = InterceptorTest.MyInterceptor.class)
public class InterceptorTest
{
    @Inject
    private Service service;

    @Test
    void test1()
    {
        assertEquals("Intercepted Hello World", service.run());
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @InterceptorBinding
    public @interface Wrap {

    }

    @Interceptor
    @Wrap
    public static class MyInterceptor {
        @AroundInvoke
        public Object restrictAccessBasedOnTime(InvocationContext ctx) throws Exception {
            final Object result = ctx.proceed();
            if (result instanceof String) {
                return "Intercepted " + result;
            } else {
                return result;
            }
        }
    }

    @ApplicationScoped
    public static class Service
    {
        @Wrap
        public String run()
        {
            return "Hello World";
        }
    }
}
