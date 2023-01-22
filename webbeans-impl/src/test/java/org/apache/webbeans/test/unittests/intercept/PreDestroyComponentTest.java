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

import jakarta.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.PreDestroyComponent;
import org.junit.Test;

public class PreDestroyComponentTest extends AbstractUnitTest
{

    @SuppressWarnings("unchecked")
    @Test
    public void testTypedComponent() throws Throwable
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(CheckWithCheckPayment.class);
        beanClasses.add(PreDestroyComponent.class);

        startContainer(beanClasses, null);

        CheckWithCheckPayment object = getInstance(CheckWithCheckPayment.class, new AnyLiteral());
        PreDestroyComponent object2 = getInstance(PreDestroyComponent.class);
        
        object2.getP();

        Assert.assertTrue(object instanceof CheckWithCheckPayment);
        Assert.assertTrue(object2 instanceof PreDestroyComponent);

        PreDestroyComponent pcc = object2;
        CheckWithCheckPayment payment = (CheckWithCheckPayment) pcc.getP();
        payment.setValue(true);


        Assert.assertNotNull(pcc.getP());
        Assert.assertSame(object.getValue(), payment.getValue());

        Assert.assertFalse(PreDestroyComponent.isDestroyed());

        getLifecycle().getContextService().endContext(RequestScoped.class, null);

        Assert.assertTrue(PreDestroyComponent.isDestroyed());
    }

}
