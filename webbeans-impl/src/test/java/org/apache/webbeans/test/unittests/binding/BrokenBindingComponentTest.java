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
package org.apache.webbeans.test.unittests.binding;

import org.apache.webbeans.test.component.binding.BindingWithNonBindingAnnotationTypeComponent;
import org.apache.webbeans.test.component.binding.BindingWithNonBindingArrayTypeComponent;
import org.apache.webbeans.test.servlet.TestContext;
import org.apache.webbeans.util.Asserts;
import org.junit.Before;
import org.junit.Test;

public class BrokenBindingComponentTest extends TestContext
{

    public BrokenBindingComponentTest()
    {
        super(BrokenBindingComponentTest.class.getName());
    }

    @Before
    public void init()
    {
        super.init();
    }

    @Test
    public void testNonBindingArrayType()
    {
        Exception exc = null;

        try
        {
            defineSimpleWebBean(BindingWithNonBindingArrayTypeComponent.class);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            exc = e;
        }

        Asserts.assertNotNull(exc);

    }

    @Test
    public void testNonBindingAnnotationType()
    {
        Exception exc = null;

        try
        {
            defineSimpleWebBean(BindingWithNonBindingAnnotationTypeComponent.class);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            exc = e;
        }

        Asserts.assertNotNull(exc);

    }

}
