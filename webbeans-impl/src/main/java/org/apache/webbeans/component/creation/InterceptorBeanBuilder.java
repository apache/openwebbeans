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
package org.apache.webbeans.component.creation;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.AroundInvoke;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.webbeans.component.InterceptorBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;


/**
 * Bean builder for {@link org.apache.webbeans.component.InterceptorBean}s.
 */
public abstract class InterceptorBeanBuilder<T> extends AbstractInjectionTargetBeanBuilder<T>
{
    private final InterceptorBean<T> bean;

    protected InterceptorBeanBuilder(InterceptorBean<T> bean)
    {
        super(bean, Dependent.class);
        this.bean = bean;
    }

    /**
     * If this method returns <code>false</code> the {@link #getBean()} method must not get called.
     *
     * @return <code>true</code> if the Interceptor is enabled and a Bean should get created
     */
    public abstract boolean isInterceptorEnabled();

    protected void checkInterceptorConditions()
    {
        Set<AnnotatedMethod<? super T>> methods = getAnnotated().getMethods();
        for(AnnotatedMethod method : methods)
        {
            List<AnnotatedParameter> parms = method.getParameters();
            for (AnnotatedParameter parameter : parms)
            {
                if (parameter.isAnnotationPresent(Produces.class))
                {
                    throw new WebBeansConfigurationException("Interceptor class : " + getBeanType()
                            + " can not have producer methods but it has one with name : "
                            + method.getJavaMember().getName());
                }
            }
        }
    }

    protected void defineInterceptorRules()
    {
        defineInterceptorMethods();
        defineInjectedMethods();
        defineInjectedFields();
    }

    /**
     * <p>Grab all methods which act as interceptors for the various
     * {@link javax.enterprise.inject.spi.InterceptionType}s.</p>
     *
     * <p>This method will also check some rules, e.g. that there must not be
     * more than a single {@link javax.interceptor.AroundInvoke} method
     * on a class.</p>
     *
     * <p>For the interceptors where multiple are allowed, the following rules apply:
     * <ul>
     *     <li>Superclass methods first</li>
     *     <li>Non-private methods override and derogates their superclass counterparts.</li>
     *     <li>Private methods with the same signature stack (superclass first).</li>
     *     <li>There must only be a single method for each InterceptorType in the same class.</li>
     * </ul>
     * </p>
     */
    protected void defineInterceptorMethods()
    {
        List<Class> classHierarchy = getReverseClassHierarchy();

        AnnotatedMethod aroundInvokeMethod = null;
        List<AnnotatedMethod> postConstructMethods = new ArrayList<AnnotatedMethod>();
        List<AnnotatedMethod> preDestroyMethods = new ArrayList<AnnotatedMethod>();

        Set<AnnotatedMethod<? super T>> methods = getAnnotated().getMethods();

        for (Class clazz : classHierarchy)
        {

            for (AnnotatedMethod m : methods)
            {
                if (clazz == m.getDeclaringType().getJavaClass())
                {

                    // we only take methods from this very class and not sub- or superclasses
                    if (m.getAnnotation(AroundInvoke.class) != null)
                    {
                        if (aroundInvokeMethod != null)
                        {
                            throw new WebBeansConfigurationException("only one AroundInvoke allowed per Interceptor");
                        }
                        aroundInvokeMethod = m;
                    }

                    // PostConstruct
                    if (m.getAnnotation(PostConstruct.class) != null)
                    {
                        postConstructMethods.add(m); // add at last position
                    }
                    checkMethodOverrides(postConstructMethods, clazz, m);

                    // PreDestroy
                    if (m.getAnnotation(PreDestroy.class) != null)
                    {
                        preDestroyMethods.add(m); // add at last position
                    }
                    checkMethodOverrides(preDestroyMethods, clazz, m);

                }
            }
        }

        // and now for setting the bean info

        Set<InterceptionType> intercepts = new HashSet<InterceptionType>();

        if (aroundInvokeMethod != null)
        {
            bean.setAroundInvokeMethods(new Method[]{aroundInvokeMethod.getJavaMember()});
            intercepts.add(InterceptionType.AROUND_INVOKE);
        }


        bean.setIntercepts(intercepts);
    }

    /**
     * Check if the given annotatedMethod overrides some previously defined interceptorMethods
     * and remove them if non-private.
     * This will also detect if there are multiple methods for the same InterceptionType
     * in the same class
     */
    private void checkMethodOverrides(List<AnnotatedMethod> alreadyDefinedMethods, Class clazz, AnnotatedMethod annotatedMethod)
    {
        String methodName = null;
        Class<?>[] methodParameterTypes = null;

        Iterator<AnnotatedMethod> it = alreadyDefinedMethods.iterator();
        while (it.hasNext())
        {
            AnnotatedMethod alreadyDefined = it.next();

            if (methodName == null)
            {
                methodName = annotatedMethod.getJavaMember().getName();
                methodParameterTypes = annotatedMethod.getJavaMember().getParameterTypes();
            }

            // check for same class -> Exception
            if (alreadyDefined.getDeclaringType().getJavaClass() ==  clazz)
            {
                throw new WebBeansConfigurationException("Only one Interceptor of a certain type is allowed per class, but multiple found in class "
                                                         + annotatedMethod.getDeclaringType().getJavaClass().getName()
                                                         + " methods: " + annotatedMethod.getJavaMember().toString()
                                                         + " and " + alreadyDefined.getJavaMember().toString());
            }

            // check method overrides
            if (!Modifier.isPrivate(alreadyDefined.getJavaMember().getModifiers()))
            {
                // we only scan non-private methods, as private methods cannot get overridden.
                if (methodName.equals(alreadyDefined.getJavaMember().getName()) &&
                    methodParameterTypes.length == alreadyDefined.getJavaMember().getParameterTypes().length)
                {
                    boolean overridden = true;
                    // same name and param length so we need to check if all the paramTypes are equal.
                    if (methodParameterTypes.length > 0)
                    {
                        Class<?>[] otherParamTypes = alreadyDefined.getJavaMember().getParameterTypes();

                        for (int i = 0; i < otherParamTypes.length; i++)
                        {
                            if (!otherParamTypes[i].equals(methodParameterTypes[i]))
                            {
                                overridden = false;
                                break;
                            }
                        }
                    }

                    if (overridden)
                    {
                        // then we need to remove this method
                        it.remove();
                        continue;
                    }
                }
            }
        }

    }


}
