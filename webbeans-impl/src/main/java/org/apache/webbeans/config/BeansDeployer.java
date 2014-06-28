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
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.portable.events.ProcessBeanAttributesImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.WebBeansException;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.AbstractProducer;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.ProcessAnnotatedTypeImpl;
import org.apache.webbeans.portable.events.ProcessBeanImpl;
import org.apache.webbeans.portable.events.ProcessInjectionTargetImpl;
import org.apache.webbeans.portable.events.ProcessSyntheticAnnotatedTypeImpl;
import org.apache.webbeans.portable.events.discovery.AfterBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.discovery.AfterDeploymentValidationImpl;
import org.apache.webbeans.portable.events.discovery.AfterTypeDiscoveryImpl;
import org.apache.webbeans.portable.events.discovery.BeforeBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.generics.GProcessBeanAttributes;
import org.apache.webbeans.portable.events.generics.GProcessManagedBean;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.plugins.OpenWebBeansJavaEEPlugin;
import org.apache.webbeans.spi.plugins.OpenWebBeansWebPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.webbeans.util.InjectionExceptionUtil;
import org.apache.webbeans.util.SpecializationUtil;
import org.apache.webbeans.util.WebBeansConstants;
import org.apache.webbeans.util.WebBeansUtil;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Model;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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

    private static final Method GET_PACKAGE;
    static
    {
        try
        {
            GET_PACKAGE = ClassLoader.class.getDeclaredMethod("getPackage", String.class);
            GET_PACKAGE.setAccessible(true);
        }
        catch (final NoSuchMethodException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**Deployment is started or not*/
    protected boolean deployed = false;

    /**XML Configurator*/
    protected BeanArchiveService beanArchiveService;
    
    /**Discover ejb or not*/
    protected boolean discoverEjb = false;
    private final WebBeansContext webBeansContext;

    private final ScannerService scannerService;
    private final DecoratorsManager decoratorsManager;
    private final InterceptorsManager interceptorsManager;

    private final Map<String, Boolean> packageVetoCache = new HashMap<String, Boolean>();

    /**
     * Creates a new deployer with given xml configurator.
     * 
     * @param webBeansContext
     */
    public BeansDeployer(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        beanArchiveService = webBeansContext.getBeanArchiveService();
        scannerService = webBeansContext.getScannerService();
        decoratorsManager = webBeansContext.getDecoratorsManager();
        interceptorsManager = webBeansContext.getInterceptorsManager();

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

                List<AnnotatedType<?>> annotatedTypes = annotatedTypesFromClassPath(scanner);

                //Deploy additional Annotated Types
                addAdditionalAnnotatedTypes(annotatedTypes);

                registerAlternativesDecoratorsAndInterceptorsWithPriority(annotatedTypes);

                fireAfterTypeDiscoveryEvent();

                // Handle Specialization
                removeSpecializedTypes(annotatedTypes);

                // create beans from the discovered AnnotatedTypes
                deployFromAnnotatedTypes(annotatedTypes);


                //X TODO configure specialized producer beans.
                webBeansContext.getWebBeansUtil().configureProducerMethodSpecializations();

                // all beans which got 'overridden' by a Specialized version can be removed now
                removeDisabledBeans();
                
                // We are finally done with our bean discovery
                fireAfterBeanDiscoveryEvent();
                
                // Validate injection Points
                validateInjectionPoints();
                
                // fire event
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
     * Remove all beans which are not enabled anymore.
     * This might e.g. happen because they are 'overridden'
     * by a &#064;Specialized bean.
     * We remove those beans now to not having to take care later
     * during {@link org.apache.webbeans.container.BeanManagerImpl#resolve(java.util.Set)}
     */
    private void removeDisabledBeans()
    {
        Iterator<Bean<?>> beans = webBeansContext.getBeanManagerImpl().getBeans().iterator();
        while(beans.hasNext())
        {
            Bean<?> bean = beans.next();
            if (!((OwbBean) bean).isEnabled())
            {
                beans.remove();
            }
        }
    }

    private void registerAlternativesDecoratorsAndInterceptorsWithPriority(List<AnnotatedType<?>> annotatedTypes)
    {
        AlternativesManager alternativesManager = webBeansContext.getAlternativesManager();

        for (AnnotatedType<?> annotatedType : annotatedTypes)
        {
            if (annotatedType.getAnnotation(Alternative.class) != null)
            {
                Priority priority = annotatedType.getAnnotation(Priority.class);
                if (priority != null)
                {
                    alternativesManager.addPriorityClazzAlternative(annotatedType.getJavaClass(), priority);
                }
            }

            //X TODO handle interceptors and decorators as well
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
        beanManager.addInternalBean(webBeansUtil.getEventMetadataBean());
        
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
     * Fires event after bean discovery.
     */
    private void fireAfterTypeDiscoveryEvent()
    {
        AlternativesManager alternativesManager = webBeansContext.getAlternativesManager();
        List<Class<?>> sortedAlternatives = alternativesManager.getPrioritizedAlternatives();

        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        manager.fireLifecycleEvent(new AfterTypeDiscoveryImpl(webBeansContext, sortedAlternatives));
        // we do not need to set back the sortedAlternatives to the AlternativesManager as the API
        // and all layers in between use a mutable List. Not very elegant but spec conform.

        webBeansContext.getWebBeansUtil().inspectErrorStack(
            "There are errors that are added by AfterTypeDiscovery event observers. Look at logs for further details");
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

        packageVetoCache.clear(); // no more needed, free the memory
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
                            try
                            {
                                resolver.resolve(beans);
                            }
                            catch(AmbiguousResolutionException are)
                            {
                                // throw the Exception with even more information
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
     * Create AnnotatedTypes from the ClassPath via the ScannerService
     */
    private List<AnnotatedType<?>> annotatedTypesFromClassPath(ScannerService scanner)
    {
        logger.fine("Creating AnnotatedTypes from class files has started.");

        // Start from the class
        Set<Class<?>> classIndex = scanner.getBeanClasses();

        List<AnnotatedType<?>> annotatedTypes = new ArrayList<AnnotatedType<?>>();

        //Iterating over each class
        if (classIndex != null)
        {
            AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();

            for (Class<?> implClass : classIndex)
            {
                if (isVetoed(implClass))
                {
                    continue;
                }

                try
                {
                    //Define annotation type
                    AnnotatedType<?> annotatedType = annotatedElementFactory.newAnnotatedType(implClass);

                    if (annotatedType == null)
                    {
                        logger.info("Could not create AnnotatedType for class " + implClass);
                        continue;
                    }

                    // Fires ProcessAnnotatedType
                    ProcessAnnotatedTypeImpl<?> processAnnotatedEvent =
                            webBeansContext.getWebBeansUtil().fireProcessAnnotatedTypeEvent(annotatedType);

                    // if veto() is called
                    if (!processAnnotatedEvent.isVeto())
                    {
                        annotatedTypes.add(processAnnotatedEvent.getAnnotatedType());
                    }
                }
                catch (NoClassDefFoundError ncdfe)
                {
                    logger.info("Skipping deployment of Class " + implClass + "due to a NoClassDefFoundError: " + ncdfe.getMessage());
                }
            }
        }

        return annotatedTypes;
    }

    private boolean isVetoed(final Class<?> implClass)
    {
        if (implClass.getAnnotation(Vetoed.class) != null)
        {
            return true;
        }

        ClassLoader classLoader = implClass.getClassLoader();
        if (classLoader == null)
        {
            classLoader = BeansDeployer.class.getClassLoader();
        }

        Package pckge = implClass.getPackage();
        do
        {
            // yes we cache result with potentially different classloader but this is not portable by spec
            final String name = pckge.getName();
            {
                final Boolean result = packageVetoCache.get(name);
                if (result != null && result)
                {
                    return result;
                }
            }
            if (pckge.getAnnotation(Vetoed.class) != null)
            {
                packageVetoCache.put(pckge.getName(), true);
                return true;
            }
            else
            {
                packageVetoCache.put(pckge.getName(), false);
            }

            final int idx = name.lastIndexOf('.');
            if (idx > 0)
            {
                final String previousPackage = name.substring(0, idx);
                final Boolean result = packageVetoCache.get(previousPackage);
                if (result != null && result)
                {
                    return result;
                }
                try // this is related to classloader and not to Package actually :( so we need reflection
                {
                    pckge = Package.class.cast(GET_PACKAGE.invoke(classLoader, previousPackage));
                }
                catch (final Exception e)
                {
                    throw new IllegalStateException(e);
                }
            }
            else
            {
                pckge = null;
            }
        } while (pckge != null);

        return false;
    }

    /**
     * Process any AnnotatedTypes which got added by BeforeBeanDiscovery#addAnnotatedType
     * @param annotatedTypes
     */
    private void addAdditionalAnnotatedTypes(List<AnnotatedType<?>> annotatedTypes)
    {
        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();


        Collection<AnnotatedType<?>> additionalAnnotatedTypes = beanManager.getAdditionalAnnotatedTypes();

        for (AnnotatedType<?> annotatedType : additionalAnnotatedTypes)
        {
            // Fires ProcessAnnotatedType
            ProcessSyntheticAnnotatedTypeImpl<?> processAnnotatedEvent =
                    webBeansContext.getWebBeansUtil().fireProcessSyntheticAnnotatedTypeEvent(annotatedType);

            if (!processAnnotatedEvent.isVeto())
            {
                AnnotatedType<?> changedAnnotatedType = processAnnotatedEvent.getAnnotatedType();
                if (annotatedTypes.contains(changedAnnotatedType))
                {
                    annotatedTypes.remove(changedAnnotatedType);
                }
                annotatedTypes.add(changedAnnotatedType);
            }

        }
    }


    /**
     * Discovers and deploys classes from class path.
     * 
     * @param annotatedTypes the AnnotatedTypes which got discovered so far and are not vetoed
     * @throws ClassNotFoundException if class not found
     */
    protected void deployFromAnnotatedTypes(List<AnnotatedType<?>> annotatedTypes) throws ClassNotFoundException
    {
        logger.fine("Deploying configurations from class files has started.");

        // Start from the class
        for(AnnotatedType<?> annotatedType : annotatedTypes)
        {
            try
            {
                deploySingleAnnotatedType(annotatedType);
            }
            catch (NoClassDefFoundError ncdfe)
            {
                logger.info("Skipping deployment of Class " + annotatedType.getJavaClass() + "due to a NoClassDefFoundError: " + ncdfe.getMessage());
            }

            // if the implClass already gets processed as part of the
            // standard BDA scanning, then we don't need to 'additionally'
            // deploy it anymore.
            webBeansContext.getBeanManagerImpl().removeAdditionalAnnotatedType(annotatedType);

        }

        logger.fine("Deploying configurations from class files has ended.");

    }
    

    /**
     * Common helper method used to deploy annotated types discovered through
     * scanning or during beforeBeanDiscovery.
     * 
     * @param annotatedType the AnnotatedType representing the bean to be deployed
     */
    private void deploySingleAnnotatedType(AnnotatedType annotatedType)
    {

        Class beanClass = annotatedType.getJavaClass();

        // EJBs can be defined so test them really before going for a ManagedBean
        if (discoverEjb && EJBWebBeansConfigurator.isSessionBean(beanClass, webBeansContext))
        {
            logger.log(Level.FINE, "Found Enterprise Bean with class name : [{0}]", beanClass.getName());
            defineEnterpriseWebBean((Class<Object>) beanClass, annotatedType);
        }
        else
        {
            try
            {
                if((ClassUtil.isConcrete(beanClass) || WebBeansUtil.isDecorator(annotatedType))
                        && isValidManagedBean(annotatedType))
                {
                    defineManagedBean(annotatedType);
                }
            }
            catch (NoClassDefFoundError ncdfe)
            {
                logger.info("Skipping deployment of Class " + beanClass + "due to a NoClassDefFoundError: " + ncdfe.getMessage());
            }
        }
    }

    private boolean isValidManagedBean(final AnnotatedType<?> type)
    {
        final Class<?> beanClass = type.getJavaClass();
        final WebBeansUtil webBeansUtil = webBeansContext.getWebBeansUtil();

        // done separately to be able to swallow the logging when not relevant and avoid to pollute logs
        if (!webBeansUtil.isConstructorOk(type))
        {
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

        Set<URL> bdaLocations = scanner.getBeanXmls();
        Iterator<URL> it = bdaLocations.iterator();

        while (it.hasNext())
        {
            URL url = it.next();

            logger.fine("OpenWebBeans BeansDeployer configuring: " + url.toExternalForm());

            BeanArchiveService.BeanArchiveInformation beanArchiveInformation = beanArchiveService.getBeanArchiveInformation(url);

            configureDecorators(url, beanArchiveInformation.getDecorators());
            configureInterceptors(url, beanArchiveInformation.getInterceptors());
            configureAlternatives(url, beanArchiveInformation.getAlternativeClasses(), false);
            configureAlternatives(url, beanArchiveInformation.getAlternativeStereotypes(), true);
        }

        logger.fine("Deploying configurations from XML has ended successfully.");
    }

    private void configureAlternatives(URL bdaLocation, List<String> alternatives, boolean isStereotype)
    {
        // the alternatives in this beans.xml
        // this gets used to detect multiple definitions of the
        // same alternative in one beans.xml file.
        Set<String> alternativesInFile = new HashSet<String>();

        for (String alternativeName : alternatives)
        {
            if (alternativesInFile.contains(alternativeName))
            {
                throw new WebBeansConfigurationException(createConfigurationFailedMessage(bdaLocation) + "Given alternative : " + alternativeName
                        + " is already added as @Alternative" );
            }
            alternativesInFile.add(alternativeName);

            Class clazz = ClassUtil.getClassFromName(alternativeName);

            if (clazz == null)
            {
                throw new WebBeansConfigurationException(createConfigurationFailedMessage(bdaLocation) + "Alternative: " + alternativeName + " not found");
            }
            else
            {
                AlternativesManager manager = WebBeansContext.getInstance().getAlternativesManager();
                if (isStereotype)
                {
                    manager.addXmlStereoTypeAlternative(clazz);
                }
                else
                {
                    manager.addXmlClazzAlternative(clazz);
                }
            }
        }
    }

    private void configureDecorators(URL bdaLocation, List<String> decorators)
    {
        Set<Class> decoratorsInFile = new HashSet<Class>();

        for (String decorator : decorators)
        {
            Class<?> clazz = ClassUtil.getClassFromName(decorator);

            if (clazz == null)
            {
                throw new WebBeansConfigurationException(createConfigurationFailedMessage(bdaLocation) + "Decorator class : " +
                        decorator + " not found");
            }
            else
            {
                if ((scannerService.isBDABeansXmlScanningEnabled() && !scannerService.getBDABeansXmlScanner().addDecorator(clazz, bdaLocation.toExternalForm())) ||
                        decoratorsInFile.contains(clazz))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage(bdaLocation) + "Decorator class : " +
                            decorator + " is already defined");
                }

                decoratorsManager.addEnabledDecorator(clazz);
                decoratorsInFile.add(clazz);
            }
        }
    }

    private void configureInterceptors(URL bdaLocation, List<String> interceptors)
    {
        // the interceptors in this beans.xml
        // this gets used to detect multiple definitions of the
        // same interceptor in one beans.xml file.
        Set<Class> interceptorsInFile = new HashSet<Class>();
        
        for (String interceptor : interceptors)
        {
            Class<?> clazz = ClassUtil.getClassFromName(interceptor);

            if (clazz == null)
            {
                throw new WebBeansConfigurationException(createConfigurationFailedMessage(bdaLocation) + "Interceptor class : " +
                        interceptor + " not found");
            }
            else
            {
                Annotation[] classAnnotations;
                AnnotatedType<?> annotatedType = webBeansContext.getAnnotatedElementFactory().getAnnotatedType(clazz);
                if (annotatedType == null)
                {
                    annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(clazz);
                }

                ProcessAnnotatedTypeImpl<?> processAnnotatedEvent =
                        webBeansContext.getWebBeansUtil().fireProcessAnnotatedTypeEvent(annotatedType);

                // if veto() is called
                if (processAnnotatedEvent.isVeto())
                {
                    return;
                }

                annotatedType = processAnnotatedEvent.getAnnotatedType();

                Set<Annotation> annTypeAnnotations = annotatedType.getAnnotations();
                if (annTypeAnnotations != null)
                {
                    classAnnotations = annTypeAnnotations.toArray(new Annotation[annTypeAnnotations.size()]);
                }
                else
                {
                    classAnnotations = new Annotation[0];
                }

                if (AnnotationUtil.hasAnnotation(classAnnotations, javax.interceptor.Interceptor.class) &&
                        !webBeansContext.getAnnotationManager().hasInterceptorBindingMetaAnnotation(classAnnotations))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage(bdaLocation) + "Interceptor class : "
                            + interceptor + " must have at least one @InterceptorBinding");
                }

                // check if the interceptor got defined twice in this beans.xml
                if (interceptorsInFile.contains(clazz))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage(bdaLocation) + "Interceptor class : "
                            + interceptor + " already defined in this beans.xml file!");
                }
                interceptorsInFile.add(clazz);

                boolean isBDAScanningEnabled = scannerService.isBDABeansXmlScanningEnabled();
                if ((!isBDAScanningEnabled && interceptorsManager.isInterceptorClassEnabled(clazz)) ||
                        (isBDAScanningEnabled && !scannerService.getBDABeansXmlScanner().addInterceptor(clazz, bdaLocation.toExternalForm())))
                {
                    logger.warning( "Interceptor class : " + interceptor + " is already defined");
                }
                else
                {
                    interceptorsManager.addEnabledInterceptorClass(clazz);
                }
            }
        }
    }


    /**
     * Gets error message for XML parsing of the current XML file.
     *
     * @return the error messages
     */
    private String createConfigurationFailedMessage(URL bdaLocation)
    {
        return "WebBeans configuration defined in " + bdaLocation.toExternalForm() + " did fail. Reason is : ";
    }

    /**
     * Checks specialization on classes and remove any AnnotatedType which got 'disabled' by having a sub-class with &#064;Specializes.
     * @param annotatedTypes the annotatedTypes which got picked up during scanning. All 'disabled' annotatedTypes will be removed.
     */
    private void removeSpecializedTypes(List<AnnotatedType<?>> annotatedTypes)
    {
        logger.fine("Checking Specialization constraints has started.");
        
        try
        {
            SpecializationUtil specializationUtil = new SpecializationUtil(webBeansContext);
            specializationUtil.removeDisabledTypes(annotatedTypes);
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
    protected <T> void defineManagedBean(AnnotatedType<T> annotatedType)
    {   
        //Fires ProcessInjectionTarget event for Java EE components instances
        //That supports injections but not managed beans
        ProcessInjectionTargetImpl<T> processInjectionTargetEvent;
        Class beanClass = annotatedType.getJavaClass();
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
            BeanAttributes<T> beanAttributes = BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(annotatedType).build();

            ManagedBeanBuilder<T, ManagedBean<T>> managedBeanCreator = new ManagedBeanBuilder<T, ManagedBean<T>>(webBeansContext, annotatedType, beanAttributes);

            if(WebBeansUtil.isDecorator(annotatedType))
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Found Managed Bean Decorator with class name : [{0}]", annotatedType.getJavaClass().getName());
                }

                final ProcessBeanAttributesImpl event = fireProcessBeanAttributes(annotatedType, beanAttributes);
                if (!event.isVeto())
                {
                    beanAttributes = updateBeanAttributesIfNeeded(beanAttributes, event);

                    DecoratorBeanBuilder<T> dbb = new DecoratorBeanBuilder<T>(webBeansContext, annotatedType, beanAttributes);
                    if (dbb.isDecoratorEnabled())
                    {
                        dbb.defineDecoratorRules();
                        DecoratorBean<T> decorator = dbb.getBean();
                        webBeansContext.getDecoratorsManager().addDecorator(decorator);
                    }
                }
            }
            else if(WebBeansUtil.isCdiInterceptor(annotatedType))
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Found Managed Bean Interceptor with class name : [{0}]", annotatedType.getJavaClass().getName());
                }

                final ProcessBeanAttributesImpl event = fireProcessBeanAttributes(annotatedType, beanAttributes);
                if (!event.isVeto())
                {
                    beanAttributes = updateBeanAttributesIfNeeded(beanAttributes, event);

                    CdiInterceptorBeanBuilder<T> ibb = new CdiInterceptorBeanBuilder<T>(webBeansContext, annotatedType, beanAttributes);
                    if (ibb.isInterceptorEnabled())
                    {
                        ibb.defineCdiInterceptorRules();
                        CdiInterceptorBean<T> interceptor = ibb.getBean();
                        webBeansContext.getInterceptorsManager().addCdiInterceptor(interceptor);
                    }
                }
            }
            else
            {
                final ProcessBeanAttributesImpl event = fireProcessBeanAttributes(annotatedType, beanAttributes);
                if (!event.isVeto())
                {
                    beanAttributes = updateBeanAttributesIfNeeded(beanAttributes, event);

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
    }

    private <T> BeanAttributes<T> updateBeanAttributesIfNeeded(BeanAttributes<T> beanAttributes, ProcessBeanAttributesImpl event)
    {
        if (event.getDefinitionError() != null)
        {
            throw new DefinitionException(event.getDefinitionError());
        }
        if (event.getAttributes() != beanAttributes)
        {
            beanAttributes = event.getAttributes();
            if (!webBeansContext.getBeanManagerImpl().isScope(beanAttributes.getScope()))
            {
                throw new DefinitionException(beanAttributes.getScope() + " is not a scope");
            }
        }
        return beanAttributes;
    }

    // we don't use bm stack since it is actually quite useless
    private <T> ProcessBeanAttributesImpl fireProcessBeanAttributes(final AnnotatedType<T> annotatedType,
                                                                       final BeanAttributes<T> beanAttributes)
    {
        final ProcessBeanAttributesImpl event = new GProcessBeanAttributes(annotatedType.getJavaClass(), annotatedType, beanAttributes);
        try
        {
            webBeansContext.getBeanManagerImpl().fireEvent(event, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        }
        catch (final Exception e)
        {
            throw new DefinitionException("event ProcessBeanAttributes thrown an exception for " + annotatedType, e);
        }
        return event;
    }

    /**
     * Defines enterprise bean via plugin.
     * @param <T> bean class type
     * @param clazz bean class
     */
    protected <T> void defineEnterpriseWebBean(Class<T> clazz, AnnotatedType<T> annotatedType)
    {
        InjectionTargetBean<T> bean = (InjectionTargetBean<T>) EJBWebBeansConfigurator.defineEjbBean(clazz, annotatedType,
                                                                                                     webBeansContext);
        webBeansContext.getWebBeansUtil().setInjectionTargetBeanEnableFlag(bean);
    }
}
