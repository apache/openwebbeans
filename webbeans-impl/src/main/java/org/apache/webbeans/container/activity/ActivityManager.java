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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.Context;

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansException;

/**
 * Class is responsible for managing the activities.
 * 
 * <p>
 * There is always one root activity.
 * </p>
 * 
 * @version $Rev$ $Date$
 */
public class ActivityManager
{
    /**Root activity*/
    private BeanManagerImpl rootActivity = null;
    
    /**Setted current activities*/
    private Map<Context, BeanManagerImpl> currentActivityMap = new ConcurrentHashMap<Context, BeanManagerImpl>();
    
    /**
     * Used by the system. Do not
     * instantiate this from outside.
     */
    public ActivityManager()
    {
        
    }
    
    /**
     * Gets the activity manager.
     * 
     * @return the singleton acitivity manager
     */
    public static ActivityManager getInstance()
    {
        ActivityManager currentActivityManager = (ActivityManager)WebBeansFinder.getSingletonInstance(ActivityManager.class.getName());
        
        return currentActivityManager;
    }
    
    
    /**
     * Sets the root activity
     * 
     * @param rootActivity root activity
     */
    public synchronized void  setRootActivity(BeanManagerImpl rootActivity)
    {
        this.rootActivity = rootActivity;
    }
    
    /**
     * Gets root activity
     * 
     * @return the root activity
     */
    public BeanManagerImpl getRootActivity()
    {
        return this.rootActivity;
    }

    
    /**
     * Add new current activity for the context.
     * 
     * @param context
     * @param currentManager
     */
    public void addCurrentActivity(Context context, BeanManagerImpl currentManager)
    {
        this.currentActivityMap.put(context, currentManager); 
    }
    
    /**
     * Looks for the registered current activities.
     * <ul>
     *  <li>If there are more than one activity, throws exception.</li>
     *  <li>If no registered current activity, return the root activity.
     * </ul>
     * 
     * 
     * @return the current activity
     * @throws WebBeansException if more than one current activity exist
     */
    public BeanManagerImpl getCurrentActivity()
    {
        BeanManagerImpl currentActivity = null;
        
        Set<Context> contexts = this.currentActivityMap.keySet();
        List<BeanManagerImpl> managers = new ArrayList<BeanManagerImpl>(); 
        for(Context context : contexts)
        {
            if(context.isActive())
            {
                managers.add(this.currentActivityMap.get(context));
            }
        }
        
        if(managers.size() > 1)
        {
            throw new WebBeansException("There are more than one current activity");
        }
        else
        {
            if(!managers.isEmpty())
            {
                currentActivity = managers.get(0);   
            }            
        }

        
        if(currentActivity == null)
        {
            return getRootActivity();
        }
        
        return currentActivity;
    }
}
