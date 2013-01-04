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
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
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
     * Active interceptors
     */
    private List<Interceptor<?>> webBeansInterceptors = new ArrayList<Interceptor<?>>();

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
    }


    /**
     * Add a certain class to the enabled interceptors list.
     */
    public void addNewInterceptorClass(Class<?> interceptorClazz)
    {
        Asserts.nullCheckForClass(interceptorClazz, "interceptorClazz can not be null");

        if (!configuredInterceptorClasses.contains(interceptorClazz))
        {
            configuredInterceptorClasses.add(interceptorClazz);
        }
    }

    /**
     * Helper to compare the order of different interceptor classes
     */
    public int compare(Class<?> src, Class<?> target)
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


    public void addInterceptor(Interceptor interceptor)
    {
        webBeansInterceptors.add(interceptor);
        if (interceptor instanceof OwbBean)
        {
            OwbBean<?> owbBean = (OwbBean<?>)interceptor;
            if(owbBean.isPassivationCapable())
            {
                beanManager.addPassivationInfo((OwbBean) interceptor);
            }

        }
    }


    public List<Interceptor<?>> getInterceptors()
    {
        return webBeansInterceptors;
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


    public Set<Annotation> getInterceptorBindingTypeMetaAnnotations(Class<? extends Annotation> interceptorBindingType)
    {
        return Collections.unmodifiableSet(additionalInterceptorBindingTypes.get(interceptorBindingType));
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
