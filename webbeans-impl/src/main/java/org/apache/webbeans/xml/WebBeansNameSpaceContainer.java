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
package org.apache.webbeans.xml;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines the web beans name space.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class WebBeansNameSpaceContainer
{
    /** Hold namespace string to package name */
    private Map<String, String> nameSpaceToPackages = new ConcurrentHashMap<String, String>();

    /** Singleton instance */
    private static WebBeansNameSpaceContainer nameSpaceContainer = new WebBeansNameSpaceContainer();

    /**
     * Private constructor
     */
    private WebBeansNameSpaceContainer()
    {

    }

    /**
     * Gets container instance.
     * 
     * @return singleton instance
     */
    public static WebBeansNameSpaceContainer getInstance()
    {
        return nameSpaceContainer;
    }

    /**
     * Add new name space to package binding.
     * 
     * @param nameSpace new name space
     */
    public void addNewPackageNameSpace(String nameSpace)
    {
        // Check that nameSpace starts with urn:java
        if (nameSpace.startsWith("urn:java:"))
        {

            String packageName = nameSpace.substring("urn:java:".length(), nameSpace.length()) + ".";
            nameSpaceToPackages.put(nameSpace, packageName);
        }
    }

    /**
     * Gets package string for name space.
     * 
     * @param nameSpace name space
     * @return package for namespace
     */
    public String getPackageNameFromNameSpace(String nameSpace)
    {
        if (nameSpaceToPackages.containsKey(nameSpace))
        {
            return nameSpaceToPackages.get(nameSpace);
        }

        return null;
    }
}
