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
package org.apache.webbeans.test.unittests.intercept;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Assert;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.test.component.intercept.InterceptedComponent;
import org.apache.webbeans.test.component.intercept.InterceptorWithSuperClassInterceptedComponent;
import org.apache.webbeans.test.component.intercept.MultipleInterceptedComponent;
import org.apache.webbeans.test.component.intercept.MultipleListOfInterceptedComponent;
import org.apache.webbeans.test.component.intercept.MultipleListOfInterceptedWithExcludeClassComponent;
import org.junit.Test;

public class EJBInterceptComponentTest extends AbstractUnitTest
{

    @Test
    public void testInterceptedComponent()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(InterceptedComponent.class);
        startContainer(beanClasses, null);


        shutDownContainer();
    }

    @Test
    public void testInterceptorCalls()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(InterceptedComponent.class);
        startContainer(beanClasses, null);

        InterceptedComponent comp = getInstance(InterceptedComponent.class);
        Object s = comp.hello(null);

        Assert.assertEquals(new Integer(5), s);

        shutDownContainer();
    }

    @Test
    public void testMultipleInterceptedComponent()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(MultipleInterceptedComponent.class);
        startContainer(beanClasses, null);

        MultipleInterceptedComponent comp = getInstance(MultipleInterceptedComponent.class);
        Object obj = comp.intercepted();

        Assert.assertTrue(obj instanceof String[]);

        String[] arr = (String[]) obj;

        Assert.assertEquals(2, arr.length);
        Assert.assertTrue("key".equals(arr[0]) && "key2".equals(arr[1]) || "key".equals(arr[1]) && "key2".equals(arr[0]));

        shutDownContainer();;
    }

    @Test
    public void testInterceptorWithSuperClassComponent()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(InterceptorWithSuperClassInterceptedComponent.class);
        startContainer(beanClasses, null);

        InterceptorWithSuperClassInterceptedComponent comp = getInstance(InterceptorWithSuperClassInterceptedComponent.class);
        Object obj = comp.intercepted();

        Assert.assertTrue(obj instanceof String[]);

        String[] arr = (String[]) obj;

        Assert.assertEquals(1, arr.length);
        Assert.assertTrue("key0".equals(arr[0]));

        shutDownContainer();
    }

    @Test
    public void testMultipleListOfInterceptedComponent()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(MultipleListOfInterceptedComponent.class);
        startContainer(beanClasses, null);

        MultipleListOfInterceptedComponent comp = getInstance(MultipleListOfInterceptedComponent.class);
        Object obj = comp.intercepted();

        Assert.assertTrue(obj instanceof String);

        Assert.assertEquals("ok", (String) obj);

        shutDownContainer();
    }

    @Test
    public void testMultipleListOfInterceptedWithExcludeClassComponent()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(MultipleListOfInterceptedWithExcludeClassComponent.class);
        startContainer(beanClasses, null);

        MultipleListOfInterceptedWithExcludeClassComponent comp = getInstance(MultipleListOfInterceptedWithExcludeClassComponent.class);
        Object obj = comp.intercepted();

        Assert.assertTrue(obj instanceof String);

        Assert.assertEquals("value2", (String) obj);

        shutDownContainer();
    }

}
