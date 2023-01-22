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
package org.apache.webbeans.test.unittests.specializes;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.annotation.binding.Asynchronous;
import org.apache.webbeans.test.component.specializes.AsynhrounousSpecalizesService;
import org.apache.webbeans.test.component.specializes.SpecializesServiceInjectorComponent;
import org.junit.Assert;
import org.junit.Test;

import jakarta.enterprise.util.AnnotationLiteral;

public class SpecializationComponentTest extends AbstractUnitTest
{
    @Test
    public void testMockService()
    {
        startContainer(AsynhrounousSpecalizesService.class, SpecializesServiceInjectorComponent.class);
        
        AsynhrounousSpecalizesService instanceOther
                = getInstance(AsynhrounousSpecalizesService.class, new AnnotationLiteral<Asynchronous>() {});
        Assert.assertNotNull(instanceOther);

        SpecializesServiceInjectorComponent instance = getInstance(SpecializesServiceInjectorComponent.class);
        Assert.assertNotNull(instance);
    }
}
