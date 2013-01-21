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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.PassivationCapable;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.creation.EjbInterceptorBeanBuilder;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

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
    private List<Class<?>> configuredInterceptorClasses = new CopyOnWriteArrayList<Class<?>>();

    /**
     * Active CDI-style interceptors.
     */
    private List<Interceptor<?>> cdiInterceptors = new ArrayList<Interceptor<?>>();

    /**
     * EJB-style Interceptor beans.
     */
    private ConcurrentHashMap<Class<?>, Interceptor<?>> ejbInterceptors = new ConcurrentHashMap<Class<?>, Interceptor<?>>();

    /**Additional interceptor class*/
    private List<Class<?>> additionalInterceptorClasses = new ArrayList<Class<?>>();

    /**
     * Additional interceptor binding types we got via Extensions
     */
    private Map<Class<? extends Annotation>, Set<Annotation>> additionalInterceptorBindingTypes
            = new HashMap<Class<? extends Annotation>, Set<Annotation>>();


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
            AnnotatedType<T> annotatedType = webBeansContext.getBeanManagerImpl().createAnnotatedType(interceptorClass);
            EjbInterceptorBeanBuilder<T> buildr = new EjbInterceptorBeanBuilder<T>(webBeansContext, annotatedType);
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
        Asserts.assertNotNull(src, "src parameter can not be  null");
        Asserts.assertNotNull(target, "target parameter can not be null");

        int srcIndex = configuredInterceptorClasses.indexOf(src);
        if (srcIndex == -1)
        {
            throw new IllegalArgumentException(src.getName() + " is not an enabled interceptor!");
        }

        int targetIndex = configuredInterceptorClasses.indexOf(target);
        if (targetIndex == -1)
        {
            throw new IllegalArgumentException(target.getName() + " is not an enabled interceptor!");
        }


        if (srcIndex == targetIndex)
        {
            return 0;
        }
        else if (srcIndex < targetIndex)
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }

    /**
     * Check if the given interceptor class is in the list of enabled interceptors.
     */
    public boolean isInterceptorClassEnabled(Class<?> interceptorClazz)
    {
        Asserts.nullCheckForClass(interceptorClazz, "interceptorClazz can not be null");

        return configuredInterceptorClasses.contains(interceptorClazz);
    }

    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        List<Interceptor<?>> interceptorList = new ArrayList<Interceptor<?>>();
        for (Interceptor<?> interceptor : cdiInterceptors)
        {
            if (interceptor.intercepts(type) && intercepts(interceptor, interceptorBindings))
            {
                interceptorList.add(interceptor);
            }
        }

        Collections.sort(interceptorList, new InterceptorComparator(webBeansContext));

        return interceptorList;
    }

    private boolean intercepts(Interceptor<?> interceptor, Annotation[] requestedInterceptorBindings)
    {
        for (Annotation interceptorBinding : interceptor.getInterceptorBindings())
        {
            // if an interceptor has multiple bindings then all of them must be in the
            // requestedInterceptorBindings for a positive match

            if (!inBindingArray(interceptorBinding, requestedInterceptorBindings))
            {
                return false;
            }

        }

        return true;
    }

    private boolean inBindingArray(Annotation interceptorBinding, Annotation[] requestedInterceptorBindings)
    {
        for (Annotation requestedBinding : requestedInterceptorBindings)
        {
            if (AnnotationUtil.isQualifierEqual(requestedBinding, interceptorBinding))
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
        if (interceptor instanceof PassivationCapable)
        {
            OwbBean<?> owbBean = (OwbBean<?>)interceptor;
            if(owbBean.isPassivationCapable())
            {
                beanManager.addPassivationInfo((OwbBean) interceptor);
            }

        }
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

    public void addInterceptorBindingType(Class<? extends Annotation> bindingType, Annotation... inheritsArray)
    {
        Set<Annotation> inherits = additionalInterceptorBindingTypes.get(bindingType);
        if (inherits == null)
        {
            inherits = new HashSet<Annotation>();
            additionalInterceptorBindingTypes.put(bindingType, inherits);
        }
        for(Annotation ann : inheritsArray)
        {
            inherits.add(ann);
        }

    }

    public boolean hasInterceptorBindingType(Class<? extends Annotation> bindingType)
    {
        return additionalInterceptorBindingTypes.keySet().contains(bindingType);
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
                throw new WebBeansConfigurationException("Given class : " + interceptorClass + " is not a interceptor class");
            }   
        }                
    }    
}
