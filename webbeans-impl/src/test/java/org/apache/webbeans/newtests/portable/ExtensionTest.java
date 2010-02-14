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
package org.apache.webbeans.newtests.portable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.inject.spi.Bean;

import junit.framework.Assert;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.apache.webbeans.lifecycle.test.MockServletContext;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.portable.scopeextension.ExternalTestScopeExtension;
import org.apache.webbeans.newtests.portable.scopeextension.ExternalTestScoped;
import org.apache.webbeans.newtests.portable.scopeextension.ExternalTestScopedBean;
import org.apache.webbeans.newtests.portable.scopeextension.ExternalUnserializableTestScopedBean;
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
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(ExternalTestScopedBean.class);
        addExtension(new ExternalTestScopeExtension());
        startContainer(classes);

        MockServletContext servletContext = new MockServletContext();
        ContextFactory.initApplicationContext(servletContext);

        @SuppressWarnings("unchecked")
        Bean<ExternalTestScopedBean> bean = (Bean<ExternalTestScopedBean>) getBeanManager().getBeans(ExternalTestScopedBean.class, 
                                                                                                     new DefaultLiteral()).iterator().next();
        
        ExternalTestScopedBean instance = (ExternalTestScopedBean) getBeanManager().getReference(bean, ExternalTestScopedBean.class, 
                                                                                                 getBeanManager().createCreationalContext(bean));
        
        Assert.assertNotNull(instance);
        
        Assert.assertTrue(getLifecycle().getBeanManager().isPassivatingScope(ExternalTestScoped.class));
        
        //Fire shut down
        getBeanManager().fireEvent(new BeforeShutdownImpl(), new Annotation[0]);

        ContextFactory.destroyApplicationContext(servletContext);
        
        shutDownContainer();
        
    }
    
    /**
     * Classes in a passivatable scope must be Serializable
     */
    @Test
    public void testUnserializableBean() 
    {
        try 
        {
            Collection<Class<?>> classes = new ArrayList<Class<?>>();
            classes.add(ExternalUnserializableTestScopedBean.class);
            addExtension(new ExternalTestScopeExtension());
            startContainer(classes);
            
            // we must not get here since an Exception is expected!
            Assert.fail();
        }
        catch (DefinitionException dex)
        {
            // this is expected!
        }
    }
}
