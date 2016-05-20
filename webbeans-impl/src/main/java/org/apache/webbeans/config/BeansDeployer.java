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
import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.BuiltInOwbBean;
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
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.event.OwbObserverMethod;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.WebBeansException;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.DefinitionException;

import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.AbstractProducer;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.BaseProducerProducer;
import org.apache.webbeans.portable.events.ProcessBeanImpl;
import org.apache.webbeans.portable.events.ProcessSyntheticAnnotatedTypeImpl;
import org.apache.webbeans.portable.events.discovery.AfterBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.discovery.AfterDeploymentValidationImpl;
import org.apache.webbeans.portable.events.discovery.AfterTypeDiscoveryImpl;
import org.apache.webbeans.portable.events.discovery.BeforeBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.generics.GProcessAnnotatedType;
import org.apache.webbeans.portable.events.generics.GProcessManagedBean;
import org.apache.webbeans.spi.BdaScannerService;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.plugins.OpenWebBeansJavaEEPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.webbeans.util.GenericsUtil;
import org.apache.webbeans.util.InjectionExceptionUtil;
import org.apache.webbeans.util.SpecializationUtil;
import org.apache.webbeans.util.WebBeansConstants;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.DefaultBeanArchiveInformation;

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
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.Producer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static org.apache.webbeans.spi.BeanArchiveService.BeanDiscoveryMode;
import static org.apache.webbeans.spi.BeanArchiveService.BeanArchiveInformation;

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
     * This BdaInfo is used for all manually added annotated types or in case
     * a non-Bda-aware ScannerService got configured.
     */
    private final DefaultBeanArchiveInformation defaultBeanArchiveInformation;

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

        defaultBeanArchiveInformation = new DefaultBeanArchiveInformation();
        defaultBeanArchiveInformation.setBeanDiscoveryMode(BeanDiscoveryMode.ALL);
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

                webBeansContext.getBeanManagerImpl().getInjectionResolver().setStartup(true);
                
                //Fire Event
                fireBeforeBeanDiscoveryEvent();
                
                //Configure Default Beans
                configureDefaultBeans();

                Map<BeanArchiveInformation, List<AnnotatedType<?>>> annotatedTypesPerBda = annotatedTypesFromClassPath(scanner);

                List<AnnotatedType<?>> globalBdaAnnotatedTypes = annotatedTypesPerBda.get(defaultBeanArchiveInformation);

                // Deploy additional Annotated Types which got added via BeforeBeanDiscovery#addAnnotatedType
                addAdditionalAnnotatedTypes(webBeansContext.getBeanManagerImpl().getAdditionalAnnotatedTypes(), globalBdaAnnotatedTypes);

                for (List<AnnotatedType<?>> at : annotatedTypesPerBda.values())
                {
                    registerAlternativesDecoratorsAndInterceptorsWithPriority(at);
                }

                addAdditionalAnnotatedTypes(fireAfterTypeDiscoveryEvent(), globalBdaAnnotatedTypes);

                // Also configures deployments, interceptors, decorators.
                deployFromXML(scanner);

                final Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> beanAttributesPerBda
                    = getBeanAttributes(annotatedTypesPerBda);

                // shouldn't be used anymore, view is now beanAttributes
                annotatedTypesPerBda.clear();

                SpecializationUtil specializationUtil = new SpecializationUtil(webBeansContext);
                specializationUtil.removeDisabledBeanAttributes(beanAttributesPerBda, null, true);


                //Checking stereotype conditions
                checkStereoTypes(scanner);

                // Handle Specialization
                specializationUtil.removeDisabledBeanAttributes(
                        beanAttributesPerBda,
                        new SpecializationUtil.BeanAttributesProvider()
                        {
                            @Override
                            public <T> BeanAttributes get(final AnnotatedType<T> at)
                            {
                                ExtendedBeanAttributes<?> data = null;
                                for (Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> beanAttributes : beanAttributesPerBda.values())
                                {
                                    data = beanAttributes.get(at);
                                    if (data != null)
                                    {
                                        break;
                                    }

                                }
                                return data == null ? null : data.beanAttributes;
                            }
                        },
                        false);

                // create beans from the discovered AnnotatedTypes
                deployFromBeanAttributes(beanAttributesPerBda);

                //X TODO configure specialized producer beans.
                webBeansContext.getWebBeansUtil().configureProducerMethodSpecializations();

                // all beans which got 'overridden' by a Specialized version can be removed now
                removeDisabledBeans();
                
                // We are finally done with our bean discovery
                fireAfterBeanDiscoveryEvent();

                validateAlternatives(beanAttributesPerBda);

                validateInjectionPoints();
                validateDisposeParameters();

                validateDecoratorDecoratedTypes();
                validateDecoratorGenericTypes();

                webBeansContext.getBeanManagerImpl().getNotificationManager().clearCaches();

                // fire event
                fireAfterDeploymentValidationEvent();


                // do some cleanup after the deployment
                scanner.release();
                webBeansContext.getAnnotatedElementFactory().clear();
                webBeansContext.getBeanManagerImpl().getInjectionResolver().setStartup(false);
            }
        }
        catch (UnsatisfiedResolutionException e)
        {
            throw new WebBeansDeploymentException(e);
        }
        catch (AmbiguousResolutionException e)
        {
            throw new WebBeansDeploymentException(e);
        }
        catch (UnproxyableResolutionException e)
        {
            // the tck expects a DeploymentException, but it really should be a DefinitionException, see i.e. https://issues.jboss.org/browse/CDITCK-346
            throw new WebBeansDeploymentException(e);
        }
        catch (IllegalArgumentException e)
        {
            throw new WebBeansConfigurationException(e);
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

    private Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> getBeanAttributes(
                                final Map<BeanArchiveInformation, List<AnnotatedType<?>>> annotatedTypesPerBda)
    {
        final Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> beanAttributesPerBda
            = new HashMap<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>>();

        for (Map.Entry<BeanArchiveInformation, List<AnnotatedType<?>>> atEntry : annotatedTypesPerBda.entrySet())
        {
            BeanArchiveInformation bdaInfo = atEntry.getKey();
            List<AnnotatedType<?>> annotatedTypes = atEntry.getValue();

            boolean onlyScopedBeans = BeanDiscoveryMode.SCOPED.equals(bdaInfo.getBeanDiscoveryMode());

            final Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> bdaBeanAttributes = new IdentityHashMap<AnnotatedType<?>, ExtendedBeanAttributes<?>>(annotatedTypes.size());
            final Iterator<AnnotatedType<?>> iterator = annotatedTypes.iterator();
            while (iterator.hasNext())
            {
                final AnnotatedType<?> at = iterator.next();
                final Class beanClass = at.getJavaClass();
                final boolean isEjb = discoverEjb && EJBWebBeansConfigurator.isSessionBean(beanClass, webBeansContext);
                try
                {
                    if (isEjb || (ClassUtil.isConcrete(beanClass) || WebBeansUtil.isDecorator(at)) && isValidManagedBean(at))
                    {
                        final BeanAttributesImpl tBeanAttributes = BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(at, onlyScopedBeans).build();
                        if (tBeanAttributes != null)
                        {
                            final BeanAttributes<?> beanAttributes = webBeansContext.getWebBeansUtil().fireProcessBeanAttributes(at, at.getJavaClass(), tBeanAttributes);
                            if (beanAttributes != null)
                            {
                                bdaBeanAttributes.put(at, new ExtendedBeanAttributes(beanAttributes, isEjb));
                            }
                        }
                    }
                    else
                    {
                        iterator.remove();
                    }
                }
                catch (final NoClassDefFoundError ncdfe)
                {
                    logger.info("Skipping deployment of Class " + beanClass + "due to a NoClassDefFoundError: " + ncdfe.getMessage());
                }
            }

            beanAttributesPerBda.put(bdaInfo, bdaBeanAttributes);
        }


        return beanAttributesPerBda;
    }

    private void validateDisposeParameters()
    {
        final WebBeansUtil webBeansUtil = webBeansContext.getWebBeansUtil();
        for (final Bean<?> bean : webBeansContext.getBeanManagerImpl().getBeans())
        {
            if (ProducerMethodBean.class.isInstance(bean))
            {
                final Producer<?> producer = AbstractProducerBean.class.cast(bean).getProducer();
                if (BaseProducerProducer.class.isInstance(producer))
                {
                    final BaseProducerProducer producerProducer = BaseProducerProducer.class.cast(producer);
                    final Set<InjectionPoint> disposalIPs = producerProducer.getDisposalIPs();
                    if (disposalIPs != null && !producerProducer.isAnyDisposal()) // any can be ambiguous but that's not an issue
                    {
                        webBeansUtil.validate(disposalIPs, bean);
                    }
                }
            }
        }
    }


    /**
     * @throws DefinitionException if {@link javax.enterprise.inject.spi.Decorator#getDecoratedTypes()} isEmpty
     */
    private void validateDecoratorDecoratedTypes()
    {
        for (Decorator decorator : decoratorsManager.getDecorators())
        {
            if (decorator.getDecoratedTypes().isEmpty())
            {
                throw new WebBeansConfigurationException("Decorator must implement at least one interface (java.io.Serializeable will be ignored)");
            }
        }
    }

    // avoid delegate implementing Foo<A> and decorator implementing Foo<B> with no link between A and B
    private void validateDecoratorGenericTypes()
    {
        for (final Decorator<?> decorator : decoratorsManager.getDecorators())
        {
            final Type type = decorator.getDelegateType();

            // capture ParameterizedType from decorator type
            final Collection<Type> types = new HashSet<Type>();
            if (Class.class.isInstance(type))
            {
                Class<?> c = Class.class.cast(type);
                while (c != Object.class && c != null)
                {
                    types.add(c);
                    for (final Type t : asList(c.getGenericInterfaces()))
                    {
                        if (ParameterizedType.class.isInstance(t))
                        {
                            types.add(t);
                        }
                    }
                    final Type genericSuperclass = c.getGenericSuperclass();
                    if (ParameterizedType.class.isInstance(genericSuperclass))
                    {
                        types.add(genericSuperclass);
                    }
                    c = c.getSuperclass();
                }
            } // else?

            // check arguments matches with decorator API
            for (final Type api : decorator.getTypes())
            {
                if (!ParameterizedType.class.isInstance(api)) // no need to check here
                {
                    continue;
                }

                final ParameterizedType pt1 = ParameterizedType.class.cast(api);
                for (final Type t : types)
                {
                    if (ParameterizedType.class.isInstance(t))
                    {
                        final ParameterizedType pt2 = ParameterizedType.class.cast(t);

                        if (pt1.getRawType() == pt2.getRawType() &&
                            !GenericsUtil.isAssignableFrom(true, false, pt1, pt2))
                        {
                            throw new WebBeansConfigurationException("Generic error matching " + api + " and " + t);
                        }
                    }
                }
            }
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
            if (annotatedType.getAnnotation(javax.interceptor.Interceptor.class) != null)
            {
                Priority priority = annotatedType.getAnnotation(Priority.class);
                if (priority != null)
                {
                    final Class<?> javaClass = annotatedType.getJavaClass();
                    interceptorsManager.addPriorityClazzInterceptor(javaClass, priority);
                }
            }
            if (annotatedType.getAnnotation(javax.decorator.Decorator.class) != null)
            {
                Priority priority = annotatedType.getAnnotation(Priority.class);
                if (priority != null)
                {
                    final Class<?> javaClass = annotatedType.getJavaClass();
                    decoratorsManager.addPriorityClazzDecorator(javaClass, priority);
                }
            }
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

        // Register PrincipalBean
        beanManager.addInternalBean(webBeansUtil.getPrincipalBean());
        
        //REgister Provider Beans
        OpenWebBeansJavaEEPlugin beanEeProvider = webBeansContext.getPluginLoader().getJavaEEPlugin();

        if(beanEeProvider != null)
        {
            beanEeProvider.registerEEBeans();
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
        BeforeBeanDiscoveryImpl event = new BeforeBeanDiscoveryImpl(webBeansContext);
        manager.fireLifecycleEvent(event);
        event.setStarted();
    }
    
    /**
     * Fires event after bean discovery.
     */
    private void fireAfterBeanDiscoveryEvent()
    {
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        manager.setAfterBeanDiscoveryStart();
        final AfterBeanDiscoveryImpl event = new AfterBeanDiscoveryImpl(webBeansContext);
        manager.fireLifecycleEvent(event);


        webBeansContext.getWebBeansUtil().inspectDefinitionErrorStack(
                "There are errors that are added by AfterBeanDiscovery event observers. Look at logs for further details");

        manager.setAfterBeanDiscoveryDone();
        event.setStarted();
    }
    
    /**
     * Fires event after bean discovery.
     */
    private List<AnnotatedType<?>> fireAfterTypeDiscoveryEvent()
    {
        final BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        final List<AnnotatedType<?>> newAt = new LinkedList<AnnotatedType<?>>();
        final List<Class<?>> interceptors = interceptorsManager.getPrioritizedInterceptors();
        final List<Class<?>> decorators = decoratorsManager.getPrioritizedDecorators();
        final List<Class<?>> alternatives = webBeansContext.getAlternativesManager().getPrioritizedAlternatives();

        // match AfterTypeDiscovery expected order (1, 2, 3...)
        Collections.reverse(interceptors);
        Collections.reverse(decorators);
        Collections.reverse(alternatives);
        final AfterTypeDiscoveryImpl event = new AfterTypeDiscoveryImpl(webBeansContext, newAt,
                interceptors, decorators, alternatives);
        manager.fireLifecycleEvent(event);
        // reverse to keep "selection" order - decorator and interceptors considers it in their sorting.
        // NOTE: from here priorityClass.getSorted() MUST NOT be recomputed (ie no priorityClass.add(...))
        Collections.reverse(alternatives);
        event.setStarted();

        // we do not need to set back the sortedAlternatives to the AlternativesManager as the API
        // and all layers in between use a mutable List. Not very elegant but spec conform.

        webBeansContext.getWebBeansUtil().inspectDeploymentErrorStack(
                "There are errors that are added by AfterTypeDiscovery event observers. Look at logs for further details");
        return newAt;
    }

    /**
     * Fires event after deployment valdiation.
     */
    private void fireAfterDeploymentValidationEvent()
    {
        final BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        manager.setAfterDeploymentValidationFired(true);
        final AfterDeploymentValidationImpl event = new AfterDeploymentValidationImpl(manager);
        manager.fireLifecycleEvent(event);

        webBeansContext.getWebBeansUtil().inspectDeploymentErrorStack(
                "There are errors that are added by AfterDeploymentValidation event observers. Look at logs for further details");

        packageVetoCache.clear(); // no more needed, free the memory
        event.setStarted();
    }

    /**
     * Check if all XML configured alternatives end up as alternative beans
     * @param beanAttributesPerBda
     */
    private void validateAlternatives(Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> beanAttributesPerBda)
    {
        Set<Class<?>> xmlConfiguredAlternatives = webBeansContext.getAlternativesManager().getXmlConfiguredAlternatives();
        InjectionResolver injectionResolver = webBeansContext.getBeanManagerImpl().getInjectionResolver();

        for (Class<?> alternativeClass : xmlConfiguredAlternatives)
        {
            if (AnnotationUtil.hasClassAnnotation(alternativeClass, Alternative.class) ||
                AnnotationUtil.hasMetaAnnotation(alternativeClass.getAnnotations(), Alternative.class))
            {
                continue;
            }

            boolean foundAlternativeClass = false;

            Set<Bean<?>> beans = injectionResolver.implResolveByType(false, alternativeClass, AnyLiteral.INSTANCE);
            if (beans == null || beans.isEmpty())
            {
                out:
                for (Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> annotatedTypeExtendedBeanAttributesMap : beanAttributesPerBda.values())
                {
                    for (Map.Entry<AnnotatedType<?>, ExtendedBeanAttributes<?>> exType : annotatedTypeExtendedBeanAttributesMap.entrySet())
                    {
                        if (alternativeClass.equals(exType.getKey().getJavaClass()))
                        {
                            if (exType.getValue().beanAttributes.isAlternative() ||
                                exType.getKey().getAnnotation(Alternative.class) != null)
                            {
                                foundAlternativeClass = true;
                                break out; // all fine, continue with the next
                            }
                        }
                    }
                }
            }
            else
            {
                for (Bean<?> bean : beans)
                {
                    if (bean.isAlternative())
                    {
                        foundAlternativeClass = true;
                        break;
                    }
                }
            }
            if (!foundAlternativeClass)
            {
                throw new WebBeansDeploymentException("Given alternative class : " + alternativeClass.getName() +
                    " is not annotated wih @Alternative or not an enabled bean");
            }

        }

    }


    /**
     * Validate all injection points.
     */
    private void validateInjectionPoints()
    {
        logger.fine("Validation of injection points has started.");

        decoratorsManager.validateDecoratorClasses();
        interceptorsManager.validateInterceptorClasses();

        //Adding decorators to validate
        Set<Decorator<?>> decorators = decoratorsManager.getDecorators();

        logger.fine("Validation of the decorator's injection points has started.");
        
        //Validate Decorators
        validate(decorators);
        
        //Adding interceptors to validate
        List<javax.enterprise.inject.spi.Interceptor<?>> interceptors = interceptorsManager.getCdiInterceptors();
        
        logger.fine("Validation of the interceptor's injection points has started.");
        
        //Validate Interceptors
        validate(interceptors);

        logger.fine("Validation of the beans' injection points has started.");

        Set<Bean<?>> beans = webBeansContext.getBeanManagerImpl().getBeans();
        
        //Validate Others
        validate(beans);
        
        logger.fine("Validation of the observer methods' injection points has started.");
        
        //Validate Observers
        validateObservers(webBeansContext.getBeanManagerImpl().getNotificationManager().getObserverMethods());

        logger.info(OWBLogConst.INFO_0003);
    }
    
    /**
     * Validates beans.
     * 
     * @param beans deployed beans
     */
    private <T, B extends Bean<?>> void validate(Collection<B> beans)
    {
        webBeansContext.getBeanManagerImpl().getInjectionResolver().clearCaches();

        if (beans != null && beans.size() > 0)
        {
            Stack<String> beanNames = new Stack<String>();
            for (Bean<?> bean : beans)
            {
                try
                {

                    if (bean instanceof OwbBean && !((OwbBean) bean).isEnabled())
                    {
                        // we skip disabled beans
                        continue;
                    }

                    //don't validate the cdi-api
                    if (bean.getBeanClass().getName().startsWith(JAVAX_ENTERPRISE_PACKAGE))
                    {
                        if (BuiltInOwbBean.class.isInstance(bean))
                        {
                            final Class<?> proxyable = BuiltInOwbBean.class.cast(bean).proxyableType();
                            if (proxyable != null)
                            {
                                final AbstractProducer producer = AbstractProducer.class.cast(OwbBean.class.cast(bean).getProducer());
                                final AnnotatedType<?> annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(proxyable);
                                producer.defineInterceptorStack(bean, annotatedType, webBeansContext);
                            }
                        }
                        continue;
                    }

                    String beanName = bean.getName();
                    if (beanName != null)
                    {
                        beanNames.push(beanName);
                    }

                    if (bean instanceof OwbBean && !(bean instanceof Interceptor) && !(bean instanceof Decorator))
                    {
                        AbstractProducer<T> producer = null;

                        OwbBean<T> owbBean = (OwbBean<T>) bean;
                        if (ManagedBean.class.isInstance(bean)) // in this case don't use producer which can be wrapped
                        {
                            producer = ManagedBean.class.cast(bean).getOriginalInjectionTarget();
                        }
                        if (producer == null && owbBean.getProducer() instanceof AbstractProducer)
                        {
                            producer = (AbstractProducer<T>) owbBean.getProducer();
                        }
                        if (producer != null)
                        {
                            AnnotatedType<T> annotatedType;
                            if (owbBean instanceof InjectionTargetBean)
                            {
                                annotatedType = ((InjectionTargetBean<T>) owbBean).getAnnotatedType();
                            }
                            else
                            {
                                annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(owbBean.getReturnType());
                            }
                            producer.defineInterceptorStack(owbBean, annotatedType, webBeansContext);
                        }
                    }

                    //Bean injection points
                    Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();

                    //Check injection points
                    if (injectionPoints != null)
                    {
                        webBeansContext.getWebBeansUtil().validate(injectionPoints, bean);
                    }

                    //Check passivation scope
                    checkPassivationScope(bean);
                }
                catch (RuntimeException e)
                {
                    throw ExceptionUtil.addInformation(e, "Problem while validating bean " + bean);
                }

            }
            //Validate Bean names
            validateBeanNames(beanNames);

            //Clear Names
            beanNames.clear();
        }
        
    }
    
    private void validateObservers(Collection<ObserverMethod<?>> observerMethods)
    {
        for (ObserverMethod<?> observerMethod: observerMethods)
        {
            if (observerMethod instanceof OwbObserverMethod)
            {
                OwbObserverMethod<?> owbObserverMethod = (OwbObserverMethod<?>)observerMethod;
                webBeansContext.getWebBeansUtil().validate(owbObserverMethod.getInjectionPoints(), null);
            }
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
                                resolver.resolve(beans, null);
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
                        if(part != null && part.equals(other))
                        {
                            throw new WebBeansDeploymentException("EL name of one bean is of the form x.y, where y is a valid bean EL name, and " +
                                    "x is the EL name of the other bean for the bean name : " + beanName);
                        }
                    }
                }                
            }            
        }
    }

    /**
     * Create AnnotatedTypes from the ClassPath via the ScannerService
     */
    private Map<BeanArchiveInformation, List<AnnotatedType<?>>> annotatedTypesFromClassPath(ScannerService scanner)
    {
        logger.fine("Creating AnnotatedTypes from class files has started.");
        Set<Class<?>> foundClasses = new HashSet<Class<?>>(100);

        Map<BeanArchiveInformation, List<AnnotatedType<?>>> annotatedTypesPerBda
            = new HashMap<BeanArchiveInformation, List<AnnotatedType<?>>>();

        if (scanner instanceof BdaScannerService)
        {
            Map<BeanArchiveInformation, Set<Class<?>>> beanClassesPerBda = ((BdaScannerService) scanner).getBeanClassesPerBda();

            for (Map.Entry<BeanArchiveInformation, Set<Class<?>>> bdaEntry : beanClassesPerBda.entrySet())
            {
                BeanArchiveInformation bdaInfo = bdaEntry.getKey();
                List<AnnotatedType<?>> annotatedTypes = annotatedTypesFromBdaClassPath(bdaEntry.getValue(), foundClasses);
                annotatedTypesPerBda.put(bdaEntry.getKey(), annotatedTypes);
            }

            // also add the rest of the class es to the default bda
            // we also need this initialised in case annotatedTypes get added manually at a later step
            annotatedTypesPerBda.put(defaultBeanArchiveInformation, annotatedTypesFromBdaClassPath(scanner.getBeanClasses(), foundClasses));
        }
        else
        {
            // this path is only for backward compat to older ScannerService implementations

            Set<Class<?>> classIndex = scanner.getBeanClasses();
            List<AnnotatedType<?>> annotatedTypes = annotatedTypesFromBdaClassPath(classIndex, foundClasses);

            annotatedTypesPerBda.put(defaultBeanArchiveInformation, annotatedTypes);
        }


        return annotatedTypesPerBda;
    }

    /**
     * @param foundClasses classes which already got processed. To prevent picking up the same class from multiple classpaths
     */
    private List<AnnotatedType<?>> annotatedTypesFromBdaClassPath(Set<Class<?>> classIndex, Set<Class<?>> foundClasses)
    {
        List<AnnotatedType<?>> annotatedTypes = new ArrayList<AnnotatedType<?>>();

        //Iterating over each class
        if (classIndex != null)
        {
            AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();

            for (Class<?> implClass : classIndex)
            {
                if (foundClasses.contains(implClass))
                {
                    // skip this class
                    continue;
                }

                foundClasses.add(implClass);

                if (isVetoed(implClass))
                {
                    if (isEEComponent(implClass))
                    {
                        // fire injection point events and forget
                        AnnotatedType<?> annotatedType = annotatedElementFactory.newAnnotatedType(implClass);
                        InjectionTarget<?> it = webBeansContext.getBeanManagerImpl().createInjectionTarget(annotatedType);
                        for (final InjectionPoint ip : it.getInjectionPoints())
                        {
                            webBeansContext.getWebBeansUtil().fireProcessInjectionPointEvent(ip);
                        }
                    }
                    continue;
                }

                try
                {
                    //Define annotation type
                    AnnotatedType<?> annotatedType = annotatedElementFactory.getAnnotatedType(implClass);
                    if (annotatedType == null) // mean no annotation created it (normal case)
                    {
                        annotatedType = annotatedElementFactory.newAnnotatedType(implClass);
                    }

                    if (annotatedType == null)
                    {
                        logger.info("Could not create AnnotatedType for class " + implClass);
                        continue;
                    }

                    // Fires ProcessAnnotatedType
                    if (!annotatedType.getJavaClass().isAnnotation())
                    {
                        GProcessAnnotatedType processAnnotatedEvent = webBeansContext.getWebBeansUtil().fireProcessAnnotatedTypeEvent(annotatedType);
                        if (!processAnnotatedEvent.isVeto())
                        {
                            annotatedTypes.add(processAnnotatedEvent.getAnnotatedType());
                        }
                        processAnnotatedEvent.setStarted();
                    }
                    else
                    {
                        annotatedTypes.add(annotatedType);
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

    private boolean isEEComponent(final Class<?> impl)
    {
        OpenWebBeansJavaEEPlugin eePlugin = webBeansContext.getPluginLoader().getJavaEEPlugin();
        return eePlugin != null && eePlugin.isEEComponent(impl);
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
        if (pckge == null)
        {
            return false;
        }
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
    private void addAdditionalAnnotatedTypes(Collection<AnnotatedType<?>> toDeploy, List<AnnotatedType<?>> annotatedTypes)
    {
        for (AnnotatedType<?> annotatedType : toDeploy)
        {
            // Fires ProcessAnnotatedType
            ProcessSyntheticAnnotatedTypeImpl<?> processAnnotatedEvent = !annotatedType.getJavaClass().isAnnotation() ?
                    webBeansContext.getWebBeansUtil().fireProcessSyntheticAnnotatedTypeEvent(annotatedType) : null;

            if (processAnnotatedEvent == null || !processAnnotatedEvent.isVeto())
            {
                AnnotatedType<?> changedAnnotatedType = processAnnotatedEvent == null ? annotatedType : processAnnotatedEvent.getAnnotatedType();
                if (annotatedTypes.contains(changedAnnotatedType))
                {
                    annotatedTypes.remove(changedAnnotatedType);
                }
                annotatedTypes.add(changedAnnotatedType);
            }
            if (processAnnotatedEvent != null)
            {
                processAnnotatedEvent.setStarted();
            }
        }
    }


    /**
     * Discovers and deploys classes from class path.
     * 
     * @param beanAttributesPerBda the AnnotatedTypes which got discovered so far and are not vetoed
     * @throws ClassNotFoundException if class not found
     */
    protected void deployFromBeanAttributes( Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> beanAttributesPerBda)
    {
        logger.fine("Deploying configurations from class files has started.");

        for (Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> beanAttributesMap : beanAttributesPerBda.values())
        {

            // Start from the class
            for (Map.Entry<AnnotatedType<?>, ExtendedBeanAttributes<?>> annotatedType : beanAttributesMap.entrySet())
            {
                try
                {
                    deploySingleAnnotatedType(annotatedType.getKey(), annotatedType.getValue(), beanAttributesMap);
                }
                catch (NoClassDefFoundError ncdfe)
                {
                    logger.info("Skipping deployment of Class " + annotatedType.getKey().getJavaClass() + "due to a NoClassDefFoundError: " + ncdfe.getMessage());
                }

                // if the implClass already gets processed as part of the
                // standard BDA scanning, then we don't need to 'additionally'
                // deploy it anymore.
                webBeansContext.getBeanManagerImpl().removeAdditionalAnnotatedType(annotatedType.getKey());

            }
        }

        logger.fine("Deploying configurations from class files has ended.");

    }
    

    /**
     * Common helper method used to deploy annotated types discovered through
     * scanning or during beforeBeanDiscovery.
     * 
     * @param annotatedTypeData the AnnotatedType representing the bean to be deployed with their already computed data
     */
    private void deploySingleAnnotatedType(AnnotatedType annotatedType, ExtendedBeanAttributes annotatedTypeData, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> annotatedTypes)
    {

        Class beanClass = annotatedType.getJavaClass();

        // EJBs can be defined so test them really before going for a ManagedBean
        if (annotatedTypeData.isEjb)
        {
            logger.log(Level.FINE, "Found Enterprise Bean with class name : [{0}]", beanClass.getName());
            defineEnterpriseWebBean((Class<Object>) beanClass, annotatedType, annotatedTypeData.beanAttributes);
        }
        else
        {
            try
            {
                if((ClassUtil.isConcrete(beanClass) || WebBeansUtil.isDecorator(annotatedType))
                        && isValidManagedBean(annotatedType))
                {
                    defineManagedBean(annotatedType, annotatedTypeData.beanAttributes, annotatedTypes);
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
        try
        {
            if (!webBeansUtil.isConstructorOk(type))
            {
                return false;
            }
        }
        catch (final TypeNotPresentException cnfe)
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
    protected void deployFromXML(ScannerService scanner)
        throws WebBeansDeploymentException
    {
        logger.fine("Deploying configurations from XML files has started.");

        Set<URL> bdaLocations = scanner.getBeanXmls();
        Iterator<URL> it = bdaLocations.iterator();

        while (it.hasNext())
        {
            URL url = it.next();

            logger.fine("OpenWebBeans BeansDeployer configuring: " + url.toExternalForm());

            BeanArchiveInformation beanArchiveInformation = beanArchiveService.getBeanArchiveInformation(url);

            configureDecorators(url, beanArchiveInformation.getDecorators());
            configureInterceptors(url, beanArchiveInformation.getInterceptors());
            configureAlternatives(url, beanArchiveInformation.getAlternativeClasses(), false);
            configureAlternatives(url, beanArchiveInformation.getAlternativeStereotypes(), true);
            configureAllowProxying(url, beanArchiveInformation.getAllowProxyingClasses());
        }

        logger.fine("Deploying configurations from XML has ended successfully.");
    }

    private void configureAlternatives(URL bdaLocation, List<String> alternatives, boolean isStereotype)
    {
        AlternativesManager manager = webBeansContext.getAlternativesManager();
        AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();

        // the alternatives in this beans.xml
        // this gets used to detect multiple definitions of the
        // same alternative in one beans.xml file.
        Set<String> alternativesInFile = new HashSet<String>();

        for (String alternativeName : alternatives)
        {
            if (alternativesInFile.contains(alternativeName))
            {
                throw new WebBeansDeploymentException(createConfigurationFailedMessage(bdaLocation) + "Given alternative : " + alternativeName
                        + " is already added as @Alternative" );
            }
            alternativesInFile.add(alternativeName);

            Class clazz = ClassUtil.getClassFromName(alternativeName);

            if (clazz == null)
            {
                throw new WebBeansDeploymentException(createConfigurationFailedMessage(bdaLocation) + "Alternative: " + alternativeName + " not found");
            }
            else
            {
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
                throw new WebBeansDeploymentException(createConfigurationFailedMessage(bdaLocation) + "Decorator class : " +
                        decorator + " not found");
            }
            else
            {
                if ((scannerService.isBDABeansXmlScanningEnabled() && !scannerService.getBDABeansXmlScanner().addDecorator(clazz, bdaLocation.toExternalForm())) ||
                        decoratorsInFile.contains(clazz))
                {
                    throw new WebBeansDeploymentException(createConfigurationFailedMessage(bdaLocation) + "Decorator class : " +
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
                throw new WebBeansDeploymentException(createConfigurationFailedMessage(bdaLocation) + "Interceptor class : " +
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

                GProcessAnnotatedType processAnnotatedEvent =
                        webBeansContext.getWebBeansUtil().fireProcessAnnotatedTypeEvent(annotatedType);

                // if veto() is called
                if (processAnnotatedEvent.isVeto())
                {
                    return;
                }

                annotatedType = processAnnotatedEvent.getAnnotatedType();
                processAnnotatedEvent.setStarted();

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
                    throw new WebBeansDeploymentException(createConfigurationFailedMessage(bdaLocation) + "Interceptor class : "
                            + interceptor + " must have at least one @InterceptorBinding");
                }

                // check if the interceptor got defined twice in this beans.xml
                if (interceptorsInFile.contains(clazz))
                {
                    throw new WebBeansDeploymentException(createConfigurationFailedMessage(bdaLocation) + "Interceptor class : "
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

    private void configureAllowProxying(URL url, List<String> allowProxyingClasses)
    {
        OpenWebBeansConfiguration owbConfiguration = webBeansContext.getOpenWebBeansConfiguration();
        for (String allowProxyingClass : allowProxyingClasses)
        {
            owbConfiguration.addConfigListValue(OpenWebBeansConfiguration.ALLOW_PROXYING_PARAM, allowProxyingClass);
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
                    throw new WebBeansDeploymentException("Passivation scoped defined bean must be passivation capable, " +
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
            final StereoTypeManager stereoTypeManager = webBeansContext.getStereoTypeManager();
            for(Class<?> beanClass : beanClasses)
            {                
                if(beanClass.isAnnotation())
                {
                    Class<? extends Annotation> stereoClass = (Class<? extends Annotation>) beanClass;
                    if (annotationManager.isStereoTypeAnnotation(stereoClass)
                        && stereoTypeManager.getStereoTypeModel(stereoClass.getName()) == null)
                    {
                        webBeansContext.getAnnotationManager().checkStereoTypeClass(stereoClass, stereoClass.getDeclaredAnnotations());
                        StereoTypeModel model = new StereoTypeModel(webBeansContext, stereoClass);
                        stereoTypeManager.addStereoTypeModel(model);
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
    protected <T> void defineManagedBean(AnnotatedType<T> annotatedType, BeanAttributes<T> attributes, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> annotatedTypes)
    {   
        //Fires ProcessInjectionTarget event for Java EE components instances
        //That supports injections but not managed beans
        Class beanClass = annotatedType.getJavaClass();
        if(webBeansContext.getWebBeansUtil().supportsJavaEeComponentInjections(beanClass))
        {
            //Fires ProcessInjectionTarget
            webBeansContext.getWebBeansUtil().fireProcessInjectionTargetEventForJavaEeComponents(beanClass).setStarted();
            webBeansContext.getWebBeansUtil().inspectDeploymentErrorStack(
                    "There are errors that are added by ProcessInjectionTarget event observers. Look at logs for further details");

            //Checks that not contains @Inject InjectionPoint
            webBeansContext.getAnnotationManager().checkInjectionPointForInjectInjectionPoint(beanClass);
        }

        {
            ManagedBeanBuilder<T, ManagedBean<T>> managedBeanCreator = new ManagedBeanBuilder<T, ManagedBean<T>>(webBeansContext, annotatedType, attributes);

            if(WebBeansUtil.isDecorator(annotatedType))
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Found Managed Bean Decorator with class name : [{0}]", annotatedType.getJavaClass().getName());
                }

                DecoratorBeanBuilder<T> dbb = new DecoratorBeanBuilder<T>(webBeansContext, annotatedType, attributes);
                if (dbb.isDecoratorEnabled())
                {
                    dbb.defineDecoratorRules();
                    DecoratorBean<T> decorator = dbb.getBean();
                    decoratorsManager.addDecorator(decorator);
                }
            }
            else if(WebBeansUtil.isCdiInterceptor(annotatedType))
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Found Managed Bean Interceptor with class name : [{0}]", annotatedType.getJavaClass().getName());
                }

                CdiInterceptorBeanBuilder<T> ibb = new CdiInterceptorBeanBuilder<T>(webBeansContext, annotatedType, attributes);
                if (ibb.isInterceptorEnabled())
                {
                    ibb.defineCdiInterceptorRules();
                    CdiInterceptorBean<T> interceptor = ibb.getBean();
                    interceptorsManager.addCdiInterceptor(interceptor);
                }
            }
            else
            {
                InjectionTargetBean<T> bean = managedBeanCreator.getBean();

                if (decoratorsManager.containsCustomDecoratorClass(annotatedType.getJavaClass()) ||
                    interceptorsManager.containsCustomInterceptorClass(annotatedType.getJavaClass()))
                {
                    return; //TODO discuss this case (it was ignored before)
                }

                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Found Managed Bean with class name : [{0}]", annotatedType.getJavaClass().getName());
                }

                final Set<ObserverMethod<?>> observerMethods;
                final AnnotatedType<T> beanAnnotatedType = bean.getAnnotatedType();
                final AnnotatedType<T> defaultAt = webBeansContext.getAnnotatedElementFactory().getAnnotatedType(beanAnnotatedType.getJavaClass());
                final boolean ignoreProducer = defaultAt != beanAnnotatedType && annotatedTypes.containsKey(defaultAt);
                if(bean.isEnabled())
                {
                    observerMethods = new ObserverMethodsBuilder<T>(webBeansContext, beanAnnotatedType).defineObserverMethods(bean);
                }
                else
                {
                    observerMethods = new HashSet<ObserverMethod<?>>();
                }

                final WebBeansContext wbc = bean.getWebBeansContext();
                Set<ProducerFieldBean<?>> producerFields =
                        ignoreProducer ? Collections.emptySet() : new ProducerFieldBeansBuilder(wbc, beanAnnotatedType).defineProducerFields(bean);
                Set<ProducerMethodBean<?>> producerMethods =
                        ignoreProducer ? Collections.emptySet() : new ProducerMethodBeansBuilder(wbc, beanAnnotatedType).defineProducerMethods(bean, producerFields);

                ManagedBean<T> managedBean = (ManagedBean<T>)bean;
                Map<ProducerMethodBean<?>,AnnotatedMethod<?>> annotatedMethods =
                        new HashMap<ProducerMethodBean<?>, AnnotatedMethod<?>>();

                if (!producerFields.isEmpty() || !producerMethods.isEmpty())
                {
                    final Priority priority = annotatedType.getAnnotation(Priority.class);
                    if (priority != null && !webBeansContext.getAlternativesManager()
                            .isAlternative(annotatedType.getJavaClass(), Collections.<Class<? extends Annotation>>emptySet()))
                    {
                        webBeansContext.getAlternativesManager().addPriorityClazzAlternative(annotatedType.getJavaClass(), priority);
                    }
                }

                for(ProducerMethodBean<?> producerMethod : producerMethods)
                {
                    AnnotatedMethod<?> method = webBeansContext.getAnnotatedElementFactory().newAnnotatedMethod(producerMethod.getCreatorMethod(), annotatedType);
                    webBeansContext.getWebBeansUtil().inspectDeploymentErrorStack("There are errors that are added by ProcessProducer event observers for "
                            + "ProducerMethods. Look at logs for further details");

                    annotatedMethods.put(producerMethod, method);
                }

                Map<ProducerFieldBean<?>,AnnotatedField<?>> annotatedFields =
                        new HashMap<ProducerFieldBean<?>, AnnotatedField<?>>();

                for(ProducerFieldBean<?> producerField : producerFields)
                {
                    webBeansContext.getWebBeansUtil().inspectDeploymentErrorStack("There are errors that are added by ProcessProducer event observers for"
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
                beanManager.fireEvent(processBeanEvent, true);
                processBeanEvent.setStarted();
                webBeansContext.getWebBeansUtil().inspectDefinitionErrorStack("There are errors that are added by ProcessManagedBean event observers for " +
                        "managed beans. Look at logs for further details");

                //Fires ProcessProducerMethod
                webBeansContext.getWebBeansUtil().fireProcessProducerMethodBeanEvent(annotatedMethods, annotatedType);
                webBeansContext.getWebBeansUtil().inspectDefinitionErrorStack("There are errors that are added by ProcessProducerMethod event observers for " +
                        "producer method beans. Look at logs for further details");

                //Fires ProcessProducerField
                webBeansContext.getWebBeansUtil().fireProcessProducerFieldBeanEvent(annotatedFields);
                webBeansContext.getWebBeansUtil().inspectDefinitionErrorStack("There are errors that are added by ProcessProducerField event observers for " +
                        "producer field beans. Look at logs for further details");

                //Fire ObservableMethods
                webBeansContext.getWebBeansUtil().fireProcessObservableMethodBeanEvent(observerMethodsMap);
                webBeansContext.getWebBeansUtil().inspectDefinitionErrorStack("There are errors that are added by ProcessObserverMethod event observers for " +
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
    protected <T> void defineEnterpriseWebBean(Class<T> clazz, AnnotatedType<T> annotatedType, BeanAttributes<T> attributes)
    {
        InjectionTargetBean<T> bean = (InjectionTargetBean<T>) EJBWebBeansConfigurator.defineEjbBean(
                clazz, annotatedType, attributes, webBeansContext);
        webBeansContext.getWebBeansUtil().setInjectionTargetBeanEnableFlag(bean);
    }

    public static class ExtendedBeanAttributes<T>
    {
        private final BeanAttributes<T> beanAttributes;
        private final boolean isEjb;

        public ExtendedBeanAttributes(final BeanAttributes<T> beanAttributes, final boolean isEjb)
        {
            this.beanAttributes = beanAttributes;
            this.isEjb = isEjb;
        }
    }
}
