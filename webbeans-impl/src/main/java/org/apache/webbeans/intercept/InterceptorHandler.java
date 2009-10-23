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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javassist.util.proxy.MethodHandler;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.Interceptors;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.BeansDeployer;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.ejb.EJBInterceptorConfig;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
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
 * class via methods <code>configureInterceptors(scanner)</code> and
 * <code>configureDecorators(scanner)</code>. Those methods further call
 * <code>defineInterceptor(interceptor class)</code> and <code>defineDecorator(decorator class)</code>
 * methods. Those methods finally call {@link WebBeansUtil#defineInterceptors(Class)} and
 * {@link WebBeansUtil#defineDecorators(Class)} methods for actual configuration.
 * <p>
 * Let's look at the "WebBeansUtil's" methods; 
 * </p>
 * <ul>
 * <li>
 * <code>defineInterceptors</code> : This method firstly
 * creates a "Managed Bean" for the given interceptor with
 * "WebBeansType.INTERCEPTOR" as a type. After checking some controls, it calls
 * "WebBeansInterceptorConfig#configureInterceptorClass".
 * "configureInterceptorClass" method creates a "WebBeansInterceptor" instance
 * that wraps the given managed bean instance and configuring interceptor's
 * *Interceptor Binding* annotations. If everything goes well, it adds
 * interceptor instance into the "BeanManager" interceptor list.
 * </li>
 * <li><code>defineDecorators</code> : Exactly doing same thing as "defineInterceptors". If
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
 * {@link DefinitionUtil#defineBeanInterceptorStack(AbstractBean)} and
 * {@link DefinitionUtil#defineWebBeanDecoratorStack}. In
 * "DefinitionUtil.defineSimpleWebBeanInterceptorStack", firstly it configures
 * "EJB spec. interceptors" after that configures "JSR-299 spec. interceptors."
 * In "DefinitionUtil.defineSimpleWebBeanDecoratorStack", it configures
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
public class InterceptorHandler implements MethodHandler, Serializable
{
    private static final long serialVersionUID = 1657109769733323541L;
    
    private transient static WebBeansLogger logger = WebBeansLogger.getLogger(InterceptorHandler.class);

    private AbstractBean<?> component = null;

    private transient Method calledMethod = null;

    private transient boolean isSameDecMethod = false;
    
    private transient boolean isInDecoratorCall = false;

    public InterceptorHandler(AbstractBean<?> component)
    {
        this.component = component;
    }

    @SuppressWarnings("unchecked")
    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        Object result = null;
        
        boolean interceptorRun = false;
        
        //Context of the bean
        Context webbeansContext = BeanManagerImpl.getManager().getContext(component.getScope());
        
        //Get bean instance from context
        Object webbeansInstance = webbeansContext.get((Contextual<Object>)this.component, (CreationalContext<Object>)this.component.getCreationalContext());
        
        //toString is supported but no other object method names!!!
        if ((!ClassUtil.isObjectMethod(method.getName()) || method.getName().equals("toString")) && InterceptorUtil.isWebBeansBusinessMethod(method))
        {
            checkDecoratorStackForSameDecorator(method);

            //Gets component decorator stack
            List<Object> decorators = component.getDecoratorStack();
                        
            // Run around invoke chain
            List<InterceptorData> stack = component.getInterceptorStack();
            
            List<InterceptorData> temp = new ArrayList<InterceptorData>(stack);
            
            //Filter both EJB and WebBeans interceptors
            filterCommonInterceptorStackList(temp, method);
            
            //Call Around Invokes
            if (WebBeansUtil.isContainsInterceptorMethod(temp, InterceptorType.AROUND_INVOKE))
            {
                result = callAroundInvokes(method, arguments, WebBeansUtil.getInterceptorMethods(stack, InterceptorType.AROUND_INVOKE));
                interceptorRun = true;
            }
            
            //Call decarators
            callDecorators(decorators, method, arguments);            
            

        }

        if(interceptorRun)
        {
            return result;
        }
        
        if (!method.isAccessible())
        {
            method.setAccessible(true);
        }

        return method.invoke(webbeansInstance, arguments);
    }
    
    private void checkDecoratorStackForSameDecorator(Method method)
    {
        if(this.isInDecoratorCall)
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
        }
        else
        {
            this.calledMethod = null;
        }
    }

    private void callDecorators(List<Object> decorators, Method method, Object[] arguments)
    {

        Iterator<Object> itDec = decorators.iterator();
        
        while (itDec.hasNext())
        {
            if(!this.isInDecoratorCall)
            {
                this.isInDecoratorCall = true;
            }
            
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
            finally
            {
                this.isInDecoratorCall = false;
            }

        }
    }

    private <T> Object callAroundInvokes(Method proceed, Object[] arguments, List<InterceptorData> stack) throws Exception
    {
        InvocationContextImpl impl = new InvocationContextImpl(this.component, null,proceed, arguments, stack, InterceptorType.AROUND_INVOKE);

        return impl.proceed();

    }

    private boolean shouldRemoveInterceptorCommon(InterceptorData id, Method method)
    {
        boolean isMethodAnnotatedWithInterceptorClass = false;
        
        boolean isMethodAnnotatedWithExcludeInterceptorClass = false;

        if (id.isDefinedWithWebBeansInterceptor())
        {
            if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(method.getDeclaredAnnotations()))
            {
                isMethodAnnotatedWithInterceptorClass = true;
            }

            if (AnnotationUtil.hasMethodAnnotation(method, ExcludeClassInterceptors.class))
            {
                isMethodAnnotatedWithExcludeInterceptorClass = true;
            }
        }
        else
        {
            if (AnnotationUtil.hasMethodAnnotation(method, Interceptors.class))
            {
                isMethodAnnotatedWithInterceptorClass = true;   
            }

            if (AnnotationUtil.hasMethodAnnotation(method, ExcludeClassInterceptors.class))
            {
                isMethodAnnotatedWithExcludeInterceptorClass = true;   
            }
        }

        if (isMethodAnnotatedWithInterceptorClass)
        {

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
            if (id.isDefinedInMethod() && !id.getAnnotatedMethod().equals(method))
            {
                return true;
            }
        }
        else if (id.isDefinedInMethod())
        {
            return true;
        }

        return false;
    }

    private void filterCommonInterceptorStackList(final List<InterceptorData> stack, Method method)
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

}
