/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.ejb.common.interceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.decorator.DelegateHandler;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.decorator.WebBeansDecoratorInterceptor;
import org.apache.webbeans.ejb.common.component.BaseEjbBean;
import org.apache.webbeans.ejb.common.util.EjbUtility;
import org.apache.webbeans.inject.OWBInjector;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.intercept.InterceptorDataImpl;
import org.apache.webbeans.intercept.InterceptorType;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.InvocationContextImpl;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.SecurityUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * EJB interceptor that is responsible
 * for injection dependent instances, and call
 * OWB based interceptors and decorators.
 * 
 * @version $Rev$ $Date$
 *
 */
public class OpenWebBeansEjbInterceptor
{
    //Logger instance
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(OpenWebBeansEjbInterceptor.class);
    
    /**Thread local for calling bean*/
    private static transient ThreadLocal<BaseEjbBean<?>> threadLocal = new ThreadLocal<BaseEjbBean<?>>();
    
    /**Thread local for calling creational context*/
    private static transient ThreadLocal<CreationalContext<?>> threadLocalCreationalContext = new ThreadLocal<CreationalContext<?>>();
    
    /**Intercepted methods*/
    protected transient Map<Method, List<InterceptorData>> interceptedMethodMap = new WeakHashMap<Method, List<InterceptorData>>();

    /**Non contextual Intercepted methods*/
    protected transient Map<Method, List<InterceptorData>> nonCtxInterceptedMethodMap = new WeakHashMap<Method, List<InterceptorData>>();

    /**Bean decorator objects*/
    protected transient List<Object> decorators = null;
    
    /**Delegate handler*/
    protected transient DelegateHandler delegateHandler;
    
    /**Injector*/
    private transient OWBInjector injector;
    
    /**Resolved ejb beans for non-contexctual interceptors*/
    private transient Map<Class<?>, BaseEjbBean<?>> resolvedBeans = new HashMap<Class<?>, BaseEjbBean<?>>();
    
    /**
     * Creates a new instance.
     */
    public OpenWebBeansEjbInterceptor()
    {
        
    }
    
    /**
     * Sets thread local.
     * @param ejbBean bean
     * @param creationalContext context
     */
    public static void setThreadLocal(BaseEjbBean<?> ejbBean, CreationalContext<?> creationalContext)
    {
        threadLocal.set(ejbBean);
        threadLocalCreationalContext.set(creationalContext);
    }
    
    /**
     * Remove locals.
     */
    public static void unsetThreadLocal()
    {
        threadLocal.remove();
        threadLocalCreationalContext.remove();
    }
    
    /**
     * Called for every business methods.
     * @param context invocation context
     * @return instance
     * @throws Exception
     */
    @AroundInvoke
    public Object callToOwbInterceptors(InvocationContext context) throws Exception
    {
        boolean requestCreated = false;
        boolean applicationCreated = false;
        boolean requestAlreadyActive = false;
        boolean applicationAlreadyActive = false;
        try
        {
            int result = 1000;
            //Context activities
            if( (result = activateContexts(RequestScoped.class)) == 1)
            {
                requestCreated = true;
            }
            else if(result == -1)
            {
                requestAlreadyActive = true;
            }
            
            if((result = activateContexts(ApplicationScoped.class)) == 1)
            {
                applicationCreated = true;
            }
            else if(result == -1)
            {
                applicationAlreadyActive = true;
            }
            
            if(threadLocal.get() != null)
            {
                //Calls OWB interceptors and decorators
                callInterceptorsAndDecorators(context.getMethod(), context.getTarget(), context.getParameters());    
            }
            else
            {
                //Call OWB interceptors
                callInterceptorsForNonContextuals(context.getMethod(), context.getTarget(), context.getParameters());
            }
            
        }finally
        {
            if(!requestAlreadyActive)
            {
                deActivateContexts(requestCreated, RequestScoped.class);   
            }
            if(!applicationAlreadyActive)
            {
                deActivateContexts(applicationCreated, ApplicationScoped.class);   
            }
        }
        
        return context.proceed();
    }
    
    /**
     * Post construct.
     * @param context invocation ctx
     */
    @PostConstruct
    public void afterConstruct(InvocationContext context)
    {
        InjectionTargetBean<?> injectionTarget = (InjectionTargetBean<?>) threadLocal.get();
        
        if(injectionTarget != null)
        {
            if (WebBeansUtil.isContainsInterceptorMethod(injectionTarget.getInterceptorStack(), InterceptorType.POST_CONSTRUCT))
            {                
                InvocationContextImpl impl = new InvocationContextImpl(null, context.getTarget(), null, null, 
                        InterceptorUtil.getInterceptorMethods(injectionTarget.getInterceptorStack(), InterceptorType.POST_CONSTRUCT), InterceptorType.POST_CONSTRUCT);
                impl.setCreationalContext(threadLocalCreationalContext.get());
                try
                {
                    impl.proceed();
                }
                catch (Exception e)
                {
                    logger.error(OWBLogConst.ERROR_0008, new Object[]{"@PostConstruct."}, e);                
                }
            }                        
        }
        
        Object instance = context.getTarget();
        this.injector = new OWBInjector();
        try
        {
            this.injector.inject(instance, threadLocalCreationalContext.get());
        }
        catch (Exception e)
        {
            logger.error("Error is occured while injecting dependencies of bean : " + threadLocal.get(),e);
        }
        
    }
    
    /**
     * Pre destroy.
     * @param context invocation context
     */
    @PreDestroy
    public void preDestroy(InvocationContext context)
    {
        InjectionTargetBean<?> injectionTarget = (InjectionTargetBean<?>) threadLocal.get();
        
        if(injectionTarget != null)
        {
            if (WebBeansUtil.isContainsInterceptorMethod(injectionTarget.getInterceptorStack(), InterceptorType.PRE_DESTROY))
            {                
                InvocationContextImpl impl = new InvocationContextImpl(null, context.getTarget(), null, null, 
                        InterceptorUtil.getInterceptorMethods(injectionTarget.getInterceptorStack(), InterceptorType.PRE_DESTROY), InterceptorType.PRE_DESTROY);
                impl.setCreationalContext(threadLocalCreationalContext.get());
                try
                {
                    impl.proceed();
                }
                catch (Exception e)
                {
                    logger.error(OWBLogConst.ERROR_0008, new Object[]{"@PreDestroy."}, e);
                }
            }                        
        }
        
        if(this.injector != null)
        {
            this.injector.destroy();
            this.interceptedMethodMap.clear();
            if(decorators != null)
            {
                decorators.clear();
            }
            this.resolvedBeans.clear();
            this.nonCtxInterceptedMethodMap.clear();
        }
    }
    
    /**
     * Activate given context.
     * @param scopeType scope type
     * @return true if also creates context.
     */
    private int activateContexts(Class<? extends Annotation> scopeType)
    {
        ContextsService service = ServiceLoader.getService(ContextsService.class);
        Context ctx = service.getCurrentContext(scopeType);
        
        if(scopeType == RequestScoped.class)
        {
            if(ctx != null && !ctx.isActive())
            {
                ContextFactory.activateContext(scopeType);
                return 0;
            }
            else if(ctx == null)
            {
                ContextFactory.initRequestContext(null);
                return 1;
            }
            
        }
        
        ctx = service.getCurrentContext(scopeType);
        if(ctx != null && !ctx.isActive())
        {
            ContextFactory.activateContext(scopeType);
            return 0;
        }
        else if(ctx == null)
        {
            ContextFactory.initApplicationContext(null);
            return 1;

        }     
        
        return -1;
    }
    
    /**
     * Deacitvate context.
     * @param destroy if destroy context
     * @param scopeType scope type
     */
    private void deActivateContexts(boolean destroy, Class<? extends Annotation> scopeType)
    {
        if(scopeType == ApplicationScoped.class)
        {
            if(destroy)
            {
                ContextFactory.destroyApplicationContext(null);
            }
            else
            {
                ContextFactory.deActivateContext(ApplicationScoped.class);
            }            
        }
        else
        {
            if(destroy)
            {
                ContextFactory.destroyRequestContext(null);
            }
            else
            {
                ContextFactory.deActivateContext(RequestScoped.class);
            }            
        }                
    }

    /**
     * Calls OWB related interceptors.
     * @param method business method
     * @param instance bean instance
     * @param arguments method arguments
     * @return result of operation
     * @throws Exception for any exception
     */    
    private Object callInterceptorsForNonContextuals(Method method, Object instance, Object[] arguments) throws Exception
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        
        //Try to resolve ejb bean
        BaseEjbBean<?> ejbBean = this.resolvedBeans.get(instance.getClass());
        
        //Not found
        if(ejbBean == null)
        {
            Set<Bean<?>> beans = manager.getComponents();
            for(Bean<?> bean : beans)
            {
                if(bean instanceof BaseEjbBean)
                {
                    if(bean.getBeanClass() == instance.getClass())
                    {
                        ejbBean = (BaseEjbBean<?>)bean;
                        break;
                    }
                }
            }
        }        
        
        if(ejbBean == null)
        {
            logger.warn("Unable to find EJB bean with class : " + instance.getClass());
        }
        else
        {
            CreationalContext<?> cc = manager.createCreationalContext(null);
            return runInterceptorStack(ejbBean.getInterceptorStack(), method, instance, arguments, ejbBean, cc);
        }
        
        return null;
    }
    
    /**
     * Calls OWB related interceptors and decorators.
     * @param method business method
     * @param instance bean instance
     * @param arguments method arguments
     * @return result of operation
     * @throws Exception for any exception
     */
    private Object callInterceptorsAndDecorators(Method method, Object instance, Object[] arguments) throws Exception
    {
        InjectionTargetBean<?> injectionTarget = (InjectionTargetBean<?>) threadLocal.get();
        
        String methodName = method.getName();
        if(ClassUtil.isObjectMethod(methodName) && !methodName.equals("toString"))
        {
            logger.warn("Calling method on proxy is restricted except Object.toString(), but current method is Object." + methodName);
        }
                
        if (InterceptorUtil.isWebBeansBusinessMethod(method) && 
                EjbUtility.isBusinessMethod(method, threadLocal.get()))
        {

            List<Object> decorators = null;

            if (injectionTarget.getDecoratorStack().size() > 0 && this.decorators == null)
            {
                Class<?> proxyClass = JavassistProxyFactory.getInterceptorProxyClasses().get(injectionTarget);
                if (proxyClass == null)
                {
                    ProxyFactory delegateFactory = JavassistProxyFactory.createProxyFactory(injectionTarget);
                    proxyClass = JavassistProxyFactory.getProxyClass(delegateFactory);
                    JavassistProxyFactory.getInterceptorProxyClasses().put(injectionTarget, proxyClass);
                }
                Object delegate = proxyClass.newInstance();
                this.delegateHandler = new DelegateHandler(threadLocal.get());
                ((ProxyObject)delegate).setHandler(this.delegateHandler);

                // Gets component decorator stack
                decorators = WebBeansDecoratorConfig.getDecoratorStack(injectionTarget, instance, delegate, (CreationalContextImpl<?>)threadLocalCreationalContext.get());                        
                //Sets decorator stack of delegate
                this.delegateHandler.setDecorators(decorators);
                
                this.decorators = decorators;
            }

            // Run around invoke chain
            List<InterceptorData> interceptorStack = injectionTarget.getInterceptorStack();
            if (interceptorStack.size() > 0)
            {
                if(this.interceptedMethodMap.get(method) == null)
                {
                    //Holds filtered interceptor stack
                    List<InterceptorData> filteredInterceptorStack = new ArrayList<InterceptorData>(interceptorStack);

                    // Filter both EJB and WebBeans interceptors
                    InterceptorUtil.filterCommonInterceptorStackList(filteredInterceptorStack, method);

                    // If there are both interceptors and decorators, add hook
                    // point to the end of the interceptor stack.
                    if (decorators != null && filteredInterceptorStack.size() > 0)
                    {
                        WebBeansDecoratorInterceptor lastInterceptor = new WebBeansDecoratorInterceptor(delegateHandler, instance);
                        InterceptorDataImpl data = new InterceptorDataImpl(true,lastInterceptor);
                        data.setDefinedInInterceptorClass(true);
                        data.setAroundInvoke(SecurityUtil.doPrivilegedGetDeclaredMethods(lastInterceptor.getClass())[0]);
                        //Add to last
                        filteredInterceptorStack.add(data);
                    }
                    
                    this.interceptedMethodMap.put(method, filteredInterceptorStack);
                }
                
                // Call Around Invokes
                if (WebBeansUtil.isContainsInterceptorMethod(this.interceptedMethodMap.get(method), InterceptorType.AROUND_INVOKE))
                {
                     return InterceptorUtil.callAroundInvokes(threadLocal.get(), instance, (CreationalContextImpl<?>)threadLocalCreationalContext.get(), method, 
                            arguments, InterceptorUtil.getInterceptorMethods(this.interceptedMethodMap.get(method), InterceptorType.AROUND_INVOKE));
                }
                
            }
            
            // If there are Decorators, allow the delegate handler to
            // manage the stack
            if (this.decorators != null)
            {
                return delegateHandler.invoke(instance, method, null, arguments);
            }
        }    
        
        return null;
    }
    
    private Object runInterceptorStack(List<InterceptorData> interceptorStack, Method method, Object instance, 
                                        Object[] arguments, BaseEjbBean<?> bean, CreationalContext<?> creationalContext) throws Exception
    {
        if (interceptorStack.size() > 0)
        {
            if(this.nonCtxInterceptedMethodMap.get(method) == null)
            {
                //Holds filtered interceptor stack
                List<InterceptorData> filteredInterceptorStack = new ArrayList<InterceptorData>(interceptorStack);

                // Filter both EJB and WebBeans interceptors
                InterceptorUtil.filterCommonInterceptorStackList(filteredInterceptorStack, method);                
                this.nonCtxInterceptedMethodMap.put(method, filteredInterceptorStack);
            }
            
            // Call Around Invokes
            if (WebBeansUtil.isContainsInterceptorMethod(this.nonCtxInterceptedMethodMap.get(method), InterceptorType.AROUND_INVOKE))
            {
                 return InterceptorUtil.callAroundInvokes(bean, instance, (CreationalContextImpl<?>)creationalContext, method, 
                        arguments, InterceptorUtil.getInterceptorMethods(this.nonCtxInterceptedMethodMap.get(method), InterceptorType.AROUND_INVOKE));
            }
            
        }
        
        
        return null;
        
    }
    
    //Read object
    private  void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
    {
        threadLocal = new ThreadLocal<BaseEjbBean<?>>();
        threadLocalCreationalContext = new ThreadLocal<CreationalContext<?>>();
        interceptedMethodMap = new WeakHashMap<Method, List<InterceptorData>>();
        nonCtxInterceptedMethodMap = new WeakHashMap<Method, List<InterceptorData>>();
    }
 
    
}
