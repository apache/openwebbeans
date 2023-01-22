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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.component.InterceptorBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.plugins.OpenWebBeansEjbLCAPlugin;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;


/**
 * Bean builder for {@link org.apache.webbeans.component.InterceptorBean}s.
 */
public abstract class InterceptorBeanBuilder<T, B extends InterceptorBean<T>> extends AbstractBeanBuilder
{
    protected final WebBeansContext webBeansContext;
    protected final AnnotatedType<T> annotatedType;
    protected final BeanAttributes<T> beanAttributes;

    private final OpenWebBeansEjbLCAPlugin ejbPlugin;
    private final Class<? extends Annotation> prePassivateClass;
    private final Class<? extends Annotation> postActivateClass;

    protected Map<InterceptionType, Method[]> interceptionMethods;

    protected InterceptorBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType, BeanAttributes<T> beanAttributes)
    {
        Asserts.assertNotNull(webBeansContext, Asserts.PARAM_NAME_WEBBEANSCONTEXT);
        Asserts.assertNotNull(annotatedType, "annotated type");
        Asserts.assertNotNull(beanAttributes, "beanAttributes");
        this.webBeansContext = webBeansContext;
        this.annotatedType = annotatedType;
        this.beanAttributes = beanAttributes;
        ejbPlugin = webBeansContext.getPluginLoader().getEjbLCAPlugin();
        if (ejbPlugin != null)
        {
            prePassivateClass = ejbPlugin.getPrePassivateClass();
            postActivateClass = ejbPlugin.getPostActivateClass();
        }
        else
        {
            prePassivateClass = null;
            postActivateClass = null;
        }
    }

    /**
     * If this method returns <code>false</code> the {@link #getBean()} method must not get called.
     *
     * @return <code>true</code> if the Interceptor is enabled and a Bean should get created
     */
    public abstract boolean isInterceptorEnabled();

    protected void checkInterceptorConditions()
    {
        Set<AnnotatedMethod<? super T>> methods = webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(annotatedType);
        for(AnnotatedMethod<?> method : methods)
        {
            for (AnnotatedParameter<?> parameter : method.getParameters())
            {
                if (parameter.isAnnotationPresent(Produces.class))
                {
                    throw new WebBeansConfigurationException("Interceptor class : " + annotatedType.getJavaClass()
                            + " can not have producer methods but it has one with name : "
                            + method.getJavaMember().getName());
                }
            }
        }
    }

    /**
     * <p>Grab all methods which act as interceptors for the various
     * {@link jakarta.enterprise.inject.spi.InterceptionType}s.</p>
     *
     * <p>This method will also check some rules, e.g. that there must not be
     * more than a single {@link jakarta.interceptor.AroundInvoke} method
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
     * @return <code>true</code> if we found some interceptor methods
     */
    public boolean defineInterceptorMethods()
    {
        List<Class> classHierarchy = webBeansContext.getInterceptorUtil().getReverseClassHierarchy(annotatedType.getJavaClass());

        Collection<Method> aroundInvokeMethod = null;
        List<AnnotatedMethod> postConstructMethods = new ArrayList<>();
        List<AnnotatedMethod> preDestroyMethods = new ArrayList<>();
        List<AnnotatedMethod> aroundTimeoutMethods = new ArrayList<>();
        List<AnnotatedMethod> aroundConstructMethods = new ArrayList<>();

        // EJB related interceptors
        List<AnnotatedMethod> prePassivateMethods = new ArrayList<>();
        List<AnnotatedMethod> postActivateMethods = new ArrayList<>();

        boolean interceptorFound = false;

        Set<AnnotatedMethod<? super T>> methods = webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(annotatedType);

        for (Class clazz : classHierarchy)
        {

            for (AnnotatedMethod m : methods)
            {
                int arouncConstructCount = 0;
                if (clazz == m.getJavaMember().getDeclaringClass())
                {
                    if (m.getAnnotation(AroundConstruct.class) != null)
                    {
                        if (arouncConstructCount > 0)
                        {
                            throw new WebBeansConfigurationException("only one AroundConstruct allowed per Interceptor");
                        }
                        arouncConstructCount++;
                        aroundConstructMethods.add(m);
                    }

                    // we only take methods from this very class and not sub- or superclasses
                    if (m.getAnnotation(AroundInvoke.class) != null)
                    {
                        if (aroundInvokeMethod != null)
                        {
                            for (Method ai : aroundInvokeMethod)
                            {
                                if (ai.getDeclaringClass() == m.getJavaMember().getDeclaringClass())
                                {
                                    throw new WebBeansConfigurationException("only one AroundInvoke allowed per Interceptor");
                                }
                            }
                        }
                        checkAroundInvokeConditions(m);
                        if (aroundInvokeMethod == null)
                        {
                            aroundInvokeMethod = new ArrayList<>();
                        }
                        aroundInvokeMethod.add(m.getJavaMember());
                    }

                    // PostConstruct
                    if (m.getAnnotation(PostConstruct.class) != null)
                    {
                        checkSameClassInterceptors(postConstructMethods, m);
                        postConstructMethods.add(m); // add at last position
                    }

                    // PreDestroy
                    if (m.getAnnotation(PreDestroy.class) != null)
                    {
                        checkSameClassInterceptors(preDestroyMethods, m);
                        preDestroyMethods.add(m); // add at last position
                    }

                    // AroundTimeout
                    if (m.getAnnotation(AroundTimeout.class) != null)
                    {
                        checkSameClassInterceptors(aroundTimeoutMethods, m);
                        aroundTimeoutMethods.add(m); // add at last position
                    }

                    // and now the EJB related interceptors
                    if (ejbPlugin != null)
                    {
                        if (m.getAnnotation(prePassivateClass) != null)
                        {
                            checkSameClassInterceptors(prePassivateMethods, m);
                            prePassivateMethods.add(m); // add at last position
                        }

                        // AroundTimeout
                        if (m.getAnnotation(AroundTimeout.class) != null)
                        {
                            checkSameClassInterceptors(aroundTimeoutMethods, m);
                            postActivateMethods.add(m); // add at last position
                        }

                        // AroundTimeout
                        if (m.getAnnotation(postActivateClass) != null)
                        {
                            checkSameClassInterceptors(postActivateMethods, m);
                            postActivateMethods.add(m); // add at last position
                        }
                    }
                }
            }
        }

        // and now for setting the bean info

        interceptionMethods = new HashMap<>();

        if (aroundInvokeMethod != null)
        {
            interceptorFound = true;
            interceptionMethods.put(InterceptionType.AROUND_INVOKE, aroundInvokeMethod.toArray(new Method[aroundInvokeMethod.size()]));
        }

        if (postConstructMethods.size() > 0)
        {
            interceptorFound = true;
            interceptionMethods.put(InterceptionType.POST_CONSTRUCT, getMethodArray(postConstructMethods));
        }
        if (preDestroyMethods.size() > 0)
        {
            interceptorFound = true;
            interceptionMethods.put(InterceptionType.PRE_DESTROY, getMethodArray(preDestroyMethods));
        }
        if (aroundTimeoutMethods.size() > 0)
        {
            interceptorFound = true;
            interceptionMethods.put(InterceptionType.AROUND_TIMEOUT, getMethodArray(aroundTimeoutMethods));
        }

        if (prePassivateMethods.size() > 0)
        {
            interceptorFound = true;
            interceptionMethods.put(InterceptionType.PRE_PASSIVATE, getMethodArray(prePassivateMethods));
        }
        if (postActivateMethods.size() > 0)
        {
            interceptorFound = true;
            interceptionMethods.put(InterceptionType.POST_ACTIVATE, getMethodArray(postActivateMethods));
        }
        if (aroundConstructMethods.size() > 0)
        {
            interceptorFound = true;
            interceptionMethods.put(InterceptionType.AROUND_CONSTRUCT, getMethodArray(aroundConstructMethods));
        }

        return interceptorFound;
    }

    private void checkAroundInvokeConditions(AnnotatedMethod method)
    {
        List<AnnotatedParameter<T>> parameters = method.getParameters();
        List<Class<?>> clazzParameters = new ArrayList<>();
        for(AnnotatedParameter<T> parameter : parameters)
        {
            clazzParameters.add(ClassUtil.getClazz(parameter.getBaseType()));
        }

        Class<?>[] params = clazzParameters.toArray(new Class<?>[clazzParameters.size()]);

        if (params.length != 1 || !params[0].equals(InvocationContext.class))
        {
            throw new WebBeansConfigurationException("@AroundInvoke annotated method : "
                    + method.getJavaMember().getName() + " in class : " + annotatedType.getJavaClass().getName()
                    + " can not take any formal arguments other than InvocationContext");
        }

        if (!method.getJavaMember().getReturnType().equals(Object.class))
        {
            throw new WebBeansConfigurationException("@AroundInvoke annotated method : "
                    + method.getJavaMember().getName()+ " in class : " + annotatedType.getJavaClass().getName()
                    + " must return Object type");
        }

        if (Modifier.isStatic(method.getJavaMember().getModifiers()) ||
                Modifier.isFinal(method.getJavaMember().getModifiers()))
        {
            throw new WebBeansConfigurationException("@AroundInvoke annotated method : "
                    + method.getJavaMember().getName( )+ " in class : " + annotatedType.getJavaClass().getName()
                    + " can not be static or final");
        }
    }

    /**
     * @return the a Method array with the native members of the AnnotatedMethod list
     */
    private Method[] getMethodArray(List<AnnotatedMethod> methodList)
    {
        Method[] methods = new Method[methodList.size()];
        int i=0;
        for (AnnotatedMethod am : methodList)
        {
            methods[i++] = am.getJavaMember();
        }
        return methods;
    }

    private void checkSameClassInterceptors(List<AnnotatedMethod> alreadyDefinedMethods, AnnotatedMethod annotatedMethod)
    {
        Class clazz = null;
        for (AnnotatedMethod alreadyDefined : alreadyDefinedMethods)
        {
            if (clazz == null)
            {
                clazz = annotatedMethod.getJavaMember().getDeclaringClass();
            }

            // check for same class -> Exception
            if (alreadyDefined.getJavaMember().getDeclaringClass() == clazz)
            {
                throw new WebBeansConfigurationException("Only one Interceptor of a certain type is allowed per class, but multiple found in class "
                        + annotatedMethod.getJavaMember().getDeclaringClass().getName()
                        + " methods: " + annotatedMethod.getJavaMember().toString()
                        + " and " + alreadyDefined.getJavaMember().toString());
            }
        }
    }

    protected abstract B createBean(Class<T> beanClass,
                                    boolean enabled,
                                    Map<InterceptionType, Method[]> interceptionMethods);

    public B getBean()
    {
        return createBean(annotatedType.getJavaClass(), isInterceptorEnabled(), interceptionMethods);
    }
}
