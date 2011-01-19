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
package org.apache.webbeans.corespi.se;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javassist.bytecode.ClassFile;

import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.ClassUtil;
import org.scannotation.AnnotationDB;

/**
 * Provides extensions to AnnotationDB that allow the beans.xml
 * of each BDA to be associated with a class.
 *
 * due to a file-url issue it isn't compatible with wls (see OWB-519)
 */
public class BeansXmlAnnotationDB extends AnnotationDB
{

    private static final long serialVersionUID = 4321069777044090278L;

    /** Logger instance */
    private final WebBeansLogger logger = WebBeansLogger.getLogger(BeansXmlAnnotationDB.class);

    /**
     * null or beans.xml of current BDA being processed 
     */
    private String beansXml;

    private ScannerService scannerService;

    /** Maps a resource (Ex: .jar) to a beans.xml location */
    Map<URL, URL> beansXmlResources;

    public void setBdaBeansXmlScanner(ScannerService scannerService)
    {
        this.scannerService = scannerService;
    }

    public void setCurrentBeansXml(String beansXml)
    {
        this.beansXml = beansXml;
    }

    public BeansXmlAnnotationDB()
    {
        super();
        beansXmlResources=new HashMap<URL, URL>();
    }

    @Override
    protected void scanClass(ClassFile cf)
    {
        super.scanClass(cf);
        if (beansXml != null && scannerService.isBDABeansXmlScanningEnabled())
        {
            scannerService.getBDABeansXmlScanner().setBeansXml(ClassUtil.getClassFromName(cf.getName()), beansXml);

        }
    }

    @Override
    public void scanArchives(URL... urls) throws IOException
    {
        scanArchivesBeansXml(urls);
    }

    public void setResourceBeansXml(URL resource,URL beansXml)
    {
        if(logger.wblWillLogDebug())
        {
            logger.info("resource="+resource+", beansXml="+beansXml);
        }        
        beansXmlResources.put(resource, beansXml);
    }
    
    private void scanArchivesBeansXml(URL... urls) throws IOException
    {
        // Maps a resource (Ex: .jar) to a beans.xml location
        populateResourceToBeansXml(urls);
        URL currentBeansXml = null;
        for (int i = 0; i < urls.length; i++)
        {
            currentBeansXml = beansXmlResources.get(urls[i]);
            if (currentBeansXml == null)
            {
                throw new IllegalStateException("Could not locate beans.xml for resource: " + urls[i]);
            }
            // set current beans.xml based on archive being scanned
            setCurrentBeansXml(currentBeansXml.getFile());

            super.scanArchives(urls[i]);
        }
        setCurrentBeansXml(null);
    }
    
    
    private void populateResourceToBeansXml(URL[] resourceURLs) throws IOException
    {
        URL beanXmlUrl;
        boolean isMatchFound;
        Iterator<URL> it = scannerService.getBeanXmls().iterator();
        while (it.hasNext())
        {
            isMatchFound = false;
            beanXmlUrl = it.next();
            
            //See if mapping was already assigned for this beans.xml
            if(beansXmlResources.containsValue(beanXmlUrl))
            {
                if(logger.wblWillLogDebug())
                {
                    logger.debug("beans.xml mapping already set: "+beanXmlUrl);
                }
                continue;
            }
            
            for (int i = 0; (!isMatchFound && i < resourceURLs.length); i++)
            {
                if (beanXmlUrl.getFile().startsWith(resourceURLs[i].getFile()))
                {
                    beansXmlResources.put(resourceURLs[i], beanXmlUrl);
                    isMatchFound = true;
                }
            }
            if (!isMatchFound)
            {
                StringBuilder sb = new StringBuilder();
                sb.append("Could not find resource URL to match beans.xml: ");
                sb.append(beanXmlUrl.getFile());
                sb.append(", available resource URLs=");
                for (int i = 0; i < resourceURLs.length; i++)
                {
                    sb.append(resourceURLs[i]).append(", ");
                }
                throw new IOException(sb.toString());
            }
        }
    }
}
