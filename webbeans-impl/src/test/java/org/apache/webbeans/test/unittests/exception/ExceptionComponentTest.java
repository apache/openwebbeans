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
package org.apache.webbeans.test.unittests.exception;


import javax.enterprise.inject.spi.Bean;

import java.util.Set;

import org.junit.Assert;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.exception.*;
import org.apache.webbeans.test.component.intercept.NoArgConstructorInterceptorComponent;
import org.junit.Test;

public class ExceptionComponentTest extends AbstractUnitTest
{

    @Test
    public void testFinal()
    {
        startContainer(FinalComponent.class);
        shutDownContainer();
    }

    @Test
    public void testInner()
    {
        startContainer(InnerComponent.class);

        // this should not have been picked up as managed bean according to CDI-1.0 paragraph 3.1.1
        Set<Bean<?>> innerinnerComponentBeans = getBeanManager().getBeans(InnerComponent.InnerInnerComponent.class);
        Assert.assertNotNull(innerinnerComponentBeans);
        Assert.assertEquals(0, innerinnerComponentBeans.size());
        shutDownContainer();
    }

    @Test
    public void testHasFinalMethod()
    {
        try
        {
            startContainer(HasFinalMethodComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void constructorTest()
    {
        try
        {
            startContainer(MoreThanOneConstructureComponent.class);
            Assert.fail("expecting an exception!");
            shutDownContainer();
        }
        catch (WebBeansConfigurationException e)
        {
            // all ok
            System.out.println("got expected exception: " + e.getMessage());
        }

        try
        {
            startContainer(MoreThanOneConstructureComponent2.class);
            // all ok
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
        }
        shutDownContainer();

        startContainer(NoConstructureComponent.class);
        shutDownContainer();
    }

    @Test
    public void testStaticProducerMethod()
    {
        startContainer(ProducerTypeStaticComponent.class);
        shutDownContainer();
    }

    @Test
    public void testDisposeMethod()
    {
        try
        {
            startContainer(MultipleDisposalMethodComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testNewInterface()
    {
        Assert.assertTrue(true); //No more exist
    }

    @Test
    public void testNewBinding()
    {
        try
        {
            startContainer(NewComponentBindingComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testNewMethod()
    {
        Assert.assertTrue(true); //No more test in spec
    }

    @Test
    public void testMoreThanOnePostConstruct()
    {
        try
        {
            startContainer(MoreThanOnePostConstructComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testPostConstructHasParameter()
    {
        try
        {
            startContainer(PostContructMethodHasParameterComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testPostConstructHasReturnType()
    {
        try
        {
            startContainer(PostContructMethodHasReturnTypeComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testPostConstructHasCheckedException()
    {
        try
        {
            startContainer(PostContructMethodHasCheckedExceptionComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testPostConstructHasStatic()
    {
        try
        {
            startContainer(PostContructMethodHasStaticComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testMoreThanOneAroundInvoke()
    {
        try
        {
            startContainer(MoreThanOneAroundInvokeComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testAroundInvokeWithSameMethodName()
    {
        startContainer(AroundInvokeWithSameMethodNameComponent.class);
        shutDownContainer();
    }

    @Test
    public void testAroundInvokeWithoutParameter()
    {
        try
        {

            startContainer(AroundInvokeWithoutParameterComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testAroundInvokeWithoutReturnType()
    {
        try
        {
            startContainer(AroundInvokeWithoutReturnTypeComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testAroundInvokeWithWrongReturnType()
    {
        try
        {
            startContainer(AroundInvokeWithWrongReturnTypeComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testAroundInvokeWithStatic()
    {
        try
        {
            startContainer(AroundInvokeWithStaticMethodComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testAroundInvokeWithFinal()
    {
        try
        {
            startContainer(AroundInvokeWithFinalMethodComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

    @Test
    public void testNoArgConstructorInterceptor()
    {
        try
        {
            startContainer(NoArgConstructorInterceptorComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
        shutDownContainer();
    }

}
