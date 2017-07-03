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
package org.apache.webbeans.util;


/**
 * Web beans related constants.
 * 
 * @version $Rev$ $Date$
 */
public final class WebBeansConstants
{

    private WebBeansConstants()
    {
        throw new UnsupportedOperationException();
    }

    public static final String [] OWB_INJECTABLE_RESOURCE_ANNOTATIONS = {"javax.ejb.EJB",
                                                                         "javax.annotation.Resource",
                                                                         "javax.xml.ws.WebServiceRef",
                                                                         "javax.persistence.PersistenceUnit",
                                                                         "javax.persistence.PersistenceContext"};
    
    public static final String WEB_BEANS_XML_INTERCEPTORS_ELEMENT = "interceptors";
    public static final String WEB_BEANS_XML_DECORATORS_ELEMENT = "decorators";
    public static final String WEB_BEANS_XML_ALLOW_PROXYING_ELEMENT = "allowProxying";
    public static final String WEB_BEANS_XML_ALTERNATIVES_ELEMENT = "alternatives";
    public static final String WEB_BEANS_XML_SCAN_ELEMENT = "scan";
    /** Having this tag in beans.xml fordes bean-discovery-mode="scoped" in a backward compat way */
    public static final String WEB_BEANS_XML_SCOPED_BEANS_ONLY_ELEMENT = "trim";

    public static final String WEB_BEANS_XML_CLASS = "class";
    public static final String WEB_BEANS_XML_STEREOTYPE = "stereotype";
    public static final String WEB_BEANS_XML_EXCLUDE = "exclude";
    public static final String WEB_BEANS_XML_IF_CLASS_NOT_AVAILABLE = "if-class-not-available";
    public static final String WEB_BEANS_XML_IF_CLASS_AVAILABLE = "if-class-available";
    public static final String WEB_BEANS_XML_IF_SYSTEM_PROPERTY = "if-system-property ";

    /**JNDI name of the {@link javax.enterprise.inject.spi.BeanManager} instance*/
    public static final String WEB_BEANS_MANAGER_JNDI_NAME = "java:comp/BeanManager";
        public final static String WEB_BEANS_MESSAGES = "openwebbeans/Messages";

}
