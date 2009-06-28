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
package javax.enterprise.inject;

import java.lang.annotation.Annotation;

/**
 * The <code>Instance</code> interface provides a method for obtaining 
 * instances of beans with required types and bindings.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean required type
 */
public interface Instance<T> extends Iterable<T>
{
    /**
     * Returns bean instance with required type 
     * and required binding type that are defined
     * at the injection point.
     * 
     * @return bean instance with required type and required binding type
     */
    public T get();
    
    /**
     * Creates new <code>Instance</code> with given
     * binding annotations. 
     * 
     * @param bindings
     * @return new child instance with given binding types.
     */
    public Instance<T> select(Annotation... bindings);
    
    /**
     * Returns new child instance with given class and binding types.
     * 
     * @param <U> subtype info
     * @param subtype subtype class
     * @param bindings binding types
     * @return new child instance with given class and binding types
     */
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... bindings);
    
    /**
     * Return new child instance with given class info and binding types.
     * 
     * @param <U> subtype info
     * @param subtype subtype class
     * @param bindings binding types
     * @return new child instance with given class info and binding types
     */
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... bindings);
    
    /**
     * Return true if resulotion is unsatisfied, false otherwise.
     * 
     * @return true if resulotion is unsatisfied, false otherwise
     */
    public boolean isUnsatisfied();

    /**
     * Returns true if resolution is ambigious, false otherwise.
     * 
     * @return true if resolution is ambigious, false otherwise.
     */
    public boolean isAmbiguous();


}