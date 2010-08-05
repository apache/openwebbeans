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
package org.apache.webbeans.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.SecurityUtil;

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
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(OpenWebBeansConfiguration.class);

    /**Default configuration files*/
    private final static String DEFAULT_CONFIG_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans-default.properties";
    private final static String CONFIG_EE_COMMON_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans-ee-common.properties";
    private final static String CONFIG_EE_WEB_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans-ee-web.properties";
    private final static String CONFIG_JMS_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans-jms.properties";
    private final static String CONFIG_JSF_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans-jsf.properties";
    
    /**Application specified file*/
    private final static String CONFIG_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans.properties";
    
    /**Property of application*/
    private Properties configProperties = new Properties();
        
    /**Conversation periodic delay in ms.*/
    public static final String CONVERSATION_PERIODIC_DELAY = "org.apache.webbeans.conversation.Conversation.periodicDelay";
    
    /**Use OWB Specific XML Configuration or Strict Spec XML*/
    @Deprecated //Not use any more 
    public static final String USE_OWB_SPECIFIC_XML_CONFIGURATION = "org.apache.webbeans.useOwbSpecificXmlConfig";
    
    /**Use OWB Specific Field Injection*/
    @Deprecated //Not use anymore
    public static final String USE_OWB_SPECIFIC_FIELD_INJECTION = "org.apache.webbeans.fieldInjection.useOwbSpecificInjection";    
    
    /**Use EJB Discovery or not*/
    public static final String USE_EJB_DISCOVERY = "org.apache.webbeans.spi.deployer.useEjbMetaDataDiscoveryService";
    
    /**Container lifecycle*/
    public static final String CONTAINER_LIFECYCLE = "org.apache.webbeans.spi.ContainerLifecycle";
    
    /**JNDI Service SPI*/
    public static final String JNDI_SERVICE = "org.apache.webbeans.spi.JNDIService";    
    
    /**Scanner Service*/
    public static final String SCANNER_SERVICE = "org.apache.webbeans.spi.ScannerService";

    /**Contexts Service*/
    public static final String CONTEXTS_SERVICE = "org.apache.webbeans.spi.ContextsService";
    
    /**Conversation Service*/
    public static final String CONVERSATION_SERVICE = "org.apache.webbeans.spi.ConversationService";
    
    /**Resource Injection Service*/
    public static final String RESOURCE_INJECTION_SERVICE = "org.apache.webbeans.spi.ResourceInjectionService";
    
    /**Security Service*/
    public static final String SECURITY_SERVICE = "org.apache.webbeans.spi.SecurityService";
    
    /**Validator Service*/
    public static final String VALIDATOR_SERVICE = "org.apache.webbeans.spi.ValidatorService";
    
    /**Transaction Service*/
    public static final String TRANSACTION_SERVICE = "org.apache.webbeans.spi.TransactionService";
    
    /**Application is core JSP*/
    public static final String APPLICATION_IS_JSP = "org.apache.webbeans.application.jsp";

    /**Supports conversations*/
    public static final String APPLICATION_SUPPORTS_CONVERSATION = "org.apache.webbeans.application.supportsConversation";

    /**Use of JSF2 extensions*/
    public static final String USE_JSF2_EXTENSIONS = "org.apache.webbeans.application.useJSF2Extensions";
    
    /**Use of EJB interceptor to inject EJBs*/
    public static final String USE_EJBINTERCEPTOR_INJECTION = "org.apache.webbeans.application.useEJBInterceptorInjection";
    
    /**EL Adaptor*/
    public static final String EL_ADAPTOR_CLASS = "org.apache.webbeans.spi.adaptor.ELAdaptor";
    
    public static final String PROPERTY_OWB_APPLICATION = "org.apache.webbeans.application.isOwbApplication";
    
    /**
     * Gets singleton instance.
     * @return singleton instance
     */
    public static OpenWebBeansConfiguration getInstance() 
    {
        return (OpenWebBeansConfiguration) WebBeansFinder.getSingletonInstance(OpenWebBeansConfiguration.class.getName());
    }
    
    /**
     * Parse configuration.
     */
    public OpenWebBeansConfiguration()
    {
        parseConfiguration();
        
        logger.debug("Overriden properties from System prpoerties");
        
        //Look for System properties
        loadFromSystemProperties();        
    }
    
    /**
     * Load from system properties
     */
    private void loadFromSystemProperties()
    {
        Properties properties = null;
        if(System.getSecurityManager() != null)
        {
            properties = SecurityUtil.doPrivilegedGetSystemProperties();
        }
        else
        {
            properties = System.getProperties();
        }
        
        String value = properties.getProperty(CONVERSATION_PERIODIC_DELAY);
        setPropertyFromSystemProperty(CONVERSATION_PERIODIC_DELAY, value);        
        
        value = properties.getProperty(USE_EJB_DISCOVERY);
        setPropertyFromSystemProperty(USE_EJB_DISCOVERY, value);
        
        value = properties.getProperty(USE_EJBINTERCEPTOR_INJECTION);
        setPropertyFromSystemProperty(USE_EJBINTERCEPTOR_INJECTION, value);
        
        value = properties.getProperty(CONTAINER_LIFECYCLE);
        setPropertyFromSystemProperty(CONTAINER_LIFECYCLE, value);

        value = properties.getProperty(USE_JSF2_EXTENSIONS);
        setPropertyFromSystemProperty(USE_JSF2_EXTENSIONS, value);

        value = properties.getProperty(APPLICATION_IS_JSP);
        setPropertyFromSystemProperty(APPLICATION_IS_JSP, value);

        value = properties.getProperty(TRANSACTION_SERVICE);
        setPropertyFromSystemProperty(TRANSACTION_SERVICE, value);

        value = properties.getProperty(VALIDATOR_SERVICE);
        setPropertyFromSystemProperty(VALIDATOR_SERVICE, value);

        value = properties.getProperty(SECURITY_SERVICE);
        setPropertyFromSystemProperty(SECURITY_SERVICE, value);

        value = properties.getProperty(RESOURCE_INJECTION_SERVICE);
        setPropertyFromSystemProperty(RESOURCE_INJECTION_SERVICE, value);

        value = properties.getProperty(CONVERSATION_SERVICE);
        setPropertyFromSystemProperty(CONVERSATION_SERVICE, value);

        value = properties.getProperty(CONTEXTS_SERVICE);
        setPropertyFromSystemProperty(CONTEXTS_SERVICE, value);

        value = properties.getProperty(SCANNER_SERVICE);
        setPropertyFromSystemProperty(SCANNER_SERVICE, value);

        value = properties.getProperty(JNDI_SERVICE);
        setPropertyFromSystemProperty(JNDI_SERVICE, value);
        
        value = properties.getProperty(EL_ADAPTOR_CLASS);
        setPropertyFromSystemProperty(EL_ADAPTOR_CLASS, value);
        
    }
     
    private void setPropertyFromSystemProperty(String key, String value)
    {
        if(value != null)
        {
            setProperty(key, value);
        }
    }
    
    /**
     * (re)read the configuration from the resources in the classpath.
     * @see #DEFAULT_CONFIG_PROPERTIES_NAME
     * @see #CONFIG_PROPERTIES_NAME 
     */
    public synchronized void parseConfiguration() throws WebBeansConfigurationException
    {
        Properties newConfigProperties = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        
        InputStream is = loader.getResourceAsStream(DEFAULT_CONFIG_PROPERTIES_NAME);
        load(is, newConfigProperties);
        
        is = loader.getResourceAsStream(CONFIG_JMS_PROPERTIES_NAME);
        load(is, newConfigProperties);

        is = loader.getResourceAsStream(CONFIG_JSF_PROPERTIES_NAME);
        load(is, newConfigProperties);

        is = loader.getResourceAsStream(CONFIG_EE_COMMON_PROPERTIES_NAME);
        load(is, newConfigProperties);

        is = loader.getResourceAsStream(CONFIG_EE_WEB_PROPERTIES_NAME);
        load(is, newConfigProperties);

        // and now overload those settings with the ones from the more specialized version (if available)
        
        URL configUrl = loader.getResource(CONFIG_PROPERTIES_NAME);
        if (configUrl == null)
        {
            logger.info(OWBLogConst.TEXT_CONFIG_NOT_FOUND, CONFIG_PROPERTIES_NAME);
        }
        else
        {
            logger.info(OWBLogConst.TEXT_CONFIG_FOUND,  CONFIG_PROPERTIES_NAME, configUrl);

            is = loader.getResourceAsStream(CONFIG_PROPERTIES_NAME);
            load(is, newConfigProperties);
        }

        // set the new one as perfect fit.
        configProperties = newConfigProperties;
    }
    
    private void load(InputStream is, Properties newConfigProperties)
    {
        try
        {
            if(is != null)
            {
                newConfigProperties.load(is);   
            }
        }
        catch (IOException ioEx)
        {
            throw new WebBeansConfigurationException(logger.getTokenString(OWBLogConst.EDCONF_FAIL), ioEx);
        }       
        finally
        {
            if(is != null)
            {
                try
                {
                    is.close();
                }
                catch(Exception e)
                {
                     // do nothing
                }
            }
        }
    }
    
    /**
     * Gets property.
     * @param key
     * @return String with the property value or <code>null</code>
     */
    public String getProperty(String key)
    {
        return configProperties.getProperty(key);
    }
    
    /**
     * Gets property value.
     * @param key
     * @param defaultValue
     * @return String with the property value or <code>null</code>
     */
    public String getProperty(String key,String defaultValue)
    {
        return configProperties.getProperty(key, defaultValue);
    }
    
    
    /**
     * Sets given property.
     * @param key property name
     * @param value property value
     */
    public synchronized void setProperty(String key, Object value)
    {
        configProperties.put(key, value);
    }
    
    /**
     * Returns true if owb specific injection
     * false otherwise.
     * @return true if owb specific injection
     */
    public boolean isOwbSpecificFieldInjection()
    {
        String value = getProperty(USE_OWB_SPECIFIC_FIELD_INJECTION);
        
        return Boolean.valueOf(value);
    }
    
    /**
     * Return true if use JSF2.
     * @return true if use JSF2
     */
    public boolean isUseJSF2Extensions()
    {
        String value = getProperty(USE_JSF2_EXTENSIONS);
        
        return Boolean.valueOf(value);
        
    }
    
    /**
     * Gets jsp property.
     * @return true if jsp
     */
    public boolean isJspApplication()
    {
        String value = getProperty(APPLICATION_IS_JSP);
        
        return Boolean.valueOf(value);
    }
    
    /**
     * Gets conversation supports property.
     * @return true if supports
     */
    public boolean supportsConversation()
    {
        String value = getProperty(APPLICATION_SUPPORTS_CONVERSATION);
        
        return Boolean.valueOf(value);
    }
    
    /**
     * Gets EJB injection property.
     * @return true if EJB interceptor should do injection
     */
    public boolean isUseEJBInterceptorInjection()
    {
        String value = getProperty(USE_EJBINTERCEPTOR_INJECTION);
        
        return Boolean.valueOf(value);
    }
}
