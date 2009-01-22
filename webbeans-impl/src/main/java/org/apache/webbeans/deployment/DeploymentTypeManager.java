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
package org.apache.webbeans.deployment;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.webbeans.Standard;

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.util.Asserts;

public class DeploymentTypeManager
{
    private Map<Class<? extends Annotation>, Integer> deploymentTypeMap = new ConcurrentHashMap<Class<? extends Annotation>, Integer>();

    public DeploymentTypeManager()
    {

    }

    public static DeploymentTypeManager getInstance()
    {
        DeploymentTypeManager instance = (DeploymentTypeManager) WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_DEPLOYMENT_TYPE_MANAGER);
        if (!instance.deploymentTypeMap.containsKey(Standard.class)) 
        {
            instance.deploymentTypeMap.put(Standard.class, Integer.valueOf(0));
        }
        return instance;
    }

    public void addNewDeploymentType(Class<? extends Annotation> deploymentType, Integer precedence)
    {
        Asserts.assertNotNull(deploymentType, "deploymentType parameter can not be null");
        Asserts.assertNotNull(precedence, "predence parameter can not be null");

        if (!deploymentType.equals(Standard.class))
        {
            if (!deploymentTypeMap.containsKey(deploymentType))
            {
                deploymentTypeMap.put(deploymentType, precedence);
            }
        }
    }

    public int getPrecedence(Class<? extends Annotation> deploymentType)
    {
        Asserts.assertNotNull(deploymentType, "deploymentType parameter can not be null");

        if (!deploymentTypeMap.containsKey(deploymentType))
        {
            throw new IllegalArgumentException("Deployment type with annotation class : " + deploymentType.getName() + " is not applicable");
        }
        else
        {
            return deploymentTypeMap.get(deploymentType);
        }
    }

    public int comparePrecedences(Class<? extends Annotation> typeFirst, Class<? extends Annotation> typeSecond)
    {
        Asserts.assertNotNull(typeFirst, "typeFirst parameter can not be null");
        Asserts.assertNotNull(typeSecond, "typeSecond parameter can not be null");

        int precOne = getPrecedence(typeFirst);
        int precSecond = getPrecedence(typeSecond);

        if (precOne == precSecond)
            return 0;
        else if (precOne < precSecond)
            return -1;
        else
            return 1;

    }

    public boolean isDeploymentTypeEnabled(Class<? extends Annotation> deploymentType)
    {
        Asserts.assertNotNull(deploymentType, "deploymentType parameter can not be null");
        return deploymentTypeMap.containsKey(deploymentType);
    }
    
    public List<Class<? extends Annotation>> getEnabledDeploymentTypes()
    {
        ArrayList<Class<? extends Annotation>> enabledDeploymentTypes = new ArrayList<Class<? extends Annotation>>();
        
        enabledDeploymentTypes.addAll(deploymentTypeMap.keySet());
        return enabledDeploymentTypes;
    }
}