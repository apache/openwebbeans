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
package org.apache.webbeans.ejb.definition.apitype;

import java.lang.reflect.Type;
import java.util.Set;

import org.apache.webbeans.ejb.EjbTestContext;
import org.apache.webbeans.ejb.common.util.EjbDefinitionUtility;
import org.apache.webbeans.ejb.component.OpenEjbBean;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class EjbApiTypeTest extends EjbTestContext
{
    public EjbApiTypeTest()
    {
        super(EjbApiTypeTest.class.getName());
    }
    
    @BeforeClass
    public static void init()
    {
        initEjb();
    }
    
    @AfterClass
    public static void after()
    {
        destroyEjb();
    }
    
    @Test
    public void testBalkiApiType()
    {
        OpenEjbBean<Balki> bean = defineEjbBean(Balki.class);
        EjbDefinitionUtility.defineApiType(bean);
        
        Set<Type> types = bean.getTypes();
        
        Assert.assertEquals(2, types.size());
    }
    
    @Test
    public void testDefaultLocalBalkiApiType()
    {
        OpenEjbBean<Balki_DefaultLocal> bean = defineEjbBean(Balki_DefaultLocal.class);
        EjbDefinitionUtility.defineApiType(bean);
        
        Set<Type> types = bean.getTypes();
        
        Assert.assertEquals(2, types.size());
    }
    
    @Test
    public void testClassLocalBalkiApiType()
    {
        OpenEjbBean<Balki_ClassLocal> bean = defineEjbBean(Balki_ClassLocal.class);
        EjbDefinitionUtility.defineApiType(bean);
        
        Set<Type> types = bean.getTypes();
        
        Assert.assertEquals(2, types.size());
    }
    
    @Test
    public void testClassLocalViewBalkiApiType()
    {
        OpenEjbBean<Balki_ClassView> bean = defineEjbBean(Balki_ClassView.class);
        EjbDefinitionUtility.defineApiType(bean);
        
        Set<Type> types = bean.getTypes();
        
        Assert.assertEquals(1, types.size());
    }
    

}
