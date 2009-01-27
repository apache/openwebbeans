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
package org.apache.webbeans.intercept;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.context.Context;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.Interceptors;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

import javassist.util.proxy.MethodHandler;

public class InterceptorHandler implements MethodHandler
{
    private static WebBeansLogger logger = WebBeansLogger.getLogger(InterceptorHandler.class);

    private AbstractComponent<?> component = null;

    private Method calledMethod = null;

    private boolean isSameDecMethod = false;

    public InterceptorHandler(AbstractComponent<?> component)
    {
        this.component = component;
    }

    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        Context webbeansContext = ManagerImpl.getManager().getContext(component.getScopeType());
        Object webbeansInstance = webbeansContext.get(this.component, new CreationalContextImpl());

        if (!ClassUtil.isObjectMethod(method.getName()) && InterceptorUtil.isWebBeansBusinessMethod(method))
        {
            if (this.calledMethod == null)
            {
                this.calledMethod = method;
            }
            else if (this.calledMethod.equals(method))
            {
                this.isSameDecMethod = true;
            }
            else
            {
                this.calledMethod = method;
                this.isSameDecMethod = false;
            }

            // Run around invoke chain
            List<InterceptorData> stack = component.getInterceptorStack();

            Collections.sort(stack, new InterceptorDataComparator());

            filterEJBInterceptorStackList(stack, method);
            filterWebBeansInterceptorStackList(stack, method);

            if (WebBeansUtil.isContainsInterceptorMethod(stack, InterceptorType.AROUND_INVOKE))
            {
                callAroundInvokes(instance, proceed, arguments, WebBeansUtil.getInterceptorMethods(stack, InterceptorType.AROUND_INVOKE));
            }

            List<Object> decorators = component.getDecoratorStack();
            callDecorators(decorators, method, arguments);

        }

        if (!method.isAccessible())
        {
            method.setAccessible(true);
        }

        return method.invoke(webbeansInstance, arguments);
    }

    private void callDecorators(List<Object> decorators, Method method, Object[] arguments)
    {

        Iterator<Object> itDec = decorators.iterator();
        while (itDec.hasNext())
        {
            Object decorator = itDec.next();
            try
            {
                Method decMethod = decorator.getClass().getMethod(method.getName(), method.getParameterTypes());

                if (decMethod != null)
                {
                    if (!this.isSameDecMethod)
                    {
                        if (!decMethod.isAccessible())
                        {
                            decMethod.setAccessible(true);
                        }

                        decMethod.invoke(decorator, arguments);
                    }
                }

            }
            catch (SecurityException e)
            {
                logger.error("Method security access violation for method " + method.getName() + " for  decorator class : " + decorator.getClass().getName());
                throw new WebBeansException(e);

            }
            catch (NoSuchMethodException e)
            {
                continue;
            }
            catch (InvocationTargetException e)
            {
                logger.error("Exception in calling method " + method.getName() + " for  decorator class : " + decorator.getClass().getName() + ". Look log for target checked exception.", e.getTargetException());
                throw new WebBeansException(e);
            }
            catch (IllegalAccessException e)
            {
                logger.error("Method illegal access for method " + method.getName() + " for  decorator class : " + decorator.getClass().getName());
                throw new WebBeansException(e);
            }

        }
    }

    private <T> void callAroundInvokes(Object instance, Method proceed, Object[] arguments, List<InterceptorData> stack) throws Exception
    {
        InvocationContextImpl impl = new InvocationContextImpl(instance, proceed, arguments, stack, InterceptorType.AROUND_INVOKE);

        impl.proceed();

    }

    private void filterEJBInterceptorStackList(List<InterceptorData> stack, Method method)
    {
        boolean isMethodAnnotatedWithInterceptorClass = false;
        boolean isMethodAnnotatedWithExcludeInterceptorClass = false;

        if (AnnotationUtil.isMethodHasAnnotation(method, Interceptors.class))
            isMethodAnnotatedWithInterceptorClass = true;

        if (AnnotationUtil.isMethodHasAnnotation(method, ExcludeClassInterceptors.class))
            isMethodAnnotatedWithExcludeInterceptorClass = true;

        Iterator<InterceptorData> it = stack.iterator();
        while (it.hasNext())
        {
            InterceptorData data = it.next();
            if (isMethodAnnotatedWithInterceptorClass)
            {
                if (isMethodAnnotatedWithExcludeInterceptorClass)
                {
                    if (!data.isDefinedInMethod() && data.isDefinedInInterceptorClass())
                    {
                        it.remove();
                    }
                    else if (data.isDefinedInMethod())
                    {
                        if (!data.getAnnotatedMethod().equals(method))
                        {
                            it.remove();
                        }
                    }
                }
                else
                {
                    if (data.isDefinedInMethod())
                    {
                        if (!data.getAnnotatedMethod().equals(method))
                        {
                            it.remove();
                        }
                    }
                }
            }
            else
            {
                if (data.isDefinedInMethod())
                {
                    it.remove();
                }
            }

        }

    }

    private void filterWebBeansInterceptorStackList(List<InterceptorData> stack, Method method)
    {
        boolean isMethodAnnotatedWithInterceptorClass = false;
        boolean isMethodAnnotatedWithExcludeInterceptorClass = false;

        if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(method.getDeclaredAnnotations()))
        {
            isMethodAnnotatedWithInterceptorClass = true;
        }

        if (AnnotationUtil.isMethodHasAnnotation(method, ExcludeClassInterceptors.class))
        {
            isMethodAnnotatedWithExcludeInterceptorClass = true;
        }

        Iterator<InterceptorData> it = stack.iterator();
        while (it.hasNext())
        {
            InterceptorData data = it.next();
            if (isMethodAnnotatedWithInterceptorClass)
            {
                if (isMethodAnnotatedWithExcludeInterceptorClass)
                {
                    if (!data.isDefinedInMethod() && data.isDefinedInInterceptorClass() && data.isDefinedWithWebBeansInterceptor())
                    {
                        it.remove();
                    }
                    else if (data.isDefinedInMethod() && data.isDefinedWithWebBeansInterceptor())
                    {
                        if (!data.getAnnotatedMethod().equals(method))
                        {
                            it.remove();
                        }
                    }
                }
                else
                {
                    if (data.isDefinedInMethod() && data.isDefinedWithWebBeansInterceptor())
                    {
                        if (!data.getAnnotatedMethod().equals(method))
                        {
                            it.remove();
                        }
                    }
                }
            }
            else
            {
                if (data.isDefinedInMethod() && data.isDefinedWithWebBeansInterceptor())
                {
                    it.remove();
                }
            }

        }

    }

}
