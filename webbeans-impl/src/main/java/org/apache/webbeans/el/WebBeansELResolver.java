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
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
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

    public static ThreadLocal<ELContextStore> LOCAL_CONTEXT = new ThreadLocal<ELContextStore>();
    
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
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        Object object = null;
        Bean<Object> bean = null;
        CreationalContext<Object> creationalContext = null;        
        ELContextStore store = null;
        boolean canBe = false;
        
        if (obj == null)
        {
            if((store = LOCAL_CONTEXT.get()) != null)
            {
                ELContext oldContext = store.getELContext();
                if(!oldContext.equals(context))
                {
                    store.destroy();
                    LOCAL_CONTEXT.remove();
                }
                else
                {
                    canBe = true;
                }
            }
                        
            String name = (String) property;            
            Set<Bean<?>> beans = manager.getBeans(name);
            
            if(beans != null && !beans.isEmpty())
            {
                bean = (Bean<Object>)beans.iterator().next();
                creationalContext = manager.createCreationalContext(bean);                    
                
                if(bean.getScope().equals(Dependent.class))
                {
                    if(canBe)
                    {
                       object = store.getDependent(bean);
                    }
                }                    
            }
            
            
            if(object == null)
            {
                object = manager.getInstanceByName(name,creationalContext);
                
                if (object != null)
                {                    
                    context.setPropertyResolved(true);
                    
                    store = new ELContextStore(context);
                    store.addDependent(bean, object, creationalContext);
                    LOCAL_CONTEXT.set(store);
                }                    
            }
            else
            {
                context.setPropertyResolved(true);                    
            }
            
        }

        return object;
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