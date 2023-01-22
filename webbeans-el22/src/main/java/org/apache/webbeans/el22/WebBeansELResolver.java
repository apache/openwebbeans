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
package org.apache.webbeans.el22;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.el.ELContextStore;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import java.beans.FeatureDescriptor;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;

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
 * @version $Rev: 1307826 $ $Date: 2012-03-31 18:24:37 +0300 (Sat, 31 Mar 2012) $
 *
 */
public class WebBeansELResolver extends ELResolver
{
    private WebBeansContext webBeansContext;

    public WebBeansELResolver()
    {
        webBeansContext = WebBeansContext.getInstance();
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
    public Class<?> getType(ELContext arg0, Object arg1, Object arg2) throws ELException
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    @SuppressWarnings({"unchecked","deprecation"})
    public Object getValue(ELContext context, Object base, Object property) throws ELException
    {
        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();
        //we only check root beans
        // Check if the OWB actually got used in this application
        if (base != null || !beanManager.isInUse())
        {
            return null;
        }

        //Name of the bean
        String beanName = (String) property;

        //Local store, create if not exist
        ELContextStore elContextStore = ELContextStore.getInstance(true);

        Object contextualInstance = elContextStore.findBeanByName(beanName);

        if(contextualInstance != null)
        {
            context.setPropertyResolved(true);

            return contextualInstance;
        }

        //Get beans
        Set<Bean<?>> beans = beanManager.getBeans(beanName);

        //Found?
        if(beans != null && !beans.isEmpty())
        {
            //Managed bean
            Bean<?> bean = beanManager.resolve(beans);

            if(bean.getScope().equals(Dependent.class))
            {
                contextualInstance = getDependentContextualInstance(beanManager, elContextStore, context, bean);
            }
            else
            {
                // now we check for NormalScoped beans
                contextualInstance = getNormalScopedContextualInstance(beanManager, elContextStore, context, bean, beanName);
            }
        }
        return contextualInstance;
    }

    protected Object getNormalScopedContextualInstance(BeanManagerImpl manager, ELContextStore store, ELContext context, Bean<?> bean, String beanName)
    {
        CreationalContext<?> creationalContext = manager.createCreationalContext(bean);
        Object contextualInstance = manager.getReference(bean, Object.class, creationalContext);
        if (contextualInstance != null)
        {
            context.setPropertyResolved(true);
            //Adding into store
            store.addNormalScoped(beanName, contextualInstance);
        }

        return contextualInstance;
    }


    protected Object getDependentContextualInstance(BeanManagerImpl manager, ELContextStore store, ELContext context, Bean<?> bean)
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
            CreationalContext<?> creationalContext = manager.createCreationalContext(bean);
            contextualInstance = manager.getReference(bean, bestType(bean), creationalContext);
            if (contextualInstance != null)
            {
                context.setPropertyResolved(true);
                //Adding into store
                store.addDependent(bean, contextualInstance, creationalContext);
            }
        }
        return contextualInstance;
    }

    private static Type bestType(Bean<?> bean)
    {
        if (bean == null)
        {
            return Object.class;
        }
        Class<?> bc = bean.getBeanClass();
        if (bc != null)
        {
            return bc;
        }
        if (OwbBean.class.isInstance(bean))
        {
            return OwbBean.class.cast(bean).getReturnType();
        }
        return Object.class;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public boolean isReadOnly(ELContext arg0, Object arg1, Object arg2) throws ELException
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public void setValue(ELContext arg0, Object arg1, Object arg2, Object arg3) throws ELException
    {

    }
}
