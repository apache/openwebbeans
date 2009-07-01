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

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Provides a generic operation for producing an instance of a type.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean instance type info
 */
public interface Producer<T> 
{
    /**
     * Returns instance of a bean.
     * 
     * @param ctx creational context to attach and destroy dependents
     * @return instance of a bean
     */
    public T produce(CreationalContext<T> ctx);

    /**
     * Dispose the bean instance.
     * 
     * @param instance disposed instance of bean
     */
    public void dispose(T instance);
    
    /**
     * Returns bean's set of injection points
     * 
     * @return bean's set of injection points
     */
    public Set<InjectionPoint> getInjectionPoints();
}