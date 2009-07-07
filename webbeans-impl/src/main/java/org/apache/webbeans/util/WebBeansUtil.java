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
package org.apache.webbeans.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.decorator.Decorator;
import javax.ejb.EnterpriseBean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.ScopeType;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.Initializer;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Named;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.UnproxyableResolutionException;
import javax.enterprise.inject.deployment.DeploymentType;
import javax.enterprise.inject.deployment.Specializes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.stereotype.Stereotype;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.webbeans.annotation.ApplicationScopeLiteral;
import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.annotation.NewLiteral;
import org.apache.webbeans.annotation.ProductionLiteral;
import org.apache.webbeans.annotation.RequestedScopeLiteral;
import org.apache.webbeans.annotation.StandardLiteral;
import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.component.BaseBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.ConversationBean;
import org.apache.webbeans.component.ExtensionBean;
import org.apache.webbeans.component.InjectionPointBean;
import org.apache.webbeans.component.InstanceBean;
import org.apache.webbeans.component.BeanManagerBean;
import org.apache.webbeans.component.NewBean;
import org.apache.webbeans.component.EventBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.EJBWebBeansConfigurator;
import org.apache.webbeans.config.ManagedBeanConfigurator;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.stereotype.IStereoTypeModel;
import org.apache.webbeans.ejb.EJBUtil;
import org.apache.webbeans.ejb.orm.ORMUtil;
import org.apache.webbeans.event.EventImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.apache.webbeans.exception.inject.InconsistentSpecializationException;
import org.apache.webbeans.exception.inject.NullableDependencyException;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.intercept.InterceptorDataImpl;
import org.apache.webbeans.intercept.InterceptorType;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.plugins.OpenWebBeansPlugin;
import org.apache.webbeans.plugins.PluginLoader;

/**
 * Contains some utility methods used in the all project.
 * @version $Rev$ $Date$ 
 */
public final class WebBeansUtil
{
    // No instantiate
    private WebBeansUtil()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets current classloader with current thread.
     * 
     * @return Current class loader instance
     */
    public static ClassLoader getCurrentClassLoader()
    {
        ClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
        {

            public ClassLoader run()
            {
                try
                {
                    return Thread.currentThread().getContextClassLoader();

                }
                catch (Exception e)
                {
                    return null;
                }
            }

        });

        if (loader == null)
        {
            loader = WebBeansUtil.class.getClassLoader();
        }

        return loader;
    }

    /**
     * Checks the generic type requirements.
     * 
     * @param bean managed bean instance
     */
    public static void checkGenericType(Bean<?> bean)
    {
    	Asserts.assertNotNull(bean);
    	
    	Class<?> clazz = bean.getBeanClass();
    	
        if (ClassUtil.isDefinitionConstainsTypeVariables(clazz))
        {
            if(!bean.getScopeType().equals(Dependent.class))
            {
                throw new WebBeansConfigurationException("Generic type may only defined with scope type @Dependent for bean class : " + clazz.getName());
            }
        }
    }
    
    
    /**
     * Check producer method return type.
     * 
     * @param component producer method component
     * @param type return type 
     */
    public static void checkProducerGenericType(Bean<?> component,Member member)
    {
    	Asserts.assertNotNull(component);
    	
    	Type type = null;
    	
    	if(component instanceof ProducerMethodBean)
    	{
    		type = ((ProducerMethodBean<?>)component).getCreatorMethod().getGenericReturnType();
    	}
    	else if(component instanceof ProducerFieldBean)
    	{
    		type = ((ProducerFieldBean<?>)component).getCreatorField().getGenericType();
    	}
    	else
    	{
    		throw new IllegalArgumentException("Component must be producer field or method : " + component);
    	}
    	
    	String message = "Producer field/method : " + member.getName() + " in class : " + member.getDeclaringClass().getName(); 
    	
    	if(checkGenericForProducers(type, message))
    	{
            if(!component.getScopeType().equals(Dependent.class))
            {
                throw new WebBeansConfigurationException(message + " scope type must bee @Dependent");
            }
    	}
    }
    
    //Helper method
    private static boolean checkGenericForProducers(Type type, String message)
    {
    	boolean result = false;
    	
    	if(type instanceof TypeVariable)
    	{
    		throw new WebBeansConfigurationException(message + " return type can not be type variable");
    	}
    	
    	if(ClassUtil.isParametrizedType(type))
    	{
    		Type[] actualTypes = ClassUtil.getActualTypeArguements(type);
    		
    		if(actualTypes.length == 0)
    		{
        		throw new WebBeansConfigurationException(message + " return type must define actual type arguments or type variable");
    		}
    		
    		for(Type actualType : actualTypes)
    		{
    			if(ClassUtil.isWildCardType(actualType))
    			{
    				throw new WebBeansConfigurationException(message + " return type can not define wildcard actual type argument");
    			}
    			
    			if(ClassUtil.isTypeVariable(actualType))
    			{
    				result = true; 
    			}
    		}    		
    	}
    	
    	return result;
    }
    
    /**
     * Return <code>true</code> if the given class is ok for simple web bean conditions,
     * <code>false</code> otherwise.
     * 
     * @param clazz class in hand
     * @return <code>true</code> if the given class is ok for simple web bean conditions.
     */
    public static void isSimpleWebBeanClass(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        int modifier = clazz.getModifiers();

        if (!ClassUtil.isStatic(modifier) && ClassUtil.isInnerClazz(clazz))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not be non-static inner class");

        if (!ClassUtil.isConcrete(clazz) && !AnnotationUtil.isAnnotationExistOnClass(clazz, Decorator.class))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " have to be concrete if not defines as @Decorator");

        if (EJBUtil.isEJBClass(clazz))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not be EJB class");

        if (ClassUtil.isAssignable(Servlet.class, clazz))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not implement Servlet interface");

        if (ClassUtil.isAssignable(Filter.class, clazz))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not implement Filter interface");

        if (ClassUtil.isAssignable(ServletContextListener.class, clazz))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not implement ServletContextListener");

        if (ClassUtil.isAssignable(HttpSessionListener.class, clazz))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not implement HttpSessionListener");

        if (ClassUtil.isAssignable(ServletRequestListener.class, clazz))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can notimplement ServletRequestListener");

        if (ClassUtil.isAssignable(EnterpriseBean.class, clazz))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not implement EnterpriseBean");

        // TODO ejb-jar.xml check
        if (EJBUtil.isDefinedInXML(clazz.getName()))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not defined in the ejb-jar.xml");

        // TODO orm.xml check
        if (ORMUtil.isDefinedInXML(clazz.getName()))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not defined in orm.xml");

        if (!isConstructureOk(clazz))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " must define at least one Constructor");

        // and finally call all checks which are defined in plugins like JSF, JPA, etc
        List<OpenWebBeansPlugin> plugins = PluginLoader.getInstance().getPlugins();
        for (OpenWebBeansPlugin plugin : plugins)
        {
            plugin.isSimpleBeanClass(clazz);
        }
    }

    public static <T> Constructor<T> defineConstructor(Class<T> clazz) throws WebBeansConfigurationException
    {
        Asserts.nullCheckForClass(clazz);
        Constructor<T> result = null;
        Constructor<T>[] constructors = ClassUtil.getConstructors(clazz);

        boolean inAnnotation = false;
        int j = 0;

        /* Check for @Initializer */
        for (Constructor<T> constructor : constructors)
        {
            j++;
            if (constructor.getAnnotation(Initializer.class) != null)
            {
                if (inAnnotation == true)// duplicate @In
                {
                    throw new WebBeansConfigurationException("There are more than one Constructor with Initializer annotation in class " + clazz.getName());
                }
                else
                {
                    inAnnotation = true;
                    result = constructor;
                }
            }
        }

        if (result != null)
        {
            Annotation[][] parameterAnns = result.getParameterAnnotations();
            for (Annotation[] parameters : parameterAnns)
            {
                for (Annotation param : parameters)
                {
                    Annotation btype = param.annotationType().getAnnotation(Disposes.class);
                    if (btype != null)
                    {
                        throw new WebBeansConfigurationException("Constructor parameter binding type annotation can not be @Disposes annotation in class " + clazz.getName());
                    }
                    else
                    {
                        btype = param.annotationType().getAnnotation(Observes.class);
                        if (btype != null)
                        {
                            throw new WebBeansConfigurationException("Constructor parameter binding type annotation can not be @Observes annotation in class " + clazz.getName());
                        }
                    }
                }

            }
        }

        if (result == null)
        {
            if ((result = ClassUtil.isContaintNoArgConstructor(clazz)) != null)
            {
                return result;
            }
            else
            {
                throw new WebBeansConfigurationException("No constructor is found for the class : " + clazz.getName());
            }
        }

        return result;
    }

    /**
     * Check that simple web beans class has compatible constructor.
     * 
     * @param clazz web beans simple class
     * @throws WebBeansConfigurationException if the web beans has incompatible
     *             constructor
     */
    public static boolean isConstructureOk(Class<?> clazz) throws WebBeansConfigurationException
    {
        Asserts.nullCheckForClass(clazz);

        if (ClassUtil.isContaintNoArgConstructor(clazz) != null)
        {
            return true;
        }

        Constructor<?>[] constructors = ClassUtil.getConstructors(clazz);

        int j = 0;

        for (Constructor<?> constructor : constructors)
        {
            j++;
            if (constructor.getAnnotation(Initializer.class) != null)
            {
                return true;
            }
        }

        return false;
    }
    
    /**
     * Check producer method is ok for deployment.
     * 
     * @param method producer method
     * @param parentImplClazzName parent class name
     */
    public static void checkProducerMethodForDeployment(Method method, String parentImplClazzName)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");

        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType)
        {
            ParameterizedType pType = (ParameterizedType) returnType;

            Type[] actualArguments = pType.getActualTypeArguments();

            for (Type actualType : actualArguments)
            {
                if ((actualType instanceof TypeVariable) || (actualType instanceof WildcardType))
                {
                    throw new WebBeansConfigurationException("Producer method  : " + method.getName() + " in the class : " + parentImplClazzName + " can not return TypeVariable or WildcardType return type arguments.");
                }
            }
        }

        if (AnnotationUtil.isMethodHasAnnotation(method, Initializer.class) || AnnotationUtil.isMethodParameterAnnotationExist(method, Disposes.class) || AnnotationUtil.isMethodParameterAnnotationExist(method, Observes.class))
        {
            throw new WebBeansConfigurationException("Producer method : " + method.getName() + " in the class : " + parentImplClazzName + " can not be annotated with" + " @Initializer/@Destructor annotation or has a parameter annotated with @Disposes/@Observes");
        }
    }
    
    /**
     * Check producer field is ok for deployment.
     * 
     * @param method producer method
     * @param parentImplClazzName parent class name
     */
    public static void checkProducerFieldForDeployment(Field producerField, String parentImplClazzName)
    {
        Asserts.assertNotNull(producerField, "producerField argument can not be null");

        Type returnType = producerField.getGenericType();
        if (ClassUtil.isParametrizedType(returnType))
        {
            ParameterizedType pType = (ParameterizedType) returnType;

            Type[] actualArguments = pType.getActualTypeArguments();

            for (Type actualType : actualArguments)
            {
                if ((actualType instanceof TypeVariable) || (actualType instanceof WildcardType))
                {
                    throw new WebBeansConfigurationException("Producer field  : " + producerField.getName() + " in the class : " + parentImplClazzName + " can not return TypeVariable or WildcardType return type arguments.");
                }
            }
        }
    }    

    public static void checkProducerMethodDisposal(Method disposalMethod, String parentImplClazzName)
    {
        if (AnnotationUtil.isMethodMultipleParameterAnnotationExist(disposalMethod, Disposes.class))
        {
            throw new WebBeansConfigurationException("Disposal method : " + disposalMethod.getName() + " in class " + parentImplClazzName + " has multiple @Disposes annotation parameter");
        }

        if (AnnotationUtil.isMethodHasAnnotation(disposalMethod, Initializer.class) || AnnotationUtil.isMethodParameterAnnotationExist(disposalMethod, Observes.class) || AnnotationUtil.isMethodHasAnnotation(disposalMethod, Produces.class))
        {
            throw new WebBeansConfigurationException("Disposal method : " + disposalMethod.getName() + " in the class : " + parentImplClazzName + " can not be annotated with" + " @Initializer/@Destructor/@Produces annotation or has a parameter annotated with @Observes");
        }

    }

    /**
     * Check conditions for the new binding.
     * 
     * @param annotations annotations
     * @return Annotation[] with all binding annotations
     * @throws WebBeansConfigurationException if &x0040;New plus any other binding annotation is set or
     *         if &x0040;New is used for an Interface or an abstract class.
     */
    public static Annotation[] checkForNewBindingForDeployment(Type type, Class<?> clazz, String name, Annotation[] annotations)
    {
        Asserts.assertNotNull(type, "Type argument can not be null");
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");
        Asserts.assertNotNull(annotations, "Annotations argument can not be null");

        Annotation[] as = AnnotationUtil.getBindingAnnotations(annotations);
        for (Annotation a : annotations)
        {
            if (a.annotationType().equals(New.class))
            {
                if (as.length > 1)
                {
                    throw new WebBeansConfigurationException("@New binding annotation can not have any binding annotation in class : " + clazz.getName() + " in field/method : " + name);
                }

                if (ClassUtil.isAbstract(ClassUtil.getClass(type).getModifiers()) || ClassUtil.isInterface(ClassUtil.getClass(type).getModifiers()))
                {
                    throw new WebBeansConfigurationException("@New binding annotation field can not have interface or abstract type in class : " + clazz.getName() + " in field/method : " + name);
                }
            }
        }
        
        return as;
    }

    /**
     * Check conditions for the resources.
     * 
     * @param annotations annotations
     * @throws WebBeansConfigurationException if resource annotations exists and do not fit to the fields type, etc.
     * @see AnnotationUtil#isResourceAnnotation(Class)
     */
    public static void checkForValidResources(Type type, Class<?> clazz, String name, Annotation[] annotations)
    {
        Asserts.assertNotNull(type, "Type argument can not be null");
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");
        Asserts.assertNotNull(annotations, "Annotations argument can not be null");

        List<OpenWebBeansPlugin> plugins = PluginLoader.getInstance().getPlugins();
        for (OpenWebBeansPlugin plugin : plugins)
        {
            plugin.checkForValidResources(type, clazz, name, annotations);
        }
    }
    
    /**
     * Returns true if src scope encloses the target.
     * 
     * @param src src scope
     * @param target target scope
     * @return true if src scope encloses the target
     */
    public static boolean isScopeEncloseOther(Class<? extends Annotation> src, Class<? extends Annotation> target)
    {
        Asserts.assertNotNull(src, "Src argument can not be null");
        Asserts.assertNotNull(target, "Target argument can not be null");

        if (src.equals(ConversationScoped.class))
        {
            return true;
        }
        else if (src.equals(ApplicationScoped.class))
        {
            if (target.equals(ConversationScoped.class) || (target.equals(ApplicationScoped.class)))
            {
                return false;
            }
            else
            {
                return true;
            }

        }
        else if (src.equals(SessionScoped.class))
        {
            if (target.equals(ConversationScoped.class) || target.equals(ApplicationScoped.class) || target.equals(SessionScoped.class))
            {
                return false;
            }
            else
            {
                return true;
            }

        }
        else if (src.equals(RequestScoped.class))
        {
            return false;
        }
        else
        {
            throw new WebBeansException("Scope is not correct");
        }

    }

    /**
     * New WebBeans component class.
     * 
     * @param <T>
     * @param clazz impl. class
     * @return the new component
     */
    public static <T> NewBean<T> createNewComponent(Class<T> clazz)
    {
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

        NewBean<T> comp = null;

        if (ManagedBeanConfigurator.isSimpleWebBean(clazz))
        {
            comp = new NewBean<T>(clazz, WebBeansType.MANAGED);
            comp.setConstructor(WebBeansUtil.defineConstructor(clazz));

            DefinitionUtil.defineInjectedFields(comp);
            DefinitionUtil.defineInjectedMethods(comp);
        }
        else if (EJBWebBeansConfigurator.isEJBWebBean(clazz))
        {
            comp = new NewBean<T>(clazz, WebBeansType.ENTERPRISE);
        }
        else
        {
            throw new WebBeansConfigurationException("@New annotation on type : " + clazz.getName() + " must defined as a simple or an enterprise web bean");
        }

        comp.setImplScopeType(new DependentScopeLiteral());
        comp.addBindingType(new NewLiteral());
        comp.setName(null);
        comp.addApiType(clazz);
        comp.addApiType(Object.class);
        comp.setType(new ProductionLiteral());

        return comp;
    }
    
    /**
     * Creates a new extension bean.
     * 
     * @param <T> extension service class
     * @param clazz impl. class
     * @return a new extension service bean
     */
    public static <T> ExtensionBean<T> createExtensionComponent(Class<T> clazz)
    {
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

        ExtensionBean<T> comp = null;
        comp = new ExtensionBean<T>(clazz);
        
        DefinitionUtil.defineApiTypes(comp, clazz);
        
        comp.setImplScopeType(new ApplicationScopeLiteral());
        comp.addBindingType(new CurrentLiteral());        
        comp.setType(new ProductionLiteral());
        
        DefinitionUtil.defineObserverMethods(comp, clazz);

        return comp;
    }
    

    /**
     * Returns a new managed bean from given bean.
     * 
     * @param <T> bean type parameter
     * @param component managed bean
     * @return the new bean from given managed bean
     */
    public static <T> NewBean<T> createNewSimpleBeanComponent(ManagedBean<T> component)
    {
        Asserts.assertNotNull(component, "component argument can not be null");

        NewBean<T> comp = null;

        comp = new NewBean<T>(component.getReturnType(), WebBeansType.NEW);
        
        DefinitionUtil.defineApiTypes(comp, component.getReturnType());
        comp.setConstructor(component.getConstructor());
        
        for(Field injectedField : component.getInjectedFields())
        {
            comp.addInjectedField(injectedField);
        }
        
        for(Method injectedMethod : component.getInjectedMethods())
        {
            comp.addInjectedMethod(injectedMethod);
        }
        
        List<InterceptorData> interceptorList = component.getInterceptorStack();
        if(!interceptorList.isEmpty())
        {
            component.getInterceptorStack().addAll(interceptorList);   
        }
        
        
        comp.setImplScopeType(new DependentScopeLiteral());
        comp.addBindingType(new NewLiteral());
        comp.setType(new StandardLiteral());
        comp.setName(null);
        
        Set<InjectionPoint> injectionPoints = component.getInjectionPoints();
        for(InjectionPoint injectionPoint : injectionPoints)
        {
            comp.addInjectionPoint(injectionPoint);
        }        

        return comp;
    }    
    
    public static <T> EventBean<T> createObservableImplicitComponent(Class<T> returnType, Type eventType, Annotation... annotations)
    {
        EventBean<T> component = new EventBean<T>(returnType, eventType, WebBeansType.OBSERVABLE);

        DefinitionUtil.defineApiTypes(component, returnType);
        DefinitionUtil.defineBindingTypes(component, annotations);

        component.setType(new StandardLiteral());
        component.setImplScopeType(new DependentScopeLiteral());                      

        return component;
    }

    public static BeanManagerBean getManagerComponent()
    {
        BeanManagerBean managerComponent = new BeanManagerBean();

        managerComponent.setImplScopeType(new DependentScopeLiteral());
        managerComponent.setType(new StandardLiteral());
        managerComponent.addBindingType(new CurrentLiteral());
        managerComponent.addApiType(BeanManager.class);
        managerComponent.addApiType(Object.class);

        return managerComponent;
    }
    
    public static <T> InstanceBean<T> createInstanceComponent(ParameterizedType instance,Class<Instance<T>> clazz, Type injectedType, Annotation...obtainsBindings)
    {
        InstanceBean<T> instanceComponent = new InstanceBean<T>(clazz,injectedType);
        
        instanceComponent.addApiType(clazz);
        instanceComponent.addApiType(Object.class);
        
        DefinitionUtil.defineBindingTypes(instanceComponent, obtainsBindings);
        instanceComponent.setImplScopeType(new DependentScopeLiteral());
        instanceComponent.setType(new StandardLiteral());
        instanceComponent.setName(null);
        
        
        return instanceComponent;
    }

    public static ConversationBean getConversationComponent()
    {
        ConversationBean conversationComp = new ConversationBean();

        conversationComp.addApiType(Conversation.class);
        conversationComp.addApiType(ConversationImpl.class);
        conversationComp.addApiType(Object.class);
        conversationComp.setImplScopeType(new RequestedScopeLiteral());
        conversationComp.setType(new StandardLiteral());
        conversationComp.addBindingType(new CurrentLiteral());
        conversationComp.setName("javax.context.conversation");

        return conversationComp;
    }
    
    public static InjectionPointBean getInjectionPointComponent()
    {
        return new InjectionPointBean(null);
    }

    /**
     * Check the {@link PostConstruct} or {@link PreDestroy} annotated method
     * criterias, and return post construct or pre destroy method.
     * <p>
     * Web Beans container is responsible for setting the post construct or pre
     * destroy annotation if the web beans component is not an EJB components,
     * in this case EJB container is responsible for this.
     * </p>
     * 
     * @param clazz checked class
     * @param commonAnnotation post construct or predestroy annotation
     * @return post construct or predestroy method
     */
    public static Method checkCommonAnnotationCriterias(Class<?> clazz, Class<? extends Annotation> commonAnnotation, boolean invocationContext)
    {
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

        Method[] methods = ClassUtil.getDeclaredMethods(clazz);
        Method result = null;
        boolean found = false;
        for (Method method : methods)
        {
            if (AnnotationUtil.isMethodHasAnnotation(method, commonAnnotation))
            {
                if (ClassUtil.isMoreThanOneMethodWithName(method.getName(), clazz))
                {
                    continue;
                }

                if (found == true)
                {
                    throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotation is declared more than one method in the class : " + clazz.getName());
                }
                else
                {
                    found = true;
                    result = method;

                    // Check method criterias
                    if (ClassUtil.isMethodHasParameter(method))
                    {
                        if (!invocationContext)
                            throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not take any formal arguments");
                        else
                        {
                            // Check method criterias
                            Class<?>[] params = ClassUtil.getMethodParameterTypes(method);
                            if (params.length != 1 || !params[0].equals(InvocationContext.class))
                                throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not take any formal arguments other than InvocationContext");

                        }
                    }

                    if (!ClassUtil.getReturnType(method).equals(Void.TYPE))
                    {
                        throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " must return void type");
                    }

                    if (ClassUtil.isMethodHasCheckedException(method))
                    {
                        throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not throw any checked exception");
                    }

                    if (ClassUtil.isStatic(method.getModifiers()))
                    {
                        throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not be static");
                    }
                }
            }
        }

        return result;
    }

    /**
     * Check the {@link AroundInvoke} annotated method criterias, and return
     * around invoke method.
     * <p>
     * Web Beans container is responsible for setting around invoke annotation
     * if the web beans component is not an EJB components, in this case EJB
     * container is responsible for this.
     * </p>
     * 
     * @param clazz checked class
     * @return around invoke method
     */
    public static Method checkAroundInvokeAnnotationCriterias(Class<?> clazz)
    {
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

        Method[] methods = ClassUtil.getDeclaredMethods(clazz);
        Method result = null;
        boolean found = false;
        for (Method method : methods)
        {
            if (AnnotationUtil.isMethodHasAnnotation(method, AroundInvoke.class))
            {
                // Overriden methods
                if (ClassUtil.isMoreThanOneMethodWithName(method.getName(), clazz))
                {
                    continue;
                }

                if (found == true)
                {
                    throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotation is declared more than one method in the class : " + clazz.getName());
                }
                else
                {
                    found = true;
                    result = method;

                    // Check method criterias
                    Class<?>[] params = ClassUtil.getMethodParameterTypes(method);
                    if (params.length != 1 || !params[0].equals(InvocationContext.class))
                        throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not take any formal arguments other than InvocationContext");

                    if (!ClassUtil.getReturnType(method).equals(Object.class))
                    {
                        throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " must return Object type");
                    }

                    if (!ClassUtil.isMethodHasException(method))
                    {
                        throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " must throw Exception");
                    }

                    if (ClassUtil.isStatic(method.getModifiers()) || ClassUtil.isFinal(method.getModifiers()))
                    {
                        throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not be static or final");
                    }
                }
            }
        }

        return result;
    }

    /**
     * Configures the interceptor stack of the web beans component.
     * 
     * @param clazz interceptor class
     * @param annotation annotation type
     * @param definedInInterceptorClass check if annotation is defined in
     *            interceptor class
     * @param definedInMethod check if the interceptor is defined in the comp.
     *            method
     * @param stack interceptor stack
     * @param annotatedInterceptorClassMethod if definedInMethod, this specify
     *            method
     * @param isDefinedWithWebBeans if interceptor is defined with WebBeans
     *            spec, not EJB spec
     */
    public static void configureInterceptorMethods(Interceptor<?> webBeansInterceptor, Class<?> clazz, Class<? extends Annotation> annotation, boolean definedInInterceptorClass, boolean definedInMethod, List<InterceptorData> stack, Method annotatedInterceptorClassMethod, boolean isDefinedWithWebBeans)
    {
        InterceptorData intData = null;
        Method method = null;

        if (annotation.equals(AroundInvoke.class))
        {
            method = WebBeansUtil.checkAroundInvokeAnnotationCriterias(clazz);
        }
        else if (annotation.equals(PostConstruct.class))
        {
            if (definedInInterceptorClass)
            {
                method = WebBeansUtil.checkCommonAnnotationCriterias(clazz, PostConstruct.class, true);
            }
            else
            {
                method = WebBeansUtil.checkCommonAnnotationCriterias(clazz, PostConstruct.class, false);
            }
        }
        else if (annotation.equals(PreDestroy.class))
        {
            if (definedInInterceptorClass)
            {
                method = WebBeansUtil.checkCommonAnnotationCriterias(clazz, PreDestroy.class, true);
            }
            else
            {
                method = WebBeansUtil.checkCommonAnnotationCriterias(clazz, PreDestroy.class, false);
            }
        }

        if (method != null)
        {
            intData = new InterceptorDataImpl(isDefinedWithWebBeans);
            intData.setDefinedInInterceptorClass(definedInInterceptorClass);
            intData.setDefinedInMethod(definedInMethod);
            intData.setAnnotatedMethod(annotatedInterceptorClassMethod);
            intData.setWebBeansInterceptor(webBeansInterceptor);

            if (definedInInterceptorClass)
            {
                try
                {
                    if (isDefinedWithWebBeans)
                    {
                        Object interceptorProxy = BeanManagerImpl.getManager().getInstance(webBeansInterceptor);
                        WebBeansInterceptor<?> interceptor = (WebBeansInterceptor<?>) webBeansInterceptor;
                        interceptor.setInjections(interceptorProxy);

                        //Setting interceptor proxy instance
                        intData.setInterceptorInstance(interceptorProxy);
                    }
                    else
                    {
                        if (ClassUtil.isContaintNoArgConstructor(clazz) == null)
                        {
                            throw new WebBeansConfigurationException("Interceptor class : " + clazz.getName() + " must have no-arg constructor");
                        }

                        intData.setInterceptorInstance(clazz.newInstance());
                    }

                }
                catch (WebBeansConfigurationException e1)
                {
                    throw e1;
                }
                catch (Exception e)
                {
                    throw new WebBeansException(e);
                }
            }

            intData.setInterceptor(method, annotation);

            stack.add(intData);
        }
    }

    /**
     * Returns true if interceptor stack contains interceptor with given type.
     * 
     * @param stack interceptor stack
     * @param type interceptor type
     * @return true if stack contains the interceptor with given type
     */
    public static boolean isContainsInterceptorMethod(List<InterceptorData> stack, InterceptorType type)
    {
        Iterator<InterceptorData> it = stack.iterator();
        while (it.hasNext())
        {
            Method m = null;
            InterceptorData data = it.next();

            if (type.equals(InterceptorType.AROUND_INVOKE))
            {
                m = data.getAroundInvoke();
            }
            else if (type.equals(InterceptorType.POST_CONSTRUCT))
            {
                m = data.getPostConstruct();

            }
            else if (type.equals(InterceptorType.PRE_DESTROY))
            {
                m = data.getPreDestroy();
            }

            if (m != null)
            {
                return true;
            }

        }

        return false;
    }

    /**
     * Gets list of interceptors with the given type.
     * 
     * @param stack interceptor stack
     * @param type interceptor type
     * @return list of interceptor
     */
    @SuppressWarnings("unchecked")
    public static List<InterceptorData> getInterceptorMethods(List<InterceptorData> stack, InterceptorType type)
    {
        List<InterceptorData> ai = new ArrayList<InterceptorData>();
        List<InterceptorData> pc = new ArrayList<InterceptorData>();
        List<InterceptorData> pd = new ArrayList<InterceptorData>();

        Iterator<InterceptorData> it = stack.iterator();
        while (it.hasNext())
        {
            Method m = null;
            InterceptorData data = it.next();

            if (type.equals(InterceptorType.AROUND_INVOKE))
            {
                m = data.getAroundInvoke();
                if (m != null)
                {
                    ai.add(data);
                }

            }
            else if (type.equals(InterceptorType.POST_CONSTRUCT))
            {
                m = data.getPostConstruct();
                if (m != null)
                {
                    pc.add(data);
                }

            }
            else if (type.equals(InterceptorType.PRE_DESTROY))
            {
                m = data.getPreDestroy();
                if (m != null)
                {
                    pd.add(data);
                }

            }

        }

        if (type.equals(InterceptorType.AROUND_INVOKE))
        {
            return ai;
        }
        else if (type.equals(InterceptorType.POST_CONSTRUCT))
        {
            return pc;

        }
        else if (type.equals(InterceptorType.PRE_DESTROY))
        {
            return pd;
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Returns true if array contains the StereoType meta annotation
     * 
     * @param anns annotation array
     * @return true if array contains the StereoType meta annotation
     */
    public static boolean isComponentHasStereoType(BaseBean<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");

        Set<Annotation> set = component.getOwbStereotypes();
        Annotation[] anns = new Annotation[set.size()];
        anns = set.toArray(anns);
        if (AnnotationUtil.isStereoTypeMetaAnnotationExist(anns))
        {
            return true;
        }

        return false;
    }

    public static Annotation[] getComponentStereoTypes(BaseBean<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        if (isComponentHasStereoType(component))
        {
            Set<Annotation> set = component.getOwbStereotypes();
            Annotation[] anns = new Annotation[set.size()];
            anns = set.toArray(anns);

            return AnnotationUtil.getStereotypeMetaAnnotations(anns);
        }

        return new Annotation[] {};
    }

    public static boolean isNamedExistOnStereoTypes(BaseBean<?> component)
    {
        Annotation[] types = getComponentStereoTypes(component);

        for (Annotation ann : types)
        {
            if (AnnotationUtil.isAnnotationExistOnClass(ann.annotationType(), Named.class))
            {
                return true;
            }
        }

        return false;
    }

    public static Annotation getMaxPrecedenceSteroTypeDeploymentType(BaseBean<?> component)
    {
        Annotation[] deploymentTypes = getComponentStereoTypes(component);
        Class<? extends Annotation> maxPrecedDeploymentType = null;
        Annotation result = null;
        if (deploymentTypes.length > 0)
        {
            for (Annotation dt : deploymentTypes)
            {
                if (AnnotationUtil.isMetaAnnotationExist(dt.annotationType().getDeclaredAnnotations(), DeploymentType.class))
                {
                    Annotation result2[] = AnnotationUtil.getMetaAnnotations(dt.annotationType().getDeclaredAnnotations(), DeploymentType.class);

                    Class<? extends Annotation> dtAnnot = result2[0].annotationType();
                    if (maxPrecedDeploymentType == null)
                    {
                        maxPrecedDeploymentType = dtAnnot;
                        result = result2[0];
                    }
                    else
                    {
                        if (DeploymentTypeManager.getInstance().comparePrecedences(maxPrecedDeploymentType, dtAnnot) < 0)
                        {
                            maxPrecedDeploymentType = dtAnnot;
                            result = result2[0];
                        }
                    }
                }

            }
        }

 
        return result;
    }

    public static String getSimpleWebBeanDefaultName(String clazzName)
    {
        Asserts.assertNotNull(clazzName);
        
        if(clazzName.length() > 0)
        {
            StringBuffer name = new StringBuffer(clazzName);
            name.setCharAt(0, Character.toLowerCase(name.charAt(0)));

            return name.toString();            
        }
        
        return clazzName;
    }

    public static String getProducerDefaultName(String methodName)
    {
        StringBuffer buffer = new StringBuffer(methodName);
            
        if (buffer.length() > 3 &&  (buffer.substring(0, 3).equals("get") || buffer.substring(0, 3).equals("set")))                
        {
            
            if(Character.isUpperCase(buffer.charAt(3)))
            {
                buffer.setCharAt(3, Character.toLowerCase(buffer.charAt(3)));   
            }

            return buffer.substring(3);
        }
        else if ((buffer.length() > 2 &&  buffer.substring(0, 2).equals("is")))                
        {            
            if(Character.isUpperCase(buffer.charAt(2)))
            {
                buffer.setCharAt(2, Character.toLowerCase(buffer.charAt(2)));   
            }

            return buffer.substring(2);
        }
        
        else
        {
            buffer.setCharAt(0, Character.toLowerCase(buffer.charAt(0)));
            return buffer.toString();
        }
    }

    public static void checkStereoTypeClass(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        Annotation[] annotations = clazz.getDeclaredAnnotations();

        boolean deploymentTypeFound = false;
        boolean scopeTypeFound = false;
        for (Annotation annotation : annotations)
        {
            Class<? extends Annotation> annotType = annotation.annotationType();

            if (annotType.isAnnotationPresent(DeploymentType.class))
            {
                if (deploymentTypeFound == true)
                {
                    throw new WebBeansConfigurationException("@StereoType annotation can not contain more than one @DeploymentType annotation");
                }
                else
                {
                    deploymentTypeFound = true;
                }
            }
            else if (annotType.isAnnotationPresent(ScopeType.class))
            {
                if (scopeTypeFound == true)
                {
                    throw new WebBeansConfigurationException("@StereoType annotation can not contain more than one @ScopeType annotation");
                }
                else
                {
                    scopeTypeFound = true;
                }
            }
            else if (annotType.equals(Named.class))
            {
                Named name = (Named) annotation;
                if (!name.value().equals(""))
                {
                    throw new WebBeansConfigurationException("@StereoType annotation can not define @Named annotation with value");
                }
            }
            else if (AnnotationUtil.isBindingAnnotation(annotType))
            {
                throw new WebBeansConfigurationException("@StereoType annotation can not define @BindingType annotation");
            }
            else if (AnnotationUtil.isInterceptorBindingAnnotation(annotType))
            {
                Target target = clazz.getAnnotation(Target.class);
                ElementType[] type = target.value();

                if (type.length != 1 && !type[0].equals(ElementType.TYPE))
                {
                    throw new WebBeansConfigurationException("Stereotype with @InterceptorBindingType must be defined as @Target{TYPE}");
                }

            }
        }
    }

    /**
     * Configures the bean specializations.
     * <p>
     * Specialized beans inherit the <code>name</code> property
     * from their parents. Specialized bean deployment priority
     * must be higher than its super class related bean.
     * </p>
     * 
     * @param specializedClass specialized class
     * @throws DefinitionException if name is defined
     * @throws InconsistentSpecializationException related with priority
     * @throws WebBeansConfigurationException any other exception
     */
    public static void configureSpecializations(Class<?> specializedClass)
    {
        Asserts.nullCheckForClass(specializedClass);

        Bean<?> superBean = null;
        Bean<?> specialized = null;
        Set<Bean<?>> resolvers = null;
        
        if ((resolvers = isConfiguredWebBeans(specializedClass,true)) != null)
        {            
            if(resolvers.isEmpty())
            {
                throw new InconsistentSpecializationException("Specialized bean for class : " + specializedClass + " is not enabled in the deployment.");
            }
            
            if(resolvers.size() > 1)
            {
                throw new InconsistentSpecializationException("More than one specialized bean for class : " + specializedClass + " is enabled in the deployment.");
            }
            
                                   
            specialized = resolvers.iterator().next();
            
            Class<?> superClass = specializedClass.getSuperclass();
            
            resolvers = isConfiguredWebBeans(superClass,false);
            
            for(Bean<?> candidates : resolvers)
            {
                AbstractBean<?> candidate = (AbstractBean<?>)candidates;
                
                if(!(candidate instanceof NewBean))
                {
                    if(candidate.getReturnType().equals(superClass))
                    {
                        superBean = candidates;
                        break;
                    }                    
                }                
            }
                        
            if (superBean != null)
            {
                int res = DeploymentTypeManager.getInstance().comparePrecedences(specialized.getDeploymentType(), superBean.getDeploymentType());
                if (res <= 0)
                {
                    throw new InconsistentSpecializationException("@Specializes exception. Class : " + specializedClass.getName() + " must have higher deployment type precedence from the class : " + superClass.getName());
                }
                
                AbstractBean<?> comp = (AbstractBean<?>)specialized;

                if(superBean.getName() != null)
                {
                    if(comp.getName() != null)
                    {
                        throw new DefinitionException("@Specialized Class : " + specializedClass.getName() + " may not explicitly declare a bean name");
                    }                    
                    
                    comp.setName(superBean.getName());
                    comp.setSpecializedBean(true);
                }
                                
                specialized.getBindings().addAll(superBean.getBindings());
            }
            
            else
            {
                throw new InconsistentSpecializationException("WebBean component class : " + specializedClass.getName() + " is not enabled for specialized by the " + specializedClass + " class");
            }
        }

    }

    public static Set<Bean<?>> isConfiguredWebBeans(Class<?> clazz,boolean annotate)
    {   
        Asserts.nullCheckForClass(clazz);
        
        Set<Bean<?>> beans = new HashSet<Bean<?>>();
        
        Set<Bean<?>> components = BeanManagerImpl.getManager().getComponents();
        Iterator<Bean<?>> it = components.iterator();

        while (it.hasNext())
        {
            AbstractBean<?> bean = (AbstractBean<?>)it.next();
            
            if (bean.getTypes().contains(clazz))
            {
                if(annotate)
                {
                    if(bean.getReturnType().isAnnotationPresent(Specializes.class))
                    {
                        if(!(bean instanceof NewBean))
                        {
                            if(DeploymentTypeManager.getInstance().isDeploymentTypeEnabled(bean.getDeploymentType()))
                            {
                                beans.add(bean);    
                            }                            
                        }                           
                    }                                    
                }
                else
                {
                    if(DeploymentTypeManager.getInstance().isDeploymentTypeEnabled(bean.getDeploymentType()))
                    {
                        beans.add(bean);   
                    }
                }
            }
        }

        return beans;
    }
    
    /**
     * Stereotype runtime requirements are dropped from the specification.
     * 
     * @param component
     * @param anns
     * @param errorMessage
     * @deprecated
     */
    public static void checkSteroTypeRequirements(BaseBean<?> component, Annotation[] anns, String errorMessage)
    {
        Set<Class<? extends Annotation>> allSupportedScopes = new HashSet<Class<? extends Annotation>>();
        Annotation[] stereoTypes = getComponentStereoTypes(component);        
        for (Annotation stereoType : stereoTypes)
        {
            IStereoTypeModel model = StereoTypeManager.getInstance().getStereoTypeModel(stereoType.annotationType().getName());
            Set<Class<?>> rtypes = model.getRestrictedTypes();

            if (rtypes != null)
            {
                Iterator<Class<?>> itTypes = rtypes.iterator();
                while (itTypes.hasNext())
                {
                    if (!component.getTypes().contains(itTypes.next()))
                    {
                        throw new WebBeansConfigurationException(errorMessage + " must contains all supported api types in the @Stereotype annotation " + model.getName());

                    }
                }
            }

            Set<Class<? extends Annotation>> suppScopes = model.getSupportedScopes();
            if (suppScopes != null)
            {
                if (!suppScopes.isEmpty())
                {
                    allSupportedScopes.addAll(suppScopes);
                }
            }
        }
        
        if(allSupportedScopes.size() > 0)
        {
            if (!allSupportedScopes.contains(component.getScopeType()))
            {
                throw new WebBeansConfigurationException(errorMessage + " must contains at least one required scope types in its @Stereotype annotations");
            }            
        }        

    }

    public static void checkUnproxiableApiType(Bean<?> bean, ScopeType scopeType)
    {
        Asserts.assertNotNull("bean", "bean parameter can not be null");
        Asserts.assertNotNull(scopeType, "scopeType parameter can not be null");

        Set<Type> types = bean.getTypes();
        Class<?> superClass = null;
        for (Type t : types)
        {
            Class<?> type = ClassUtil.getClazz(t);
            
            if (!type.isInterface())
            {
                if ((superClass == null) || (superClass.isAssignableFrom(type) && type != Object.class))
                {
                    superClass = type;
                }

            }
        }

        if (superClass != null && !superClass.equals(Object.class))
        {
            Constructor<?> cons = ClassUtil.isContaintNoArgConstructor(superClass);

            if (ClassUtil.isPrimitive(superClass) || ClassUtil.isArray(superClass) || ClassUtil.isFinal(superClass.getModifiers()) || ClassUtil.hasFinalMethod(superClass) || (cons == null || ClassUtil.isPrivate(cons.getModifiers())))
            {
                if (scopeType.normal())
                {
                    throw new UnproxyableResolutionException("WebBeans with api type with normal scope must be proxiable to inject, but class : " + superClass.getName() + " is not proxiable type");
                }
            }

        }

    }

    public static void checkNullable(Class<?> type, AbstractBean<?> component)
    {
        Asserts.assertNotNull(type, "type parameter can not be null");
        Asserts.assertNotNull(component, "component parameter can not be null");

        if (type.isPrimitive())
        {
            if (component.isNullable())
            {
                throw new NullableDependencyException("Injection point for primitive type resolves webbeans component with return type : " + component.getReturnType().getName() + " with nullable");
            }
        }
    }

    /**
     * Configures the producer method specialization.
     * 
     * @param component producer method component
     * @param method specialized producer method
     * @param superClass bean super class that has overriden method
     * @throws DefinitionException if the name is exist on the producer method when
     *         parent also has name
     * @throws WebBeansConfigurationException any other exceptions
     */
    public static void configureProducerSpecialization(AbstractBean<?> component, Method method, Class<?> superClass)
    {
        Method superMethod = ClassUtil.getClassMethodWithTypes(superClass, method.getName(), Arrays.asList(method.getParameterTypes()));
        if (superMethod == null)
        {
            throw new WebBeansConfigurationException("Producer method specialization is failed. Method " + method.getName() + " not found in super class : " + superClass.getName());
        }
        
        if (!AnnotationUtil.isAnnotationExist(superMethod.getAnnotations(), Produces.class))
        {
            throw new WebBeansConfigurationException("Producer method specialization is failed. Method " + method.getName() + " found in super class : " + superClass.getName() + " is not annotated with @Produces");
        }

        Annotation[] anns = AnnotationUtil.getBindingAnnotations(superMethod.getAnnotations());

        for (Annotation ann : anns)
        {
            component.addBindingType(ann);
        }
        
        configuredProducerSpecializedName(component, method, superMethod);
        
        component.setSpecializedBean(true);
        
    }
    
    /**
     * Configures the name of the producer method for specializing the parent.
     * 
     * @param component producer method component
     * @param method specialized producer method
     * @param superMethod overriden super producer method
     */
    public static void configuredProducerSpecializedName(AbstractBean<?> component,Method method,Method superMethod)
    {
        Asserts.assertNotNull(component,"component parameter can not be null");
        Asserts.assertNotNull(method,"method parameter can not be null");
        Asserts.assertNotNull(superMethod,"superMethod parameter can not be null");
        
        String name = null;
        boolean hasName = false;
        if(AnnotationUtil.isMethodHasAnnotation(superMethod, Named.class))
        {
          Named named =  superMethod.getAnnotation(Named.class);
          hasName = true;
          if(!named.value().equals(""))
          {
              name = named.value();
          }
          else
          {
              name = getProducerDefaultName(superMethod.getName());
          }
        }
        else 
        {
            Annotation[] anns = AnnotationUtil.getStereotypeMetaAnnotations(superMethod.getAnnotations());
            for(Annotation ann : anns)
            {
                if(ann.annotationType().isAnnotationPresent(Stereotype.class))
                {
                    hasName = true;
                    name = getProducerDefaultName(superMethod.getName());
                    break;
                }
            }                        
        }
        
        if(hasName)
        {
            if(AnnotationUtil.isMethodHasAnnotation(method, Named.class))
            {
                throw new DefinitionException("Specialized method : " + method.getName() + " in class : " + component.getReturnType().getName() + " may not define @Named annotation");
            }
            
            component.setName(name);
        }
        
//        else
//        {
//            component.setName(name);
//        }
        
    }
    
    public static void checkInjectedMethodParameterConditions(Method method, Class<?> clazz)
    {
        Asserts.assertNotNull(method, "method parameter can not be null");
        Asserts.nullCheckForClass(clazz);

        if (AnnotationUtil.isMethodParameterAnnotationExist(method, Disposes.class) || AnnotationUtil.isMethodParameterAnnotationExist(method, Observes.class))
        {
            throw new WebBeansConfigurationException("Initializer method parameters in method : " + method.getName() + " in class : " + clazz.getName() + " can not be annotated with @Disposes or @Observers");

        }

    }

    public static void checkInterceptorResolverParams(Annotation... interceptorBindingTypes)
    {
        if (interceptorBindingTypes == null || interceptorBindingTypes.length == 0)
        {
            throw new IllegalArgumentException("Manager.resolveInterceptors() method parameter interceptor binding types array argument can not be empty");
        }

        Annotation old = null;
        for (Annotation interceptorBindingType : interceptorBindingTypes)
        {
            if (old == null)
            {
                old = interceptorBindingType;
            }
            else
            {
                if (old.equals(interceptorBindingType))
                {
                    throw new IllegalArgumentException("Manager.resolveInterceptors() method parameter interceptor binding types array argument can not define duplicate binding annotation with name : @" + old.getClass().getName());
                }

                if (!AnnotationUtil.isInterceptorBindingAnnotation(interceptorBindingType.annotationType()))
                {
                    throw new IllegalArgumentException("Manager.resolveInterceptors() method parameter interceptor binding types array can not contain other annotation that is not @InterceptorBindingType");
                }

                old = interceptorBindingType;
            }
        }
    }

    public static void checkDecoratorResolverParams(Set<Type> apiTypes, Annotation... bindingTypes)
    {
        if (apiTypes == null || apiTypes.size() == 0)
        {
            throw new IllegalArgumentException("Manager.resolveDecorators() method parameter api types argument can not be empty");
        }

        Annotation old = null;
        for (Annotation bindingType : bindingTypes)
        {
            if (old == null)
            {
                old = bindingType;
            }
            else
            {
                if (old.annotationType().equals(bindingType.annotationType()))
                {
                    throw new IllegalArgumentException("Manager.resolveDecorators() method parameter binding types array argument can not define duplicate binding annotation with name : @" + old.annotationType().getName());
                }

                if (!AnnotationUtil.isBindingAnnotation(bindingType.annotationType()))
                {
                    throw new IllegalArgumentException("Manager.resolveDecorators() method parameter binding types array can not contain other annotation that is not @BindingType");
                }

                old = bindingType;
            }
        }

    }
    
    /**
     * Returns true if instance injection point false otherwise.
     * 
     * @param injectionPoint injection point definition
     * @return true if instance injection point
     */
    public static boolean checkObtainsInjectionPointConditions(InjectionPoint injectionPoint)
    {        
        Type type = injectionPoint.getType();
        
        Class<?> candidateClazz = null;
        if(type instanceof Class)
        {
            candidateClazz = (Class<?>)type;
        }
        else if(type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType)type;
            candidateClazz = (Class<?>)pt.getRawType();
        }
        
        if(!candidateClazz.equals(Instance.class))
        {
            return false;
        }        
        
        Class<?> rawType = null;
        
        if(ClassUtil.isParametrizedType(injectionPoint.getType()))
        {
            ParameterizedType pt = (ParameterizedType)injectionPoint.getType();
            
            rawType = (Class<?>) pt.getRawType();
            
            Type[] typeArgs = pt.getActualTypeArguments();
            
            if(!(rawType.equals(Instance.class)))
            {
                throw new WebBeansConfigurationException("<Instance> field injection " + injectionPoint.toString() + " must have type javax.inject.Instance");
            }                
            else
            {                                        
                if(typeArgs.length == 1)
                {
                    Type actualArgument = typeArgs[0];
                    
                    if(ClassUtil.isParametrizedType(actualArgument) || ClassUtil.isWildCardType(actualArgument) || ClassUtil.isTypeVariable(actualArgument))
                    {                            
                        throw new WebBeansConfigurationException("<Instance> field injection " + injectionPoint.toString() + " actual type argument can not be Parametrized, Wildcard type or Type variable");                            
                    }
                                            
                    if(ClassUtil.isDefinitionConstainsTypeVariables((Class<?>)actualArgument))
                    {
                        throw new WebBeansConfigurationException("<Instance> field injection " + injectionPoint.toString() + " must not have TypeVariable or WildCard generic type argument");                            
                    }
                }
                else
                {
                    throw new WebBeansConfigurationException("<Instance> field injection " + injectionPoint.toString() + " must not have more than one actual type argument");
                }
            }                                
        }
        else
        {
            throw new WebBeansConfigurationException("<Instance> field injection " + injectionPoint.toString() + " must be defined as ParameterizedType with one actual type argument");
        }  
        
        return true;
    }

    public static <T> void checkPassivationScope(AbstractBean<T> component, ScopeType scope)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        Asserts.assertNotNull(scope, "scope type parameter can not be null");

        boolean passivating = scope.passivating();
        Class<T> clazz = component.getReturnType();

        if (passivating)
        {
            if (!component.isSerializable())
            {
                throw new WebBeansConfigurationException("WebBeans component implementation class : " + clazz.getName() + " with passivating scope @" + scope.annotationType().getName() + " must be Serializable");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void defineInterceptors(Class<T> clazz)
    {
        if (InterceptorsManager.getInstance().isInterceptorEnabled(clazz))
        {
            ManagedBean<T> component = null;

            InterceptorUtil.checkInterceptorConditions(clazz);
            component = ManagedBeanConfigurator.define(clazz, WebBeansType.INTERCEPTOR);

            if (component != null)
            {
                WebBeansInterceptorConfig.configureInterceptorClass((ManagedBean<Object>) component, clazz.getDeclaredAnnotations());
            }
        }

    }

    @SuppressWarnings("unchecked")
    public static <T> void defineDecorators(Class<T> clazz)
    {
        if (DecoratorsManager.getInstance().isDecoratorEnabled(clazz))
        {
            ManagedBean<T> component = null;

            DecoratorUtil.checkDecoratorConditions(clazz);
            component = ManagedBeanConfigurator.define(clazz, WebBeansType.DECORATOR);

            if (component != null)
            {
                WebBeansDecoratorConfig.configureDecoratorClass((ManagedBean<Object>) component);
            }
        }
    }

    public static boolean isScopeTypeNormal(Class<? extends Annotation> scopeType)
    {
        Asserts.assertNotNull(scopeType, "scopeType argument can not be null");

        if (scopeType.isAnnotationPresent(ScopeType.class))
        {
            ScopeType scope = scopeType.getAnnotation(ScopeType.class);
            if (scope.normal())
            {
                return true;
            }

            else
            {
                return false;
            }
        }
        else
        {
            throw new IllegalArgumentException("scopeType argument must be annotated with @ScopeType");
        }
    }
    
    public static void checkNullInstance(Object instance,Class<?> scopeType, String errorMessage)
    {
        if (instance == null)
        {
            if (!scopeType.equals(Dependent.class))
            {
                throw new IllegalProductException(errorMessage);
            }
        }
        
    }
    
    public static void checkSerializableScopeType(Class<?> scopeType, boolean isSerializable, String errorMessage)
    {
        // Scope type check
        ScopeType scope = scopeType.getAnnotation(ScopeType.class);
        if (scope.passivating())
        {
            if (!isSerializable)
            {
                throw new IllegalProductException(errorMessage);
            }
        }
    }
    
    public static boolean isSimpleWebBeans(AbstractBean<?> component)
    {
        if(component.getWebBeansType().equals(WebBeansType.MANAGED) ||
                component.getWebBeansType().equals(WebBeansType.INTERCEPTOR) ||
                component.getWebBeansType().equals(WebBeansType.DECORATOR))
        {
            return true;
        }
        
        return false;
    }
    
    public static void addInjectedImplicitEventComponent(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        if(!(type instanceof ParameterizedType))
        {
            return;
        }
        
        Type[] args = new Type[0];
        
        Class<?> clazz = null;
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;
            args = pt.getActualTypeArguments();
        }
        
        clazz = (Class<?>)args[0];
        
        Annotation[] bindings = new Annotation[injectionPoint.getBindings().size()];
        bindings = injectionPoint.getBindings().toArray(bindings);
        
        Bean<?> bean = createObservableImplicitComponent(EventImpl.class, clazz, bindings);
        BeanManagerImpl.getManager().addBean(bean);                  
    }
    
    @SuppressWarnings("unchecked")
    public static <T> void addInjectedImplicitInstanceComponent(InjectionPoint injectionPoint)
    {
        ParameterizedType genericType = (ParameterizedType)injectionPoint.getType();
        
        Class<Instance<T>> clazz = (Class<Instance<T>>)genericType.getRawType();
        
        Annotation[] bindings = new Annotation[injectionPoint.getBindings().size()];
        bindings = injectionPoint.getBindings().toArray(bindings);
        
        Bean<Instance<T>> bean = createInstanceComponent(genericType,clazz, genericType.getActualTypeArguments()[0], bindings);
        BeanManagerImpl.getManager().addBean(bean);
        
    }
    
    public static Bean<?> getMostSpecializedBean(BeanManager manager, Bean<?> component)
    {
        Set<Bean<?>> beans = manager.getBeans(component.getBeanClass(), AnnotationUtil.getAnnotationsFromSet(component.getBindings()));
                
        for(Bean<?> bean : beans)
        {
            Bean<?> find = bean;
            
            if(!find.equals(component))
            {
                if(AnnotationUtil.isAnnotationExistOnClass(find.getBeanClass(), Specializes.class))
                {
                    return getMostSpecializedBean(manager, find);
                }                
            }            
        }
        
        return component;
    }      
    
    public static boolean isDeploymentTypeEnabled(Class<? extends Annotation> deploymentType)
    {
        Asserts.assertNotNull(deploymentType, "deplymentType parameter can not be null");
        
        if (!DeploymentTypeManager.getInstance().isDeploymentTypeEnabled(deploymentType))
        {
            return false;
        }
        
        return true;
        
    }
 }