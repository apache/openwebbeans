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
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;

import javax.enterprise.inject.Model;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.interceptor.Interceptor;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.NewBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.creation.ManagedBeanCreatorImpl;
import org.apache.webbeans.component.creation.BeanCreator.MetaDataProvider;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.inject.InconsistentSpecializationException;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.OpenWebBeansJavaEEPlugin;
import org.apache.webbeans.plugins.OpenWebBeansWebPlugin;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.portable.events.ProcessAnnotatedTypeImpl;
import org.apache.webbeans.portable.events.discovery.AfterBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.discovery.AfterDeploymentValidationImpl;
import org.apache.webbeans.portable.events.discovery.BeforeBeanDiscoveryImpl;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.WebBeansAnnotatedTypeUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;
import org.apache.webbeans.xml.XMLSpecializesManager;

/**
 * Deploys the all beans that are defined in the {@link WebBeansScanner} at
 * the scanner phase.
 */
@SuppressWarnings("unchecked")
public class BeansDeployer
{
    //Logger instance
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(BeansDeployer.class);

    /**Deployment is started or not*/
    protected boolean deployed = false;

    /**XML Configurator*/
    protected WebBeansXMLConfigurator xmlConfigurator = null;
    
    /**Discover ejb or not*/
    protected boolean discoverEjb = false;

    /**
     * Creates a new deployer with given xml configurator.
     * 
     * @param xmlConfigurator xml configurator
     */
    public BeansDeployer(WebBeansXMLConfigurator xmlConfigurator)
    {
        this.xmlConfigurator = xmlConfigurator;
        String usage = OpenWebBeansConfiguration.getInstance().getProperty(OpenWebBeansConfiguration.USE_EJB_DISCOVERY);
        this.discoverEjb = Boolean.parseBoolean(usage);
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
    public void deploy(ScannerService scanner)
    {
        try
        {
            if (!deployed)
            {                
                //Load Extensions
                ExtensionLoader.getInstance().loadExtensionServices();

                // Bind manager
                JNDIService service = ServiceLoader.getService(JNDIService.class);
                service.bind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME, BeanManagerImpl.getManager());

                // Register Manager built-in component
                BeanManagerImpl.getManager().addBean(WebBeansUtil.getManagerBean());

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
                
                //Check Specialization
                checkSpecializations(scanner);
                
                //Fire Event
                fireAfterBeanDiscoveryEvent();
                
                //Validate injection Points
                validateInjectionPoints();
                
                //Fire Event
                fireAfterDeploymentValidationEvent();
                
                deployed = true;
            }

        }
        catch (WebBeansConfigurationException e)
        {
            logger.error(e);
            throw e;
        }
        catch(Exception e)
        {
            logger.error(e);
            
            if(e instanceof WebBeansDeploymentException)
            {
                throw  (WebBeansDeploymentException)e;
            }
            
            throw new WebBeansDeploymentException(e);
        }
    }
    
    /**
     * Configure Default Beans.
     */
    private void configureDefaultBeans()
    {
        BeanManagerImpl beanManager = BeanManagerImpl.getManager();
        
        // Register Conversation built-in component
        beanManager.addBean(WebBeansUtil.getConversationBean());
        
        // Register InjectionPoint bean
        beanManager.addBean(WebBeansUtil.getInjectionPointBean());
        
        //Register Instance Bean
        beanManager.addBean(WebBeansUtil.getInstanceBean());
        
        //Register Event Bean
        beanManager.addBean(WebBeansUtil.getEventBean());
        
        //REgister Provider Beans
        OpenWebBeansJavaEEPlugin beanEeProvider = PluginLoader.getInstance().getJavaEEPlugin();
        OpenWebBeansWebPlugin beanWebProvider = PluginLoader.getInstance().getWebPlugin();
        
        if(beanEeProvider != null)
        {
            if(beanEeProvider.getValidatorBean() != null)
            {
                //Register Validator Bean
                beanManager.addBean(beanEeProvider.getValidatorBean());                
            }
            
            if(beanEeProvider.getValidatorFactoryBean() != null)
            {
                //Register ValidatorFactory Bean
                beanManager.addBean(beanEeProvider.getValidatorFactoryBean());                
            }
            
            if(beanEeProvider.getPrincipalBean() != null)
            {
                //Register Principal Bean
                beanManager.addBean(beanEeProvider.getPrincipalBean());                
            }
            
            if(beanEeProvider.getUserTransactionBean() != null)
            {
                //User Transaction Bean
                beanManager.addBean(beanEeProvider.getUserTransactionBean());                
            }
        }       
        else if(beanWebProvider != null)
        {
            if(beanWebProvider.getPrincipalBean() != null)
            {
                //Register Principal Bean
                beanManager.addBean(beanWebProvider.getPrincipalBean());                
            }
            
        }
            
    }
    
    /**
     * Fires event before bean discovery.
     */
    private void fireBeforeBeanDiscoveryEvent()
    {
        BeanManager manager = BeanManagerImpl.getManager();
        manager.fireEvent(new BeforeBeanDiscoveryImpl(),new Annotation[0]);
    }
    
    /**
     * Fires event after bean discovery.
     */
    private void fireAfterBeanDiscoveryEvent()
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        manager.fireEvent(new AfterBeanDiscoveryImpl(),new Annotation[0]);
        
        WebBeansUtil.inspectErrorStack("There are errors that are added by AfterBeanDiscovery event observers. Look at logs for further details");
    }
    
    /**
     * Fires event after deployment valdiation.
     */
    private void fireAfterDeploymentValidationEvent()
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        manager.fireEvent(new AfterDeploymentValidationImpl(),new Annotation[0]);
        
        WebBeansUtil.inspectErrorStack("There are errors that are added by AfterDeploymentValidation event observers. Look at logs for further details");
    }
    
    /**
     * Validate all injection points.
     */
    private void validateInjectionPoints()
    {
        logger.debug("Validation of injection points has started.");

        BeanManagerImpl manager = BeanManagerImpl.getManager();        
        Set<Bean<?>> beans = new HashSet<Bean<?>>();
        
        //Adding decorators to validate
        Set<Decorator<?>> decorators = manager.getDecorators();
        for(Decorator decorator : decorators)
        {
            WebBeansDecorator wbDec = (WebBeansDecorator)decorator;
            beans.add(wbDec);
        }
        
        
        logger.debug("Validation of the decorator's injection points has started.");
        
        //Validate Decorators
        validate(beans);
        
        beans.clear();
        
        //Adding interceptors to validate
        Set<javax.enterprise.inject.spi.Interceptor<?>> interceptors = manager.getInterceptors();
        for(javax.enterprise.inject.spi.Interceptor interceptor : interceptors)
        {
            WebBeansInterceptor wbInt = (WebBeansInterceptor)interceptor;
            beans.add(wbInt);
        }
        
        logger.debug("Validation of the interceptor's injection points has started.");
        
        //Validate Interceptors
        validate(beans);
        
        beans.clear();
        
        beans = manager.getBeans();
        
        //Validate Others
        validate(beans);
                

        logger.info(OWBLogConst.INFO_0008);
    }
    
    /**
     * Validates beans.
     * 
     * @param beans deployed beans
     */
    private void validate(Set<Bean<?>> beans)
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        
        if (beans != null && beans.size() > 0)
        {
            for (Bean<?> bean : beans)
            {
                //Configure decorator and interceptor stack for ManagedBeans
                if((bean instanceof AbstractInjectionTargetBean) && 
                        !(bean instanceof NewBean))
                {
                    if(!(bean instanceof Decorator) && !(bean instanceof javax.enterprise.inject.spi.Interceptor))
                    {
                        DefinitionUtil.defineDecoratorStack((AbstractInjectionTargetBean<Object>)bean);   
                    }
                    
                    if(!(bean instanceof javax.enterprise.inject.spi.Interceptor) &&
                            !(bean instanceof NewBean))
                    {
                        DefinitionUtil.defineBeanInterceptorStack((AbstractInjectionTargetBean<Object>)bean);   
                    }
                }
                
                checkPassivationScope(bean);
                                
                //Bean injection points
                Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
                                
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
                            if(!bean.getBeanClass().isAnnotationPresent(javax.decorator.Decorator.class))
                            {
                                throw new WebBeansConfigurationException("Delegate injection points can not defined by beans that are not decorator. Injection point : " + injectionPoint);
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
        logger.debug("Deploying configurations from class files has started.");

        // Start from the class
        Set<Class<?>> classIndex = scanner.getBeanClasses();
        
        if (classIndex != null)
        {
            for(Class<?> implClass : classIndex)
            {               
                if (ManagedBeanConfigurator.isManagedBean(implClass))
                {
                    ManagedBeanConfigurator.checkManagedBeanCondition(implClass);
                    defineManagedBean(implClass);
                }
                else if(this.discoverEjb)
                {                    
                    if(EJBWebBeansConfigurator.isSessionBean(implClass))
                    {
                        logger.info(OWBLogConst.INFO_0010, new Object[]{implClass.getName()});
                        defineEnterpriseWebBean(implClass);                        
                    }
                }
            }
        }

        logger.debug("Deploying configurations from class files has ended.");

    }
    
    /**
     * Discovers and deploys classes from XML.
     * 
     * NOTE : Currently XML file is just used for configuring.
     * 
     * @param scanner discovery scanner
     * @throws WebBeansDeploymentException if exception
     */
    protected void deployFromXML(ScannerService scanner) throws WebBeansDeploymentException
    {
        logger.debug("Deploying configurations from XML files has started.");

        Set<URL> xmlLocations = scanner.getBeanXmls();
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

        logger.debug("Deploying configurations from XML has ended successfully.");
    }
    

    /**
     * Checks specialization.
     * @param scanner scanner instance
     */
    protected void checkSpecializations(ScannerService scanner)
    {
        logger.debug("Checking Specialization constraints has started.");
        
        try
        {
            Set<Class<?>> beanClasses = scanner.getBeanClasses();
            if (beanClasses != null && beanClasses.size() > 0)
            {
                //superClassList is used to handle the case: Car, CarToyota, Bus, SchoolBus, CarFord
                //for which case, the owb should throw exception that both CarToyota and CarFord are 
                //specialize Car. 
                Class<?> superClass = null;
                ArrayList<Class<?>> superClassList = new ArrayList<Class<?>>();
                ArrayList<Class<?>> specialClassList = new ArrayList<Class<?>>();
                for(Class<?> specialClass : beanClasses)
                {
                    if(AnnotationUtil.hasClassAnnotation(specialClass, Specializes.class))
                    {
                        superClass = specialClass.getSuperclass();
                        if(superClass.equals(Object.class))
                        {
                            throw new WebBeansConfigurationException(logger.getTokenString(OWBLogConst.EXCEPT_0003) + specialClass.getName()
                                                                     + logger.getTokenString(OWBLogConst.EXCEPT_0004));
                        }
                        if (superClassList.contains(superClass))
                        {
                            throw new InconsistentSpecializationException(logger.getTokenString(OWBLogConst.EXCEPT_0005) + superClass.getName());
                        }
                        superClassList.add(superClass);
                        specialClassList.add(specialClass);
                    }
                }
                WebBeansUtil.configureSpecializations(specialClassList);                        
            }

            // XML Defined Specializations
            checkXMLSpecializations();
            
            //configure specialized producer beans.
            WebBeansUtil.configureProducerMethodSpecializations();
        }
        catch(Exception e)
        {
            throw new WebBeansDeploymentException(e);
        }
        

        logger.debug("Checking Specialization constraints has ended.");
    }

    
    /**
     * Check xml specializations.
     * NOTE : Currently XML is not used in configuration.
     */
    protected void checkXMLSpecializations()
    {
        // Check XML specializations
        Set<Class<?>> clazzes = XMLSpecializesManager.getInstance().getXMLSpecializationClasses();
        Iterator<Class<?>> it = clazzes.iterator();
        Class<?> superClass = null;
        Class<?> specialClass = null;
        ArrayList<Class<?>> specialClassList = new ArrayList<Class<?>>();
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
                    throw new InconsistentSpecializationException(logger.getTokenString(OWBLogConst.EXCEPT_XML) + logger.getTokenString(OWBLogConst.EXCEPT_0005)
                                                                 + superClass.getName());
                }
            }
            specialClassList.add(specialClass);
        }
        WebBeansUtil.configureSpecializations(specialClassList);
    }

    /**
     * Check passivations.
     */
    protected void checkPassivationScope(Bean<?> beanObj)
    {
        if(BeanManagerImpl.getManager().isPassivatingScope(beanObj.getScope()))
        {
            if(WebBeansUtil.isPassivationCapable(beanObj) == null)
            {
                if(!(beanObj instanceof AbstractProducerBean))
                {
                    throw new WebBeansConfigurationException("Passivation scoped defined bean must be passivation capable, " +
                            "but bean : " + beanObj.toString() + " is not passivation capable");                    
                }
            }
            
            ((OwbBean<?>)beanObj).validatePassivationDependencies();
        } 
    }

    /**
     * Check steretypes.
     * @param scanner scanner instance
     */
    protected void checkStereoTypes(ScannerService scanner)
    {
        logger.debug("Checking StereoType constraints has started.");

        addDefaultStereoTypes();
        
        Set<Class<?>> beanClasses = scanner.getBeanClasses();
        if (beanClasses != null && beanClasses.size() > 0)
        {
            for(Class<?> beanClass : beanClasses)
            {                
                if(beanClass.isAnnotation())
                {
                    Class<? extends Annotation> stereoClass = (Class<? extends Annotation>) beanClass;                    
                    if (AnnotationUtil.isStereoTypeAnnotation(stereoClass))
                    {
                        if (!XMLAnnotationTypeManager.getInstance().hasStereoType(stereoClass))
                        {
                            WebBeansUtil.checkStereoTypeClass(stereoClass);
                            StereoTypeModel model = new StereoTypeModel(stereoClass);
                            StereoTypeManager.getInstance().addStereoTypeModel(model);
                        }
                    }                    
                }
            }
        }

        logger.debug("Checking StereoType constraints has ended.");
    }

    /**
     * Adds default stereotypes.
     */
    protected void addDefaultStereoTypes()
    {
        StereoTypeModel model = new StereoTypeModel(Model.class);
        StereoTypeManager.getInstance().addStereoTypeModel(model);
        
        model = new StereoTypeModel(javax.decorator.Decorator.class);
        StereoTypeManager.getInstance().addStereoTypeModel(model);
        
        model = new StereoTypeModel(Interceptor.class);
        StereoTypeManager.getInstance().addStereoTypeModel(model);        
    }
    
    /**
     * Defines and configures managed bean.
     * @param <T> type info
     * @param clazz bean class
     */
    protected <T> void defineManagedBean(Class<T> clazz)
    {
        AnnotatedType<T> annotatedType = AnnotatedElementFactory.newAnnotatedType(clazz);
        
        //Fires ProcessAnnotatedType
        ProcessAnnotatedTypeImpl<T> processAnnotatedEvent = WebBeansUtil.fireProcessAnnotatedTypeEvent(annotatedType);      
        
        ManagedBean<T> managedBean = new ManagedBean<T>(clazz,WebBeansType.MANAGED);                  
        ManagedBeanCreatorImpl<T> managedBeanCreator = new ManagedBeanCreatorImpl<T>(managedBean);
        
        if(processAnnotatedEvent.isVeto())
        {
            return;
        }
        
        boolean annotationTypeSet = false;
        if(processAnnotatedEvent.isSet())
        {
            annotationTypeSet = true;
            managedBean.setAnnotatedType(annotatedType);
            annotatedType = processAnnotatedEvent.getAnnotatedType();
            managedBeanCreator.setAnnotatedType(annotatedType);
            managedBeanCreator.setMetaDataProvider(MetaDataProvider.THIRDPARTY);
        }
        
        //Decorator
        if(WebBeansAnnotatedTypeUtil.isAnnotatedTypeDecorator(annotatedType))
        {
            logger.info(OWBLogConst.INFO_0012, new Object[]{annotatedType.getJavaClass().getName()});
            if(annotationTypeSet)
            {
                WebBeansAnnotatedTypeUtil.defineDecorator(annotatedType);
            }
            else
            {
                WebBeansUtil.defineDecorator(managedBeanCreator, annotatedType);
            }
        }
        //Interceptor
        else if(WebBeansAnnotatedTypeUtil.isAnnotatedTypeInterceptor(annotatedType))
        {
            logger.info(OWBLogConst.INFO_0011, new Object[]{annotatedType.getJavaClass().getName()});
            if(annotationTypeSet)
            {
                WebBeansAnnotatedTypeUtil.defineInterceptor(annotatedType);
            }
            else
            {
                WebBeansUtil.defineInterceptor(managedBeanCreator, annotatedType);
            }
        }
        else
        {
            logger.info(OWBLogConst.INFO_0009, new Object[]{annotatedType.getJavaClass().getName()});
            WebBeansUtil.defineManagedBean(managedBeanCreator, annotatedType);   
        }
    }
    
    /**
     * Defines enterprise bean via plugin.
     * @param <T> bean class type
     * @param clazz bean class
     */
    protected <T> void defineEnterpriseWebBean(Class<T> clazz)
    {
        InjectionTargetBean<T> bean = (InjectionTargetBean<T>) EJBWebBeansConfigurator.defineEjbBean(clazz);
        WebBeansUtil.setInjectionTargetBeanEnableFlag(bean);
    }
}
