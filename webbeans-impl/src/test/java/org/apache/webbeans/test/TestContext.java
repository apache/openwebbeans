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
package org.apache.webbeans.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.decorator.Decorator;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.Interceptor;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.CdiInterceptorBean;
import org.apache.webbeans.component.DecoratorBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.creation.BeanAttributesBuilder;
import org.apache.webbeans.component.creation.CdiInterceptorBeanBuilder;
import org.apache.webbeans.component.creation.DecoratorBeanBuilder;
import org.apache.webbeans.component.creation.ManagedBeanBuilder;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.DependentContext;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.portable.events.generics.GProcessAnnotatedType;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeIsnotInterface;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeMustImplementAllDecoratedTypes;
import org.apache.webbeans.test.component.decorator.broken.MoreThanOneDelegateAttribute;
import org.apache.webbeans.test.component.decorator.broken.PaymentDecorator;
import org.apache.webbeans.test.component.decorator.clean.LargeTransactionDecorator;
import org.apache.webbeans.test.component.decorator.clean.ServiceDecorator;
import org.apache.webbeans.test.component.intercept.webbeans.ActionInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.SecureAndTransactionalInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.TransactionalInterceptor2;
import org.apache.webbeans.test.containertests.ComponentResolutionByTypeTest;
import org.apache.webbeans.test.mock.MockManager;
import org.apache.webbeans.test.servlet.ITestContext;
import org.apache.webbeans.test.servlet.TestListener;
import org.apache.webbeans.test.sterotype.StereoWithNonScope;
import org.apache.webbeans.test.sterotype.StereoWithRequestScope;
import org.apache.webbeans.test.sterotype.StereoWithSessionScope;
import org.apache.webbeans.test.sterotype.StereoWithSessionScope2;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

/**
 * Superclass of all the unit test classes. It defines some methods for
 * subclasses and also do some initializtions for running the tests succesfully.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 * @deprecated
 * Please do not use this class anymore while writing tests. Instead
 * use {@link AbstractUnitTest} class. Also do not add new tests into 
 * the package org.apache.webbeans.test folder. Add your new tests into
 * the org.apache.webbeans.newtests folder.
 * 
 */
public abstract class TestContext implements ITestContext
{
    private Logger logger = WebBeansLoggerFacade.getLogger(TestContext.class);

    /**
     * All unit test classes. It is defined for starting the tests from the
     * ServletContextListener methods
     */
    private static Set<ITestContext> testContexts = new HashSet<ITestContext>();

    /** Test class name */
    private String clazzName;

    /** MockManager is the mock implementation of the {@link BeanManager} */
    private MockManager manager;

    /** Use for XML tests */
    protected WebBeansXMLConfigurator xmlConfigurator = null;

    private WebBeansContext webBeansContext;

    /**
     * Creates new test class.
     * 
     * @param clazzName class name of the test class
     */
    protected TestContext(String clazzName)
    {
        this.clazzName = clazzName;
        TestContext.testContexts.add(this);
        this.manager = new MockManager();
        this.xmlConfigurator = new WebBeansXMLConfigurator();
        this.webBeansContext = WebBeansContext.getInstance();
        webBeansContext.getPluginLoader().startUp();
    }
    

    /**
     * Initialize the tests. NOTE : Actually this has to be defined for each
     * test classes. But for the time being, this super-class globally defines
     * some deployment types, interceptor types, decorator types and
     * stereotypes. If you would like to override default initialization,
     * override these methods in your test classes.
     */
    public void init()
    {
        manager.clear();
        WebBeansContext.getInstance().getPluginLoader().startUp();
        
        initInterceptors();
        initDecorators();
        initStereoTypes();
        initDependentContext();            
    }

    protected void initDependentContext()
    {
        DependentContext dependentContext = (DependentContext) WebBeansContext.getInstance().getContextFactory().getStandardContext(Dependent.class);
        dependentContext.setActive(true);
    }

    /**
     * Initialize some predefined interceptors.
     */
    protected void initInterceptors()
    {
        initializeInterceptorType(TransactionalInterceptor2.class);
        initializeInterceptorType(ActionInterceptor.class);

    }

    /**
     * Initialize some predefined decorators.
     */
    protected void initDecorators()
    {
        initializeDecoratorType(DelegateAttributeIsnotInterface.class);
        initializeDecoratorType(MoreThanOneDelegateAttribute.class);
        initializeDecoratorType(PaymentDecorator.class);
        initializeDecoratorType(DelegateAttributeMustImplementAllDecoratedTypes.class);
        initializeDecoratorType(ServiceDecorator.class);
        initializeDecoratorType(LargeTransactionDecorator.class);

    }

    /**
     * Initialize some predefined stereotypes.
     */
    protected void initStereoTypes()
    {
        initDefaultStereoTypes();
        initializeStereoType(StereoWithNonScope.class);
        initializeStereoType(StereoWithRequestScope.class);
        initializeStereoType(StereoWithSessionScope.class);
        initializeStereoType(StereoWithSessionScope2.class);

    }

    /**
     * Default stereo types
     */
    protected void initDefaultStereoTypes()
    {
        initializeStereoType(Interceptor.class);
        initializeStereoType(Decorator.class);
    }


    /**
     * This will be called whenever the test is failed. NOT : This method is
     * used for running the tests from the ServletContextListener. It is
     * not used for normal unit tests.
     * 
     * @see TestListener
     * @see ComponentResolutionByTypeTest
     * @param methodName failed method name
     */
    public void fail(String methodName)
    {
        logger.severe("Test Class: " + clazzName + ",Method Name: " + methodName + " is FAILED");
    }

    /**
     * Initialize all tests. NOT : This method is used for initializing the all
     * tests classes from the ServletContextListener. It is not used for
     * normal unit tests.
     * 
     * @see TestListener
     * @see ComponentResolutionByTypeTest
     */
    public static void initTests()
    {
        Iterator<ITestContext> it = testContexts.iterator();
        while (it.hasNext())
        {
            it.next().init();
        }

    }

    /**
     * Start all tests. NOT : This method is used for starting the all tests
     * classes from the ServletContextListener. It is not used for
     * normal unit tests.
     * 
     * @see TestListener
     * @see ComponentResolutionByTypeTest
     */
    public static void startAllTests(Object object)
    {
        Iterator<ITestContext> it = testContexts.iterator();
        while (it.hasNext())
        {
            it.next().startTests(object);
        }

    }

    /**
     * Ending all tests. NOT : This method is used for ending the all tests
     * classes from the ServletContextListener. It is not used for
     * normal unit tests.
     * 
     * @see TestListener
     * @see ComponentResolutionByTypeTest
     */
    public static void endAllTests(Object object)
    {
        Iterator<ITestContext> it = testContexts.iterator();
        while (it.hasNext())
        {
            it.next().endTests(object);
        }

    }

    /**
     * Defines simple webbeans from the given class.
     * 
     * @param clazz simple webbeans class
     * @return simple webbean
     */
    protected <T> InjectionTargetBean<T> defineManagedBean(Class<T> clazz)
    {
        ManagedBean<T> bean;

        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        bean = define(clazz, WebBeansType.MANAGED, webBeansContext.getAnnotatedElementFactory().newAnnotatedType(clazz));
        if (bean != null)
        {
            if (bean instanceof InjectionTargetBean)
            {
                ((InjectionTargetBean) bean).defineBeanInterceptorStack();
            }

            getComponents().add((AbstractOwbBean<?>) bean);
            manager.addBean(bean);

            GProcessAnnotatedType type = new GProcessAnnotatedType(webBeansContext.getAnnotatedElementFactory().newAnnotatedType(clazz));
            manager.fireEvent(type, new Annotation[0]);            
        }

        return bean;
    }

    /**
     * Defines simple webbeans interceptor.
     * 
     * @param clazz interceptor class
     * @return the new interceptor
     */
    @SuppressWarnings("unchecked")
    protected <T> AbstractOwbBean<T> defineInterceptor(Class<T> clazz)
    {
        ManagedBean<T> component = null;

        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        webBeansContext.getWebBeansUtil().checkManagedBeanCondition(clazz);

        webBeansContext.getInterceptorsManager().addEnabledInterceptorClass(clazz);
        AnnotatedType<T> annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(clazz);
        BeanAttributesImpl<T> beanAttributes = BeanAttributesBuilder.forContext(getWebBeansContext()).newBeanAttibutes(annotatedType).build();
        CdiInterceptorBeanBuilder<T> ibb = new CdiInterceptorBeanBuilder<T>(webBeansContext, annotatedType, beanAttributes);
        ibb.defineCdiInterceptorRules();
        CdiInterceptorBean<T> bean = ibb.getBean();
        webBeansContext.getInterceptorsManager().addCdiInterceptor(bean);
        return bean;
    }

    /**
     * Defines the simple webbeans decorator.
     * 
     * @param clazz decorator class
     * @return the new decorator
     */
    protected <T> AbstractOwbBean<T> defineDecorator(Class<T> clazz)
    {

        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        if (webBeansContext.getDecoratorsManager().isDecoratorEnabled(clazz))
        {
            AnnotatedType<T> annotatedType = webBeansContext.getBeanManagerImpl().createAnnotatedType(clazz);
            BeanAttributesImpl<T> beanAttributes = BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(annotatedType).build();
            DecoratorBeanBuilder<T> dbb = new DecoratorBeanBuilder<T>(webBeansContext, annotatedType, beanAttributes);
            dbb.defineDecoratorRules();

            DecoratorBean<T> bean = dbb.getBean();
            webBeansContext.getDecoratorsManager().addDecorator(bean);
            return bean;
        }

        return null;
    }

    /**
     * Clear all components in the {@link MockManager}
     */
    protected void clear()
    {
        this.manager.clear();
        WebBeansContext.getInstance().getPluginLoader().startUp();
    }

    /**
     * Gets all components in the {@link MockManager}
     * 
     * @return all components
     */
    protected List<AbstractOwbBean<?>> getComponents()
    {
        return manager.getComponents();
    }

    /**
     * Return the size of the webbeans in the {@link MockManager}
     * 
     * @return the size of the components in the {@link MockManager}
     */
    protected int getDeployedComponents()
    {
        return manager.getDeployedCompnents();
    }

    /**
     * Gets the webbeans instance.
     * 
     * @param name name of the webbean
     * @return the webbeans instance
     */
    protected Object getInstanceByName(String name)
    {
        Bean<?> bean = manager.resolve(manager.getBeans(name));
        if (bean == null)
        {
            return null;
        }

        return manager.getReference(bean, Object.class, manager.createCreationalContext(bean));
    }

    /**
     * Gets the context with given scope type.
     * 
     * @param scopeType scope type
     * @return the context with given scope type
     */
    protected Context getContext(Class<? extends Annotation> scopeType)
    {
        return manager.getContext(scopeType);
    }

    /**
     * Gets the {@link MockManager} instance.
     * 
     * @return manager instance
     */
    protected MockManager getManager()
    {
        return manager;
    }

    protected WebBeansContext getWebBeansContext()
    {
        return webBeansContext;
    }

    /**
     * Return new MockHttpSession
     * 
     * @return new mock session
     */
    protected Object getSession()
    {
        //X TODO huh? WTF...
        return new Object();
    }

    /**
     * Add new stereotype model.
     * 
     * @param stereoClass stereotype class
     */
    protected void initializeStereoType(Class<? extends Annotation> stereoClass)
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        webBeansContext.getAnnotationManager().checkStereoTypeClass(stereoClass, stereoClass.getDeclaredAnnotations());
        StereoTypeModel model = new StereoTypeModel(webBeansContext, stereoClass);
        webBeansContext.getStereoTypeManager().addStereoTypeModel(model);
    }

    /**
     * Add new interceptor class.
     * 
     * @param interceptorClazz interceptor class
     */
    protected void initializeInterceptorType(Class<?> interceptorClazz)
    {
        WebBeansContext.getInstance().getInterceptorsManager().addEnabledInterceptorClass(interceptorClazz);

    }

    /**
     * Add new deocrator class.
     * 
     * @param decoratorClazz decorator class
     */
    protected void initializeDecoratorType(Class<?> decoratorClazz)
    {
        WebBeansContext.getInstance().getDecoratorsManager().addEnabledDecorator(decoratorClazz);

    }

    /**
     * End tests for sub-class. NOTE : This method is used for ending the all
     * test methods in sub-class from the ServletContextListener. It is
     * not used for normal unit tests.
     * 
     * @see TestListener
     * @see ComponentResolutionByTypeTest
     */
    public void endTests(Object ctx)
    {

    }

    /**
     * Start tests for sub-class. NOTE : This method is used for starting the all
     * test methods in sub-class from the ServletContextListener. It is
     * not used for normal unit tests.
     * 
     * @see TestListener
     * @see ComponentResolutionByTypeTest
     */
    public void startTests(Object ctx)
    {
    }     

    /**
     * Returns the newly created Simple WebBean Component.
     *
     * @param clazz Simple WebBean Component implementation class
     * @return the newly created Simple WebBean Component
     * @throws WebBeansConfigurationException if any configuration exception occurs
     */
    private <T> ManagedBean<T> define(Class<T> clazz, WebBeansType type, AnnotatedType<T> annotatedType) throws WebBeansConfigurationException
    {
        WebBeansContext webBeansContext = WebBeansContext.currentInstance();
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();

        int modifier = clazz.getModifiers();

        if (AnnotationUtil.hasClassAnnotation(clazz, Decorator.class) && AnnotationUtil.hasClassAnnotation(clazz, Interceptor.class))
        {
            throw new WebBeansConfigurationException("ManagedBean implementation class : " + clazz.getName()
                                                     + " may not annotated with both @Interceptor and @Decorator annotation");
        }

        if (!AnnotationUtil.hasClassAnnotation(clazz, Decorator.class) && !AnnotationUtil.hasClassAnnotation(clazz, Interceptor.class))
        {
            webBeansContext.getInterceptorUtil().checkSimpleWebBeansInterceptorConditions(clazz);
        }

        if (Modifier.isInterface(modifier))
        {
            throw new WebBeansConfigurationException("ManagedBean implementation class : " + clazz.getName() + " may not _defined as interface");
        }

        BeanAttributesImpl<T> beanAttributes = BeanAttributesBuilder.forContext(getWebBeansContext()).newBeanAttibutes(annotatedType).build();
        ManagedBeanBuilder<T, ManagedBean<T>> managedBeanCreator = new ManagedBeanBuilder<T, ManagedBean<T>>(webBeansContext, annotatedType, beanAttributes);
        managedBeanCreator.defineEnabled();
        ManagedBean<T> component = managedBeanCreator.getBean();
        List<AnnotatedMethod<?>> postConstructMethods
            = webBeansContext.getInterceptorUtil().getLifecycleMethods(component.getAnnotatedType(), PostConstruct.class, true);
        List<AnnotatedMethod<?>> preDestroyMethods
            = webBeansContext.getInterceptorUtil().getLifecycleMethods(component.getAnnotatedType(), PreDestroy.class, false);
        component.setProducer(new InjectionTargetImpl<T>(component.getAnnotatedType(), component.getInjectionPoints(), webBeansContext, postConstructMethods, preDestroyMethods));

        webBeansContext.getWebBeansUtil().setInjectionTargetBeanEnableFlag(component);

        //Dropped from the speicification
        //WebBeansUtil.checkSteroTypeRequirements(component, clazz.getDeclaredAnnotations(), "Simple WebBean Component implementation class : " + clazz.getName());

        Set<ProducerMethodBean<?>> producerMethods = managedBeanCreator.defineProducerMethods(component);
        for (ProducerMethodBean<?> producerMethod : producerMethods)
        {
            // add them one after the other to enable serialization handling et al
            manager.addBean(producerMethod);
        }

        Set<ProducerFieldBean<?>> producerFields = managedBeanCreator.defineProducerFields(component);
        for (ProducerFieldBean<?> producerField : producerFields)
        {
            // add them one after the other to enable serialization handling et al
            manager.addBean(producerField);
        }

        managedBeanCreator.defineObserverMethods(component);

        return component;
    }
    
    /**
     * Configures the web bean api types.
     * 
     * @param <T> generic class type
     * @param bean configuring web beans component
     * @param clazz bean implementation class
     */
    private static <T> void defineApiTypes(AbstractOwbBean<T> bean, Class<T> clazz)
    {
        //Looking for bean types
        Typed beanTypes = clazz.getAnnotation(Typed.class);
        if(beanTypes != null)
        {
            defineUserDefinedBeanTypes(bean, null, beanTypes);            
        }
        else
        {
            defineNormalApiTypes(bean, clazz);
        }
        removeIgnoredInterfaces(bean);
    }

    private static <T> void removeIgnoredInterfaces(AbstractOwbBean<T> bean)
    {
        Set<String> ignoredInterfaces = bean.getWebBeansContext().getOpenWebBeansConfiguration().getIgnoredInterfaces();
        for (Iterator<Type> i = bean.getTypes().iterator(); i.hasNext(); )
        {
            Type t = i.next();
            if (t instanceof Class && ignoredInterfaces.contains(((Class<?>) t).getName()))
            {
                i.remove();
            }
        }
    }
    
    private static <T> void defineUserDefinedBeanTypes(AbstractOwbBean<T> bean, Type producerGenericReturnType, Typed beanTypes)
    {
        if(producerGenericReturnType != null)
        {
            defineNormalProducerMethodApi((AbstractProducerBean<T>)bean, producerGenericReturnType);
        }
        else
        {
            defineNormalApiTypes(bean, bean.getReturnType());
        }
        
        //@Type values
        Class<?>[] types = beanTypes.value();        
        
        //Normal api types
        Set<Type> apiTypes = bean.getTypes();
        //New api types
        Set<Type> newTypes = new HashSet<Type>();
        for(Class<?> type : types)
        {
            Type foundType = null;
            
            for(Type apiType : apiTypes)
            {
                if(ClassUtil.getClazz(apiType) == type)
                {
                    foundType = apiType;
                    break;
                }
            }
            
            if(foundType == null)
            {
                throw new WebBeansConfigurationException("@Type values must be in bean api types : " + bean.getTypes());
            }
            
            newTypes.add(foundType);
        }
        
        apiTypes.clear();
        apiTypes.addAll(newTypes);
        
        apiTypes.add(Object.class);
    }

    private static <T> void defineNormalApiTypes(AbstractOwbBean<T> bean, Class<T> clazz)
    {
        bean.getTypes().add(Object.class);
        ClassUtil.setTypeHierarchy(bean.getTypes(), clazz);           
    }

    private static <T> void defineNormalProducerMethodApi(AbstractProducerBean<T> producerBean, Type type)
    {
        Set<Type> types = producerBean.getTypes();
        types.add(Object.class);
        
        Class<?> clazz  = ClassUtil.getClazz(type);
        
        if (clazz != null && (clazz.isPrimitive() || clazz.isArray()))
        {
            types.add(clazz);
        }
        else
        {
            ClassUtil.setTypeHierarchy(producerBean.getTypes(), type);
        }                    
    }
}
