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
package org.apache.webbeans.test.interceptors.factory;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.interceptors.factory.beans.InterceptionFactoryBeansProducer;
import org.apache.webbeans.test.interceptors.factory.beans.InterfaceWithInterceptors;
import org.apache.webbeans.test.interceptors.factory.beans.InterfaceWithoutInterceptors;
import org.apache.webbeans.test.interceptors.factory.beans.Secure2Interceptor;
import org.apache.webbeans.test.interceptors.factory.beans.TransactionalInterceptor;
import org.junit.Assert;
import org.junit.Test;

public class InterceptionFactoryOnInterfacesTest  extends AbstractUnitTest
{

    @Test
    public void testNoInterceptorOnTheInterface() throws Exception
    {
    	startContainer(InterceptionFactoryBeansProducer.class);
    	InterfaceWithoutInterceptors instance = getInstance("noInterceptorOnTheInterface");
    	Assert.assertEquals("dummy", instance.getName()); // regular method
    	Assert.assertEquals("John Doe", instance.getDefaultName()); // default method
    	shutDownContainer();
    }
	
    @Test
    public void testInterceptorsOnTheInterface() throws Exception
    {
    	addInterceptor(TransactionalInterceptor.class);
    	startContainer(InterceptionFactoryBeansProducer.class);
    	InterfaceWithInterceptors instance = getInstance("interceptorsOnTheInterface");
    	Assert.assertEquals("intercepted dummy", instance.getName()); // regular method
    	Assert.assertEquals("intercepted John Doe", instance.getDefaultName()); // default method
    	shutDownContainer();
    }
    
    @Test
    public void testNoInterceptorOnTheInterfacePlusProgrammaticallyAddedBindings() throws Exception
    {
    	addInterceptor(TransactionalInterceptor.class);
    	startContainer(InterceptionFactoryBeansProducer.class);
    	InterfaceWithoutInterceptors instance = getInstance("noInterceptorOnTheInterfacePlusProgrammaticallyAddedBindings");
    	Assert.assertEquals("intercepted dummy", instance.getName()); // regular method
    	Assert.assertEquals("intercepted John Doe", instance.getDefaultName()); // default method
    	shutDownContainer();
    }
    
    @Test
    public void testInterceptorsOnTheInterfacePlusProgrammaticallyAddedBindings() throws Exception
    {
    	addInterceptor(TransactionalInterceptor.class);
    	addInterceptor(Secure2Interceptor.class);
    	startContainer(InterceptionFactoryBeansProducer.class);
    	InterfaceWithInterceptors instance = getInstance("interceptorsOnTheInterfacePlusProgrammaticallyAddedBindings");
    	Assert.assertEquals("intercepted dummy secured", instance.getName()); // regular method
    	Assert.assertEquals("intercepted John Doe secured", instance.getDefaultName()); // default method
    	shutDownContainer();
    }
}
