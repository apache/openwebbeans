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
package org.apache.webbeans.test.portable.events.extensions;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.ProcessManagedBean;

import org.apache.webbeans.test.portable.events.beans.Apple;

public class AppleExtension1 implements Extension
{
    public static int TYPED_CALLED = 0;
    public static int CALLED = 0;
    
    public static int MANAGED_TYPED_CALLED = 0;
    public static int MANAGED_CALLED = 0;
    
    public static void reset()
    {
        TYPED_CALLED = 0;
        CALLED = 0;
        MANAGED_TYPED_CALLED = 0;
        MANAGED_CALLED = 0;
    }
    
    public void typedProcessBean(@Observes ProcessBean<Apple> event)
    {
        TYPED_CALLED++;
    }
    
    public void processBean(@Observes ProcessBean event)
    {
        CALLED++;
    } 
    
    
    public void typedProcessManagedBean(@Observes ProcessManagedBean<Apple> event)
    {
        MANAGED_TYPED_CALLED++;
    }
    
    public void processManagedBean(@Observes ProcessManagedBean event)
    {
        MANAGED_CALLED++;
    } 
}
