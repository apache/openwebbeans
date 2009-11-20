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

import javax.enterprise.inject.spi.Bean;
import javax.servlet.ServletContext;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.ejb.EJBInterceptorConfig;
import org.apache.webbeans.test.TestContext;
import org.apache.webbeans.test.component.exception.AroundInvokeWithFinalMethodComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithSameMethodNameComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithStaticMethodComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithWrongReturnTypeComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithoutExceptionComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithoutParameterComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithoutReturnTypeComponent;
import org.apache.webbeans.test.component.exception.ComponentTypeExceptionComponent;
import org.apache.webbeans.test.component.exception.FinalComponent;
import org.apache.webbeans.test.component.exception.HasFinalMethodComponent;
import org.apache.webbeans.test.component.exception.MoreThanOneAroundInvokeComponent;
import org.apache.webbeans.test.component.exception.MoreThanOneConstructureComponent;
import org.apache.webbeans.test.component.exception.MoreThanOneConstructureComponent2;
import org.apache.webbeans.test.component.exception.MoreThanOnePostConstructComponent;
import org.apache.webbeans.test.component.exception.MultipleDisposalMethodComponent;
import org.apache.webbeans.test.component.exception.NewComponentBindingComponent;
import org.apache.webbeans.test.component.exception.NewComponentInterfaceComponent;
import org.apache.webbeans.test.component.exception.NewMethodComponentBindingComponent;
import org.apache.webbeans.test.component.exception.NoConstructureComponent;
import org.apache.webbeans.test.component.exception.PostContructMethodHasCheckedExceptionComponent;
import org.apache.webbeans.test.component.exception.PostContructMethodHasParameterComponent;
import org.apache.webbeans.test.component.exception.PostContructMethodHasReturnTypeComponent;
import org.apache.webbeans.test.component.exception.PostContructMethodHasStaticComponent;
import org.apache.webbeans.test.component.exception.ProducerTypeExceptionComponent;
import org.apache.webbeans.test.component.exception.ProducerTypeStaticComponent;
import org.apache.webbeans.test.component.exception.InnerComponent.InnerInnerComponent;
import org.apache.webbeans.test.component.intercept.NoArgConstructorInterceptorComponent;
import org.junit.Before;
import org.junit.Test;

public class ExceptionComponentTest extends TestContext
{

    public ExceptionComponentTest()
    {
        super(ExceptionComponentTest.class.getName());
    }

    public void endTests(ServletContext ctx)
    {

    }

    @Before
    public void init()
    {
        super.init();

    }

    @Test
    public void testComponentTypeException()
    {
        try
        {
            OpenWebBeansConfiguration.getInstance().setProperty(OpenWebBeansConfiguration.USE_INJECTION_RESOLVER_VIA_ALTERNATIVE, "false");
            clear();
            defineManagedBean(ComponentTypeExceptionComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println(e.getMessage());
            return; // all ok!
        }
        finally
        {
            OpenWebBeansConfiguration.getInstance().setProperty(OpenWebBeansConfiguration.USE_INJECTION_RESOLVER_VIA_ALTERNATIVE, "true");
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testProducerMethodComponentTypeException()
    {
        WebBeansConfigurationException exc = null;
        try
        {
            OpenWebBeansConfiguration.getInstance().setProperty(OpenWebBeansConfiguration.USE_INJECTION_RESOLVER_VIA_ALTERNATIVE, "false");
            clear();
            defineManagedBean(ProducerTypeExceptionComponent.class);

        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        finally
        {
            OpenWebBeansConfiguration.getInstance().setProperty(OpenWebBeansConfiguration.USE_INJECTION_RESOLVER_VIA_ALTERNATIVE, "true");
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testFinal()
    {
        clear();
        defineManagedBean(FinalComponent.class);
    }

    @Test
    public void testAbstract()
    {
        try
        {
            clear();
            defineManagedBean(AbstractBean.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testInner()
    {
        try
        {
            clear();
            defineManagedBean(InnerInnerComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testHasFinalMethod()
    {
        try
        {
            clear();
            defineManagedBean(HasFinalMethodComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void constructorTest()
    {
        try
        {
            clear();
            defineManagedBean(MoreThanOneConstructureComponent.class);
            Assert.fail("expecting an exception!");
        }
        catch (WebBeansConfigurationException e)
        {
            // all ok
            System.out.println("got expected exception: " + e.getMessage());
        }

        try
        {
            clear();
            defineManagedBean(MoreThanOneConstructureComponent2.class);
            // all ok
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
        }

        clear();
        defineManagedBean(NoConstructureComponent.class);
    }

    @Test
    public void testStaticProducerMethod()
    {
        clear();
        defineManagedBean(ProducerTypeStaticComponent.class);
    }

    @Test
    public void testDisposeMethod()
    {
        try
        {
            clear();
            defineManagedBean(MultipleDisposalMethodComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testNewInterface()
    {
        try
        {
            clear();
            defineManagedBean(NewComponentInterfaceComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testNewBinding()
    {
        try
        {
            clear();
            defineManagedBean(NewComponentBindingComponent.class);
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testNewMethod()
    {
        WebBeansConfigurationException exc = null;
        try
        {
            clear();
            defineManagedBean(NewMethodComponentBindingComponent.class);

        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println(e.getMessage());
            exc = e;
        }

        Assert.assertNotNull(exc);

    }

    @Test
    public void testMoreThanOnePostConstruct()
    {
        try
        {
            clear();
            AbstractBean<MoreThanOnePostConstructComponent> component = defineManagedBean(MoreThanOnePostConstructComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testPostConstructHasParameter()
    {
        try
        {
            clear();
            AbstractBean<PostContructMethodHasParameterComponent> component = defineManagedBean(PostContructMethodHasParameterComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testPostConstructHasReturnType()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(PostContructMethodHasReturnTypeComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testPostConstructHasCheckedException()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(PostContructMethodHasCheckedExceptionComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testPostConstructHasStatic()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(PostContructMethodHasStaticComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testMoreThanOneAroundInvoke()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(MoreThanOneAroundInvokeComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testAroundInvokeWithSameMethodName()
    {
        clear();
        defineManagedBean(AroundInvokeWithSameMethodNameComponent.class);
        Bean<?> comp = getComponents().get(0);

        Assert.assertEquals(0, ((AbstractBean<?>) comp).getInterceptorStack().size());
    }

    @Test
    public void testAroundInvokeWithoutParameter()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(AroundInvokeWithoutParameterComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testAroundInvokeWithoutReturnType()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(AroundInvokeWithoutReturnTypeComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testAroundInvokeWithWrongReturnType()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(AroundInvokeWithWrongReturnTypeComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testAroundInvokeWithoutException()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(AroundInvokeWithoutExceptionComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testAroundInvokeWithStatic()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(AroundInvokeWithStaticMethodComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testAroundInvokeWithFinal()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(AroundInvokeWithFinalMethodComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testNoArgConstructorInterceptor()
    {
        try
        {
            clear();
            AbstractBean<?> component = defineManagedBean(NoArgConstructorInterceptorComponent.class);
            EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
        }
        catch (WebBeansConfigurationException e)
        {
            System.out.println("got expected exception: " + e.getMessage());
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

}
