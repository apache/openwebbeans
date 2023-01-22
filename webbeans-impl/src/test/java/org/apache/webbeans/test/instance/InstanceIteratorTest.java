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

import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.stream.Collectors.toList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class InstanceIteratorTest extends AbstractUnitTest
{
    @Test
    public void testInstanceIteratorWithBeanSelector() {
        startContainer(
                Qualifier1.class,
                Qualifier2.class,
                ShardContract.class,
                Bean1.class,
                Bean2.class,
                BeanSelector.class,
                InstanceHolder.class);

        InstanceHolder instanceHolder = getInstance(InstanceHolder.class);
        assertNotNull(instanceHolder);

        Instance<ShardContract> instance = instanceHolder.getInstance();

        int count = 0;

        for (ShardContract ignored : instance)
        {
            count++;
        }
        assertEquals(3, count); //contextual instances: Bean1, Bean2, 2nd instance of Bean1 exposed by the producer
    }

    @Test
    public void testInstanceIteratorWithoutImpl()
    {
        startContainer(InstanceIteratorHolder.class);

        InstanceIteratorHolder instanceIteratorHolder = getInstance(InstanceIteratorHolder.class);
        assertNotNull(instanceIteratorHolder);

        Assert.assertFalse(instanceIteratorHolder.iterateOverContracts());
    }

    @Test
    public void testInstanceIteratorWithImpl()
    {
        startContainer(InstanceIteratorHolder.class, Bean1.class);

        InstanceIteratorHolder instanceIteratorHolder = getInstance(InstanceIteratorHolder.class);
        assertNotNull(instanceIteratorHolder);

        Assert.assertTrue(instanceIteratorHolder.iterateOverContracts());
    }

    @Test
    public void testDestroyDependents()
    {
        ABean.COUNT.set(0);
        startContainer(GetDependents.class, ABean.class);

        final Set<Bean<?>> rbs = getBeanManager().getBeans(GetDependents.class);
        final Bean<?> rb = getBeanManager().resolve(rbs);
        final GetDependents getter = GetDependents.class.cast(getBeanManager().getReference(
                rb, GetDependents.class, getBeanManager().createCreationalContext(rb)));
        assertNotNull(getter);

        final Collection<ABean> beans = getter.get();
        assertEquals(1, beans.size());

        final ABean bean = beans.iterator().next();
        assertFalse(bean.isDestroyed());
        assertEquals(0, ABean.COUNT.get());

        final AlterableContext alterableContext = AlterableContext.class.cast(
                getBeanManager().getContext(ApplicationScoped.class));
        alterableContext.destroy(rb);

        assertTrue(bean.isDestroyed());
        assertEquals(1, ABean.COUNT.get());
    }

    public static class ABean
    {
        private static final AtomicInteger COUNT = new AtomicInteger();
        private boolean destroyed;

        public boolean isDestroyed()
        {
            return destroyed;
        }

        @PreDestroy
        private void destroy()
        {
            destroyed = true;
            COUNT.incrementAndGet();
        }
    }

    @ApplicationScoped
    public static class GetDependents
    {
        @Inject
        private Instance<ABean> beans;

        public Collection<ABean> get()
        {
            return StreamSupport.stream(beans.spliterator(), false).collect(toList());
        }
    }

    public static class InstanceHolder
    {
        @Inject
        @Any
        private Instance<ShardContract> instance;


        public Instance<ShardContract> getInstance()
        {
            return instance;
        }
    }

    @RequestScoped
    public static class InstanceIteratorHolder
    {
        @Inject
        @Any
        private Instance<ShardContract> instances;

        public boolean iterateOverContracts()
        {
            boolean foundSomething = false;
            for (ShardContract contract: instances)
            {
                foundSomething = true;
            }

            return foundSomething;
        }
    }

    @Target({TYPE, METHOD, PARAMETER})
    @Retention(RUNTIME)
    @Qualifier
    public @interface Qualifier1
    {
    }

    @Target({TYPE, METHOD, PARAMETER})
    @Retention(RUNTIME)
    @Qualifier
    public @interface Qualifier2
    {
    }

    public interface ShardContract
    {
    }

    @ApplicationScoped
    @Qualifier1
    public static class Bean1 implements ShardContract
    {
    }

    @ApplicationScoped
    @Qualifier2
    public static class Bean2 implements ShardContract
    {
    }

    public static class BeanSelector
    {
        @Produces
        protected ShardContract selectBean(@Qualifier1 ShardContract bean)
        {
            return bean; //usually there are different beans -> one gets selected, however, it isn't needed for the test
        }
    }
}
