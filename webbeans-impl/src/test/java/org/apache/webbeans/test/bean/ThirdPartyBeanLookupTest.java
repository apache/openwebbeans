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
package org.apache.webbeans.test.bean;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ThirdPartyBeanLookupTest extends AbstractUnitTest
{
    @Inject
    private Ext ext;
    @Inject
    private Some some;

    @Test
    public void areEquals()
    {
        addExtension(new Ext());
        startContainer(Collections.<Class<?>>emptyList(), Collections.<String>emptyList(), true);
        assertEquals(ext.getInstance().getVal(), some.getVal());

        final BeanManager bm = CDI.current().getBeanManager();
        final Bean<?> bean = bm.resolve(bm.getBeans(Some.class));
        final Some instance = Some.class.cast(bm.getReference(bean, bean.getBeanClass(), null));
        assertEquals(instance.getVal(), some.getVal());
    }

    public static class Ext implements Extension
    {
        private SomeBean bean;
        private Some instance;

        void add(@Observes final AfterBeanDiscovery abf)
        {
            bean = new SomeBean();
            abf.addBean(bean);
        }

        void lookup(@Observes final AfterDeploymentValidation adv, final BeanManager bm)
        {
            instance = Some.class.cast(bm.getReference(bean, bean.getBeanClass(), null));
        }

        public Some getInstance()
        {
            return instance;
        }
    }

    private static class SomeBean implements Bean<Some>
    {
        @Override
        public Set<InjectionPoint> getInjectionPoints()
        {
            return Collections.emptySet();
        }

        @Override
        public Class<?> getBeanClass()
        {
            return Some.class;
        }

        @Override
        public Some create(final CreationalContext<Some> context)
        {
            return new Some();
        }

        @Override
        public void destroy(final Some instance, final CreationalContext<Some> context)
        {
            // no-op
        }

        @Override
        public Set<Type> getTypes()
        {
            return Collections.<Type>singleton(Some.class);
        }

        @Override
        public Set<Annotation> getQualifiers()
        {
            return Collections.<Annotation>singleton(DefaultLiteral.INSTANCE);
        }

        @Override
        public Class<? extends Annotation> getScope()
        {
            return ApplicationScoped.class;
        }

        @Override
        public String getName()
        {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes()
        {
            return Collections.emptySet();
        }

        @Override
        public boolean isAlternative()
        {
            return false;
        }
    }

    public static class Some {
        private final int val = System.identityHashCode(this);

        public int getVal() {
            return val;
        }
    }
}
