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
package org.apache.webbeans.web.tomcat7;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.InstanceManager;
import org.apache.webbeans.util.ExceptionUtil;

public class TomcatInstanceManager implements InstanceManager
{
    private static final Log log = LogFactory.getLog(TomcatInstanceManager.class);

    private InstanceManager processor;

    private ClassLoader loader;

    private Map<Object, Object> objects = new ConcurrentHashMap<>();

    public TomcatInstanceManager(ClassLoader loader, InstanceManager processor)
    {
        this.processor = processor;
        this.loader = loader;
    }

    @Override
    public void destroyInstance(Object instance) throws IllegalAccessException, InvocationTargetException
    {
        Object injectorInstance = this.objects.remove(instance);
        if (injectorInstance != null)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Destroying the OpenWebBeans injector instance");
                }
                TomcatUtil.destroy(injectorInstance, loader);
            }
            catch (Exception e)
            {
                log.error("Error is occured while destroying the OpenWebBeans injector instance", e);
            }
        }
        this.processor.destroyInstance(instance);
        if (log.isDebugEnabled())
        {
            log.debug("Number of 'objects' map entries after destroying instance: " + this.objects.size());
        }
    }

    @Override
    public Object newInstance(Class<?> aClass) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException
    {
        // Creates a defaut instance
        try
        {
            Object object = this.processor.newInstance(aClass);

            // Inject dependencies
            inject(object);

            return object;
        }
        catch (Exception e)
        {
            // sadly this is required as the Tomcat InstanceManager introduced an additional Exception in their signature :(
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    @Override
    public Object newInstance(String str) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException
    {
        try
        {
            // Creates a defaut instance
            Object object = this.processor.newInstance(str);

            // Inject dependencies
            inject(object);

            return object;
        }
        catch (Exception e)
        {
            // sadly this is required as the Tomcat InstanceManager introduced an additional Exception in their signature :(
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    @Override
    public void newInstance(Object object) throws IllegalAccessException, InvocationTargetException, NamingException
    {
        // Inject dependencies
        inject(object);
    }

    @Override
    public Object newInstance(String str, ClassLoader cl) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException
    {
        try
        {
            // Creates a defaut instance
            Object object = this.processor.newInstance(str, cl);

            // Inject dependencies
            inject(object);

            return object;
        }
        catch (Exception e)
        {
            // sadly this is required as the Tomcat InstanceManager introduced an additional Exception in their signature :(
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    private void inject(Object object)
    {
        try
        {
            if(log.isDebugEnabled())
            {
                log.debug("Injecting the dependencies for OpenWebBeans, " +
                          "instance : " + object);
            }

            Object injectorInstance = TomcatUtil.inject(object, loader);
            if (injectorInstance != null)
            {
                this.objects.put(object, injectorInstance);
            }
            if (log.isDebugEnabled())
            {
                log.debug("Number of 'objects' map entries after injecting instance: " + this.objects.size());
            }
        }
        catch (Exception e)
        {
            log.error("Error is occured while injecting the OpenWebBeans " +
                      "dependencies for instance " + object,e);
        }
    }

}
