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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.New;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.inject.NullableDependencyException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Injection point resolver class.
 * <p/>
 * <p>
 * It is a singleton class per ClassLoader per JVM. It is
 * responsible for resolving the bean instances at the injection points for
 * its bean manager.
 * </p>
 *
 * @version $Rev$ $Date$
 * @see org.apache.webbeans.config.WebBeansFinder
 */
public class InjectionResolver
{
    private final Logger logger = WebBeansLoggerFacade.getLogger(InjectionResolver.class);

    /**
     * Bean Manager
     */
    private WebBeansContext webBeansContext;
    
    private final static Annotation[] DEFAULT_LITERAL_ARRAY = new Annotation[]{new DefaultLiteral()};
    private final static Annotation[] ANY_LITERAL_ARRAY = new Annotation[]{new AnyLiteral()};

    /**
     * This Map contains all resolved beans via it's type and qualifiers.
     * If a bean have resolved as not existing, the entry will contain <code>null</code> as value.
     * The Long key is a hashCode, see {@link #getBeanCacheKey(Type, String, Annotation...)}
     */
    private Map<Long, Set<Bean<?>>> resolvedBeansByType = new ConcurrentHashMap<Long, Set<Bean<?>>>();

    /**
     * This Map contains all resolved beans via it's ExpressionLanguage name.
     */
    private Map<String, Set<Bean<?>>> resolvedBeansByName = new ConcurrentHashMap<String, Set<Bean<?>>>();

    //X TODO refactor. public static variables are utterly ugly
    public static ThreadLocal<InjectionPoint> injectionPoints = new ThreadLocal<InjectionPoint>();

    /**
     * Creates a new injection resolve for given bean manager.
     *
     * @param webBeansContext WebBeansContext
     */
    public InjectionResolver(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;

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
     * Returns bean manager injection resolver.
     *
     * @return bean manager injection resolver
     * @see org.apache.webbeans.config.WebBeansFinder
     */
    public static InjectionResolver getInstance()
    {
        InjectionResolver instance = WebBeansContext.getInstance().getBeanManagerImpl().getInjectionResolver();

        return instance;
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

    }

    /**
     * Check that bean exist in the deployment for given
     * injection point definition.
     *
     * @param injectionPoint injection point
     * @throws WebBeansConfigurationException If bean is not avialable in the current deployment for given injection
     */
    public void checkInjectionPoints(InjectionPoint injectionPoint)
    {
        WebBeansUtil.checkInjectionPointNamedQualifier(injectionPoint);

        Type type = injectionPoint.getType();
        Class<?> clazz = null;

        if (ClassUtil.isTypeVariable(type))
        {
            throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " type can not be defined as Typevariable or Wildcard type!");
        }

        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;

            clazz = (Class<?>) pt.getRawType();
        }
        else
        {
            clazz = (Class<?>) type;
        }

        Annotation[] qualifiers = new Annotation[injectionPoint.getQualifiers().size()];
        qualifiers = injectionPoint.getQualifiers().toArray(qualifiers);

        Set<Bean<?>> beanSet = implResolveByType(type, injectionPoint.getBean().getBeanClass(), qualifiers);

        if (beanSet.isEmpty())
        {
            if (qualifiers.length == 1 && qualifiers[0].annotationType().equals(New.class))
            {
                New newQualifier = (New) qualifiers[0];

                if (newQualifier.value() == New.class)
                {
                    beanSet.add(webBeansContext.getWebBeansUtil().createNewComponent(clazz, type));
                }
                else
                {
                    beanSet.add(webBeansContext.getWebBeansUtil().createNewComponent(newQualifier.value(), null));
                }

            }
        }

        webBeansContext.getResolutionUtil().checkResolvedBeans(beanSet, clazz, qualifiers, injectionPoint);

        Bean<?> bean = beanSet.iterator().next();

        if (clazz.isPrimitive())
        {
            if (bean.isNullable())
            {
                throw new NullableDependencyException("Injection point type : " + injectionPoint + " type is primitive but resolved bean can have nullable objects!");
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
        Class<?> clazz = null;

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
            qualifiers = new Annotation[1];
            qualifiers[0] = new AnyLiteral();
        }

        Class injectionPointClass = null;
        if (injectionPoint.getBean() != null)
        {
            injectionPointClass = injectionPoint.getBean().getBeanClass();
        }
        Set<Bean<?>> beanSet = implResolveByType(type, injectionPointClass, qualifiers);

        if (beanSet.isEmpty())
        {
            if (qualifiers.length == 1 && qualifiers[0].annotationType().equals(New.class))
            {
                New newQualifier = (New) qualifiers[0];

                if (newQualifier.value() == New.class)
                {
                    beanSet.add(webBeansContext.getWebBeansUtil().createNewComponent(clazz, type));
                }
                else
                {
                    beanSet.add(webBeansContext.getWebBeansUtil().createNewComponent(newQualifier.value(), null));
                }

            }
        }

        webBeansContext.getResolutionUtil().checkResolvedBeans(beanSet, clazz, qualifiers, injectionPoint);

        return beanSet.iterator().next();

    }


    private boolean isInstanceOrEventInjection(Type type)
    {
        Class<?> clazz = null;
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
        Asserts.assertNotNull(name, "name parameter can not be null");

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

        //Look for enable/disable
        resolvedComponents = findByEnabled(resolvedComponents);

        //Still Ambigious, check for specialization
        if (resolvedComponents.size() > 1)
        {
            //Check for specialization
            Set<Bean<?>> specializedComponents = findSpecializedForNameResolution(resolvedComponents);
            if (specializedComponents.size() > 0)
            {
                resolvedComponents = specializedComponents;
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

    private Set<Bean<?>> findByEnabled(Set<Bean<?>> resolvedComponents)
    {
        Set<Bean<?>> specializedComponents = new HashSet<Bean<?>>();
        if (resolvedComponents.size() > 0)
        {
            for (Bean<?> bean : resolvedComponents)
            {
                AbstractOwbBean<?> component = (AbstractOwbBean<?>) bean;

                if (component.isEnabled())
                {
                    specializedComponents.add(component);
                }
            }
        }

        return specializedComponents;

    }


    /**
     * Returns filtered set by specialization.
     *
     * @param resolvedComponents result beans
     * @return filtered set by specialization
     */
    private Set<Bean<?>> findSpecializedForNameResolution(Set<Bean<?>> resolvedComponents)
    {
        Set<Bean<?>> specializedComponents = new HashSet<Bean<?>>();
        if (resolvedComponents.size() > 0)
        {
            for (Bean<?> bean : resolvedComponents)
            {
                AbstractOwbBean<?> component = (AbstractOwbBean<?>) bean;

                if (component.isSpecializedBean())
                {
                    specializedComponents.add(component);
                }
            }
        }

        return specializedComponents;
    }

    /**
     * Resolution by type.
     *
     * @param injectionPointType injection point api type
     * @param qualifiers         qualifiers of the injection point
     * @return set of resolved beans
     */
    public Set<Bean<?>> implResolveByType(Type injectionPointType, Annotation... qualifiers)
    {
        return implResolveByType(injectionPointType, null, qualifiers);
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
     * @param injectionPointType injection point api type
     * @param qualifiers         qualifiers of the injection point
     * @return set of resolved beans
     */
    public Set<Bean<?>> implResolveByType(Type injectionPointType, Class<?> injectinPointClass, Annotation... qualifiers)
    {
        ScannerService scannerService = webBeansContext.getScannerService();
        String bdaBeansXMLFilePath = null;
        if (scannerService.isBDABeansXmlScanningEnabled())
        {
            if (injectinPointClass == null)
            {
                // Retrieve ip from thread local for producer case
                InjectionPoint ip = injectionPoints.get();
                if (ip != null)
                {
                    injectinPointClass = ip.getBean().getBeanClass();
                }
            }
            bdaBeansXMLFilePath = getBDABeansXMLPath(injectinPointClass);
        }

        boolean currentQualifier = false;

        if (isInstanceOrEventInjection(injectionPointType))
        {
            qualifiers = ANY_LITERAL_ARRAY;
        }
        else
        {
            if (qualifiers.length == 0)
            {
                qualifiers = DEFAULT_LITERAL_ARRAY;
                currentQualifier = true;
            }
        }

        Long cacheKey = getBeanCacheKey(injectionPointType, bdaBeansXMLFilePath, qualifiers);

        Set<Bean<?>> resolvedComponents = resolvedBeansByType.get(cacheKey);
        if (resolvedComponents != null)
        {
            return resolvedComponents;
        }

        resolvedComponents = new HashSet<Bean<?>>();

        boolean returnAll = false;

        if (injectionPointType.equals(Object.class) && currentQualifier)
        {
            returnAll = true;
        }

        for (Bean<?> component : webBeansContext.getBeanManagerImpl().getBeans())
        {

            if (returnAll)
            {
                resolvedComponents.add(component);
            }
            else
            {
                for (Type componentApiType : component.getTypes())
                {

                    if (ClassUtil.isAssignable(componentApiType, injectionPointType))
                    {
                        resolvedComponents.add(component);
                        break;
                    }
                }
            }
        }

        // Look for qualifiers
        resolvedComponents = findByQualifier(resolvedComponents, qualifiers);

        if (!injectionPointType.equals(Object.class))
        {
            // see OWB-658 skip if all Object.class instances are requested
            // as there is no way to Alternative Object.class

            // Look for alternative
            resolvedComponents = findByAlternatives(resolvedComponents, bdaBeansXMLFilePath);
        }

        // Ambigious resolution, check for specialization
        if (resolvedComponents.size() > 1)
        {
            //Look for specialization
            resolvedComponents = findBySpecialization(resolvedComponents);
        }

        resolvedBeansByType.put(cacheKey, resolvedComponents);
        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "DEBUG_ADD_BYTYPE_CACHE_BEANS", cacheKey);
        }

        return resolvedComponents;
    }

    private Long getBeanCacheKey(Type injectionPointType, String bdaBeansXMLPath, Annotation... qualifiers)
    {

        long cacheKey = getTypeHashCode(injectionPointType);

        if (bdaBeansXMLPath != null)
        {
            cacheKey += 29L * bdaBeansXMLPath.hashCode();
        }

        for (Annotation a : qualifiers)
        {
            cacheKey += 29L * getQualifierHashCode(a);
        }
        return cacheKey;
    }

    /**
     * We need this method as some weird JVMs return 0 as hashCode for classes.
     * In that case we return the hashCode of the String.
     */
    private int getTypeHashCode(Type type)
    {
        int typeHash = type.hashCode();
        if (typeHash == 0)
        {
            return type.toString().hashCode();
        }

        return typeHash;
    }

    /**
     * Calculate the hashCode of a Qualifier
     */
    private long getQualifierHashCode(Annotation a)
    {
        Class annotationClass = getAnnotationClass(a.getClass());

        if (annotationClass == null)
        {
            return getTypeHashCode(a.getClass());
        }

        // the hashCode of an Annotation is calculated solely via the hashCodes
        // of it's members. If there are no members, it is 0.
        // thus we first need to get the annotation-class hashCode
        long hashCode = getTypeHashCode(annotationClass);

        // and now add the hashCode of all it's Nonbinding members
        // the following algorithm is defined by the Annotation class definition
        // see the JavaDoc for Annotation!
        // we only change it so far that we skip evaluating @Nonbinding members
        Method[] methods = annotationClass.getDeclaredMethods();

        for (Method method : methods)
        {
            if (method.isAnnotationPresent(Nonbinding.class))
            {
                continue;
            }

            // Member name
            int name = 127 * method.getName().hashCode();

            // Member value
            Object object = callMethod(a, method);
            int value = 0;
            if(object.getClass().isArray())
            {
                Class<?> type = object.getClass().getComponentType();
                if(type.isPrimitive())
                {
                    if(Long.TYPE == type)
                    {
                        value = Arrays.hashCode((Long[]) object);
                    }
                    else if(Integer.TYPE == type)
                    {
                        value = Arrays.hashCode((Integer[])object);
                    }
                    else if(Short.TYPE == type)
                    {
                        value = Arrays.hashCode((Short[])object);
                    }
                    else if(Double.TYPE == type)
                    {
                        value = Arrays.hashCode((Double[])object);
                    }
                    else if(Float.TYPE == type)
                    {
                        value = Arrays.hashCode((Float[])object);
                    }
                    else if(Boolean.TYPE == type)
                    {
                        value = Arrays.hashCode((Long[])object);
                    }
                    else if(Byte.TYPE == type)
                    {
                        value = Arrays.hashCode((Byte[])object);
                    }
                    else if(Character.TYPE == type)
                    {
                        value = Arrays.hashCode((Character[])object);
                    }
                }
                else
                {
                    value = Arrays.hashCode((Object[])object);
                }
            }
            else
            {
                value = object.hashCode();
            }

            hashCode += name ^ value;


            hashCode += 29L * a.hashCode();
        }

        return hashCode;
    }

    private Class getAnnotationClass(Class a)
    {
        for (Class i : a.getInterfaces())
        {
            if (i.isAnnotation())
            {
                return i;
            }
        }
        return null;
    }

    /**
     * Helper method for calculating the hashCode of an annotation.
     */
    private Object callMethod(Object instance, Method method)
    {
        try
        {
            if (!method.isAccessible())
            {
                method.setAccessible(true);
            }

            return method.invoke(instance, AnnotationUtil.EMPTY_OBJECT_ARRAY);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception in method call : " + method.getName(), e);
        }
    }

    /**
     * Returns specialized beans if exists, otherwise return input result
     *
     * @param result result beans
     * @return specialized beans if exists, otherwise return input result
     */
    public Set<Bean<?>> findBySpecialization(Set<Bean<?>> result)
    {
        Iterator<Bean<?>> it = result.iterator();
        Set<Bean<?>> res = new HashSet<Bean<?>>();

        while (it.hasNext())
        {
            AbstractOwbBean<?> component = (AbstractOwbBean<?>) it.next();
            if (component.isSpecializedBean() && component.isEnabled())
            {
                res.add(component);
            }
        }

        if (res.size() > 0)
        {
            return res;
        }

        return result;
    }

    /**
     * Gets alternatives from set.
     *
     * @param result resolved set
     * @return containes alternatives
     */
    public Set<Bean<?>> findByAlternatives(Set<Bean<?>> result)
    {
        return findByAlternatives(result, null);
    }

    /**
     * Gets alternatives from set.
     *
     * @param result resolved set
     * @return containes alternatives
     */
    public Set<Bean<?>> findByAlternatives(Set<Bean<?>> result, String bdaBeansXMLFilePath)
    {
        Set<Bean<?>> alternativeSet = new HashSet<Bean<?>>();
        Set<Bean<?>> enableSet = new HashSet<Bean<?>>();
        boolean containsAlternative = false;

        if (bdaBeansXMLFilePath != null)
        {
            // per BDA beans.xml
            for (Bean<?> bean : result)
            {
                if (bean.isAlternative())
                {
                    if (isAltBeanInInjectionPointBDA(bdaBeansXMLFilePath, bean))
                    {
                        if (!containsAlternative)
                        {
                            containsAlternative = true;
                        }
                        alternativeSet.add(bean);
                    }
                }
                else
                {
                    if (!containsAlternative)
                    {
                        // Do not check isEnabled flag to allow beans to be
                        // added on a per BDA basis when a bean is disabled due
                        // to specialize alternative defined in a different BDA
                        enableSet.add(bean);
                    }
                }
            }
        }
        else
        {
            for (Bean<?> bean : result)
            {
                if (bean.isAlternative())
                {
                    if (!containsAlternative)
                    {
                        containsAlternative = true;
                    }
                    alternativeSet.add(bean);
                }
                else
                {
                    if (!containsAlternative)
                    {
                        AbstractOwbBean<?> temp = (AbstractOwbBean<?>) bean;
                        if (temp.isEnabled())
                        {
                            enableSet.add(bean);
                        }
                    }
                }
            }
        }

        if (containsAlternative)
        {
            return alternativeSet;
        }

        return enableSet;
    }

    private boolean isAltBeanInInjectionPointBDA(String bdaBeansXMLFilePath, Bean<?> altBean)
    {

        ScannerService scannerService = webBeansContext.getScannerService();
        BDABeansXmlScanner beansXMLScanner = scannerService.getBDABeansXmlScanner();

        Set<Class<?>> definedAlternatives = beansXMLScanner.getAlternatives(bdaBeansXMLFilePath);

        if (definedAlternatives.contains(altBean.getBeanClass()))
        {
            return true;
        }

        Set<Class<? extends Annotation>> definedStereotypes = beansXMLScanner.getStereotypes(bdaBeansXMLFilePath);

        for (Class<? extends Annotation> stereoAnnotations : definedStereotypes)
        {
            if (AnnotationUtil.hasClassAnnotation(altBean.getBeanClass(), stereoAnnotations))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns filtered bean set according to the qualifiers.
     *
     * @param remainingSet bean set for filtering by qualifier
     * @param annotations  qualifiers on injection point
     * @return filtered bean set according to the qualifiers
     */
    private Set<Bean<?>> findByQualifier(Set<Bean<?>> remainingSet, Annotation... annotations)
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
                        if (AnnotationUtil.isQualifierEqual(qualifier, annot))
                        {
                            i++;
                        }
                    }

                }
            }

            if (i == annotations.length)
            {
                result.add(component);
                i = 0;
            }

        }

        return result;
    }
}
