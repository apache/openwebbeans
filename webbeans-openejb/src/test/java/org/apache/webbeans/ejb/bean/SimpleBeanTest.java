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
package org.apache.webbeans.ejb.bean;

import junit.framework.Assert;

import org.apache.webbeans.ejb.EjbPlugin;
import org.apache.webbeans.ejb.EjbTestContext;
import org.apache.webbeans.ejb.component.OpenEjbBean;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.generics.GProcessAnnotatedType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleBeanTest extends EjbTestContext
{
    public SimpleBeanTest()
    {
        super(SimpleBeanTest.class.getName());
    }
    
    @BeforeClass
    public static void setup()
    {
        initEjb();
    }
    
    @AfterClass
    public static void destroy()
    {
        destroyEjb();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLocalMethod()
    {
        EjbPlugin plugin = new EjbPlugin();
        boolean value = plugin.isSessionBean(SimpleBean.class);
        
        Assert.assertTrue(value);
        
        GProcessAnnotatedType annotatedType = new GProcessAnnotatedType(AnnotatedElementFactory.getInstance().newAnnotatedType(SimpleBean.class));
        
        OpenEjbBean<SimpleBean> bean = (OpenEjbBean<SimpleBean>)plugin.defineSessionBean(SimpleBean.class, annotatedType);
        Assert.assertNotNull(bean);
        
        
        
    }
}
