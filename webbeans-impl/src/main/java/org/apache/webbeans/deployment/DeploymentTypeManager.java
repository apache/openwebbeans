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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Production;
import javax.inject.Standard;

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
            instance.deploymentTypeMap.put(Production.class, Integer.valueOf(1));

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

    public void removeProduction()
    {
        this.deploymentTypeMap.remove(Production.class);
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

    private static class DeploymentComparator implements Comparator<DeploymentTypeObject>
    {

        public int compare(DeploymentTypeObject o1, DeploymentTypeObject o2)
        {
            if (o1.getPrecedence() < o2.getPrecedence())
            {
                return -1;
            }
            else if (o1.getPrecedence() > o2.getPrecedence())
            {
                return 1;
            }

            return 0;
        }

    }

    private static class DeploymentTypeObject
    {
        private int precedence;
        private Class<? extends Annotation> type;

        public DeploymentTypeObject(Class<? extends Annotation> clazz, int precedence)
        {
            this.precedence = precedence;
            this.type = clazz;
        }

        /**
         * @return the precedence
         */
        protected int getPrecedence()
        {
            return precedence;
        }

        /**
         * @param precedence the precedence to set
         */
        protected void setPrecedence(int precedence)
        {
            this.precedence = precedence;
        }

        /**
         * @return the type
         */
        protected Class<? extends Annotation> getType()
        {
            return type;
        }

        /**
         * @param type the type to set
         */
        protected void setType(Class<? extends Annotation> type)
        {
            this.type = type;
        }

    }

    public List<Class<? extends Annotation>> getEnabledDeploymentTypes()
    {
        List<Class<? extends Annotation>> enabledDeploymentTypes = new ArrayList<Class<? extends Annotation>>();

        List<DeploymentTypeObject> objects = new ArrayList<DeploymentTypeObject>();

        Set<Class<? extends Annotation>> keys = this.deploymentTypeMap.keySet();
        Iterator<Class<? extends Annotation>> it = keys.iterator();
        while (it.hasNext())
        {
            Class<? extends Annotation> key = it.next();
            Integer value = this.deploymentTypeMap.get(key);

            objects.add(new DeploymentTypeObject(key, value));
        }

        Collections.sort(objects, new DeploymentComparator());

        for (DeploymentTypeObject obj : objects)
        {
            enabledDeploymentTypes.add(obj.getType());
        }

        return enabledDeploymentTypes;
    }
}