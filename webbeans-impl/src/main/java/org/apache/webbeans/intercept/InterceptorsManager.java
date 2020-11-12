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
package org.apache.webbeans.intercept;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.creation.BeanAttributesBuilder;
import org.apache.webbeans.component.creation.EjbInterceptorBeanBuilder;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.PriorityClasses;

/**
 * This class keeps all the enabled interceptor classes information of a certain BeanManager.
 */
public class InterceptorsManager
{
    private final WebBeansContext webBeansContext;
    private final BeanManagerImpl beanManager;

    /**
     * Interceptor classes which got added via beans.xml
     */
    private List<Class<?>> configuredInterceptorClasses = new CopyOnWriteArrayList<>();

    /**
     * Active CDI-style interceptors.
     */
    private List<Interceptor<?>> cdiInterceptors = new ArrayList<>();

    /**
     * EJB-style Interceptor beans.
     */
    private ConcurrentMap<Class<?>, Interceptor<?>> ejbInterceptors = new ConcurrentHashMap<>();

    /**Additional interceptor class*/
    private List<Class<?>> additionalInterceptorClasses = new ArrayList<>();

    /**
     * Additional interceptor binding types we got via Extensions
     */
    private Map<Class<? extends Annotation>, Set<Annotation>> additionalInterceptorBindingTypes
            = new HashMap<>();
    private final Collection<AnnotatedType<?>> additionalInterceptorBindingTypesAnnotatedTypes
            = new ArrayList<>();

    private final PriorityClasses priorityInterceptors = new PriorityClasses();


    public InterceptorsManager(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        beanManager = webBeansContext.getBeanManagerImpl();
    }

    /**
     * Clears all info.
     * This must only be called by the BeanManager.
     */
    public void clear()
    {
        additionalInterceptorBindingTypes.clear();
        additionalInterceptorClasses.clear();
        configuredInterceptorClasses.clear();
        cdiInterceptors.clear();
        ejbInterceptors.clear();
        priorityInterceptors.clear();
    }


    /**
     * Add a certain class to the enabled interceptors list.
     */
    public void addEnabledInterceptorClass(Class<?> interceptorClazz)
    {
        Asserts.nullCheckForClass(interceptorClazz, "interceptorClazz can not be null");

        if (!configuredInterceptorClasses.contains(interceptorClazz))
        {
            configuredInterceptorClasses.add(interceptorClazz);
        }
    }

    /**
     * get the EJB-style Interceptor
     * @param interceptorClass
     * @param <T>
     * @return
     */
    public <T> Interceptor<T> getEjbInterceptorForClass(Class<T> interceptorClass)
    {
        Interceptor<T> interceptor = (Interceptor<T>) ejbInterceptors.get(interceptorClass);
        if (interceptor == null)
        {
            AnnotatedType<T> annotatedType = webBeansContext.getAnnotatedElementFactory().getAnnotatedType(interceptorClass);
            if (annotatedType == null)
            {
                annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(interceptorClass);
            }

            BeanAttributesImpl<T> beanAttributes = BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(annotatedType).build();
            EjbInterceptorBeanBuilder<T> buildr = new EjbInterceptorBeanBuilder<>(webBeansContext, annotatedType, beanAttributes);
            buildr.defineEjbInterceptorRules();
            Interceptor<T> i = buildr.getBean();
            interceptor = (Interceptor<T>) ejbInterceptors.putIfAbsent(interceptorClass, i);
            if (interceptor == null)
            {
                interceptor = i;
            }
        }

        return interceptor;
    }

    /**
     * Helper to compare the order of different interceptor classes
     */
    public int compareCdiInterceptors(Class<?> src, Class<?> target)
    {
        Asserts.assertNotNull(src, "src");
        Asserts.assertNotNull(target, "target");

        int p1 = priorityInterceptors.getSorted().indexOf(src);
        int p2 = priorityInterceptors.getSorted().indexOf(target);

        int srcIndex = p1;
        if (srcIndex == -1)
        {
            int i = configuredInterceptorClasses.indexOf(src);
            if (i == -1)
            {
                throw new IllegalArgumentException(src.getName() + " is not an enabled interceptor!");
            }
            srcIndex = priorityInterceptors.getSorted().size() + i;
        }

        int targetIndex = p2;
        if (targetIndex == -1)
        {
            int i = configuredInterceptorClasses.indexOf(target);
            if (i == -1)
            {
                throw new IllegalArgumentException(target.getName() + " is not an enabled interceptor!");
            }
            targetIndex = priorityInterceptors.getSorted().size() + i;
        }

        if (srcIndex == -1 && targetIndex != -1)
        {
            return -1;
        }
        if (srcIndex != -1 && targetIndex == -1)
        {
            return 1;
        }
        return srcIndex - targetIndex;
    }

    /**
     * Check if the given interceptor class is in the list of enabled interceptors.
     */
    public boolean isInterceptorClassEnabled(Class<?> interceptorClazz)
    {
        Asserts.nullCheckForClass(interceptorClazz, "interceptorClazz can not be null");

        return configuredInterceptorClasses.contains(interceptorClazz)
                || priorityInterceptors.contains(interceptorClazz);
    }

    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        List<Interceptor<?>> interceptorList = new ArrayList<>();
        for (Interceptor<?> interceptor : cdiInterceptors)
        {
            if (interceptor.intercepts(type) && intercepts(interceptor, interceptorBindings) && isInterceptorClassEnabled(interceptor.getBeanClass()))
            {
                interceptorList.add(interceptor);
            }
        }

        interceptorList.sort(new InterceptorComparator(webBeansContext));

        return interceptorList;
    }

    private boolean intercepts(Interceptor<?> interceptor, Annotation[] requestedInterceptorBindings)
    {
        for (Annotation interceptorBinding : interceptor.getInterceptorBindings())
        {
            // if an interceptor has multiple bindings then all of them must be in the
            // requestedInterceptorBindings for a positive match

            // first check AT since it can override some methods (@NonBinding)
            boolean found = false;
            for (AnnotatedType<?> at : additionalInterceptorBindingTypesAnnotatedTypes)
            {
                if (interceptorBinding.annotationType().equals(at.getJavaClass()))
                {
                    found = true;
                    if (!inBindingArray(at, interceptorBinding, requestedInterceptorBindings))
                    {
                        return false;
                    }
                }
            }
            if (!found)
            {
                if (!inBindingArray(interceptorBinding, requestedInterceptorBindings))
                {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean inBindingArray(AnnotatedType<?> at, Annotation interceptorBinding, Annotation[] requestedInterceptorBindings)
    {
        for (Annotation requestedBinding : requestedInterceptorBindings)
        {
            if (AnnotationUtil.isCdiAnnotationEqual(at, requestedBinding, interceptorBinding))
            {
                return true;
            }
        }
        return false;
    }

    private boolean inBindingArray(Annotation interceptorBinding, Annotation[] requestedInterceptorBindings)
    {
        for (Annotation requestedBinding : requestedInterceptorBindings)
        {
            if (AnnotationUtil.isCdiAnnotationEqual(requestedBinding, interceptorBinding))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a CDI-style interceptor.
     * These are interceptors declared using an {@link javax.interceptor.InterceptorBinding}.
     * @param interceptor
     */
    public void addCdiInterceptor(Interceptor interceptor)
    {
        cdiInterceptors.add(interceptor);
        beanManager.addPassivationInfo(interceptor);
    }


    public List<Interceptor<?>> getCdiInterceptors()
    {
        return cdiInterceptors;
    }

    public void addCustomInterceptorClass(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        additionalInterceptorClasses.add(clazz);
    }

    public boolean containsCustomInterceptorClass(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        return additionalInterceptorClasses.contains(clazz);
    }

    public void addInterceptorBindingType(AnnotatedType<? extends Annotation> annotatedType)
    {
        additionalInterceptorBindingTypesAnnotatedTypes.add(annotatedType);
    }

    public void addInterceptorBindingType(Class<? extends Annotation> bindingType, Annotation... inheritsArray)
    {
        Set<Annotation> inherits = additionalInterceptorBindingTypes.computeIfAbsent(bindingType, k -> new HashSet<>());
        Collections.addAll(inherits, inheritsArray);
    }

    public boolean hasInterceptorBindingType(Class<? extends Annotation> bindingType)
    {
        boolean contains = additionalInterceptorBindingTypes.keySet().contains(bindingType);
        if (contains)
        {
            return true;
        }
        for (AnnotatedType<?> at : additionalInterceptorBindingTypesAnnotatedTypes)
        {
            if (bindingType.equals(at.getJavaClass()))
            {
                return true;
            }
        }
        return false;
    }


    public void validateInterceptorClasses()
    {
        for(Class<?> interceptorClass : configuredInterceptorClasses)
        {
            AnnotatedType<?> annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(interceptorClass);

            // Validate decorator classes
            if(!annotatedType.isAnnotationPresent(javax.interceptor.Interceptor.class) &&
               !containsCustomInterceptorClass(interceptorClass))
            {
                throw new WebBeansDeploymentException("Given class : " + interceptorClass + " is not a interceptor class");
            }   
        }
    }

    public int getPriority(final Class<?> type)
    {
        return priorityInterceptors.getPriority(type).orElseGet(() -> getPrioritizedInterceptors().indexOf(type));
    }

    public List<Class<?>> getPrioritizedInterceptors()
    {
        return priorityInterceptors.getSorted();
    }

    public void addPriorityClazzInterceptor(Class<?> javaClass, int priority)
    {
        priorityInterceptors.add(javaClass, priority);
    }
}
