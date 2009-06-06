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
package org.apache.webbeans.intercept;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.AroundInvoke;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.Component;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Configures the Web Beans related interceptors.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 * @see WebBeansInterceptor
 */
public final class WebBeansInterceptorConfig
{
    /** Logger instance */
    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansInterceptorConfig.class);

    /*
     * Private
     */
    private WebBeansInterceptorConfig()
    {

    }

    /**
     * Configures WebBeans specific interceptor class.
     * 
     * @param interceptorClazz interceptor class
     */
    public static void configureInterceptorClass(AbstractComponent<Object> delegate, Annotation[] interceptorBindingTypes)
    {
        logger.info("Configuring the Web Beans Interceptor Class : " + delegate.getReturnType().getName() + " started");

        WebBeansInterceptor interceptor = new WebBeansInterceptor(delegate);

        for (Annotation ann : interceptorBindingTypes)
        {
            interceptor.addInterceptorBindingType(ann.annotationType(), ann);
        }

        logger.info("Configuring the Web Beans Interceptor Class : " + delegate.getReturnType() + " ended");

        ManagerImpl.getManager().addInterceptor(interceptor);

    }

    /**
     * Configures the given class for applicable interceptors.
     * 
     * @param clazz configuration interceptors for this
     */
    public static void configure(Component<?> component, List<InterceptorData> stack)
    {
        Class<?> clazz = component.getReturnType();
        
        if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(clazz.getDeclaredAnnotations()))
        {

            Set<Annotation> bindingTypeSet = new HashSet<Annotation>();

            Annotation[] anns = AnnotationUtil.getInterceptorBindingMetaAnnotations(clazz.getDeclaredAnnotations());

            for (Annotation ann : anns)
            {
                bindingTypeSet.add(ann);
            }

            Annotation[] stereoTypes = AnnotationUtil.getStereotypeMetaAnnotations(clazz.getDeclaredAnnotations());
            for (Annotation stero : stereoTypes)
            {
                if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(stero.annotationType().getDeclaredAnnotations()))
                {
                    Annotation[] steroInterceptorBindings = AnnotationUtil.getInterceptorBindingMetaAnnotations(stero.annotationType().getDeclaredAnnotations());

                    for (Annotation ann : steroInterceptorBindings)
                    {
                        bindingTypeSet.add(ann);
                    }
                }
            }
            
            //Look for inherited binding types
            IBeanInheritedMetaData metadata = component.getInheritedMetaData();
            if(metadata != null)
            {
                Set<Annotation> inheritedBindingTypes = metadata.getInheritedInterceptorBindingTypes();
                if(!inheritedBindingTypes.isEmpty())
                {
                    bindingTypeSet.addAll(inheritedBindingTypes);   
                }
            }

            anns = new Annotation[bindingTypeSet.size()];
            anns = bindingTypeSet.toArray(anns);

            Set<Interceptor> set = findDeployedWebBeansInterceptor(anns);

            // Adding class interceptors
            addComponentInterceptors(set, stack);
        }

        // Method level interceptors.
        addMethodInterceptors(clazz, stack);

    }

    public static void addComponentInterceptors(Set<Interceptor> set, List<InterceptorData> stack)
    {
        Iterator<Interceptor> it = set.iterator();
        while (it.hasNext())
        {
            WebBeansInterceptor interceptor = (WebBeansInterceptor) it.next();

            // interceptor binding
            WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), AroundInvoke.class, true, false, stack, null, true);
            WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PostConstruct.class, true, false, stack, null, true);
            WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PreDestroy.class, true, false, stack, null, true);

        }

    }

    private static void addMethodInterceptors(Class<?> clazz, List<InterceptorData> stack)
    {
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods)
        {
            if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(method.getAnnotations()))
            {
                Annotation[] anns = AnnotationUtil.getInterceptorBindingMetaAnnotations(method.getDeclaredAnnotations());
                Annotation[] annsClazz = AnnotationUtil.getInterceptorBindingMetaAnnotations(clazz.getDeclaredAnnotations());

                Set<Annotation> set = new HashSet<Annotation>();

                for (Annotation ann : anns)
                {
                    set.add(ann);
                }

                for (Annotation ann : annsClazz)
                {
                    set.add(ann);
                }

                Annotation[] stereoTypes = AnnotationUtil.getStereotypeMetaAnnotations(clazz.getDeclaredAnnotations());
                for (Annotation stero : stereoTypes)
                {
                    if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(stero.annotationType().getDeclaredAnnotations()))
                    {
                        Annotation[] steroInterceptorBindings = AnnotationUtil.getInterceptorBindingMetaAnnotations(stero.annotationType().getDeclaredAnnotations());

                        for (Annotation ann : steroInterceptorBindings)
                        {
                            set.add(ann);
                        }
                    }
                }

                Annotation[] result = new Annotation[set.size()];
                result = set.toArray(result);

                Set<Interceptor> setInterceptors = findDeployedWebBeansInterceptor(result);
                Iterator<Interceptor> it = setInterceptors.iterator();

                while (it.hasNext())
                {
                    WebBeansInterceptor interceptor = (WebBeansInterceptor) it.next();

                    WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), AroundInvoke.class, true, true, stack, method, true);
                    WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PostConstruct.class, true, true, stack, method, true);
                    WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PreDestroy.class, true, true, stack, method, true);
                }

            }
        }

    }

    /**
     * Gets the configured webbeans interceptors.
     * 
     * @return the configured webbeans interceptors
     */
    private static Set<Interceptor> getWebBeansInterceptors()
    {
        return Collections.unmodifiableSet(ManagerImpl.getManager().getInterceptors());
    }

    /*
     * Find the deployed interceptors with given interceptor binding types.
     */
    public static Set<Interceptor> findDeployedWebBeansInterceptor(Annotation[] anns)
    {
        Set<Interceptor> set = new HashSet<Interceptor>();

        Iterator<Interceptor> it = getWebBeansInterceptors().iterator();
        WebBeansInterceptor interceptor = null;

        List<Class<? extends Annotation>> bindingTypes = new ArrayList<Class<? extends Annotation>>();
        List<Annotation> listAnnot = new ArrayList<Annotation>();
        for (Annotation ann : anns)
        {
            bindingTypes.add(ann.annotationType());
            listAnnot.add(ann);
        }

        while (it.hasNext())
        {
            interceptor = (WebBeansInterceptor) it.next();

            if (interceptor.isBindingTypesExist(bindingTypes, listAnnot))
            {
                set.add(interceptor);
                set.addAll(interceptor.getMetaInceptors());
            }
        }

        return set;
    }
}
