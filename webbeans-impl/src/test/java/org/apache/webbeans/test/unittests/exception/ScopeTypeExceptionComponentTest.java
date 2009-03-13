/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.test.unittests.exception;

import javax.context.Dependent;
import javax.context.RequestScoped;
import javax.context.SessionScoped;
import javax.inject.manager.Bean;
import javax.servlet.ServletContext;

import junit.framework.Assert;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.test.component.exception.stero.ComponentDefaultScopeWithDifferentScopeSteros;
import org.apache.webbeans.test.component.exception.stero.ComponentDefaultScopeWithNonScopeStero;
import org.apache.webbeans.test.component.exception.stero.ComponentNonDefaultScopeWithDifferentScopeSteros;
import org.apache.webbeans.test.component.exception.stero.ComponentWithDefaultScopeStero;
import org.apache.webbeans.test.component.exception.stero.ComponentWithDifferentScopeSteros;
import org.apache.webbeans.test.component.exception.stero.ComponentWithNonScopeStero;
import org.apache.webbeans.test.component.exception.stero.ComponentWithSameScopeSteros;
import org.apache.webbeans.test.component.exception.stero.ComponentWithoutScopeStero;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;

public class ScopeTypeExceptionComponentTest extends TestContext
{

    public ScopeTypeExceptionComponentTest()
    {
        super(ScopeTypeExceptionComponentTest.class.getName());
    }

    public void endTests(ServletContext ctx)
    {

    }

    @Before
    public void init()
    {
        super.init();
    }

    public void startTests(ServletContext ctx)
    {

    }

    @Test
    public void testComponentWithNonScopeStero()
    {
        clear();
        defineSimpleWebBean(ComponentWithNonScopeStero.class);
        Bean<?> bean = getComponents().get(0);

        Assert.assertEquals(Dependent.class, bean.getScopeType());
    }

    @Test
    public void testComponentDefaultScopeWithNonScopeStero()
    {
        WebBeansConfigurationException exc = null;
        clear();
        defineSimpleWebBean(ComponentDefaultScopeWithNonScopeStero.class);
        Bean<?> bean = getComponents().get(0);

        Assert.assertEquals(SessionScoped.class, bean.getScopeType());
    }

    @Test
    public void testComponentWithDefaultScopeStero()
    {
        clear();
        defineSimpleWebBean(ComponentWithDefaultScopeStero.class);
        Bean<?> bean = getComponents().get(0);

        Assert.assertEquals(RequestScoped.class, bean.getScopeType());
    }

    @Test
    public void testComponentWithDifferentScopeSteros()
    {
        WebBeansConfigurationException exc = null;
        clear();
        try
        {
            defineSimpleWebBean(ComponentWithDifferentScopeSteros.class);

        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println(e.getMessage());
            exc = e;

        }

        Assert.assertNotNull(exc);
    }

    @Test
    public void testComponentWithoutScopeStero()
    {
        clear();
        defineSimpleWebBean(ComponentWithoutScopeStero.class);
        Bean<?> bean = getComponents().get(0);

        Assert.assertEquals(Dependent.class, bean.getScopeType());
    }

    @Test
    public void testComponentWithSameScopeSteros()
    {
        clear();
        defineSimpleWebBean(ComponentWithSameScopeSteros.class);
        Bean<?> bean = getComponents().get(0);

        Assert.assertEquals(SessionScoped.class, bean.getScopeType());
    }

    @Test
    public void testComponentDefaultScopeWithDifferentScopeSteros()
    {
        clear();
        defineSimpleWebBean(ComponentDefaultScopeWithDifferentScopeSteros.class);
        Bean<?> bean = getComponents().get(0);

        Assert.assertEquals(SessionScoped.class, bean.getScopeType());
    }

    @Test
    public void testComponentNonDefaultScopeWithDifferentScopeSteros()
    {
        WebBeansConfigurationException exc = null;
        clear();
        try
        {
            defineSimpleWebBean(ComponentNonDefaultScopeWithDifferentScopeSteros.class);

        }
        catch (WebBeansConfigurationException e)
        {
            exc = e;
        }

        Assert.assertNotNull(exc);
    }

}
