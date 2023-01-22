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
package org.apache.webbeans.test.interceptors.factory.beans;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Named;

import org.apache.webbeans.test.component.intercept.webbeans.bindings.Secure2;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.Transactional;

public class InterceptionFactoryBeansProducer {
	
	private static class Secure2Literal extends AnnotationLiteral<Secure2> implements Secure2 {
		
		@Override
		public String[] rolesAllowed() {
			return new String[0];
		}
	}
	
	private final class UnproxyableClass implements InterfaceWithoutInterceptors {
		
		private final String name;
		
		public UnproxyableClass(String name) {
			this.name = name;
		}
		
		@Override
		public String getName() {
			return name;
		}
	}
	
	@Produces
	@Named("noInterceptorOnTheInterface")
	public InterfaceWithoutInterceptors noInterceptorOnTheInterface(InterceptionFactory<InterfaceWithoutInterceptors> factory) {
		return factory.createInterceptedInstance(new UnproxyableClass("dummy"));
	}
	
	@Produces
	@Named("interceptorsOnTheInterface")
	public InterfaceWithInterceptors interceptorsOnTheInterface(InterceptionFactory<InterfaceWithInterceptors> factory) {
		return factory.createInterceptedInstance(() -> "dummy");
	}
	
	@Produces
	@Named("noInterceptorOnTheInterfacePlusProgrammaticallyAddedBindings")
	public InterfaceWithoutInterceptors noInterceptorOnTheInterfacePlusProgrammaticallyAddedBindings(InterceptionFactory<InterfaceWithoutInterceptors> factory) {
		factory.configure().add(new AnnotationLiteral<Transactional>() {});
		return factory.createInterceptedInstance(new UnproxyableClass("dummy"));
	}
	
	@Produces
	@Named("interceptorsOnTheInterfacePlusProgrammaticallyAddedBindings")
	public InterfaceWithInterceptors interceptorsOnTheInterfacePlusProgrammaticallyAddedBindings(InterceptionFactory<InterfaceWithInterceptors> factory) {
		factory.configure().add(new Secure2Literal());
		return factory.createInterceptedInstance(() -> "dummy");
	}
}
