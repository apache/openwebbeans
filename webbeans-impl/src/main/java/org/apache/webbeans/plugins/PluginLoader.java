package org.apache.webbeans.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.webbeans.config.WebBeansFinder;
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
        logger.debug("PluginLoader startUp called");
        ArrayList<OpenWebBeansPlugin> ps = new ArrayList<OpenWebBeansPlugin>();

        ServiceLoader<OpenWebBeansPlugin> owbPluginsLoader = ServiceLoader.load(OpenWebBeansPlugin.class);
        Iterator<OpenWebBeansPlugin> pluginIter = owbPluginsLoader.iterator();
        while(pluginIter.hasNext()) 
        {
          OpenWebBeansPlugin plugin = pluginIter.next();
          logger.info("adding OpenWebBeansPlugin " + plugin.getClass().getSimpleName());
          ps.add(plugin);
        }   
        
        // just to make sure the plugins aren't modified afterwards
        plugins = Collections.unmodifiableList(ps);
    }
    
    /**
     * Tell all the plugins to free up all locked resources.
     * This must be called before the WebApplication gets undeployed or stopped.
     * @throws WebBeansConfigurationException
     */
    public void shutDown() throws WebBeansConfigurationException
    {
        logger.debug("PluginLoader shutDown called");
        
        if (plugins == null)
        {
            logger.warn("No plugins to shutDown!");
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
                logger.error("error while shutdown the pugin " + pluginName, e);
                failedShutdown.add(pluginName);
            }
        }
        
        if (!failedShutdown.isEmpty())
        {
            throw new WebBeansConfigurationException("got Exceptions while sending shutdown to the following plugins: "
                                                     + failedShutdown.toString());
        }
    }
    
    /**
     * @return an unmodifiable list of all registered {code OpenWebBeansPlugin}s
     */
    public List<OpenWebBeansPlugin> getPlugins()
    {
        return plugins;
    }
    
}
