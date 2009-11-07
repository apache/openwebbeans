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
package javax.enterprise.event;

import java.lang.annotation.Annotation;

import javax.enterprise.util.TypeLiteral;

/**
 * The event interface is used for firing events with specific classifiers and types.
 *
 * A built-in event bean is provided by the container.
 * 
 * @param <T> the event type 
 */
public interface Event<T>
{

    public void fire(T event);
    
    public Event<T> select(Annotation... qualifiers);
    
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers);
    
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers);

}
