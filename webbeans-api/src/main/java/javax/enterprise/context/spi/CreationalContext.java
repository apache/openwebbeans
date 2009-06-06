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
package javax.enterprise.context.spi;

/**
 * The CreationalContext holds incomplete Bean instances. This may be caused by
 * a situation like in the following example: <code>
 * &#x0040;ApplicationScoped class Foo 
 * { 
 *   &#x0040;Current Bar _bar; 
 * }
 * 
 * &#x0040;ApplicationScoped class Bar 
 * { 
 *   &#x0040;Current Foo _bar; 
 * } 
 * </code>
 * 
 * <p>
 * Generally it is used for prohibiting the circular references of the webbeans.
 * </p>
 * 
 */
public interface CreationalContext<T>
{
    /**
     * Puts new incomplete instance into the creational context.
     * 
     * @param incompleteInstance incomplete webbeans instance
     */
    public void push(T incompleteInstance);

}
