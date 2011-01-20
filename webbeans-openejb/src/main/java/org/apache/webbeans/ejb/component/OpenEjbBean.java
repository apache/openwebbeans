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
package org.apache.webbeans.ejb.component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.SessionBeanType;

import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.InterfaceType;
import org.apache.openejb.DeploymentInfo.BusinessLocalHome;
import org.apache.openejb.core.CoreDeploymentInfo;
import org.apache.openejb.core.ivm.EjbHomeProxyHandler;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.ejb.common.component.BaseEjbBean;

/**
 * Defines bean contract for the session beans.
 * 
 * @version $Rev$ $Date$
 */
public class OpenEjbBean<T> extends BaseEjbBean<T>
{
    /**OpenEJB deployment info*/
    private DeploymentInfo deploymentInfo;    
    
    /**
     * Creates a new instance of the session bean.
     * @param ejbClassType ebj class type
     */
    public OpenEjbBean(Class<T> ejbClassType, SessionBeanType type)
    {
        super(ejbClassType, type, WebBeansContext.getInstance());
    }
    
    /**
     * Sets session bean's deployment info.
     * @param deploymentInfo deployment info
     */
    public void setDeploymentInfo(DeploymentInfo deploymentInfo)
    {
        this.deploymentInfo = deploymentInfo;
    }
        
    /**
     * Returns bean's deployment info.
     * @return bean's deployment info
     */
    public DeploymentInfo getDeploymentInfo()
    {
        return this.deploymentInfo;
    }
            
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected T getInstance(CreationalContext<T> creationalContext)
    {
        List<Class> interfaces = new ArrayList<Class>();
        for(Class clazz : getBusinessLocalInterfaces())
        {
            interfaces.add(clazz);
        }
        
        BusinessLocalHome home = getBusinessLocalHome(interfaces);
        return (T)home.create();
    }

    
    /**
     * Gets ejb name.
     * @return ejb name
     */
    public String getEjbName()
    {
        return this.deploymentInfo.getEjbName();
    }

    /* (non-Javadoc)
     * @see org.apache.webbeans.ejb.common.component.BaseEjbBean#getBusinessLocalInterfaces()
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Class<?>> getBusinessLocalInterfaces()
    {
        List<Class<?>> clazzes = new ArrayList<Class<?>>();        
        List<Class> cl = this.deploymentInfo.getBusinessLocalInterfaces();
        
        if(cl != null && !cl.isEmpty())
        {
            for(Class<?> c : cl)
            {
                clazzes.add(c);
            }
        }
        
        return clazzes;
    }

    @Override
    public List<Method> getRemoveMethods()
    {
        return this.deploymentInfo.getRemoveMethods();
    }
        
    @SuppressWarnings("unchecked")
    private BusinessLocalHome getBusinessLocalHome(List<Class> interfaces)
    {
        if (getBusinessLocalInterfaces().size() == 0)
        {
            throw new IllegalStateException("This component has no business local interfaces: " 
                    + ((CoreDeploymentInfo)this.deploymentInfo).getDeploymentID());
        }
        
        if (interfaces.size() == 0)
        {
            throw new IllegalArgumentException("No interface classes were specified");
        }
       
        for (Class<?> clazz : interfaces)
        {
            if (!getBusinessLocalInterfaces().contains(clazz))
            {
                throw new IllegalArgumentException("Not a business interface of this bean:" + clazz.getName());
            }
        }

        return (BusinessLocalHome) EjbHomeProxyHandler.createHomeProxy(this.deploymentInfo, InterfaceType.BUSINESS_LOCAL_HOME, interfaces);
    }
    
}