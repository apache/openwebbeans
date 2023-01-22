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
package org.apache.webbeans.test.injection.named;

import static org.apache.webbeans.util.Asserts.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class NamedTests extends AbstractUnitTest {

    @Test
    public void testNamedInjection()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();

        beanClasses.add(NamedInterface.class);
        beanClasses.add(NamedBean.class);
        beanClasses.add(DefaultNamedBean.class);
        beanClasses.add(NamedInjectionPoints.class);
        
        startContainer(beanClasses, beanXmls);        

        Bean<?> injectionPointsBean = getBeanManager().getBeans(NamedInjectionPoints.class).iterator().next();
        CreationalContext<?> context = getBeanManager().createCreationalContext(injectionPointsBean);
        NamedInjectionPoints consumer = (NamedInjectionPoints) getBeanManager().getReference(injectionPointsBean, NamedInjectionPoints.class, context);
        
        Assert.assertEquals("NamedBean", consumer.getNamedBeanWithNamedInjectionPoint().getName());
        Assert.assertEquals("NamedBean", consumer.getNamed().getName());
        Assert.assertEquals("DefaultNamedBean", consumer.getDefaultNamedBeanWithNamedInjectionPoint().getName());
        Assert.assertEquals("DefaultNamedBean", consumer.getDefaultNamedBean().getName());

        shutDownContainer();       
        
    }

    @Test
    public void beanConfiguratorNamedQualifier()
    {
        addExtension(new Extension()
        {
            void addBean(@Observes final AfterBeanDiscovery afterBeanDiscovery)
            {
                afterBeanDiscovery.addBean()
                                  .qualifiers(NamedLiteral.of("test"))
                                  .beanClass(NamedInterface.class)
                                  .types(NamedInterface.class)
                                  .createWith(it -> new NamedBean());
            }
        });
        startContainer();
        assertNotNull(getInstance(NamedInterface.class));
    }
}
