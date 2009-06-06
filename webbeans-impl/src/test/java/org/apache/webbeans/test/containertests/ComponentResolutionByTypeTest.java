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
package org.apache.webbeans.test.containertests;

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;

import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.test.annotation.binding.AnnotationWithBindingMember;
import org.apache.webbeans.test.annotation.binding.AnnotationWithNonBindingMember;
import org.apache.webbeans.test.component.BindingComponent;
import org.apache.webbeans.test.component.NonBindingComponent;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Test;

public class ComponentResolutionByTypeTest extends TestContext
{
    public @AnnotationWithBindingMember(value = "B", number = 3)
    BindingComponent s1 = null;
    public @AnnotationWithBindingMember(value = "B")
    BindingComponent s2 = null;

    public @AnnotationWithNonBindingMember(value = "B", arg1 = "arg1", arg2 = "arg2")
    NonBindingComponent s3 = null;
    public @AnnotationWithNonBindingMember(value = "B", arg1 = "arg11", arg2 = "arg21")
    NonBindingComponent s4 = null;
    public @AnnotationWithNonBindingMember(value = "C", arg1 = "arg11", arg2 = "arg21")
    NonBindingComponent s5 = null;

    private static BeanManager cont = ManagerImpl.getManager();
    private static final String CLAZZ_NAME = ComponentResolutionByTypeTest.class.getName();

    public ComponentResolutionByTypeTest()
    {
        super(CLAZZ_NAME);
    }

    public void init()
    {

    }

    public void endTests(ServletContext ctx)
    {
        cont = null;
    }

    public void startTests(ServletContext ctx)
    {

        try
        {
            testBindingTypeNonOk();
            testBindingTypeOk();
            testNonBindingTypeNonOk();
            testNonBindingTypeOk1();
            testNonBindingTypeOk2();

        }
        catch (Throwable e)
        {

        }
    }

    @Test
    public void startTests()
    {

        try
        {
            testBindingTypeNonOk();
            testBindingTypeOk();
            testNonBindingTypeNonOk();
            testNonBindingTypeOk1();
            testNonBindingTypeOk2();

        }
        catch (Throwable e)
        {

        }
    }

    public void testBindingTypeOk() throws Throwable
    {
        try
        {
            cont.resolveByType(BindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s1").getAnnotations());
            pass("testBindingTypeOk");

        }
        catch (Throwable e)
        {
            fail("testBindingTypeOk");

        }

    }

    public void testBindingTypeNonOk() throws Throwable
    {
        try
        {
            cont.resolveByType(BindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s2").getAnnotations());
            fail("testBindingTypeNonOk");

        }
        catch (Throwable e)
        {
            pass("testBindingTypeNonOk");

        }

    }

    public void testNonBindingTypeOk1() throws Throwable
    {
        try
        {
            cont.resolveByType(NonBindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s3").getAnnotations());
            pass("testNonBindingTypeOk1");

        }
        catch (Throwable e)
        {
            fail("testNonBindingTypeOk1");

        }

    }

    public void testNonBindingTypeOk2() throws Throwable
    {
        try
        {
            cont.resolveByType(NonBindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s4").getAnnotations());
            pass("testNonBindingTypeOk2");

        }
        catch (Throwable e)
        {
            fail("testNonBindingTypeOk2");

        }

    }

    public void testNonBindingTypeNonOk() throws Throwable
    {
        try
        {
            cont.resolveByType(NonBindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s5").getAnnotations());
            fail("testNonBindingTypeNonOk");

        }
        catch (Throwable e)
        {
            pass("testNonBindingTypeNonOk");

        }

    }

}
