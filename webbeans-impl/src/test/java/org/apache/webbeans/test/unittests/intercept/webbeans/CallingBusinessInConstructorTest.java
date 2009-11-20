/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.test.unittests.intercept.webbeans;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.RequestScoped;

import junit.framework.Assert;

import org.apache.webbeans.common.AbstractUnitTest;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.RequestContext;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.test.component.intercept.webbeans.CallBusinessInConstructorBean;
import org.apache.webbeans.test.component.intercept.webbeans.SecureInterceptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CallingBusinessInConstructorTest extends AbstractUnitTest
{
    public CallingBusinessInConstructorTest()
    {
        super();
    }
    
    @Before
    public void init()
    {
        SecureInterceptor.CALL = false;
    }
    
    @After
    public void after()
    {
        SecureInterceptor.CALL = false;
    }
    
    @Test
    public void testCallBusinessInConstructor()
    {
        ContextFactory.initRequestContext(null);
        
        // interceptors must be enabled via XML. We fake this by adding our interceptor manually.
        InterceptorsManager.getInstance().addNewInterceptor(SecureInterceptor.class);

        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(SecureInterceptor.class);
        classes.add(CallBusinessInConstructorBean.class);
        startContainer(classes);

        CallBusinessInConstructorBean instance = (CallBusinessInConstructorBean) getInstance(CallBusinessInConstructorBean.class);
        //getingetInstanceByName("callBusinessInConstructorBean");
        
        Assert.assertNotNull(instance);
        
        Assert.assertTrue(SecureInterceptor.CALL);
        
        ContextFactory.destroyRequestContext(null);
        
        SecureInterceptor.CALL = false;
        
        ContextFactory.initRequestContext(null);
        
        RequestContext ctx = (RequestContext) ContextFactory.getStandardContext(RequestScoped.class);
                
        instance = getInstanceByName(CallBusinessInConstructorBean.class, "callBusinessInConstructorBean");
        
        Assert.assertNotNull(instance);
        
        Assert.assertTrue(!SecureInterceptor.CALL);
        
        ContextFactory.destroyRequestContext(null);
        
    }

}
