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
package org.apache.webbeans.container;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.GenericsUtil;
import org.apache.webbeans.util.InjectionExceptionUtil;
import org.apache.webbeans.util.SingleItemSet;
import org.apache.webbeans.util.WebBeansUtil;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.New;
import javax.enterprise.inject.UnproxyableResolutionException;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.webbeans.util.InjectionExceptionUtil.throwAmbiguousResolutionException;

/**
 * Injection point resolver class.
 * <p/>
 * <p>
 * It is a singleton class per BeanManager. It is
 * responsible for resolving the bean instances at the injection points for
 * its bean manager.
 * </p>
 *
 * @version $Rev$ $Date$
 */
public class InjectionResolver
{
    private static final Logger logger = WebBeansLoggerFacade.getLogger(InjectionResolver.class);

    /**
     * Bean Manager
     */
    private WebBeansContext webBeansContext;

    private AlternativesManager alternativesManager;
    
    /**
     * This Map contains all resolved beans via it's type and qualifiers.
     * If a bean have resolved as not existing, the entry will contain <code>null</code> as value.
     * The Long key is a hashCode, see {@link BeanCacheKey#BeanCacheKey(boolean, Type, String, Annotation...)}
     */
    private Map<BeanCacheKey, Set<Bean<?>>> resolvedBeansByType = new ConcurrentHashMap<BeanCacheKey, Set<Bean<?>>>();

    /**
     * This Map contains all resolved beans via it's ExpressionLanguage name.
     */
    private Map<String, Set<Bean<?>>> resolvedBeansByName = new ConcurrentHashMap<String, Set<Bean<?>>>();

    /**
     * Creates a new injection resolve for given bean manager.
     *
     * @param webBeansContext WebBeansContext
     */
    public InjectionResolver(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        this.alternativesManager = webBeansContext.getAlternativesManager();
    }

    /**
     * Clear caches.
     */
    public void clearCaches()
    {
        resolvedBeansByName.clear();
        resolvedBeansByType.clear();
    }

    /**
     * Check the type of the injection point.
     * <p>
     * Injection point type can not be {@link java.lang.reflect.TypeVariable}.
     * </p>
     *
     * @param injectionPoint injection point
     * @throws WebBeansConfigurationException if not obey the rule
     */
    public void checkInjectionPointType(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();

        //Check for injection point type variable
        if (ClassUtil.isTypeVariable(type))
        {
            throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " can not define Type Variable generic type");
        }

        //Check for raw event type (10.3.2)
        if (type == Event.class)
        {
            throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " needs to define type argument for " + Event.class.getName());
        }
        if (type == Instance.class)
        {
            throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " needs to define type argument for " + Instance.class.getName());
        }

        // not that happy about this check here and at runtime but few TCKs test Weld behavior only...
        final Bean<?> bean = resolve(implResolveByType(false, type, injectionPoint.getQualifiers().toArray(new Annotation[injectionPoint.getQualifiers().size()])),
                                     injectionPoint);
        if (bean != null && ManagedBean.class.isInstance(bean))
        {
            try
            {
                ManagedBean.class.cast(bean).valid();
            }
            catch (final UnproxyableResolutionException ure)
            {
                throw new WebBeansDeploymentException(ure);
            }
        }
    }

    /**
     * Check that a valid enabled bean exists in the deployment for the given
     * injection point definition.
     *
     * @param injectionPoint injection point
     * @throws WebBeansConfigurationException If bean is not available in the current deployment for given injection
     */
    public void checkInjectionPoint(InjectionPoint injectionPoint)
    {
        WebBeansUtil.checkInjectionPointNamedQualifier(injectionPoint);

        Type type = injectionPoint.getType();
        if (ClassUtil.isTypeVariable(type))
        {
            throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " type can not be defined as Typevariable or Wildcard type!");
        }

        if (webBeansContext.getBeanManagerImpl().isAfterBeanDiscoveryFired())
        {
            Annotation[] qualifiers = new Annotation[injectionPoint.getQualifiers().size()];
            qualifiers = injectionPoint.getQualifiers().toArray(qualifiers);

            // OWB-890 some 3rd party InjectionPoints return null in getBean();
            Class<?> injectionPointClass = Object.class; // the fallback
            Bean injectionPointBean = injectionPoint.getBean();
            if (injectionPointBean != null)
            {
                injectionPointClass = injectionPointBean.getBeanClass();
            }
            if (injectionPointClass == null && type instanceof Class)
            {
                injectionPointClass = (Class) type;
            }

            Set<Bean<?>> beanSet = implResolveByType(injectionPoint.isDelegate(), type, injectionPointClass, qualifiers);

            if (beanSet.isEmpty())
            {
                if (qualifiers.length == 1 && qualifiers[0].annotationType().equals(New.class))
                {
                    createNewBean(injectionPoint, type, qualifiers, beanSet);
                }
            }

            Bean<?> bean = resolve(beanSet, injectionPoint);

            if (bean == null)
            {
                Class<?> clazz;
                if (type instanceof ParameterizedType)
                {
                    ParameterizedType pt = (ParameterizedType) type;

                    clazz = (Class<?>) pt.getRawType();
                }
                else
                {
                    clazz = (Class<?>) type;
                }
                InjectionExceptionUtil.throwUnsatisfiedResolutionException(clazz, injectionPoint, qualifiers);
            }
        }
    }


    /**
     * Returns bean for injection point.
     *
     * @param injectionPoint injection point declaration
     * @return bean for injection point
     */
    public Bean<?> getInjectionPointBean(InjectionPoint injectionPoint)
    {

        Type type = injectionPoint.getType();
        Class<?> clazz;

        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;
            clazz = (Class<?>) pt.getRawType();
        }
        else
        {
            clazz = (Class<?>) type;
        }

        Set<Annotation> qualSet = injectionPoint.getQualifiers();
        Annotation[] qualifiers = qualSet.toArray(new Annotation[qualSet.size()]);
        if (isInstanceOrEventInjection(type))
        {
            qualifiers = AnyLiteral.ARRAY;
        }

        Set<Bean<?>> beanSet = implResolveByType(injectionPoint.isDelegate(), type, clazz, qualifiers);

        if (beanSet.isEmpty())
        {
            if (qualifiers.length == 1 && qualifiers[0].annotationType().equals(New.class))
            {
                createNewBean(injectionPoint, type, qualifiers, beanSet);
            }
            else
            {
                InjectionExceptionUtil.throwUnsatisfiedResolutionException(clazz, injectionPoint, qualifiers);
            }
        }

        return resolve(beanSet, injectionPoint);
    }

    private void createNewBean(InjectionPoint injectionPoint, Type type, Annotation[] qualifiers, Set<Bean<?>> beanSet)
    {
        New newQualifier = (New) qualifiers[0];
        Class<?> newType;
        if (newQualifier.value() == New.class)
        {
            newType = ClassUtil.getClass(type);
        }
        else
        {
            newType = newQualifier.value();
        }
        Set<Bean<?>> beans = implResolveByType(injectionPoint.isDelegate(), newType, injectionPoint.getBean().getBeanClass(), AnyLiteral.INSTANCE);
        if (beans.isEmpty())
        {
            beanSet.add(webBeansContext.getWebBeansUtil().createNewComponent(newType));
        }
        else
        {
            // we just need the bean for the injection points. So when we find an InjectionTargetBean, we can just take it.
            for (Bean<?> bean: beans)
            {
                if (bean instanceof InjectionTargetBean)
                {
                    beanSet.add(webBeansContext.getWebBeansUtil().createNewComponent((OwbBean)bean, (Class)newType));
                    break;
                }
            }
            if (beanSet.isEmpty())
            {
                //Hmm, no InjectionTargetBean available, then we have to create the injection points on our own
                beanSet.add(webBeansContext.getWebBeansUtil().createNewComponent((Class)newType));
            }
        }
    }


    private boolean isInstanceOrEventInjection(Type type)
    {
        Class<?> clazz;
        boolean injectInstanceOrEventProvider = false;
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;
            clazz = (Class<?>) pt.getRawType();

            if (clazz.isAssignableFrom(Instance.class) || clazz.isAssignableFrom(Event.class))
            {
                injectInstanceOrEventProvider = true;
            }
        }

        return injectInstanceOrEventProvider;
    }


    /**
     * Returns set of beans for given bean name.
     *
     * @param name bean name
     * @return set of beans for given bean name
     */
    @SuppressWarnings("unchecked")
    public Set<Bean<?>> implResolveByName(String name)
    {
        Asserts.assertNotNull(name, "name parameter");

        String cacheKey = name;
        Set<Bean<?>> resolvedComponents = resolvedBeansByName.get(cacheKey);
        if (resolvedComponents != null)
        {
            return resolvedComponents;
        }

        resolvedComponents = new HashSet<Bean<?>>();
        Set<Bean<?>> deployedComponents = webBeansContext.getBeanManagerImpl().getBeans();

        Iterator<Bean<?>> it = deployedComponents.iterator();
        //Finding all beans with given name
        while (it.hasNext())
        {
            Bean<?> component = it.next();
            if (component.getName() != null)
            {
                if (component.getName().equals(name))
                {
                    resolvedComponents.add(component);
                }
            }
        }

        if (resolvedComponents.isEmpty())
        {
            // maintain negative cache but use standard empty set so we can garbage collect
            resolvedBeansByName.put(cacheKey, Collections.EMPTY_SET);
        }
        else
        {
            resolvedBeansByName.put(cacheKey, resolvedComponents);
        }
        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "DEBUG_ADD_BYNAME_CACHE_BEANS", cacheKey);
        }

        return resolvedComponents;
    }


    /**
     * Resolution by type.
     *
     * @param isDelegate whether the InjectionPoint is for a {@link javax.decorator.Delegate}
     * @param injectionPointType injection point api type
     * @param qualifiers         qualifiers of the injection point
     * @return set of resolved beans
     */
    public Set<Bean<?>> implResolveByType(boolean isDelegate, Type injectionPointType, Annotation... qualifiers)
    {
        return implResolveByType(isDelegate, injectionPointType, null, qualifiers);
    }

    private String getBDABeansXMLPath(Class<?> injectionPointBeanClass)
    {
        if (injectionPointBeanClass == null)
        {
            return null;
        }

        ScannerService scannerService = webBeansContext.getScannerService();
        BDABeansXmlScanner beansXMLScanner = scannerService.getBDABeansXmlScanner();
        return beansXMLScanner.getBeansXml(injectionPointBeanClass);
    }

    /**
     * Resolution by type.
     *
     * @param isDelegate whether the InjectionPoint is for a {@link javax.decorator.Delegate}
     * @param injectionPointType injection point api type
     * @param qualifiers         qualifiers of the injection point
     * @return set of resolved beans
     */
    public Set<Bean<?>> implResolveByType(boolean isDelegate, Type injectionPointType,
                                          Class<?> injectionPointClass, Annotation... qualifiers)
    {
        ScannerService scannerService = webBeansContext.getScannerService();
        String bdaBeansXMLFilePath = null;
        if (scannerService.isBDABeansXmlScanningEnabled())
        {
            bdaBeansXMLFilePath = getBDABeansXMLPath(injectionPointClass);
        }

        boolean currentQualifier = false;

        if (isInstanceOrEventInjection(injectionPointType))
        {
            qualifiers = AnyLiteral.ARRAY;
        }
        else
        {
            if (qualifiers.length == 0)
            {
                qualifiers = DefaultLiteral.ARRAY;
                currentQualifier = true;
            }
        }

        validateInjectionPointType(injectionPointType);

        BeanCacheKey cacheKey = new BeanCacheKey(isDelegate, injectionPointType, bdaBeansXMLFilePath, qualifiers);

        Set<Bean<?>> resolvedComponents = resolvedBeansByType.get(cacheKey);
        if (resolvedComponents != null)
        {
            return resolvedComponents;
        }

        resolvedComponents = new HashSet<Bean<?>>();

        boolean returnAll = injectionPointType.equals(Object.class) && currentQualifier;

        for (Bean<?> component : webBeansContext.getBeanManagerImpl().getBeans())
        {
            // no need to check instanceof OwbBean as we always wrap in a
            // ThirdpartyBeanImpl at least
            if (!((OwbBean) component).isEnabled())
            {
                continue;
            }

            if (returnAll)
            {
                resolvedComponents.add(component);
            }
            else
            {
                for (Type componentApiType : component.getTypes())
                {

                    if (ClassUtil.isRawClassEquals(injectionPointType, componentApiType))
                    {
                        resolvedComponents.add(component);
                        break;
                    }
                }
            }
        }

        if (!returnAll)
        {
            // Look for qualifiers
            resolvedComponents = findByQualifier(resolvedComponents, injectionPointType, qualifiers);

            // have an additional round of checks for assignability of parameterized types.
            Set<Bean<?>> byParameterizedType = findByParameterizedType(resolvedComponents, injectionPointType, isDelegate);
            if (byParameterizedType.isEmpty())
            {
                resolvedComponents = findByBeanType(resolvedComponents, injectionPointType, isDelegate);
            }
            else
            {
                resolvedComponents = byParameterizedType;
            }
        }
        resolvedBeansByType.put(cacheKey, resolvedComponents);
        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "DEBUG_ADD_BYTYPE_CACHE_BEANS", cacheKey);
        }

        return resolvedComponents;
    }

    private Set<Bean<?>> findByBeanType(Set<Bean<?>> allComponents, Type injectionPointType, boolean isDelegate)
    {
        Set<Bean<?>> resolved = new HashSet<Bean<?>>();
        for (Bean<?> bean : allComponents)
        {
            boolean isProducer = AbstractProducerBean.class.isInstance(bean);
            for (Type type : bean.getTypes())
            {
                if (GenericsUtil.satisfiesDependency(isDelegate, isProducer, injectionPointType, type))
                {
                    resolved.add(bean);
                }

                if (!ClassUtil.isParametrizedType(injectionPointType)
                        && ClassUtil.isRawClassEquals(injectionPointType, type))
                {
                    resolved.add(bean);
                }
            }
        }
        return resolved;
    }

    private Set<Bean<?>> findByParameterizedType(Set<Bean<?>> allComponents, Type injectionPointType, boolean isDelegate)
    {
        Bean<?> rawProducerBean = null;

        Set<Bean<?>> resolvedComponents = new HashSet<Bean<?>>();
        for (Bean<?> component : allComponents)
        {
            boolean isProducer = AbstractProducerBean.class.isInstance(component);
            for (Type componentApiType : component.getTypes())
            {

                if (GenericsUtil.satisfiesDependency(isDelegate, isProducer, injectionPointType, componentApiType))
                {
                    resolvedComponents.add(component);
                    break;
                }
                else if (isProducer && componentApiType instanceof Class && ClassUtil.isRawClassEquals(injectionPointType, componentApiType))
                {
                   rawProducerBean = component;
                }
            }
        }

        if (resolvedComponents.isEmpty() && rawProducerBean != null)
        {
            resolvedComponents.add(rawProducerBean);
        }

        return resolvedComponents;
    }


    /**
     * Verify that we have a legal Type at the injection point.
     * CDI can basically only handle Class and ParameterizedType injection points atm.
     * @throws WebBeansConfigurationException on TypeVariable, WildcardType and GenericArrayType
     * @throws IllegalArgumentException if the type is not yet supported by the spec.
     */
    private void validateInjectionPointType(Type injectionPointType)
    {
        if (injectionPointType instanceof TypeVariable || injectionPointType instanceof WildcardType || injectionPointType instanceof GenericArrayType)
        {
            throw new WebBeansConfigurationException("Injection point cannot define Type Variable " + injectionPointType);
        }

        if (!(injectionPointType instanceof Class) &&
            !(injectionPointType instanceof ParameterizedType))
        {
            throw new IllegalArgumentException("Unsupported type " + injectionPointType.getClass());
        }
    }

    /**
     * Gets alternatives from set.
     *
     * @param beans resolved set
     * @return contains alternatives
     */
    public <X> Set<Bean<? extends X>> findByAlternatives(Set<Bean<? extends X>> beans)
    {
        // first check whether we have Alternatives with a Priority annotation
        List<Class<?>> prioritizedAlternatives = alternativesManager.getPrioritizedAlternatives();

        for (Class<?> alternativeClazz : prioritizedAlternatives)
        {
            for (Bean<? extends X> bean: beans)
            {
                if (alternativeClazz.equals(bean.getBeanClass()))
                {
                    return new SingleItemSet<Bean<? extends X>>(bean);
                }
            }

        }


        // if none such Alternative got found let's check the 'old' alternatives from beans.xml
        Set<Bean<? extends X>> alternativeSet = new HashSet<Bean<? extends X>>();
        Set<Bean<? extends X>> enableSet = new HashSet<Bean<? extends X>>();

        for (Bean<? extends X> bean : beans)
        {
            if (bean.isAlternative())
            {
                alternativeSet.add(bean);
            }
            else
            {
                if (alternativeSet.isEmpty())
                {
                    AbstractOwbBean<?> temp = (AbstractOwbBean<?>) bean;
                    if (temp.isEnabled())
                    {
                        enableSet.add(bean);
                    }
                }
            }
        }

        if (!alternativeSet.isEmpty())
        {
            return alternativeSet;
        }

        return enableSet;
    }

    /**
     * resolve any ambiguity by checking for Alternatives.
     * If any &#064;Alternative exists, then we pick the one with the
     * highest priority.
     *
     * @param beans
     * @param injectionPoint only used for logging. Can be null.
     * @param <X>
     * @return the single resolved bean, null if none is activated
     * @throws javax.enterprise.inject.AmbiguousResolutionException if more than 1 bean is active
     */
    public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans, InjectionPoint injectionPoint)
    {
        if (beans == null || beans.isEmpty())
        {
            return null;
        }

        if (beans.size() == 1)
        {
            // if there is only one Bean left, then there is for sure no ambiguity.
            return beans.iterator().next();
        }

        Set set = resolveAll(beans);
        
        if (set.isEmpty())
        {
            return null;
        }

        if(set.size() > 1)
        {
            throwAmbiguousResolutionException(set, null, injectionPoint);
        }

        return (Bean<? extends X>)set.iterator().next();
    }

    public <X> Set<Bean<? extends X>> resolveAll(Set<Bean<? extends X>> beans)
    {
        if (beans == null || beans.isEmpty())
        {
            return Collections.emptySet();
        }

        Set set = findByAlternatives(beans);

        if (set == null || set.isEmpty())
        {
            return Collections.emptySet();
        }

        return set;
    }

    /**
     * Returns filtered bean set according to the qualifiers.
     *
     * @param remainingSet bean set for filtering by qualifier
     * @param annotations  qualifiers on injection point
     * @return filtered bean set according to the qualifiers
     */
    private Set<Bean<?>> findByQualifier(Set<Bean<?>> remainingSet, Type type, Annotation... annotations)
    {
        Iterator<Bean<?>> it = remainingSet.iterator();
        Set<Bean<?>> result = new HashSet<Bean<?>>();

        while (it.hasNext())
        {
            Bean<?> component = it.next();
            Set<Annotation> qTypes = component.getQualifiers();

            int i = 0;
            for (Annotation annot : annotations)
            {
                Iterator<Annotation> itQualifiers = qTypes.iterator();
                while (itQualifiers.hasNext())
                {
                    Annotation qualifier = itQualifiers.next();
                    if (annot.annotationType().equals(qualifier.annotationType()))
                    {
                        AnnotatedType<?> at = webBeansContext.getBeanManagerImpl().getAdditionalAnnotatedTypeQualifiers().get(qualifier.annotationType());
                        if (at == null)
                        {
                            if (AnnotationUtil.isCdiAnnotationEqual(qualifier, annot))
                            {
                                i++;
                            }
                        }
                        else
                        {
                            if (AnnotationUtil.isCdiAnnotationEqual(at, qualifier, annot))
                            {
                                i++;
                            }
                        }
                    }

                }
            }

            if (i == annotations.length)
            {
                result.add(component);
            }
        }

        if (result.isEmpty() && annotations.length == 1 && New.class.equals(annotations[0].annotationType()))
        { // happen in TCKs, shouldn't be the case in real apps
            New newQualifier = (New)annotations[0];
            Class<?> beanClass;
            if (newQualifier.value() != New.class)
            {
                beanClass = newQualifier.value(); 
            }
            else
            {
                beanClass = GenericsUtil.getRawType(type);
            }
            result.add(webBeansContext.getWebBeansUtil().createNewComponent(beanClass));
        }

        return result;
    }
}
