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

/* Requires the following import where referenced:
 * import org.apache.webbeans.config.OWBLogConst;
 */

public class OWBLogConst
{
    public final static String DEFAULT_MSGS_PROPERTIES_NAME = "javax.openwebbeans.Messages";

    public final static String TEXT_INTERCEPT_CLASS  = "TEXT_INTERCEPT_CLASS";  // Interceptor Class : 
    public final static String TEXT_ANNO_CLASS       = "TEXT_ANNO_CLASS";       // Annotated Decorator Class : 
    public final static String TEXT_XML_CLASS        = "TEXT_XML_CLASS";        // XML based Decorator Class : 
    public final static String TEXT_CONFIG_PROP      = "TEXT_CONFIG_PROP";      // Config properties [
    public final static String TEXT_CONFIG_NOT_FOUND = "TEXT_CONFIG_NOT_FOUND"; // ] not found. Using default settings.
    public final static String TEXT_CONFIG_FOUND     = "TEXT_CONFIG_FOUND";     // ] found at location :
    public final static String TEXT_OVERRIDING       = "TEXT_OVERRIDING";       // . Overriding default settings.
    public final static String TEXT_MB_IMPL          = "TEXT_MB_IMPL";          // Managed Bean implementation class : 
    public final static String TEXT_SAME_SCOPE       = "TEXT_SAME_SCOPE";       //  stereotypes must declare the same @Scope annotations.
    public final static String TEXT_JAVA_TYPENAME    = "TEXT_JAVA_TYPENAME";    // Java type with name : 

    public final static String INFO_0001 = "INFO_0001"; // Using discovery service implementation class : [{1}]
    public final static String INFO_0002 = "INFO_0002"; // OpenWebBeans Container is started, it took {1} ms.
    public final static String INFO_0003 = "INFO_0003"; // OpenWebBeans Container is stopped for context path, 
    public final static String INFO_0004 = "INFO_0004"; // Session is passivated. Session id : [{1}] 
    public final static String INFO_0005 = "INFO_0005"; // Session is activated. Session id : [{1}] 
    public final static String INFO_0006 = "INFO_0006"; // Starting configuration of Web Beans {1}
    public final static String INFO_0007 = "INFO_0007"; // Finished configuration of Web Beans {1}
    public final static String INFO_0008 = "INFO_0008"; // All injection points are validated successfully.
    public final static String INFO_0009 = "INFO_0009"; // Found Managed Bean with class name : [{1}]
    public final static String INFO_0010 = "INFO_0010"; // Found Enterprise Bean with class name : [{1}]
    public final static String INFO_0011 = "INFO_0011"; // Found Managed Bean Interceptor with class name : [{1}]
    public final static String INFO_0012 = "INFO_0012"; // Found Managed Bean Decorator with class name : [{1}]
    public final static String INFO_0013 = "INFO_0013"; // Adding OpenWebBeansPlugin :
    public final static String INFO_0014 = "INFO_0014"; // Destroying the conversation context with cid: [{0}] for view: [{1}]
    public final static String INFO_0015 = "INFO_0015"; // Restoring conversation with cid: [{0}] for view: [{1}]
    public final static String INFO_0016 = "INFO_0016"; // Creating a new transitional conversation for view: [{0}]

    public final static String WARN_0001 = "WARN_0001"; // No plugins to shutDown.
    public final static String WARN_0002 = "WARN_0002"; // Alternative XML content is wrong. Child of <alternatives> must be <class>,<stereotype> but found : 
    public final static String WARN_0003 = "WARN_0003"; // Discovery service not found. Continue by using MetaDataDiscoveryStandard as a default.
    public final static String WARN_0004 = "WARN_0004"; // OpenWebBeans Container is already started.
    public final static String WARN_0005 = "WARN_0005"; // OpenWebBeans Container is already stopped.
    public final static String WARN_0006 = "WARN_0006"; // Conversation already started with cid : [{1}]
    public final static String WARN_0007 = "WARN_0007"; // Conversation already ended with cid : [{1}]
    public final static String WARN_0008 = "WARN_0008"; // Problems processing type {0}. Type will be ignored. (Exception: {1} {2})

    public final static String ERROR_0001 = "ERROR_0001"; // Unable to inject resource for : [{1}]
    public final static String ERROR_0002 = "ERROR_0002"; // Initialization of the WebBeans container has failed.
    public final static String ERROR_0003 = "ERROR_0003"; // An exception has occurred in the transactional observer.
    public final static String ERROR_0004 = "ERROR_0004"; // Unable to initialize InitialContext object.
    public final static String ERROR_0005 = "ERROR_0005"; // Unable to bind object with name : [{1}]
    public final static String ERROR_0006 = "ERROR_0006"; // Security exception. Cannot access decorator class: [{1}] method : [{2}]
    public final static String ERROR_0007 = "ERROR_0007"; // Delegate field is not found on the given decorator class : [{1}]
    public final static String ERROR_0008 = "ERROR_0008"; // Error occurred while executing {1}
    public final static String ERROR_0009 = "ERROR_0009"; // Error while shutting down the plugin : [{1}]
    public final static String ERROR_0010 = "ERROR_0010"; // An error occurred while closing the JMS instance.
    public final static String ERROR_0011 = "ERROR_0011"; // Method security access violation for method : [{1}] in decorator class : [{2}]
    public final static String ERROR_0012 = "ERROR_0012"; // Exception in calling method : [{1}] in decorator class : [{2}]. Look in the log for target checked exception.
    public final static String ERROR_0013 = "ERROR_0013"; // Got an Exception while starting a fresh session!
    public final static String ERROR_0014 = "ERROR_0014"; // Method illegal access for method : [{1}] in decorator class : [{2}]
    public final static String ERROR_0015 = "ERROR_0015"; // Illegal access exception for field : [{1}] in decorator class : [{2}]
    public final static String ERROR_0016 = "ERROR_0016"; // IllegalArgumentException has occurred while calling the field: [{1}] on the class: [{2}]
    public final static String ERROR_0017 = "ERROR_0017"; // IllegalAccessException has occurred while calling the field: [{1}] on the class: [{2}]

    public final static String FATAL_0001 = "FATAL_0001"; // Exception thrown while destroying bean instance : {1}
    public final static String FATAL_0002 = "FATAL_0002"; // Unable to read root element of the given input stream.

    public final static String EDCONF_FAIL = "CRITICAL_DEFAULT_CONFIG_FAILURE"; // Problem while loading OpenWebBeans default configuration.
    public final static String ESCONF_FAIL = "CRITICAL_SPECIAL_CONFIG_FAILURE"; // Problem while loading OpenWebBeans specialized configuration.
    public final static String EXCEPT_0001 = "EXCEPT_0001"; // Wrong initialization object.
    public final static String EXCEPT_0002 = "EXCEPT_0002"; // Wrong ended object.
    public final static String EXCEPT_0003 = "EXCEPT_0003"; // Specialized class [
    public final static String EXCEPT_0004 = "EXCEPT_0004"; // ] must extend another class.
    public final static String EXCEPT_XML  = "EXCEPT_XML";  // XML Specialization Error : 
    public final static String EXCEPT_0005 = "EXCEPT_0005"; // More than one class specialized the same super class :
    public final static String EXCEPT_0006 = "EXCEPT_0006"; // Got Exceptions while sending shutdown to the following plugins : 
    public final static String EXCEPT_0007 = "EXCEPT_0007"; // TransactionPhase not supported: 
    public final static String EXCEPT_0008 = "EXCEPT_0008"; // Exception is thrown while handling event object with type : 
    public final static String EXCEPT_0009 = "EXCEPT_0009"; // Unable to unbind object with name : 
    public final static String EXCEPT_0010 = "EXCEPT_0010"; // Unable to lookup object with name : 
    public final static String EXCEPT_0011 = "EXCEPT_0011"; // Could not find Decorator delegate attribute for decorator class : 
    public final static String EXCEPT_0012 = "EXCEPT_0012"; // All elements in the beans.xml file have to declare name space.
    public final static String EXCEPT_0013 = "EXCEPT_0013"; // Unable to read root element of the given input stream.
    public final static String EXCEPT_0014 = "EXCEPT_0014"; // Multiple class with name : 
}
