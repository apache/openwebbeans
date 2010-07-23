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
package org.apache.webbeans.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.container.BeanManagerImpl;

/**
 * JSF or JSP expression language a.k.a EL resolver.
 * 
 * <p>
 * EL is registered with the JSF in faces-config.xml if there exist a faces-config.xml
 * in the application location <code>WEB-INF/</code>. Otherwise it is registered with
 * JspApplicationContext at start-up. 
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
    
    public WebBeansELResolver()
    {
        
    }
    
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
    @SuppressWarnings("unchecked")
    public Object getValue(ELContext context, Object obj, Object property) throws NullPointerException, PropertyNotFoundException, ELException
    {
        //Bean instance
        Object contextualInstance = null;
        
        //Managed bean
        Bean<Object> bean = null;
        
        //Creational context for creating instance
        CreationalContext<Object> creationalContext = null;
        
        if (obj == null)
        {                      
            //Local store, create if not exist
            ELContextStore store = ELContextStore.getInstance(true);

            //Manager instance
            BeanManagerImpl manager = store.getBeanManager();

            //Name of the bean
            String name = (String) property;
            //Get beans
            Set<Bean<?>> beans = manager.getBeans(name);

            //Found?
            if(beans != null && !beans.isEmpty())
            {
                bean = (Bean<Object>)beans.iterator().next();

                if(bean.getScope().equals(Dependent.class))
                {
                    contextualInstance = getDependentContextualInstance(manager, store, context, bean);
                }
                else
                {
                    // now we check for NormalScoped beans
                    contextualInstance = getNormalScopedContextualInstance(manager, store, context, bean);
                }
            }
        }

        return contextualInstance;
    }

    private Object getNormalScopedContextualInstance(BeanManagerImpl manager, ELContextStore store, ELContext context, Bean<Object> bean)
    {

        Object contextualInstance = store.getNormalScoped(bean);

        if (contextualInstance != null)
        {
            context.setPropertyResolved(true);
        }
        else
        {
            CreationalContext<Object> creationalContext = manager.createCreationalContext(bean);
            contextualInstance = manager.getReference(bean, Object.class, creationalContext);
            if (contextualInstance != null)
            {
                context.setPropertyResolved(true);
                //Adding into store
                store.addNormalScoped(bean, contextualInstance);
            }
        }

        return contextualInstance;
    }


    private Object getDependentContextualInstance(BeanManagerImpl manager, ELContextStore store, ELContext context, Bean<Object> bean)
    {
        Object contextualInstance = store.getDependent(bean);
        if(contextualInstance != null)
        {
            //Object found on the store
            context.setPropertyResolved(true);
        }
        else
        {
            // If no contextualInstance found on the store
            CreationalContext<Object> creationalContext = manager.createCreationalContext(bean);
            contextualInstance = manager.getReference(bean, Object.class, creationalContext);
            if (contextualInstance != null)
            {
                context.setPropertyResolved(true);
                //Adding into store
                store.addDependent(bean, contextualInstance, creationalContext);
            }
        }
        return contextualInstance;
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