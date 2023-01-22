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
package org.apache.webbeans.test.unittests.binding;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.binding.BindingWithNonBindingAnnotationTypeComponent;
import org.apache.webbeans.test.component.binding.BindingWithNonBindingArrayTypeComponent;
import org.junit.Test;

import jakarta.enterprise.inject.spi.DefinitionException;

public class BrokenBindingComponentTest extends AbstractUnitTest
{
    @Test
    public void testNonBindingArrayType()
    {
        try
        {
            startContainer(BindingWithNonBindingArrayTypeComponent.class);
        }
        catch (DefinitionException e)
        {
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

    @Test
    public void testNonBindingAnnotationType()
    {
        try
        {
            startContainer(BindingWithNonBindingAnnotationTypeComponent.class);
        }
        catch (DefinitionException e)
        {
            return; // all ok!
        }
        Assert.fail("expecting an exception!");
    }

}
