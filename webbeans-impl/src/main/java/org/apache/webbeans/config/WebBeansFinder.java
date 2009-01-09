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

import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

public class WebBeansFinder
{
    public static final String SINGLETON_MANAGER = "org.apache.webbeans.container.ManagerImpl";

    public static final String SINGLETON_DECORATORS_MANAGER = "org.apache.webbeans.decorator.DecoratorsManager";

    public static final String SINGLETON_DEPLOYMENT_TYPE_MANAGER = "org.apache.webbeans.deployment.DeploymentTypeManager";

    public static final String SINGLETON_STEREOTYPE_MANAGER = "org.apache.webbeans.deployment.StereoTypeManager";

    public static final String SINGLETON_NOTIFICATION_MANAGER = "org.apache.webbeans.event.NotificationManager";

    public static final String SINGLETON_INTERCEPTORS_MANAGER = "org.apache.webbeans.intercept.InterceptorsManager";

    public static final String SINGLETON_CONVERSATION_MANAGER = "org.apache.webbeans.jsf.ConversationManager";

    public static final String SINGLETON_XML_ANNOTATION_TYPE_MANAGER = "org.apache.webbeans.xml.XMLAnnotationTypeManager";

    public static final String SINGLETON_XML_SPECIALIZES_MANAGER = "org.apache.webbeans.xml.XMLSpecializesManager";

    public static final String SINGLETON_INJECTION_RESOLVER = "org.apache.webbeans.container.InjectionResolver";

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
}