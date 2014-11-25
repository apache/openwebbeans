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
package org.apache.webbeans.config;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.CdiInterceptorBean;
import org.apache.webbeans.component.DecoratorBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.creation.BeanAttributesBuilder;
import org.apache.webbeans.component.creation.CdiInterceptorBeanBuilder;
import org.apache.webbeans.component.creation.DecoratorBeanBuilder;
import org.apache.webbeans.component.creation.ManagedBeanBuilder;
import org.apache.webbeans.component.creation.ObserverMethodsBuilder;
import org.apache.webbeans.component.creation.ProducerFieldBeansBuilder;
import org.apache.webbeans.component.creation.ProducerMethodBeansBuilder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectableBeanManager;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.corespi.se.DefaultJndiService;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.apache.webbeans.exception.inject.DeploymentException;
import org.apache.webbeans.exception.inject.InconsistentSpecializationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.AbstractProducer;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.ProcessAnnotatedTypeImpl;
import org.apache.webbeans.portable.events.ProcessBeanImpl;
import org.apache.webbeans.portable.events.ProcessInjectionTargetImpl;
import org.apache.webbeans.portable.events.discovery.AfterBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.discovery.AfterDeploymentValidationImpl;
import org.apache.webbeans.portable.events.discovery.BeforeBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.generics.GProcessManagedBean;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.plugins.OpenWebBeansJavaEEPlugin;
import org.apache.webbeans.spi.plugins.OpenWebBeansWebPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.webbeans.util.InjectionExceptionUtil;
import org.apache.webbeans.util.WebBeansConstants;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.UnproxyableResolutionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deploys the all beans that are defined in the {@link org.apache.webbeans.spi.ScannerService} at
 * the scanner phase.
 */
@SuppressWarnings("unchecked")
//This class written as single threaded.
public class BeansDeployer
{
    //Logger instance
    private static final Logger logger = WebBeansLoggerFacade.getLogger(BeansDeployer.class);
    public static final String JAVAX_ENTERPRISE_PACKAGE = "javax.enterprise.";

    /**Deployment is started or not*/
    protected boolean deployed = false;

    /**XML Configurator*/
    protected WebBeansXMLConfigurator xmlConfigurator = null;
    
    /**Discover ejb or not*/
    protected boolean discoverEjb = false;
    private final WebBeansContext webBeansContext;

    /**
     * Creates a new deployer with given xml configurator.
     * 
     * @param xmlConfigurator xml configurator
     * @param webBeansContext
     */
    public BeansDeployer(WebBeansXMLConfigurator xmlConfigurator, WebBeansContext webBeansContext)
    {
        this.xmlConfigurator = xmlConfigurator;
        this.webBeansContext = webBeansContext;
        String usage = this.webBeansContext.getOpenWebBeansConfiguration().getProperty(OpenWebBeansConfiguration.USE_EJB_DISCOVERY);
        discoverEjb = Boolean.parseBoolean(usage);
    }

    /**
     * Deploys all the defined web beans components in the container startup.
     * <p>
     * It deploys from the web-beans.xml files and from the class files. It uses
     * the {@link org.apache.webbeans.spi.ScannerService} to get classes.
     * </p>
     * 
     * @throws WebBeansDeploymentException if any deployment exception occurs
     */
    public synchronized void deploy(ScannerService scanner)
    {
        try
        {
            if (!deployed)
            {                
                //Load Extensions
                webBeansContext.getExtensionLoader().loadExtensionServices();

                // Bind manager
                JNDIService service = webBeansContext.getService(JNDIService.class);
                
                //Default jndi is just a map
                if(service instanceof DefaultJndiService)
                {
                    service.bind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME, new InjectableBeanManager(webBeansContext.getBeanManagerImpl()));
                }
                //Assume, actual JNDI implementation
                else
                {
                    service.bind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME, webBeansContext.getBeanManagerImpl().getReference());
                }

                // Register Manager built-in component
                webBeansContext.getBeanManagerImpl().addInternalBean(webBeansContext.getWebBeansUtil().getManagerBean());

                //Fire Event
                fireBeforeBeanDiscoveryEvent();
                
                //Deploy bean from XML. Also configures deployments, interceptors, decorators.
                deployFromXML(scanner);
                
                //Checking stereotype conditions
                checkStereoTypes(scanner);
                
                //Configure Default Beans
                configureDefaultBeans();
                                
                //Discover classpath classes
                deployFromClassPath(scanner);
                
                //Deploy additional Annotated Types
                deployAdditionalAnnotatedTypes();

                //Check Specialization
                processSpecializations(scanner);
                
                //Fire Event
                fireAfterBeanDiscoveryEvent();
                
                //Validate injection Points
                validateInjectionPoints();
                
                //Fire Event
                fireAfterDeploymentValidationEvent();


                // do some cleanup after the deployment
                scanner.release();
                webBeansContext.getAnnotatedElementFactory().clear();
            }
        }
        catch (UnsatisfiedResolutionException e)
        {
            throw new DeploymentException(e);
        }
        catch (AmbiguousResolutionException e)
        {
            throw new DeploymentException(e);
        }
        catch (UnproxyableResolutionException e)
        {
            // the tck expects a DeploymentException, but it really should be a DefinitionException, see i.e. https://issues.jboss.org/browse/CDITCK-346
            throw new DeploymentException(e);
        }
        catch (WebBeansConfigurationException e)
        {
            throw new DeploymentException(e);
        }
        catch (Exception e)
        {
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
        finally
        {
            //if bootstrapping failed, it doesn't make sense to do it again
            //esp. because #addInternalBean might have been called already and would cause an exception in the next run
            deployed = true;
        }
    }

    /**
     * Configure Default Beans.
     */
    private void configureDefaultBeans()
    {
        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();
        WebBeansUtil webBeansUtil = webBeansContext.getWebBeansUtil();

        // Register Conversation built-in component
        beanManager.addInternalBean(webBeansUtil.getConversationBean());
        
        // Register InjectionPoint bean
        beanManager.addInternalBean(webBeansUtil.getInjectionPointBean());
        
        //Register Instance Bean
        beanManager.addInternalBean(webBeansUtil.getInstanceBean());
        
        //Register Event Bean
        beanManager.addInternalBean(webBeansUtil.getEventBean());
        
        //Register Metadata Beans
        beanManager.addInternalBean(webBeansUtil.getBeanMetadataBean());
        beanManager.addInternalBean(webBeansUtil.getInterceptorMetadataBean());
        beanManager.addInternalBean(webBeansUtil.getDecoratorMetadataBean());
        beanManager.addInternalBean(webBeansUtil.getInterceptedOrDecoratedBeanMetadataBean());
        
        //REgister Provider Beans
        OpenWebBeansJavaEEPlugin beanEeProvider = webBeansContext.getPluginLoader().getJavaEEPlugin();
        OpenWebBeansWebPlugin beanWebProvider = webBeansContext.getPluginLoader().getWebPlugin();
        
        if(beanEeProvider != null)
        {
            addDefaultBean(webBeansContext, "org.apache.webbeans.ee.common.beans.PrincipalBean");
            addDefaultBean(webBeansContext, "org.apache.webbeans.ee.beans.ValidatorBean");
            addDefaultBean(webBeansContext, "org.apache.webbeans.ee.beans.ValidatorFactoryBean");
            addDefaultBean(webBeansContext, "org.apache.webbeans.ee.beans.UserTransactionBean");
        }
        else if(beanWebProvider != null)
        {
            addDefaultBean(webBeansContext, "org.apache.webbeans.ee.common.beans.PrincipalBean");
        }
            
    }
    
    private void addDefaultBean(WebBeansContext ctx,String className)
    {
        Bean<?> bean = null;
        
        Class<?> beanClass = ClassUtil.getClassFromName(className);
        if(beanClass != null)
        {
            bean  = (Bean)newInstance(ctx, beanClass);
        }
        
        if(bean != null)
        {
            ctx.getBeanManagerImpl().addInternalBean(bean);
        }
    }

    /**
     * create a new instance of the class
     */
    private Object newInstance(WebBeansContext wbc, Class<?> clazz)
    {
        try
        {
            if(System.getSecurityManager() != null)
            {
                final Constructor<?> c = webBeansContext.getSecurityService().doPrivilegedGetConstructor(clazz, WebBeansContext.class);
                if (c == null)
                {
                    return webBeansContext.getSecurityService().doPrivilegedObjectCreate(clazz);
                }
                return c.newInstance(wbc);
            }

            if (clazz.getConstructors().length > 0)
            {
                try
                {
                    return clazz.getConstructor(new Class<?>[] { WebBeansContext.class }).newInstance(wbc);
                }
                catch (final Exception e)
                {
                    return clazz.newInstance();
                }
            }
            return clazz.newInstance();

        }
        catch(Exception e)
        {
            Throwable cause = e;
            if(e instanceof PrivilegedActionException)
            {
                cause = e.getCause();
            }

            String error = "Error occurred while creating an instance of class : " + clazz.getName();
            logger.log(Level.SEVERE, error, cause);
            throw new WebBeansException(error,cause);
        }
    }

    /**
     * Fires event before bean discovery.
     */
    private void fireBeforeBeanDiscoveryEvent()
    {
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        manager.fireLifecycleEvent(new BeforeBeanDiscoveryImpl(webBeansContext));
    }
    
    /**
     * Fires event after bean discovery.
     */
    private void fireAfterBeanDiscoveryEvent()
    {
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        manager.fireLifecycleEvent(new AfterBeanDiscoveryImpl(webBeansContext));

        webBeansContext.getWebBeansUtil().inspectErrorStack(
                "There are errors that are added by AfterBeanDiscovery event observers. Look at logs for further details");
    }
    
    /**
     * Fires event after deployment valdiation.
     */
    private void fireAfterDeploymentValidationEvent()
    {
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        manager.fireLifecycleEvent(new AfterDeploymentValidationImpl(manager));

        webBeansContext.getWebBeansUtil().inspectErrorStack(
            "There are errors that are added by AfterDeploymentValidation event observers. Look at logs for further details");
    }
    
    /**
     * Validate all injection points.
     */
    private void validateInjectionPoints()
    {
        logger.fine("Validation of injection points has started.");

        webBeansContext.getDecoratorsManager().validateDecoratorClasses();
        webBeansContext.getInterceptorsManager().validateInterceptorClasses();

        Set<Bean<?>> beans = new HashSet<Bean<?>>();
        
        //Adding decorators to validate
        Set<Decorator<?>> decorators = webBeansContext.getDecoratorsManager().getDecorators();

        beans.addAll(decorators);

        
        logger.fine("Validation of the decorator's injection points has started.");
        
        //Validate Decorators
        validate(beans);
        
        beans.clear();
        
        //Adding interceptors to validate
        List<javax.enterprise.inject.spi.Interceptor<?>> interceptors = webBeansContext.getInterceptorsManager().getCdiInterceptors();
        for(javax.enterprise.inject.spi.Interceptor interceptor : interceptors)
        {
            beans.add(interceptor);
        }
        
        logger.fine("Validation of the interceptor's injection points has started.");
        
        //Validate Interceptors
        validate(beans);
        
        beans.clear();
        
        beans = webBeansContext.getBeanManagerImpl().getBeans();
        
        //Validate Others
        validate(beans);                

        logger.info(OWBLogConst.INFO_0003);
    }
    
    /**
     * Validates beans.
     * 
     * @param beans deployed beans
     */
    private <T> void validate(Set<Bean<?>> beans)
    {
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        
        if (beans != null && beans.size() > 0)
        {
           Stack<String> beanNames = new Stack<String>();
            for (Bean<?> bean : beans)
            {
                if (bean instanceof OwbBean && !((OwbBean)bean).isEnabled())
                {
                    // we skip disabled beans
                    continue;
                }

                //don't validate the cdi-api
                if (bean.getBeanClass().getName().startsWith(JAVAX_ENTERPRISE_PACKAGE))
                {
                    continue;
                }

                String beanName = bean.getName();
                if(beanName != null)
                {
                    beanNames.push(beanName);
                }
                
                if (bean instanceof OwbBean && !(bean instanceof Interceptor) && !(bean instanceof Decorator))
                {
                    OwbBean<T> owbBean = (OwbBean<T>)bean;
                    if (owbBean.getProducer() instanceof AbstractProducer)
                    {
                        AbstractProducer<T> producer = (AbstractProducer<T>)owbBean.getProducer();
                        AnnotatedType<T> annotatedType;
                        if (owbBean instanceof InjectionTargetBean)
                        {
                            annotatedType = ((InjectionTargetBean<T>)owbBean).getAnnotatedType();
                        }
                        else
                        {
                            annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(owbBean.getReturnType());
                        }
                        producer.defineInterceptorStack(owbBean, annotatedType, webBeansContext);
                    }
                }
                
                //Check passivation scope
                checkPassivationScope(bean);
                                
                //Bean injection points
                Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
                                
                //Check injection points
                if(injectionPoints != null)
                {
                    for (InjectionPoint injectionPoint : injectionPoints)
                    {
                        if(!injectionPoint.isDelegate())
                        {
                            manager.validate(injectionPoint);   
                        }
                        else
                        {
                            if(!bean.getBeanClass().isAnnotationPresent(javax.decorator.Decorator.class)
                                    && !webBeansContext.getDecoratorsManager().containsCustomDecoratorClass(bean.getBeanClass()))
                            {
                                throw new WebBeansConfigurationException(
                                        "Delegate injection points can not defined by beans that are not decorator. Injection point : "
                                        + injectionPoint);
                            }
                        }
                    }                    
                }
            }
            
            //Validate Bean names
            validateBeanNames(beanNames);
            
            //Clear Names
            beanNames.clear();
        }
        
    }

    private void validateBeanNames(Stack<String> beanNames)
    {
        if(beanNames.size() > 0)
        {   
            for(String beanName : beanNames)
            {
                for(String other : beanNames)
                {
                    String part = null;
                    int i = beanName.lastIndexOf('.');
                    if(i != -1)
                    {
                        part = beanName.substring(0,i);                
                    }
                    
                    if(beanName.equals(other))
                    {
                        InjectionResolver resolver = webBeansContext.getBeanManagerImpl().getInjectionResolver();
                        Set<Bean<?>> beans = resolver.implResolveByName(beanName);
                        if(beans.size() > 1)
                        {
                            beans = resolver.findByAlternatives(beans);                            
                            if(beans.size() > 1)
                            {
                                InjectionExceptionUtil.throwAmbiguousResolutionExceptionForBeanName(beans, beanName);
                            }   
                        }
                    }
                    else
                    {
                        if(part != null)
                        {
                            if(part.equals(other))
                            {
                                throw new WebBeansConfigurationException("EL name of one bean is of the form x.y, where y is a valid bean EL name, and " +
                                        "x is the EL name of the other bean for the bean name : " + beanName);
                            }                        
                        }
                    }
                }                
            }            
        }
    }
    
    
    
    /**
     * Discovers and deploys classes from class path.
     * 
     * @param scanner discovery scanner
     * @throws ClassNotFoundException if class not found
     */
    protected void deployFromClassPath(ScannerService scanner) throws ClassNotFoundException
    {
        logger.fine("Deploying configurations from class files has started.");

        // Start from the class
        Set<Class<?>> classIndex = scanner.getBeanClasses();
        
        //Iterating over each class
        if (classIndex != null)
        {
            AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();

            for(Class<?> implClass : classIndex)
            {
                //Define annotation type
                AnnotatedType<?> annotatedType = annotatedElementFactory.newAnnotatedType(implClass);
                
                if (null != annotatedType)
                {
                    deploySingleAnnotatedType(implClass, annotatedType);

                    // if the implClass already gets processed as part of the
                    // standard BDA scanning, then we don't need to 'additionally'
                    // deploy it anymore.
                    webBeansContext.getBeanManagerImpl().removeAdditionalAnnotatedType(annotatedType);
                } 
                else
                {
                    if (logger.isLoggable(Level.FINE))
                    {
                        logger.fine("Error creating managed bean " + implClass);
                    }

                }

            }
        }

        logger.fine("Deploying configurations from class files has ended.");

    }
    
    /**
     * Deploys any AnnotatedTypes added to the BeanManager during beforeBeanDiscovery.
     */
    private void deployAdditionalAnnotatedTypes()
    {
        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();

        Collection<AnnotatedType<?>> annotatedTypes = beanManager.getAdditionalAnnotatedTypes();
        
        for(AnnotatedType<?> type : annotatedTypes)
        {
            Class implClass = type.getJavaClass();

            deploySingleAnnotatedType(implClass, type);
        }
    }

    /**
     * Common helper method used to deploy annotated types discovered through
     * scanning or during beforeBeanDiscovery.
     * 
     * @param implClass the class of the bean to be deployed
     * @param annotatedType the AnnotatedType representing the bean to be deployed
     */
    private void deploySingleAnnotatedType(Class implClass, AnnotatedType annotatedType)
    {
        // Fires ProcessAnnotatedType
        ProcessAnnotatedTypeImpl<?> processAnnotatedEvent =
            webBeansContext.getWebBeansUtil().fireProcessAnnotatedTypeEvent(annotatedType);

        // if veto() is called
        if (processAnnotatedEvent.isVeto())
        {
            return;
        }

        Class beanClass = processAnnotatedEvent.getAnnotatedType().getJavaClass();

        // EJBs can be defined so test them really before going for a ManagedBean
        if (discoverEjb && EJBWebBeansConfigurator.isSessionBean(implClass, webBeansContext))
        {
            logger.log(Level.FINE, "Found Enterprise Bean with class name : [{0}]", implClass.getName());
            defineEnterpriseWebBean((Class<Object>) implClass, (ProcessAnnotatedTypeImpl<Object>) processAnnotatedEvent);
        }
        else
        {
            try
            {
                if((ClassUtil.isConcrete(beanClass) || WebBeansUtil.isDecorator(processAnnotatedEvent.getAnnotatedType()))
                        && isValidManagedBean(processAnnotatedEvent.getAnnotatedType()))
                {
                    defineManagedBean(processAnnotatedEvent);
                }
            }
            catch (NoClassDefFoundError ncdfe)
            {
                logger.info("Skipping deployment of Class " + implClass + "due to a NoClassDefFoundError: " + ncdfe.getMessage());
            }
        }
    }

    private boolean isValidManagedBean(final AnnotatedType<?> type)
    {
        final Class<?> beanClass = type.getJavaClass();
        final WebBeansUtil webBeansUtil = webBeansContext.getWebBeansUtil();

        // done separately to be able to swallow the logging when not relevant and avoid to pollute logs
        if (!webBeansUtil.isConstructorOk(beanClass))
        {
            if (isNormalScoped(type))
            {
                logger.info("Bean implementation class : " + beanClass.getName() + " must define at least one Constructor");
            } // else not an issue
            return false;
        }

        try
        {
            webBeansUtil.checkManagedBean(beanClass);
        }
        catch (final DefinitionException e)
        {
            logger.log(Level.FINE, "skipped deployment of: " + beanClass.getName() + " reason: " + e.getMessage());
            logger.log(Level.FINER, "skipped deployment of: " + beanClass.getName() + " details: ", e);
            return false;
        }
        //we are not allowed to catch possible exceptions thrown by the following method
        webBeansUtil.checkManagedBeanCondition(beanClass);
        return true;
    }

    private static boolean isNormalScoped(final AnnotatedType<?> type)
    {
        final Set<Annotation> annotations = type.getAnnotations();
        if (annotations != null)
        {
            for (final Annotation a : annotations)
            {
                if (AnnotationUtil.hasMetaAnnotation(a.annotationType().getAnnotations(), NormalScope.class)
                        || AnnotationUtil.hasAnnotation(a.annotationType().getAnnotations(), NormalScope.class))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Discovers and deploys alternatives, interceptors and decorators from XML.
     * 
     * @param scanner discovery scanner
     *
     * @throws WebBeansDeploymentException if a problem occurs
     */
    protected void deployFromXML(ScannerService scanner) throws WebBeansDeploymentException
    {
        logger.fine("Deploying configurations from XML files has started.");

        Set<URL> xmlLocations = scanner.getBeanXmls();
        Iterator<URL> it = xmlLocations.iterator();

        while (it.hasNext())
        {
            URL url = it.next();

            if (logger.isLoggable(Level.FINE))
            {
                logger.fine("OpenWebBeans BeansDeployer configuring: " + url.toExternalForm());
            }

            InputStream fis = null;
            try
            {
                fis = url.openStream();

                xmlConfigurator.configure(fis, url.toExternalForm(), scanner);
            }
            catch (IOException e)
            {
                throw new WebBeansDeploymentException("Error configuring: filename: " + url.toExternalForm() , e);
            }
            finally
            {
                if (fis != null)
                {
                    try
                    {
                        fis.close();
                    }
                    catch (IOException e)
                    {
                        // all ok, ignore this!
                    }
                }
            }
        }

        if(logger.isLoggable(Level.FINE))
        {
            logger.fine("Deploying configurations from XML has ended successfully.");
        }
    }

    /**
     * Checks specialization.
     * @param scanner scanner instance
     */
    protected void processSpecializations(ScannerService scanner)
    {
        logger.fine("Checking Specialization constraints has started.");
        
        try
        {
            Set<Class<?>> beanClasses = scanner.getBeanClasses();
            if (beanClasses != null && beanClasses.size() > 0)
            {
                //superClassList is used to handle the case: Car, CarToyota, Bus, SchoolBus, CarFord
                //for which case, the owb should throw exception that both CarToyota and CarFord are 
                //specialize Car. 
                Class<?> superClass;
                ArrayList<Class<?>> superClassList = new ArrayList<Class<?>>();
                ArrayList<Class<?>> specialClassList = new ArrayList<Class<?>>();
                for(Class<?> specialClass : beanClasses)
                {
                    if(AnnotationUtil.hasClassAnnotation(specialClass, Specializes.class))
                    {
                        superClass = specialClass.getSuperclass();
                        if(superClass.equals(Object.class))
                        {
                            throw new WebBeansConfigurationException(WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0003) + specialClass.getName()
                                                                     + WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0004));
                        }
                        if (superClassList.contains(superClass))
                        {
                            // since CDI 1.1 we have to wrap this in a DeploymentException
                            InconsistentSpecializationException exception
                                = new InconsistentSpecializationException(WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0005) + superClass.getName());
                            throw new WebBeansDeploymentException(exception);
                        }
                        superClassList.add(superClass);
                        specialClassList.add(specialClass);
                    }
                }
                webBeansContext.getWebBeansUtil().configureSpecializations(specialClassList);
            }


            //configure specialized producer beans.
            webBeansContext.getWebBeansUtil().configureProducerMethodSpecializations();

            removeProducersInDisabledBeans();
        }
        catch (DefinitionException e)
        {
            throw e;
        }
        catch (DeploymentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new WebBeansDeploymentException(e);
        }
        

        logger.fine("Checking Specialization constraints has ended.");
    }

    protected void removeProducersInDisabledBeans()
    {
        for (final Bean<?> bean : webBeansContext.getBeanManagerImpl().getBeans())
        {
            if (bean instanceof AbstractProducerBean)
            {
                final AbstractProducerBean<?> producerBean = (AbstractProducerBean<?>) bean;
                final InjectionTargetBean<?> ownerBean = producerBean.getOwnerBean();
                if (!ownerBean.isEnabled())
                {
                    // if the parent component is disabled, then we also need to disabled the producer fields and methods in it as well.
                    producerBean.setEnabled(false);
                }
            }
        }
    }



    /**
     * Check passivations.
     */
    protected void checkPassivationScope(Bean<?> beanObj)
    {
        boolean validate = false;
        
        if(beanObj instanceof EnterpriseBeanMarker)
        {
            EnterpriseBeanMarker marker = (EnterpriseBeanMarker)beanObj;
            if(marker.isPassivationCapable())
            {
                validate = true;   
            }
        }        
        else if(webBeansContext.getBeanManagerImpl().isPassivatingScope(beanObj.getScope()))
        {
            if(WebBeansUtil.getPassivationId(beanObj) == null)
            {
                if(!(beanObj instanceof AbstractProducerBean))
                {
                    throw new WebBeansConfigurationException("Passivation scoped defined bean must be passivation capable, " +
                            "but bean : " + beanObj.toString() + " is not passivation capable");
                }
            }

            validate = true;
        }
        
        if(validate)
        {
            webBeansContext.getDeploymentValidationService().validatePassivationCapable((OwbBean<?>)beanObj);
        }
    }

    /**
     * Check steretypes.
     * @param scanner scanner instance
     */
    protected void checkStereoTypes(ScannerService scanner)
    {
        logger.fine("Checking StereoType constraints has started.");

        addDefaultStereoTypes();

        final AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        
        Set<Class<?>> beanClasses = scanner.getBeanClasses();
        if (beanClasses != null && beanClasses.size() > 0)
        {
            for(Class<?> beanClass : beanClasses)
            {                
                if(beanClass.isAnnotation())
                {
                    Class<? extends Annotation> stereoClass = (Class<? extends Annotation>) beanClass;
                    if (annotationManager.isStereoTypeAnnotation(stereoClass))
                    {
                        webBeansContext.getAnnotationManager().checkStereoTypeClass(stereoClass, stereoClass.getDeclaredAnnotations());
                        StereoTypeModel model = new StereoTypeModel(webBeansContext, stereoClass);
                        webBeansContext.getStereoTypeManager().addStereoTypeModel(model);
                    }
                }
            }
        }

        logger.fine("Checking StereoType constraints has ended.");
    }

    /**
     * Adds default stereotypes.
     */
    protected void addDefaultStereoTypes()
    {
        StereoTypeModel model = new StereoTypeModel(webBeansContext, Model.class);
        webBeansContext.getStereoTypeManager().addStereoTypeModel(model);
    }

    /**
     * Defines and configures managed bean.
     * @param <T> type info
     */
    protected <T> void defineManagedBean(ProcessAnnotatedTypeImpl<T> processAnnotatedEvent)
    {   
        //Bean manager
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        
        //Create an annotated type
        AnnotatedType<T> annotatedType = processAnnotatedEvent.getAnnotatedType();
                                
        //Fires ProcessInjectionTarget event for Java EE components instances
        //That supports injections but not managed beans
        ProcessInjectionTargetImpl<T> processInjectionTargetEvent;
        Class beanClass = processAnnotatedEvent.getAnnotatedType().getJavaClass();
        if(webBeansContext.getWebBeansUtil().supportsJavaEeComponentInjections(beanClass))
        {
            //Fires ProcessInjectionTarget
            processInjectionTargetEvent =
                webBeansContext.getWebBeansUtil().fireProcessInjectionTargetEventForJavaEeComponents(beanClass);
            webBeansContext.getWebBeansUtil().inspectErrorStack(
                "There are errors that are added by ProcessInjectionTarget event observers. Look at logs for further details");

            //Checks that not contains @Inject InjectionPoint
            webBeansContext.getAnnotationManager().checkInjectionPointForInjectInjectionPoint(beanClass);
        }

        {
            BeanAttributesImpl<T> beanAttributes = BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(annotatedType).build();

            ManagedBeanBuilder<T, ManagedBean<T>> managedBeanCreator = new ManagedBeanBuilder<T, ManagedBean<T>>(webBeansContext, annotatedType, beanAttributes);

            if(WebBeansUtil.isDecorator(annotatedType))
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Found Managed Bean Decorator with class name : [{0}]", annotatedType.getJavaClass().getName());
                }
                DecoratorBeanBuilder<T> dbb = new DecoratorBeanBuilder<T>(webBeansContext, annotatedType, beanAttributes);
                if (dbb.isDecoratorEnabled())
                {
                    dbb.defineDecoratorRules();
                    DecoratorBean<T> decorator = dbb.getBean();
                    webBeansContext.getDecoratorsManager().addDecorator(decorator);
                }
            }
            else if(WebBeansUtil.isCdiInterceptor(annotatedType))
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Found Managed Bean Interceptor with class name : [{0}]", annotatedType.getJavaClass().getName());
                }
                
                CdiInterceptorBeanBuilder<T> ibb = new CdiInterceptorBeanBuilder<T>(webBeansContext, annotatedType, beanAttributes);
                if (ibb.isInterceptorEnabled())
                {
                    ibb.defineCdiInterceptorRules();
                    CdiInterceptorBean<T> interceptor = ibb.getBean();
                    webBeansContext.getInterceptorsManager().addCdiInterceptor(interceptor);
                }
            }
            else
            {
                InjectionTargetBean<T> bean = managedBeanCreator.getBean();

                if (webBeansContext.getDecoratorsManager().containsCustomDecoratorClass(annotatedType.getJavaClass()) ||
                    webBeansContext.getInterceptorsManager().containsCustomInterceptorClass(annotatedType.getJavaClass()))
                {
                    return; //TODO discuss this case (it was ignored before)
                }
                
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Found Managed Bean with class name : [{0}]", annotatedType.getJavaClass().getName());
                }

                Set<ObserverMethod<?>> observerMethods = new HashSet<ObserverMethod<?>>();
                if(bean.isEnabled())
                {
                    observerMethods = new ObserverMethodsBuilder<T, InjectionTargetBean<T>>(webBeansContext, bean.getAnnotatedType()).defineObserverMethods(bean);
                }
                Set<ProducerMethodBean<?>> producerMethods = new ProducerMethodBeansBuilder(bean.getWebBeansContext(), bean.getAnnotatedType()).defineProducerMethods(bean);
                Set<ProducerFieldBean<?>> producerFields = new ProducerFieldBeansBuilder(bean.getWebBeansContext(), bean.getAnnotatedType()).defineProducerFields(bean);

                ManagedBean<T> managedBean = (ManagedBean<T>)bean;
                Map<ProducerMethodBean<?>,AnnotatedMethod<?>> annotatedMethods =
                        new HashMap<ProducerMethodBean<?>, AnnotatedMethod<?>>();

                for(ProducerMethodBean<?> producerMethod : producerMethods)
                {
                    AnnotatedMethod<?> method = webBeansContext.getAnnotatedElementFactory().newAnnotatedMethod(producerMethod.getCreatorMethod(), annotatedType);
                    webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessProducer event observers for "
                            + "ProducerMethods. Look at logs for further details");

                    annotatedMethods.put(producerMethod, method);
                }

                Map<ProducerFieldBean<?>,AnnotatedField<?>> annotatedFields =
                        new HashMap<ProducerFieldBean<?>, AnnotatedField<?>>();

                for(ProducerFieldBean<?> producerField : producerFields)
                {
                    webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessProducer event observers for"
                            + " ProducerFields. Look at logs for further details");

                    annotatedFields.put(producerField,
                            webBeansContext.getAnnotatedElementFactory().newAnnotatedField(
                                producerField.getCreatorField(),
                                webBeansContext.getAnnotatedElementFactory().newAnnotatedType(producerField.getBeanClass())));
                }

                Map<ObserverMethod<?>,AnnotatedMethod<?>> observerMethodsMap =
                        new HashMap<ObserverMethod<?>, AnnotatedMethod<?>>();

                for(ObserverMethod<?> observerMethod : observerMethods)
                {
                    ObserverMethodImpl<?> impl = (ObserverMethodImpl<?>)observerMethod;
                    AnnotatedMethod<?> method = impl.getObserverMethod();

                    observerMethodsMap.put(observerMethod, method);
                }

                BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();

                //Fires ProcessManagedBean
                ProcessBeanImpl<T> processBeanEvent = new GProcessManagedBean(managedBean, annotatedType);
                beanManager.fireEvent(processBeanEvent);
                webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessManagedBean event observers for " +
                        "managed beans. Look at logs for further details");

                //Fires ProcessProducerMethod
                webBeansContext.getWebBeansUtil().fireProcessProducerMethodBeanEvent(annotatedMethods, annotatedType);
                webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessProducerMethod event observers for " +
                        "producer method beans. Look at logs for further details");

                //Fires ProcessProducerField
                webBeansContext.getWebBeansUtil().fireProcessProducerFieldBeanEvent(annotatedFields);
                webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessProducerField event observers for " +
                        "producer field beans. Look at logs for further details");

                //Fire ObservableMethods
                webBeansContext.getWebBeansUtil().fireProcessObservableMethodBeanEvent(observerMethodsMap);
                webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessObserverMethod event observers for " +
                        "observer methods. Look at logs for further details");

                if(!webBeansContext.getWebBeansUtil().isAnnotatedTypeDecoratorOrInterceptor(annotatedType))
                {
                    beanManager.addBean(bean);
                    for (ProducerMethodBean<?> producerMethod : producerMethods)
                    {
                        // add them one after the other to enable serialization handling et al
                        beanManager.addBean(producerMethod);
                    }
                    for (ProducerFieldBean<?> producerField : producerFields)
                    {
                        // add them one after the other to enable serialization handling et al
                        beanManager.addBean(producerField);
                    }
                }
            }
        }
    }
    
    /**
     * Defines enterprise bean via plugin.
     * @param <T> bean class type
     * @param clazz bean class
     */
    protected <T> void defineEnterpriseWebBean(Class<T> clazz, ProcessAnnotatedType<T> processAnnotatedTypeEvent)
    {
        InjectionTargetBean<T> bean = (InjectionTargetBean<T>) EJBWebBeansConfigurator.defineEjbBean(clazz, processAnnotatedTypeEvent,
                                                                                                     webBeansContext);
        webBeansContext.getWebBeansUtil().setInjectionTargetBeanEnableFlag(bean);
    }
}
