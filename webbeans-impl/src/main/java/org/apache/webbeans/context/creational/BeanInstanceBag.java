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
package org.apache.webbeans.context.creational;

import javax.enterprise.context.spi.CreationalContext;

public class BeanInstanceBag<T>
{
    private final CreationalContext<T> beanCreationalContext;
    
    private volatile T beanInstance;
    
    public BeanInstanceBag(CreationalContext<T> beanCreationalContext)
    {
        this.beanCreationalContext = beanCreationalContext;
    }

    /**
     * @return the beanCreationalContext
     */
    public CreationalContext<T> getBeanCreationalContext()
    {
        return beanCreationalContext;
    }
    
    

    /**
     * @param beanInstance the beanInstance to set
     */
    public void setBeanInstance(T beanInstance)
    {
        this.beanInstance = beanInstance;
    }

    /**
     * @return the beanInstance
     */
    public T getBeanInstance()
    {
        return beanInstance;
    }

}
