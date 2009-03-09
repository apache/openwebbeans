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
import java.util.HashMap;
import java.util.Map;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.scannotation.AnnotationDB;

public abstract class AbstractMetaDataDiscovery implements MetaDataDiscoveryService
{
    /** Location of the beans.xml files. */
    protected Map<String, InputStream> WEBBEANS_XML_LOCATIONS = new HashMap<String, InputStream>();

    //private Map<String, InputStream> EJB_XML_LOCATIONS = new HashMap<String, InputStream>();

    /** Annotation Database */
    protected AnnotationDB ANNOTATION_DB = null;

    protected AbstractMetaDataDiscovery()
    {
        try
        {
            if (ANNOTATION_DB == null)
            {
                ANNOTATION_DB = new AnnotationDB();
                ANNOTATION_DB.setScanClassAnnotations(true);
                ANNOTATION_DB.crossReferenceMetaAnnotations();    
                ANNOTATION_DB.setScanFieldAnnotations(false);
                ANNOTATION_DB.setScanMethodAnnotations(false);
                ANNOTATION_DB.setScanParameterAnnotations(false);

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
    public Map<String, InputStream> getWEBBEANS_XML_LOCATIONS()
    {
        return WEBBEANS_XML_LOCATIONS;
    }

    /**
     * @return the aNNOTATION_DB
     */
    public AnnotationDB getANNOTATION_DB()
    {
        return ANNOTATION_DB;
    }
    
    
    
}
