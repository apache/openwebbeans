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
package org.apache.webbeans.corespi.scanner;


import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.ClassUtil;
import org.scannotation.AnnotationDB;

public abstract class AbstractMetaDataDiscovery implements ScannerService
{
    /** Location of the beans.xml files. */
    private Set<URL> webBeansXmlLocations = new HashSet<URL>();

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
     * @return the aNNOTATION_DB
     */
    protected AnnotationDB getAnnotationDB()
    {
        return annotationDB;
    }
    
    /**
     * add the given beans.xml path to the locations list 
     * @param file location path
     */
    protected void addWebBeansXmlLocation(URL file)
    {
        webBeansXmlLocations.add(file);
    }

    /* (non-Javadoc)
     * @see org.apache.webbeans.corespi.ScannerService#getBeanClasses()
     */
    @Override
    public Set<Class<?>> getBeanClasses()
    {
        Set<Class<?>> classSet = new HashSet<Class<?>>();
        Map<String,Set<String>> index = this.annotationDB.getClassIndex();
        
        if(index != null)
        {
            Set<String> strSet = index.keySet();
            if(strSet != null)
            {
                for(String str : strSet)
                {
                    classSet.add(ClassUtil.getClassFromName(str));   
                }
            }   
        }    
        
        return classSet;
    }

    /* (non-Javadoc)
     * @see org.apache.webbeans.corespi.ScannerService#getBeanXmls()
     */
    @Override
    public Set<URL> getBeanXmls()
    {
        return Collections.unmodifiableSet(webBeansXmlLocations);
    }

}
