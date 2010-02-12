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
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.AroundInvoke;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
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
    public static <T> void configureInterceptorClass(AbstractInjectionTargetBean<T> delegate, Annotation[] interceptorBindingTypes)
    {
        logger.info(OWBLogConst.INFO_0011, new Object[]{logger.getTokenString(OWBLogConst.TEXT_INTERCEPT_CLASS), delegate.getReturnType()});

        WebBeansInterceptor<T> interceptor = new WebBeansInterceptor<T>(delegate);

        for (Annotation ann : interceptorBindingTypes)
        {
            interceptor.addInterceptorBinding(ann.annotationType(), ann);
        }

        logger.info(OWBLogConst.INFO_0012, new Object[]{logger.getTokenString(OWBLogConst.TEXT_INTERCEPT_CLASS), delegate.getReturnType()});

        BeanManagerImpl.getManager().addInterceptor(interceptor);

    }

    /**
     * Configures the given class for applicable interceptors.
     * 
     * @param clazz configuration interceptors for this
     */
    public static void configure(AbstractInjectionTargetBean<?> component, List<InterceptorData> stack)
    {
        Class<?> clazz = ((AbstractBean<?>)component).getReturnType();
        AnnotatedType<?> annotatedType = component.getAnnotatedType();
        Set<Annotation> annotations = null;
        
        if(annotatedType != null)
        {
            annotations = annotatedType.getAnnotations();
        }
        
        Set<Interceptor<?>> componentInterceptors = null;

        Set<Annotation> bindingTypeSet = new HashSet<Annotation>();
        Annotation[] anns = new Annotation[0];
        Annotation[] typeAnns = null;
        if(annotations != null)
        {
            typeAnns = annotations.toArray(new Annotation[0]);            
        }
        else
        {
            typeAnns = clazz.getDeclaredAnnotations();
        }
        if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(typeAnns))
        {
            anns = AnnotationUtil.getInterceptorBindingMetaAnnotations(typeAnns);

            for (Annotation ann : anns)
            {
                bindingTypeSet.add(ann);
            }
        }
        
        //check for stereotypes
        Annotation[] stereoTypes = AnnotationUtil.getStereotypeMetaAnnotations(typeAnns);
        for (Annotation stero : stereoTypes)
        {
            if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(stero.annotationType().getDeclaredAnnotations()))
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
            Set<Annotation> inheritedBindingTypes = metadata.getInheritedInterceptorBindings();
            if(!inheritedBindingTypes.isEmpty())
            {
                bindingTypeSet.addAll(inheritedBindingTypes);   
            }
        }

        anns = new Annotation[bindingTypeSet.size()];
        anns = bindingTypeSet.toArray(anns);
        
        if(anns.length > 0)
        {
            componentInterceptors = findDeployedWebBeansInterceptor(anns);

            // Adding class interceptors
            addComponentInterceptors(componentInterceptors, stack);            
        }
                

        // Method level interceptors.
        if(annotatedType == null)
        {
            addMethodInterceptors(clazz, stack, componentInterceptors);   
        }
        else
        {
            addMethodInterceptors(annotatedType, stack, componentInterceptors);            
        }
        
        Collections.sort(stack, new InterceptorDataComparator());

    }

    public static void addComponentInterceptors(Set<Interceptor<?>> set, List<InterceptorData> stack)
    {
        Iterator<Interceptor<?>> it = set.iterator();
        while (it.hasNext())
        {
            WebBeansInterceptor<?> interceptor = (WebBeansInterceptor<?>) it.next();
            
            AnnotatedType<?> annotatedType = null;
            if((annotatedType = interceptor.getAnnotatedType()) != null)
            {
                // interceptor binding
                WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, AroundInvoke.class, true, false, stack, null, true);
                WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, PostConstruct.class, true, false, stack, null, true);
                WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, PreDestroy.class, true, false, stack, null, true);                
            }
            else
            {
                // interceptor binding
                WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), AroundInvoke.class, true, false, stack, null, true);
                WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PostConstruct.class, true, false, stack, null, true);
                WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PreDestroy.class, true, false, stack, null, true);                
            }
        }

    }

    private static void addMethodInterceptors(Class<?> clazz, List<InterceptorData> stack, Set<Interceptor<?>> componentInterceptors)
    {
        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods)
        {
            Set<Annotation> interceptorAnns = new HashSet<Annotation>();

            if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(method.getAnnotations()))
            {                
                Annotation[] anns = AnnotationUtil.getInterceptorBindingMetaAnnotations(method.getDeclaredAnnotations());
                Annotation[] annsClazz = AnnotationUtil.getInterceptorBindingMetaAnnotations(clazz.getDeclaredAnnotations());

                for (Annotation ann : anns)
                {
                    interceptorAnns.add(ann);
                }

                for (Annotation ann : annsClazz)
                {
                    interceptorAnns.add(ann);
                }
            }

            Annotation[] stereoTypes = AnnotationUtil.getStereotypeMetaAnnotations(clazz.getDeclaredAnnotations());
            for (Annotation stero : stereoTypes)
            {
                if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(stero.annotationType().getDeclaredAnnotations()))
                {
                    Annotation[] steroInterceptorBindings = AnnotationUtil.getInterceptorBindingMetaAnnotations(stero.annotationType().getDeclaredAnnotations());

                    for (Annotation ann : steroInterceptorBindings)
                    {
                        interceptorAnns.add(ann);
                    }
                }
            }

            if (!interceptorAnns.isEmpty())
            {
                Annotation[] result = new Annotation[interceptorAnns.size()];
                result = interceptorAnns.toArray(result);
    
                Set<Interceptor<?>> setInterceptors = findDeployedWebBeansInterceptor(result);
                
                if(componentInterceptors != null)
                {
                    setInterceptors.removeAll(componentInterceptors);   
                }
    
                Iterator<Interceptor<?>> it = setInterceptors.iterator();
    
                while (it.hasNext())
                {
                    WebBeansInterceptor<?> interceptor = (WebBeansInterceptor<?>) it.next();
    
                    WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), AroundInvoke.class, true, true, stack, method, true);
                    WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PostConstruct.class, true, true, stack, method, true);
                    WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PreDestroy.class, true, true, stack, method, true);
                }
            }
        }

    }
    
    @SuppressWarnings("unchecked")
    private static <T> void addMethodInterceptors(AnnotatedType<T> annotatedType, List<InterceptorData> stack, Set<Interceptor<?>> componentInterceptors)
    {

        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for(AnnotatedMethod<? super T> methodA : methods)
        {
            AnnotatedMethod<T> methodB = (AnnotatedMethod<T>)methodA;
            Method method = methodB.getJavaMember();
            Set<Annotation> interceptorAnns = new HashSet<Annotation>();
            
            Annotation[] methodAnns = AnnotationUtil.getAnnotationsFromSet(methodB.getAnnotations());
            if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(methodAnns))
            {                
                Annotation[] anns = AnnotationUtil.getInterceptorBindingMetaAnnotations(methodAnns);
                Annotation[] annsClazz = AnnotationUtil.getInterceptorBindingMetaAnnotations(AnnotationUtil.getAnnotationsFromSet(annotatedType.getAnnotations()));

                for (Annotation ann : anns)
                {
                    interceptorAnns.add(ann);
                }

                for (Annotation ann : annsClazz)
                {
                    interceptorAnns.add(ann);
                }
            }

            Annotation[] stereoTypes = AnnotationUtil.getStereotypeMetaAnnotations(AnnotationUtil.getAnnotationsFromSet(annotatedType.getAnnotations()));
            for (Annotation stero : stereoTypes)
            {
                if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(stero.annotationType().getDeclaredAnnotations()))
                {
                    Annotation[] steroInterceptorBindings = AnnotationUtil.getInterceptorBindingMetaAnnotations(stero.annotationType().getDeclaredAnnotations());

                    for (Annotation ann : steroInterceptorBindings)
                    {
                        interceptorAnns.add(ann);
                    }
                }
            }

            if (!interceptorAnns.isEmpty())
            {
                Annotation[] result = new Annotation[interceptorAnns.size()];
                result = interceptorAnns.toArray(result);
    
                Set<Interceptor<?>> setInterceptors = findDeployedWebBeansInterceptor(result);
                
                if(componentInterceptors != null)
                {
                    setInterceptors.removeAll(componentInterceptors);   
                }
    
                Iterator<Interceptor<?>> it = setInterceptors.iterator();
    
                while (it.hasNext())
                {
                    WebBeansInterceptor<?> interceptor = (WebBeansInterceptor<?>) it.next();
    
                    WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, AroundInvoke.class, true, true, stack, method, true);
                    WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, PostConstruct.class, true, true, stack, method, true);
                    WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, PreDestroy.class, true, true, stack, method, true);
                }
            }            
        }
        
    }    

    /**
     * Gets the configured webbeans interceptors.
     * 
     * @return the configured webbeans interceptors
     */
    private static Set<Interceptor<?>> getWebBeansInterceptors()
    {
        return Collections.unmodifiableSet(BeanManagerImpl.getManager().getInterceptors());
    }

    /*
     * Find the deployed interceptors with given interceptor binding types.
     */
    public static Set<Interceptor<?>> findDeployedWebBeansInterceptor(Annotation[] anns)
    {
        Set<Interceptor<?>> set = new HashSet<Interceptor<?>>();

        Iterator<Interceptor<?>> it = getWebBeansInterceptors().iterator();
        WebBeansInterceptor<?> interceptor = null;

        List<Class<? extends Annotation>> bindingTypes = new ArrayList<Class<? extends Annotation>>();
        List<Annotation> listAnnot = new ArrayList<Annotation>();
        for (Annotation ann : anns)
        {
            bindingTypes.add(ann.annotationType());
            listAnnot.add(ann);
        }

        while (it.hasNext())
        {
            interceptor = (WebBeansInterceptor<?>) it.next();
      
            if (interceptor.hasBinding(bindingTypes, listAnnot))
            {
                set.add(interceptor);
                set.addAll(interceptor.getMetaInceptors());
            }
        }
        
        return set;
    }
}
