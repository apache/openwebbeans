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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EnterpriseBean;
import javax.faces.component.UIComponent;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.Entity;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;
import javax.webbeans.ApplicationScoped;
import javax.webbeans.Conversation;
import javax.webbeans.ConversationScoped;
import javax.webbeans.Decorator;
import javax.webbeans.DeploymentType;
import javax.webbeans.Destructor;
import javax.webbeans.Disposes;
import javax.webbeans.DuplicateBindingTypeException;
import javax.webbeans.InconsistentSpecializationException;
import javax.webbeans.Initializer;
import javax.webbeans.Named;
import javax.webbeans.New;
import javax.webbeans.Fires;
import javax.webbeans.NullableDependencyException;
import javax.webbeans.Observes;
import javax.webbeans.Produces;
import javax.webbeans.RequestScoped;
import javax.webbeans.ScopeType;
import javax.webbeans.SessionScoped;
import javax.webbeans.UnproxyableDependencyException;
import javax.webbeans.manager.Bean;
import javax.webbeans.manager.Interceptor;
import javax.webbeans.manager.Manager;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.annotation.NewLiteral;
import org.apache.webbeans.annotation.ProductionLiteral;
import org.apache.webbeans.annotation.RequestedScopeLiteral;
import org.apache.webbeans.annotation.StandardLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.Component;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.component.ConversationComponent;
import org.apache.webbeans.component.ManagerComponentImpl;
import org.apache.webbeans.component.NewComponentImpl;
import org.apache.webbeans.component.ObservableComponentImpl;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.EJBWebBeansConfigurator;
import org.apache.webbeans.config.SimpleWebBeansConfigurator;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.stereotype.IStereoTypeModel;
import org.apache.webbeans.ejb.EJBUtil;
import org.apache.webbeans.ejb.orm.ORMUtil;
import org.apache.webbeans.event.EventUtil;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.intercept.InterceptorDataImpl;
import org.apache.webbeans.intercept.InterceptorType;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.jsf.ConversationImpl;

/**
 * Contains some utility methods used in the all project.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
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
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Return true if the given class is ok for simple web bean conditions,
     * falase otherwise.
     * 
     * @param clazz class in hand
     * @return true if the given class is ok for simple web bean conditions.
     */
    public static void isSimpleWebBeanClass(Class<?> clazz)
    {
        try
        {
            Asserts.nullCheckForClass(clazz);
            int modifier = clazz.getModifiers();

            if (ClassUtil.isParametrized(clazz))
                throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not be parametrized type");

            if (!ClassUtil.isStatic(modifier) && ClassUtil.isInnerClazz(clazz))
                throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not be non-static inner class");

            if (!ClassUtil.isConcrete(clazz) && !AnnotationUtil.isAnnotationExistOnClass(clazz, Decorator.class))
                throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " have to be concrete if not defines as @Decorator");

            if (AnnotationUtil.isAnnotationExistOnClass(clazz, Entity.class))
                throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not be JPA Entity class");

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

            if (ClassUtil.isAssignable(UIComponent.class, clazz))
                throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not implement JSF UIComponent");

            // TODO ejb-jar.xml check
            if (EJBUtil.isDefinedInXML(clazz.getName()))
                throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not defined in the ejb-jar.xml");

            // TODO orm.xml check
            if (ORMUtil.isDefinedInXML(clazz.getName()))
                throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not defined in orm.xml");

            if (!isConstructureOk(clazz))
                throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " must define at least one Constructor");

        }
        catch (WebBeansConfigurationException e)
        {
            throw e;
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
                    throw new WebBeansConfigurationException("There are more than one Constrcutor with Initializer annotation in class " + clazz.getName());
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
            Type[] observableTypes = AnnotationUtil.getConstructorParameterGenericTypesWithGivenAnnotation(result, Fires.class);
            EventUtil.checkObservableMethodParameterConditions(observableTypes, "constructor parameter", "constructor : " + result.getName() + "in class : " + clazz.getName());

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

        if (AnnotationUtil.isMethodHasAnnotation(method, Initializer.class) || AnnotationUtil.isMethodHasAnnotation(method, Destructor.class) || AnnotationUtil.isMethodParameterAnnotationExist(method, Disposes.class) || AnnotationUtil.isMethodParameterAnnotationExist(method, Observes.class))
        {
            throw new WebBeansConfigurationException("Producer method : " + method.getName() + " in the class : " + parentImplClazzName + " can not be annotated with" + " @Initializer/@Destructor annotation or has a parameter annotated with @Disposes/@Observes");
        }
    }

    public static void checkProducerMethodDisposal(Method disposalMethod, String parentImplClazzName)
    {
        Type[] observableTypes = AnnotationUtil.getMethodParameterGenericTypesWithGivenAnnotation(disposalMethod, Fires.class);
        EventUtil.checkObservableMethodParameterConditions(observableTypes, "method parameter", "method : " + disposalMethod.getName() + "in class : " + parentImplClazzName);

        if (AnnotationUtil.isMethodMultipleParameterAnnotationExist(disposalMethod, Disposes.class))
        {
            throw new WebBeansConfigurationException("Disposal method : " + disposalMethod.getName() + " in class " + parentImplClazzName + " has multiple @Disposes annotation parameter");
        }

        if (AnnotationUtil.isMethodHasAnnotation(disposalMethod, Initializer.class) || AnnotationUtil.isMethodHasAnnotation(disposalMethod, Destructor.class) || AnnotationUtil.isMethodParameterAnnotationExist(disposalMethod, Observes.class) || AnnotationUtil.isMethodHasAnnotation(disposalMethod, Produces.class))
        {
            throw new WebBeansConfigurationException("Disposal method : " + disposalMethod.getName() + " in the class : " + parentImplClazzName + " can not be annotated with" + " @Initializer/@Destructor/@Produces annotation or has a parameter annotated with @Observes");
        }

    }

    /**
     * Check conditions for the new binding.
     * 
     * @param annotations annotations
     */
    public static void checkForNewBindingForDeployment(Type type, Class<?> clazz, String name, Annotation... annotations)
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
    public static <T> NewComponentImpl<T> createNewComponent(Class<T> clazz)
    {
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

        NewComponentImpl<T> comp = null;

        if (SimpleWebBeansConfigurator.isSimpleWebBean(clazz))
        {
            comp = new NewComponentImpl<T>(clazz, WebBeansType.SIMPLE);
            comp.setConstructor(WebBeansUtil.defineConstructor(clazz));

            DefinitionUtil.defineInjectedFields(comp);
            DefinitionUtil.defineInjectedMethods(comp);
        }
        else if (EJBWebBeansConfigurator.isEJBWebBean(clazz))
        {
            comp = new NewComponentImpl<T>(clazz, WebBeansType.ENTERPRISE);
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

    public static <T, K> ObservableComponentImpl<T, K> createObservableImplicitComponent(Class<T> returnType, Class<K> eventType, Annotation... annotations)
    {
        ObservableComponentImpl<T, K> component = new ObservableComponentImpl<T, K>(returnType, eventType, WebBeansType.OBSERVABLE);

        DefinitionUtil.defineApiTypes(component, returnType);
        DefinitionUtil.defineBindingTypes(component, annotations);

        Constructor<T> constructor = null;

        try
        {
            constructor = returnType.getConstructor(new Class<?>[] { Annotation[].class, Class.class });

        }
        catch (SecurityException e)
        {
            throw new WebBeansException("Security exception for getting EventImpl class constructor", e);

        }
        catch (NoSuchMethodException e)
        {
            throw new WebBeansException("No constructor found in EventImpl class", e);
        }

        component.setConstructor(constructor);
        component.setType(new StandardLiteral());
        component.setImplScopeType(new DependentScopeLiteral());

        return component;
    }

    public static ManagerComponentImpl getManagerComponent()
    {
        ManagerComponentImpl managerComponent = new ManagerComponentImpl();

        managerComponent.setImplScopeType(new DependentScopeLiteral());
        managerComponent.setType(new StandardLiteral());
        managerComponent.addBindingType(new CurrentLiteral());
        managerComponent.addApiType(Manager.class);
        managerComponent.addApiType(Object.class);

        return managerComponent;
    }

    public static ConversationComponent getConversationComponent()
    {
        ConversationComponent conversationComp = new ConversationComponent();

        conversationComp.addApiType(Conversation.class);
        conversationComp.addApiType(ConversationImpl.class);
        conversationComp.addApiType(Object.class);
        conversationComp.setImplScopeType(new RequestedScopeLiteral());
        conversationComp.setType(new StandardLiteral());
        conversationComp.addBindingType(new CurrentLiteral());

        return conversationComp;
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
    public static void configureInterceptorMethods(Interceptor webBeansInterceptor, Class<?> clazz, Class<? extends Annotation> annotation, boolean definedInInterceptorClass, boolean definedInMethod, List<InterceptorData> stack, Method annotatedInterceptorClassMethod, boolean isDefinedWithWebBeans)
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
                        Object interceptorProxy = ManagerImpl.getManager().getInstance(webBeansInterceptor);
                        WebBeansInterceptor interceptor = (WebBeansInterceptor) webBeansInterceptor;
                        interceptor.setInjections(interceptorProxy);

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
    public static boolean isComponentHasStereoType(Component<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");

        Set<Annotation> set = component.getStereoTypes();
        Annotation[] anns = new Annotation[set.size()];
        anns = set.toArray(anns);
        if (AnnotationUtil.isStereoTypeMetaAnnotationExist(anns))
        {
            return true;
        }

        return false;
    }

    public static Annotation[] getComponentStereoTypes(Component<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        if (isComponentHasStereoType(component))
        {
            Set<Annotation> set = component.getStereoTypes();
            Annotation[] anns = new Annotation[set.size()];
            anns = set.toArray(anns);

            return AnnotationUtil.getStereotypeMetaAnnotations(anns);
        }

        return new Annotation[] {};
    }

    public static boolean isNamedExistOnStereoTypes(Component<?> component)
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

    public static Annotation getMaxPrecedenceSteroTypeDeploymentType(Component<?> component)
    {
        Annotation[] deploymentTypes = getComponentStereoTypes(component);
        Class<? extends Annotation> maxPrecedDeploymentType = null;
        Annotation result = null;
        if (deploymentTypes.length > 0)
        {
            for (Annotation dt : deploymentTypes)
            {
                if (AnnotationUtil.isMetaAnnotationExist(dt.annotationType().getAnnotations(), DeploymentType.class))
                {
                    Annotation result2[] = AnnotationUtil.getMetaAnnotations(dt.annotationType().getAnnotations(), DeploymentType.class);

                    Class<? extends Annotation> dtAnnot = result2[0].getClass();
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

        if (result == null)
        {
            return new ProductionLiteral();

        }

        return result;
    }

    public static String getSimpleWebBeanDefaultName(String clazzName)
    {
        StringBuffer name = new StringBuffer(clazzName);
        name.setCharAt(0, Character.toLowerCase(name.charAt(0)));

        return name.toString();
    }

    public static String getProducerDefaultName(String methodName)
    {
        StringBuffer buffer = new StringBuffer(methodName);

        if (buffer.substring(0, 3).equals("get"))
        {
            buffer.setCharAt(3, Character.toLowerCase(buffer.charAt(3)));

            return buffer.substring(3);
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

        Annotation[] annotations = clazz.getAnnotations();

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

    public static void configureSpecializations(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        Bean<?> parent = null;
        Bean<?> child = null;
        if ((parent = isConfiguredWebBeans(clazz)) != null)
        {
            Class<?> superClass = clazz.getSuperclass();
            if ((child = isConfiguredWebBeans(superClass)) != null)
            {
                int res = DeploymentTypeManager.getInstance().comparePrecedences(parent.getDeploymentType(), child.getDeploymentType());
                if (res <= 0)
                {
                    throw new InconsistentSpecializationException("@Specializes exception. Class : " + clazz.getName() + " must have higher deployment type precedence from the class : " + superClass.getName());
                }
                parent.getBindingTypes().addAll(child.getBindingTypes());
            }
            else
            {
                throw new WebBeansConfigurationException("@Specializes exception. WebBean component class : " + clazz.getName() + " does not extends other WebBeans it specialize");
            }
        }

    }

    public static Bean<?> isConfiguredWebBeans(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        Set<Bean<?>> components = ManagerImpl.getManager().getComponents();
        Iterator<Bean<?>> it = components.iterator();

        while (it.hasNext())
        {
            Bean<?> bean = it.next();
            if (bean.getTypes().contains(clazz))
            {
                return bean;
            }
        }

        return null;
    }

    public static void checkSteroTypeRequirements(Component<?> component, Annotation[] anns, String errorMessage)
    {
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
                    if (!suppScopes.contains(component.getScopeType()))
                    {
                        throw new WebBeansConfigurationException(errorMessage + " must contains all required scope types in the @Stereotype annotation " + model.getName());
                    }
                }
            }
        }
    }

    public static void checkUnproxiableApiType(Bean<?> bean, ScopeType scopeType)
    {
        Asserts.assertNotNull("bean", "bean parameter can not be null");
        Asserts.assertNotNull(scopeType, "scopeType parameter can not be null");

        Set<Class<?>> types = bean.getTypes();
        Class<?> superClass = null;
        for (Class<?> type : types)
        {
            if (!type.isInterface())
            {
                if ((superClass == null) || (superClass.isAssignableFrom(type) && type != Object.class))
                {
                    superClass = type;
                }

            }
        }

        if (superClass != null)
        {
            Constructor<?> cons = ClassUtil.isContaintNoArgConstructor(superClass);

            if (ClassUtil.isPrimitive(superClass) || ClassUtil.isArray(superClass) || ClassUtil.isFinal(superClass.getModifiers()) || ClassUtil.hasFinalMethod(superClass) || (cons == null || ClassUtil.isPrivate(cons.getModifiers())))
            {
                if (scopeType.normal())
                {
                    throw new UnproxyableDependencyException("WebBeans with api type with normal scope must be proxiable to inject, but class : " + superClass.getName() + " is not proxiable type");
                }
            }

        }

    }

    public static void checkNullable(Class<?> type, AbstractComponent<?> component)
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

    public static void configureProducerSpecialization(AbstractComponent<?> component, Method method, Class<?> superClass)
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
    }

    public static void checkInjectedMethodParameterConditions(Method method, Class<?> clazz)
    {
        Asserts.assertNotNull(method, "method parameter can not be null");
        Asserts.nullCheckForClass(clazz);

        Type[] observableTypes = AnnotationUtil.getMethodParameterGenericTypesWithGivenAnnotation(method, Fires.class);
        EventUtil.checkObservableMethodParameterConditions(observableTypes, "method parameter", "method : " + method.getName() + "in class : " + clazz.getName());

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
                    throw new DuplicateBindingTypeException("Manager.resolveInterceptors() method parameter interceptor binding types array argument can not define duplicate binding annotation with name : @" + old.getClass().getName());
                }

                if (!AnnotationUtil.isInterceptorBindingAnnotation(interceptorBindingType.annotationType()))
                {
                    throw new IllegalArgumentException("Manager.resolveInterceptors() method parameter interceptor binding types array can not contain other annotation that is not @InterceptorBindingType");
                }

                old = interceptorBindingType;
            }
        }
    }

    public static void checkDecoratorResolverParams(Set<Class<?>> apiTypes, Annotation... bindingTypes)
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
                    throw new DuplicateBindingTypeException("Manager.resolveDecorators() method parameter binding types array argument can not define duplicate binding annotation with name : @" + old.getClass().getName());
                }

                if (!AnnotationUtil.isBindingAnnotation(bindingType.annotationType()))
                {
                    throw new IllegalArgumentException("Manager.resolveDecorators() method parameter binding types array can not contain other annotation that is not @BindingType");
                }

                old = bindingType;
            }
        }

    }

    public static <T> void checkObservableFieldsConditions(Class<T> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        Field[] candidateFields = AnnotationUtil.getClazzFieldsWithGivenAnnotation(clazz, Fires.class);

        for (Field candidateField : candidateFields)
        {
            EventUtil.checkObservableFieldConditions(candidateField.getGenericType(), candidateField.getName(), clazz.getName());
        }

    }

    public static <T> void checkPassivationScope(AbstractComponent<T> component, ScopeType scope)
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
    public static <T> void defineSimpleWebBeansInterceptors(Class<T> clazz)
    {
        if (InterceptorsManager.getInstance().isInterceptorEnabled(clazz))
        {
            ComponentImpl<T> component = null;

            InterceptorUtil.checkInterceptorConditions(clazz);
            component = SimpleWebBeansConfigurator.define(clazz, WebBeansType.INTERCEPTOR);

            if (component != null)
            {
                WebBeansInterceptorConfig.configureInterceptorClass((ComponentImpl<Object>) component, clazz.getDeclaredAnnotations());
            }
        }

    }

    @SuppressWarnings("unchecked")
    public static <T> void defineSimpleWebBeansDecorators(Class<T> clazz)
    {
        if (DecoratorsManager.getInstance().isDecoratorEnabled(clazz))
        {
            ComponentImpl<T> component = null;

            DecoratorUtil.checkDecoratorConditions(clazz);
            component = SimpleWebBeansConfigurator.define(clazz, WebBeansType.DECORATOR);

            if (component != null)
            {
                WebBeansDecoratorConfig.configureDecoratorClass((ComponentImpl<Object>) component);
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
}