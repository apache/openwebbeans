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
package org.apache.webbeans.util;

import javax.enterprise.inject.spi.BeanManager;

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
    
    /** Prefix of the web beans package */
    public static final String WEB_BEANS_PREFIX = "javax.webbeans";

    /** Webbeans systemId URL */
    public static final String WEB_BEANS_XML_SYSID = "http://java.sun.com/jee/web-beans-1.0.xsd";

    /** Classpath systemId */
    public static final String CLASSPATH_URI_SCHEMA = "classpath:";

    /** WebBeans namespace */
    public static final String WEB_BEANS_NAMESPACE = "urn:java:ee";

    /** <Deploy> XML Element */
    public static final String WEB_BEANS_XML_DEPLOY_ELEMENT = "Deploy";

    /** <Interceptors> XML Element */
    public static final String WEB_BEANS_XML_INTERCEPTORS_ELEMENT = "Interceptors";
    
    public static final String WEB_BEANS_XML_SPEC_SPECIFIC_INTERCEPTORS_ELEMENT = "interceptors";
    
    public static final String WEB_BEANS_XML_SPEC_SPECIFIC_DECORATORS_ELEMENT = "decorators";
    
    public static final String WEB_BEANS_XML_SPEC_SPECIFIC_DEPLOY_ELEMENT = "deploy";
    
    public static final String WEB_BEANS_XML_OWB_SPECIFIC_ALTERNATIVES = "Alternatives";
    
    public static final String WEB_BEANS_XML_SPEC_SPECIFIC_ALTERNATIVES = "alternatives";
    
    public static final String WEB_BEANS_XML_SPEC_SPECIFIC_CLASS = "class";
    
    public static final String WEB_BEANS_XML_SPEC_SPECIFIC_STEREOTYPE = "stereotype";
    
    public static final String WEB_BEANS_XML_OWB_SPECIFIC_CLASS = "Class";
    
    public static final String WEB_BEANS_XML_OWB_SPECIFIC_STEREOTYPE = "Stereotype";
    
    public static final String WEB_BEANS_XML_INTERCEPTOR_ELEMENT = "Interceptor";

    /** <Decorators> XML Element */
    public static final String WEB_BEANS_XML_DECORATORS_ELEMENT = "Decorators";

    public static final String WEB_BEANS_XML_DECORATOR_ELEMENT = "Decorator";

    /** <Queue> XML Element */
    public static final String WEB_BEANS_XML_TOPIC_ELEMENT = "Topic";

    /** <Topic> XML Element */
    public static final String WEB_BEANS_XML_QUEUE_ELEMENT = "Queue";
    
    public static final String WEB_BEANS_XML_JMS_RESOURCE = "Resource";
    
    public static final String WEB_BEANS_XML_JMS_RESOURCE_NAME = "name";
    
    public static final String WEB_BEANS_XML_JMS_RESOURCE_MAPPED_NAME = "mappedName";

    public static final String WEB_BEANS_XML_INITIALIZER_ELEMENT = "Inject";

    public static final String WEB_BEANS_XML_DESTRUCTOR_ELEMENT = "Destructor";

    public static final String WEB_BEANS_XML_PRODUCES_ELEMENT = "Produces";

    public static final String WEB_BEANS_XML_DISPOSES_ELEMENT = "Disposes";

    public static final String WEB_BEANS_XML_OBSERVES_ELEMENT = "Observers";

    public static final String WEB_BEANS_XML_DECORATES_ELEMENT = "Decorates";

    public static final String WEB_BEANS_XML_STANDART_ELEMENT = "Standard";

    public static final String WEB_BEANS_XML_BINDING_TYPE = "BindingType";

    public static final String WEB_BEANS_XML_INTERCEPTOR_BINDING_TYPE = "InterceptorBindingType";

    public static final String WEB_BEANS_XML_STEREOTYPE = "StereoType";

    public static final String WEB_BEANS_XML_VALUE_ELEMENT = "value";

    public static final String WEB_BEANS_XML_NAMED_ELEMENT = "Named";

    public static final String WEB_BEANS_XML_ARRAY_ELEMENT = "Array";

    /**JNDI name of the {@link BeanManager} instance*/
    public static final String WEB_BEANS_MANAGER_JNDI_NAME = "java:comp/BeanManager";

}
