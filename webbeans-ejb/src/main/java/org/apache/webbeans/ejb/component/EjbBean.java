/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.ejb.component;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.SessionBeanType;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.InterfaceType;
import org.apache.openejb.assembler.classic.JndiBuilder;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.WebBeansType;

/**
 * @version $Rev$ $Date$
 */
public class EjbBean<T> extends AbstractInjectionTargetBean<T> implements EnterpriseBeanMarker
{
    private SessionBeanType ejbType;
    
    private DeploymentInfo deploymentInfo;    
    
    private T instance = null;
    
    public EjbBean(Class<T> ejbClassType)
    {
        super(WebBeansType.ENTERPRISE,ejbClassType);
    }

    public void setDeploymentInfo(DeploymentInfo deploymentInfo)
    {
        this.deploymentInfo = deploymentInfo;
    }
        
    public DeploymentInfo getDeploymentInfo()
    {
        return this.deploymentInfo;
    }
    
        
    @Override
    public void injectFields(T instance, CreationalContext<T> creationalContext)
    {
        //No-operations
    }
    
    @SuppressWarnings("unchecked")
    public void injectFieldInInterceptor(Object instance, CreationalContext<?> creationalContext)
    {
        super.injectFields((T)instance, (CreationalContext<T>)creationalContext);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T createComponentInstance(CreationalContext<T> creationalContext)
    {
        if(this.instance == null)
        {
            ContainerSystem containerSystem =  SystemInstance.get().getComponent(ContainerSystem.class);
            Context jndiContext = containerSystem.getJNDIContext();
            DeploymentInfo deploymentInfo = this.getDeploymentInfo();
            try
            {
                Class<T> intf = deploymentInfo.getInterface(InterfaceType.BUSINESS_LOCAL);
                String jndiName = "java:openejb/Deployment/" + JndiBuilder.format(deploymentInfo.getDeploymentID(), intf.getName()); 
                this.instance = intf.cast(jndiContext.lookup(jndiName));                             
                
            }catch(NamingException e)
            {
                throw new RuntimeException(e);
            }        
        }

        return instance;
    }

    @Override
    protected void destroyComponentInstance(T instance)
    {
        this.instance = null;
    }
    
    public void setEjbType(SessionBeanType type)
    {
        this.ejbType = type;
        
    }
    
    public String getEjbName()
    {
        return this.deploymentInfo.getEjbName();
    }
    
    public SessionBeanType getEjbType()
    {
        return this.ejbType;
    }

}