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
package org.apache.webbeans.spi.deployer;


import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.scannotation.AnnotationDB;

public abstract class AbstractMetaDataDiscovery implements MetaDataDiscoveryService
{
    /** Location of the beans.xml files. */
    private Set<String> webBeansXmlLocations = new HashSet<String>();

    //private Map<String, InputStream> EJB_XML_LOCATIONS = new HashMap<String, InputStream>();

    /** Annotation Database */
    private AnnotationDB annotationDB = null;

    protected AbstractMetaDataDiscovery()
    {
        try
        {
            if (annotationDB == null)
            {
                annotationDB = new AnnotationDB();
                annotationDB.setScanClassAnnotations(true);
                annotationDB.crossReferenceMetaAnnotations();    
                annotationDB.setScanFieldAnnotations(false);
                annotationDB.setScanMethodAnnotations(false);
                annotationDB.setScanParameterAnnotations(false);
            }            
            
        }
        catch(Exception e)
        {
            throw new WebBeansDeploymentException(e);
        }                
    }
    
    /**
     * Configure the Web Beans Container with deployment information and fills
     * annotation database and beans.xml stream database.
     * 
     * @throws WebBeansConfigurationException if any run time exception occurs
     */
    public void scan() throws WebBeansDeploymentException
    {
        try
        {
            configure();
        }
        catch (Exception e)
        {
            throw new WebBeansDeploymentException(e);
        }
    }
    
    
    abstract protected void configure() throws Exception;
    
    public void init(Object object)
    {
        
    }
    
    /**
     * @return the wEBBEANS_XML_LOCATIONS
     */
    public Set<String> getWebBeansXmlLocations()
    {
        return Collections.unmodifiableSet(webBeansXmlLocations);
    }

    /**
     * @return the aNNOTATION_DB
     */
    protected AnnotationDB getAnnotationDB()
    {
        return annotationDB;
    }
    
    public Map<String, Set<String>> getAnnotationIndex()
    {
        return annotationDB.getAnnotationIndex();
    }

    public Map<String, Set<String>> getClassIndex()
    {
        return annotationDB.getClassIndex();
    }

    /**
     * add the given beans.xml path to the locations list 
     * @param file location path
     */
    protected void addWebBeansXmlLocation(String file)
    {
        webBeansXmlLocations.add(file);
    }

}
