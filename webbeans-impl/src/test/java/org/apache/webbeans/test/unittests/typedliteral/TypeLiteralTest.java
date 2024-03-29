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
package org.apache.webbeans.test.unittests.typedliteral;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.literals.InstanceTypeLiteralBean;
import org.apache.webbeans.test.component.literals.InstanceTypeLiteralBean.IntegerOrder;
import org.apache.webbeans.test.component.literals.InstanceTypeLiteralBean.StringOrder;
import org.junit.Test;

public class TypeLiteralTest extends AbstractUnitTest
{
    public static class Literal1 extends TypeLiteral<Map<String, String>>
    {
        
    }
    
    public TypeLiteral<List<Integer>> literal2 = new TypeLiteral<List<Integer>>(){};
    
    @Test
    public void testLiterals()
    {
        Literal1 literal1 = new Literal1();
        literal1.getRawType().equals(Map.class);
        
        literal2.getRawType().equals(List.class);
    }
    
    @Test
    public void testTypeLiteralInInstance()
    {
        startContainer(StringOrder.class, IntegerOrder.class, InstanceTypeLiteralBean.class);
        
        InstanceTypeLiteralBean beaninstance = getInstance(InstanceTypeLiteralBean.class);
        Object produce = beaninstance.produce(0);
        Assert.assertTrue(produce instanceof Instance);
        
        Instance<IntegerOrder> order = (Instance<IntegerOrder>)produce;
        Assert.assertTrue(order.get() instanceof IntegerOrder);
        
        produce = beaninstance.produce(1);
        Assert.assertTrue(produce instanceof Instance);

        Instance<StringOrder> order2 = (Instance<StringOrder>)produce;
        Assert.assertTrue(order2.get() instanceof StringOrder);
    }
}
