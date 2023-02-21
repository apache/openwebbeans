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
import java.util.stream.Collectors;

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
    private final WebBeansContext webBeansContext;

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
        final BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();

        // Check if the OWB actually got used in this application
        if (!beanManager.isInUse())
        {
            return null;
        }

        //Name of the bean
        final String beanName = (String) property;

        // Local store, create if not exist
        final ELContextStore elContextStore = ELContextStore.getInstance(true);

        // Already available in the cache. Let's return it
        final Object contextualInstance = elContextStore.findBeanByName(beanName);
        if(contextualInstance != null)
        {
            context.setPropertyResolved(true);
            return contextualInstance;
        }

        // check if it's a recursive call to handle dotted based names
        if (base instanceof WrappedValueExpressionNode)
        {
            final String baseBeanName = ((WrappedValueExpressionNode) base).getFqBeanName();
            return findDottedName(context, baseBeanName, beanManager, elContextStore, beanName);
        }

        // Get bean candidates
        final Set<Bean<?>> beans = beanManager.getBeans(beanName);

        // Found?
        if(beans != null && !beans.isEmpty())
        {
            return getBeanWithScope(context, beanManager, beanName, elContextStore, beans);
        }
        else
        {
            // Fallback for TCK because CDI allows CDI beans to contain dots like @Named("magic.golden.fish")
            return findDottedName(context, null, beanManager, elContextStore, beanName);
        }

    }

    private Object getBeanWithScope(final ELContext context, final BeanManagerImpl beanManager, final String beanName,
                                    final ELContextStore elContextStore, final Set<Bean<?>> beans)
    {
        // Managed bean
        final Bean<?> bean = beanManager.resolve(beans);

        if(bean.getScope().equals(Dependent.class))
        {
            return getDependentContextualInstance(beanManager, elContextStore, context, bean);
        }
        else
        {
            // now we check for NormalScoped beans
            return getNormalScopedContextualInstance(beanManager, elContextStore, context, bean, beanName);
        }
    }

    private Object findDottedName(final ELContext context, final Object base, final BeanManagerImpl beanManager,
                                  final ELContextStore elContextStore, final String beanName)
    {

        final String fqBeanName = base == null ? beanName : base + "." + beanName;
        final Set<Bean<?>> anyBeanName = beanManager.getBeans().stream()
                                                    .filter(b -> b.getName() != null)
                                                    .filter(b -> b.getName().startsWith(fqBeanName))
                                                    .collect(Collectors.toSet());

        // looks like a good candidate
        if (anyBeanName.size() == 1 && fqBeanName.equals(anyBeanName.iterator().next().getName()))
        {
            return getBeanWithScope(context, beanManager, beanName, elContextStore, anyBeanName);
        }
        // more than one bean with the same beginning or name not matching
        else if (!anyBeanName.isEmpty())
        {
            context.setPropertyResolved(true);
            return new WrappedValueExpressionNode(fqBeanName);
        }
        return null;
    }

    protected Object getNormalScopedContextualInstance(BeanManagerImpl manager, ELContextStore store, ELContext context,
                                                       Bean<?> bean, String beanName)
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
