/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.WebBeansConstants;

/**
 * Defines the web beans name space.
 */
public class WebBeansNameSpaceContainer
{
    /** Hold namespace string to package name */
    private Map<String, List<String>> nameSpaceToPackages = new ConcurrentHashMap<String, List<String>>();
    
    
    public WebBeansNameSpaceContainer()
    {
        
    }

    public static WebBeansNameSpaceContainer getInstance()
    {
        return WebBeansContext.getInstance().getWebBeansNameSpaceContainer();
    }

    /**
     * Add new name space to package binding.
     * 
     * @param nameSpace new name space
     */
    public void addNewPackageNameSpace(String nameSpace)
    {
        // Check that nameSpace starts with urn:java
        if (nameSpace.startsWith("urn:java:") && !nameSpace.equals(WebBeansConstants.WEB_BEANS_NAMESPACE))
        {

            String packageName = nameSpace.substring("urn:java:".length(), nameSpace.length()) + ".";
            
            if(this.nameSpaceToPackages.containsKey(packageName))
            {
                this.nameSpaceToPackages.get(nameSpace).add(packageName);
            }
            else
            {
                List<String> packageList = new ArrayList<String>();
                packageList.add(packageName);
                
                this.nameSpaceToPackages.put(nameSpace, packageList);
            }
            
        }
        
        if(nameSpace.equals(WebBeansConstants.WEB_BEANS_NAMESPACE))
        {
            if(!this.nameSpaceToPackages.containsKey(nameSpace))
            {
                List<String> eePackages = new ArrayList<String>();
                eePackages.add("java.lang.");
                eePackages.add("java.util.");
                eePackages.add("javax.enterprise.context.");
                eePackages.add("javax.enterprise.context.spi.");
                eePackages.add("javax.enterprise.event.");
                eePackages.add("javax.enterprise.inject.");
                eePackages.add("org.apache.webbeans.annotation.deployment.");
                eePackages.add("javax.enterprise.inject.spi.");
                eePackages.add("javax.inject.");
                eePackages.add("javax.context.");
                eePackages.add("javax.interceptor.");
                eePackages.add("javax.decorator.");
                eePackages.add("javax.event.");
                eePackages.add("javax.ejb.");
                eePackages.add("javax.persistence.");
                eePackages.add("javax.xml.ws.");
                eePackages.add("javax.jms.");
                eePackages.add("javax.sql.");
                
                this.nameSpaceToPackages.put(nameSpace, eePackages);
            }
        }
    }

    /**
     * Gets package string for name space.
     * 
     * @param nameSpace name space
     * @return package for namespace
     */
    public List<String> getPackageNameFromNameSpace(String nameSpace)
    {
        if (nameSpaceToPackages.containsKey(nameSpace))
        {
            return nameSpaceToPackages.get(nameSpace);
        }

        return null;
    }
}
