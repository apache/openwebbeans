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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import javassist.util.proxy.ProxyFactory;
import org.apache.webbeans.exception.WebBeansException;

/** @deprecated  use SecurityService instead */
public class SecurityUtil
{
    private static final int METHOD_CLASS_GETDECLAREDMETHOD = 0x03;

    private static final int METHOD_CLASS_GETDECLAREDMETHODS = 0x04;

    private static final int METHOD_CLASS_GETDECLAREDFIELD = 0x05;

    private static final int METHOD_CLASS_GETDECLAREDFIELDS = 0x06;
    
    private static final PrivilegedActionGetSystemProperties SYSTEM_PROPERTY_ACTION = new PrivilegedActionGetSystemProperties();

    public static <T> Method[] doPrivilegedGetDeclaredMethods(Class<T> clazz)
    {
        Object obj = AccessController.doPrivileged(
                new PrivilegedActionForClass(clazz, null, METHOD_CLASS_GETDECLAREDMETHODS));
        return (Method[])obj;
    }

    public static <T> Field[] doPrivilegedGetDeclaredFields(Class<T> clazz)
    {
        Object obj = AccessController.doPrivileged(
                new PrivilegedActionForClass(clazz, null, METHOD_CLASS_GETDECLAREDFIELDS));
        return (Field[])obj;
    }

    protected static class PrivilegedActionForClass implements PrivilegedAction<Object>
    {
        private Class<?> clazz;

        private Object parameters;

        private int method;

        protected PrivilegedActionForClass(Class<?> clazz, Object parameters, int method)
        {
            this.clazz = clazz;
            this.parameters = parameters;
            this.method = method;
        }

        public Object run()
        {
            try
            {
                switch (method)
                {
                    case METHOD_CLASS_GETDECLAREDMETHOD:
                        String name = (String)((Object[])parameters)[0];
                        Class<?>[] realParameters = (Class<?>[])((Object[])parameters)[1];
                        return clazz.getDeclaredMethod(name, realParameters);
                    case METHOD_CLASS_GETDECLAREDMETHODS:
                        return clazz.getDeclaredMethods();
                    case METHOD_CLASS_GETDECLAREDFIELD:
                        return clazz.getDeclaredField((String)parameters);
                    case METHOD_CLASS_GETDECLAREDFIELDS:
                        return clazz.getDeclaredFields();

                    default:
                        return new WebBeansException("unknown security method: " + method);
                }
            }
            catch (Exception exception)
            {
                return exception;
            }
        }

    }

    public static Object doPrivilegedSetAccessible(AccessibleObject obj, boolean flag)
    {
        AccessController.doPrivileged(new PrivilegedActionForAccessibleObject(obj, flag));
        return null;
    }

    protected static class PrivilegedActionForAccessibleObject implements PrivilegedAction<Object>
    {

        private AccessibleObject object;

        private boolean flag;

        protected PrivilegedActionForAccessibleObject(AccessibleObject object, boolean flag)
        {
            this.object = object;
            this.flag = flag;
        }

        public Object run()
        {
            object.setAccessible(flag);
            return null;
        }
    }


    public static Class<?> doPrivilegedCreateClass(ProxyFactory factory)
    {
        Class<?> ret = (Class<?>)AccessController.doPrivileged(new PrivilegedActionForProxyFactory(factory));
        return ret;
    }
    
    public static String doPrivilegedGetSystemProperty(String propertyName, String defaultValue)
    {
        String value = AccessController.doPrivileged(new PrivilegedActionForProperty(propertyName, defaultValue));
        
        return value;
    }
    
    public static Object doPrivilegedObjectCreate(Class<?> clazz) throws PrivilegedActionException
    {
        return AccessController.doPrivileged(new PrivilegedActionForObjectCreation(clazz));
    }

    public static Properties doPrivilegedGetSystemProperties()
    {
        return AccessController.doPrivileged(SYSTEM_PROPERTY_ACTION);
    }

    protected static class PrivilegedActionForProperty implements PrivilegedAction<String>
    {
        private final String propertyName;
        
        private final String defaultValue;

        protected PrivilegedActionForProperty(String propertyName, String defaultValue)
        {
            this.propertyName = propertyName;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public String run()
        {
            return System.getProperty(this.propertyName,this.defaultValue);
        }
        
    }
    
    protected static class PrivilegedActionGetSystemProperties implements PrivilegedAction<Properties>
    {
        
        @Override
        public Properties run()
        {
            return System.getProperties();
        }
        
    }
    
    protected static class PrivilegedActionForObjectCreation implements PrivilegedExceptionAction<Object>
    {
        private Class<?> clazz;
        
        protected PrivilegedActionForObjectCreation(Class<?> clazz)
        {
            this.clazz = clazz;
        }

        @Override
        public Object run() throws Exception
        {
            try
            {
                return clazz.newInstance();
            }
            catch (InstantiationException e)
            {
                throw e;
            }
            catch (IllegalAccessException e)
            {
                throw e;
            }
        }
        
    }

    protected static class PrivilegedActionForProxyFactory implements PrivilegedAction<Object>
    {
        private ProxyFactory factory;

        protected PrivilegedActionForProxyFactory(ProxyFactory factory)
        {
            this.factory = factory;
        }

        public Object run()
        {
            return factory.createClass();
        }
    }

}
