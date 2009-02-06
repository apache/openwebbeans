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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;

/**
 * This is the internal helper class for low level access to JNDI
 * @see org.apache.webbeans.spi.JNDIService for transparent access over SPI 
 */
public final class JNDIUtil
{
    private static InitialContext initialContext = null;

    private static final WebBeansLogger LOGGER = WebBeansLogger.getLogger(JNDIUtil.class);

    static
    {
        try
        {
            initialContext = new InitialContext();

        }
        catch (Exception e)
        {
            LOGGER.error("Unable to initialize InitialContext object", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    private JNDIUtil()
    {

    }

    public static InitialContext getInitialContext()
    {
        return initialContext;
    }

    public static void bind(String name, Object object)
    {
        Asserts.assertNotNull(name, "name parameter can not be null");
        Asserts.assertNotNull(object, "object parameter can not be null");

        try
        {
            initialContext.bind(name, object);

        }
        catch (NamingException e)
        {
            LOGGER.error("Unable to bind object with name : " + name, e);
        }
    }

    public static void unbind(String name)
    {
        Asserts.assertNotNull(name, "name parameter can not be null");

        try
        {
            initialContext.unbind(name);

        }
        catch (NamingException e)
        {
            LOGGER.error(e);
            throw new WebBeansException("Unable to unbind object with name : " + name, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T lookup(String name, Class<? extends T> expectedClass) throws WebBeansException
    {
        Asserts.assertNotNull(name, "name parameter can not be null");

        try
        {
            return (T) initialContext.lookup(name);
        } catch (NamingException e)
        {
            LOGGER.error(e);
            throw new WebBeansException("Unable to lookup object with name : " + name, e);
        }
    }

}
