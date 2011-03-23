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

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javassist.util.proxy.ProxyFactory;
import org.apache.webbeans.exception.WebBeansException;

/** @deprecated  use SecurityService instead */
public class SecurityUtil
{

    private static final int METHOD_CLASS_GETDECLAREDMETHODS = 0x04;

    public static <T> Method[] doPrivilegedGetDeclaredMethods(Class<T> clazz)
    {
        Object obj = AccessController.doPrivileged(
                new PrivilegedActionForClass(clazz, null, METHOD_CLASS_GETDECLAREDMETHODS));
        return (Method[])obj;
    }

    protected static class PrivilegedActionForClass implements PrivilegedAction<Object>
    {
        private Class<?> clazz;

        private int method;

        protected PrivilegedActionForClass(Class<?> clazz, Object parameters, int method)
        {
            this.clazz = clazz;
            this.method = method;
        }

        public Object run()
        {
            try
            {
                switch (method)
                {
                    case METHOD_CLASS_GETDECLAREDMETHODS:
                        return clazz.getDeclaredMethods();

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

    public static Class<?> doPrivilegedCreateClass(ProxyFactory factory)
    {
        Class<?> ret = (Class<?>)AccessController.doPrivileged(new PrivilegedActionForProxyFactory(factory));
        return ret;
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
