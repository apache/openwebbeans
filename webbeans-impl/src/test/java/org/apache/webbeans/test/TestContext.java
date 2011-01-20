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

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.decorator.Decorator;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.interceptor.Interceptor;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.xml.XMLManagedBean;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.ManagedBeanConfigurator;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.DependentContext;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.portable.events.generics.GProcessAnnotatedType;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeIsnotInterface;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeMustImplementAllDecoratedTypes;
import org.apache.webbeans.test.component.decorator.broken.MoreThanOneDelegateAttribute;
import org.apache.webbeans.test.component.decorator.broken.PaymentDecorator;
import org.apache.webbeans.test.component.decorator.clean.LargeTransactionDecorator;
import org.apache.webbeans.test.component.decorator.clean.ServiceDecorator;
import org.apache.webbeans.test.component.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.WebBeanswithMetaInterceptor;
import org.apache.webbeans.test.containertests.ComponentResolutionByTypeTest;
import org.apache.webbeans.test.mock.MockManager;
import org.apache.webbeans.test.servlet.ITestContext;
import org.apache.webbeans.test.servlet.TestListener;
import org.apache.webbeans.test.sterotype.StereoWithNonScope;
import org.apache.webbeans.test.sterotype.StereoWithRequestScope;
import org.apache.webbeans.test.sterotype.StereoWithSessionScope;
import org.apache.webbeans.test.sterotype.StereoWithSessionScope2;
import org.apache.webbeans.test.unittests.xml.XMLTest;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;
import org.apache.webbeans.xml.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


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
    private WebBeansLogger logger = WebBeansLogger.getLogger(TestContext.class);

    /**
     * All unit test classes. It is defined for starting the tests from the
     * {@link ServletContextListener} methods
     */
    private static Set<ITestContext> testContexts = new HashSet<ITestContext>();

    /** Test class name */
    private String clazzName;

    /** MockManager is the mock implementation of the {@link BeanManager} */
    private MockManager manager;

    /** Use for XML tests */
    protected WebBeansXMLConfigurator xmlConfigurator = null;

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
        WebBeansContext.getInstance().getPluginLoader().startUp();
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
        initializeInterceptorType(WebBeansInterceptor.class);
        initializeInterceptorType(WebBeanswithMetaInterceptor.class);

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
    
    protected void addInstanceImplicitBean(Bean<?> bean)
    {
        Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
        for(InjectionPoint injectionPoint : injectionPoints)
        {
            //If contains the @Obtains, defines implicit component
            if(injectionPoint.getAnnotated().getBaseType().equals(Instance.class))
            {
                //WebBeansUtil.addInjectedImplicitInstanceComponent(injectionPoint);
            }                                    
        }

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
     * Call before test.
     */
    protected void beforeTest()
    {

    }

    /**
     * This will be called whenever the test is failed. NOT : This method is
     * used for running the tests from the {@link ServletContextListener}. It is
     * not used for normal unit tests.
     * 
     * @see TestListener
     * @see ComponentResolutionByTypeTest
     * @param methodName failed method name
     */
    public void fail(String methodName)
    {
        logger.error("Test Class: " + clazzName + ",Method Name: " + methodName + " is FAILED");
    }

    /**
     * This will be called whenever the test is passed. NOT : This method is
     * used for running the tests from the {@link ServletContextListener}. It is
     * not used for normal unit tests.
     * 
     * @see TestListener
     * @see ComponentResolutionByTypeTest
     * @param methodName passed method name
     */
    public void pass(String methodName)
    {
        logger.info("Test Class: " + clazzName + ",Method Name: " + methodName + " is PASSED");
    }

    /**
     * Initialize all tests. NOT : This method is used for initializing the all
     * tests classes from the {@link ServletContextListener}. It is not used for
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
     * classes from the {@link ServletContextListener}. It is not used for
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
     * classes from the {@link ServletContextListener}. It is not used for
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
    protected <T> AbstractInjectionTargetBean<T> defineManagedBean(Class<T> clazz)
    {
        ManagedBean<T> bean = null;

        bean = ManagedBeanConfigurator.define(clazz, WebBeansType.MANAGED);
        if (bean != null)
        {
            manager.addBean(WebBeansUtil.createNewBean(bean));
            DecoratorUtil.checkManagedBeanDecoratorConditions(bean,null);
            WebBeansDecoratorConfig.configureDecarotors(bean);
            DefinitionUtil.defineBeanInterceptorStack(bean);

            getComponents().add((AbstractOwbBean<?>) bean);
            manager.addBean(bean);

            GProcessAnnotatedType type = new GProcessAnnotatedType(WebBeansContext.getInstance().getAnnotatedElementFactory().newAnnotatedType(clazz));
            manager.fireEvent(type, new Annotation[0]);            
        }

        return bean;
    }

    /**
     * Defines XML defined new simple webbean.
     * 
     * @param simpleClass webbeans class
     * @param webBeanDecleration element decleration defines simple webbeans
     */
    protected <T> XMLManagedBean<T> defineXMLSimpleWebBeans(Class<T> simpleClass, Element webBeanDecleration)
    {
        XMLManagedBean<T> bean = null;
        bean = this.xmlConfigurator.configureSimpleWebBean(simpleClass, webBeanDecleration);

        return bean;
    }

    /**
     * Protected helper function which loads a WebBean definition from the given
     * xmlResourcePath. This will first do a Class lookup and take his
     * annotations as a base, later overlaying it with the definitions from the
     * given XML.
     * 
     * @param xmlResourcePath
     * @return XMLComponentImpl<?> with the WebBean definition
     */
    protected XMLManagedBean<?> getWebBeanFromXml(String xmlResourcePath)
    {
        InputStream stream = XMLTest.class.getClassLoader().getResourceAsStream(xmlResourcePath);
        Assert.assertNotNull(stream);

        Element beanElement = null;
        Element rootElement = XMLUtil.getRootElement(stream);
        NodeList ns = rootElement.getChildNodes();
        for(int i = 0; i<=ns.getLength(); i++)
        {
        	Node node = ns.item(i);
        	if (!(node instanceof Element)) continue;
        	beanElement = (Element)node;
        	break;
        }
        Class<?> clazz = XMLUtil.getElementJavaType(beanElement);

        XMLManagedBean<?> def = defineXMLSimpleWebBeans(clazz, beanElement);

        return def;
    }

    /**
     * Private helper function which loads a WebBean definition from the given
     * xmlResourcePath. This will first do a Class lookup and take his
     * annotations as a base, later overlaying it with the definitions from the
     * given XML.
     * 
     * @param xmlResourcePath
     * @return XMLComponentImpl<?> with the WebBean definition
     */
    @SuppressWarnings("unchecked")
    protected AbstractOwbBean<?> getWebBeanFromXml(String xmlResourcePath, Class<?> desiredClazz, Annotation... bindings)
    {
        InputStream stream = XMLTest.class.getClassLoader().getResourceAsStream(xmlResourcePath);
        Assert.assertNotNull(stream);

        Element beanElement;
        Element rootElement = XMLUtil.getRootElement(stream);
        NodeList nodes = rootElement.getChildNodes();
        for(int i=0; i<nodes.getLength(); i++)
        {
        	Node node = nodes.item(i);
        	if (!(node instanceof Element)) {
        		continue;
        	}
        	beanElement = (Element)node;
            Class<?> clazz = XMLUtil.getElementJavaType(beanElement);

            defineXMLSimpleWebBeans(clazz, beanElement);
        }

        Set<Bean<?>> beans = getManager().getBeans(desiredClazz, bindings);
        if (beans != null && beans.size() == 1)
        {
            return (AbstractOwbBean<?>) beans.iterator().next();
        }
        return null;
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

        ManagedBeanConfigurator.checkManagedBeanCondition(clazz);

        WebBeansContext.getInstance().getInterceptorsManager().addNewInterceptor(clazz);
        InterceptorUtil.checkInterceptorConditions(clazz);
        component = ManagedBeanConfigurator.define(clazz, WebBeansType.INTERCEPTOR);
        WebBeansInterceptorConfig.configureInterceptorClass((ManagedBean<Object>) component,
                                                            WebBeansContext.getInstance().getAnnotationManager().getInterceptorBindingMetaAnnotations(
                                                                clazz.getDeclaredAnnotations()));


        return component;
    }

    /**
     * Defines the simple webbeans decorator.
     * 
     * @param clazz decorator class
     * @return the new decorator
     */
    @SuppressWarnings("unchecked")
    protected <T> AbstractOwbBean<T> defineDecorator(Class<T> clazz)
    {
        ManagedBean<T> component = null;

        if (WebBeansContext.getInstance().getDecoratorsManager().isDecoratorEnabled(clazz))
        {
            DecoratorUtil.checkDecoratorConditions(clazz);
            component = ManagedBeanConfigurator.define(clazz, WebBeansType.DECORATOR);

            if (component != null)
            {
                WebBeansDecoratorConfig.configureDecoratorClass((ManagedBean<Object>) component);
            }
        }

        return component;
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
     * Gets the ith component in the {@link MockManager}
     * 
     * @param i ith component in the {@link MockManager}
     * @return the ith component in the list
     */
    protected AbstractOwbBean<?> getComponent(int i)
    {
        return manager.getComponent(i);
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
        return manager.getInstanceByName(name);
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

    /**
     * Return new {@link MockHttpSession}
     * 
     * @return new mock session
     */
    protected Object getSession()
    {
        return new Object();
    }

    /**
     * Configuration of the webbeans XML file.
     * 
     * @param file input stream
     * @param fileName file name
     */
    protected void configureFromXML(InputStream file, String fileName)
    {
        this.xmlConfigurator.configure(file, fileName);
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
        WebBeansContext.getInstance().getInterceptorsManager().addNewInterceptor(interceptorClazz);

    }

    /**
     * Add new deocrator class.
     * 
     * @param decoratorClazz decorator class
     */
    protected void initializeDecoratorType(Class<?> decoratorClazz)
    {
        WebBeansContext.getInstance().getDecoratorsManager().addNewDecorator(decoratorClazz);

    }

    /**
     * End tests for sub-class. NOTE : This method is used for ending the all
     * test methods in sub-class from the {@link ServletContextListener}. It is
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
     * test methods in sub-class from the {@link ServletContextListener}. It is
     * not used for normal unit tests.
     * 
     * @see TestListener
     * @see ComponentResolutionByTypeTest
     */
    public void startTests(Object ctx)
    {
    }     

}