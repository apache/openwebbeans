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
package org.apache.webbeans.test.events.injectiontarget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.ProcessInjectionTarget;
import jakarta.inject.Inject;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ReplaceInjectionTargetTest extends AbstractUnitTest
{
    public static class IJBean {
        @Inject
        private InjectedBean injection;
    }

    public static class InjectedBean {

    }

    public static class MyInjectionTarget implements InjectionTarget<IJBean> {
        private static boolean injected = false;

        private final InjectionTarget<IJBean> injectionTarget;

        public MyInjectionTarget(InjectionTarget<IJBean> injectionTarget) {
            this.injectionTarget = injectionTarget;
        }

        @Override
        public void inject(IJBean instance, CreationalContext<IJBean> ctx) {
            injected = true;
            injectionTarget.inject(instance, ctx);
        }

        @Override
        public void postConstruct(IJBean instance) {
            injectionTarget.postConstruct(instance);
        }

        @Override
        public void preDestroy(IJBean instance) {
            injectionTarget.preDestroy(instance);
        }

        @Override
        public IJBean produce(CreationalContext<IJBean> ijBeanCreationalContext) {
            return injectionTarget.produce(ijBeanCreationalContext);
        }

        @Override
        public void dispose(IJBean instance) {
            injectionTarget.dispose(instance);
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return injectionTarget.getInjectionPoints();
        }
    }

    public static class InjectionTargetReplacer implements Extension {
        public void replaceInjectionTarget(@Observes ProcessInjectionTarget<IJBean> event) {
            event.setInjectionTarget(new MyInjectionTarget(event.getInjectionTarget()));
        }
    }

    @Test
    public void checkCustomWrapperIsUsed() {
        addExtension(new InjectionTargetReplacer());

        final Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(IJBean.class);
        beanClasses.add(InjectedBean.class);

        startContainer(beanClasses, null);

        final Set<Bean<?>> beans = getBeanManager().getBeans(IJBean.class);
        assertNotNull(beans);
        assertFalse(beans.isEmpty());
        Bean<?> bean = getBeanManager().resolve(beans);
        CreationalContext<?> cc = getBeanManager().createCreationalContext(bean);
        assertNotNull(getBeanManager().getReference(beans.iterator().next(), IJBean.class, cc));
        assertTrue(MyInjectionTarget.injected);

        shutDownContainer();
    }
}
