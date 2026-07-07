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

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.el.ELContextStore;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


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

    /**
     * beanName (or dotted-prefix) -> {@code true}, for names which are known to never resolve
     * to a dot-named {@link Bean}. Backed by a {@link ConcurrentHashMap} instead of a
     * {@link java.util.concurrent.CopyOnWriteArraySet} because misses can be caused by
     * arbitrary/high-cardinality EL identifiers (e.g. per-row expressions), which would
     * otherwise degrade to an O(n) array copy on every single new entry.
     */
    private final Map<String, Boolean> dotNamedBeansNegativeCache = new ConcurrentHashMap<>();

    /**
     * Lazily built, immutable-after-publish index of all {@link Bean}s whose name contains a dot,
     * keyed by their full name. Only such beans can ever be candidates for
     * {@link #findDottedName(ELContext, Object, BeanManagerImpl, ELContextStore, String)}, so
     * indexing just this (usually tiny or empty) subset avoids a linear scan over
     * <b>all</b> managed beans on every EL root-property access.
     */
    private volatile NavigableMap<String, Set<Bean<?>>> dottedBeanNameIndex;

    public WebBeansELResolver()
    {
        webBeansContext = WebBeansContext.getInstance();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public Class<?> getType(ELContext context, Object base, Object property) throws ELException
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
        // we only check root beans
        // or if its wrapped because of dot-names
        if (base != null && !(base instanceof WrappedValueExpressionNode))
        {
            return null;
        }

        // Check if the OWB actually got used in this application
        final BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();
        if (!beanManager.isInUse())
        {
            return null;
        }

        //Name of the bean
        final String beanName = (String) property;

        // Fast path for root-level identifiers already known to never resolve to any CDI
        // bean (exact or dot-prefixed), e.g. JSF implicit objects (#{request}, #{cc}, ...)
        // or <ui:repeat>/<h:dataTable> row variables, which get re-evaluated for every row
        // of every request. Skips the ELContextStore lookup and bean-name resolution below
        // entirely. Safe because this cache is only ever populated from findDottedName(...)
        // with a null base after beanManager.getBeans(beanName) was already found empty, so
        // a hit here guarantees neither an exact nor a dotted match exists.
        if (base == null && dotNamedBeansNegativeCache.containsKey(beanName))
        {
            return null;
        }

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
        if (dotNamedBeansNegativeCache.containsKey(fqBeanName))
        {
            return null;
        }

        final Set<Bean<?>> anyBeanName = getDottedBeanCandidates(beanManager, fqBeanName);

        // no exact and no startsWith match
        if (anyBeanName.isEmpty())
        {
            dotNamedBeansNegativeCache.put(fqBeanName, Boolean.TRUE);
            return null;
        }

        // exact match
        if (anyBeanName.size() == 1 && fqBeanName.equals(anyBeanName.iterator().next().getName()))
        {
            return getBeanWithScope(context, beanManager, fqBeanName, elContextStore, anyBeanName);
        }

        // more than one bean with the same beginning
        context.setPropertyResolved(true);
        return new WrappedValueExpressionNode(fqBeanName);
    }

    /**
     * Returns all beans whose name is either exactly {@code fqBeanName} or starts with
     * {@code fqBeanName + "."}, using a sorted index over dot-named beans only, instead of
     * scanning every managed bean.
     */
    private Set<Bean<?>> getDottedBeanCandidates(final BeanManagerImpl beanManager, final String fqBeanName)
    {
        NavigableMap<String, Set<Bean<?>>> index = getDottedBeanNameIndex(beanManager);
        if (index.isEmpty())
        {
            return Collections.emptySet();
        }

        String prefix = fqBeanName + '.';
        Set<Bean<?>> result = null;

        Set<Bean<?>> exact = index.get(fqBeanName);
        if (exact != null)
        {
            result = new HashSet<>(exact);
        }

        for (Map.Entry<String, Set<Bean<?>>> entry : index.tailMap(prefix, true).entrySet())
        {
            if (!entry.getKey().startsWith(prefix))
            {
                break;
            }
            if (result == null)
            {
                result = new HashSet<>();
            }
            result.addAll(entry.getValue());
        }

        return result == null ? Collections.emptySet() : result;
    }

    /**
     * Lazily builds an index of all beans with a dot in their name, sorted by name so that
     * prefix-matches can be found via a small {@link NavigableMap#tailMap(Object, boolean)} range
     * instead of iterating over every bean in the application. This mirrors the permanent-cache
     * assumption already used by {@link #dotNamedBeansNegativeCache}: once the container is up and
     * serving EL expressions, the deployed bean set is stable.
     */
    private NavigableMap<String, Set<Bean<?>>> getDottedBeanNameIndex(final BeanManagerImpl beanManager)
    {
        if (dottedBeanNameIndex == null)
        {
            synchronized (this)
            {
                if (dottedBeanNameIndex == null)
                {
                    NavigableMap<String, Set<Bean<?>>> index = new TreeMap<>();
                    for (Bean<?> bean : beanManager.getBeans())
                    {
                        final String name = bean.getName();
                        if (name != null && name.indexOf('.') >= 0)
                        {
                            index.computeIfAbsent(name, k -> new HashSet<>()).add(bean);
                        }
                    }
                    dottedBeanNameIndex = index;
                }
            }
        }
        return dottedBeanNameIndex;
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
    public boolean isReadOnly(ELContext context, Object base, Object property) throws ELException
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) throws ELException
    {

    }
}
