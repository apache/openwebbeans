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


import java.lang.annotation.Annotation;
import java.util.Set;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.binding.AnyBindingComponent;
import org.apache.webbeans.test.component.binding.DefaultAnyBinding;
import org.apache.webbeans.test.component.binding.NonAnyBindingComponent;
import org.junit.Assert;
import org.junit.Test;

import jakarta.enterprise.inject.spi.Bean;

public class AnyBindingTest extends AbstractUnitTest {

    @Test
    public void testAny()
    {
        startContainer(AnyBindingComponent.class, NonAnyBindingComponent.class, DefaultAnyBinding.class);

        Bean<AnyBindingComponent> comp1 = getBean(AnyBindingComponent.class, AnyLiteral.INSTANCE);
        Set<Annotation> qualifiers = comp1.getQualifiers();

        Assert.assertEquals(2, qualifiers.size());

        Bean<NonAnyBindingComponent> comp2 = getBean(NonAnyBindingComponent.class, AnyLiteral.INSTANCE);
        qualifiers = comp2.getQualifiers();

        Assert.assertEquals(4, qualifiers.size());


        Bean<DefaultAnyBinding> comp3 = getBean(DefaultAnyBinding.class, AnyLiteral.INSTANCE);
        qualifiers = comp3.getQualifiers();

        Assert.assertEquals(2, qualifiers.size());
    }
}
