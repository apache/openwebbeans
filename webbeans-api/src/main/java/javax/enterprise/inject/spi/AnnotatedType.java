/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.enterprise.inject.spi;

import java.util.Set;

/**
 * Defines alternative meta data for bean class.
 * 
 * @version $Rev$ $Date$
 */
public interface AnnotatedType<X> extends Annotated
{
	/**
	 * Returns class of bean.
	 * 
	 * @return class of bean
	 */
	public Class<X> getJavaClass();
	
	/**
	 * Returns set of bean constructors.
	 * 
	 * @return set of constructors
	 */
	public Set<AnnotatedConstructor<X>> getConstructors();
	
	/**
	 * Returns set of bean methods.
	 * 
	 * @return set of bean methods
	 */
	public Set<AnnotatedMethod<? super X>> getMethods();
	
	/**
	 * Returns set of bean fields.
	 * 
	 * @return set of bean fields.
	 */
	public Set<AnnotatedField<? super X>> getFields();
}
