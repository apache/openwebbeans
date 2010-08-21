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

import java.lang.annotation.Annotation;
import java.util.Properties;

import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.config.BeansDeployer;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.jms.JMSManager;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.portable.events.discovery.BeforeShutdownImpl;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.WebBeansConstants;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

public abstract class AbstractLifeCycle implements ContainerLifecycle
{
    //Logger instance
    protected WebBeansLogger logger;
    
    /**Discover bean classes*/
    protected ScannerService scannerService;
    
    protected final ContextsService contextsService;

    /**Deploy discovered beans*/
    private final BeansDeployer deployer;

    /**XML discovery. */
    //XML discovery is removed from the specification. It is here for next revisions of spec.
    private final WebBeansXMLConfigurator xmlDeployer;
    
    /**Using for lookup operations*/
    private final JNDIService jndiService;
    
    /**Root container.*/
    private final BeanManagerImpl beanManager;
    
    protected AbstractLifeCycle()
    {
        this(null);
    }
    
    protected AbstractLifeCycle(Properties properties)
    {
        beforeInitApplication(properties);
        
        this.beanManager = (BeanManagerImpl) WebBeansFinder.getSingletonInstance(BeanManagerImpl.class.getName());
        this.xmlDeployer = new WebBeansXMLConfigurator();
        this.deployer = new BeansDeployer(xmlDeployer);
        this.jndiService = ServiceLoader.getService(JNDIService.class);    
        this.beanManager.setXMLConfigurator(this.xmlDeployer);
        this.scannerService = ServiceLoader.getService(ScannerService.class);
        this.contextsService = ServiceLoader.getService(ContextsService.class);
        
        initApplication(properties);                
    }

    @Override
    public BeanManager getBeanManager()
    {        
        return this.beanManager;
    }
    
    @Override
    public void startApplication(Object startupObject) throws Exception
    {
        // Initalize Application Context
        logger.info(OWBLogConst.INFO_0005);
        
        long begin = System.currentTimeMillis();
        
        //Before Start
        beforeStartApplication(startupObject);
        
        //Load all plugins
        PluginLoader.getInstance().startUp();
        
        //Initialize contexts
        this.contextsService.init(startupObject);
        
        //Scanning process
        logger.debug("Scanning classpaths for beans artifacts.");

        //Scan
        this.scannerService.scan();
        
        //Deploy beans
        logger.debug("Deploying scanned beans.");

        //Deploy
        deployer.deploy(this.scannerService);

        //Start actual starting on sub-classes
        afterStartApplication(startupObject);
        
        logger.info(OWBLogConst.INFO_0001, Long.toString(System.currentTimeMillis() - begin));        
    }

    @Override
    public void stopApplication(Object endObject)
    {
        logger.debug("OpenWebBeans Container is stopping.");

        try
        {
            //Sub-classes operations            
            beforeStopApplication(endObject);
            
            //Fire shut down
            this.beanManager.fireEvent(new BeforeShutdownImpl(), new Annotation[0]);
            
            //Destroys context
            this.contextsService.destroy(endObject);
            
            //Unbind BeanManager
            jndiService.unbind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME);

            //Free all plugin resources
            PluginLoader.getInstance().shutDown();
            
            //Clear extensions
            ExtensionLoader.getInstance().clear();
            
            //Delete Resolutions Cache
            InjectionResolver.getInstance().clearCaches();
            
            //Delte proxies
            JavassistProxyFactory.getInstance().clear();
            
            //Delete AnnotateTypeCache
            AnnotatedElementFactory.getInstance().clear();
            
            //Delete JMS Model Cache
            JMSManager.getInstance().clear();
            
            //After Stop
            afterStopApplication(endObject);
            
            //Clear singleton list
            WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());
                        
        }
        catch (Exception e)
        {
            logger.error(OWBLogConst.ERROR_0021, e);
        }
        
    }

    /**
     * @return the logger
     */
    protected WebBeansLogger getLogger()
    {
        return logger;
    }

    /**
     * @return the scannerService
     */
    protected ScannerService getScannerService()
    {
        return scannerService;
    }

    /**
     * @return the contextsService
     */
    public ContextsService getContextService()
    {
        return contextsService;
    }

    /**
     * @return the deployer
     */
    protected BeansDeployer getDeployer()
    {
        return deployer;
    }

    /**
     * @return the xmlDeployer
     */
    protected WebBeansXMLConfigurator getXmlDeployer()
    {
        return xmlDeployer;
    }

    /**
     * @return the jndiService
     */
    protected JNDIService getJndiService()
    {
        return jndiService;
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
        
    protected void afterStartApplication(Object startupObject) throws Exception
    {
        //Do nothing as default
    }

    protected void afterStopApplication(Object stopObject) throws Exception
    {
        //Do nothing as default
    }
    
    protected void beforeStartApplication(Object startupObject) throws Exception
    {
        //Do nothing as default
    }

    protected void beforeStopApplication(Object stopObject) throws Exception
    {
        //Do nothing as default
    }    
}
