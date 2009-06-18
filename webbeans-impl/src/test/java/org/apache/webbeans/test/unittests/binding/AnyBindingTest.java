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
package org.apache.webbeans.test.unittests.binding;


import java.lang.annotation.Annotation;
import java.util.Set;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.test.component.binding.AnyBindingComponent;
import org.apache.webbeans.test.component.binding.DefaultAnyBinding;
import org.apache.webbeans.test.component.binding.NonAnyBindingComponent;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AnyBindingTest extends TestContext{

	public AnyBindingTest()
	{
		super(AnyBindingTest.class.getName());
	}
	
	@Before
	public void init()
	{
		initDefaultDeploymentTypes();
	}
	
	@Test
	public void testAny()
	{
		AbstractComponent<AnyBindingComponent> comp1 = defineSimpleWebBean(AnyBindingComponent.class);
		Set<Annotation> bindings = comp1.getBindings();
		
		Assert.assertEquals(2, bindings.size());
		
		AbstractComponent<NonAnyBindingComponent> comp2 = defineSimpleWebBean(NonAnyBindingComponent.class);
		bindings = comp2.getBindings();
		
		Assert.assertEquals(4, bindings.size());
		

		AbstractComponent<DefaultAnyBinding> comp3 = defineSimpleWebBean(DefaultAnyBinding.class);
		bindings = comp3.getBindings();
		
		Assert.assertEquals(2, bindings.size());

		
	}
}
