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
package org.apache.webbeans.web.tests.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javassist.util.proxy.ProxyObjectOutputStream;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;

import junit.framework.Assert;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.web.failover.OwbProxyObjectInputStream;
import org.junit.Test;

public class BeanManagerSerialzationTest extends AbstractUnitTest {
	
	@Test
	public void testBeanManagerSerialization() throws IOException, ClassNotFoundException{
		
		Collection<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add(BeanInjectedWithBeanManager.class);
		
		startContainer(classes);
		
		Bean<?> bean = getBeanManager().getBeans(BeanInjectedWithBeanManager.class, new AnnotationLiteral<Default>(){}).iterator().next();
		Object instance = getBeanManager().getReference(bean, BeanInjectedWithBeanManager.class, getBeanManager().createCreationalContext(bean));
	    BeanInjectedWithBeanManager beanInstance = (BeanInjectedWithBeanManager) instance;
		//First ensure we got a valid instance, and it has an injected BeanManager
		Assert.assertTrue(beanInstance != null);
		Assert.assertTrue(beanInstance.testBeanManagerNonNull());
		
		//Write out the BeanInjectedWithBeanManager
		//TODO: Replace this with ObjectOutputStream if Javaassist is no longer used
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ProxyObjectOutputStream proxyOut = new ProxyObjectOutputStream(baos);
	    proxyOut.writeObject(instance);
	    
	    //Read it back in.  Worth noting that in a unittest environment we likely aren't matching tiered classloading behavior
	    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
	    OwbProxyObjectInputStream owbIS = new OwbProxyObjectInputStream(bais);
	    Object newInstance = owbIS.readObject();
	    BeanInjectedWithBeanManager newBeanInstance = (BeanInjectedWithBeanManager) newInstance;
	    
	    //Ensure the object we got back was not null, and the injected BeanManager is still nonNull
	    Assert.assertTrue(newBeanInstance != null); 
	    Assert.assertTrue(newBeanInstance.testBeanManagerNonNull());
	}

}
