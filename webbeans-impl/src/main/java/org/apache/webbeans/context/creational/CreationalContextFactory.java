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

import javax.context.CreationalContext;
import javax.inject.manager.Bean;

import org.apache.webbeans.config.WebBeansFinder;

public class CreationalContextFactory<T>
{
    private CreationalContextImpl<T> impl;
    
    public CreationalContextFactory()
    {
        impl = new CreationalContextImpl<T>();
    }
    
    @SuppressWarnings("unchecked")
    public static CreationalContextFactory getInstance()
    {
        return (CreationalContextFactory)WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_CREATIONAL_CONTEXT_FACTORY);
    }
    
    public CreationalContext<T> getCreationalContext(Bean<T> bean)
    {        
        return impl.getCreationalContextImpl(bean);   
    }
    
    public void clear()
    {
        impl.clear();
    }
}
