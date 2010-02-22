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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.decorator.Decorator;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.UnproxyableResolutionException;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Scope;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.ApplicationScopeLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.annotation.NewLiteral;
import org.apache.webbeans.annotation.RequestedScopeLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.BeanManagerBean;
import org.apache.webbeans.component.ConversationBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.EventBean;
import org.apache.webbeans.component.ExtensionBean;
import org.apache.webbeans.component.InjectionPointBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.InstanceBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.NewBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.creation.ManagedBeanCreatorImpl;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.EJBWebBeansConfigurator;
import org.apache.webbeans.config.ManagedBeanConfigurator;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.ExternalScope;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.apache.webbeans.exception.inject.InconsistentSpecializationException;
import org.apache.webbeans.exception.inject.NullableDependencyException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.intercept.InterceptorDataImpl;
import org.apache.webbeans.intercept.InterceptorType;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.OpenWebBeansPlugin;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.creation.InjectionTargetProducer;
import org.apache.webbeans.portable.events.ProcessBeanImpl;
import org.apache.webbeans.portable.events.ProcessInjectionTargetImpl;
import org.apache.webbeans.portable.events.ProcessProducerImpl;
import org.apache.webbeans.portable.events.discovery.ErrorStack;
import org.apache.webbeans.portable.events.generics.GProcessAnnotatedType;
import org.apache.webbeans.portable.events.generics.GProcessBean;
import org.apache.webbeans.portable.events.generics.GProcessInjectionTarget;
import org.apache.webbeans.portable.events.generics.GProcessManagedBean;
import org.apache.webbeans.portable.events.generics.GProcessObservableMethod;
import org.apache.webbeans.portable.events.generics.GProcessProducer;
import org.apache.webbeans.portable.events.generics.GProcessProducerField;
import org.apache.webbeans.portable.events.generics.GProcessProducerMethod;
import org.apache.webbeans.portable.events.generics.GProcessSessionBean;

/**
 * Contains some utility methods used in the all project.
 * 
 * @version $Rev$ $Date$ 
 */
@SuppressWarnings("unchecked")
public final class WebBeansUtil
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansUtil.class);
    
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
            if(!bean.getScope().equals(Dependent.class))
            {
                throw new WebBeansConfigurationException("Generic type may only defined with scope @Dependent for ManagedBean class : " + clazz.getName());
            }
        }
    }
    
    
    /**
     * Check producer method/field bean return type. 
     * @param bean producer bean instance
     * @param member related member instance
     */
    public static void checkProducerGenericType(Bean<?> bean,Member member)
    {
    	Asserts.assertNotNull(bean,"Bean is null");
    	
    	Type type = null;
    	
    	if(bean instanceof ProducerMethodBean)
    	{
    		type = ((ProducerMethodBean<?>)bean).getCreatorMethod().getGenericReturnType();
    	}
    	else if(bean instanceof ProducerFieldBean)
    	{
    		type = ((ProducerFieldBean<?>)bean).getCreatorField().getGenericType();
    	}
    	else
    	{
    		throw new IllegalArgumentException("Bean must be Producer Field or Method Bean instance : " + bean);
    	}
    	
    	String message = "Producer Field/Method Bean with name : " + member.getName() + " in bean class : " + member.getDeclaringClass().getName(); 
    	
    	if(checkGenericForProducers(type, message))
    	{
            if(!bean.getScope().equals(Dependent.class))
            {
                throw new WebBeansConfigurationException(message + " scope must bee @Dependent");
            }
    	}
    }
    
    /**
     * Check generic types for producer method and fields.
     * @param type generic return type
     * @param message error message
     * @return true if parametrized type argument is TypeVariable 
     */
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
     * Return <code>true</code> if the given class is ok for manage bean conditions,
     * <code>false</code> otherwise.
     * 
     * @param clazz class in hand
     * @return <code>true</code> if the given class is ok for simple web bean conditions.
     */
    public static void isManagedBeanClass(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz, "Class is null");
        
        int modifier = clazz.getModifiers();

        if (!ClassUtil.isStatic(modifier) && ClassUtil.isInnerClazz(clazz))
            throw new WebBeansConfigurationException("Bean implementation class : " + clazz.getName() + " can not be non-static inner class");

        if (!ClassUtil.isConcrete(clazz) && !AnnotationUtil.hasClassAnnotation(clazz, Decorator.class))
            throw new WebBeansConfigurationException("Bean implementation class : " + clazz.getName() + " have to be concrete if not defines as @Decorator");
                
        if (!isConstructureOk(clazz))
        {
            throw new WebBeansConfigurationException("Bean implementation class : " + clazz.getName() + " must define at least one Constructor");   
        }
        
        if(Extension.class.isAssignableFrom(clazz))
        {
            throw new WebBeansConfigurationException("Bean implementation class can not implement javax.enterprise.inject.spi.Extension.!");
        }
            
        // and finally call all checks which are defined in plugins like JSF, JPA, etc
        List<OpenWebBeansPlugin> plugins = PluginLoader.getInstance().getPlugins();
        for (OpenWebBeansPlugin plugin : plugins)
        {
            plugin.isManagedBean(clazz);
        }
    }

    /**
     * Defines applicable constructor.
     * @param <T> type info
     * @param clazz class type
     * @return constructor
     * @throws WebBeansConfigurationException any configuration exception
     */
    public static <T> Constructor<T> defineConstructor(Class<T> clazz) throws WebBeansConfigurationException
    {
        Asserts.nullCheckForClass(clazz);
        Constructor<T>[] constructors = ClassUtil.getConstructors(clazz);
        
        return defineConstructor(constructors, clazz);
        
    }
    
    
    public static <T>  Constructor<T> defineConstructor(Constructor<T>[] constructors, Class<T> clazz)
    {
        Constructor<T> result = null;
        
        boolean inAnnotation = false;
        int j = 0;

        /* Check for @Initializer */
        for (Constructor<T> constructor : constructors)
        {
            j++;
            if (constructor.getAnnotation(Inject.class) != null)
            {
                if (inAnnotation == true)// duplicate @In
                {
                    throw new WebBeansConfigurationException("There are more than one Constructor with Initializer annotation in class " + clazz.getName());
                }
                inAnnotation = true;
                result = constructor;
            }
        }
        
        if (result == null)
        {
            result = ClassUtil.isContaintNoArgConstructor(clazz);
            
            if(result == null)
            {
                throw new WebBeansConfigurationException("No constructor is found for the class : " + clazz.getName());
            }
        }
        
        
        Annotation[][] parameterAnns = result.getParameterAnnotations();
        for (Annotation[] parameters : parameterAnns)
        {
            for (Annotation param : parameters)
            {
                if (param.annotationType().equals(Disposes.class))
                {
                    throw new WebBeansConfigurationException("Constructor parameter annotations can not contain @Disposes annotation in class : " + clazz.getName());
                }
                
                if(param.annotationType().equals(Observes.class))
                {
                    throw new WebBeansConfigurationException("Constructor parameter annotations can not contain @Observes annotation in class : " + clazz.getName());
                }
            }

        }

        return result;        
        
    }

    /**
     * Check that simple web beans class has compatible constructor. 
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
            if (constructor.getAnnotation(Inject.class) != null)
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

        if (AnnotationUtil.hasMethodAnnotation(method, Inject.class) || AnnotationUtil.hasMethodParameterAnnotation(method, Disposes.class) || AnnotationUtil.hasMethodParameterAnnotation(method, Observes.class))
        {
            throw new WebBeansConfigurationException("Producer Method Bean with name : " + method.getName() + " in bean class : " + parentImplClazzName + " can not be annotated with" + " @Initializer/@Destructor annotation or has a parameter annotated with @Disposes/@Observes");
        }
    }
    
    /**
     * CheckProducerMethodDisposal.
     * @param disposalMethod disposal method
     * @param definedBeanClassName bean class name 
     */
    public static void checkProducerMethodDisposal(Method disposalMethod, String definedBeanClassName)
    {
        if (AnnotationUtil.hasMethodMultipleParameterAnnotation(disposalMethod, Disposes.class))
        {
            throw new WebBeansConfigurationException("Disposal method : " + disposalMethod.getName() + " in class " + definedBeanClassName + " has multiple @Disposes annotation parameter");
        }

        if (AnnotationUtil.hasMethodAnnotation(disposalMethod, Inject.class) || AnnotationUtil.hasMethodParameterAnnotation(disposalMethod, Observes.class) || AnnotationUtil.hasMethodAnnotation(disposalMethod, Produces.class))
        {
            throw new WebBeansConfigurationException("Disposal method : " + disposalMethod.getName() + " in the class : " + definedBeanClassName + " can not be annotated with" + " @Initializer/@Destructor/@Produces annotation or has a parameter annotated with @Observes");
        }

    }

    /**
     * Check conditions for the new binding. 
     * @param annotations annotations
     * @return Annotation[] with all binding annotations
     * @throws WebBeansConfigurationException if New plus any other binding annotation is set         
     */
    public static Annotation[] checkForNewQualifierForDeployment(Type type, Class<?> clazz, String name, Annotation[] annotations)
    {
        Asserts.assertNotNull(type, "Type argument can not be null");
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(annotations, "Annotations argument can not be null");

        Annotation[] as = AnnotationUtil.getQualifierAnnotations(annotations);
        for (Annotation a : annotations)
        {
            if (a.annotationType().equals(New.class))
            {
                if (as.length > 1)
                {
                    throw new WebBeansConfigurationException("@New binding annotation can not have any binding annotation in class : " + clazz.getName() + " in field/method : " + name);
                }
            }
        }
        
        return as;
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
            return true;

        }
        else if (src.equals(SessionScoped.class))
        {
            if (target.equals(ConversationScoped.class) || target.equals(ApplicationScoped.class) || target.equals(SessionScoped.class))
            {
                return false;
            }
            return true;

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
        Asserts.nullCheckForClass(clazz);

        NewBean<T> comp = null;

        if (ManagedBeanConfigurator.isManagedBean(clazz))
        {
            comp = new NewBean<T>(clazz, WebBeansType.MANAGED);
            comp.setConstructor(WebBeansUtil.defineConstructor(clazz));
            DefinitionUtil.addConstructorInjectionPointMetaData(comp, comp.getConstructor());

            DefinitionUtil.defineInjectedFields(comp);
            DefinitionUtil.defineInjectedMethods(comp);
        }
        else if (EJBWebBeansConfigurator.isSessionBean(clazz))
        {
            comp = new NewBean<T>(clazz, WebBeansType.ENTERPRISE);
        }
        else
        {
            throw new WebBeansConfigurationException("@New annotation on type : " + clazz.getName() + " must defined as a simple or an enterprise web bean");
        }

        comp.setImplScopeType(new DependentScopeLiteral());
        comp.addQualifier(new NewLiteral(clazz));
        comp.setName(null);
        comp.addApiType(clazz);
        comp.addApiType(Object.class);

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
        Asserts.nullCheckForClass(clazz);

        ExtensionBean<T> comp = null;
        comp = new ExtensionBean<T>(clazz);
        comp.setEnabled(true);
        
        DefinitionUtil.defineApiTypes(comp, clazz);
        
        comp.setImplScopeType(new ApplicationScopeLiteral());
        comp.addQualifier(new DefaultLiteral());
        
        DefinitionUtil.defineObserverMethods(comp, clazz);

        return comp;
    }
    

    /**
     * Returns a new managed bean from given bean.
     * 
     * @param <T> bean type parameter
     * @param bean bean instance
     * @return the new bean from given managed bean
     */
    public static <T> NewBean<T> createNewBean(AbstractInjectionTargetBean<T> bean)
    {
        Asserts.assertNotNull(bean, "bean argument can not be null");

        NewBean<T> comp = null;

        comp = new NewBean<T>(bean.getReturnType(), WebBeansType.NEW);
        
        comp.getTypes().addAll(bean.getTypes());
        
        if(bean instanceof ManagedBean)
        {
            comp.setConstructor(((ManagedBean)bean).getConstructor());   
        }
        
        for(Field injectedField : bean.getInjectedFields())
        {
            comp.addInjectedField(injectedField);
        }
        
        for(Method injectedMethod : bean.getInjectedMethods())
        {
            comp.addInjectedMethod(injectedMethod);
        }
        
        List<InterceptorData> interceptorList = bean.getInterceptorStack();
        if(!interceptorList.isEmpty())
        {
            comp.getInterceptorStack().addAll(interceptorList);   
        }
        
        
        comp.setImplScopeType(new DependentScopeLiteral());
        comp.addQualifier(new NewLiteral(bean.getBeanClass()));
        comp.setName(null);
        
        Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
        for(InjectionPoint injectionPoint : injectionPoints)
        {
            comp.addInjectionPoint(injectionPoint);
        }        

        return comp;
    }    
    
//    /**
//     * Creates a new event bean instance.
//     * @param <T> type info
//     * @param returnType bean api type
//     * @param eventType event type
//     * @param annotations event binding annotations
//     * @return new event bean instance
//     */
//    public static <T> EventBean<T> createObservableImplicitComponent(Class<T> returnType, Type eventType, Annotation... annotations)
//    {
//        EventBean<T> component = new EventBean<T>(returnType, eventType, WebBeansType.OBSERVABLE);
//
//        DefinitionUtil.defineApiTypes(component, returnType);
//        DefinitionUtil.defineQualifiers(component, annotations);
//
//        component.setImplScopeType(new DependentScopeLiteral());                      
//
//        return component;
//    }

    /**
     * Creates a new manager bean instance.
     * @return new manager bean instance
     */
    public static BeanManagerBean getManagerBean()
    {
        BeanManagerBean managerComponent = new BeanManagerBean();

        managerComponent.setImplScopeType(new DependentScopeLiteral());
        managerComponent.addQualifier(new DefaultLiteral());
        managerComponent.addQualifier(new AnyLiteral());
        managerComponent.addApiType(BeanManager.class);
        managerComponent.addApiType(Object.class);

        return managerComponent;
    }
    
    /**
     * Creates a new instance bean.
     * @return new instance bean
     */
    @SuppressWarnings("serial")
    public static <T> InstanceBean<T> getInstanceBean()
    {
        InstanceBean<T> instanceBean = new InstanceBean<T>();
        
        instanceBean.getTypes().add(new TypeLiteral<Instance<?>>(){}.getRawType());
        instanceBean.getTypes().add(new TypeLiteral<Provider<?>>(){}.getRawType());
        instanceBean.addApiType(Object.class);
        
        instanceBean.addQualifier(new AnyLiteral());
        instanceBean.setImplScopeType(new DependentScopeLiteral());
        instanceBean.setName(null);
                
        return instanceBean;
    }
    
    /**
     * Creates a new event bean.
     * @return new event bean
     */
    @SuppressWarnings("serial")
    public static <T> EventBean<T> getEventBean()
    {
        EventBean<T> eventBean = new EventBean<T>();
        
        eventBean.getTypes().add(new TypeLiteral<Event<?>>(){}.getRawType());
        eventBean.addApiType(Object.class);
        
        eventBean.addQualifier(new AnyLiteral());
        eventBean.setImplScopeType(new DependentScopeLiteral());
        eventBean.setName(null);
                
        return eventBean;
    }
    

    /**
     * Returns new conversation bean instance.
     * @return new conversation bean
     */
    public static ConversationBean getConversationBean()
    {
        ConversationBean conversationComp = new ConversationBean();

        conversationComp.addApiType(Conversation.class);
        conversationComp.addApiType(ConversationImpl.class);
        conversationComp.addApiType(Object.class);
        conversationComp.setImplScopeType(new RequestedScopeLiteral());
        conversationComp.addQualifier(new DefaultLiteral());
        conversationComp.addQualifier(new AnyLiteral());
        conversationComp.setName("javax.enterprise.context.conversation");
        
        WebBeansDecoratorConfig.configureDecarotors(conversationComp);
        
        return conversationComp;
    }
    
    /**
     * Returns a new injected point bean instance.
     * @return new injected point bean
     */
    public static InjectionPointBean getInjectionPointBean()
    {
        return new InjectionPointBean();
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
        Asserts.nullCheckForClass(clazz);        

        Method[] methods = ClassUtil.getDeclaredMethods(clazz);
        Method result = null;
        boolean found = false;
        for (Method method : methods)
        {
            if (AnnotationUtil.hasMethodAnnotation(method, commonAnnotation))
            {
                if (ClassUtil.isMoreThanOneMethodWithName(method.getName(), clazz))
                {
                    continue;
                }

                if (found == true)
                {
                    throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotation is declared more than one method in the class : " + clazz.getName());
                }
                
                found = true;
                result = method;

                // Check method criterias
                if (ClassUtil.isMethodHasParameter(method))
                {
                    if (!invocationContext)
                    {
                        throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not take any formal arguments");   
                    }
                    // Check method criterias
                    Class<?>[] params = ClassUtil.getMethodParameterTypes(method);
                    if (params.length != 1 || !params[0].equals(InvocationContext.class))
                    {
                        throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not take any formal arguments other than InvocationContext");
                    }
                }
                else if(invocationContext)
                {
                    throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " must take a parameter with class type javax.interceptor.InvocationContext.");                        
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

        return result;
    }
    
    public static <T> Method checkCommonAnnotationCriterias(AnnotatedType<T> annotatedType, Class<? extends Annotation> commonAnnotation, boolean invocationContext)
    {
        Class<?> clazz = annotatedType.getJavaClass();        

        Method result = null;
        boolean found = false;
        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for(AnnotatedMethod<? super T> methodA : methods)
        {
            AnnotatedMethod<T> methodB = (AnnotatedMethod<T>)methodA;
            Method method = methodB.getJavaMember();
            if (method.isAnnotationPresent(commonAnnotation))
            {
                if (ClassUtil.isMoreThanOneMethodWithName(method.getName(), clazz))
                {
                    continue;
                }

                if (found == true)
                {
                    throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotation is declared more than one method in the class : " + clazz.getName());
                }
                found = true;
                result = method;

                // Check method criterias
                if (methodB.getParameters().isEmpty())
                {
                    if (!invocationContext)
                    {
                        throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not take any formal arguments");   
                    }
                    
                    List<AnnotatedParameter<T>> parameters = methodB.getParameters();
                    List<Class<?>> clazzParameters = new ArrayList<Class<?>>();
                    for(AnnotatedParameter<T> parameter : parameters)
                    {
                        clazzParameters.add(ClassUtil.getClazz(parameter.getBaseType()));
                    }
                    
                    Class<?>[] params = clazzParameters.toArray(new Class<?>[0]);
                    if (params.length != 1 || !params[0].equals(InvocationContext.class))
                    {
                        throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " can not take any formal arguments other than InvocationContext");
                    }
                }
                else if(invocationContext)
                {
                    throw new WebBeansConfigurationException("@" + commonAnnotation.getSimpleName() + " annotated method : " + method.getName() + " in class : " + clazz.getName() + " must take a parameter with class type javax.interceptor.InvocationContext.");                        
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
        Asserts.nullCheckForClass(clazz);

        Method[] methods = ClassUtil.getDeclaredMethods(clazz);
        Method result = null;
        boolean found = false;
        for (Method method : methods)
        {
            if (AnnotationUtil.hasMethodAnnotation(method, AroundInvoke.class))
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

        return result;
    }
    
    public static <T> Method checkAroundInvokeAnnotationCriterias(AnnotatedType<T> annotatedType)
    {
        Method result = null;
        boolean found = false;
        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for(AnnotatedMethod<? super T> methodA : methods)
        {
            AnnotatedMethod<T> method = (AnnotatedMethod<T>)methodA;
         
            if (method.isAnnotationPresent(AroundInvoke.class))
            {
                // Overriden methods
                if (ClassUtil.isMoreThanOneMethodWithName(method.getJavaMember().getName(), annotatedType.getJavaClass()))
                {
                    continue;
                }

                if (found == true)
                {
                    throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotation is declared more than one method in the class : " + annotatedType.getJavaClass().getName());
                }
                
                found = true;
                result = method.getJavaMember();

                List<AnnotatedParameter<T>> parameters = method.getParameters();
                List<Class<?>> clazzParameters = new ArrayList<Class<?>>();
                for(AnnotatedParameter<T> parameter : parameters)
                {
                    clazzParameters.add(ClassUtil.getClazz(parameter.getBaseType()));
                }
                
                Class<?>[] params = clazzParameters.toArray(new Class<?>[0]);
                
                if (params.length != 1 || !params[0].equals(InvocationContext.class))
                    throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotated method : " + method.getJavaMember().getName() + " in class : " + annotatedType.getJavaClass().getName() + " can not take any formal arguments other than InvocationContext");

                if (!ClassUtil.getReturnType(method.getJavaMember()).equals(Object.class))
                {
                    throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotated method : " + method.getJavaMember().getName()+ " in class : " + annotatedType.getJavaClass().getName() + " must return Object type");
                }

                if (!ClassUtil.isMethodHasException(method.getJavaMember()))
                {
                    throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotated method : " + method.getJavaMember().getName( )+ " in class : " + annotatedType.getJavaClass().getName() + " must throw Exception");
                }

                if (ClassUtil.isStatic(method.getJavaMember().getModifiers()) || ClassUtil.isFinal(method.getJavaMember().getModifiers()))
                {
                    throw new WebBeansConfigurationException("@" + AroundInvoke.class.getSimpleName() + " annotated method : " + method.getJavaMember().getName( )+ " in class : " + annotatedType.getJavaClass().getName() + " can not be static or final");
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
    public static void configureInterceptorMethods(Interceptor<?> webBeansInterceptor, Class<?> clazz, Class<? extends Annotation> annotation, 
                                                    boolean definedInInterceptorClass, boolean definedInMethod, List<InterceptorData> stack, 
                                                    Method annotatedInterceptorClassMethod, boolean isDefinedWithWebBeans)
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
            intData.setInterceptorBindingMethod(annotatedInterceptorClassMethod);
            intData.setWebBeansInterceptor(webBeansInterceptor);

            if (definedInInterceptorClass)
            {
                try
                {
                    if (!isDefinedWithWebBeans)
                    {
                        intData.setInterceptorInstance(newInstanceForced(clazz));
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

            intData.setInterceptorMethod(method, annotation);

            stack.add(intData);
        }
    }
    
    
    public static <T> void configureInterceptorMethods(Interceptor<?> webBeansInterceptor, AnnotatedType<T> annotatedType, Class<? extends Annotation> annotation, boolean definedInInterceptorClass, boolean definedInMethod, List<InterceptorData> stack, Method annotatedInterceptorClassMethod, boolean isDefinedWithWebBeans)
    {
        InterceptorData intData = null;
        Method method = null;

        if (annotation.equals(AroundInvoke.class))
        {
            method = WebBeansUtil.checkAroundInvokeAnnotationCriterias(annotatedType);
        }
        else if (annotation.equals(PostConstruct.class))
        {
            if (definedInInterceptorClass)
            {
                method = WebBeansUtil.checkCommonAnnotationCriterias(annotatedType, PostConstruct.class, true);
            }
            else
            {
                method = WebBeansUtil.checkCommonAnnotationCriterias(annotatedType, PostConstruct.class, false);
            }
        }
        else if (annotation.equals(PreDestroy.class))
        {
            if (definedInInterceptorClass)
            {
                method = WebBeansUtil.checkCommonAnnotationCriterias(annotatedType, PreDestroy.class, true);
            }
            else
            {
                method = WebBeansUtil.checkCommonAnnotationCriterias(annotatedType, PreDestroy.class, false);
            }
        }

        if (method != null)
        {
            intData = new InterceptorDataImpl(isDefinedWithWebBeans);
            intData.setDefinedInInterceptorClass(definedInInterceptorClass);
            intData.setDefinedInMethod(definedInMethod);
            intData.setInterceptorBindingMethod(annotatedInterceptorClassMethod);
            intData.setWebBeansInterceptor(webBeansInterceptor);

            if (definedInInterceptorClass)
            {
                try
                {
                    if (!isDefinedWithWebBeans)
                    {
                        intData.setInterceptorInstance(newInstanceForced(annotatedType.getJavaClass()));
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

            intData.setInterceptorMethod(method, annotation);

            stack.add(intData);
        }
    }
    

    /**
     * Create a new instance of the given class using it's default constructor
     * regardless if the constructor is visible or not.
     * This is needed to construct some package scope classes in the TCK.
     * 
     * @param <T>
     * @param clazz
     * @return
     * @throws WebBeansConfigurationException
     */
    public static <T> T newInstanceForced(Class<T> clazz) 
    throws WebBeansConfigurationException 
    {
        Constructor<T> ct = ClassUtil.isContaintNoArgConstructor(clazz);
        if (ct == null)
        {
            throw new WebBeansConfigurationException("class : " + clazz.getName() + " must have no-arg constructor");
        }

        if (!ct.isAccessible())
        {
            ct.setAccessible(true);
        }
        
        try 
        {
            return ct.newInstance();
        } 
        catch( IllegalArgumentException e )
        {
            throw new WebBeansConfigurationException("class : " + clazz.getName() + " is not constructable", e);
        } 
        catch( IllegalAccessException e ) 
        {
            throw new WebBeansConfigurationException("class : " + clazz.getName() + " is not constructable", e);
        } 
        catch( InvocationTargetException e ) 
        {
            throw new WebBeansConfigurationException("class : " + clazz.getName() + " is not constructable", e);
        } 
        catch( InstantiationException e ) 
        {
            throw new WebBeansConfigurationException("class : " + clazz.getName() + " is not constructable", e);
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
    public static boolean isComponentHasStereoType(OwbBean<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");

        Set<Annotation> set = component.getOwbStereotypes();
        Annotation[] anns = new Annotation[set.size()];
        anns = set.toArray(anns);
        if (AnnotationUtil.hasStereoTypeMetaAnnotation(anns))
        {
            return true;
        }

        return false;
    }

    /**
     * Returns bean stereotypes.
     * @param bean bean instance
     * @return bean stereotypes
     */
    public static Annotation[] getComponentStereoTypes(OwbBean<?> bean)
    {
        Asserts.assertNotNull(bean, "bean parameter can not be null");
        if (isComponentHasStereoType(bean))
        {
            Set<Annotation> set = bean.getOwbStereotypes();
            Annotation[] anns = new Annotation[set.size()];
            anns = set.toArray(anns);

            return AnnotationUtil.getStereotypeMetaAnnotations(anns);
        }

        return new Annotation[] {};
    }

    /**
     * Returns true if name exists,false otherwise.
     * @param bean bean instance
     * @return true if name exists
     */
    public static boolean hasNamedOnStereoTypes(OwbBean<?> bean)
    {
        Annotation[] types = getComponentStereoTypes(bean);
        
        for (Annotation ann : types)
        {
            if (AnnotationUtil.hasClassAnnotation(ann.annotationType(), Named.class))
            {
                return true;
            }
        }

        return false;
    }

    public static String getManagedBeanDefaultName(String clazzName)
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

    /**
     * Validates that given class obeys stereotype model
     * defined by the specification.
     * @param clazz stereotype class
     */
    public static void checkStereoTypeClass(Class<? extends Annotation> clazz)
    {
        checkStereoTypeClass(clazz, clazz.getDeclaredAnnotations());
    }
    
    /**
     * Validates that given class obeys stereotype model
     * defined by the specification.
     * @param clazz stereotype class
     */
    public static void checkStereoTypeClass(Class<? extends Annotation> clazz, Annotation...annotations)
    {
        Asserts.nullCheckForClass(clazz);

        boolean scopeTypeFound = false;
        for (Annotation annotation : annotations)
        {
            Class<? extends Annotation> annotType = annotation.annotationType();

            if (annotType.isAnnotationPresent(NormalScope.class) || annotType.isAnnotationPresent(Scope.class))
            {
                if (scopeTypeFound == true)
                {
                    throw new WebBeansConfigurationException("@StereoType annotation can not contain more than one @Scope/@NormalScope annotation");
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
            else if (AnnotationUtil.isQualifierAnnotation(annotType))
            {
                throw new WebBeansConfigurationException("@StereoType annotation can not define @Qualifier annotation");
            }
            else if (AnnotationUtil.isInterceptorBindingAnnotation(annotType))
            {
                Target target = clazz.getAnnotation(Target.class);
                ElementType[] type = target.value();

                if (type.length != 1 && !type[0].equals(ElementType.TYPE))
                {
                    throw new WebBeansConfigurationException("Stereotype with @InterceptorBinding must be defined as @Target{TYPE}");
                }

            }
        }
    }
    
    /**
     * Return true if a list of beans are directly specialized/extended each other.
     * 
     * @param beans, a set of specialized beans.
     * 
     * @return
     */
    protected static boolean isDirectlySpecializedBeanSet(Set<Bean<?>> beans) {
    	
    	ArrayList<AbstractOwbBean<?>> beanList = new ArrayList<AbstractOwbBean<?>>();

    	for(Bean<?> bb : beans) 
    	{
    		AbstractOwbBean<?>bean = (AbstractOwbBean<?>)bb;
    		beanList.add(bean);
    	}
    	
    	java.util.Collections.sort(beanList, new java.util.Comparator() 
    	{
    		public int compare(Object o1, Object o2) 
    		{
    			AbstractOwbBean<?> b1 = (AbstractOwbBean<?>)o1;
    			AbstractOwbBean<?> b2 = (AbstractOwbBean<?>)o2;
    			Class c1 = b1.getReturnType();
    			Class c2 = b2.getReturnType();
    			if (c2.isAssignableFrom(c1)) return 1;
    			if (c1.isAssignableFrom(c2)) return -1;
    			throw new InconsistentSpecializationException(c1 + " and " + c2 + "are not assignable to each other." );
    		}
    	});

    	for(int i=0; i<beanList.size() - 1; i++) 
    	{
    		if (!beanList.get(i).getReturnType().equals(beanList.get(i+1).getReturnType().getSuperclass()))
    				return false;
    	}
    	return true;
    }

    public static void configureSpecializations(List<Class<?>> beanClasses)
    {
    	for(Class<?> clazz : beanClasses) 
    	{
    		configureSpecializations(clazz, beanClasses);
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
    protected static void configureSpecializations(Class<?> specializedClass, List<Class<?>> beanClasses)
    {
        Asserts.nullCheckForClass(specializedClass);

        Bean<?> superBean = null;
        Bean<?> specialized = null;
        Set<Bean<?>> resolvers = null;
        AlternativesManager altManager = AlternativesManager.getInstance();
        
        if ((resolvers = isConfiguredWebBeans(specializedClass, true)) != null)
        {            
            if(resolvers.isEmpty())
            {
                throw new InconsistentSpecializationException("Specialized bean for class : " + specializedClass + " is not enabled in the deployment.");
            }
            
            specialized = resolvers.iterator().next();
            
            if(resolvers.size() > 1)
            {
            	if (!isDirectlySpecializedBeanSet(resolvers)) 
            	{
            		throw new InconsistentSpecializationException("More than one specialized bean for class : " + specializedClass + " is enabled in the deployment.");
            	}
            	// find the widest bean which satisfies the specializedClass
                for( Bean<?> sp : resolvers) {
                	if (sp == specialized) continue;
                	if (((AbstractOwbBean<?>)sp).getReturnType().isAssignableFrom(((AbstractOwbBean<?>)specialized).getReturnType())) 
                	{
                		specialized = sp;
                	}
                }
            }
            
            Class<?> superClass = specializedClass.getSuperclass();
            
            resolvers = isConfiguredWebBeans(superClass,false);
            
            for(Bean<?> candidates : resolvers)
            {
                AbstractOwbBean<?> candidate = (AbstractOwbBean<?>)candidates;
                
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
            	// Recursively configure super class first if super class is also a special bean.
            	// So the name and bean meta data could be populated to this beanclass.  
            	if (beanClasses.contains(superClass) && ((AbstractOwbBean<?>)superBean).isEnabled()) 
            	{
            		configureSpecializations(superClass, beanClasses);
            	}
            	
            	if (!AnnotationUtil.hasClassAnnotation(specializedClass, Alternative.class)) 
            	{
            		//disable superbean if the current bean is not an alternative 
            		((AbstractOwbBean<?>)superBean).setEnabled(false);
            	} 
            	else if(altManager.isClassAlternative(specializedClass)) 
            	{
            		//disable superbean if the current bean is an enabled alternative 
            		((AbstractOwbBean<?>)superBean).setEnabled(false);
            	}
                                
                AbstractOwbBean<?> comp = (AbstractOwbBean<?>)specialized;

                if(superBean.getName() != null)
                {
                    if(comp.getName() != null)
                    {
                        throw new DefinitionException("@Specialized Class : " + specializedClass.getName() + " may not explicitly declare a bean name");
                    }                    
                    
                    comp.setName(superBean.getName());
                    comp.setSpecializedBean(true);
                }
                                
                specialized.getQualifiers().addAll(superBean.getQualifiers());
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
            AbstractOwbBean<?> bean = (AbstractOwbBean<?>)it.next();
            
            if (bean.getTypes().contains(clazz))
            {
                if(annotate)
                {
                    if(bean.getReturnType().isAnnotationPresent(Specializes.class))
                    {
                        if(!(bean instanceof NewBean))
                        {
                            beans.add(bean);
                        }                           
                    }                                    
                }
                else
                {
                    beans.add(bean);
                }
            }
        }

        return beans;
    }
    
    public static void checkUnproxiableApiType(Bean<?> bean, Class<? extends Annotation> scopeType)
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
                if (scopeType.isAnnotationPresent(NormalScope.class))
                {
                    throw new UnproxyableResolutionException("WebBeans with api type with normal scope must be proxiable to inject, but class : " + superClass.getName() + " is not proxiable type");
                }
            }

        }

    }

    public static void checkNullable(Class<?> type, AbstractOwbBean<?> component)
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
    public static void configureProducerSpecialization(AbstractOwbBean<?> component, Method method, Class<?> superClass)
    {
        Method superMethod = ClassUtil.getClassMethodWithTypes(superClass, method.getName(), Arrays.asList(method.getParameterTypes()));
        if (superMethod == null)
        {
            throw new WebBeansConfigurationException("Producer method specialization is failed. Method " + method.getName() + " not found in super class : " + superClass.getName());
        }
        
        if (!AnnotationUtil.hasAnnotation(superMethod.getAnnotations(), Produces.class))
        {
            throw new WebBeansConfigurationException("Producer method specialization is failed. Method " + method.getName() + " found in super class : " + superClass.getName() + " is not annotated with @Produces");
        }

        Annotation[] anns = AnnotationUtil.getQualifierAnnotations(superMethod.getAnnotations());

        for (Annotation ann : anns)
        {
            component.addQualifier(ann);
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
    public static void configuredProducerSpecializedName(AbstractOwbBean<?> component,Method method,Method superMethod)
    {
        Asserts.assertNotNull(component,"component parameter can not be null");
        Asserts.assertNotNull(method,"method parameter can not be null");
        Asserts.assertNotNull(superMethod,"superMethod parameter can not be null");
        
        String name = null;
        boolean hasName = false;
        if(AnnotationUtil.hasMethodAnnotation(superMethod, Named.class))
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
            if(AnnotationUtil.hasMethodAnnotation(method, Named.class))
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

        if (AnnotationUtil.hasMethodParameterAnnotation(method, Disposes.class) || AnnotationUtil.hasMethodParameterAnnotation(method, Observes.class))
        {
            throw new WebBeansConfigurationException("Initializer method parameters in method : " + method.getName() + " in class : " + clazz.getName() + " can not be annotated with @Disposes or @Observers");

        }

    }

    public static void checkInterceptorResolverParams(Annotation... interceptorBindings)
    {
        if (interceptorBindings == null || interceptorBindings.length == 0)
        {
            throw new IllegalArgumentException("Manager.resolveInterceptors() method parameter interceptor bindings array argument can not be empty");
        }

        Annotation old = null;
        for (Annotation interceptorBinding : interceptorBindings)
        {
            if (!AnnotationUtil.isInterceptorBindingAnnotation(interceptorBinding.annotationType()))
            {
                throw new IllegalArgumentException("Manager.resolveInterceptors() method parameter interceptor bindings array can not contain other annotation that is not @InterceptorBinding");
            }
            
            if (old == null)
            {
                old = interceptorBinding;
            }
            else
            {
                if (old.equals(interceptorBinding))
                {
                    throw new IllegalArgumentException("Manager.resolveInterceptors() method parameter interceptor bindings array argument can not define duplicate binding annotation with name : @" + old.getClass().getName());
                }

                old = interceptorBinding;
            }
        }
    }

    public static void checkDecoratorResolverParams(Set<Type> apiTypes, Annotation... qualifiers)
    {
        if (apiTypes == null || apiTypes.size() == 0)
        {
            throw new IllegalArgumentException("Manager.resolveDecorators() method parameter api types argument can not be empty");
        }

        Annotation old = null;
        for (Annotation qualifier : qualifiers)
        {
            if (old == null)
            {
                old = qualifier;
            }
            else
            {
                if (old.annotationType().equals(qualifier.annotationType()))
                {
                    throw new IllegalArgumentException("Manager.resolveDecorators() method parameter qualifiers array argument can not define duplicate qualifier annotation with name : @" + old.annotationType().getName());
                }

                if (!AnnotationUtil.isQualifierAnnotation(qualifier.annotationType()))
                {
                    throw new IllegalArgumentException("Manager.resolveDecorators() method parameter qualifiers array can not contain other annotation that is not @Qualifier");
                }

                old = qualifier;
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
        
        if(!candidateClazz.isAssignableFrom(Instance.class))
        {
            return false;
        }        
        
        Class<?> rawType = null;
        
        if(ClassUtil.isParametrizedType(injectionPoint.getType()))
        {
            ParameterizedType pt = (ParameterizedType)injectionPoint.getType();
            
            rawType = (Class<?>) pt.getRawType();
            
            Type[] typeArgs = pt.getActualTypeArguments();
            
            if(!(rawType.isAssignableFrom(Instance.class)))
            {
                throw new WebBeansConfigurationException("<Instance> field injection " + injectionPoint.toString() + " must have type javax.inject.Instance");
            }                
            else
            {                       
                if(typeArgs.length != 1)
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
    
    public static <T> void defineInterceptor(ManagedBeanCreatorImpl<T> managedBeanCreator, AnnotatedType<T> annotatedType)
    {
        Class<?> clazz = annotatedType.getJavaClass();
        if (InterceptorsManager.getInstance().isInterceptorEnabled(clazz))
        {
            ManagedBean<T> component = null;

            InterceptorUtil.checkInterceptorConditions(clazz);
            component = defineManagedBean(managedBeanCreator, annotatedType);

            if (component != null)
            {
                WebBeansInterceptorConfig.configureInterceptorClass((ManagedBean<Object>) component, 
                		AnnotationUtil.getInterceptorBindingMetaAnnotations(clazz.getDeclaredAnnotations()));
            }
        }

    }

    
    /**
     * Define decorator bean.
     * @param <T> type info
     * @param clazz decorator class
     */
    public static <T> void defineDecorator(ManagedBeanCreatorImpl<T> creator, AnnotatedType<T> annotatedType)
    {
        Class<T> clazz = annotatedType.getJavaClass();
        if (DecoratorsManager.getInstance().isDecoratorEnabled(clazz))
        {
            ManagedBean<T> delegate = null;

            DecoratorUtil.checkDecoratorConditions(clazz);
            delegate = defineManagedBean(creator, annotatedType);

            if (delegate != null)
            {
                WebBeansDecoratorConfig.configureDecoratorClass((ManagedBean<Object>) delegate);
            }
            else
            {
                logger.trace("Unable to configure decorator with class : " + clazz);
            }
        }
    }

    public static boolean isScopeTypeNormal(Class<? extends Annotation> scopeType)
    {
        Asserts.assertNotNull(scopeType, "scopeType argument can not be null");

        if (scopeType.isAnnotationPresent(NormalScope.class))
        {
            return true;
        }
        
        if(scopeType.isAnnotationPresent(Scope.class))
        {
            return false;
        }

        List<ExternalScope> additionalScopes = BeanManagerImpl.getManager().getAdditionalScopes();
        for (ExternalScope additionalScope : additionalScopes)
        {
            if (additionalScope.getScope().equals(scopeType))
            {
                return additionalScope.isNormal();
            }
        }
        
        // no scopetype found so far -> kawumms
        throw new IllegalArgumentException("scopeType argument must be annotated with @Scope or @NormalScope");
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
        NormalScope scope = scopeType.getAnnotation(NormalScope.class);
        if(scope != null)
        {
            if (scope.passivating())
            {
                if (!isSerializable)
                {
                    throw new IllegalProductException(errorMessage);
                }
            }            
        }
    }
    
    public static boolean isManagedBean(AbstractOwbBean<?> component)
    {
        if(component.getWebBeansType().equals(WebBeansType.MANAGED) ||
                component.getWebBeansType().equals(WebBeansType.INTERCEPTOR) ||
                component.getWebBeansType().equals(WebBeansType.DECORATOR))
        {
            return true;
        }
        
        return false;
    }
    
    public static boolean isProducerBean(AbstractOwbBean<?> bean)
    {
        if(bean.getWebBeansType().equals(WebBeansType.PRODUCERFIELD) ||
                bean.getWebBeansType().equals(WebBeansType.PRODUCERMETHOD))
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns true if bean is an enterprise bean, false otherwise.
     * @param bean bean instance
     * @return true if bean is an enterprise bean
     */
    public static boolean isEnterpriseBean(AbstractOwbBean<?> bean)
    {
        Asserts.assertNotNull(bean,"Bean is null");
        
        if(bean.getWebBeansType().equals(WebBeansType.ENTERPRISE))
        {
            return true;
        }
        
        return false;
    }
    
//    public static void addInjectedImplicitEventComponent(InjectionPoint injectionPoint)
//    {
//        Type type = injectionPoint.getType();
//        
//        if(!(type instanceof ParameterizedType))
//        {
//            return;
//        }
//        
//        Type[] args = new Type[0];
//        
//        Class<?> clazz = null;
//        if (type instanceof ParameterizedType)
//        {
//            ParameterizedType pt = (ParameterizedType) type;
//            args = pt.getActualTypeArguments();
//        }
//        
//        clazz = (Class<?>)args[0];
//        
//        Annotation[] qualifiers = new Annotation[injectionPoint.getQualifiers().size()];
//        qualifiers = injectionPoint.getQualifiers().toArray(qualifiers);
//        
//        Bean<?> bean = createObservableImplicitComponent(EventImpl.class, clazz, qualifiers);
//        BeanManagerImpl.getManager().addBean(bean);                  
//    }
    
    
//    public static <T> void addInjectedImplicitInstanceComponent(InjectionPoint injectionPoint)
//    {
//        ParameterizedType genericType = (ParameterizedType)injectionPoint.getType();
//        
//        Class<Instance<T>> clazz = (Class<Instance<T>>)genericType.getRawType();
//        
//        Annotation[] qualifiers = new Annotation[injectionPoint.getQualifiers().size()];
//        qualifiers = injectionPoint.getQualifiers().toArray(qualifiers);
//        
//        Bean<Instance<T>> bean = createInstanceComponent(genericType,clazz, genericType.getActualTypeArguments()[0], qualifiers);
//        BeanManagerImpl.getManager().addBean(bean);
//        
//    }
    
    public static Bean<?> getMostSpecializedBean(BeanManager manager, Bean<?> component)
    {
        Set<Bean<?>> beans = manager.getBeans(component.getBeanClass(), AnnotationUtil.getAnnotationsFromSet(component.getQualifiers()));
                
        for(Bean<?> bean : beans)
        {
            Bean<?> find = bean;
            
            if(!find.equals(component))
            {
                if(AnnotationUtil.hasClassAnnotation(find.getBeanClass(), Specializes.class))
                {
                    return getMostSpecializedBean(manager, find);
                }                
            }            
        }
        
        return component;
    }      
    
    /**
     * Returns <code>ProcessAnnotatedType</code> event. 
     * @param <T> bean type
     * @param clazz bean class
     * @return event
     */
    public static <T> GProcessAnnotatedType fireProcessAnnotatedTypeEvent(AnnotatedType<T> annotatedType)
    {                
        GProcessAnnotatedType processAnnotatedEvent = new GProcessAnnotatedType(annotatedType);
        
        //Fires ProcessAnnotatedType
        BeanManagerImpl.getManager().fireEvent(processAnnotatedEvent,AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        
        return processAnnotatedEvent;        
    }
    
    /**
     * Returns <code>ProcessInjectionTarget</code> event.
     * @param <T> bean type
     * @param bean bean instance
     * @return event
     */
    public static <T> GProcessInjectionTarget fireProcessInjectionTargetEvent(AbstractInjectionTargetBean<T> bean)
    {
        AnnotatedType<T> annotatedType = AnnotatedElementFactory.newAnnotatedType(bean.getReturnType());
        InjectionTargetProducer<T> injectionTarget = new InjectionTargetProducer<T>(bean);
        GProcessInjectionTarget processInjectionTargetEvent = new GProcessInjectionTarget(injectionTarget,annotatedType);
        
        //Fires ProcessInjectionTarget
        BeanManagerImpl.getManager().fireEvent(processInjectionTargetEvent, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        
        return processInjectionTargetEvent;
        
    }
    
    public static GProcessProducer fireProcessProducerEventForMethod(ProducerMethodBean<?> producerMethod,AnnotatedMethod<?> method)
    {         
        GProcessProducer producerEvent = new GProcessProducer(method);
        
        //Fires ProcessProducer for methods
        BeanManagerImpl.getManager().fireEvent(producerEvent, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        
        return producerEvent;
    }
    
    public static GProcessProducer fireProcessProducerEventForField(ProducerFieldBean<?> producerField,AnnotatedField<?> field)
    {         
        GProcessProducer producerEvent = new GProcessProducer(field);
        
        //Fires ProcessProducer for fields
        BeanManagerImpl.getManager().fireEvent(producerEvent, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        
        return producerEvent;
    }
    
    public static void fireProcessProducerMethodBeanEvent(Map<ProducerMethodBean<?>,AnnotatedMethod<?>> annotatedMethods)
    {
        for(ProducerMethodBean<?> bean : annotatedMethods.keySet())
        {
            AnnotatedMethod<?> annotatedMethod = annotatedMethods.get(bean);                
            Method disposal = bean.getDisposalMethod();
            
            AnnotatedMethod<?> disposalAnnotated = null;
            GProcessProducerMethod processProducerMethodEvent = null;
            if(disposal != null)
            {
                disposalAnnotated = AnnotatedElementFactory.newAnnotatedMethod(disposal, bean.getParent().getReturnType());
                processProducerMethodEvent = new GProcessProducerMethod(bean,annotatedMethod,disposalAnnotated.getParameters().get(0));                
            }
            else
            {
                processProducerMethodEvent = new GProcessProducerMethod(bean,annotatedMethod,null);
            }
            

            //Fires ProcessProducer
            BeanManagerImpl.getManager().fireEvent(processProducerMethodEvent, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        }                
    }
    
    public static void fireProcessObservableMethodBeanEvent(Map<ObserverMethod<?>,AnnotatedMethod<?>> annotatedMethods)
    {
        for(ObserverMethod<?> observableMethod : annotatedMethods.keySet())
        {
            AnnotatedMethod<?> annotatedMethod = annotatedMethods.get(observableMethod);                
            
            GProcessObservableMethod event = new GProcessObservableMethod(annotatedMethod, observableMethod);

            //Fires ProcessProducer
            BeanManagerImpl.getManager().fireEvent(event, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        }                
    }
    
    
    public static void fireProcessProducerFieldBeanEvent(Map<ProducerFieldBean<?>,AnnotatedField<?>> annotatedFields)
    {
        for(ProducerFieldBean<?> bean : annotatedFields.keySet())
        {
            AnnotatedField<?> field = annotatedFields.get(bean);
            
            GProcessProducerField processProducerFieldEvent = new GProcessProducerField(bean,field);
            
            //Fire ProcessProducer
            BeanManagerImpl.getManager().fireEvent(processProducerFieldEvent, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        }        
    }
    
    /**
     * Returns true if bean instance is an enterprise bean instance
     * false otherwise.
     * @param beanInstance bean instance
     * @return true if bean instance is an enterprise bean instance
     */
    public static boolean isBeanHasEnterpriseMarker(Object beanInstance)
    {
        Asserts.assertNotNull(beanInstance,"Bean instance is null");
        
        if(beanInstance instanceof EnterpriseBeanMarker)
        {
            return true;
        }
        
        return false;
    }
    
    public static void checkInjectionPointNamedQualifier(InjectionPoint injectionPoint)
    {
        Set<Annotation> qualifierset = injectionPoint.getQualifiers();
        Named namedQualifier = null;
        for(Annotation qualifier : qualifierset)
        {
            if(qualifier.annotationType().equals(Named.class))
            {
                namedQualifier = (Named)qualifier;
                break;
            }
        }
        
        if(namedQualifier != null)
        {
            String value = namedQualifier.value();
            
            if(value == null || value.equals(""))
            {
                Member member = injectionPoint.getMember();
                if(!(member instanceof Field))
                {
                    throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " can not define @Named qualifier without value!");
                }
            }
        }        
        
    }
    
    /**
     * Sets bean enabled flag.
     * @param bean bean instance
     */
    public static void setInjectionTargetBeanEnableFlag(InjectionTargetBean<?> bean)
    {
        Asserts.assertNotNull(bean, "bean can not be null");
        
        if(hasInjectionTargetBeanAnnotatedWithAlternative(bean))
        {
            if(!AlternativesManager.getInstance().isBeanHasAlternative(bean))
            {
                bean.setEnabled(false);
            }
        }
    }
    
   
    public static boolean hasInjectionTargetBeanAnnotatedWithAlternative(InjectionTargetBean<?> bean)
    {
        Asserts.assertNotNull(bean, "bean can not be null");
        
        boolean alternative = false;
        
        if(AnnotationUtil.hasClassAnnotation(bean.getBeanClass(), Alternative.class))
        {
           alternative = true; 
        }
        
        if(!alternative)
        {
            Set<Class<? extends Annotation>> stereotypes = bean.getStereotypes();
            for(Class<? extends Annotation> stereoType : stereotypes)
            {
                if(AnnotationUtil.hasClassAnnotation(stereoType, Alternative.class))
                {
                    alternative = true;
                    break;
                }
            }
            
        }
        
        return alternative;
        
    }
    
    public static void setBeanEnableFlagForProducerBean(InjectionTargetBean<?> parent,AbstractProducerBean<?> producer, Annotation[] annotations)
    {
        Asserts.assertNotNull(parent, "parent can not be null");
        Asserts.assertNotNull(producer, "producer can not be null");
        
        boolean alternative = false;
        
        if(AnnotationUtil.hasAnnotation(annotations, Alternative.class))
        {
           alternative = true; 
        }
        
        if(!alternative)
        {
            Set<Class<? extends Annotation>> stereotypes = producer.getStereotypes();
            for(Class<? extends Annotation> stereoType : stereotypes)
            {
                if(AnnotationUtil.hasClassAnnotation(stereoType, Alternative.class))
                {
                    alternative = true;
                    break;
                }
            }            
        }
        
        if(alternative)
        {
            if(hasInjectionTargetBeanAnnotatedWithAlternative(parent) && 
                    AlternativesManager.getInstance().isBeanHasAlternative(parent))
            {
                producer.setEnabled(true);
            }
            else
            {
                producer.setEnabled(false);
            }
        }
        else
        {
            producer.setEnabled(parent.isEnabled());
        }
    }
    
    public static Object getObjectFromCreationalContext(Bean<?> bean,CreationalContextImpl<?> cc)
    {
        if(cc == null)
        {
            throw new IllegalArgumentException("Creational context is null");
        }
        
        if(cc.getBean().equals(bean))
        {
            return cc.getProxyInstance();   
        }            
        else
        {
            if(cc.getOwnerCreational() != null)
            {
                return getObjectFromCreationalContext(bean, cc.getOwnerCreational());   
            }
        }
        
        return null;
    }
    
    public static boolean isExtensionEventType(Class<?> clazz)
    {
        if(clazz.equals(BeforeBeanDiscovery.class) ||
                clazz.equals(AfterBeanDiscovery.class) ||
                clazz.equals(AfterDeploymentValidation.class) ||
                clazz.equals(BeforeShutdown.class) ||
                clazz.equals(GProcessAnnotatedType.class) ||
                clazz.equals(GProcessInjectionTarget.class) ||
                clazz.equals(GProcessProducer.class) ||
                clazz.equals(GProcessProducerField.class) ||
                clazz.equals(GProcessProducerMethod.class) ||
                clazz.equals(GProcessManagedBean.class) ||
                clazz.equals(GProcessBean.class) ||
                clazz.equals(GProcessSessionBean.class) ||
                clazz.equals(GProcessObservableMethod.class)
                )
        {
            return true;
        }
        
        return false;
    }
    
    public static boolean isExtensionBeanEventType(Class<?> clazz)
    {
        if(clazz.equals(GProcessAnnotatedType.class) ||
                clazz.equals(GProcessInjectionTarget.class) ||
                clazz.equals(GProcessManagedBean.class) ||
                clazz.equals(GProcessSessionBean.class) ||
                clazz.equals(GProcessBean.class)
                )
        {
            return true;
        }
        
        return false;        
    }
    
    public static boolean isDefaultExtensionBeanEventType(Class<?> clazz)
    {
        if(clazz.equals(ProcessAnnotatedType.class) ||
                clazz.equals(ProcessInjectionTarget.class) ||
                clazz.equals(ProcessManagedBean.class) ||
                clazz.equals(ProcessBean.class) ||
                clazz.equals(ProcessSessionBean.class)
                )
        {
            return true;
        }
        
        return false;        
    }    
    
    public static boolean isExtensionProducerOrObserverEventType(Class<?> clazz)
    {
        if(clazz.equals(GProcessProducer.class) ||
                clazz.equals(GProcessProducerField.class) ||
                clazz.equals(GProcessProducerMethod.class) ||
                clazz.equals(GProcessObservableMethod.class)
                )
        {
            return true;
        }
        
        return false;

    }
    
    public static boolean isDefaultExtensionProducerOrObserverEventType(Class<?> clazz)
    {
        if(clazz.equals(ProcessProducer.class) ||
                clazz.equals(ProcessProducerField.class) ||
                clazz.equals(ProcessProducerMethod.class) ||
                clazz.equals(ProcessObserverMethod.class)
                )
        {
            return true;
        }
        
        return false;

    }    
    
    public static boolean isDependent(Bean<?> bean)
    {
        return ((OwbBean) bean).isDependent();
    }
    
    public static void inspectErrorStack(String logMessage)
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        //Looks for errors
        ErrorStack stack = manager.getErrorStack();
        try
        {
            if(stack.hasErrors())
            {
                stack.logErrors();
                throw new WebBeansConfigurationException(logMessage);
            }            
        }finally
        {
            stack.clear();
        }        
    }

    /**
     *
     * @param contextual the {@link Bean} to check
     * @return the uniqueId if it is {@link PassivationCapable} and enabled
     */
    public static String isPassivationCapable(Contextual<?> contextual)
    {
        if(contextual instanceof Bean)
        {
            if(contextual instanceof AbstractOwbBean)
            {
                if( ((AbstractOwbBean<?>)contextual).isPassivationCapable())
                {
                    return ((AbstractOwbBean<?>)contextual).getId();
                }
            }
            
            else if(contextual instanceof PassivationCapable)
            {
                PassivationCapable pc = (PassivationCapable)contextual;
                
                return pc.getId();
            }
        }
        else
        {
            if((contextual instanceof PassivationCapable) && (contextual instanceof Serializable))
            {
                PassivationCapable pc = (PassivationCapable)contextual;
                
                return pc.getId();
            }
        }
        
        return null;
    }
    
 
    public static <T> ManagedBean<T> defineManagedBean(ManagedBeanCreatorImpl<T> managedBeanCreator,AnnotatedType<T> annotatedType)
    {
        ManagedBean<T> managedBean = managedBeanCreator.getBean();
        
        Class<T> clazz = annotatedType.getJavaClass();
        
        managedBeanCreator.defineSerializable();

        //Define meta-data
        managedBeanCreator.defineStereoTypes();
        //Scope type
        managedBeanCreator.defineScopeType(logger.getTokenString(OWBLogConst.TEXT_MB_IMPL) + clazz.getName() + logger.getTokenString(OWBLogConst.TEXT_SAME_SCOPE));                                                            
        //Check for Enabled via Alternative
        WebBeansUtil.setInjectionTargetBeanEnableFlag(managedBean);
        
        managedBeanCreator.defineApiType();
        managedBeanCreator.checkCreateConditions();
        managedBeanCreator.defineQualifier();
        managedBeanCreator.defineName(WebBeansUtil.getManagedBeanDefaultName(clazz.getSimpleName()));
        managedBeanCreator.defineConstructor();            
        Set<ProducerMethodBean<?>> producerMethods = managedBeanCreator.defineProducerMethods();
        Set<ProducerFieldBean<?>> producerFields = managedBeanCreator.defineProducerFields();
        managedBeanCreator.defineInjectedFields();
        managedBeanCreator.defineInjectedMethods();
        
        Set<ObserverMethod<?>> observerMethods = new HashSet<ObserverMethod<?>>();
        if(managedBean.isEnabled())
        {
            observerMethods = managedBeanCreator.defineObserverMethods();
        }
                                
        //Fires ProcessInjectionTarget
        ProcessInjectionTargetImpl<T> processInjectionTargetEvent = WebBeansUtil.fireProcessInjectionTargetEvent(managedBean);    
        WebBeansUtil.inspectErrorStack("There are errors that are added by ProcessInjectionTarget event observers. Look at logs for further details");
        
        if(processInjectionTargetEvent.isSet())
        {
            managedBeanCreator.setInjectedTarget(processInjectionTargetEvent.getInjectionTarget());
        }
        
        Map<ProducerMethodBean<?>,AnnotatedMethod<?>> annotatedMethods = new HashMap<ProducerMethodBean<?>, AnnotatedMethod<?>>(); 
        for(ProducerMethodBean<?> producerMethod : producerMethods)
        {
            AnnotatedMethod<?> method = AnnotatedElementFactory.newAnnotatedMethod(producerMethod.getCreatorMethod(), producerMethod.getParent().getReturnType());
            ProcessProducerImpl<?, ?> producerEvent = WebBeansUtil.fireProcessProducerEventForMethod(producerMethod,method);                
            WebBeansUtil.inspectErrorStack("There are errors that are added by ProcessProducer event observers for ProducerMethods. Look at logs for further details");

            annotatedMethods.put(producerMethod, method);
            
            if(producerEvent.isProducerSet())
            {
                producerMethod.setProducer((Producer)managedBeanCreator);
            }
            
            producerEvent.setProducerSet(false);
        }
        
        Map<ProducerFieldBean<?>,AnnotatedField<?>> annotatedFields = new HashMap<ProducerFieldBean<?>, AnnotatedField<?>>();
        for(ProducerFieldBean<?> producerField : producerFields)
        {
            AnnotatedField<?> field = AnnotatedElementFactory.newAnnotatedField(producerField.getCreatorField(), producerField.getParent().getReturnType());
            ProcessProducerImpl<?, ?> producerEvent = WebBeansUtil.fireProcessProducerEventForField(producerField, field);
            WebBeansUtil.inspectErrorStack("There are errors that are added by ProcessProducer event observers for ProducerFields. Look at logs for further details");
            
            annotatedFields.put(producerField, field);
            
            if(producerEvent.isProducerSet())
            {
                producerField.setProducer((Producer) managedBeanCreator);
            }
            
            producerEvent.setProducerSet(false);
        }

        Map<ObserverMethod<?>,AnnotatedMethod<?>> observerMethodsMap = new HashMap<ObserverMethod<?>, AnnotatedMethod<?>>(); 
        for(ObserverMethod<?> observerMethod : observerMethods)
        {
            ObserverMethodImpl<?> impl = (ObserverMethodImpl<?>)observerMethod;
            AnnotatedMethod<?> method = AnnotatedElementFactory.newAnnotatedMethod(impl.getObserverMethod(), impl.getBeanClass());
            
            observerMethodsMap.put(observerMethod, method);
        }
        
        BeanManagerImpl beanManager = BeanManagerImpl.getManager();
        
        //Fires ProcessManagedBean
        ProcessBeanImpl<T> processBeanEvent = new GProcessManagedBean(managedBean,annotatedType);            
        beanManager.fireEvent(processBeanEvent, new Annotation[0]);            
        WebBeansUtil.inspectErrorStack("There are errors that are added by ProcessManagedBean event observers for managed beans. Look at logs for further details");
        
        //Fires ProcessProducerMethod
        WebBeansUtil.fireProcessProducerMethodBeanEvent(annotatedMethods);            
        WebBeansUtil.inspectErrorStack("There are errors that are added by ProcessProducerMethod event observers for producer method beans. Look at logs for further details");            
        
        //Fires ProcessProducerField
        WebBeansUtil.fireProcessProducerFieldBeanEvent(annotatedFields);
        WebBeansUtil.inspectErrorStack("There are errors that are added by ProcessProducerField event observers for producer field beans. Look at logs for further details");            
        
        //Fire ObservableMethods
        WebBeansUtil.fireProcessObservableMethodBeanEvent(observerMethodsMap);
        WebBeansUtil.inspectErrorStack("There are errors that are added by ProcessObserverMethod event observers for observer methods. Look at logs for further details");
        
        //Set InjectionTarget that is used by the container to inject dependencies!
        if(managedBeanCreator.isInjectionTargetSet())
        {
            managedBean.setInjectionTarget(managedBeanCreator);   
        }

        beanManager.addBean(WebBeansUtil.createNewBean(managedBean));                
        if(!WebBeansAnnotatedTypeUtil.isAnnotatedTypeDecoratorOrInterceptor(annotatedType))
        {
            DecoratorUtil.checkManagedBeanDecoratorConditions(managedBean);
            beanManager.addBean(managedBean);
            for (ProducerMethodBean<?> producerMethod : producerMethods)
            {
                // add them one after the other to enable serialization handling et al
                beanManager.addBean(producerMethod);
            }
            managedBeanCreator.defineDisposalMethods();//Define disposal method after adding producers
            for (ProducerFieldBean<?> producerField : producerFields)
            {
                // add them one after the other to enable serialization handling et al
                beanManager.addBean(producerField);
            }
        }
        
        return managedBean;
    }

    /**
     * Determines if the injection is to be performed into a static field.
     * 
     * @param injectionPoint
     * @return <code>true</code> if the injection is into a static field
     */
    public static boolean isStaticInjection(InjectionPoint injectionPoint) 
    {
        if (injectionPoint != null)
        {
            Member member = injectionPoint.getMember(); 
            if (member != null && Modifier.isStatic(member.getModifiers()))
            {
                return true;
            }
        }
        
        return false;
    }
    
}