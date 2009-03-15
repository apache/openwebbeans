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
package org.apache.webbeans.container.activity;


import javax.inject.manager.Bean;

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.ManagerImpl;

public class ActivityManager
{
    private ManagerImpl rootActivity = null;
    
    private ManagerImpl currentActivity = null;
    
    public ActivityManager()
    {
        
    }
    
    public static ActivityManager getInstance()
    {
        ActivityManager currentActivityManager = (ActivityManager)WebBeansFinder.getSingletonInstance(ActivityManager.class.getName());
        
        return currentActivityManager;
    }
    
    public void setRootActivity(ManagerImpl rootActivity)
    {
        this.rootActivity = rootActivity;
    }
    
    public ManagerImpl getRootActivity()
    {
        return this.rootActivity;
    }

    public static void addBean(Bean<?> bean)
    {
        getInstance().getRootActivity().addBean(bean);
    }
    
    public void setCurrentActivity(ManagerImpl currentManager)
    {
        currentActivity = currentManager; 
    }
    
    public ManagerImpl getCurrentActivity()
    {
        if(currentActivity == null)
        {
            return getRootActivity();
        }
        
        return currentActivity;
    }
}
