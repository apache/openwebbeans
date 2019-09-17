package org.apache.webbeans.test.portable;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;

import org.junit.Assert;

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
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.portable.addannotated.extension.AddAdditionalAnnotatedTypeExtension;
import org.junit.Test;

public class BeforeBeanDiscoveryTest extends AbstractUnitTest
{
    @Test
    public void testAddAdditionalAnnotatedTypeFallbackForNull()
    {
        addExtension(new Extension()
        {
            void addBean(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager)
            {
                beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(Foo.class), null);
            }
        });
        startContainer(Foo.class);

        // just check we don't have :
        // javax.enterprise.inject.AmbiguousResolutionException: There is more than one Bean @Default
        final Foo instance = getInstance(Foo.class);
        assertNotNull(instance);
    }

    @Test
    public void testAddAdditionalAnnotatedType()
    {
        Collection<String> beanXmls = new ArrayList<String>();

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();

        addExtension(new AddAdditionalAnnotatedTypeExtension());

        startContainer(beanClasses, beanXmls);

        Bean<?> bean = getBeanManager().getBeans(AddAdditionalAnnotatedTypeExtension.MyBean.class, new AnnotationLiteral<Default>()
        {
        }).iterator().next();

        // Bean should not be null, as we added it as an additional annotated
        // type during before bean discovery in the extension
        Assert.assertNotNull(bean);

        AddAdditionalAnnotatedTypeExtension.MyConfigBean1 myConfigBean1 = getInstance(AddAdditionalAnnotatedTypeExtension.MyConfigBean1.class);
        Assert.assertNotNull(myConfigBean1);
        Assert.assertEquals("1", myConfigBean1.getId());

        AddAdditionalAnnotatedTypeExtension.MyConfigBean2 myConfigBean2 = getInstance(AddAdditionalAnnotatedTypeExtension.MyConfigBean2.class);
        Assert.assertNotNull(myConfigBean2);
        Assert.assertEquals("2", myConfigBean2.getId());

        shutDownContainer();
    }

    @Test
    public void testAddAdditionalAnnotatedTypeWithPresentClass()
    {
        addExtension(new AddAdditionalAnnotatedTypeExtension());

        startContainer(AddAdditionalAnnotatedTypeExtension.MyBean.class);

        Bean<?> bean = getBeanManager().getBeans(AddAdditionalAnnotatedTypeExtension.MyBean.class).iterator().next();

        // Bean should not be null, as we added it as an additional annotated
        // type during before bean discovery in the extension
        Assert.assertNotNull(bean);

        shutDownContainer();
    }

    @ApplicationScoped
    public static class Foo
    {
    }
}
