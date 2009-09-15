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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * An InjectionPoint object provides metadata information about an injection point.
 * An instance of InjectionPoint may represent one of the following types:
 * <ul>
 *  <li>an injected field</li>
 *  <li>a parameter of a bean constructor</li>
 *  <li>an initializer method</li>
 *  <li>a producer method</li>
 *  <li>a disposer method</li>
 *  <li>an observer method</li>
 * </ul>
 * 
 * @version $Rev$ $Date$
 */
public interface InjectionPoint
{
    /**
     * Returns required type of the injection point.
     * 
     * @return type of the injection point
     */
    public Type getType();

    /**
     * Returns required qualifiers of the injection point.
     * 
     * @return qualifiers at the injection point
     */
    public Set<Annotation> getQualifiers();

    /**
     * Returns the injection point owner bean.
     * <p>
     * If there is no bean for the injection point,
     * it returns null.
     * </p>
     * 
     * @return injection point owner bean
     */
    public Bean<?> getBean();

    /**
     * Returns appered point for injection point. One of
     * <ul>
     *  <li>{@link Field} object</li>
     *  <li>{@link Constructor} parameter</li>
     *  <li>{@link Method} producer method parameter</li>
     *  <li>{@link Method} disposal method parameter</li>
     *  <li>{@link Method} observer method parameter</li>
     * </ul>
     * 
     * @return where the injection point is appeared 
     */
    public Member getMember();
    
    /**
     * Returns annotated object representation of member.
     * 
     * @return annotated
     */
    public Annotated getAnnotated();
    
    /**
     * Returns true if injection point is decorator delegate,
     * false otherwise.
     * 
     * @return true if injection point is decorator delegate
     */
    public boolean isDelegate();
    
    /**
     * Returns true if injection point is transient,
     * false otherwise.
     * 
     * @return true if injection point is transient
     */
    public boolean isTransient();
}
