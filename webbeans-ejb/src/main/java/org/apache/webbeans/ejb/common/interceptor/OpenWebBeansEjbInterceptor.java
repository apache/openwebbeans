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
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.corespi.ServiceLoader;
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
    
    private static class CallReturnValue
    {
        // true if OWB kicked off the interceptor/decorator stack, so the EJB method does not need to be separately invoked.
        public boolean INTERCEPTOR_OR_DECORATOR_CALL = false;
        public Object RETURN_VALUE = null;
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
        threadLocal.set(null);
        threadLocalCreationalContext.set(null);
        
        threadLocal.remove();
        threadLocalCreationalContext.remove();
    }
    
    /**
     * Called for every business methods.
     * @param ejbContext invocation context
     * @return instance
     * @throws Exception
     */
    @AroundInvoke
    public Object callToOwbInterceptors(InvocationContext ejbContext) throws Exception
    {
        CallReturnValue rv = null;
        boolean requestCreated = false;
        boolean applicationCreated = false;
        boolean requestAlreadyActive = false;
        boolean applicationAlreadyActive = false;
       
        if (logger.wblWillLogDebug())
        { 
            logger.debug("Intercepting EJB method {0} ", ejbContext.getMethod());
        }
        
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
                rv = callInterceptorsAndDecorators(ejbContext.getMethod(), ejbContext.getTarget(), ejbContext.getParameters(), ejbContext);    
            }
            else
            {
                //Call OWB interceptors
                rv = callInterceptorsForNonContextuals(ejbContext.getMethod(), ejbContext.getTarget(), ejbContext.getParameters(), ejbContext);
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
        
        //If bean has no interceptor or decorator
        //Call ejb bean instance
        if(!rv.INTERCEPTOR_OR_DECORATOR_CALL)
        {
            return ejbContext.proceed();
        }
        
        return rv.RETURN_VALUE;
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
                    //run OWB interceptors
                    impl.proceed();
                    
                    //run EJB interceptors
                    context.proceed();
                }
                catch (Exception e)
                {
                    logger.error(OWBLogConst.ERROR_0008, e, "@PostConstruct.");    
                    throw new RuntimeException(e);
                }
            }                        
        }
        else 
        { 
            runPrePostForNonContextual(context, InterceptorType.POST_CONSTRUCT);
        }
  
        if (OpenWebBeansConfiguration.getInstance().isUseEJBInterceptorInjection()) 
        { 
            Object instance = context.getTarget();
            this.injector = new OWBInjector();
            try
            {
                this.injector.inject(instance, threadLocalCreationalContext.get());
            }
            catch (Exception e)
            {
                logger.error(OWBLogConst.ERROR_0026, e, threadLocal.get());
            }
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
                    //Call OWB interceptord
                    impl.proceed();
                    
                    //Call EJB interceptors
                    context.proceed();
                }
                catch (Exception e)
                {
                    logger.error(OWBLogConst.ERROR_0008, e, "@PreDestroy.");
                    throw new RuntimeException(e);
                }
            }                        
        }
        else 
        { 
            runPrePostForNonContextual(context, InterceptorType.PRE_DESTROY);
        }
        
        if(this.injector != null)
        {
            this.injector.destroy();
        }
        
        this.interceptedMethodMap.clear();
        this.resolvedBeans.clear();
        this.nonCtxInterceptedMethodMap.clear();
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
     * Find the ManagedBean that corresponds to an instance of an EJB class
     * @param instance an instance of a class whose corresponding Managed Bean is to be searched for
     * @return the correspondin BaseEjbBean, null if not found
     */
    private BaseEjbBean<?> findTargetBean(Object instance) 
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        if (instance == null) { 
            logger.debug("findTargetBean was passed a null instance.");
            return null;
        }
        if (logger.wblWillLogDebug())
        {
            logger.debug("looking up bean for instance [{0}]", instance.getClass());
        }

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
                        if (logger.wblWillLogDebug())
                        {
                            logger.debug("Found managed bean for [{0}] [{1}]", instance.getClass(), ejbBean);
                        }
                        this.resolvedBeans.put(instance.getClass(), ejbBean);
                        break;
                    }
                }
            }
        }        
        else 
        {
            if (logger.wblWillLogDebug())
            {
                logger.debug("Managed bean for [{0}] found in cache: [{1}]", instance.getClass(),  ejbBean);
            }
        }
        
        return ejbBean;
    }
    /**
     * Calls OWB related interceptors.
     * @param method business method
     * @param instance bean instance
     * @param arguments method arguments
     * @return result of operation
     * @throws Exception for any exception
     */    
    private CallReturnValue callInterceptorsForNonContextuals(Method method, Object instance, Object[] arguments, InvocationContext ejbContext) throws Exception
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        
        //Try to resolve ejb bean
        BaseEjbBean<?> ejbBean =  findTargetBean(instance);
            
        CallReturnValue rv = new CallReturnValue();
        rv.INTERCEPTOR_OR_DECORATOR_CALL = false;
        if(ejbBean == null)
        {
            if (logger.wblWillLogWarn())
            {
                logger.warn(OWBLogConst.WARN_0008,  instance.getClass(), manager.getComponents());
            }
            return rv;
        }
        else
        {
            CreationalContext<?> cc = manager.createCreationalContext(null);
            try 
            { 
                return runInterceptorStack(ejbBean.getInterceptorStack(), method, instance, arguments, ejbBean, cc, ejbContext);
            }
            finally { 
                cc.release();
            }
        }

    }
    
    /**
     * Calls OWB related interceptors and decorators.
     * @param method business method
     * @param instance bean instance
     * @param arguments method arguments
     * @return result of operation
     * @throws Exception for any exception
     */
    private CallReturnValue callInterceptorsAndDecorators(Method method, Object instance, Object[] arguments, InvocationContext ejbContext) throws Exception
    {
        CallReturnValue rv = new CallReturnValue();
        InjectionTargetBean<?> injectionTarget = (InjectionTargetBean<?>) threadLocal.get();
        
        String methodName = method.getName();
        if(ClassUtil.isObjectMethod(methodName) && !methodName.equals("toString"))
        {
            logger.trace("Calling method on proxy is restricted except Object.toString(), but current method is Object. [{0}]", methodName);
        }
                
        if (InterceptorUtil.isWebBeansBusinessMethod(method) && 
                EjbUtility.isBusinessMethod(method, threadLocal.get()))
        {

            List<Object> decorators = null;
            DelegateHandler delegateHandler = null;
            if (injectionTarget.getDecoratorStack().size() > 0)
            {
                Class<?> proxyClass = JavassistProxyFactory.getInterceptorProxyClasses().get(injectionTarget);
                if (proxyClass == null)
                {
                    ProxyFactory delegateFactory = JavassistProxyFactory.createProxyFactory(injectionTarget);
                    proxyClass = JavassistProxyFactory.getProxyClass(delegateFactory);
                    JavassistProxyFactory.getInterceptorProxyClasses().put(injectionTarget, proxyClass);
                }
                Object delegate = proxyClass.newInstance();
                delegateHandler = new DelegateHandler(threadLocal.get(),ejbContext);
                ((ProxyObject)delegate).setHandler(delegateHandler);

                // Gets component decorator stack
                decorators = WebBeansDecoratorConfig.getDecoratorStack(injectionTarget, instance, delegate, (CreationalContextImpl<?>)threadLocalCreationalContext.get());                        
                //Sets decorator stack of delegate
                delegateHandler.setDecorators(decorators);
                
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

                    logger.debug("Interceptor stack for method {0}: {1}", method, filteredInterceptorStack);
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
                     rv.INTERCEPTOR_OR_DECORATOR_CALL = true;
                     rv.RETURN_VALUE = InterceptorUtil.callAroundInvokes(threadLocal.get(), instance, (CreationalContextImpl<?>)threadLocalCreationalContext.get(), method, 
                            arguments, InterceptorUtil.getInterceptorMethods(this.interceptedMethodMap.get(method), InterceptorType.AROUND_INVOKE), ejbContext);
                     
                     return rv;
                }
                
            }
            
            // If there are Decorators, allow the delegate handler to
            // manage the stack
            if (decorators != null)
            {
                rv.INTERCEPTOR_OR_DECORATOR_CALL = true;
                rv.RETURN_VALUE = delegateHandler.invoke(instance, method, null, arguments); 
                return rv;
            }
        }    
        
        rv.INTERCEPTOR_OR_DECORATOR_CALL = false;
        
        return rv;
    }
    
    private CallReturnValue runInterceptorStack(List<InterceptorData> interceptorStack, Method method, Object instance, 
                                        Object[] arguments, BaseEjbBean<?> bean, CreationalContext<?> creationalContext, InvocationContext ejbContext) throws Exception
    {
        CallReturnValue rv = new CallReturnValue();
        if (interceptorStack.size() > 0)
        {
            if(this.nonCtxInterceptedMethodMap.get(method) == null)
            {
                //Holds filtered interceptor stack
                List<InterceptorData> filteredInterceptorStack = new ArrayList<InterceptorData>(interceptorStack);

                // Filter both EJB and WebBeans interceptors
                InterceptorUtil.filterCommonInterceptorStackList(filteredInterceptorStack, method);  
                logger.debug("Interceptor stack for method {0}: {1}", method, filteredInterceptorStack);
                this.nonCtxInterceptedMethodMap.put(method, filteredInterceptorStack);
            }
            
            // Call Around Invokes
            if (WebBeansUtil.isContainsInterceptorMethod(this.nonCtxInterceptedMethodMap.get(method), InterceptorType.AROUND_INVOKE))
            {
                 rv.INTERCEPTOR_OR_DECORATOR_CALL = true;
                 rv.RETURN_VALUE = InterceptorUtil.callAroundInvokes(bean, instance, (CreationalContextImpl<?>)creationalContext, method, 
                        arguments, InterceptorUtil.getInterceptorMethods(this.nonCtxInterceptedMethodMap.get(method), InterceptorType.AROUND_INVOKE),
                        ejbContext);
                 
                 return rv;
            }
            
        }
        
        rv.INTERCEPTOR_OR_DECORATOR_CALL = false;
        
        return rv;
        
    }
   
    /**
     * Run @PostConstruct or @PreDestroy for a non-contextual EJB
     * @param ejbContext the EJB containers InvocationContext
     * @param interceptorType PreDestroy or PostConstruct
     */
    public void runPrePostForNonContextual(InvocationContext ejbContext, InterceptorType interceptorType) 
    {
        CreationalContext<?> localcc = null;
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        Object instance = ejbContext.getTarget();
        
        BaseEjbBean<?> bean = findTargetBean(instance);
        if (bean == null) { 
            logger.debug("No bean for instance [{0}]", instance);
            return;
        }
        
        List<InterceptorData> interceptorStack = bean.getInterceptorStack();
        
        if (interceptorStack.size() > 0 && WebBeansUtil.isContainsInterceptorMethod(interceptorStack, interceptorType)) 
        {
            localcc = manager.createCreationalContext(null);
            
            InvocationContextImpl impl = new InvocationContextImpl(null, instance, null, null, 
                    InterceptorUtil.getInterceptorMethods(interceptorStack, interceptorType), interceptorType);
            impl.setCreationalContext(localcc);
            
            try
            {
                impl.proceed();
            }
            catch (Exception e)
            {
                logger.error(OWBLogConst.ERROR_0008, e, interceptorType);                
            }    
        }       
        else 
        { 
            logger.debug("No lifecycle interceptors for [{0}]", instance);
        }

        try 
        { 
           ejbContext.proceed();
        }
        catch (Exception e) 
        { 
            logger.warn(OWBLogConst.WARN_0007, e);
            throw new RuntimeException(e);
        }
        finally 
        { 
          if (localcc != null) localcc.release();
        }
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
