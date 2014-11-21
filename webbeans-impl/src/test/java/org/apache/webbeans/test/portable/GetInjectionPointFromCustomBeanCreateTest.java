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
package org.apache.webbeans.test.portable;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GetInjectionPointFromCustomBeanCreateTest extends AbstractUnitTest
{

    @Test
    public void doTest()
    {
        addExtension(new AddBeanExtension());
        startContainer(BeanHolder.class);

        final BeanHolder beanHolder = getInstance(BeanHolder.class);
        final InjectionPoint ip = beanHolder.getManuelBean().ip;
        assertNotNull(ip);
        assertEquals(singleton(new DefaultLiteral()), ip.getQualifiers());
        assertEquals(ManuelBean.class, ip.getType());
        assertEquals(BeanHolder.class, ip.getBean().getBeanClass());
    }

    public static class AddBeanExtension implements Extension
    {
        protected void addBeans(@Observes final AfterBeanDiscovery abd, final BeanManager beanManager)
        {
            final AnnotatedType<ManuelBean> annotatedType = beanManager.createAnnotatedType(ManuelBean.class);

            final InjectionTarget<ManuelBean> injectionTarget = beanManager.createInjectionTarget(annotatedType);
            final Bean<ManuelBean> newBean = new Bean<ManuelBean>()
            {
                @Override
                public Set<Type> getTypes()
                {
                    return Collections.<Type>singleton(ManuelBean.class);
                }

                @Override
                public Set<Annotation> getQualifiers()
                {
                    return Collections.<Annotation>singleton(DefaultLiteral.INSTANCE);
                }

                @Override
                public Class<? extends Annotation> getScope()
                {
                    return Dependent.class;
                }

                @Override
                public String getName()
                {
                    return null;
                }

                @Override
                public boolean isNullable()
                {
                    return false;
                }

                @Override
                public Set<InjectionPoint> getInjectionPoints()
                {
                    return Collections.emptySet();
                }

                @Override
                public Class<?> getBeanClass()
                {
                    return ManuelBean.class;
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

                @Override
                public ManuelBean create(final CreationalContext<ManuelBean> manuelBeanCreationalContext)
                {
                    final ManuelBean result = new ManuelBean();
                    final Bean<InjectionPoint> bean = (Bean<InjectionPoint>) beanManager.resolve(beanManager.getBeans(InjectionPoint.class));
                    result.ip = (InjectionPoint) beanManager.getReference(bean, InjectionPoint.class, manuelBeanCreationalContext);
                    return result;
                }

                @Override
                public void destroy(final ManuelBean manuelBean, final CreationalContext<ManuelBean> manuelBeanCreationalContext)
                {
                    manuelBeanCreationalContext.release();
                }
            };
            abd.addBean(newBean);
        }

    }

    public static class BeanHolder
    {
        @Inject
        private ManuelBean manuelBean;

        public ManuelBean getManuelBean()
        {
            return manuelBean;
        }
    }

    public static class ManuelBean
    {
        private InjectionPoint ip;
    }
}
