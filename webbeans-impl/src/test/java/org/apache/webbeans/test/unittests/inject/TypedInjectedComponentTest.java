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
package org.apache.webbeans.test.unittests.inject;

import jakarta.enterprise.util.AnnotationLiteral;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.annotation.binding.Binding1;
import org.apache.webbeans.test.annotation.binding.Binding2;
import org.apache.webbeans.test.component.service.ITyped2;
import org.apache.webbeans.test.component.service.Typed2;
import org.apache.webbeans.test.component.service.TypedInjection;
import org.apache.webbeans.test.component.service.TypedInjectionWithoutArguments;
import org.junit.Test;

public class TypedInjectedComponentTest extends AbstractUnitTest
{
    @Test
    public void testTypedComponent() throws Throwable
    {
        startContainer(Typed2.class, TypedInjection.class);
        TypedInjection i = getInstance(TypedInjection.class);
        Typed2 typed2 = (Typed2)i.getV();
        typed2.setValue(true);


        Assert.assertTrue(i.getV() instanceof ITyped2);

        Typed2 obj2 = getInstance(Typed2.class,
                new AnnotationLiteral<Binding1>() {}, new AnnotationLiteral<Binding2>() {});

        Assert.assertSame(typed2.isValue(), obj2.isValue());
    }

    @Test
    public void testTypedComponentWithoutArgument() throws Throwable
    {
        startContainer(Typed2.class, TypedInjectionWithoutArguments.class);
        TypedInjectionWithoutArguments i = getInstance(TypedInjectionWithoutArguments.class);

        Typed2 typed2 = (Typed2)i.getV();
        typed2.setValue(true);

        Assert.assertTrue(i.getV() instanceof ITyped2);

        Typed2 obj2 = getInstance(Typed2.class,
                new AnnotationLiteral<Binding1>() {}, new AnnotationLiteral<Binding2>() {});

        Assert.assertSame(typed2.isValue(), obj2.isValue());
    }

}
