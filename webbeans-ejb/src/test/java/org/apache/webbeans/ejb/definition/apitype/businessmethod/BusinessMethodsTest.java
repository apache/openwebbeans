/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.ejb.definition.apitype.businessmethod;

import org.apache.webbeans.ejb.EjbTestContext;
import org.apache.webbeans.ejb.component.EjbComponentImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BusinessMethodsTest extends EjbTestContext
{
    public BusinessMethodsTest()
    {
        super(BusinessMethodsTest.class.getName());
    }

    @Before
    public void init()
    {
        super.init();
    }
    
    @Test
    public void testBusinessMethod()
    {
        EjbComponentImpl<Hairy> hairy = defineEjbBean(Hairy.class);
        
        Assert.assertEquals(3, hairy.getBusinessMethods().size());
    }
    
    @Test
    public void testBusinessMethodLocalView()
    {
        EjbComponentImpl<Hairy_LocalView> hairy = defineEjbBean(Hairy_LocalView.class);
        
        Assert.assertEquals(4, hairy.getBusinessMethods().size());
    }
    
}
