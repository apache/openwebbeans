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

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.OpenWebBeansEjbLCAPlugin;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.SecurityUtil;
import org.apache.webbeans.util.WebBeansUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.AroundInvoke;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Configures the Web Beans related interceptors.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @version $Rev$ $Date$
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
        if(delegate.getScope() != Dependent.class)
        {
            if(logger.wblWillLogWarn())
            {
                logger.warn(OWBLogConst.WARN_0005_1, delegate.getBeanClass().getName());
            }
        }
        
        if(delegate.getName() != null)
        {
            if(logger.wblWillLogWarn())
            {
                logger.warn(OWBLogConst.WARN_0005_2, delegate.getBeanClass().getName());
            }   
        }   
        
        if(delegate.isAlternative())
        {
            if(logger.wblWillLogWarn())
            {
                logger.warn(OWBLogConst.WARN_0005_3, delegate.getBeanClass().getName());
            }                
        }        
        
        logger.debug("Configuring interceptor class : [{0}]", delegate.getReturnType());
        WebBeansInterceptor<T> interceptor = new WebBeansInterceptor<T>(delegate);

        List<Annotation> anns = Arrays.asList(interceptorBindingTypes);
        
        for (Annotation ann : interceptorBindingTypes)
        {
            checkAnns(anns, ann, delegate);
            interceptor.addInterceptorBinding(ann.annotationType(), ann);
        }


        WebBeansContext.getInstance().getBeanManagerImpl().addInterceptor(interceptor);

    }
    
    private static void checkAnns(List<Annotation> list, Annotation ann, Bean<?> bean)
    {
        for(Annotation old : list)
        {
            if(old.annotationType().equals(ann.annotationType()))
            {
                if(!AnnotationUtil.isQualifierEqual(ann, old))
                {
                    throw new WebBeansConfigurationException("Interceptor Binding types must be equal for interceptor : " + bean);
                }                
            }
        }
    }

    /**
     * Configures the given class for applicable interceptors.
     * 
     * @param clazz configuration interceptors for this
     */
    public static void configure(AbstractInjectionTargetBean<?> component, List<InterceptorData> stack)
    {
        Class<?> clazz = ((AbstractOwbBean<?>)component).getReturnType();
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
        AnnotationManager annotationManager = WebBeansContext.getInstance().getAnnotationManager();
        if (annotationManager.hasInterceptorBindingMetaAnnotation(typeAnns))
        {
            anns = annotationManager.getInterceptorBindingMetaAnnotations(typeAnns);

            for (Annotation ann : anns)
            {
                bindingTypeSet.add(ann);
            }
        }

        // check for stereotypes _explicitly_ declared on the bean class (not
        // inherited)
        Annotation[] stereoTypes =
            annotationManager.getStereotypeMetaAnnotations(typeAnns);
        for (Annotation stero : stereoTypes)
        {
            if (annotationManager.hasInterceptorBindingMetaAnnotation(stero.annotationType().getDeclaredAnnotations()))
            {
                Annotation[] steroInterceptorBindings = annotationManager.getInterceptorBindingMetaAnnotations(
                        stero.annotationType().getDeclaredAnnotations());

                for (Annotation ann : steroInterceptorBindings)
                {
                    bindingTypeSet.add(ann);
                }
            }
        }

        // Look for inherited binding types, keeping in mind that
        // IBeanInheritedMetaData knows nothing of the transitive
        // relationships of Interceptor Bindings or Stereotypes. We must resolve
        // these here.
        IBeanInheritedMetaData metadata = component.getInheritedMetaData();
        if (metadata != null)
        {
            Set<Annotation> inheritedBindingTypes = metadata.getInheritedInterceptorBindings();
            if (!inheritedBindingTypes.isEmpty())
            {
                Annotation[] inheritedAnns = new Annotation[inheritedBindingTypes.size()];
                inheritedAnns = inheritedBindingTypes.toArray(inheritedAnns);
                anns = annotationManager.getInterceptorBindingMetaAnnotations(inheritedAnns);
                bindingTypeSet.addAll(Arrays.asList(anns));
            }

            // Retrieve inherited stereotypes, check for meta-annotations, and
            // find the ultimate set of bindings
            Set<Annotation> inheritedStereotypes = metadata.getInheritedStereoTypes();

            if (!inheritedStereotypes.isEmpty())
            {
                // We need AnnotationUtil to resolve the transitive relationship
                // of stereotypes we've found
                Annotation[] inherited = new Annotation[inheritedStereotypes.size()];
                inherited = inheritedStereotypes.toArray(inherited);
                Annotation[] transitiveStereotypes = annotationManager.getStereotypeMetaAnnotations(inherited);

                for (Annotation stereo : transitiveStereotypes)
                {
                    if (annotationManager.hasInterceptorBindingMetaAnnotation(stereo.annotationType().getDeclaredAnnotations()))
                    {
                        Annotation[] steroInterceptorBindings =
                            annotationManager.getInterceptorBindingMetaAnnotations(stereo.annotationType().getDeclaredAnnotations());
                        for (Annotation ann : steroInterceptorBindings)
                        {
                            bindingTypeSet.add(ann);
                        }
                    }
                }
            }
        }
        
        anns = new Annotation[bindingTypeSet.size()];
        anns = bindingTypeSet.toArray(anns);
        
        //Spec Section 9.5.2
        List<Annotation> beanAnnots = Arrays.asList(anns);
        for(Annotation checkAnn : anns)
        {
            checkAnns(beanAnnots, checkAnn, component);
        }

        if (anns.length > 0)
        {
            componentInterceptors = findDeployedWebBeansInterceptor(anns);

            // Adding class interceptors
            addComponentInterceptors(componentInterceptors, stack);
        }

        // Method level interceptors.
        if(annotatedType == null)
        {
            addMethodInterceptors(clazz, stack, componentInterceptors, bindingTypeSet);   
        }
        else
        {
            addMethodInterceptors(annotatedType, stack, componentInterceptors);
        }
        filterInterceptorsPerBDA(component,stack);
        
        Collections.sort(stack, new InterceptorDataComparator());

    }

    private static void filterInterceptorsPerBDA(AbstractInjectionTargetBean<?> component, List<InterceptorData> stack)
    {

        ScannerService scannerService = component.getWebBeansContext().getScannerService();
        if (!scannerService.isBDABeansXmlScanningEnabled())
        {
            return;
        }
        BDABeansXmlScanner beansXMLScanner = scannerService.getBDABeansXmlScanner();
        String beanBDABeansXML = beansXMLScanner.getBeansXml(component.getBeanClass());
        Set<Class<?>> definedInterceptors = beansXMLScanner.getInterceptors(beanBDABeansXML);

        InterceptorData interceptorData;

        if (stack != null && stack.size() > 0)
        {
            Iterator<InterceptorData> it = stack.iterator();
            while (it.hasNext())
            {
                interceptorData = (InterceptorData) it.next();
                if (!definedInterceptors.contains(interceptorData.getInterceptorClass()))
                {
                    it.remove();
                }
            }
        }

    }

    public static void addComponentInterceptors(Set<Interceptor<?>> set, List<InterceptorData> stack)
    {
        Iterator<Interceptor<?>> it = set.iterator();
        while (it.hasNext())
        {
            WebBeansInterceptor<?> interceptor = (WebBeansInterceptor<?>) it.next();
            AnnotatedType<?> annotatedType = interceptor.getAnnotatedType();

            OpenWebBeansEjbLCAPlugin ejbPlugin = WebBeansContext.getInstance().getPluginLoader().getEjbLCAPlugin();
            Class <? extends Annotation> prePassivateClass = null;
            Class <? extends Annotation> postActivateClass = null;
            Class <? extends Annotation> aroundTimeoutClass = null;
            if (null != ejbPlugin)
            {
                prePassivateClass = ejbPlugin.getPrePassivateClass();
                postActivateClass = ejbPlugin.getPostActivateClass();
                aroundTimeoutClass = ejbPlugin.getAroundTimeoutClass();
            }
            
            if(annotatedType != null)
            {
                // interceptor binding
                WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, AroundInvoke.class, true, false, stack, null);
                WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, PostConstruct.class, true, false, stack, null);
                WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, PreDestroy.class, true, false, stack, null);   
                
                if (null != ejbPlugin)
                {
                    WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, prePassivateClass, true, false, stack, null);                
                    WebBeansUtil.configureInterceptorMethods(interceptor, annotatedType, postActivateClass, true, false, stack, null);       
                    
                }                
            }
            else
            {
                // interceptor binding
                WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), AroundInvoke.class, true, false, stack, null, true);
                WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PostConstruct.class, true, false, stack, null, true);
                WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PreDestroy.class, true, false, stack, null, true);
                
                if (null != ejbPlugin) 
                {
                    WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), prePassivateClass, true, false, stack, null, true);                
                    WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), postActivateClass, true, false, stack, null, true);       
                    
                }                

            }
        }

    }

    /**
     * Add configured interceptors, combining the bindings at the component-level with annotations on methods
     * @param clazz the bean class
     * @param stack the current interceptor stack for the bean
     * @param componentInterceptors the configured interceptors from the component level
     * @param resolvedComponentInterceptorBindings complete (including transitive) set of component-level interceptor bindings
     */
    private static void addMethodInterceptors(Class<?> clazz, List<InterceptorData> stack, Set<Interceptor<?>> componentInterceptors,
                                              Set<Annotation> resolvedComponentInterceptorBindings)
    {
        AnnotationManager annotationManager = WebBeansContext.getInstance().getAnnotationManager();

        // All methods, not just those declared
        Method[] methods = clazz.getMethods();
        Set<Method> set = new HashSet<Method>();
        for(Method m : methods)
        {
            set.add(m);
        }
        
        //GE : I added for private, protected etc. methods.
        //Not just for public methods.
        methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);
        for(Method m : methods)
        {
            set.add(m);
        }
        
        methods = set.toArray(new Method[0]);

        for (Method method : methods)
        {
            Set<Annotation> interceptorAnns = new HashSet<Annotation>();
            if (annotationManager.hasInterceptorBindingMetaAnnotation(
                method.getDeclaredAnnotations()))
            {
                Annotation[] anns =
                    annotationManager.getInterceptorBindingMetaAnnotations(
                        method.getAnnotations());
                for (Annotation ann : anns)
                {
                    interceptorAnns.add(ann);
                }
            }

            // To find the right interceptors, we need to consider method and
            // class-level combined
            interceptorAnns.addAll(resolvedComponentInterceptorBindings);

            if (!interceptorAnns.isEmpty())
            {
                Annotation[] result = new Annotation[interceptorAnns.size()];
                result = interceptorAnns.toArray(result);

                Set<Interceptor<?>> setInterceptors = findDeployedWebBeansInterceptor(result);

                if (componentInterceptors != null)
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

                    OpenWebBeansEjbLCAPlugin ejbPlugin = WebBeansContext.getInstance().getPluginLoader().getEjbLCAPlugin();
                    if (null != ejbPlugin)
                    {
                        WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), ejbPlugin.getPrePassivateClass(), true, true, stack, method, true);
                        WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), ejbPlugin.getPostActivateClass(), true, true, stack, method, true);
                    }
            
                    
                }
            }
        }

    }
    
    @SuppressWarnings("unchecked")
    private static <T> void addMethodInterceptors(AnnotatedType<T> annotatedType, List<InterceptorData> stack, Set<Interceptor<?>> componentInterceptors)
    {

        AnnotationManager annotationManager = WebBeansContext.getInstance().getAnnotationManager();
        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for(AnnotatedMethod<? super T> methodA : methods)
        {
            AnnotatedMethod<T> methodB = (AnnotatedMethod<T>)methodA;
            Method method = methodB.getJavaMember();
            Set<Annotation> interceptorAnns = new HashSet<Annotation>();
            
            Annotation[] methodAnns = AnnotationUtil.getAnnotationsFromSet(methodB.getAnnotations());
            if (annotationManager.hasInterceptorBindingMetaAnnotation(methodAnns))
            {
                Annotation[] anns =
                    annotationManager.getInterceptorBindingMetaAnnotations(
                        methodAnns);
                Annotation[] annsClazz =
                    annotationManager.getInterceptorBindingMetaAnnotations(
                        AnnotationUtil.getAnnotationsFromSet(annotatedType.getAnnotations()));

                for (Annotation ann : anns)
                {
                    interceptorAnns.add(ann);
                }

                for (Annotation ann : annsClazz)
                {
                    interceptorAnns.add(ann);
                }
            }

            Annotation[] stereoTypes =
                annotationManager.getStereotypeMetaAnnotations(
                    AnnotationUtil.getAnnotationsFromSet(annotatedType.getAnnotations()));
            for (Annotation stero : stereoTypes)
            {
                if (annotationManager.hasInterceptorBindingMetaAnnotation(
                    stero.annotationType().getDeclaredAnnotations()))
                {
                    Annotation[] steroInterceptorBindings =
                        annotationManager.getInterceptorBindingMetaAnnotations(
                            stero.annotationType().getDeclaredAnnotations());

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
                    
                    AnnotatedType<?> interAnnoType = interceptor.getAnnotatedType();
                    
                    if(interAnnoType == null)
                    {
                        WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), AroundInvoke.class, true, true, stack, method, true);
                        WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PostConstruct.class, true, true, stack, method, true);
                        WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PreDestroy.class, true, true, stack, method, true);                        
                    }
                    else
                    {
                        WebBeansUtil.configureInterceptorMethods(interceptor, interAnnoType, AroundInvoke.class, true, true, stack, method);
                        WebBeansUtil.configureInterceptorMethods(interceptor, interAnnoType, PostConstruct.class, true, true, stack, method);
                        WebBeansUtil.configureInterceptorMethods(interceptor, interAnnoType, PreDestroy.class, true, true, stack, method);                        
                    }
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
        return Collections.unmodifiableSet(WebBeansContext.getInstance().getBeanManagerImpl().getInterceptors());
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
