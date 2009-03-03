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
package org.apache.webbeans.test.unittests;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Current;
import javax.inject.manager.Manager;
import javax.servlet.ServletContext;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.test.component.service.ITyped;
import org.apache.webbeans.test.component.service.TypedComponent;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;

public class TypedComponentTest extends TestContext
{
    Manager container = null;
    ITyped<String> s = null;

    public TypedComponentTest()
    {
        super(TypedComponentTest.class.getSimpleName());
    }

    public void endTests(ServletContext ctx)
    {

    }

    @Before
    public void init()
    {
        super.init();
        this.container = ManagerImpl.getManager();
    }

    public void startTests(ServletContext ctx)
    {

    }

    @Test
    public void testTypedComponent() throws Throwable
    {
        clear();
        defineSimpleWebBean(TypedComponent.class);
        List<AbstractComponent<?>> list = getComponents();

        @SuppressWarnings("unused")
        AbstractComponent<?> itype = (AbstractComponent<?>) container.resolveByType(TypedComponentTest.class.getDeclaredField("s").getType(), new Current()
        {

            public Class<? extends Annotation> annotationType()
            {

                return Current.class;
            }

        }).iterator().next();

        Assert.assertTrue(list.size() > 0 ? true : false);
    }

}
