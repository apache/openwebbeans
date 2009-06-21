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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Describes annotated member properties.
 * 
 * @version $Rev$ $Date$
 */
public interface Annotated 
{
	/**
	 * Returns type of the element.
	 * 
	 * @return type of the element
	 */
	public Type getBaseType();
	
	/**
	 * Returns set of type closures. Type closure means
	 * that {@link Annotated#getBaseType()} is assignable.
	 * 
	 * @return set of type closures.
	 */
	public Set<Type> getTypeClosure();
	
	/**
	 * Gets annotated element's annotation member if exist, null otherwise
	 * 
	 * @param <T> generic annotatation class type
	 * @param annotationType class of the annotation
	 * @return annotated element's annotation member if exist, null otherwise
	 */
	public <T extends Annotation> T getAnnotation(Class<T> annotationType);
	
	/**
	 * Gets annotated member all annotations.
	 * 
	 * @return annotated member annotations
	 */
	public Set<Annotation> getAnnotations();
	
	/**
	 * Returns true if annotated member has annotation for given annotation type,
	 * false otherwise.
	 * 
	 * @param annotationType type of the annotation
	 * @return true if annotated member has annotation for given annotation type
	 */
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType);

}