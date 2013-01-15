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
package org.apache.webbeans.portable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.component.SelfInterceptorBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableConstructor;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethod;
import org.apache.webbeans.intercept.DefaultInterceptorHandler;
import org.apache.webbeans.intercept.LifecycleInterceptorInvocationContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.proxy.OwbInterceptorProxy;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ExceptionUtil;


import static org.apache.webbeans.intercept.InterceptorResolutionService.BeanInterceptorInfo;

public class InjectionTargetImpl<T> extends AbstractProducer<T> implements InjectionTarget<T>
{

    private AnnotatedType<T> type;
    private AnnotatedConstructor<T> constructor;
    protected final WebBeansContext webBeansContext;

    /**
     * If the InjectionTarget has a &#064;PostConstruct method, <code>null</code> if not.
     * This methods only gets used if the produced instance is not intercepted.
     * This methods must have the signature <code>void METHOD();</code>
     * They are ordered as <b>superclass first</b>.
     */
    private List<AnnotatedMethod<?>> postConstructMethods;

    /**
     * If the InjectionTarget has a &#064;PreDestroy method, <code>null</code> if not.
     * This methods only gets used if the produced instance is not intercepted.
     * This methods must have the signature <code>void METHOD();</code>
     * They are ordered as <b>subclass first</b>.
     */
    private List<AnnotatedMethod<?>> preDestroyMethods;

    /**
     * static information about Interceptors and Decorators of that bean
     */
    private BeanInterceptorInfo interceptorInfo = null;

    /**
     * Defines the interceptor/decorator stack for the InjectionTargetBean.
     * In case this is already defined, we get the ProxyClass for the Bean
     * or <code>null</code> if this Bean doesn't need any proxy.
     * This logic is handled inside the Bean and not in the BeanBuilder as
     * this can also be created lazily
     *
     * the Proxy Class or <code>null</code> if this Bean is not intercepted nor decorated.
     */
    private Class<? extends T>  proxyClass;

    /**
     * List of all Interceptors per Method.
     */
    private Map<Method, List<Interceptor<?>>> methodInterceptors = null;


    public InjectionTargetImpl(AnnotatedType<T> annotatedType, Set<InjectionPoint> points, WebBeansContext webBeansContext,
                               List<AnnotatedMethod<?>> postConstructMethods, List<AnnotatedMethod<?>> preDestroyMethods)
    {
        super(points);
        Asserts.assertNotNull(annotatedType);
        Asserts.assertNotNull(webBeansContext);
        type = annotatedType;
        this.webBeansContext = webBeansContext;
        this.postConstructMethods = postConstructMethods;
        this.preDestroyMethods = preDestroyMethods;
    }

    public void setInterceptorInfo(BeanInterceptorInfo interceptorInfo, Class<? extends T> proxyClass, Map<Method, List<Interceptor<?>>> methodInterceptors)
    {
        this.interceptorInfo = interceptorInfo;
        this.proxyClass = proxyClass;
        this.methodInterceptors = methodInterceptors;
    }

    @Override
    public T produce(CreationalContext<T> creationalContext)
    {
        T instance = new InjectableConstructor<T>(getConstructor().getJavaMember(), this, (CreationalContextImpl<T>) creationalContext).doInjection();

        if (interceptorInfo != null)
        {
            // apply interceptorInfo
            InterceptorDecoratorProxyFactory pf = webBeansContext.getInterceptorDecoratorProxyFactory();

            Map<Interceptor<?>,Object> interceptorInstances  = new HashMap<Interceptor<?>, Object>();
            for (Interceptor interceptorBean : interceptorInfo.getInterceptors())
            {
                Object interceptorInstance;
                if (interceptorBean instanceof SelfInterceptorBean)
                {
                    interceptorInstance = instance;
                }
                else
                {
                    interceptorInstance = interceptorBean.create(creationalContext);
                }
                interceptorInstances.put(interceptorBean, interceptorInstance);
            }

            InterceptorHandler interceptorHandler = new DefaultInterceptorHandler<T>(instance, methodInterceptors, interceptorInstances);

            T proxyInstance = pf.createProxyInstance(proxyClass, instance, interceptorHandler);
            instance = proxyInstance;
        }

        return instance;
    }

    @Override
    public void dispose(T instance)
    {
    }

    @Override
    public void inject(T instance, CreationalContext<T> context)
    {
        inject(instance.getClass(), instance, (CreationalContextImpl<T>) context);
    }

    private void inject(Class<?> type, T instance, CreationalContextImpl<T> context)
    {
        if (type == null || type.equals(Object.class))
        {
            return;
        }
        inject(type.getSuperclass(), instance, context);
        injectFields(type, instance, context);
        injectMethods(type, instance, context);
        injectInitializerMethods(type, instance, context);
        injectResources(instance);
    }

    private void injectFields(Class<?> type, T instance, CreationalContextImpl<T> context)
    {
        for (InjectionPoint injectionPoint : getInjectionPoints())
        {
            if (injectionPoint.getMember().getDeclaringClass().equals(type))
            {
                if (injectionPoint.getMember() instanceof Field)
                {
                    new InjectableField<T>((Field) injectionPoint.getMember(), instance, this, context).doInjection();
                }
            }
        }
    }

    private void injectMethods(Class<?> type, T instance, CreationalContextImpl<T> context)
    {
        Set<Member> injectedMethods = new HashSet<Member>();
        for (InjectionPoint injectionPoint : getInjectionPoints())
        {
            if (injectionPoint.getMember().getDeclaringClass().equals(type))
            {
                if (injectionPoint.getMember() instanceof Method
                        && !injectedMethods.contains(injectionPoint.getMember())
                        && !isProducerMethod(injectionPoint)
                        && !isDisposalMethod(injectionPoint)
                        && !isObserverMethod(injectionPoint))
                {
                    new InjectableMethod<T>((Method) injectionPoint.getMember(), instance, this, context).doInjection();
                    injectedMethods.add(injectionPoint.getMember());
                }
            }
        }
    }

    /**
     * Performs injection on initializer methods, which are methods that are annotated with &#64;Inject,
     * but have no parameter and thus no injection point.
     */
    private void injectInitializerMethods(Class<?> declaringType, T instance, CreationalContextImpl<T> context)
    {
        for (AnnotatedMethod<? super T> method : type.getMethods())
        {
            if (method.getDeclaringType().getJavaClass().equals(declaringType) && method.isAnnotationPresent(Inject.class) && method.getParameters().isEmpty())
            {
                new InjectableMethod<T>(method.getJavaMember(), instance, this, context).doInjection();
            }
        }
    }
    
    private void injectResources(T instance)
    {
        try
        {
            ResourceInjectionService service = null;
            try
            {
                service = webBeansContext.getService(ResourceInjectionService.class);
            
            }
            catch (Exception e)
            {
                // When running in tests
            }
        
            if (service != null)
            {
                service.injectJavaEEResources(instance);   
            }
        }
        catch (Exception e)
        {
            throw new WebBeansException(MessageFormat.format(
                WebBeansLoggerFacade.getTokenString(OWBLogConst.ERROR_0023), instance), e);
        }
    }

    @Override
    public void postConstruct(final T instance)
    {
        if (postConstructMethods == null)
        {
            return;
        }


        Map<Interceptor<?>, ?> interceptorInstances = null;
        List<Interceptor<?>> postConstructInterceptors = null;
        T internalInstance = instance;

        if (interceptorInfo != null && instance instanceof OwbInterceptorProxy)
        {
            InterceptorDecoratorProxyFactory pf = webBeansContext.getInterceptorDecoratorProxyFactory();
            InterceptorHandler ih = pf.getInterceptorHandler((OwbInterceptorProxy) instance);
            if (ih instanceof DefaultInterceptorHandler)
            {
                DefaultInterceptorHandler dih = (DefaultInterceptorHandler) ih;
                interceptorInstances = dih.getInstances();
                internalInstance = (T) dih.getTarget();
            }

            // we are cheating a bit right now. We could also calculate the real ones upfront
            postConstructInterceptors = new ArrayList<Interceptor<?>>(interceptorInfo.getInterceptors());
        }

        InvocationContext ic = new LifecycleInterceptorInvocationContext<T>(internalInstance, InterceptionType.POST_CONSTRUCT, postConstructInterceptors,
                                                                            interceptorInstances, postConstructMethods);
        try
        {
            ic.proceed();
        }
        catch (Exception e)
        {
            ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    @Override
    public void preDestroy(T instance)
    {
        if (preDestroyMethods == null)
        {
            return;
        }

        Map<Interceptor<?>, ?> interceptorInstances = null;
        List<Interceptor<?>> preDestroyInterceptors = null;
        T internalInstance = instance;

        if (interceptorInfo != null && instance instanceof OwbInterceptorProxy)
        {
            InterceptorDecoratorProxyFactory pf = webBeansContext.getInterceptorDecoratorProxyFactory();
            InterceptorHandler ih = pf.getInterceptorHandler((OwbInterceptorProxy) instance);
            if (ih instanceof DefaultInterceptorHandler)
            {
                DefaultInterceptorHandler dih = (DefaultInterceptorHandler) ih;
                interceptorInstances = dih.getInstances();
                internalInstance = (T) dih.getTarget();
            }

            // we are cheating a bit right now. We could also calculate the real ones upfront
            preDestroyInterceptors = new ArrayList<Interceptor<?>>(interceptorInfo.getInterceptors());
        }

        InvocationContext ic = new LifecycleInterceptorInvocationContext<T>(internalInstance, InterceptionType.PRE_DESTROY, preDestroyInterceptors,
                                                                            interceptorInstances, preDestroyMethods);
        try
        {
            ic.proceed();
        }
        catch (Exception e)
        {
            ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    private AnnotatedConstructor<T> getConstructor()
    {
        if (constructor != null)
        {
            return constructor;
        }
        AnnotatedConstructor<T> constructor = null;
        for (InjectionPoint injectionPoint : getInjectionPoints())
        {
            if (injectionPoint.getMember() instanceof Constructor)
            {
                if (constructor == null)
                {
                    constructor = (AnnotatedConstructor<T>)((AnnotatedParameter<T>)injectionPoint.getAnnotated()).getDeclaringCallable();
                }
                else if (!constructor.equals(injectionPoint.getAnnotated()))
                {
                    throw new IllegalArgumentException("More than one constructor found for injection: "
                                                       + constructor.getJavaMember() + " and " + ((AnnotatedMember)injectionPoint.getAnnotated()).getJavaMember());
                }
            }
        }
        if (constructor != null)
        {
            this.constructor = constructor;
        }
        else
        {
            this.constructor = new AnnotatedConstructorImpl<T>(webBeansContext, getDefaultConstructor(), type);
        }
        return this.constructor;
    }

    private Constructor<T> getDefaultConstructor()
    {
        return webBeansContext.getWebBeansUtil().getNoArgConstructor(type.getJavaClass());
    }
    
    private boolean isProducerMethod(InjectionPoint injectionPoint)
    {
        return ((AnnotatedElement)injectionPoint.getMember()).isAnnotationPresent(Produces.class);
    }

    private boolean isObserverMethod(InjectionPoint injectionPoint)
    {
        if (!(injectionPoint.getMember() instanceof Method))
        {
            return false;
        }
        Method method = (Method) injectionPoint.getMember();
        for (Annotation[] annotations : method.getParameterAnnotations())
        {
            for (Annotation annotation : annotations)
            {
                if (annotation.annotationType().equals(Observes.class))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isDisposalMethod(InjectionPoint injectionPoint)
    {
        if (!(injectionPoint.getMember() instanceof Method))
        {
            return false;
        }
        Method method = (Method) injectionPoint.getMember();
        for (Annotation[] annotations : method.getParameterAnnotations())
        {
            for (Annotation annotation : annotations)
            {
                if (annotation.annotationType().equals(Disposes.class))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
