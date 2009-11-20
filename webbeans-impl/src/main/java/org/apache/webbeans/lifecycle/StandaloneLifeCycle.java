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
package org.apache.webbeans.lifecycle;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.config.BeansDeployer;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.activity.ActivityManager;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.portable.events.discovery.BeforeShutdownImpl;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.Lifecycle;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.spi.deployer.MetaDataDiscoveryService;
import org.apache.webbeans.spi.se.deployer.MetaDataDiscoveryStandard;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

public class StandaloneLifeCycle implements Lifecycle
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(StandaloneLifeCycle.class);
    
    /**Deploy discovered beans*/
    private final BeansDeployer beansDeployer;
        
    /**Root container.*/
    protected final BeanManagerImpl beanManager;
    
    private AtomicBoolean inited = new AtomicBoolean(false);
    
    private AtomicBoolean started = new AtomicBoolean(false);
    
    private AtomicBoolean stopped = new AtomicBoolean(false);
    
    protected MetaDataDiscoveryService discoveryService = null;
    
    protected final WebBeansXMLConfigurator xmlConfig;
    
    public StandaloneLifeCycle()
    {
        this.beanManager = new BeanManagerImpl();
        this.xmlConfig = new WebBeansXMLConfigurator();
        this.beansDeployer = new BeansDeployer(this.xmlConfig);
        
        init();
    }
    
    @Override
    public void init()
    {
        if(inited.compareAndSet(false, true))
        {
            discoveryService = ServiceLoader.getService(MetaDataDiscoveryService.class);
            
            if(discoveryService == null)
            {
                String message = "Discovery service not found!. Continue with using MetaDataDiscoveryStandard as a default"; 
                logger.warn(message);
                
                this.discoveryService = new MetaDataDiscoveryStandard();
            }
            else
            {
                logger.info("Using discovery service implementation class : " + discoveryService.getClass());
            }
            
            beanManager.setXMLConfigurator(this.xmlConfig);        
            ActivityManager.getInstance().setRootActivity(this.beanManager);        

        }
    }

    @Override
    public void applicationStarted(Object startupObject) throws WebBeansException
    {
        if(this.started.compareAndSet(false, true))
        {            
            logger.info("OpenWebBeans Container is starting");
            long begin = System.currentTimeMillis();
            
            //Singleton context
            ContextFactory.initSingletonContext(null);

            // load all optional plugins
            PluginLoader.getInstance().startUp();

            logger.info("Scanning classpaths for beans artifacts");

            this.discoveryService.scan();

            logger.info("Deploying scanned beans");

            this.beansDeployer.deploy(this.discoveryService);
                        
            long end = System.currentTimeMillis();
            
            logger.info("OpenWebBeans Container is started, it takes " + Long.toString(end - begin) + " ms.");            
            
        }
        else
        {
            logger.warn("OpenWebBeans Container is already started");
        }
        
    }
    
    @Override
    public void applicationEnded(Object endObject)
    {
        if(this.stopped.compareAndSet(false, true))
        {
            logger.info("OpenWebBeans Container is stopping");
            

            //Fire shut down
            this.beanManager.fireEvent(new BeforeShutdownImpl(), new Annotation[0]);
            
            JNDIService jndiService = ServiceLoader.getService(JNDIService.class);
            
            jndiService.unbind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME);
                    
            ContextFactory.destroySingletonContext(null);

            // finally free all plugin resources
            PluginLoader.getInstance().shutDown();
            
            //Clear extensions
            ExtensionLoader.getInstance().clear();

            //Clear singleton list
            WebBeansFinder.clearInstances();
                    
            logger.info("OpenWebBeans Container is stopped");        
            
        }
        else
        {
            logger.warn("OpenWebBeans Container is already stopped");
        }        
        
    }

    @Override
    public BeanManager getBeanManager()
    {
        return this.beanManager;
    }

    public MetaDataDiscoveryService getDiscoveryService()
    {
        return this.discoveryService;
    }
}
