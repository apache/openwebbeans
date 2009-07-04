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
package org.apache.webbeans.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.servlet.jsp.JspApplicationContext;

import org.apache.webbeans.container.BeanManagerImpl;

/**
 * JSF or JSP expression language a.k.a EL resolver.
 * 
 * <p>
 * EL is registered with the JSF in faces-config.xml if there exist a faces-config.xml
 * in the application location <code>WEB-INF/</code>. Otherwise it is registered with
 * {@link JspApplicationContext} at start-up. 
 * </p>
 * 
 * <p>
 * All <code>@Dependent</code> scoped contextual instances created during an EL 
 * expression evaluation are destroyed when the evaluation completes.
 * </p>
 * 
 * @version $Rev$ $Date$
 *
 */
public class WebBeansELResolver extends ELResolver
{    

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getCommonPropertyType(ELContext arg0, Object arg1)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext arg0, Object arg1)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public Class<?> getType(ELContext arg0, Object arg1, Object arg2) throws NullPointerException, PropertyNotFoundException, ELException
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public Object getValue(ELContext context, Object obj, Object property) throws NullPointerException, PropertyNotFoundException, ELException
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();

        Object object = null;
        Bean<?> bean = null;

        boolean isResolution = false;
        try
        {
            if (obj == null)
            {
                String name = (String) property;
                object = manager.getInstanceByName(name);
                
                if (object != null)
                {
                    isResolution = true;
                    context.setPropertyResolved(true);
                    //It is used for destroying
                    bean = manager.resolveByName(name).iterator().next();
                }

            }

        }
        finally
        {
            if (isResolution)
            {
                if (bean != null)
                {
                    destroyBean(bean, object);
                }                
            }
        }

        return object;
    }

    /**
     * Destroys the bean.
     * 
     * @param <T> bean type info
     * @param bean dependent context scoped bean
     * @param instance bean instance
     */
    @SuppressWarnings("unchecked")
    private <T> void destroyBean(Bean<T> bean, Object instance)
    {
        if (bean.getScopeType().equals(Dependent.class))
        {
            T inst = (T) instance;

            //TODO Creational Context
            bean.destroy(inst,null);
        }
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public boolean isReadOnly(ELContext arg0, Object arg1, Object arg2) throws NullPointerException, PropertyNotFoundException, ELException
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public void setValue(ELContext arg0, Object arg1, Object arg2, Object arg3) throws NullPointerException, PropertyNotFoundException, PropertyNotWritableException, ELException
    {

    }

}