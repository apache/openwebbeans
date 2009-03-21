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
package org.apache.webbeans.ejb.definition;

import java.lang.reflect.Type;
import java.util.Set;

import org.apache.webbeans.ejb.EjbTestContext;
import org.apache.webbeans.ejb.EjbType;
import org.apache.webbeans.ejb.component.EjbComponentImpl;
import org.apache.webbeans.ejb.util.Utility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EjbApiTypeTest extends EjbTestContext
{
    public EjbApiTypeTest()
    {
        super(EjbApiTypeTest.class.getName());
    }
    
    @Before
    public void init()
    {
        super.init();
    }
    
    @Test
    public void testBalkiApiType()
    {
        EjbComponentImpl<Balki> bean = defineEjbBean(Balki.class, EjbType.STATELESS);
        Utility.defineApiType(bean);
        
        Set<Type> types = bean.getTypes();
        
        Assert.assertEquals(2, types.size());
    }
    
    @Test
    public void testDefaultLocalBalkiApiType()
    {
        EjbComponentImpl<Balki_DefaultLocal> bean = defineEjbBean(Balki_DefaultLocal.class, EjbType.STATELESS);
        Utility.defineApiType(bean);
        
        Set<Type> types = bean.getTypes();
        
        Assert.assertEquals(2, types.size());
    }
    
    @Test
    public void testClassLocalBalkiApiType()
    {
        EjbComponentImpl<Balki_ClassLocal> bean = defineEjbBean(Balki_ClassLocal.class, EjbType.STATELESS);
        Utility.defineApiType(bean);
        
        Set<Type> types = bean.getTypes();
        
        Assert.assertEquals(2, types.size());
    }
    
    @Test
    public void testClassLocalViewBalkiApiType()
    {
        EjbComponentImpl<Balki_ClassView> bean = defineEjbBean(Balki_ClassView.class, EjbType.STATELESS);
        Utility.defineApiType(bean);
        
        Set<Type> types = bean.getTypes();
        
        Assert.assertEquals(2, types.size());
    }
    

}
