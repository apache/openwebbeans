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

import java.lang.reflect.Member;

/**
 * Defines annotated members common contract. 
 * 
 * <p>
 * Annotated members could be one of the followings
 * <ul>
 *  <li>Class Fields</li>
 *  <li>Class Methods</li>
 *  <li>Class Constructors</li>
 *  <li>Constructor or method Parameters</li>
 * </ul>
 * </p>
 * fields, constructors, methods.
 * 
 * @version $Rev$ $Date$
 * 
 */
public interface AnnotatedMember<X> extends Annotated 
{
	/**
	 * Returns base java member.
	 * 
	 * @return java member
	 */
	public Member getJavaMember();
	
	/**
	 * Returns true if member modifiers contain static keyword
	 * false otherwise.
	 * 
	 * @return true if member modifiers contain static keyword
	 */
	public boolean isStatic();
	
	/**
	 * Returns member's declaring type.
	 * 
	 * @return member's declaring type
	 */
	public AnnotatedType<X> getDeclaringType();

}