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
package org.apache.webbeans.test.component.third;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

public class ThirdPartyExtension implements Extension
{
    public void addThirdPartyBean(@Observes AfterBeanDiscovery afterBeanDiscovery)
    {
        afterBeanDiscovery.addBean(new Bean<SomeFakeBean>()
        {
            @Override
            public Set<Type> getTypes()
            {
                Set<Type> types = new HashSet<Type>();
                types.add(Object.class);
                types.add(SomeFakeBean.class);
                return types;
            }

            @Override
            public Set<Annotation> getQualifiers()
            {
                return emptySet();
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
            public Set<Class<? extends Annotation>> getStereotypes()
            {
                return emptySet();
            }

            @Override
            public boolean isAlternative()
            {
                return false;
            }

            @Override
            public SomeFakeBean create(CreationalContext<SomeFakeBean> creationalContext)
            {
                return new SomeFakeBean();
            }

            @Override
            public void destroy(SomeFakeBean someFakeBean, CreationalContext<SomeFakeBean> creationalContext)
            {

            }

            @Override
            public Set<InjectionPoint> getInjectionPoints()
            {
                return emptySet();
            }

            @Override
            public Class<?> getBeanClass()
            {
                return SomeFakeBean.class;
            }

            @Override
            public boolean isNullable()
            {
                return false;
            }
        });
    }

    public static class SomeFakeBean
    {
        public String doProcess()
        {
            return "processed";
        }
    }
}
