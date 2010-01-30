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
package org.apache.webbeans.lifecycle;

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ServiceLoader;

public final class LifecycleFactory
{
    public LifecycleFactory()
    {
        
    }
    
    public static LifecycleFactory getInstance()
    {
        LifecycleFactory currentLifecycle = (LifecycleFactory)WebBeansFinder.getSingletonInstance(LifecycleFactory.class.getName());
        
        return currentLifecycle;
    }
    
    public ContainerLifecycle getLifecycle()
    {
        ContainerLifecycle lifecycle = ServiceLoader.getService(ContainerLifecycle.class);
        
        return lifecycle;
    }
}
