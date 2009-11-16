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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.Producer;
import javax.interceptor.Interceptor;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.creation.ManagedBeanCreatorImpl;
import org.apache.webbeans.component.creation.BeanCreator.MetaDataProvider;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.inject.InconsistentSpecializationException;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.portable.events.ProcessAnnotatedTypeImpl;
import org.apache.webbeans.portable.events.ProcessBeanImpl;
import org.apache.webbeans.portable.events.ProcessInjectionTargetImpl;
import org.apache.webbeans.portable.events.ProcessProducerImpl;
import org.apache.webbeans.portable.events.discovery.AfterBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.discovery.AfterDeploymentValidationImpl;
import org.apache.webbeans.portable.events.discovery.BeforeBeanDiscoveryImpl;
import org.apache.webbeans.portable.events.generics.GProcessManagedBean;
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
                //Load Extensions
                ExtensionLoader.getInstance().loadExtensionServices();

                // Bind manager
                JNDIService service = ServiceLoader.getService(JNDIService.class);
                service.bind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME, BeanManagerImpl.getManager());

                //Fire Event
                fireBeforeBeanDiscoveryEvent();
                
                //Deploy bean from XML. Also configures deployments, interceptors, decorators.
                deployFromXML(scanner);
                
                //Checking stereotype conditions
                checkStereoTypes(scanner);
                
                //Configure Interceptors
                configureInterceptors(scanner);
                
                //Configure Decorators
                configureDecorators(scanner);
                
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
            e.printStackTrace();
            throw e;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            
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
    
    private void configureDefaultBeans()
    {
        // Register Manager built-in component
        BeanManagerImpl.getManager().addBean(WebBeansUtil.getManagerBean());

        // Register Conversation built-in component
        BeanManagerImpl.getManager().addBean(WebBeansUtil.getConversationBean());
        
        // Register InjectionPoint bean
        BeanManagerImpl.getManager().addBean(WebBeansUtil.getInjectionPointBean());
        
        //Register Instance Bean
        BeanManagerImpl.getManager().addBean(WebBeansUtil.getInstanceBean());        
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
        BeanManager manager = BeanManagerImpl.getManager();
        manager.fireEvent(new AfterBeanDiscoveryImpl(),new Annotation[0]);
        
    }
    
    /**
     * Fires event after deployment valdiation.
     */
    private void fireAfterDeploymentValidationEvent()
    {
        BeanManager manager = BeanManagerImpl.getManager();
        manager.fireEvent(new AfterDeploymentValidationImpl(),new Annotation[0]);        
    }
    
    /**
     * Validate all injection points.
     */
    private void validateInjectionPoints()
    {
        logger.info("Validation of injection points are started");

        BeanManagerImpl manager = BeanManagerImpl.getManager();        
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
                //Bean injection points
                Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
                                
                for (InjectionPoint injectionPoint : injectionPoints)
                {
                    manager.validate(injectionPoint);
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
                if(AnnotationUtil.hasClassAnnotation(implClass, javax.decorator.Decorator.class) ||
                        AnnotationUtil.hasClassAnnotation(implClass, Interceptor.class))
                {
                    continue;
                }
                
                if (ManagedBeanConfigurator.isManagedBean(implClass))
                {
                    logger.info("Managed Bean with class name : " + componentClassName + " is found");
                    defineManagedBean(implClass);
                }
                else if(this.discoverEjb)
                {                    
                    if(EJBWebBeansConfigurator.isSessionBean(implClass))
                    {
                        logger.info("Enterprise Bean with class name : " + componentClassName + " is found");
                        defineEnterpriseWebBean(implClass);
                        
                    }
                }
            }
        }

        logger.info("Deploying configurations from class files is ended");

    }
    
    /**
     * Discovers and deploys classes from XML.
     * 
     * NOTE : Currently XML file is just used for configuring.
     * 
     * @param scanner discovery scanner
     * @throws WebBeansDeploymentException if exception
     */
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
    
    /**
     * Discovers and deploys interceptors.
     * 
     * @param scanner discovery scanner
     * @throws ClassNotFoundException if class not found
     */
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

                logger.info("Managed Bean Interceptor with class name : " + interceptorClazz + " is found");

                defineInterceptor(implClass);
            }
        }

        logger.info("Configuring the Interceptors is ended");

    }

    /**
     * Discovers and deploys decorators.
     * 
     * @param scanner discovery scanner
     * @throws ClassNotFoundException if class not found
     */
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
                logger.info("Managed Bean Decorator with class name : " + decoratorClazz + " is found");

                defineDecorator(implClass);
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
        Set<Bean<?>> beans = BeanManagerImpl.getManager().getBeans();

        if (beans != null && beans.size() > 0)
        {
            Iterator<Bean<?>> itBeans = beans.iterator();
            while (itBeans.hasNext())
            {
                Object beanObj = itBeans.next();
                if (beanObj instanceof ManagedBean)
                {
                    ManagedBean<?> component = (ManagedBean<?>) beanObj;
                    NormalScope scope = component.getScope().getAnnotation(NormalScope.class);
                    if(scope != null)
                    {
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
                    if (!XMLAnnotationTypeManager.getInstance().hasStereoType(stereoClass))
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
    
    /**
     * Defines and creates a new {@link ManagedBean}.
     * 
     * <p>
     * It fires each event that is defined in the specification
     * section 11.5, <b>Container Lifecycle Events</b>
     * </p>
     * 
     * @param <T> bean class
     * @param clazz managed bean class
     */
    protected <T> void defineManagedBean(Class<T> clazz)
    {
        if (!AnnotationUtil.hasClassAnnotation(clazz, Interceptor.class) && !AnnotationUtil.hasClassAnnotation(clazz, javax.decorator.Decorator.class))
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
            
            if(processAnnotatedEvent.isSet())
            {
                managedBeanCreator.setMetaDataProvider(MetaDataProvider.THIRDPARTY);
            }
            
            //Define meta-data
            managedBeanCreator.defineSerializable();
            managedBeanCreator.defineStereoTypes();
            Class<? extends Annotation> deploymentType = managedBeanCreator.defineDeploymentType("There are more than one @DeploymentType annotation in ManagedBean implementation class : " + managedBean.getReturnType().getName());
            managedBeanCreator.defineApiType();
            managedBeanCreator.defineScopeType("ManagedBean implementation class : " + clazz.getName() + " stereotypes must declare same @Scope annotations");
            managedBeanCreator.defineQualifier();
            managedBeanCreator.defineName(WebBeansUtil.getSimpleWebBeanDefaultName(clazz.getSimpleName()));
            managedBeanCreator.defineConstructor();            
            Set<ProducerMethodBean<?>> producerMethods = managedBeanCreator.defineProducerMethods();       
            Set<ProducerFieldBean<?>> producerFields = managedBeanCreator.defineProducerFields();           
            managedBeanCreator.defineInjectedFields();
            managedBeanCreator.defineInjectedMethods();
            managedBeanCreator.defineDecoratorStack();
            managedBeanCreator.defineInterceptorStack();
            
            Set<ObserverMethod<?>> observerMethods = managedBeanCreator.defineObserverMethods();
                                    
            //Fires ProcessInjectionTarget
            ProcessInjectionTargetImpl<T> processInjectionTargetEvent = WebBeansUtil.fireProcessInjectionTargetEvent(managedBean);            
            if(processInjectionTargetEvent.isSet())
            {
                managedBeanCreator.setInjectedTarget(processInjectionTargetEvent.getInjectionTarget());
            }
            
            Map<ProducerMethodBean<?>,AnnotatedMethod<?>> annotatedMethods = new HashMap<ProducerMethodBean<?>, AnnotatedMethod<?>>(); 
            for(ProducerMethodBean<?> producerMethod : producerMethods)
            {
                AnnotatedMethod<?> method = AnnotatedElementFactory.newAnnotatedMethod(producerMethod.getCreatorMethod(), producerMethod.getParent().getReturnType());
                ProcessProducerImpl<?, ?> producerEvent = WebBeansUtil.fireProcessProducerEventForMethod(producerMethod,method);

                annotatedMethods.put(producerMethod, method);
                
                if(producerEvent.isProducerSet())
                {
                    producerMethod.setProducer((Producer) managedBeanCreator.getProducer());
                }
                
                producerEvent.setProducerSet(false);
            }
            
            Map<ProducerFieldBean<?>,AnnotatedField<?>> annotatedFields = new HashMap<ProducerFieldBean<?>, AnnotatedField<?>>();
            for(ProducerFieldBean<?> producerField : producerFields)
            {
                AnnotatedField<?> field = AnnotatedElementFactory.newAnnotatedField(producerField.getCreatorField(), producerField.getParent().getReturnType());
                ProcessProducerImpl<?, ?> producerEvent = WebBeansUtil.fireProcessProducerEventForField(producerField, field);
                
                annotatedFields.put(producerField, field);
                
                if(producerEvent.isProducerSet())
                {
                    producerField.setProducer((Producer) managedBeanCreator.getProducer());
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
            
            //Fires ProcessManagedBean
            ProcessBeanImpl<T> processBeanEvent = new GProcessManagedBean(managedBean,annotatedType);            
            BeanManagerImpl.getManager().fireEvent(processBeanEvent, new Annotation[0]);
            
            //Fires ProcessProducerMethod
            WebBeansUtil.fireProcessProducerMethodBeanEvent(annotatedMethods);
            
            //Fires ProcessProducerField
            WebBeansUtil.fireProcessProducerFieldBeanEvent(annotatedFields);
            
            //Fire ObservableMethods
            WebBeansUtil.fireProcessObservableMethodBeanEvent(observerMethodsMap);
            
            //Set InjectionTarget that is used by the container to inject dependencies!
            if(managedBeanCreator.isInjectionTargetSet())
            {
                managedBean.setInjectionTarget(managedBeanCreator.getInjectedTarget());   
            }
            
            boolean useAlternative = OpenWebBeansConfiguration.getInstance().useAlternativeOrDeploymentType();
            boolean addBeans = true;
            if(!useAlternative)
            {
                // Check if the deployment type is enabled.
                if (!WebBeansUtil.isDeploymentTypeEnabled(deploymentType))
                {
                    addBeans = false;
                }
                
            }
            
            if (addBeans)
            {                
                BeanManagerImpl.getManager().addBean(WebBeansUtil.createNewBean(managedBean));                
                DecoratorUtil.checkManagedBeanDecoratorConditions(managedBean);
                BeanManagerImpl.getManager().addBean(managedBean);
                BeanManagerImpl.getManager().getBeans().addAll(producerMethods);
                managedBeanCreator.defineDisposalMethods();//Define disposal method after adding producers
                BeanManagerImpl.getManager().getBeans().addAll(producerFields);
            }
        }
    }

    /**
     * Defines the new interceptor with given class.
     * 
     * @param clazz interceptor class
     */
    protected <T> void defineInterceptor(Class<T> clazz)
    {
        WebBeansUtil.defineInterceptors(clazz);
    }

    protected <T> void defineDecorator(Class<T> clazz)
    {
        WebBeansUtil.defineDecorators(clazz);
    }

    /**
     * Defines enterprise bean via plugin.
     * @param <T> bean class type
     * @param clazz bean class
     */
    protected <T> void defineEnterpriseWebBean(Class<T> clazz)
    {
        EJBWebBeansConfigurator.defineEjbBean(clazz);
    }
}