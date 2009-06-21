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
package javax.enterprise.inject.spi;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Initializer;

/**
 * Provides a generic operation for producing an instance of a type.
 * 
 * @version $Rev$ $Date$
 * 
 * <T> bean type
 */
public interface Producer<T> 
{
	/**
	 * Its result depends on bean type.
	 * 
	 * <p>
	 * 
	 * <ul>
	 * 
	 * 	<li><b>Bean Class</b> : It calls the constructor annotated with {@link Initializer} if it 
	 * exists, or the constructor with no parameters otherwise.</li>
	 *  
	 *  <li><b>Producer Method or Field</b> : Calls the producer method on, 
	 *  or accesses the producer field of, a contextual instance of the most 
	 *  specialized bean that specializes the bean that declares the producer method</li>
	 * 
	 * </ul>
	 * 
	 * </p>

	 * @param creationalContext creational context
	 * 
	 * @return an instance of bean
	 */
	public T produce(CreationalContext<T> creationalContext);
	
	/**
	 * Its result depends on bean type.
	 * <p>
	 * <ul>
	 * 	<li><b>Bean Class</b> : Does nothing.</li>
	 *  <li><b>Producer Method</b> : Calls disposer method or any other cleanup.
	 * </ul>
	 * </p>
	 * 
	 * @param instance dispose istance
	 */
	public void dispose(T instance);
	
	/**
	 * Its result depends on bean type.
	 * 
	 * <p>
	 * 
	 * <ul>
	 * 	<li><b>Bean Class</b> : Returns the set of InjectionPoint objects representing all injected fields, 
	 * bean constructor parameters and initializer method parameters.</li>
	 *
	 *  <li><b>Producer Method</b> : Returns the set of InjectionPoint objects 
	 *  representing all parameters of the producer method.</li>
	 *  
	 * </ul>
	 * 
	 * </p>
	 * 
	 * @return set of injection points
	 */
	public Set<InjectionPoint> getInjectionPoints();
	
	
}