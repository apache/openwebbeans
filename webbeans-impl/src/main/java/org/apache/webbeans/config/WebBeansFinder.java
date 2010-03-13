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

import java.util.HashMap;
import java.util.Map;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansNameSpaceContainer;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;
import org.apache.webbeans.xml.XMLSpecializesManager;

public class WebBeansFinder
{
    public static final String SINGLETON_MANAGER = BeanManagerImpl.class.getName();

    public static final String SINGLETON_DECORATORS_MANAGER = DecoratorsManager.class.getName();

    public static final String SINGLETON_STEREOTYPE_MANAGER = StereoTypeManager.class.getName();

    public static final String SINGLETON_INTERCEPTORS_MANAGER = InterceptorsManager.class.getName();

    public static final String SINGLETON_CONVERSATION_MANAGER = ConversationManager.class.getName();

    public static final String SINGLETON_XML_ANNOTATION_TYPE_MANAGER = XMLAnnotationTypeManager.class.getName();

    public static final String SINGLETON_XML_SPECIALIZES_MANAGER = XMLSpecializesManager.class.getName();

    public static final String SINGLETON_CREATIONAL_CONTEXT_FACTORY = CreationalContextFactory.class.getName();
    
    public static final String SINGLETON_SESSION_CONTEXT_MANAGER = "org.apache.webbeans.web.context.SessionContextManager";
    
    public static final String SINGLETON_WEBBEANS_NAMESPACE_CONTAINER = WebBeansNameSpaceContainer.class.getName();

    private static Map<String, Map<ClassLoader, Object>> singletonMap = new HashMap<String, Map<ClassLoader, Object>>();

    public static Object getSingletonInstance(String singletonName)
    {
       return getSingletonInstance(singletonName, WebBeansUtil.getCurrentClassLoader());
    }
    
    public static Object getSingletonInstance(String singletonName, ClassLoader cl)
    {
        Object object = null;

        synchronized (singletonMap)
        {
            Map<ClassLoader, Object> managerMap = singletonMap.get(singletonName);

            if (managerMap == null)
            {
                managerMap = new HashMap<ClassLoader, Object>();
                singletonMap.put(singletonName, managerMap);
            }
            object = managerMap.get(cl);
            /* No singleton for this application, create one */
            if (object == null)
            {
                try
                {

                    Class<?> clazz = cl.loadClass(singletonName);
                    
                    object = clazz.newInstance();
                    managerMap.put(cl, object);

                }
                catch (InstantiationException e)
                {
                    throw new WebBeansException("Unable to instantiate class : " + singletonName, e);
                }
                catch (IllegalAccessException e)
                {
                    throw new WebBeansException("Illegal access exception in creating instance with class : " + singletonName, e);
                }
                catch (ClassNotFoundException e)
                {
                    throw new WebBeansException("Class not found exception in creating instance with class : " + singletonName, e);                }
            }
        }

        return object;
    }
    
    /**
     * Clear all deployment instances when the application is undeployed.
     */
    public static void clearInstances()
    {
        if(singletonMap != null)
        {
            singletonMap.clear();   
        }
    }
    
    public static void removeInstance(String name)
    {
        singletonMap.remove(name);
    }
}