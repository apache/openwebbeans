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
package org.apache.webbeans.test.portable.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertTrue;

// CDI 4.0 spec does not clearly state what beanClass should do in case of a synthetic bean
// Weld users tend to set this to the Extension that registered the bean
// see https://github.com/eclipse-ee4j/mojarra/pull/5158 or https://github.com/eclipse-ee4j/soteria/pull/338
// This test ensures that beans configured this way work in OWB and are proxied on the right type (used to be beanClass before)
public class SyntheticBeanMismatchedBeanClassTest extends AbstractUnitTest {

    @Test
    public void withBeanConfigurator() {
        addExtension(new MyExtension());
        startContainer();

        assertTrue(getInstance("myFirstBean") instanceof MyFirstBean);
    }

    @Test
    public void withInheritedBean() {
        addExtension(new MyExtension());
        startContainer();

        assertTrue(getInstance("mySecondBean") instanceof MySecondBean);
    }

    public static class MyExtension implements Extension {
        public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
            // CDI 2.0+ Bean Configurator
            abd.addBean()
                    .beanClass(MyExtension.class)
                    .scope(ApplicationScoped.class)
                    .name("myFirstBean")
                    .types(Object.class, MyFirstBean.class)
                    .createWith(ctx -> new MyFirstBean());

            // Old school Bean without CDI 2.0 builder
            abd.addBean(new Bean<MySecondBean>() {
                @Override
                public Class<?> getBeanClass() {
                    return MyExtension.class;
                }

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    return Collections.emptySet();
                }

                @Override
                public MySecondBean create(CreationalContext<MySecondBean> creationalContext) {
                    return new MySecondBean();
                }

                @Override
                public void destroy(MySecondBean instance, CreationalContext<MySecondBean> creationalContext) {}

                @Override
                public Set<Type> getTypes() {
                    return Set.of(Object.class, MySecondBean.class);
                }

                @Override
                public Set<Annotation> getQualifiers() {
                    return Set.of();
                }

                @Override
                public Class<? extends Annotation> getScope() {
                    return ApplicationScoped.class;
                }

                @Override
                public String getName() {
                    return "mySecondBean";
                }

                @Override
                public Set<Class<? extends Annotation>> getStereotypes() {
                    return Set.of();
                }

                @Override
                public boolean isAlternative() {
                    return false;
                }
            });
        }
    }

    public static class MyFirstBean { }
    public static class MySecondBean { }
}
