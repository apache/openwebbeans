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
package org.apache.webbeans.web.scanner;

import org.apache.webbeans.corespi.scanner.AbstractMetaDataDiscovery;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.webbeans.util.WebBeansUtil;

import javax.servlet.ServletContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Configures the web application to find beans.
 */
public class WebScannerService extends AbstractMetaDataDiscovery
{
    public static final String WEB_INF_BEANS_XML = "/WEB-INF/beans.xml";

    private final static Logger logger = WebBeansLoggerFacade.getLogger(WebScannerService.class);

    protected ServletContext servletContext = null;
    private BeanArchiveService beanArchiveService;

    public WebScannerService()
    {
        
    }

    @Override
    public void init(Object context)
    {
        super.init(context);
        servletContext = (ServletContext) context;
    }
    
    @Override
    protected void configure()
    {
        ClassLoader loader = WebBeansUtil.getCurrentClassLoader();
        addWarBeansArchive();

        registerBeanArchives(loader);
    }

    /**
     * Returns the web application class path if it contains
     * a beans.xml marker file.
     * 
     * @return the web application class path
     * @throws Exception if any exception occurs
     */
    protected void addWarBeansArchive()
    {
        if (servletContext == null)
        {
            // this may happen if we are running in a test container, in IDE development, etc
            return;
        }

        URL url = null;
        try
        {
            url = servletContext.getResource(WEB_INF_BEANS_XML);
        }
        catch (MalformedURLException e)
        {
            ExceptionUtil.throwAsRuntimeException(e);
        }

        if (url != null)
        {
            addWebBeansXmlLocation(url);

            // the deployment URL already was part of the classpath
            // so no need to do anything else
        }
    }

}
