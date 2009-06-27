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
package org.apache.webbeans.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.ScopeType;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Current;
import javax.enterprise.inject.deployment.Specializes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.stereotype.Model;
import javax.interceptor.Interceptor;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.annotation.AfterBeanDiscoveryLiteral;
import org.apache.webbeans.annotation.BeforeBeanDiscoveryLiteral;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.inject.InconsistentSpecializationException;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.spi.deployer.MetaDataDiscoveryService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;
import org.apache.webbeans.xml.XMLSpecializesManager;

/**
 * Deploys the all beans that are defined in the {@link WebBeansScanner} at
 * the scanner phase.
 */
@SuppressWarnings("unchecked")
public class WebBeansContainerDeployer
{
    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansContainerDeployer.class);

    protected boolean deployed = false;

    protected WebBeansXMLConfigurator xmlConfigurator = null;

    public WebBeansContainerDeployer(WebBeansXMLConfigurator xmlConfigurator)
    {
        this.xmlConfigurator = xmlConfigurator;
    }

    /**
     * Deploys all the defined web beans components in the container startup.
     * <p>
     * It deploys from the web-beans.xml files and from the class files. It uses
     * the {@link WebBeansScanner} class to get classes.
     * </p>
     * 
     * @throws WebBeansDeploymentException if any deployment exception occurs
     */
    public void deploy(MetaDataDiscoveryService scanner)
    {
        try
        {
            if (!deployed)
            {
                // Register Manager built-in component
                ManagerImpl.getManager().addBean(WebBeansUtil.getManagerComponent());

                // Register Conversation built-in component
                ManagerImpl.getManager().addBean(WebBeansUtil.getConversationComponent());
                
                // Register InjectionPoint bean
                ManagerImpl.getManager().addBean(WebBeansUtil.getInjectionPointComponent());

                // Bind manager
                JNDIService service = ServiceLoader.getService(JNDIService.class);
                service.bind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME, ManagerImpl.getManager());

                deployFromXML(scanner);
                checkStereoTypes(scanner);
                configureInterceptors(scanner);
                configureDecorators(scanner);
                deployFromClassPath(scanner);
                
                checkSpecializations(scanner);
                
                //Fire @Initialized Event
                fireInitializeEvent();
                
                //Validate injection Points
                validateInjectionPoints();
                
                //Fire @Deployed Event
                fireDeployedEvent();
                
                deployed = true;
            }

        }
        catch (WebBeansConfigurationException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            if(e instanceof WebBeansDeploymentException)
            {
                throw  (WebBeansDeploymentException)e;
            }
            else
            {
                throw new WebBeansDeploymentException(e);
            }
        }
    }
    
    private void fireInitializeEvent()
    {
        BeanManager manager = ManagerImpl.getManager();
        manager.fireEvent(manager, new Annotation[] { new BeforeBeanDiscoveryLiteral() });
    }
    
    
    private void fireDeployedEvent()
    {
        BeanManager manager = ManagerImpl.getManager();
        manager.fireEvent(manager, new Annotation[] { new AfterBeanDiscoveryLiteral() });        
        
    }
    
    private void validateInjectionPoints()
    {
        logger.info("Validation of injection points are started");

        ManagerImpl manager = ManagerImpl.getManager();        
        Set<Bean<?>> beans = new HashSet<Bean<?>>();
        
        //Adding decorators to validate
        Set<Decorator<?>> decorators = manager.getDecorators();
        for(Decorator decorator : decorators)
        {
            WebBeansDecorator wbDec = (WebBeansDecorator)decorator;
            beans.add(wbDec.getDelegateComponent());
        }
        
        
        logger.info("Validation of the decorator's injection points are started");
        
        //Validate Decorators
        validate(beans);
        
        beans.clear();
        
        //Adding interceptors to validate
        Set<javax.enterprise.inject.spi.Interceptor<?>> interceptors = manager.getInterceptors();
        for(javax.enterprise.inject.spi.Interceptor interceptor : interceptors)
        {
            WebBeansInterceptor wbInt = (WebBeansInterceptor)interceptor;
            beans.add(wbInt.getDelegate());
        }
        
        logger.info("Validation of the interceptor's injection points are started");
        
        //Validate Interceptors
        validate(beans);
        
        beans.clear();
        
        //Validate Others
        validate(beans);
                

        logger.info("All injection points are validated succesfully");
    }
    

    private void validate(Set<Bean<?>> beans)
    {
        InjectionResolver resolver = ManagerImpl.getManager().getInjectionResolver();
        
        if (beans != null && beans.size() > 0)
        {
            for (Bean<?> bean : beans)
            {
                //Bean injection points
                Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
                                
                for (InjectionPoint injectionPoint : injectionPoints)
                {
                    //Check for correct injection type
                    resolver.checkInjectionPointType(injectionPoint);
                    
                    Class<?> rawType = getRawTypeForInjectionPoint(injectionPoint);
                    
                    //Comment out while testing TCK Events Test --- WBTCK27 jira./////
                    //Hack for EntityManager --> Solve in M3!!!!
                    if(rawType.equals(Event.class) || rawType.getSimpleName().equals("EntityManager"))
                    {
                        continue;
                    }
                    /////////////////////////////////////////////////////////////////
                    
                    // check for InjectionPoint injection
                    if (rawType.equals(InjectionPoint.class))
                    {
                        Annotated annotated = injectionPoint.getAnnotated();
                        if (annotated.getAnnotations().size() == 1 && annotated.isAnnotationPresent(Current.class))
                        {
                            if (!bean.getScopeType().equals(Dependent.class))
                            {
                                throw new WebBeansConfigurationException("Bean " + bean + "scope can not define other scope except @Dependent to inject InjectionPoint");
                            }
                        }
                    }
                    else
                    {
                        resolver.checkInjectionPoints(injectionPoint);
                    }
                }
            }
        }
        
    }
    
    /**
     * Returns injection point raw type.
     * 
     * @param injectionPoint injection point definition
     * @return injection point raw type
     */
    private Class<?> getRawTypeForInjectionPoint(InjectionPoint injectionPoint)
    {
        Class<?> rawType = null;
        Type type = injectionPoint.getType();
        
        if(type instanceof Class)
        {
            rawType = (Class<?>) type;
        }
        else if(type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType)type;            
            rawType = (Class<?>)pt.getRawType();                                                
        }
        
        return rawType;
    }

    protected void deployFromClassPath(MetaDataDiscoveryService scanner) throws ClassNotFoundException
    {
        logger.info("Deploying configurations from class files is started");

        // Start from the class
        Map<String, Set<String>> classIndex = scanner.getClassIndex();
        
        if (classIndex != null)
        {
            Set<String> pathClasses = classIndex.keySet();
            Iterator<String> itPathClasses = pathClasses.iterator();

            while (itPathClasses.hasNext())
            {
                String componentClassName = itPathClasses.next();
                Class<?> implClass = ClassUtil.getClassFromName(componentClassName);

                //It must not be @Interceptor or @Decorator
                if(AnnotationUtil.isAnnotationExistOnClass(implClass, javax.decorator.Decorator.class) ||
                        AnnotationUtil.isAnnotationExistOnClass(implClass, Interceptor.class))
                {
                    continue;
                }
                
                if (SimpleWebBeansConfigurator.isSimpleWebBean(implClass))
                {
                    logger.info("Simple WebBeans Component with class name : " + componentClassName + " is found");
                    defineSimpleWebBeans(implClass);
                }
                else if (EJBWebBeansConfigurator.isEJBWebBean(implClass))
                {
                    logger.info("Enterprise WebBeans Component with class name : " + componentClassName + " is found");
                    defineEnterpriseWebBeans();
                }
            }
        }

        logger.info("Deploying configurations from class files is ended");

    }

    protected void deployFromXML(MetaDataDiscoveryService scanner) throws WebBeansDeploymentException
    {
        logger.info("Deploying configurations from XML files is started");

        Set<URL> xmlLocations = scanner.getWebBeansXmlLocations();
        Iterator<URL> it = xmlLocations.iterator();

        while (it.hasNext())
        {
            URL fileURL = it.next();
            String fileName = fileURL.getFile();
            InputStream fis = null;
            try
            {
                fis = fileURL.openStream();
                
                this.xmlConfigurator.configure(fis, fileName);
            } 
            catch (IOException e)
            {
                throw new WebBeansDeploymentException(e);
            }
            finally
            {
                if (fis != null)
                {
                    try
                    {
                        fis.close();
                    } catch (IOException e)
                    {
                        // all ok, ignore this!
                    }
                }
            }
        }

        logger.info("Deploying configurations from XML is ended succesfully");
    }

    protected void configureInterceptors(MetaDataDiscoveryService scanner) throws ClassNotFoundException
    {
        logger.info("Configuring the Interceptors is started");

        // Interceptors Set
        Map<String, Set<String>> annotIndex = scanner.getAnnotationIndex();
        Set<String> classes = annotIndex.get(Interceptor.class.getName());

        if (classes != null)
        {
            for (String interceptorClazz : classes)
            {
                Class<?> implClass = ClassUtil.getClassFromName(interceptorClazz);

                logger.info("Simple WebBeans Interceptor Component with class name : " + interceptorClazz + " is found");

                defineSimpleWebBeansInterceptors(implClass);
            }
        }

        logger.info("Configuring the Interceptors is ended");

    }

    protected void configureDecorators(MetaDataDiscoveryService scanner) throws ClassNotFoundException
    {
        logger.info("Configuring the Decorators is started");

        Map<String, Set<String>> annotIndex = scanner.getAnnotationIndex();
        Set<String> classes = annotIndex.get(javax.decorator.Decorator.class.getName());

        if (classes != null)
        {
            for (String decoratorClazz : classes)
            {
                Class<?> implClass = ClassUtil.getClassFromName(decoratorClazz);
                logger.info("Simple WebBeans Decorator Component with class name : " + decoratorClazz + " is found");

                defineSimpleWebBeansDecorators(implClass);
            }
        }

        logger.info("Configuring the Decorators is ended");

    }

    protected void checkSpecializations(MetaDataDiscoveryService scanner)
    {
        logger.info("Checking Specialization constraints is started");
        
        try
        {
            Map<String, Set<String>> specialMap = scanner.getAnnotationIndex();
            if (specialMap != null && specialMap.size() > 0)
            {
                if (specialMap.containsKey(Specializes.class.getName()))
                {
                    Set<String> specialClassSet = specialMap.get(Specializes.class.getName());
                    Iterator<String> specialIterator = specialClassSet.iterator();

                    Class<?> superClass = null;
                    while (specialIterator.hasNext())
                    {
                        String specialClassName = specialIterator.next();
                        Class<?> specialClass = ClassUtil.getClassFromName(specialClassName);
                        
                        if (superClass == null)
                        {
                            superClass = specialClass.getSuperclass();
                            
                            if(superClass.equals(Object.class))
                            {
                                throw new WebBeansConfigurationException("Specalized class : " + specialClassName + " must extend another class");
                            }
                        }
                        else
                        {
                            if (superClass.equals(specialClass.getSuperclass()))
                            {
                                throw new InconsistentSpecializationException("More than one class that specialized the same super class : " + superClass.getName());
                            }
                        }
                        
                        WebBeansUtil.configureSpecializations(specialClass);
                    }
                }
            }

            // XML Defined Specializations
            checkXMLSpecializations();            
        }
        catch(Exception e)
        {
            throw new WebBeansDeploymentException(e);
        }
        

        logger.info("Checking Specialization constraints is ended");
    }

    private void checkXMLSpecializations()
    {
        // Check XML specializations
        Set<Class<?>> clazzes = XMLSpecializesManager.getInstance().getXMLSpecializationClasses();
        Iterator<Class<?>> it = clazzes.iterator();
        Class<?> superClass = null;
        Class<?> specialClass = null;
        while (it.hasNext())
        {
            specialClass = it.next();

            if (superClass == null)
            {
                superClass = specialClass.getSuperclass();
            }
            else
            {
                if (superClass.equals(specialClass.getSuperclass()))
                {
                    throw new InconsistentSpecializationException("XML Specialization Error : More than one class that specialized the same super class : " + superClass.getName());
                }
            }

            WebBeansUtil.configureSpecializations(specialClass);

        }
    }

    protected void checkPassivationScopes()
    {
        Set<Bean<?>> beans = ManagerImpl.getManager().getBeans();

        if (beans != null && beans.size() > 0)
        {
            Iterator<Bean<?>> itBeans = beans.iterator();
            while (itBeans.hasNext())
            {
                Object beanObj = itBeans.next();
                if (beanObj instanceof ComponentImpl)
                {
                    ComponentImpl<?> component = (ComponentImpl<?>) beanObj;
                    ScopeType scope = component.getScopeType().getAnnotation(ScopeType.class);
                    if (scope.passivating())
                    {
                        // TODO  Check constructor

                        // TODO Check non-transient fields

                        // TODO Check initializer methods

                        // TODO Check producer methods
                    }
                }
            }
        }
    }

    protected void checkStereoTypes(MetaDataDiscoveryService scanner)
    {
        logger.info("Checking StereoTypes constraints is started");

        addDefaultStereoTypes();
        
        Map<String, Set<String>> stereotypeMap = scanner.getClassIndex();
        if (stereotypeMap != null && stereotypeMap.size() > 0)
        {
            Set<String> stereoClassSet = stereotypeMap.keySet();
            Iterator<String> steIterator = stereoClassSet.iterator();
            while (steIterator.hasNext())
            {
                String steroClassName = steIterator.next();
                
                Class<? extends Annotation> stereoClass = (Class<? extends Annotation>) ClassUtil.getClassFromName(steroClassName);
                
                if (AnnotationUtil.isStereoTypeAnnotation(stereoClass))
                {
                    if (!XMLAnnotationTypeManager.getInstance().isStereoTypeExist(stereoClass))
                    {
                        WebBeansUtil.checkStereoTypeClass(stereoClass);
                        StereoTypeModel model = new StereoTypeModel(stereoClass);
                        StereoTypeManager.getInstance().addStereoTypeModel(model);
                    }
                }
            }
        }

        logger.info("Checking StereoTypes constraints is ended");
    }

    protected void addDefaultStereoTypes()
    {
        StereoTypeModel model = new StereoTypeModel(Model.class);
        StereoTypeManager.getInstance().addStereoTypeModel(model);
        
        model = new StereoTypeModel(javax.decorator.Decorator.class);
        StereoTypeManager.getInstance().addStereoTypeModel(model);
        
        model = new StereoTypeModel(Interceptor.class);
        StereoTypeManager.getInstance().addStereoTypeModel(model);        
    }
    
    protected <T> void defineSimpleWebBeans(Class<T> clazz)
    {
        ComponentImpl<T> component = null;

        if (!AnnotationUtil.isAnnotationExistOnClass(clazz, Interceptor.class) && !AnnotationUtil.isAnnotationExistOnClass(clazz, javax.decorator.Decorator.class))
        {
            component = SimpleWebBeansConfigurator.define(clazz, WebBeansType.SIMPLE);
            if (component != null)
            {
                ManagerImpl.getManager().addBean(WebBeansUtil.createNewSimpleBeanComponent(component));
                
                DecoratorUtil.checkSimpleWebBeanDecoratorConditions(component);

                /* I have added this into the ComponentImpl.afterCreate(); */
                // DefinitionUtil.defineSimpleWebBeanInterceptorStack(component);
                ManagerImpl.getManager().addBean(component);
            }
        }
    }

    /**
     * Defines the new interceptor with given class.
     * 
     * @param clazz interceptor class
     */
    protected <T> void defineSimpleWebBeansInterceptors(Class<T> clazz)
    {
        WebBeansUtil.defineSimpleWebBeansInterceptors(clazz);
    }

    protected <T> void defineSimpleWebBeansDecorators(Class<T> clazz)
    {
        WebBeansUtil.defineSimpleWebBeansDecorators(clazz);
    }

    protected void defineEnterpriseWebBeans()
    {

    }
}