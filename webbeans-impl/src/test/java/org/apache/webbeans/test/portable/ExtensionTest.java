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

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.junit.Assert;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.config.WebBeansContext;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.contexts.SerializationTest;
import org.apache.webbeans.test.portable.alternative.Egg;
import org.apache.webbeans.test.portable.alternative.HalfEgg;
import org.apache.webbeans.test.portable.alternative.WoodEgg;
import org.apache.webbeans.test.portable.events.extensions.AlternativeExtension;
import org.apache.webbeans.test.portable.scopeextension.BeanWithExtensionInjected;
import org.apache.webbeans.test.portable.scopeextension.ExternalTestScopeExtension;
import org.apache.webbeans.test.portable.scopeextension.ExternalTestScoped;
import org.apache.webbeans.test.portable.scopeextension.ExternalTestScopedBean;
import org.apache.webbeans.test.portable.scopeextension.ExternalUnserializableTestScopedBean;
import org.apache.webbeans.test.portable.scopeextension.broken.CdiBeanWithLifecycleObserver;
import org.apache.webbeans.portable.events.discovery.BeforeShutdownImpl;
import org.junit.Test;

/**
 * This test checks if an extension gets loaded correctly and
 * if all specified events get fired.
 */
public class ExtensionTest extends AbstractUnitTest
{
    public ExtensionTest()
    {
    }

    
    /**
     * This test adds a scope and tests if the lifecycle works
     */
    @Test
    public void testScopeExtension()
    {
        addExtension(new ExternalTestScopeExtension());
        startContainer(ExternalTestScopedBean.class);

        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        webBeansContext.getContextsService().startContext(ApplicationScoped.class, null);

        @SuppressWarnings("unchecked")
        Bean<ExternalTestScopedBean> bean = (Bean<ExternalTestScopedBean>) getBeanManager().getBeans(ExternalTestScopedBean.class, 
                                                                                                     new DefaultLiteral()).iterator().next();
        
        ExternalTestScopedBean instance = (ExternalTestScopedBean) getBeanManager().getReference(bean, ExternalTestScopedBean.class, 
                                                                                                 getBeanManager().createCreationalContext(bean));
        
        Assert.assertNotNull(instance);
        
        Assert.assertTrue(getBeanManager().isPassivatingScope(ExternalTestScoped.class));
        
        //Fire shut down
        BeanManagerImpl.class.cast(getBeanManager()).fireEvent(new BeforeShutdownImpl(), true);

        webBeansContext.getContextsService().endContext(ApplicationScoped.class, null);

        shutDownContainer();
    }

    @Test
    public void testLifecycleObservingInStandardCdiBeans() throws Exception
    {
        CdiBeanWithLifecycleObserver.beforeBeanDiscoveryCalled = false;
        CdiBeanWithLifecycleObserver.afterBeanDiscoveryCalled = false;

        startContainer(CdiBeanWithLifecycleObserver.class);

        CdiBeanWithLifecycleObserver instance = getInstance(CdiBeanWithLifecycleObserver.class);
        Assert.assertNotNull(instance);

        Assert.assertFalse(CdiBeanWithLifecycleObserver.beforeBeanDiscoveryCalled);
        Assert.assertFalse(CdiBeanWithLifecycleObserver.afterBeanDiscoveryCalled);
    }
    
    /**
     * Classes in a passivatable scope must be Serializable
     */
    @Test
    public void testUnserializableBean() 
    {
        try 
        {
            addExtension(new ExternalTestScopeExtension());
            startContainer(ExternalUnserializableTestScopedBean.class);
            
            // we must not get here since an Exception is expected!
            Assert.fail();
        }
        catch (DefinitionException dex)
        {
            // this is expected!
        }
    }

    /**
     * This will test if an &#0064;Alternative can get added dynamically.
     * Please note that we cannot do much more than checking if the
     * dynamically enabled &#0064;Alternative is now disabled.
     * That's because with CDI-1.0 it's not possible to also add
     * the Alternative to beans.xml nor programmatically.
     * This feature only gets added in CDI-1.1 with
     * ProcessModule#getAlternatives() which can be modified.
     *
     * TODO: extend test for CDI-1.1
     */
    @Test
    public void testAlternativeExtenson()
    {
        addExtension(new AlternativeExtension());
        startContainer(Egg.class, HalfEgg.class);

        Egg egg = getInstance(Egg.class);
        Assert.assertTrue(egg instanceof Egg);
        Assert.assertFalse(egg instanceof HalfEgg);
        Set<Bean<?>> beans = getBeanManager().getBeans(HalfEgg.class);
        Assert.assertTrue(beans == null || beans.size() == 0);

        shutDownContainer();
    }

    /**
     * Test the dynamic removal of an &#064;Alternative annotation
     * via ProcessAnnotatedType.
     */
    @Test
    public void testRemoveAlternativeExtension()
    {
        addExtension(new AlternativeExtension());
        startContainer(Egg.class, WoodEgg.class);

        Set<Bean<?>> beans = getBeanManager().getBeans(Egg.class);
        Assert.assertTrue(beans != null && beans.size() == 2);

    }

    @Test
    public void testInjectedExtensionSerialisation() throws Exception
    {
        addExtension(new ExternalTestScopeExtension());
        startContainer(BeanWithExtensionInjected.class);

        BeanWithExtensionInjected instance = getInstance(BeanWithExtensionInjected.class);
        Assert.assertNotNull(instance);
        Assert.assertNotNull(instance.getExtension());

        Bean<ExternalTestScopeExtension> extensionBean = getBean(ExternalTestScopeExtension.class);
        Assert.assertNotNull(extensionBean);
        Assert.assertTrue(extensionBean instanceof PassivationCapable);
        Assert.assertNotNull(((PassivationCapable) extensionBean).getId());

        byte[] ba = SerializationTest.serializeObject(instance);
        Assert.assertNotNull(ba);
        BeanWithExtensionInjected serializedInstance
                = (BeanWithExtensionInjected) SerializationTest.deSerializeObject(ba);
        Assert.assertNotNull(serializedInstance);
        Assert.assertNotNull(serializedInstance.getExtension());

    }

}
