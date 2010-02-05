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
 * Defines bean contract for the session beans.
 * 
 * @version $Rev$ $Date$
 */
public class EjbBean<T> extends AbstractInjectionTargetBean<T> implements EnterpriseBeanMarker
{
    /**Session bean type*/
    private SessionBeanType ejbType;
    
    /**OpenEJB deployment info*/
    private DeploymentInfo deploymentInfo;    
    
    /**Current bean instance*/
    private T instance = null;
    
    /**Injected reference local interface type*/
    private Class<?> iface = null;
    
    /**Remove stateful bean instance*/
    private boolean removeStatefulInstance = false;
    
    /**
     * Creates a new instance of the session bean.
     * @param ejbClassType ebj class type
     */
    public EjbBean(Class<T> ejbClassType)
    {
        super(WebBeansType.ENTERPRISE,ejbClassType);
    }

    /**
     * Sets local interface type.
     * @param iface local interface type
     */
    public void setIface(Class<?> iface)
    {
        this.iface = iface;
    }
    
    /**
     * Sets remove flag.
     * @param remove flag
     */
    public void setRemoveStatefulInstance(boolean remove)
    {
        this.removeStatefulInstance = remove;
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
    public void injectFields(T instance, CreationalContext<T> creationalContext)
    {
        //No-operations
    }
    
    
    
    /* (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractBean#isPassivationCapable()
     */
    @Override
    public boolean isPassivationCapable()
    {
        if(this.ejbType.equals(SessionBeanType.STATELESS))
        {
            return true;
        }
        
        return false;
    }

    /**
     * Inject session bean injected fields. It is called from
     * interceptor.
     * @param instance bean instance
     * @param creationalContext creational context instance
     */
    @SuppressWarnings("unchecked")
    public void injectFieldInInterceptor(Object instance, CreationalContext<?> creationalContext)
    {
        super.injectFields((T)instance, (CreationalContext<T>)creationalContext);
    }

    /**
     * {@inheritDoc}
     */
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
                if(iface != null)
                {
                    InterfaceType type = deploymentInfo.getInterfaceType(iface);
                    if(!type.equals(InterfaceType.BUSINESS_LOCAL))
                    {
                        throw new IllegalArgumentException("Interface type is not legal business local interface for session bean class : " + getReturnType().getName());
                    }   
                }    
                else
                {
                    iface = this.deploymentInfo.getBusinessLocalInterface();
                }
                
                String jndiName = "java:openejb/Deployment/" + JndiBuilder.format(deploymentInfo.getDeploymentID(), this.iface.getName()); 
                this.instance = (T)this.iface.cast(jndiContext.lookup(jndiName));                             
                
            }catch(NamingException e)
            {
                throw new RuntimeException(e);
            }        
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroyComponentInstance(T instance)
    {
        if(removeStatefulInstance && getEjbType().equals(SessionBeanType.STATEFUL))
        {
            //Call remove method
        }
        
        this.instance = null;
    }
    
    /**
     * Sets session bean type.
     * @param type session bean type
     */
    public void setEjbType(SessionBeanType type)
    {
        this.ejbType = type;
        
    }
    
    /**
     * Gets ejb name.
     * @return ejb name
     */
    public String getEjbName()
    {
        return this.deploymentInfo.getEjbName();
    }
    
    /**
     * Gets ejb session type.
     * @return type of the ejb
     */
    public SessionBeanType getEjbType()
    {
        return this.ejbType;
    }

}