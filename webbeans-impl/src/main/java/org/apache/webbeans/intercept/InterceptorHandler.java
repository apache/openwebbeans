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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import javax.interceptor.ExcludeClassInterceptors;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.BeansDeployer;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.decorator.DelegateHandler;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.decorator.WebBeansDecoratorInterceptor;
import org.apache.webbeans.intercept.ejb.EJBInterceptorConfig;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Logic for how interceptors & decorators work in OWB.
 * 
 * <ul>
 * <li><b>1- Configuration of decorators and interceptors</b>
 * <p>
 * Decorators and Interceptors are configured from {@link BeansDeployer}
 * class via methods <code>defineManagedBean(class)</code> and Those methods further call
 * <code>defineInterceptor(interceptor class)</code> and <code>defineDecorator(decorator class)</code>
 * methods. Those methods finally call {@link WebBeansUtil#defineInterceptor(Class)} and
 * {@link WebBeansUtil#defineDecorator(Class)} methods for actual configuration.
 * <p>
 * Let's look at the "WebBeansUtil's" methods; 
 * </p>
 * <ul>
 * <li>
 * <code>defineInterceptor</code> : This method firstly
 * creates a "Managed Bean" for the given interceptor with
 * "WebBeansType.INTERCEPTOR" as a type. After checking some controls, it calls
 * "WebBeansInterceptorConfig#configureInterceptorClass".
 * "configureInterceptorClass" method creates a "WebBeansInterceptor" instance
 * that wraps the given managed bean instance and configuring interceptor's
 * *Interceptor Binding* annotations. If everything goes well, it adds
 * interceptor instance into the "BeanManager" interceptor list.
 * </li>
 * <li><code>defineDecorator</code> : Exactly doing same thing as "defineInterceptor". If
 * everything goes well, it adds decorator instance into the "BeanManager"
 * decorator list.</li>
 * </p>
 * </li></ul>
 * <li><b>2* Configuring ManagedBean Instance Interceptor and Decorator Stack</b>
 * <p>
 * Currently interceptors and decorators are supported for the "Managed Beans".
 * OWB delegates calling of "EJB Beans" interceptors to the EJB container. It
 * does not provide built-in interceptor and decorator support for EJB beans.
 * Current implementation supports configuration of the interceptors on the
 * "Managed Beans" with 2 different scenarios, i.e. it supports
 * "EJB related interceptors ( defined by EJB specification)" and
 * "JSR-299 related interceptors (defined by interceptor bindings)". Managed
 * Beans interceptor and decorator stacks are configured after they are
 * instantiated by the container first time. This method can be found in the
 * AbstractInjectionTargetBean" class "afterConstructor()" method. Actual
 * configuration is done by the
 * {@link DefinitionUtil#defineBeanInterceptorStack(AbstractOwbBean)} and
 * {@link DefinitionUtil#defineDecoratorStack}. In
 * "DefinitionUtil.defineBeanInterceptorStack", firstly it configures
 * "EJB spec. interceptors" after that configures "JSR-299 spec. interceptors."
 * In "DefinitionUtil.defineDecoratorStack", it configures
 * decorator stack. "EJBInterceptorConfig" class is responsible for finding all
 * interceptors for given managed bean class according to the EJB Specification.
 * (But as you said, it may not include AroundInvoke/PostConstruct etc.
 * disablement scenario!). "WebBeansInterceptorConfig" class is responsible for
 * finding all interceptors for a given managed bean class according to the
 * "JSR-299, spec." It adds all interceptors into the bean's interceptor stack.
 * It first adds "EJB" related interceptors, after that adds "JSR-299" related
 * interceptors. For "JSR-299" related interceptors, it orders the interceptors
 * according to the "InterceptorComparator". Basically, it puts interceptors in
 * order according to how they are ordered in a "beans.xml" configuration file.
 * Similarly, it configures managed bean's decorator stack according to the
 * decorator resolution rules. Also, it orders decorators according to the
 * "beans.xml" configuration file that contains decorator declarations.
 * </p>
 * </li>
 * <li><b>3* Invocation of Interceptors and Decorators</b>
 * <p>
 * Invocation is handled by the "InterceptorHandler" class (It has an absurd
 * name, it can be changed to a more meaningful name :)). It works nearly same
 * as what you have explained. First of all, it checks that calling method is a
 * business method of a managed bean or not. After that it filters interceptor
 * stack for calling method (Current design of filtering may not be optimal!).
 * Firstly it adds EJB interceptor to the list and then adds JSR-299
 * interceptors. After that, it starts to call all interceptors in order. After
 * consuming all interceptors it calls decorators. (as you explained, seems that
 * the logic may not be correct here. Currently, interceptors and decorators are
 * not related with each other. They are called independently).This must be changed!.
 * </p>
 * </li>
 * </ul>
 * 
 * @version $Rev$ $Date$
 * 
 * @see WebBeansInterceptorConfig
 * @see WebBeansDecoratorConfig
 * @see WebBeansInterceptor
 * @see WebBeansDecorator
 * @see EJBInterceptorConfig
 */
public abstract class InterceptorHandler implements MethodHandler, Serializable
{
    /**Default serial id*/
    private static final long serialVersionUID = 1L;
    
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(InterceptorHandler.class);
    
    /**Proxied bean*/
    protected OwbBean<?> bean = null;

    /**
     * Creates a new handler.
     * @param bean proxied bean
     */
    protected InterceptorHandler(OwbBean<?> bean)
    {
        this.bean = bean;
    }

    /**
     * Calls decorators and interceptors and actual
     * bean method.
     * @param instance actual bean instance
     * @param method business method
     * @param proceed proceed method
     * @param arguments method arguments
     * @param ownerCreationalContext bean creational context
     * @return method result
     * @throws Exception for exception
     */
    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments, CreationalContextImpl<?> ownerCreationalContext) throws Exception
    {
        Object result = null;
        
        try
        {
            if (bean instanceof InjectionTargetBean<?>)
            {
                InjectionTargetBean<?> injectionTarget = (InjectionTargetBean<?>) this.bean;

                // toString is supported but no other object method names!!!
                if ((!ClassUtil.isObjectMethod(method.getName()) || method.getName().equals("toString")) 
                    && InterceptorUtil.isWebBeansBusinessMethod(method))
                {

                    DelegateHandler delegateHandler = null;
                    List<Object> decorators = null;

                    if (injectionTarget.getDecoratorStack().size() > 0)
                    {
                        Class<?> proxyClass = JavassistProxyFactory.getInterceptorProxyClasses().get(bean);
                        if (proxyClass == null)
                        {
                            ProxyFactory delegateFactory = JavassistProxyFactory.createProxyFactory(bean);
                            proxyClass = JavassistProxyFactory.getProxyClass(delegateFactory);
                            JavassistProxyFactory.getInterceptorProxyClasses().put(bean, proxyClass);
                        }
                        Object delegate = proxyClass.newInstance();
                        delegateHandler = new DelegateHandler();
                        ((ProxyObject)delegate).setHandler(delegateHandler);

                        // Gets component decorator stack
                        decorators = WebBeansDecoratorConfig.getDecoratorStack(injectionTarget, instance, delegate, ownerCreationalContext);                        
                        //Sets decorator stack of delegate
                        delegateHandler.setDecorators(decorators);
                    }

                    // Run around invoke chain
                    List<InterceptorData> interceptorStack = injectionTarget.getInterceptorStack();
                    if (interceptorStack.size() > 0)
                    {
                        //Holds filtered interceptor stack
                        List<InterceptorData> filteredInterceptorStack = new ArrayList<InterceptorData>(interceptorStack);
    
                        // Filter both EJB and WebBeans interceptors
                        filterCommonInterceptorStackList(filteredInterceptorStack, method, ownerCreationalContext);
    
                        // If there are both interceptors and decorators, add hook
                        // point to the end of the interceptor stack.
                        if (decorators != null && filteredInterceptorStack.size() > 0)
                        {
                            WebBeansDecoratorInterceptor lastInterceptor = new WebBeansDecoratorInterceptor(delegateHandler, instance);
                            InterceptorDataImpl data = new InterceptorDataImpl(true,lastInterceptor);
                            data.setDefinedInInterceptorClass(true);
                            data.setAroundInvoke(lastInterceptor.getClass().getDeclaredMethods()[0]);
                            //Add to last
                            filteredInterceptorStack.add(data);
                        }
    
                        // Call Around Invokes
                        if (WebBeansUtil.isContainsInterceptorMethod(filteredInterceptorStack, InterceptorType.AROUND_INVOKE))
                        {
                            return callAroundInvokes(method, arguments, WebBeansUtil.getInterceptorMethods(filteredInterceptorStack, InterceptorType.AROUND_INVOKE));
                        }
                    }
                    
                    // If there are Decorators, allow the delegate handler to
                    // manage the stack
                    if (decorators != null)
                    {
                        return delegateHandler.invoke(instance, method, proceed, arguments);
                    }
                }
            }
            
            //If here call actual method            
            //If not interceptor or decorator calls
            //Do normal calling
            boolean access = method.isAccessible();
            method.setAccessible(true);
            try
            {
                result = method.invoke(instance, arguments);
                
            }finally
            {
                method.setAccessible(access);
            }
            
        }
        catch (InvocationTargetException e)
        {
            Throwable target = e.getTargetException();
            //Look for target exception
            if (Exception.class.isAssignableFrom(target.getClass()))
            {
                throw (Exception) target;
            }
            else
            {
                throw e;
            }
        }

        return result;
    }

    /**
     * Call around invoke method of the given bean on
     * calling interceptedMethod.
     * @param interceptedMethod intercepted bean method
     * @param arguments method actual arguments
     * @param stack interceptor stack
     * @return return of method
     * @throws Exception for any exception
     */
    protected abstract Object callAroundInvokes(Method interceptedMethod, Object[] arguments, List<InterceptorData> stack) throws Exception;
    
    /**
     * 
     * @return bean manager
     */
    protected BeanManagerImpl getBeanManager()
    {
        return BeanManagerImpl.getManager();
    }
    
    
    /**
     * Returns true if this interceptor data is not related
     * false othwewise.
     * @param id interceptor data
     * @param method called method
     * @return true if this interceptor data is not related
     */
    private boolean shouldRemoveInterceptorCommon(InterceptorData id, Method method)
    {
        boolean isMethodAnnotatedWithExcludeInterceptorClass = false;
        if (AnnotationUtil.hasMethodAnnotation(method, ExcludeClassInterceptors.class))
        {
            isMethodAnnotatedWithExcludeInterceptorClass = true;
        }

        if (isMethodAnnotatedWithExcludeInterceptorClass)
        {
            // If the interceptor is defined at the class level it should be
            // removed due to ExcludeClassInterceptors method annotation
            if (!id.isDefinedInMethod() && id.isDefinedInInterceptorClass())
            {
                return true;
            }
        }

        // If the interceptor is defined in a different method, remove it
        if (id.isDefinedInMethod() && !id.getInterceptorBindingMethod().equals(method))
        {
            return true;
        }

        return false;
    }

    /**
     * Filter bean interceptor stack.
     * @param stack interceptor stack
     * @param method called method on proxy
     * @param ownerCreationalContext bean creational context
     */
    private void filterCommonInterceptorStackList(List<InterceptorData> stack, Method method, CreationalContextImpl<?> ownerCreationalContext)
    {
        Iterator<InterceptorData> it = stack.iterator();
        while (it.hasNext())
        {
            InterceptorData data = it.next();

            if (shouldRemoveInterceptorCommon(data, method))
            {
                it.remove();
            }
        }        
    }
        
    /**
     * Write to stream.
     * @param s stream
     * @throws IOException
     */
    private  void writeObject(ObjectOutputStream s) throws IOException
    {
        s.writeLong(serialVersionUID);
        // we have to write the ids for all beans, not only PassivationCapable
        // since this gets serialized along with the Bean proxy.
        String passivationId = this.bean.getId();
        if (passivationId!= null)
        {
            s.writeObject(passivationId);
        }
        else
        {
            s.writeObject(null);
            logger.warn("Trying to serialize not passivated capable bean proxy : " + this.bean);
        }
    }
    
    /**
     * Read from stream.
     * @param s stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private  void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
    {
        if(s.readLong() == serialVersionUID)
        {
            String passivationId = (String) s.readObject();
            if (passivationId != null)
            {
                this.bean = (OwbBean<?>)BeanManagerImpl.getManager().getPassivationCapableBean(passivationId);
            }
        }
        else
        {
            logger.warn("Trying to deserialize not passivated capable bean proxy : " + this.bean);
        }
    }

}
