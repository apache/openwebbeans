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
package org.apache.webbeans.newtests.portable;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.spi.ContextsService;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
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

public class CustomBeanDestroyCalledTest extends AbstractUnitTest
{

    @Test
    public void dependentScopedBeanTest()
    {
        addExtension(new ExternalBeanExtension());
        startContainer(BeanHolder.class);

        final ContextsService contextsService = getWebBeansContext().getContextsService();

        contextsService.startContext(RequestScoped.class, null);

        final BeanHolder beanHolder = getInstance(BeanHolder.class);
        Assert.assertNotNull(beanHolder.getManuelBean());
        Assert.assertTrue(beanHolder.getManuelBean().isConstructCalled());

        ManuelBean refToPreviousManuelBean = beanHolder.getManuelBean();

        contextsService.endContext(RequestScoped.class, null);

        Assert.assertTrue(refToPreviousManuelBean.isLifecycleDestroyCalled());
        Assert.assertTrue(refToPreviousManuelBean.isDestroyCalled());
    }

    public static class ExternalBeanExtension implements Extension
    {
        protected void addBeans(@Observes AfterBeanDiscovery abd, BeanManager beanManager)
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
                public ManuelBean create(CreationalContext<ManuelBean> manuelBeanCreationalContext)
                {
                    final ManuelBean result = new ManuelBean();
                    injectionTarget.postConstruct(result);
                    return result;
                }

                @Override
                public void destroy(ManuelBean manuelBean, CreationalContext<ManuelBean> manuelBeanCreationalContext)
                {
                    manuelBean.setLifecycleDestroyCalled(true);
                    injectionTarget.preDestroy(manuelBean);
                    manuelBeanCreationalContext.release();
                }
            };
            abd.addBean(newBean);
        }

    }
    @RequestScoped
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
        private boolean constructCalled;
        private boolean destroyCalled;
        private boolean lifecycleDestroyCalled;

        @PostConstruct
        protected void onConstruct()
        {
            this.constructCalled = true;
        }

        @PreDestroy
        protected void onDestroy()
        {
            this.destroyCalled = true;
        }

        public boolean isConstructCalled()
        {
            return constructCalled;
        }

        public boolean isDestroyCalled()
        {
            return destroyCalled;
        }

        public boolean isLifecycleDestroyCalled()
        {
            return lifecycleDestroyCalled;
        }

        public void setLifecycleDestroyCalled(boolean lifecycleDestroyCalled)
        {
            this.lifecycleDestroyCalled = lifecycleDestroyCalled;
        }
    }
}
