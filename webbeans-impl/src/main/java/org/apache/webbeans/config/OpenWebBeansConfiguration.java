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
package org.apache.webbeans.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLogger;

/**
 * This class performs a lookup of various configuration properties 
 * There are 2 different configuration files
 * <ol>
 *  <li><code>META-INF/openwebbeans.properties</code> contains the currently used configuration</li>
 *  <li><code>META-INF/openwebbeans-default.properties</code> contains all default values</li>
 * </ol>
 * 
 * Both configuration files will be loaded via the ClassLoader. 
 * The <code>META-INF/openwebbeans.properties</code> doesn't have to contain the full set of
 * available configuration properties. If it doesn't contain a specific property, 
 * the value will be looked up in <code>META-INF/openwebbeans-default.properties<code>
 */
public class OpenWebBeansConfiguration
{
    private final static String DEFALULT_CONFIG_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans-default.properties";
    private final static String CONFIG_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans.properties";

    private Properties configProperties = new Properties();
    private WebBeansLogger logger = WebBeansLogger.getLogger(OpenWebBeansConfiguration.class);
    
    /**Conversation periodic delay in ms.*/
    public static final String CONVERSATION_PERIODIC_DELAY = "org.apache.webbeans.conversation.Conversation.periodicDelay";
    
    /**Use OWB Specific XML Configuration or Strict Spec XML*/
    public static final String USE_OWB_SPECIFIC_XML_CONFIGURATION = "org.apache.webbeans.useOwbSpecificXmlConfig";
    
    /**Use OWB Specific Field Injection*/
    public static final String USE_OWB_SPECIFIC_FIELD_INJECTION = "org.apache.webbeans.fieldInjection.useOwbSpecificInjection";    
    
    /**Use EJB Discovery or not*/
    public static final String USE_EJB_DISCOVERY = "org.apache.webbeans.spi.deployer.UseEjbMetaDataDiscoveryService";
    
    public static OpenWebBeansConfiguration getInstance() {
        return (OpenWebBeansConfiguration) WebBeansFinder.getSingletonInstance(OpenWebBeansConfiguration.class.getName());
    }
    
    public OpenWebBeansConfiguration()
    {
        parseConfiguration();
    }
    
    /**
     * (re)read the configuration from the resources in the classpath.
     * @see #DEFALULT_CONFIG_PROPERTIES_NAME
     * @see #CONFIG_PROPERTIES_NAME 
     */
    public synchronized void parseConfiguration() throws WebBeansConfigurationException
    {
        Properties newConfigProperties = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        
        InputStream is = loader.getResourceAsStream(DEFALULT_CONFIG_PROPERTIES_NAME);
        try
        {
            newConfigProperties.load(is);
        }
        catch (IOException ioEx)
        {
            throw new WebBeansConfigurationException("problem while loading OpenWebBeans default configuration", ioEx);
        }
        
        // and now overload those settings with the ones from the more specialized version (if available)
        
        URL configUrl = loader.getResource(CONFIG_PROPERTIES_NAME);
        if (configUrl == null)
        {
            logger.info("No config properties " + CONFIG_PROPERTIES_NAME + " found. Using default settings.");
        }
        else
        {
            logger.info("Config properties " + CONFIG_PROPERTIES_NAME + " found at location "
                        + configUrl.toString()
                        + ". Overriding default settings.");

            is = loader.getResourceAsStream(CONFIG_PROPERTIES_NAME);
            try
            {
                newConfigProperties.load(is);
            }
            catch (IOException ioEx)
            {
                throw new WebBeansConfigurationException("problem while loading OpenWebBeans specialized configuration", ioEx);
            }
            
        }

        // set the new one as perfect fit.
        configProperties = newConfigProperties;
    }
    
    /**
     * @param key
     * @return String with the property value or <code>null</code>
     */
    public String getProperty(String key)
    {
        return configProperties.getProperty(key);
    }
    
    /**
     * @param key
     * @param defaultValue
     * @return String with the property value or <code>null</code>
     */
    public String getProperty(String key,String defaultValue)
    {
        return configProperties.getProperty(key, defaultValue);
    }
    
    public boolean isOwbSpecificFieldInjection()
    {
        String value = getProperty(USE_OWB_SPECIFIC_FIELD_INJECTION);
        
        return Boolean.valueOf(value);
    }
    
}
