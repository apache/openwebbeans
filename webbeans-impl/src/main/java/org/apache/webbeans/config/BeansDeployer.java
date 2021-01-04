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
import org.apache.webbeans.configurator.AnnotatedTypeConfiguratorImpl;
import org.apache.webbeans.container.AnnotatedTypeWrapper;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectableBeanManager;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.context.control.ActivateRequestContextInterceptorBean;
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
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.DefinitionException;

import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.AbstractProducer;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.BaseProducerProducer;
import org.apache.webbeans.portable.events.ProcessBeanAttributesImpl;
import org.apache.webbeans.portable.events.ProcessBeanImpl;
import org.apache.webbeans.portable.events.ProcessSyntheticAnnotatedTypeImpl;
import org.apache.webbeans.portable.events.discovery.AfterBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.discovery.AfterDeploymentValidationImpl;
import org.apache.webbeans.portable.events.discovery.AfterTypeDiscoveryImpl;
import org.apache.webbeans.portable.events.discovery.AnnotatedTypeConfiguratorHolder;
import org.apache.webbeans.portable.events.discovery.BeforeBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.generics.GProcessAnnotatedType;
import org.apache.webbeans.portable.events.generics.GProcessBean;
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
import java.lang.reflect.Modifier;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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


    /**Deployment is started or not*/
    protected boolean deployed;

    /**XML Configurator*/
    protected BeanArchiveService beanArchiveService;
    
    /**Discover ejb or not*/
    protected boolean discoverEjb;
    private final WebBeansContext webBeansContext;

    private final ScannerService scannerService;
    private final DecoratorsManager decoratorsManager;
    private final InterceptorsManager interceptorsManager;

    private final Map<String, Boolean> packageVetoCache = new HashMap<>();

    protected boolean skipVetoedOnPackages;
    protected boolean skipNoClassDefFoundTriggers;
    protected boolean skipValidations;

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
        skipVetoedOnPackages = Boolean.parseBoolean(this.webBeansContext.getOpenWebBeansConfiguration().getProperty(
                "org.apache.webbeans.spi.deployer.skipVetoedOnPackages"));
        skipValidations = Boolean.parseBoolean(this.webBeansContext.getOpenWebBeansConfiguration().getProperty(
                "org.apache.webbeans.spi.deployer.skipValidations"));
        skipNoClassDefFoundTriggers = this.webBeansContext.getOpenWebBeansConfiguration().isSkipNoClassDefFoundErrorTriggers();

        defaultBeanArchiveInformation = new DefaultBeanArchiveInformation("default");
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
                // Register built-in RequestContextController
                webBeansContext.getBeanManagerImpl().addInternalBean(webBeansContext.getWebBeansUtil().getRequestContextControllerBean());
                webBeansContext.getInterceptorsManager().addCdiInterceptor(webBeansContext.getWebBeansUtil().getRequestContextInterceptorBean());
                webBeansContext.getInterceptorsManager().addPriorityClazzInterceptor(
                        ActivateRequestContextInterceptorBean.InterceptorClass.class,
                        javax.interceptor.Interceptor.Priority.PLATFORM_BEFORE + 100);

                //Fire Event
                fireBeforeBeanDiscoveryEvent();
                
                //Configure Default Beans
                configureDefaultBeans();

                Map<BeanArchiveInformation, List<AnnotatedType<?>>> annotatedTypesPerBda = annotatedTypesFromClassPath(scanner);

                List<AnnotatedType<?>> globalBdaAnnotatedTypes = annotatedTypesPerBda.get(defaultBeanArchiveInformation);

                // Deploy additional Annotated Types which got added via BeforeBeanDiscovery#addAnnotatedType
                final Collection<AnnotatedType<?>> additionalAnnotatedTypes =
                        webBeansContext.getBeanManagerImpl().getAdditionalAnnotatedTypes();
                addAdditionalAnnotatedTypes(additionalAnnotatedTypes, globalBdaAnnotatedTypes);

                for (List<AnnotatedType<?>> at : annotatedTypesPerBda.values())
                {
                    registerAlternativesDecoratorsAndInterceptorsWithPriority(at);
                }

                // Also configures deployments, interceptors, decorators.
                deployFromXML(scanner);

                addAdditionalAnnotatedTypes(fireAfterTypeDiscoveryEvent(), globalBdaAnnotatedTypes);


                Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> beanAttributesPerBda
                    = getBeanAttributes(annotatedTypesPerBda);

                // shouldn't be used anymore, view is now beanAttributes
                annotatedTypesPerBda.clear();

                SpecializationUtil specializationUtil = new SpecializationUtil(webBeansContext);
                specializationUtil.removeDisabledBeanAttributes(beanAttributesPerBda, null, true);


                //Checking stereotype conditions
                checkStereoTypes(beanAttributesPerBda);

                // Handle Specialization
                specializationUtil.removeDisabledBeanAttributes(
                        beanAttributesPerBda,
                        new SpecializationUtil.BeanAttributesProvider()
                        {
                            @Override
                            public <T> BeanAttributes get(AnnotatedType<T> at)
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

                configureProducerMethodSpecializations();

                // all beans which got 'overridden' by a Specialized version can be removed now
                removeDisabledBeans();
                
                // We are finally done with our bean discovery
                fireAfterBeanDiscoveryEvent();

                // activate InjectionResolver cache now
                webBeansContext.getBeanManagerImpl().getInjectionResolver().setStartup(false);

                // drop no more needed memory data
                webBeansContext.getNotificationManager().afterStart();

                if (!skipValidations)
                {
                    validateAlternatives(beanAttributesPerBda);

                    validateInjectionPoints();
                    validateDisposeParameters();

                    validateDecoratorDecoratedTypes();
                    validateDecoratorGenericTypes();

                    validateNames();
                }
                else
                {
                    webBeansContext.getBeanManagerImpl().getBeans().forEach(bean -> {
                        if (BuiltInOwbBean.class.isInstance(bean))
                        {
                            Class<?> proxyable = BuiltInOwbBean.class.cast(bean).proxyableType();
                            if (proxyable != null)
                            {
                                AbstractProducer producer = AbstractProducer.class.cast(OwbBean.class.cast(bean).getProducer());
                                AnnotatedType<?> annotatedType = webBeansContext.getAnnotatedElementFactory()
                                        .newAnnotatedType(proxyable);
                                producer.defineInterceptorStack(bean, annotatedType, webBeansContext);
                            }
                        }
                        else if (bean instanceof OwbBean &&
                                !(bean instanceof Interceptor) &&
                                !(bean instanceof Decorator))
                        {
                            AbstractProducer producer = null;
                            OwbBean<?> owbBean = (OwbBean<?>) bean;
                            if (ManagedBean.class.isInstance(bean)) // in this case don't use producer which can be wrapped
                            {
                                producer = ManagedBean.class.cast(bean).getOriginalInjectionTarget();
                            }
                            if (producer == null && owbBean.getProducer() instanceof AbstractProducer)
                            {
                                producer = (AbstractProducer) owbBean.getProducer();
                            }
                            if (producer != null)
                            {
                                AnnotatedType<?> annotatedType;
                                if (owbBean instanceof InjectionTargetBean)
                                {
                                    annotatedType = ((InjectionTargetBean<?>) owbBean).getAnnotatedType();
                                }
                                else
                                {
                                    annotatedType = webBeansContext.getAnnotatedElementFactory()
                                            .newAnnotatedType(owbBean.getReturnType());
                                }
                                producer.defineInterceptorStack(owbBean, annotatedType, webBeansContext);
                            }
                        }
                    });
                }

                if (webBeansContext.getNotificationManager().getObserverMethods().stream()
                        .anyMatch(ObserverMethod::isAsync))
                {
                    // enforce it to be loaded and ensuring it works before runtime
                    webBeansContext.getNotificationManager().getDefaultNotificationOptions()
                            .getExecutor().execute(() -> {});
                }

                // fire event
                fireAfterDeploymentValidationEvent();


                // do some cleanup after the deployment
                scanner.release();
                webBeansContext.getAnnotatedElementFactory().clear();
                webBeansContext.getNotificationManager().clearCaches();
                webBeansContext.getAnnotationManager().clearCaches();
            }
        }
        catch (UnsatisfiedResolutionException | UnproxyableResolutionException | AmbiguousResolutionException e)
        {
            throw new WebBeansDeploymentException(e);
        }
        // the tck expects a DeploymentException, but it really should be a DefinitionException, see i.e. https://issues.jboss.org/browse/CDITCK-346
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

    /**
     * Ensure "foo" and "foo.bar" conflict and is reported as a DeploymentException but foo.bar and foo.dummy don't conflict.
     */
    private void validateNames()
    {
        Collection<String> names = new HashSet<>();
        Collection<String> partials = new HashSet<>();
        for (Bean<?> bean : webBeansContext.getBeanManagerImpl().getBeans())
        {
            // the skip logic needs some revisit but this validation is not useful enough to justify to resolve all alternatives here
            if (bean.isAlternative())
            {
                continue;
            }
            if (AbstractProducerBean.class.isInstance(bean) && AbstractProducerBean.class.cast(bean).getOwnerComponent().isAlternative())
            {
                continue;
            }

            String name = bean.getName();
            if (name != null)
            {
                if (name.contains("."))
                {
                    String[] segments = name.split("\\.");
                    String current = "";
                    for (int i = 0; i < segments.length - 1; i++)
                    {
                        current += (i > 0 ? "." : "") + segments[i];
                        partials.add(current);
                        if (names.contains(current))
                        {
                            throw new WebBeansDeploymentException("Name '" + name + "' is conflicting with '" + current + "'");
                        }
                    }
                }

                if (!names.add(name) || partials.contains(name))
                {
                    throw new WebBeansDeploymentException("Name '" + name + "' is conflicting");
                }
            }
        }
    }

    private Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> getBeanAttributes(
                                Map<BeanArchiveInformation, List<AnnotatedType<?>>> annotatedTypesPerBda)
    {
        Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> beanAttributesPerBda
            = new HashMap<>();

        for (Map.Entry<BeanArchiveInformation, List<AnnotatedType<?>>> atEntry : annotatedTypesPerBda.entrySet())
        {
            BeanArchiveInformation bdaInfo = atEntry.getKey();
            List<AnnotatedType<?>> annotatedTypes = atEntry.getValue();

            boolean onlyScopedBeans = BeanDiscoveryMode.TRIM == bdaInfo.getBeanDiscoveryMode();

            Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> bdaBeanAttributes = new IdentityHashMap<>(annotatedTypes.size());
            Iterator<AnnotatedType<?>> iterator = annotatedTypes.iterator();
            while (iterator.hasNext())
            {
                AnnotatedType<?> at = iterator.next();
                Class beanClass = at.getJavaClass();
                boolean isEjb = discoverEjb && EJBWebBeansConfigurator.isSessionBean(beanClass, webBeansContext);
                try
                {
                    if (isEjb || (ClassUtil.isConcrete(beanClass) || WebBeansUtil.isDecorator(at)) && isValidManagedBean(at))
                    {
                        BeanAttributesImpl beanAttributes = BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(at, onlyScopedBeans && !isEjb).build();
                        if (beanAttributes != null &&
                                (!beanAttributes.isAlternative() || isEnabledAlternative(at, beanAttributes.getStereotypes())))
                        {
                            ProcessBeanAttributesImpl<?> processBeanAttributes
                                = webBeansContext.getWebBeansUtil().fireProcessBeanAttributes(at, at.getJavaClass(), beanAttributes);
                            if (processBeanAttributes != null)
                            {
                                BeanAttributes<?> newBeanAttributes = processBeanAttributes.getAttributes();
                                if (beanAttributes != newBeanAttributes)
                                {
                                    // check stereotypes
                                    for (Class<? extends Annotation> stereotype : newBeanAttributes.getStereotypes())
                                    {
                                        if (!webBeansContext.getBeanManagerImpl().isStereotype((stereotype)))
                                        {
                                            throw new WebBeansConfigurationException("Custom BeanAttributes#getStereotypes() must only contain Stereotypes!");
                                        }
                                    }
                                }

                                bdaBeanAttributes.put(at, new ExtendedBeanAttributes(newBeanAttributes, isEjb, processBeanAttributes.isIgnoreFinalMethods()));
                            }
                        }
                    }
                    else
                    {
                        iterator.remove();
                    }
                }
                catch (NoClassDefFoundError ncdfe)
                {
                    logger.info("Skipping deployment of Class " + beanClass + "due to a NoClassDefFoundError: " + ncdfe.getMessage());
                }
                catch (UnsatisfiedLinkError ule)
                {
                    logger.info("Skipping deployment of Class " + beanClass + "due to a UnsatisfiedLinkError: " + ule.getMessage());
                }
            }

            beanAttributesPerBda.put(bdaInfo, bdaBeanAttributes);
        }


        return beanAttributesPerBda;
    }

    private boolean isEnabledAlternative(AnnotatedType<?> at, Set<Class<? extends Annotation>> stereotypes)
    {
        AlternativesManager alternativesManager = webBeansContext.getAlternativesManager();

        if (alternativesManager.isAlternative(at.getJavaClass(), stereotypes))
        {
            return true;
        }
        if (stereotypes != null && !stereotypes.isEmpty() && at.getAnnotations(Priority.class) != null)
        {
            for (Class<? extends Annotation> stereotype : stereotypes)
            {
                if (alternativesManager.isAlternativeStereotype(stereotype))
                {
                    return true;
                }
            }

        }
        return false;
    }

    private void validateDisposeParameters()
    {
        WebBeansUtil webBeansUtil = webBeansContext.getWebBeansUtil();
        for (Bean<?> bean : webBeansContext.getBeanManagerImpl().getBeans())
        {
            if (ProducerMethodBean.class.isInstance(bean))
            {
                Producer<?> producer = AbstractProducerBean.class.cast(bean).getProducer();
                if (BaseProducerProducer.class.isInstance(producer))
                {
                    BaseProducerProducer producerProducer = BaseProducerProducer.class.cast(producer);
                    Set<InjectionPoint> disposalIPs = producerProducer.getDisposalIPs();
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
        for (Decorator<?> decorator : decoratorsManager.getDecorators())
        {
            Type type = decorator.getDelegateType();

            // capture ParameterizedType from decorator type
            Collection<Type> types = new HashSet<>();
            if (Class.class.isInstance(type))
            {
                Class<?> c = Class.class.cast(type);
                while (c != Object.class && c != null)
                {
                    types.add(c);
                    for (Type t : asList(c.getGenericInterfaces()))
                    {
                        if (ParameterizedType.class.isInstance(t))
                        {
                            types.add(t);
                        }
                    }
                    Type genericSuperclass = c.getGenericSuperclass();
                    if (ParameterizedType.class.isInstance(genericSuperclass))
                    {
                        types.add(genericSuperclass);
                    }
                    c = c.getSuperclass();
                }
            } // else?

            // check arguments matches with decorator API
            for (Type api : decorator.getTypes())
            {
                if (!ParameterizedType.class.isInstance(api)) // no need to check here
                {
                    continue;
                }

                ParameterizedType pt1 = ParameterizedType.class.cast(api);
                for (Type t : types)
                {
                    if (ParameterizedType.class.isInstance(t))
                    {
                        ParameterizedType pt2 = ParameterizedType.class.cast(t);

                        if (pt1.getRawType() == pt2.getRawType() &&
                            !GenericsUtil.isAssignableFrom(true, false, pt1, pt2, new HashMap<>()))
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
        webBeansContext.getBeanManagerImpl().getBeans().removeIf(bean -> !((OwbBean) bean).isEnabled());
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
                    Class<?> javaClass = annotatedType.getJavaClass();
                    interceptorsManager.addPriorityClazzInterceptor(javaClass, priority.value());
                }
            }
            if (annotatedType.getAnnotation(javax.decorator.Decorator.class) != null)
            {
                Priority priority = annotatedType.getAnnotation(Priority.class);
                if (priority != null)
                {
                    Class<?> javaClass = annotatedType.getJavaClass();
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

        beanManager.addInternalBean(webBeansUtil.getInterceptionFactoryBean());

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
                Constructor<?> c = webBeansContext.getSecurityService().doPrivilegedGetConstructor(clazz, WebBeansContext.class);
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
                catch (Exception e)
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
        for (AnnotatedTypeConfiguratorHolder holder : event.getAnnotatedTypeConfigurators())
        {
            manager.addAdditionalAnnotatedType(
                holder.getExtension(),
                holder.getAnnotatedTypeConfigurator().getNewAnnotatedType(),
                holder.getId());
        }

        for (AnnotatedTypeConfiguratorImpl<?> interceptorATC : event.getInterceptorBindingConfigurators())
        {
            webBeansContext.getInterceptorsManager().addInterceptorBindingType((AnnotatedType) interceptorATC.getNewAnnotatedType());
        }

        for (AnnotatedTypeConfiguratorImpl<?> qualifierAt : event.getQualifierConfigurators())
        {
            webBeansContext.getBeanManagerImpl().addAdditionalQualifier((AnnotatedType) qualifierAt.getNewAnnotatedType());

        }

        event.setStarted();
    }
    
    /**
     * Fires event after bean discovery.
     */
    private void fireAfterBeanDiscoveryEvent()
    {
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        manager.setAfterBeanDiscoveryStart();
        AfterBeanDiscoveryImpl event = new AfterBeanDiscoveryImpl(webBeansContext);
        manager.fireLifecycleEvent(event);

        event.deployConfiguredBeans();

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
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        List<AnnotatedType<?>> newAt = new LinkedList<>();
        List<Class<?>> interceptors = interceptorsManager.getPrioritizedInterceptors();
        List<Class<?>> decorators = decoratorsManager.getPrioritizedDecorators();
        List<Class<?>> alternatives = webBeansContext.getAlternativesManager().getPrioritizedAlternatives();

        // match AfterTypeDiscovery expected order (1, 2, 3...)
        Collections.reverse(interceptors);
        Collections.reverse(decorators);
        Collections.reverse(alternatives);

        AfterTypeDiscoveryImpl event = new AfterTypeDiscoveryImpl(webBeansContext, newAt,
                interceptors, decorators, alternatives);
        manager.fireLifecycleEvent(event);

        // reverse to keep "selection" order - decorator and interceptors considers it in their sorting.
        // NOTE: from here priorityClass.getSorted() MUST NOT be recomputed (ie no priorityClass.add(...))
        Collections.reverse(alternatives);

        for (AnnotatedTypeConfiguratorHolder holder : event.getAnnotatedTypeConfigurators())
        {
            AnnotatedType<?> at = holder.getAnnotatedTypeConfigurator().getNewAnnotatedType();
            manager.addAdditionalAnnotatedType(
                holder.getExtension(),
                at,
                holder.getId());

            newAt.add(at);
        }

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
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        manager.setAfterDeploymentValidationFired(true);
        AfterDeploymentValidationImpl event = new AfterDeploymentValidationImpl(manager);
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
            // If the class itself is annotated with @Alternative, then all is fine
            if (AnnotationUtil.hasClassAnnotation(alternativeClass, Alternative.class) ||
                AnnotationUtil.hasMetaAnnotation(alternativeClass.getAnnotations(), Alternative.class))
            {
                continue;
            }

            if (hasAlternativeProducerMethod(alternativeClass))
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

    private boolean hasAlternativeProducerMethod(Class<?> alternativeClass)
    {
        // It's also ok if the class has an @Alternative producer method
        List<Method> nonPrivateMethods = ClassUtil.getNonPrivateMethods(alternativeClass, true);
        for (Method method : nonPrivateMethods)
        {
            if ((method.getAnnotation(Alternative.class) != null ||
                AnnotationUtil.hasMetaAnnotation(method.getAnnotations(), Alternative.class))
                    && method.getAnnotation(Produces.class) != null )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Configure direct/indirect specialized producer method beans.
     * Also disable 'overwritten' producer method beans.
     * But only if they got overwritten in an enabled alternative.
     */
    public void configureProducerMethodSpecializations()
    {
        Set<Bean<?>> beans = webBeansContext.getBeanManagerImpl().getBeans();

        // Collect all producer method beans
        // and sort them with subclasses first
        // This is important as we can later go top-down and
        // disable all 'overwritten' producer methods which must be
        // further down in the list
        List<ProducerMethodBean> producerMethodBeans = beans.stream()
            .filter(ProducerMethodBean.class::isInstance)
            .map(ProducerMethodBean.class::cast)
            .collect(Collectors.toList());

        checkSpecializedProducerMethodConditions(producerMethodBeans);

        for (int i = 0; i < producerMethodBeans.size(); i++)
        {
            ProducerMethodBean<?> producerMethodBean = producerMethodBeans.get(i);
            if (!producerMethodBean.isEnabled())
            {
                continue;
            }

            for (int j = 0; j < producerMethodBeans.size(); j++)
            {
                ProducerMethodBean<?> otherProducerMethodBean = producerMethodBeans.get(j);

                if (i==j)
                {
                    // makes no sense to compare with ourselves
                    continue;
                }

                if (!otherProducerMethodBean.isEnabled())
                {
                    // already disabled
                    continue;
                }

                if (producerMethodBean.getBeanClass().equals(otherProducerMethodBean.getBeanClass()))
                {
                    // must be another producerMethod from the same class. Probably different qualifier?
                    continue;
                }

                if (otherProducerMethodBean.getBeanClass().isAssignableFrom(producerMethodBean.getBeanClass()))
                {
                    // yikes we did hit a superclass!

                    if (producerMethodBean.getCreatorMethod().getName().equals(otherProducerMethodBean.getCreatorMethod().getName()))
                    {
                        // disable the other bean as it got 'specialized' with the current bean.
                        otherProducerMethodBean.setEnabled(false);
                    }
                }
            }
        }
    }

    /**
     * Verify that all conditions for Specialized producer methdods are met.
     * See spec section 3.3.3. Specializing a producer method
     */
    private void checkSpecializedProducerMethodConditions(List<ProducerMethodBean> producerBeans)
    {
        Set<String> methodsDisabledDueToSpecialization = new HashSet<>();
        for (ProducerMethodBean producerBean : producerBeans)
        {
            if (producerBean.isSpecializedBean())
            {
                Method creatorMethod = producerBean.getCreatorMethod();

                Method overloadedMethod = webBeansContext.getSecurityService().doPrivilegedGetDeclaredMethod(
                        creatorMethod.getDeclaringClass().getSuperclass(),
                        creatorMethod.getName(),
                        creatorMethod.getParameterTypes());
                if (overloadedMethod == null)
                {
                    throw new WebBeansConfigurationException("Annotated producer method specialization failed : " + creatorMethod.getName()
                        + " not found in super class : " + creatorMethod.getDeclaringClass().getSuperclass().getName()
                        + " for annotated method : " + creatorMethod);
                }

                if (!AnnotationUtil.hasAnnotation(creatorMethod.getAnnotations(), Produces.class))
                {
                    throw new WebBeansConfigurationException("Annotated producer method specialization failed : " + creatorMethod.getName()
                        + " found in super class : " + creatorMethod.getDeclaringClass().getSuperclass().getName()
                        + " is not annotated with @Produces" + " for annotated method : " + creatorMethod);
                }

                String superMethod = overloadedMethod.toString();
                if (methodsDisabledDueToSpecialization.contains(superMethod))
                {
                    throw new WebBeansDeploymentException("Multiple specializations for the same producer method got detected for type"
                        + producerBean);
                }
                methodsDisabledDueToSpecialization.add(superMethod);
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
        validateObservers(webBeansContext.getNotificationManager().getObserverMethods());

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
            LinkedList<String> beanNames = new LinkedList<>();
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
                            Class<?> proxyable = BuiltInOwbBean.class.cast(bean).proxyableType();
                            if (proxyable != null)
                            {
                                AbstractProducer producer = AbstractProducer.class.cast(OwbBean.class.cast(bean).getProducer());
                                AnnotatedType<?> annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(proxyable);
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

    private void validateBeanNames(LinkedList<String> beanNames)
    {
        if(beanNames.size() > 0)
        {   
            InjectionResolver resolver = webBeansContext.getBeanManagerImpl().getInjectionResolver();

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
        Set<Class<?>> foundClasses = new HashSet<>(100);

        Map<BeanArchiveInformation, List<AnnotatedType<?>>> annotatedTypesPerBda
            = new HashMap<>();

        if (scanner instanceof BdaScannerService)
        {
            Map<BeanArchiveInformation, Set<Class<?>>> beanClassesPerBda = ((BdaScannerService) scanner).getBeanClassesPerBda();

            for (Map.Entry<BeanArchiveInformation, Set<Class<?>>> bdaEntry : beanClassesPerBda.entrySet())
            {
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
        List<AnnotatedType<?>> annotatedTypes = new ArrayList<>();

        //Iterating over each class
        if (classIndex != null)
        {
            AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();
            boolean hasPATObserver = webBeansContext.getNotificationManager().hasProcessAnnotatedTypeObservers();
            for (Class<?> implClass : classIndex)
            {
                if (foundClasses.contains(implClass) || implClass.isAnonymousClass() ||
                        Modifier.isPrivate(implClass.getModifiers() /* likely inner class */))
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
                        for (InjectionPoint ip : it.getInjectionPoints())
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

                    // trigger a NoClassDefFoundError here, otherwise it would be thrown in observer methods
                    Class<?> javaClass = annotatedType.getJavaClass();
                    if (!skipNoClassDefFoundTriggers)
                    {
                        javaClass.getDeclaredMethods();
                        javaClass.getDeclaredFields();
                    }

                    // Fires ProcessAnnotatedType
                    if (hasPATObserver && !javaClass.isAnnotation())
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
                catch (UnsatisfiedLinkError ule)
                {
                    logger.info("Skipping deployment of Class " + implClass + "due to a UnsatisfiedLinkError: " + ule.getMessage());
                }
            }
        }

        return annotatedTypes;
    }

    private boolean isEEComponent(Class<?> impl)
    {
        OpenWebBeansJavaEEPlugin eePlugin = webBeansContext.getPluginLoader().getJavaEEPlugin();
        return eePlugin != null && eePlugin.isEEComponent(impl);
    }

    private boolean isVetoed(Class<?> implClass)
    {
        if (implClass.getAnnotation(Vetoed.class) != null)
        {
            return true;
        }

        Package pckge = implClass.getPackage();
        if (pckge == null)
        {
            return false;
        }

        do
        {
            // yes we cache result with potentially different classloader but this is not portable by spec
            String name = pckge.getName();
            Boolean packageVetoed = packageVetoCache.get(name);
            if (packageVetoed == null)
            {
                if (pckge.getAnnotation(Vetoed.class) != null)
                {
                    packageVetoCache.put(pckge.getName(), true);
                    return true;
                }
                else
                {
                    packageVetoCache.put(pckge.getName(), false);
                }
            }
            else if (packageVetoed)
            {
                return true;
            }

            if (skipVetoedOnPackages) // we want to avoid loadClass with this property, not cached reflection
            {
                return false;
            }

            ClassLoader classLoader = implClass.getClassLoader();
            if (classLoader == null)
            {
                classLoader = BeansDeployer.class.getClassLoader();
            }

            int idx = name.lastIndexOf('.');
            if (idx > 0)
            {
                String previousPackage = name.substring(0, idx);
                Boolean result = packageVetoCache.get(previousPackage);
                if (result != null)
                {
                    return result;
                }
                while (true)
                {
                    try // not always existing but enables to go further when getPackage is not available (graal)
                    {
                        pckge = classLoader.loadClass(previousPackage +
                                (previousPackage.isEmpty() ? "" :".") + "package-info").getPackage();
                        break;
                    }
                    catch (Exception e)
                    {
                        if (previousPackage.isEmpty())
                        {
                            pckge = null;
                            break;
                        }
                        packageVetoCache.put(previousPackage, false);
                        idx = previousPackage.lastIndexOf('.');
                        if (idx > 0)
                        {
                            previousPackage = previousPackage.substring(0, idx);
                        }
                        else
                        {
                            previousPackage = "";
                        }
                    }
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
     * Process any AnnotatedTypes which got added by BeforeBeanDiscovery#addAnnotatedType and other events
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
    protected void deployFromBeanAttributes( Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> beanAttributesPerBda )
    {
        logger.fine("Deploying configurations from class files has started.");

        BeanManagerImpl bm = webBeansContext.getBeanManagerImpl();
        for (Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> beanAttributesMap : beanAttributesPerBda.values())
        {

            // Start from the class
            for (Map.Entry<AnnotatedType<?>, ExtendedBeanAttributes<?>> annotatedType : beanAttributesMap.entrySet())
            {
                final AnnotatedType<?> key = annotatedType.getKey();
                final Collection<? extends AnnotatedType<?>> userAnnotatedTypes =
                        bm.getUserAnnotatedTypes(key.getJavaClass());
                // if we have a matching AT (same type+annotations+default id) we skip it since we already deployed it
                if (userAnnotatedTypes != null && userAnnotatedTypes.stream().anyMatch(it ->
                        it != key &&
                        AnnotatedTypeWrapper.class.isInstance(it) &&
                        AnnotatedTypeWrapper.class.cast(it).getId().endsWith(AnnotatedElementFactory.OWB_DEFAULT_KEY) &&
                        it.getAnnotations().equals(key.getAnnotations()))) // strictly it is qualifiers only but faster
                {
                    continue;
                }
                try
                {
                    deploySingleAnnotatedType(key, annotatedType.getValue(), beanAttributesMap);
                }
                catch (NoClassDefFoundError ncdfe)
                {
                    logger.info("Skipping deployment of Class " + key.getJavaClass() + "due to a NoClassDefFoundError: " + ncdfe.getMessage());
                }
                catch (UnsatisfiedLinkError ule)
                {
                    logger.info("Skipping deployment of Class " + key.getJavaClass() + "due to a UnsatisfiedLinkError: " + ule.getMessage());
                }

                // if the implClass already gets processed as part of the
                // standard BDA scanning, then we don't need to 'additionally'
                // deploy it anymore.
                bm.removeAdditionalAnnotatedType(key);

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
                    defineManagedBean(annotatedType, annotatedTypeData, annotatedTypes);
                }
            }
            catch (NoClassDefFoundError ncdfe)
            {
                logger.warning("Skipping deployment of Class " + beanClass + " due to a NoClassDefFoundError: " + ncdfe.getMessage());
            }
            catch (UnsatisfiedLinkError ule)
            {
                logger.info("Skipping deployment of Class " + beanClass + "due to a UnsatisfiedLinkError: " + ule.getMessage());
            }
        }
    }

    private boolean isValidManagedBean(AnnotatedType<?> type)
    {
        Class<?> beanClass = type.getJavaClass();
        WebBeansUtil webBeansUtil = webBeansContext.getWebBeansUtil();

        try
        {
            webBeansUtil.checkManagedBean(beanClass);
        }
        catch (DefinitionException e)
        {
            logger.log(Level.FINE, "skipped deployment of: " + beanClass.getName() + " reason: " + e.getMessage());
            logger.log(Level.FINER, "skipped deployment of: " + beanClass.getName() + " details: ", e);
            return false;
        }

        // done separately to be able to swallow the logging when not relevant and avoid to pollute logs
        try
        {
            if (!webBeansUtil.isConstructorOk(type))
            {
                return false;
            }
        }
        catch (TypeNotPresentException cnfe)
        {
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

        for (URL url : bdaLocations)
        {
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

        // the alternatives in this beans.xml
        // this gets used to detect multiple definitions of the
        // same alternative in one beans.xml file.
        Set<String> alternativesInFile = new HashSet<>();

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
        Set<Class> decoratorsInFile = new HashSet<>();

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
        Set<Class> interceptorsInFile = new HashSet<>();
        
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
                    int priority = -1;
                    if (!isBDAScanningEnabled)
                    {
                        priority = interceptorsManager.getPriority(clazz);
                    }
                    logger.fine( "Interceptor class : " + interceptor + " is already defined" +
                            (priority >= 0 ? " with priority " + priority : ""));
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
     */
    protected void checkStereoTypes(Map<BeanArchiveInformation, Map<AnnotatedType<?>, ExtendedBeanAttributes<?>>> beanAttributesPerBda)
    {
        logger.fine("Checking StereoType constraints has started.");

        addDefaultStereoTypes();

        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        StereoTypeManager stereoTypeManager = webBeansContext.getStereoTypeManager();

        // all the Stereotypes we did already check
        Set<Class<? extends Annotation>> verifiedStereotypes = new HashSet<>();

        for (Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> annotatedTypeExtendedBeanAttributesMap : beanAttributesPerBda.values())
        {
            for (ExtendedBeanAttributes<?> extendedBeanAttributes : annotatedTypeExtendedBeanAttributesMap.values())
            {
                Set<Class<? extends Annotation>> stereotypes = extendedBeanAttributes.beanAttributes.getStereotypes();

                for (Class<? extends Annotation> stereoClass : stereotypes)
                {
                    if (verifiedStereotypes.contains(stereoClass))
                    {
                        continue;
                    }
                    if (annotationManager.isStereoTypeAnnotation(stereoClass)
                        && stereoTypeManager.getStereoTypeModel(stereoClass.getName()) == null)
                    {
                        webBeansContext.getAnnotationManager().checkStereoTypeClass(stereoClass, stereoClass.getDeclaredAnnotations());
                        StereoTypeModel model = new StereoTypeModel(webBeansContext, stereoClass);
                        stereoTypeManager.addStereoTypeModel(model);
                    }
                    verifiedStereotypes.add(stereoClass);
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
     */
    protected <T> void defineManagedBean(AnnotatedType<T> annotatedType,
                                         ExtendedBeanAttributes extendedBeanAttributes,
                                         Map<AnnotatedType<?>, ExtendedBeanAttributes<?>> annotatedTypes)
    {
        BeanAttributes attributes = extendedBeanAttributes.beanAttributes;

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
            ManagedBeanBuilder<T, ManagedBean<T>> managedBeanCreator
                = new ManagedBeanBuilder<>(webBeansContext, annotatedType, attributes, extendedBeanAttributes.ignoreFinalMethods);

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

                    //Fires ProcessBean
                    ProcessBeanImpl<T> processBeanEvent = new GProcessBean(decorator, annotatedType);
                    webBeansContext.getBeanManagerImpl().fireEvent(processBeanEvent, true);
                    processBeanEvent.setStarted();

                    webBeansContext.getWebBeansUtil().inspectDefinitionErrorStack("There are errors that are added by ProcessBean event observers for " +
                        "interceptor beans. Look at logs for further details");

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

                    //Fires ProcessBean
                    ProcessBeanImpl<T> processBeanEvent = new GProcessBean(interceptor, annotatedType);
                    webBeansContext.getBeanManagerImpl().fireEvent(processBeanEvent, true);
                    processBeanEvent.setStarted();

                    webBeansContext.getWebBeansUtil().inspectDefinitionErrorStack("There are errors that are added by ProcessBean event observers for " +
                        "interceptor beans. Look at logs for further details");

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

                Set<ObserverMethod<?>> observerMethods;
                AnnotatedType<T> beanAnnotatedType = bean.getAnnotatedType();
                if(bean.isEnabled())
                {
                    observerMethods = new ObserverMethodsBuilder<>(webBeansContext, beanAnnotatedType).defineObserverMethods(bean);
                }
                else
                {
                    observerMethods = new HashSet<>();
                }

                WebBeansContext wbc = bean.getWebBeansContext();
                Set<ProducerFieldBean<?>> producerFields = new ProducerFieldBeansBuilder(wbc, beanAnnotatedType).defineProducerFields(bean);
                Set<ProducerMethodBean<?>> producerMethods = new ProducerMethodBeansBuilder(wbc, beanAnnotatedType).defineProducerMethods(bean, producerFields);

                ManagedBean<T> managedBean = (ManagedBean<T>)bean;
                Map<ProducerMethodBean<?>,AnnotatedMethod<?>> annotatedMethods =
                    new HashMap<>();

                if (!producerFields.isEmpty() || !producerMethods.isEmpty())
                {
                    Priority priority = annotatedType.getAnnotation(Priority.class);
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
                    new HashMap<>();

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
                    new HashMap<>();

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
        private final boolean ignoreFinalMethods;

        public ExtendedBeanAttributes(BeanAttributes<T> beanAttributes, boolean isEjb, boolean ignoreFinalMethods)
        {
            this.beanAttributes = beanAttributes;
            this.isEjb = isEjb;
            this.ignoreFinalMethods = ignoreFinalMethods;
        }
    }
}
