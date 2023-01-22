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
package org.apache.webbeans.test.unittests.decorator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;

import org.apache.webbeans.annotation.RequestedScopeLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.annotation.binding.DummyAnnotationLiteral;
import org.apache.webbeans.test.component.CheckWithCheckPaymentDecoratorField;
import org.apache.webbeans.test.component.IPayment;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeIsnotInterface;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeMustImplementAllDecoratedTypes;
import org.apache.webbeans.test.component.decorator.broken.MoreThanOneDelegateAttribute;
import org.apache.webbeans.test.component.decorator.broken.PaymentDecorator;
import org.junit.Test;

import jakarta.enterprise.inject.spi.DefinitionException;

public class DecoratorExceptionTest extends AbstractUnitTest
{
    @Test
    public void testDelegateAttributeIsnotInterface()
    {
        try
        {
            addDecorator(DelegateAttributeIsnotInterface.class);
            startContainer();
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testMoreThanOneDelegateAttribute()
    {
        try
        {
            addDecorator(MoreThanOneDelegateAttribute.class);
            startContainer();
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testApplyToSimpleWebBeanFinalMethodsDecoratorImplements()
    {
        try
        {
            addDecorator(PaymentDecorator.class);
            startContainer(CheckWithCheckPaymentDecoratorField.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testDelegateAttributeMustImplementAllDecoratedTypes()
    {
        try
        {
            addDecorator(DelegateAttributeMustImplementAllDecoratedTypes.class);
            startContainer();
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testResolveDuplicateBindingParameterType()
    {
        try
        {
            startContainer(IPayment.class);
            Set<Type> api = new HashSet<Type>();
            api.add(IPayment.class);

            Annotation[] anns = new Annotation[2];
            anns[0] = new DummyAnnotationLiteral();
            anns[1] = new DummyAnnotationLiteral();

            getBeanManager().resolveDecorators(api, anns);
            Assert.fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testResolveNonBindingTypeAnnotation()
    {
        try
        {
            startContainer(IPayment.class);
            Set<Type> api = new HashSet<Type>();
            api.add(IPayment.class);

            Annotation[] anns = new Annotation[2];
            anns[0] = new RequestedScopeLiteral();

            getBeanManager().resolveDecorators(api, anns);
            Assert.fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testResolveApiTypesEmpty()
    {
        try
        {
            startContainer();
            Set<Type> api = new HashSet<Type>();

            Annotation[] anns = new Annotation[2];
            anns[0] = new DummyAnnotationLiteral();

            getBeanManager().resolveDecorators(api, anns);
            Assert.fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException e)
        {
            return; // all ok
        }
    }

}
