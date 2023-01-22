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
package org.apache.webbeans.lifecycle;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.config.BeansDeployer;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectableBeanManager;
import org.apache.webbeans.portable.events.discovery.BeforeShutdownImpl;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.WebBeansConstants;
import org.apache.webbeans.util.WebBeansUtil;

public abstract class AbstractLifeCycle implements ContainerLifecycle
{
    protected AtomicBoolean started = new AtomicBoolean(false);

    //Logger instance
    protected Logger logger;
    
    /**Discover bean classes*/
    protected ScannerService scannerService;
    
    protected final ContextsService contextsService;

    /**Deploy discovered beans*/
    private final BeansDeployer deployer;

    /**Using for lookup operations*/
    private final JNDIService jndiService;
    
    /**Root container.*/
    private final BeanManagerImpl beanManager;
    protected final WebBeansContext webBeansContext;

    protected AbstractLifeCycle()
    {
        this(null);
    }
    
    protected AbstractLifeCycle(Properties properties)
    {
        this(properties, WebBeansContext.getInstance());
    }

    protected AbstractLifeCycle(Properties properties, WebBeansContext webBeansContext)
    {
        beforeInitApplication(properties);

        this.webBeansContext = webBeansContext;
        beanManager = this.webBeansContext.getBeanManagerImpl();

        deployer = new BeansDeployer(webBeansContext);

        jndiService = this.webBeansContext.getService(JNDIService.class);
        scannerService = this.webBeansContext.getScannerService();
        contextsService = this.webBeansContext.getService(ContextsService.class);
        initApplication(properties);
    }

    public WebBeansContext getWebBeansContext()
    {
        return webBeansContext;
    }

    @Override
    public BeanManager getBeanManager()
    {        
        return new InjectableBeanManager(beanManager);
    }
    
    @Override
    public void startApplication(Object startupObject)
    {
        bootstrapApplication(startupObject);
    }

    protected synchronized void bootstrapApplication(Object startupObject)
    {
        if (started.get())
        {
            return;
        }

        // Initalize Application Context
        logger.info(OWBLogConst.INFO_0005);
        
        long begin = System.currentTimeMillis();
        
        //Before Start
        beforeStartApplication(startupObject);
        
        //Load all plugins
        webBeansContext.getPluginLoader().startUp();
        
        //Initialize contexts
        contextsService.init(startupObject);
        
        //Scanning process
        logger.fine("Scanning classpaths for beans artifacts.");

        //Scan
        scannerService.scan();
        
        //Deploy beans
        logger.fine("Deploying scanned beans.");

        //Deploy
        deployer.deploy(scannerService);

        //Start actual starting on sub-classes
        afterStartApplication(startupObject);

        if (logger.isLoggable(Level.INFO))
        {
            logger.log(Level.INFO, OWBLogConst.INFO_0001, Long.toString(System.currentTimeMillis() - begin));
        }

        started.set(true);
    }

    @Override
    public void stopApplication(Object endObject)
    {
        logger.fine("OpenWebBeans Container is stopping.");

        try
        {
            //Sub-classes operations            
            beforeStopApplication(endObject);

            //Fire shut down
            beanManager.fireLifecycleEvent(new BeforeShutdownImpl());

            contextsService.destroy(endObject);

            //Unbind BeanManager
            jndiService.unbind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME);

            //Free all plugin resources
            webBeansContext.getPluginLoader().shutDown();
            
            //Clear extensions
            webBeansContext.getExtensionLoader().clear();
            
            //After Stop
            afterStopApplication(endObject);

            // Clear BeanManager
            beanManager.clear();
        }
        catch (Exception e)
        {
            if (logger.isLoggable(Level.SEVERE))
            {
                logger.log(Level.SEVERE, OWBLogConst.ERROR_0021, e);
            }
        }
        finally
        {
            started.set(false);
            // Clear singleton list
            WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());
        }
    }

    /**
     * @return the contextsService
     */
    @Override
    public ContextsService getContextService()
    {
        return contextsService;
    }

    @Override
    public void initApplication(Properties properties)
    {
        afterInitApplication(properties);
    }    
    
    protected void beforeInitApplication(Properties properties)
    {
        //Do nothing as default
    }
    
    protected void afterInitApplication(Properties properties)
    {
        //Do nothing as default
    }

    protected void afterStartApplication(Object startupObject)
    {
        //Do nothing as default
    }

    protected void afterStopApplication(Object stopObject)
    {
        //Do nothing as default
    }
    
    protected void beforeStartApplication(Object startupObject)
    {
        //Do nothing as default
    }

    protected void beforeStopApplication(Object stopObject)
    {
        //Do nothing as default
    }    
}
