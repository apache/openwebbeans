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
package org.apache.webbeans.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLogger;

/**
 * <p>OpenWebBeans plugins are used to extend 'core' functionality of the injection
 * mechanism with functionality of other frameworks.</p>
 * 
 * <p>Core functionality are all parts which are available in a standard
 * JDK-1.5 SE runtime. Extended functionality are things like JPA, JSF, EJB etc.</p>
 * 
 * <p>The plugin mechanism is based on the ServiceProvider functionality 
 * {@link http://java.sun.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider}.
 * A jar containing an OWB plugin has to expose itself in the file
 * <code>META-INF/services/org.apache.webbeans.plugins.OpenWebBeansPlugin</code></p> 
 *
 * TODO: what about ordering the plugins via an ordinal?
 */
public class PluginLoader
{
    /**Logger instance*/
    private static WebBeansLogger logger = WebBeansLogger.getLogger(PluginLoader.class);

    /** unmodifiable list with all found OWB plugins */
    private List<OpenWebBeansPlugin> plugins = null;

    private AtomicBoolean started = new AtomicBoolean(false);
    
    /**
     * @return singleton PluginLoader 
     */
    public static PluginLoader getInstance()
    {
        return (PluginLoader) WebBeansFinder.getSingletonInstance(PluginLoader.class.getName());
    }
    
    public PluginLoader()
    {
    }    

    /**
     * load and startup all registered plugins.
     * This must be called once the WebApplication is started.
     * @throws WebBeansConfigurationException
     */
    public void startUp() throws WebBeansConfigurationException
    {
        if(this.started.compareAndSet(false, true))
        {
            logger.debug("PluginLoader startUp called.");
            ArrayList<OpenWebBeansPlugin> ps = new ArrayList<OpenWebBeansPlugin>();

            ServiceLoader<OpenWebBeansPlugin> owbPluginsLoader = ServiceLoader.load(OpenWebBeansPlugin.class);
            Iterator<OpenWebBeansPlugin> pluginIter = owbPluginsLoader.iterator();
            while(pluginIter.hasNext()) 
            {
              OpenWebBeansPlugin plugin = pluginIter.next();
              logger.info(OWBLogConst.INFO_0013, new Object[]{plugin.getClass().getSimpleName()});
              plugin.startUp();
              ps.add(plugin);
            }   
            
            // just to make sure the plugins aren't modified afterwards
            plugins = Collections.unmodifiableList(ps);            
        }
        else
        {
            logger.debug("PluginLoader is already started.");
        }
    }
    
    /**
     * Tell all the plugins to free up all locked resources.
     * This must be called before the WebApplication gets undeployed or stopped.
     * @throws WebBeansConfigurationException
     */
    public void shutDown() throws WebBeansConfigurationException
    {
        if(this.started.compareAndSet(true, false))
        {
            logger.debug("PluginLoader shutDown called.");
            
            if (plugins == null)
            {
                logger.warn(OWBLogConst.WARN_0001);
                return;
            }

            ArrayList<String> failedShutdown = new ArrayList<String>();

            for (OpenWebBeansPlugin plugin : plugins)
            {
                try 
                {
                    plugin.shutDown();
                }
                catch (Exception e)
                {
                    // we catch ALL exceptions, since we like to continue shutting down all other plugins!
                    String pluginName = plugin.getClass().getSimpleName();
                    logger.error(OWBLogConst.ERROR_0009, new Object[]{pluginName}, e);
                    failedShutdown.add(pluginName);
                }
            }
            
            if (!failedShutdown.isEmpty())
            {
                throw new WebBeansConfigurationException(logger.getTokenString(OWBLogConst.EXCEPT_0006) + failedShutdown.toString());
            }            
        }
        else
        {
            logger.debug("PluginLoader is already shut down.");
        }
    }
    
    /**
     * @return an unmodifiable list of all registered {code OpenWebBeansPlugin}s
     */
    public List<OpenWebBeansPlugin> getPlugins()
    {
        return plugins;
    }
 
    /**
     * Gets ejb plugin
     * 
     * @return ejb plugin
     */
    public OpenWebBeansEjbPlugin getEjbPlugin()
    {
        for(OpenWebBeansPlugin plugin : this.plugins)
        {
            if(plugin instanceof OpenWebBeansEjbPlugin)
            {
                return (OpenWebBeansEjbPlugin)plugin;
            }
        }
        
        return null;
    }
    
    /**
     * Gets the JMS plugin
     * 
     * @return jms plugin
     */
    public OpenWebBeansJmsPlugin getJmsPlugin()
    {
        for(OpenWebBeansPlugin plugin : this.plugins)
        {
            if(plugin instanceof OpenWebBeansJmsPlugin)
            {
                return (OpenWebBeansJmsPlugin)plugin;
            }
        }
        
        return null;
    }
    
 
    public boolean isShowDown()
    {
        return !this.started.get();
    }
}
