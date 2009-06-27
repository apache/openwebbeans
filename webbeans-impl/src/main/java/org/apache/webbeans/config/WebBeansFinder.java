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

import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.SessionContextManager;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansNameSpaceContainer;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;
import org.apache.webbeans.xml.XMLSpecializesManager;

public class WebBeansFinder
{
    public static final String SINGLETON_MANAGER = ManagerImpl.class.getName();

    public static final String SINGLETON_DECORATORS_MANAGER = DecoratorsManager.class.getName();

    public static final String SINGLETON_DEPLOYMENT_TYPE_MANAGER = DeploymentTypeManager.class.getName();

    public static final String SINGLETON_STEREOTYPE_MANAGER = StereoTypeManager.class.getName();

    public static final String SINGLETON_INTERCEPTORS_MANAGER = InterceptorsManager.class.getName();

    public static final String SINGLETON_CONVERSATION_MANAGER = ConversationManager.class.getName();

    public static final String SINGLETON_XML_ANNOTATION_TYPE_MANAGER = XMLAnnotationTypeManager.class.getName();

    public static final String SINGLETON_XML_SPECIALIZES_MANAGER = XMLSpecializesManager.class.getName();

    public static final String SINGLETON_CREATIONAL_CONTEXT_FACTORY = CreationalContextFactory.class.getName();
    
    public static final String SINGLETON_SESSION_CONTEXT_MANAGER = SessionContextManager.class.getName();
    
    public static final String SINGLETON_WEBBEANS_NAMESPACE_CONTAINER = WebBeansNameSpaceContainer.class.getName();

    private static Map<String, Map<ClassLoader, Object>> singletonMap = new HashMap<String, Map<ClassLoader, Object>>();

    public static Object getSingletonInstance(String singletonName)
    {
        Object object = null;

        synchronized (singletonMap)
        {
            ClassLoader classLoader = WebBeansUtil.getCurrentClassLoader();
            Map<ClassLoader, Object> managerMap = singletonMap.get(singletonName);

            if (managerMap == null)
            {
                managerMap = new HashMap<ClassLoader, Object>();
                singletonMap.put(singletonName, managerMap);
            }
            object = managerMap.get(classLoader);
            /* No singleton for this application, create one */
            if (object == null)
            {
                Class<?> clazz = ClassUtil.getClassFromName(singletonName);
                if (clazz == null)
                {
                    throw new WebBeansException("Cannot find class : " + singletonName);
                }
                try
                {
                    object = clazz.newInstance();
                    managerMap.put(classLoader, object);

                }
                catch (InstantiationException e)
                {
                    throw new WebBeansException("Unable to instantiate class : " + singletonName, e);
                }
                catch (IllegalAccessException e)
                {
                    throw new WebBeansException("Illegal access exception in creating instance with class : " + singletonName, e);
                }
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