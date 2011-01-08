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
package org.apache.webbeans.ejb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.SessionBeanType;

import org.apache.openejb.Container;
import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.assembler.classic.DeploymentListener;
import org.apache.openejb.assembler.classic.EjbJarInfo;
import org.apache.openejb.assembler.classic.EnterpriseBeanInfo;
import org.apache.openejb.assembler.classic.JndiBuilder;
import org.apache.openejb.assembler.classic.JndiBuilder.JndiNameStrategy;
import org.apache.openejb.core.CoreContainerSystem;
import org.apache.openejb.core.CoreDeploymentInfo;
import org.apache.openejb.core.singleton.SingletonContainer;
import org.apache.openejb.core.stateful.StatefulContainer;
import org.apache.openejb.core.stateless.StatelessContainer;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.webbeans.ejb.common.util.EjbDefinitionUtility;
import org.apache.webbeans.ejb.common.util.EjbUtility;
import org.apache.webbeans.ejb.component.OpenEjbBean;
import org.apache.webbeans.ejb.resource.EJBInstanceProxy;
import org.apache.webbeans.ejb.service.OpenEJBSecurityService;
import org.apache.webbeans.ejb.service.OpenEJBTransactionService;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.SecurityService;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.spi.plugins.AbstractOwbPlugin;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.util.SecurityUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * EJB related stuff.
 * <p>
 * EJB functionality depends on OpenEJB.
 * </p>
 * 
 * @version $Rev$ $Date$
 */
public class EjbPlugin extends AbstractOwbPlugin implements OpenWebBeansEjbPlugin, DeploymentListener
{
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(EjbPlugin.class);

    /**OpenEJB container system*/
    private ContainerSystem containerSystem = null;

    /***OpenEJB assembler */
    private Assembler assembler;
    
    /**Deployed applications*/
    private final Set<AppInfo> deployedApplications = new CopyOnWriteArraySet<AppInfo>();

    /**Stateless Beans*/
    private Map<Class<?>, DeploymentInfo> statelessBeans = new ConcurrentHashMap<Class<?>, DeploymentInfo>();
    
    /**Stateful Beans*/
    private Map<Class<?>, DeploymentInfo> statefulBeans = new ConcurrentHashMap<Class<?>, DeploymentInfo>();

    /**Singleton Beans*/
    private Map<Class<?>, DeploymentInfo> singletonBeans = new ConcurrentHashMap<Class<?>, DeploymentInfo>();
    
    /**EJB interface class name to proxy instance*/
    private Map<String, EJBInstanceProxy<?>> ejbInstances = new ConcurrentHashMap<String, EJBInstanceProxy<?>>();

    /**Transaction service*/
    private static final TransactionService TRANSACTION_SERVICE = new OpenEJBTransactionService();
    
    /**Security service*/
    private static final SecurityService SECURITY_SERVICE = new OpenEJBSecurityService();
    
    /**JNDI Name strategy*/
    private final Map<String, JndiNameStrategy> nameStrategies = new TreeMap<String, JndiNameStrategy>();

    // This is here for standalone tests are correctly run
    // Not used in anywhere
    private boolean useInTest = false;

    public EjbPlugin()
    {

    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.plugins.AbstractOwbPlugin#shutDown()
     */
    @Override
    public void shutDown() throws WebBeansConfigurationException
    {
        try
        {
            super.shutDown();
            this.deployedApplications.clear();
            this.statelessBeans.clear();
            this.statefulBeans.clear();
            this.singletonBeans.clear();
            this.containerSystem = null;
            this.ejbInstances.clear();
            this.nameStrategies.clear();
            this.assembler.removeDeploymentListener(this);
        }
        catch (Exception e)
        {
            throw new WebBeansConfigurationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getEjbInstance(String intfName, Class<T> intf) throws Exception
    {
        EJBInstanceProxy<T> proxy = (EJBInstanceProxy<T>) this.ejbInstances.get(intfName);
        if (proxy != null)
        {
            return intf.cast(proxy.getObject());
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.plugins.AbstractOwbPlugin#startUp()
     */
    @Override
    public void startUp() throws WebBeansConfigurationException
    {
        try
        {
            super.startUp();
        }
        catch (Exception e)
        {
            throw new WebBeansConfigurationException(e);
        }

        // Get container and assembler from OpenEJB
        containerSystem = (CoreContainerSystem) SystemInstance.get().getComponent(ContainerSystem.class);
        assembler = SystemInstance.get().getComponent(Assembler.class);

        // We register ourselves as a listener to the deployment
        if (assembler != null)
        {
            assembler.addDeploymentListener(this);
            for (AppInfo appInfo : assembler.getDeployedApplications())
            {
                afterApplicationCreated(appInfo);
            }
        }
    }

    // Used for tests
    public void setUseInTest(boolean useInTest)
    {
        this.useInTest = useInTest;
    }

    /**
     * OpenEJB call back method It is used to get the list of deployed
     * application and store the Stateless and Stateful pools localy.
     * 
     * @param appInfo applications informations
     */
    public void afterApplicationCreated(AppInfo appInfo)
    {
        logger.debug("Retrieving deployed EJB modules");
        if (deployedApplications.add(appInfo))
        {
            List<DeploymentInfo> statelessList = new ArrayList<DeploymentInfo>();
            List<DeploymentInfo> statefulList = new ArrayList<DeploymentInfo>();
            List<DeploymentInfo> singletonList = new ArrayList<DeploymentInfo>();

            for (EjbJarInfo ejbJar : appInfo.ejbJars)
            {
                for (EnterpriseBeanInfo bean : ejbJar.enterpriseBeans)
                {
                    switch (bean.type)
                    {
                    case EnterpriseBeanInfo.STATELESS:
                        statelessList.add(containerSystem.getDeploymentInfo(bean.ejbDeploymentId));
                        break;
                    case EnterpriseBeanInfo.STATEFUL:
                        statefulList.add(containerSystem.getDeploymentInfo(bean.ejbDeploymentId));
                        break;
                    case EnterpriseBeanInfo.SINGLETON:
                        singletonList.add(containerSystem.getDeploymentInfo(bean.ejbDeploymentId));
                        break;
                    default:
                        break;
                    }
                }
            }

            // Means that this is not the our deployment archive
            boolean result = addBeanDeploymentInfos(statelessList.toArray(new DeploymentInfo[statelessList.size()]), SessionBeanType.STATELESS);
            if (!result)
            {
                deployedApplications.remove(appInfo);
                return;
            }

            result = addBeanDeploymentInfos(statefulList.toArray(new DeploymentInfo[statefulList.size()]), SessionBeanType.STATEFUL);
            if (!result)
            {
                deployedApplications.remove(appInfo);
                return;
            }
            
            
            result = addBeanDeploymentInfos(singletonList.toArray(new DeploymentInfo[singletonList.size()]), SessionBeanType.SINGLETON);
            if (!result)
            {
                deployedApplications.remove(appInfo);
                return;
            }            
        }
    }

    /**
     * OpenEJB callback method Not used.
     */
    public void beforeApplicationDestroyed(AppInfo appInfo)
    {
        if (this.deployedApplications.contains(appInfo))
        {
            this.deployedApplications.remove(appInfo);
            for (EjbJarInfo ejbJar : appInfo.ejbJars)
            {
                for (EnterpriseBeanInfo bean : ejbJar.enterpriseBeans)
                {
                    switch (bean.type)
                    {
                    case EnterpriseBeanInfo.STATELESS:
                        this.statelessBeans.remove(containerSystem.getDeploymentInfo(bean.ejbDeploymentId).getBeanClass());
                        break;
                    case EnterpriseBeanInfo.STATEFUL:
                        this.statefulBeans.remove(containerSystem.getDeploymentInfo(bean.ejbDeploymentId).getBeanClass());
                        break;
                    case EnterpriseBeanInfo.SINGLETON:
                        this.singletonBeans.remove(containerSystem.getDeploymentInfo(bean.ejbDeploymentId).getBeanClass());
                        break;
                    default:
                        break;
                    }

                    this.ejbInstances.remove(bean.ejbName);
                }
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public <T> Bean<T> defineSessionBean(Class<T> clazz, ProcessAnnotatedType<T> processAnnotatedTypeEvent)
    {
        if (!isSessionBean(clazz))
        {
            throw new IllegalArgumentException("Given class is not an session bean class");
        }

        DeploymentInfo info = null;
        SessionBeanType type = SessionBeanType.STATELESS;

        if (isStatelessBean(clazz))
        {
            info = this.statelessBeans.get(clazz);
        }
        else if (isStatefulBean(clazz))
        {
            info = this.statefulBeans.get(clazz);
            type = SessionBeanType.STATEFUL;
        }
        else if (isSingletonBean(clazz))
        {
            info = this.singletonBeans.get(clazz);
            type = SessionBeanType.SINGLETON;
        }
        else
        {
            throw new IllegalArgumentException("Illegal EJB type with class : " + clazz.getName());
        }

        OpenEjbBean<T> bean = new OpenEjbBean<T>(clazz, type);
        bean.setDeploymentInfo(info);

        EjbUtility.fireEvents(clazz, bean, processAnnotatedTypeEvent);

        return bean;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isSessionBean(Class<?> clazz)
    {
        // This is used in tests, because in reality containerSystem is not
        // null
        if (this.containerSystem == null || useInTest)
        {
            // Used for tests
            useInTest = false;

            this.containerSystem = SystemInstance.get().getComponent(ContainerSystem.class);
            Container[] containers = this.containerSystem.containers();
            for (Container container : containers)
            {
                DeploymentInfo[] deployments = container.deployments();
                if (container instanceof StatelessContainer)
                {
                    addBeanDeploymentInfos(deployments, SessionBeanType.STATELESS);
                }
                else if (container instanceof StatefulContainer)
                {
                    addBeanDeploymentInfos(deployments, SessionBeanType.STATEFUL);
                }
                else if (container instanceof SingletonContainer)
                {
                    addBeanDeploymentInfos(deployments, SessionBeanType.SINGLETON);
                }
            }
        }

        return isSingletonBean(clazz) || isStatelessBean(clazz) || isStatefulBean(clazz);
    }
    
    /**
     * Checks deployment.
     * @param deployments ejb deployments
     * @param type session bean type
     * @return true if this deployment ok
     */
    private boolean addBeanDeploymentInfos(DeploymentInfo[] deployments, SessionBeanType type)
    {
        boolean classLoaderEquality = false;
        
        if(deployments.length == 0)
        {
            return true;
        }
        
        for (DeploymentInfo deployment : deployments)
        {
            boolean inTest = Boolean.valueOf(SecurityUtil.doPrivilegedGetSystemProperty("EjbPlugin.test", "false"));
            classLoaderEquality = deployment.getBeanClass().getClassLoader().equals(WebBeansUtil.getCurrentClassLoader());

            // Yes, this EJB archive is deployed within this web
            // application
            if (inTest || classLoaderEquality)
            {
                if (type.equals(SessionBeanType.STATELESS))
                {
                    this.statelessBeans.put(deployment.getBeanClass(), deployment);
                }
                else if (type.equals(SessionBeanType.STATEFUL))
                {
                    this.statefulBeans.put(deployment.getBeanClass(), deployment);
                }
                else if (type.equals(SessionBeanType.SINGLETON))
                {
                    this.singletonBeans.put(deployment.getBeanClass(), deployment);
                }

                Map<String, EJBInstanceProxy<?>> bindings = getEjbBindings((CoreDeploymentInfo) deployment);
                for (Entry<String, EJBInstanceProxy<?>> entry : bindings.entrySet())
                {
                    String beanName = entry.getKey();
                    if (!this.ejbInstances.containsKey(beanName))
                    {
                        EJBInstanceProxy<?> ejb = entry.getValue();
                        this.ejbInstances.put(beanName, ejb);
                        logger.info("Exported EJB " + deployment.getEjbName() + " with interface " + entry.getValue().getInterface().getName());
                    }
                }
            }
        }

        return classLoaderEquality;
    }
    
    /**
     * {@inheritDoc}
     */
    public void isManagedBean(Class<?> clazz) throws WebBeansConfigurationException
    {
        if (isSessionBean(clazz))
        {
            throw new WebBeansConfigurationException("Managed Bean implementation class : " + clazz.getName() + " can not be sesion bean class");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSingletonBean(Class<?> clazz)
    {
        return this.singletonBeans.containsKey(clazz);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatefulBean(Class<?> clazz)
    {
        return this.statefulBeans.containsKey(clazz);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatelessBean(Class<?> clazz)
    {
        return this.statelessBeans.containsKey(clazz);
    }

    /**
     * {@inheritDoc}
     */
    public Object getSessionBeanProxy(Bean<?> bean, Class<?> iface, CreationalContext<?> creationalContext)
    {
        return EjbDefinitionUtility.defineEjbBeanProxy((OpenEjbBean<?>) bean, iface, creationalContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getSupportedService(Class<T> serviceClass)
    {
        if (serviceClass == TransactionService.class)
        {
            return serviceClass.cast(TRANSACTION_SERVICE);
        }
        else if (serviceClass == SecurityService.class)
        {
            return serviceClass.cast(SECURITY_SERVICE);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportService(Class<?> serviceClass)
    {
        if ((serviceClass == TransactionService.class) || serviceClass == SecurityService.class)
        {
            return true;
        }

        return false;
    }

    /**
     * Creates strategy for jndi naming.
     * @param appInfo app info
     * @param deployments deployments
     * @param deployment deployment
     * @return strategy
     * @throws OpenEJBException if any exception
     */
    public JndiNameStrategy createStrategy(AppInfo appInfo, List<DeploymentInfo> deployments, DeploymentInfo deployment) throws OpenEJBException
    {
        JndiNameStrategy strategy = nameStrategies.get(deployment.getModuleID());
        if (strategy != null)
        {
            return strategy;
        }

        String deploymentId = (String) deployment.getDeploymentID();
        for (EjbJarInfo ejbJar : appInfo.ejbJars)
        {
            if (ejbJar.moduleId.equals(deployment.getModuleID()))
            {
                Set<String> moduleDeploymentIds = new TreeSet<String>();
                for (EnterpriseBeanInfo enterpriseBean : ejbJar.enterpriseBeans)
                {
                    moduleDeploymentIds.add(enterpriseBean.ejbDeploymentId);
                }
                Map<String, DeploymentInfo> moduleDeployments = new TreeMap<String, DeploymentInfo>();
                for (DeploymentInfo deploymentInfo : deployments)
                {
                    if (moduleDeploymentIds.contains(deploymentId))
                    {
                        moduleDeployments.put((String) deploymentInfo.getDeploymentID(), deploymentInfo);
                    }
                }
                strategy = JndiBuilder.createStrategy(ejbJar, moduleDeployments);
                for (String moduleDeploymentId : moduleDeploymentIds)
                {
                    nameStrategies.put(moduleDeploymentId, strategy);
                }
                return strategy;
            }
        }

        return null;
    }
    
    /**
     * Gets map of intfc --> ejb proxy
     * @param deployment deployment info
     * @return map of ejb proxy instance with interface
     */
    @SuppressWarnings("unchecked")
    public Map<String, EJBInstanceProxy<?>> getEjbBindings(CoreDeploymentInfo deployment)
    {
        Map<String, EJBInstanceProxy<?>> bindings = new TreeMap<String, EJBInstanceProxy<?>>();

        Class<?> remoteHome = deployment.getHomeInterface();
        if (remoteHome != null)
        {
            bindings.put(remoteHome.getName(), new EJBInstanceProxy(deployment, remoteHome));
        }

        Class<?> localHome = deployment.getLocalHomeInterface();
        if (localHome != null)
        {
            bindings.put(localHome.getName(), new EJBInstanceProxy(deployment, remoteHome));
        }

        for (Class<?> businessLocal : deployment.getBusinessLocalInterfaces())
        {
            bindings.put(businessLocal.getName(), new EJBInstanceProxy(deployment, businessLocal));
        }

        for (Class<?> businessRemote : deployment.getBusinessRemoteInterfaces())
        {
            bindings.put(businessRemote.getName(), new EJBInstanceProxy(deployment, businessRemote));
        }

        return bindings;
    }

}
