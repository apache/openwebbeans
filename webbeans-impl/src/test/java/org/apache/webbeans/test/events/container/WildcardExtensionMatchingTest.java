/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.events.container;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collection;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class WildcardExtensionMatchingTest extends AbstractUnitTest {
    @Test
    public void injectionPoint() {
        final Collection<InjectionPoint> points = new ArrayList<>();
        addExtension(new Extension() {
            void add(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
                afterBeanDiscovery.addBean()
                        .id("v1")
                        .types(String.class, Object.class)
                        .createWith(c -> "ok")
                        .beanClass(MyBean.class)
                        .qualifiers(NamedLiteral.of("v1"))
                        .scope(Dependent.class);
                afterBeanDiscovery.addBean()
                        .id("v2")
                        .types(Boolean.class, Object.class)
                        .createWith(c -> 1)
                        .beanClass(MyBean.class)
                        .qualifiers(NamedLiteral.of("v2"))
                        .scope(Dependent.class);
            }

            private void processInjectionPoint(@Observes final ProcessInjectionPoint<?, ? extends String> event) {
                points.add(event.getInjectionPoint());
            }
        });
        addInterceptor(MyInterceptor.class);
        startContainer(MyBean.class);
        assertEquals(1, points.size());
    }

    @InterceptorBinding
    @Retention(RUNTIME)
    public @interface Foo {}

    @Interceptor
    @Foo
    public static class MyInterceptor {
        @Inject
        @Intercepted
        private Bean<?> bean;

        @AroundInvoke
        public Object call(final InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    public static class MyBean {
        @Inject
        @Named("v1")
        private String v1;

        @Inject
        @Named("v2")
        private Boolean v2;
    }
}
