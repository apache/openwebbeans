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

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;

public class AfterBeanDiscoveryEventTest extends AbstractUnitTest {
    @Inject
    private MyWrapper wrapper;

    @Test
    public void validCase()
    {
        addExtension(new MyExtension());
        startContainer(Collections.<Class<?>>emptyList(), Collections.<String>emptyList(), true);
        wrapper.check();
    }

    public static class MyExtension implements Extension
    {
        public void createBeans(@Observes final AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager)
        {
            // 1. shouldn't throw IllegalStateException at this stage
            beanManager.getBeans(Object.class);

            // 2. create a bean with an injection before adding the injection bean (concurrent extensions case)
            {
                final AnnotatedType<MyWrapper> annotatedType = beanManager.createAnnotatedType(MyWrapper.class);
                // injections point can't be validated yet, should be after the whole phase
                beanManager.createInjectionTarget(annotatedType);

                // add the bean
                afterBeanDiscovery.addBean(beanManager.createBean(
                        beanManager.createBeanAttributes(annotatedType), MyWrapper.class,
                        beanManager.getInjectionTargetFactory(annotatedType)));
            }

            // 3. add the injected bean
            {
                final AnnotatedType<MyInjection> annotatedType = beanManager.createAnnotatedType(MyInjection.class);
                afterBeanDiscovery.addBean(beanManager.createBean(
                        beanManager.createBeanAttributes(annotatedType), MyInjection.class,
                        beanManager.getInjectionTargetFactory(annotatedType)));
            }
        }
    }

    public static class MyInjection
    {
    }

    public static class MyWrapper
    {
        @Inject
        private MyInjection inj;

        public void check()
        {
            assertNotNull(inj);
        }
    }
}
