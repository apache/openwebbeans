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
package org.apache.webbeans.test.portable;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Prioritized;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;


/**
 * Test that custom Prioritized Beans work in conjunction with &#064;Priority.
 * See OWB-1219.
 */
public class CustomPrioritizedBeanTest extends AbstractUnitTest
{

    @Test
    public void testCustomPrioritizedBeanInAbdTest()
    {
        addExtension(new CustomBeanAbdExtension());

        startContainer(MySampleClass.class, MySampleAltrnativeClass.class);

        MySampleInterface instance = getInstance(MySampleInterface.class);
        Assert.assertEquals(MySampleAlternativeBean.class.getSimpleName(), instance.impl());
    }

    public static class CustomBeanAbdExtension implements Extension
    {
        public void addBean(@Observes AfterBeanDiscovery abd)
        {
            abd.addBean(new MySampleAlternativeBean());
        }
    }

    public interface MySampleInterface
    {
        String impl();
    }

    @RequestScoped
    public static class MySampleClass implements MySampleInterface
    {
        @Override
        public String impl()
        {
            return this.getClass().getSimpleName();
        }
    }

    @Alternative
    @Priority(1000)
    @RequestScoped
    public static class MySampleAltrnativeClass implements MySampleInterface
    {
        @Override
        public String impl()
        {
            return this.getClass().getSimpleName();
        }
    }

    public static class MySampleAlternativeBean implements Bean<MySampleInterface>, Prioritized
    {
        @Override
        public Set<InjectionPoint> getInjectionPoints()
        {
            return Collections.emptySet();
        }

        @Override
        public Class<?> getBeanClass()
        {
            return MySampleInterface.class;
        }

        @Override
        public MySampleInterface create(CreationalContext<MySampleInterface> context)
        {
            return new MySampleInterface()
            {
                @Override
                public String impl()
                {
                    return MySampleAlternativeBean.class.getSimpleName();
                }
            };
        }

        @Override
        public void destroy(MySampleInterface instance, CreationalContext<MySampleInterface> context)
        {
            // no-op
        }

        @Override
        public Set<Type> getTypes()
        {
            return new HashSet<>(Arrays.asList(Object.class, MySampleInterface.class));
        }

        @Override
        public Set<Annotation> getQualifiers()
        {
            return Collections.singleton(Default.Literal.INSTANCE);
        }

        @Override
        public Class<? extends Annotation> getScope()
        {
            return RequestScoped.class;
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
            return true;
        }

        @Override
        public int getPriority()
        {
            return 2000;
        }
    }


}
