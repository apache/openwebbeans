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
package org.apache.webbeans.lifecycle.test;

import org.apache.webbeans.corespi.scanner.AbstractMetaDataDiscovery;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.util.Asserts;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * Used by each test. 
 * @version $Rev$ $Date$
 *
 */
public class OpenWebBeansTestMetaDataDiscoveryService extends AbstractMetaDataDiscovery
{

    private Collection<Class<?>> classes;

    public OpenWebBeansTestMetaDataDiscoveryService()
    {
        super();
    }

    @Override
    protected void configure()
    {
        // Nothing to scan, we add all our stuff manually...
    }
    
    /**
     * Those classes will be scanned by container.
     * @param classes deployed classes
     */
    public void deployClasses(Collection<Class<?>> classes)
    {
        this.classes = classes;
    }

    @Override
    public Set<Class<?>> getBeanClasses()
    {
        return classes == null ? emptySet() : new HashSet<>(classes);
    }

    /**
     * Those xmls will be scanned by container.
     * @param xmls beans xmls
     */
    public void deployXMLs(Collection<String> xmls)
    {
        if(xmls != null)
        {
            for(String url : xmls)
            {
                try
                {
                    addBeanXml(new URL(url));
                }
                catch (MalformedURLException e)
                {
                    throw new WebBeansDeploymentException("could not convert to URL: " + url, e);
                }
            }
        }
    }
    
    /**
     * Adds new beans.xml url for scanning.
     * @param url new xml url
     */
    private void addBeanXml(URL url)
    {
        Asserts.assertNotNull(url);
        
        addWebBeansXmlLocation(url);
    }

}
