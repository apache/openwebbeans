/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.context.creational;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.context.CreationalContext;
import javax.inject.manager.Bean;

/** {@inheritDoc} */
public class CreationalContextImpl<T> implements CreationalContext<T>
{
    private Map<Bean<?>,Object> incompleteInstancesMap = new ConcurrentHashMap<Bean<?>, Object>();

    private Bean<T> incompleteBean = null;
    
    CreationalContextImpl()
    {
        
    }
    
    protected CreationalContextImpl<T> getCreationalContextImpl(Bean<T> incompleteBean)
    {
        CreationalContextImpl<T> impl = new CreationalContextImpl<T>();        
        
        impl.incompleteBean = incompleteBean;
        impl.incompleteInstancesMap = this.incompleteInstancesMap;
        
        return impl;
        
    }
    
    public void push(T incompleteInstance)
    {
        this.incompleteInstancesMap.put(this.incompleteBean, incompleteInstance);
        
    }
    
    public Object get(Bean<?> incompleteBean)
    {
        return incompleteInstancesMap.get(incompleteBean);
    }
    
    public void remove(Bean<?> bean)
    {
        if(this.incompleteInstancesMap.containsKey(bean))
        {
            this.incompleteInstancesMap.remove(bean);   
        }
    }
    
    public void clear()
    {
        this.incompleteInstancesMap.clear();
    }

}