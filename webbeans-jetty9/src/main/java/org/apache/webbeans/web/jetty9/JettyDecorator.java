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

package org.apache.webbeans.web.jetty9;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.util.Decorator;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Jetty support for OpenWebBeans.
 */
public class JettyDecorator implements Decorator
{
    private static final Logger log = Log.getLogger(JettyDecorator.class);

    private ClassLoader loader;
    private Map<Object, Object> objects = new ConcurrentHashMap<>();

    public JettyDecorator(ClassLoader loader)
    {
        this.loader = loader;
    }

    @Override
    public <T> T decorate(T object)
    {
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("Injecting the dependencies for OpenWebBeans, " +
                        "instance : " + object);
            }

            Object injectorInstance = JettyUtil.inject(object, loader);
            if (injectorInstance != null)
            {
                objects.put(object, injectorInstance);
            }
        }
        catch (Exception e)
        {
            log.warn("Error is occured while injecting the OpenWebBeans " +
                    "dependencies for instance " + object, e);
        }
        return object;
    }

    @Override
    public void destroy(Object instance)
    {
        Object injectorInstance = objects.get(instance);
        if (injectorInstance != null)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Destroying the OpenWebBeans injector instance");
                }
                JettyUtil.destroy(injectorInstance, loader);
            }
            catch (Exception e)
            {
                log.warn("Erros is occured while destroying the OpenWebBeans injector instance", e);
            }
        }
    }
}
