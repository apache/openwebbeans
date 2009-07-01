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

import javax.enterprise.context.spi.CreationalContext;

/**
 * Injection related operations on the bean instance.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean instance type info
 */
public interface InjectionTarget<T> extends Producer<T>
{
    /**
     * Does injection on the bean instance.
     * 
     * @param instance bean instance
     * @param ctx creational context
     */
    public void inject(T instance, CreationalContext<T> ctx);
    
    /**
     * Calls <code>@PostConstruct</code> method if it has one.
     * 
     * @param instance bean instance
     */
    public void postConstruct(T instance);

    /**
     * Calls <code>@PreDestroy</code> method if it has one.
     * 
     * @param instance bean instance
     */    
    public void preDestroy(T instance);
}