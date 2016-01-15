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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
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
    private static final Logger logger = WebBeansLoggerFacade.getLogger(OpenWebBeansConfiguration.class);

    /**
     * Conversation periodic delay in ms. This is not used by OWB out of the box.
     * Some Containers might implement it though.
     */
    public static final String CONVERSATION_PERIODIC_DELAY = "org.apache.webbeans.conversation.Conversation.periodicDelay";

    /**Timeout interval in ms*/
    public static final String CONVERSATION_TIMEOUT_INTERVAL = "org.apache.webbeans.conversation.Conversation.timeoutInterval";

    /**
     * Environment property which comma separated list of classes which
     * should NOT fail with UnproxyableResolutionException
     */
    public static final String ALLOW_PROXYING_PARAM = "javax.enterprise.inject.allowProxying.classes";

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

    /** A list of known JARs/paths which should not be scanned for beans */
    public static final String SCAN_EXCLUSION_PATHS = "org.apache.webbeans.scanExclusionPaths";

    /**
     * a comma-separated list of fully qualified class names that should be ignored
     * when determining if a decorator matches its delegate.  These are typically added by
     * weaving or bytecode modification.
     */
    public static final String IGNORED_INTERFACES = "org.apache.webbeans.ignoredDecoratorInterfaces";

    /**
     * By default we do _not_ force session creation in our WebBeansConfigurationListener. We only create the
     * Session if we really need the SessionContext. E.g. when we create a Contextual Instance in it.
     * Sometimes this creates a problem as the HttpSession can only be created BEFORE anything got written back
     * to the client.
     * With this configuration you can choose between 3 settings
     * <ul>
     *     <li>&quot;true&quot; the Session will <u>always</u> eagerly be created at the begin of a request</li>
     *     <li>&quot;false&quot; the Session will <u>never</u> eagerly be created but only lazily when the first &#064;SessionScoped bean gets used</li>
     *     <li>any other value will be interpreted as Java regular expression for request URIs which need eager Session initialization</li>
     * </ul>
     */
    public static final String EAGER_SESSION_INITIALISATION = "org.apache.webbeans.web.eagerSessionInitialisation";

    /**
     * The Java Version to use for the generated proxy classes.
     * If "auto" then we will pick the version of the current JVM.
     * The default is set to "1.6" as some tools in jetty/tomcat/etc still
     * cannot properly handle Java8 (mostly due to older Eclipse JDT versions).
     */
    public static final String GENERATOR_JAVA_VERSIN = "org.apache.webbeans.generator.javaVersion";


    /**Default configuration files*/
    private static final String DEFAULT_CONFIG_PROPERTIES_NAME = "META-INF/openwebbeans/openwebbeans.properties";

    private static final String AUTO_CONFIG = "auto";

    /**Property of application*/
    private final Properties configProperties = new Properties();


    private Set<String> ignoredInterfaces;

    /**
     * All configured lists per key.
     *
     * For a single key the following configuration sources will get parsed:
     * <ul>
     *     <li>all {@link #DEFAULT_CONFIG_PROPERTIES_NAME} files</li>
     *     <li>{@code System.getProperties()}</li>
     *     <li>{@code System.env()}</li>
     * </ul>
     *
     * All the found values will get split by comma (',') and all trimmed values
     * will get stored in a Set.
     *
     */
    private Map<String, Set<String>> configuredLists = new HashMap<String, Set<String>>();


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

        configProperties.clear();

        // set the new one as perfect fit.
        if(newConfigProperties != null)
        {
            overrideWithGlobalSettings(newConfigProperties);

            configProperties.putAll(newConfigProperties);
        }

    }

    /**
     * Take the given commaSeparatedVals and spit them by ',' and trim them.
     * @return all trimmed values or an empty list
     */
    public List<String> splitValues(String commaSeparatedVals)
    {
        ArrayList<String> values = new ArrayList<String>();
        if (commaSeparatedVals != null)
        {
            for (String value : commaSeparatedVals.split(","))
            {
                value = value.trim();
                if (!value.isEmpty())
                {
                    values.add(value);
                }
            }
        }
        return values;
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

    /**
     * Scan all openwebbeans.properties files + system properties +
     * syste.env for the given key.
     * If the key is comma separated then use the separate tokens.
     * All the values get put into a big set.
     */
    public synchronized Set<String> getConfigListValues(String keyName)
    {
        Set<String> allValues = configuredLists.get(keyName);
        if (allValues != null)
        {
            return allValues;
        }

        allValues = new HashSet<String>();
        try
        {
            List<Properties> properties = PropertyLoader.loadAllProperties(DEFAULT_CONFIG_PROPERTIES_NAME);
            if (properties != null)
            {
                for (Properties property : properties)
                {
                    String values = (String) property.get(keyName);
                    allValues.addAll(splitValues(values));
                }
            }
        }
        catch (IOException e)
        {
            logger.log(Level.SEVERE, "Error while loading the propertyFile " + DEFAULT_CONFIG_PROPERTIES_NAME, e);
            return Collections.EMPTY_SET;
        }

        // search for the key in the properties
        String value = System.getProperty(keyName);
        allValues.addAll(splitValues(value));

        // search for the key in the environment
        String envKeyName = keyName.toUpperCase().replace(".", "_");
        value = System.getenv(envKeyName);
        allValues.addAll(splitValues(value));

        configuredLists.put(keyName, allValues);

        return allValues;
    }

    /**
     * Add a configuration value to the Set of configured values registered
     * under the keyName.
     *
     * Calling this method ensures that all the configured values are first
     * read from the environment and configuration properties.
     *
     * @see #getConfigListValues(String) get's called internally to insure the list is initialised
     */
    public synchronized void addConfigListValue(String keyName, String value)
    {
        Set<String> configListValues = getConfigListValues(keyName);
        configListValues.add(value);
    }

    public boolean supportsInterceptionOnProducers()
    {
        return "true".equals(getProperty(PRODUCER_INTERCEPTION_SUPPORT, "true"));
    }

    public String getGeneratorJavaVersion()
    {
        String generatorJavaVersion = getProperty(GENERATOR_JAVA_VERSIN);
        if (generatorJavaVersion == null || AUTO_CONFIG.equals(generatorJavaVersion))
        {
            return System.getProperty("java.version");
        }

        return generatorJavaVersion;
    }
}
