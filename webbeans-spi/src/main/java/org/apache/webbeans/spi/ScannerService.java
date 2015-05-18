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
package org.apache.webbeans.spi;

import java.net.URL;
import java.util.Set;


/**
 * <p>This SPI is for abstracting the class scanning.</p>
 *
 * <p>In a production environment Many different modules need to perform
 * class scanning (EJB, JSF, JPA, ...). This SPI allows us to only have one 
 * central class scanner for the whole application server
 * which only performs the scanning once at startup of each WebApp.</p>
 *
 * <p>All URL path Strings in this interface contain the the protocol,
 * e.g. 'file:/...' we get directly from {@link java.net.URL#toExternalForm()}</p>
 *
 */
public interface ScannerService
{
    /**
     * Any initialisation action that is
     * required by the implementation.
     * @param object initialization object
     */
    void init(Object object);
    
    /**
     * Perform the actual class scanning.
     */
    void scan();


    /**
     * This method will get called once the information found by the current
     * scan is not needed anymore and the ScannerService might free up
     * resources.
     */
    void release();

    
    /**
     * Get the URLs of all bean archives in the deployment.
     * In OWB-1.x this did give the base paths to META-INF/beans.xml
     * files. Now, this will either return the the beans.xml locations
     * or the base URL for the JAR if it is an 'implicit bean archive'.
     * @return the URL of the beans.xml files.
     */
    Set<URL> getBeanXmls();
    
    /**
     * Gets beans classes that are found in the
     * deployment archives. 
     * @return bean classes
     */
    Set<Class<?>> getBeanClasses();

    /**
     * Indicates if BDABeansXmlScanner is available. This method 
     * should only return true if a BDABeansXmlScanner is implemented
     * and the OpenWebBeansConfiguration.USE_BDA_BEANSXML_SCANNER 
     * custom property is set to true.
     * @return T - BDABeansXmlScanner is available and enabled;
     * F - No BDABeansXmlScanner is available or it is disabled
     */
    boolean isBDABeansXmlScanningEnabled();
    
    /**
     * Gets BDABeansXMLScanner used to determine the beans.xml 
     * modifiers (interceptors, decorators, and, alternatives) that
     * are enabled per BDA. This is different from the default behavior
     * that enables modifiers per application and not just in one BDA
     * contained in an application.
     * @return null or reference to BDABeansXMLScanner
     */
    BDABeansXmlScanner getBDABeansXmlScanner();
}
