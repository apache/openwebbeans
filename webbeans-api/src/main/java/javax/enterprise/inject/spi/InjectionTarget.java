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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.spi.CreationalContext;

/**
 * Provides operations for performing dependency injection and lifecycle
 * callbacks on an instance of a type.
 * 
 * @version $Rev$ $Date$ 
 * 
 * <T> bean type
 */
public interface InjectionTarget<T> extends Producer<T>
{
    /**
     * Performs dependency injection upon the given object.
     * 
     * @param instance bean instance
     * @param ctx creational context
     */
    public void inject(T instance, CreationalContext<T> ctx);
    
    /**
     * Calls {@link PostConstruct} callback method if exists.
     * 
     * @param instance bean instance
     */
    public void postConstruct(T instance);

    /**
     * Calls {@link PreDestroy} callback method if exists.
     * 
     * @param instance bean instance
     */    
    public void preDestroy(T instance);
    
}