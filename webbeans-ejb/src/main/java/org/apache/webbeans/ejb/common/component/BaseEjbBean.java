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
package org.apache.webbeans.ejb.common.component;

import java.lang.reflect.Method;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.SessionBeanType;

import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.util.ClassUtil;

/**
 * Defines bean contract for the session beans.
 * 
 * @version $Rev$ $Date$
 */
public abstract class BaseEjbBean<T> extends AbstractInjectionTargetBean<T> implements EnterpriseBeanMarker
{
    /**Session bean type*/
    protected SessionBeanType ejbType;
    
    /**Injected reference local interface type*/
    protected Class<?> iface = null;
    
    /**Remove stateful bean instance*/
    protected boolean removeStatefulInstance = false;
    
    /**
     * Creates a new instance of the session bean.
     * @param ejbClassType ebj class type
     */
    public BaseEjbBean(Class<T> ejbClassType)
    {
        super(WebBeansType.ENTERPRISE,ejbClassType);

        //Setting inherited meta data instance
        setInheritedMetaData();
    }

    /**
     * Sets local interface type.
     * @param iface local interface type
     */
    public void setIface(Class<?> iface)
    {
        this.iface = iface;
    }
    
    public Class<?> getIface()
    {
        return this.iface;
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
        if(this.ejbType.equals(SessionBeanType.STATEFUL))
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
    protected T createComponentInstance(CreationalContext<T> creationalContext)
    {
        return getInstance(creationalContext);
    }
    
    /**
     * Sublclasses must return instance.
     * @param creationalContext creational context
     * @return instance
     */
    protected abstract T getInstance(CreationalContext<T> creationalContext);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroyComponentInstance(T instance, CreationalContext<T> creational)
    {
        if(!removeStatefulInstance && getEjbType().equals(SessionBeanType.STATEFUL))
        {
            //Call remove method
            List<Method> methods = getRemoveMethods();
            for(Method method : methods)
            {
                ClassUtil.callInstanceMethod(method, instance, ClassUtil.OBJECT_EMPTY);
            }
        }        
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
     * Subclasses can override this.
     * @return remove methods
     */
    public List<Method> getRemoveMethods()
    {
        return null;
    }
    
    /**
     * Subclasses must override this to return local interfaces.
     * @return local business interfaces.
     */
    public List<Class<?>> getBusinessLocalInterfaces()
    {
        return null;
    }
    
    /**
     * Subclasses must override this to return ejb name
     * @return ejb name
     */    
    public String getEjbName()
    {
        return null;
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