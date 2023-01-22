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
package org.apache.webbeans.test.unittests.pfield;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.CheckWithMoneyPayment;
import org.apache.webbeans.test.component.PaymentProcessorComponent;
import org.apache.webbeans.test.component.pfield.ProducerFieldDefinitionComponent;
import org.apache.webbeans.test.component.pfield.ProducerFieldDefinitionParameterized;
import org.apache.webbeans.test.component.pfield.ProducerFieldInjectedComponent;
import org.apache.webbeans.test.component.pfield.ProducerFieldInjectedWrongType;
import org.apache.webbeans.test.component.pfield.broken.TypeVariableProducerField;
import org.junit.Assert;
import org.junit.Test;

public class ProducerFieldComponentTest extends AbstractUnitTest
{
    @Test
    public void testInjectedProducerField()
    {
        startContainer(CheckWithCheckPayment.class, CheckWithMoneyPayment.class, PaymentProcessorComponent.class,
                ProducerFieldDefinitionComponent.class, ProducerFieldInjectedComponent.class);
        
        ProducerFieldDefinitionComponent defineComponentInstance = getInstance(ProducerFieldDefinitionComponent.class);
        Assert.assertNotNull(defineComponentInstance);
        Assert.assertTrue(defineComponentInstance.isExist());
        
        ProducerFieldInjectedComponent injectedComponentInstance = getInstance(ProducerFieldInjectedComponent.class);
        Assert.assertNotNull(injectedComponentInstance);
        Assert.assertNotNull(injectedComponentInstance.getPaymentProcessorName());
    }
    
    @Test(expected = DefinitionException.class)
    public void testInjectedProducerFieldIncorrectType()
    {
        startContainer(ProducerFieldDefinitionParameterized.class, ProducerFieldInjectedWrongType.class);

        ProducerFieldInjectedWrongType instance = getInstance(ProducerFieldInjectedWrongType.class);
        instance.getMyList();

    }
    
    @Test(expected = WebBeansConfigurationException.class)
    public void testProducerFieldTypeVariable()
    {
        startContainer(TypeVariableProducerField.class);
    }

}
