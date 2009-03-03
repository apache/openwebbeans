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
package org.apache.webbeans.test.tck.mock;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.webbeans.config.EJBWebBeansConfigurator;
import org.apache.webbeans.config.SimpleWebBeansConfigurator;
import org.apache.webbeans.config.WebBeansContainerDeployer;
import org.apache.webbeans.config.WebBeansScanner;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;

public class TCKWebBeansContainerDeployer extends WebBeansContainerDeployer
{
    private Set<Class<?>> classes = new HashSet<Class<?>>();
    private Set<URL> beansXml = new HashSet<URL>();
    
    public TCKWebBeansContainerDeployer(WebBeansXMLConfigurator xmlConfigurator)
    {
        super(xmlConfigurator);
    }

    public void addBeanClass(Class<?> clazz)
    {
        Asserts.assertNotNull(clazz);
        classes.add(clazz);
    }
    
    public void addBeanXml(URL url)
    {
        Asserts.assertNotNull(url);
        beansXml.add(url);
    }
    
    public void clear()
    {
        this.classes.clear();
        this.beansXml.clear();
        this.deployed = false;
    }
    
    protected void deployFromXML(WebBeansScanner scanner)
    {
        if(scanner == null)
        {
            Iterator<URL> urlIt = beansXml.iterator();
            while(urlIt.hasNext())
            {
                URL url = urlIt.next();
                try
                {
                    InputStream stream = url.openStream();
                    if(stream.available() > 0)
                    {
                        this.xmlConfigurator.configure(url.openStream(), url.getFile());    
                    }
                    
                }
                catch (IOException e)
                {
                    
                }
            }
        }
        else
        {
            super.deployFromXML(scanner);
        }
    }
    
    
    protected void deployFromClassPath(WebBeansScanner scanner) throws ClassNotFoundException
    {
        if(scanner == null)
        {
            Iterator<Class<?>> it = classes.iterator();
            while(it.hasNext())
            {
                Class<?> clazz = it.next();
                
                if (SimpleWebBeansConfigurator.isSimpleWebBean(clazz))
                {
                    defineSimpleWebBeans(clazz);
                }
                else if (EJBWebBeansConfigurator.isEJBWebBean(clazz))
                {
                    defineEnterpriseWebBeans();
                }
                
            }
        }
        else
        {
            super.deployFromClassPath(scanner);
        }
    }    

    @SuppressWarnings("unchecked")
    protected void checkStereoTypes(WebBeansScanner scanner)
    {
        if(scanner == null)
        {
            Iterator<Class<?>> it = classes.iterator();
            while(it.hasNext())
            {
                Class<? extends Annotation> stereoClass = (Class<Annotation>)it.next();
                
                if (AnnotationUtil.isStereoTypeAnnotation(stereoClass))
                {
                    if (!XMLAnnotationTypeManager.getInstance().isStereoTypeExist(stereoClass))
                    {
                        WebBeansUtil.checkStereoTypeClass(stereoClass);
                        StereoTypeModel model = new StereoTypeModel(stereoClass);
                        StereoTypeManager.getInstance().addStereoTypeModel(model);
                    }
                }
                
                
            }
        }
        else
        {
            super.checkSpecializations(scanner);
        }
        
    }

    
    protected void checkSpecializations(WebBeansScanner scanner)
    {
    }

    
    protected void configureDecorators(WebBeansScanner scanner) throws ClassNotFoundException
    {
        
    }

    
    protected void configureInterceptors(WebBeansScanner scanner) throws ClassNotFoundException
    {
        
    }
    
}