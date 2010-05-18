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
package org.apache.webbeans.decorator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.interceptor.InvocationContext;

import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.SecurityUtil;

import javassist.util.proxy.MethodHandler;

public class DelegateHandler implements MethodHandler
{
    private transient static WebBeansLogger logger = WebBeansLogger.getLogger(DelegateHandler.class);

    private transient List<Object> decorators;
    private transient int position = 0;

    private transient Object actualBean = null;
    
    private transient OwbBean<?> bean = null;
    
    private transient InvocationContext ejbContext;

    public DelegateHandler(OwbBean<?> bean)
    {
        this.bean = bean;
    }
    
    public DelegateHandler(OwbBean<?> bean, InvocationContext ejbContext)
    {
        this.bean = bean;
        this.ejbContext = ejbContext;
    }    
    
    @Override
    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        // Tuck away a reference to the bean being Decorated
        if (actualBean == null)
        {
            actualBean = instance;
        }

        while (position < decorators.size())
        {

            Object decorator = decorators.get(position++);

            try
            {
                Method decMethod = decorator.getClass().getMethod(method.getName(), method.getParameterTypes());
                boolean methodInInterface = checkForMethodInInterfaces(decorator.getClass(), method);

                if (decMethod != null && methodInInterface)
                {
                    if (!decMethod.isAccessible())
                    {
                        SecurityUtil.doPrivilegedSetAccessible(decMethod, true);
                    }

                    Object returnValue = decMethod.invoke(decorator, arguments);
                    position--;
                    return returnValue;
                }

            }
            catch (SecurityException e)
            {
                logger.error(OWBLogConst.ERROR_0011, method.getName(), decorator.getClass().getName());
                throw new WebBeansException(e);

            }
            catch (NoSuchMethodException e)
            {
                continue;
            }
            catch (InvocationTargetException e)
            {
                logger.error(OWBLogConst.ERROR_0012, e.getTargetException(), method.getName(), decorator.getClass().getName());

                throw new WebBeansException(e);
            }
            catch (IllegalAccessException e)
            {
                logger.error(OWBLogConst.ERROR_0014, method.getName(), decorator.getClass().getName());
                throw new WebBeansException(e);
            }

        }

        if (!method.isAccessible())
        {
            SecurityUtil.doPrivilegedSetAccessible(method, true);
        }

        Object result = null;
        
        if(!(bean instanceof EnterpriseBeanMarker))
        {
            result = method.invoke(actualBean, arguments);
        }
        else
        {
            if(ejbContext != null)
            {
                result = ejbContext.proceed();
            }
        }
        
        return result;

    }

    /**
     * Helper method to locate method in any of the interfaces of the Decorator.
     * 
     * @param Class whose interfaces we want to check
     * @param Method to check for in Interfaces
     * @return True if the method exists in any of the interfaces of the
     *         Decorator
     */
    private boolean checkForMethodInInterfaces(Class<? extends Object> class1, Method m)
    {
        Class<?>[] interfaces = class1.getInterfaces();
        for (int i = 0; i < interfaces.length; i++)
        {
            try
            {
                if (interfaces[i].getMethod(m.getName(), m.getParameterTypes()) != null)
                {
                    return true;
                }
            }
            catch (NoSuchMethodException exception)
            {
                // no-op, we didn't find the method
            }
        }
        //
        return false;
    }

    public void setDecorators(List<Object> dec)
    {
        this.decorators = dec;
    }

}
