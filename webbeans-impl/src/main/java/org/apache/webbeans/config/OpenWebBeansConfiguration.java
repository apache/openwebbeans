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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;

/**
 * Defines configuration for OpenWebBeans.
 * 
 * The algorithm is easy:
 * <ul>
 * <li>Load all properties you can find with the name (META-INF/openwebbeans/openwebbeans.properties),</li>
 * <li>Sort the property files via their configuration.ordinal in ascending order,</li>
 * <li>Overload them in a loop,</li>
 * <li><Overload them via System.getProperties/li>
 * <li><Overload them via System.getenv/li>
 * <li>Use the final list of properties.</li>
 * </ul>
 */
public class OpenWebBeansConfiguration
{
    /**Logger instance*/
    private final static Logger logger = WebBeansLoggerFacade.getLogger(OpenWebBeansConfiguration.class);

    /**Default configuration files*/
    private final static String DEFAULT_CONFIG_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans.properties";
    
    /**Property of application*/
    private final Properties configProperties = new Properties();
        
    /**Conversation periodic delay in ms.*/
    public static final String CONVERSATION_PERIODIC_DELAY = "org.apache.webbeans.conversation.Conversation.periodicDelay";
    
    /**Timeout interval in ms*/
    public static final String CONVERSATION_TIMEOUT_INTERVAL = "org.apache.webbeans.conversation.Conversation.timeoutInterval";

    /**
     * Lifycycle methods like {@link javax.annotation.PostConstruct} and
     * {@link javax.annotation.PreDestroy} must not define a checked Exception
     * regarding to the spec. But this is often unnecessary restrictive so we
     * allow to disable this check application wide.
     */
    public static final String INTERCEPTOR_FORCE_NO_CHECKED_EXCEPTIONS = "org.apache.webbeans.forceNoCheckedExceptions";

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

    /** @Produces with interceptor/decorator support */
    public static final String PRODUCER_INTERCEPTION_SUPPORT = "org.apache.webbeans.application.supportsProducerInterception";

    /**EL Adaptor*/
    public static final String EL_ADAPTOR_CLASS = "org.apache.webbeans.spi.adaptor.ELAdaptor";

    /** prefix followed by the fully qualified scope name, for configuring InterceptorHandlers for our proxies.*/
    public static final String PROXY_MAPPING_PREFIX = "org.apache.webbeans.proxy.mapping.";

    /**
     * Use BDABeansXmlScanner to determine if interceptors, decorators, and
     * alternatives are enabled in the beans.xml of a given BDA. For an
     * application containing jar1 and jar2, this implies that an interceptor
     * enabled in the beans.xml of jar1 is not automatically enabled in jar2
     * @deprecated as spec section 5 and 12 contradict each other and the BDA per jar handling is broken anyway
     **/
    public static final String USE_BDA_BEANSXML_SCANNER = "org.apache.webbeans.useBDABeansXMLScanner";

    /**
     * a comma-separated list of fully qualified class names that should be ignored
     * when determining if a decorator matches its delegate.  These are typically added by
     * weaving or bytecode modification.
     */
    public static final String IGNORED_INTERFACES = "org.apache.webbeans.ignoredDecoratorInterfaces";

    private Set<String> ignoredInterfaces;

    /**
     * you can configure this externally as well.
     *
     * @param properties
     */
    public OpenWebBeansConfiguration(Properties properties)
    {
        this();

        // and override all settings with the given properties
        configProperties.putAll(properties);
    }

    /**
     * Parse configuration.
     */
    public OpenWebBeansConfiguration()
    {
        parseConfiguration();
    }


    /**
     * (re)read the configuration from the resources in the classpath.
     * @see #DEFAULT_CONFIG_PROPERTIES_NAME
     * @see #DEFAULT_CONFIG_PROPERTIES_NAME
     */
    public synchronized void parseConfiguration() throws WebBeansConfigurationException
    {
        Properties newConfigProperties = PropertyLoader.getProperties(DEFAULT_CONFIG_PROPERTIES_NAME);

        overrideWithGlobalSettings(newConfigProperties);

        configProperties.clear();
        // set the new one as perfect fit.
        if(newConfigProperties != null)
        {
            configProperties.putAll(newConfigProperties);
        }

    }

    private void overrideWithGlobalSettings(Properties configProperties)
    {
        logger.fine("Overriding properties from System and Env properties");

        Properties systemProperties;
        if(System.getSecurityManager() != null)
        {
            systemProperties = doPrivilegedGetSystemProperties();
        }
        else
        {
            systemProperties = System.getProperties();
        }

        Map<String, String> systemEnvironment = System.getenv();

        for (Map.Entry property : configProperties.entrySet())
        {
            String key = (String) property.getKey();
            String value = (String) property.getValue();

            value = systemProperties.getProperty(key) != null ? systemProperties.getProperty(key) : value;
            value = systemEnvironment.get(key) != null ? systemEnvironment.get(key) : value;

            configProperties.put(key, value);
        }
    }

    private Properties doPrivilegedGetSystemProperties()
    {
        return AccessController.doPrivileged(
                new PrivilegedAction<Properties>()
                {
                    @Override
                    public Properties run()
                    {
                        return System.getProperties();
                    }

                }
        );
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

    public synchronized Set<String> getIgnoredInterfaces()
    {
        if (ignoredInterfaces == null)
        {
            String ignoredInterfacesString = getProperty(IGNORED_INTERFACES);
            if (ignoredInterfacesString != null)
            {
                ignoredInterfaces = new HashSet<String>(Arrays.asList(ignoredInterfacesString.split("[,\\p{javaWhitespace}]")));
            }
            else
            {
                ignoredInterfaces = Collections.emptySet();
            }
        }
        return ignoredInterfaces;
    }

    public boolean supportsInterceptionOnProducers()
    {
        return "true".equals(getProperty(PRODUCER_INTERCEPTION_SUPPORT, "true"));
    }
}
