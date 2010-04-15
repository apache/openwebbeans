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
package org.apache.webbeans.ejb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.SessionBeanType;

import org.apache.openejb.Container;
import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.core.singleton.SingletonContainer;
import org.apache.openejb.core.stateful.StatefulContainer;
import org.apache.openejb.core.stateless.StatelessContainer;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.webbeans.ejb.common.util.EjbDefinitionUtility;
import org.apache.webbeans.ejb.common.util.EjbUtility;
import org.apache.webbeans.ejb.component.OpenEjbBean;
import org.apache.webbeans.ejb.service.OpenEJBSecurityService;
import org.apache.webbeans.ejb.service.OpenEJBTransactionService;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.plugins.AbstractOwbPlugin;
import org.apache.webbeans.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.spi.SecurityService;
import org.apache.webbeans.spi.TransactionService;

/**
 * EJB related stuff.
 * <p>
 * EJB functionality depends on OpenEJB.
 * </p>
 * @version $Rev$ $Date$
 *
 */
public class EjbPlugin extends AbstractOwbPlugin implements OpenWebBeansEjbPlugin
{
    private ContainerSystem containerSystem = null;
    
    private Map<Class<?>,DeploymentInfo> statelessBeans = new ConcurrentHashMap<Class<?>, DeploymentInfo>();
    
    private Map<Class<?>,DeploymentInfo> statefullBeans = new ConcurrentHashMap<Class<?>, DeploymentInfo>();
    
    private Map<Class<?>,DeploymentInfo> singletonBeans = new ConcurrentHashMap<Class<?>, DeploymentInfo>();
    
    private static final TransactionService TRANSACTION_SERVICE = new OpenEJBTransactionService();
    
    private static final SecurityService SECURITY_SERVICE = new OpenEJBSecurityService();

    public EjbPlugin()
    {
        
    }
        
    @Override
    public <T> Bean<T> defineSessionBean(Class<T> clazz)
    {
        if(!isSessionBean(clazz))
        {
            throw new IllegalArgumentException("Given class is not an session bean class");
        }
        
        DeploymentInfo info = null;
        SessionBeanType type = SessionBeanType.STATELESS;
        
        if(isStatelessBean(clazz))
        {
            info = this.statelessBeans.get(clazz);
        }
        else if(isStatefulBean(clazz))
        {
            info = this.statefullBeans.get(clazz);
            type = SessionBeanType.STATEFUL;
        }
        else if(isSingletonBean(clazz))
        {
            info = this.singletonBeans.get(clazz);
            type = SessionBeanType.SINGLETON;
        }
        else
        {
            throw new IllegalArgumentException("Illegal EJB type with class : " + clazz.getName());
        }
        
        OpenEjbBean<T> bean = new OpenEjbBean<T>(clazz);
        bean.setDeploymentInfo(info);
        bean.setEjbType(type);
        
        EjbUtility.fireEvents(clazz, bean);
        
        return bean;
    }

    @Override
    public boolean isSessionBean(Class<?> clazz)
    {
        if(this.containerSystem == null)
        {
            this.containerSystem = SystemInstance.get().getComponent(ContainerSystem.class);
            Container[] containers = this.containerSystem.containers();
            for(Container container : containers)
            {
                DeploymentInfo[] deployments = container.deployments();
                if(container instanceof StatelessContainer)
                {
                     addBeanDeploymentInfos(deployments, SessionBeanType.STATELESS);
                }
                else if(container instanceof StatefulContainer)
                {
                     addBeanDeploymentInfos(deployments, SessionBeanType.STATEFUL);
                }
                else if(container instanceof SingletonContainer)
                {
                     addBeanDeploymentInfos(deployments, SessionBeanType.SINGLETON);
                }                
            }
        }
                
        
        return isSingletonBean(clazz) || isStatelessBean(clazz) || isStatefulBean(clazz);
    }
    
    private void addBeanDeploymentInfos(DeploymentInfo[] deployments, SessionBeanType type)
    {
        for(DeploymentInfo deployment : deployments)
        {
            if(type.equals(SessionBeanType.STATELESS))
            {
                this.statelessBeans.put(deployment.getBeanClass(),deployment);   
            }
            else if(type.equals(SessionBeanType.STATEFUL))
            {
                this.statefullBeans.put(deployment.getBeanClass(),deployment);
            }
            else if(type.equals(SessionBeanType.SINGLETON))
            {
                this.singletonBeans.put(deployment.getBeanClass(), deployment);
            }
        }
    }
    
    public void isManagedBean(Class<?> clazz) throws WebBeansConfigurationException
    {
        if(isSessionBean(clazz))
        {
            throw new WebBeansConfigurationException("Managed Bean implementation class : " + clazz.getName() + " can not be sesion bean class");            
        }
    }

    @Override
    public boolean isSingletonBean(Class<?> clazz)
    {
        return this.singletonBeans.containsKey(clazz);
    }

    @Override
    public boolean isStatefulBean(Class<?> clazz)
    {
        return this.statefullBeans.containsKey(clazz);
    }

    @Override
    public boolean isStatelessBean(Class<?> clazz)
    {
        return this.statelessBeans.containsKey(clazz);
    }

    @Override
    public Object getSessionBeanProxy(Bean<?> bean, Class<?> iface, CreationalContext<?> creationalContext)
    {
        return EjbDefinitionUtility.defineEjbBeanProxy((OpenEjbBean<?>)bean,iface, creationalContext);
    }
    
    
    @Override    
    public <T> T getSupportedService(Class<T> serviceClass)
    {
        if(serviceClass == TransactionService.class)
        {
            return serviceClass.cast(TRANSACTION_SERVICE);    
        }
        else if(serviceClass == SecurityService.class)
        {
            return serviceClass.cast(SECURITY_SERVICE);
        }
        
        return null;
    }

    @Override
    public boolean supportService(Class<?> serviceClass)
    {
        if((serviceClass == TransactionService.class) ||
                serviceClass == SecurityService.class)
        {
            return true;
        }
        
        return false;
    }
       
}
