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
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptorBeanPleaseRemove;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.plugins.OpenWebBeansEjbLCAPlugin;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ArrayUtil;
import org.apache.webbeans.util.Asserts;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configures the Web Beans related interceptors.
 *
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @version $Rev$ $Date$
 * @see org.apache.webbeans.intercept.webbeans.WebBeansInterceptorBeanPleaseRemove
 *
 * @deprecated this class can most probably get removed. All important logic is contained in {@link InterceptorResolutionService}
 */
public final class WebBeansInterceptorConfig
{
    /** Logger instance */
    private static Logger logger = WebBeansLoggerFacade.getLogger(WebBeansInterceptorConfig.class);

    private WebBeansContext webBeansContext;

    public WebBeansInterceptorConfig(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * Configure bean instance interceptor stack.
     * @param bean bean instance
     * @deprecated old InterceptorData based config
     */
    public void defineBeanInterceptorStack(AbstractInjectionTargetBean<?> bean)
    {
        Asserts.assertNotNull(bean, "bean parameter can no be null");
        if (!bean.getInterceptorStack().isEmpty())
        {
            // the interceptorstack already got defined!
            return;
        }

        // If bean is not session bean
        if(!(bean instanceof EnterpriseBeanMarker))
        {
            bean.getWebBeansContext().getEJBInterceptorConfig().configure(bean.getAnnotatedType(), bean.getInterceptorStack());
        }
        else
        {
            //Check for injected fields in EJB @Interceptors
            List<InterceptorData> stack = new ArrayList<InterceptorData>();
            bean.getWebBeansContext().getEJBInterceptorConfig().configure(bean.getAnnotatedType(), stack);

            final OpenWebBeansEjbPlugin ejbPlugin = bean.getWebBeansContext().getPluginLoader().getEjbPlugin();
            final boolean isStateful = ejbPlugin.isStatefulBean(bean.getBeanClass());

            if (isStateful)
            {
                for (InterceptorData data : stack)
                {
                    if (data.isDefinedInInterceptorClass())
                    {
                        AnnotationManager annotationManager = bean.getWebBeansContext().getAnnotationManager();
                        if (!annotationManager.checkInjectionPointForInterceptorPassivation(data.getInterceptorClass()))
                        {
                            throw new WebBeansConfigurationException("Enterprise bean : " + bean.toString() +
                                    " interceptors must have serializable injection points");
                        }
                    }
                }
            }
        }

        // For every injection target bean
        configure(bean, bean.getInterceptorStack());
    }

    /**
     * Configures WebBeans specific interceptor class.
     *
     * @param interceptorBindingTypes interceptor class
     */
    public <T> void configureInterceptorClass(AbstractInjectionTargetBean<T> delegate, Annotation[] interceptorBindingTypes)
    {
        if(delegate.getScope() != Dependent.class)
        {
            if(logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, OWBLogConst.WARN_0005_1, delegate.getBeanClass().getName());
            }
        }

        if(delegate.getName() != null)
        {
            if(logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, OWBLogConst.WARN_0005_2, delegate.getBeanClass().getName());
            }
        }

        if(delegate.isAlternative())
        {
            if(logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, OWBLogConst.WARN_0005_3, delegate.getBeanClass().getName());
            }
        }

        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "Configuring interceptor class : [{0}]", delegate.getReturnType());
        }
        WebBeansInterceptorBeanPleaseRemove<T> interceptor = new WebBeansInterceptorBeanPleaseRemove<T>(delegate);

        for (Annotation ann : interceptorBindingTypes)
        {
            checkInterceptorAnnotations(interceptorBindingTypes, ann, delegate);
            interceptor.addInterceptorBinding(ann.annotationType(), ann);
        }


        delegate.getWebBeansContext().getInterceptorsManager().addInterceptor(interceptor);

    }

    private void checkInterceptorAnnotations(Annotation[] interceptorBindingTypes, Annotation ann, Bean<?> bean)
    {
        for(Annotation old : interceptorBindingTypes)
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
     */
    public void configure(AbstractInjectionTargetBean<?> component, List<InterceptorData> stack)
    {
        AnnotatedType<?> annotatedType = component.getAnnotatedType();
        Set<Annotation> annotations = annotatedType.getAnnotations();

        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        Annotation[] typeAnns;
        typeAnns = annotations.toArray(new Annotation[annotations.size()]);
        Set<Annotation> bindingTypeSet = annotationManager.getInterceptorAnnotations(ArrayUtil.asSet(typeAnns));

        Annotation[] anns;
        Set<Interceptor<?>> componentInterceptors = null;

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

        anns = bindingTypeSet.toArray(new Annotation[bindingTypeSet.size()]);

        //Spec Section 9.5.2
        for(Annotation checkAnn : anns)
        {
            checkInterceptorAnnotations(anns, checkAnn, component);
        }

        if (anns.length > 0)
        {
            componentInterceptors = findDeployedWebBeansInterceptor(anns);

            // Adding class interceptors
            addComponentInterceptors(componentInterceptors, stack);
        }

        // Method level interceptors.
        addMethodInterceptors(annotatedType, stack, componentInterceptors);
        filterInterceptorsPerBDA(component,stack);

        Collections.sort(stack, new InterceptorDataComparator(component.getWebBeansContext()));

    }

    private void filterInterceptorsPerBDA(AbstractInjectionTargetBean<?> component, List<InterceptorData> stack)
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

    public void addComponentInterceptors(Set<Interceptor<?>> set, List<InterceptorData> stack)
    {
        Iterator<Interceptor<?>> it = set.iterator();
        while (it.hasNext())
        {
            WebBeansInterceptorBeanPleaseRemove<?> interceptor = (WebBeansInterceptorBeanPleaseRemove<?>) it.next();
            AnnotatedType<?> annotatedType = interceptor.getAnnotatedType();

            OpenWebBeansEjbLCAPlugin ejbPlugin = webBeansContext.getPluginLoader().getEjbLCAPlugin();
            Class <? extends Annotation> prePassivateClass = null;
            Class <? extends Annotation> postActivateClass = null;
            if (null != ejbPlugin)
            {
                prePassivateClass = ejbPlugin.getPrePassivateClass();
                postActivateClass = ejbPlugin.getPostActivateClass();
            }

            // interceptor binding
            webBeansContext.getWebBeansUtil().configureInterceptorMethods(interceptor, annotatedType,
                                                                          AroundInvoke.class, true,
                                                                          false, stack, null, true);
            webBeansContext.getWebBeansUtil().configureInterceptorMethods(interceptor, annotatedType,
                                                                          PostConstruct.class, true,
                                                                          false, stack, null, true);
            webBeansContext.getWebBeansUtil().configureInterceptorMethods(interceptor, annotatedType,
                                                                          PreDestroy.class, true,
                                                                          false, stack, null, true);

            if (null != ejbPlugin)
            {
                webBeansContext.getWebBeansUtil().configureInterceptorMethods(interceptor,
                                                                              annotatedType,
                                                                              prePassivateClass,
                                                                              true, false, stack,
                                                                              null, true);
                webBeansContext.getWebBeansUtil().configureInterceptorMethods(interceptor,
                                                                                 annotatedType,
                                                                                 postActivateClass,
                                                                                 true, false, stack,
                                                                                 null, true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void addMethodInterceptors(AnnotatedType<T> annotatedType,
                                           List<InterceptorData> stack,
                                           Set<Interceptor<?>> componentInterceptors)
    {

        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for(AnnotatedMethod<? super T> methodA : methods)
        {
            AnnotatedMethod<T> methodB = (AnnotatedMethod<T>)methodA;
            Method method = methodB.getJavaMember();
            Set<Annotation> interceptorAnns = new HashSet<Annotation>();

            Annotation[] methodAnns = AnnotationUtil.asArray(methodB.getAnnotations());
            if (annotationManager.hasInterceptorBindingMetaAnnotation(methodAnns))
            {
                Annotation[] anns =
                    annotationManager.getInterceptorBindingMetaAnnotations(
                        methodAnns);
                Annotation[] annsClazz =
                    annotationManager.getInterceptorBindingMetaAnnotations(
                        AnnotationUtil.asArray(annotatedType.getAnnotations()));

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
                    AnnotationUtil.asArray(annotatedType.getAnnotations()));
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
                    WebBeansInterceptorBeanPleaseRemove<?> interceptor = (WebBeansInterceptorBeanPleaseRemove<?>) it.next();

                    AnnotatedType<?> interAnnoType = interceptor.getAnnotatedType();
                    webBeansContext.getWebBeansUtil().configureInterceptorMethods(interceptor,
                                                                                  interAnnoType,
                                                                                  AroundInvoke.class,
                                                                                  true, true, stack,
                                                                                  method, true);
                    webBeansContext.getWebBeansUtil().configureInterceptorMethods(interceptor,
                                                                                  interAnnoType,
                                                                                  PostConstruct.class,
                                                                                  true, true, stack,
                                                                                  method, true);
                    webBeansContext.getWebBeansUtil().configureInterceptorMethods(interceptor,
                                                                                  interAnnoType,
                                                                                  PreDestroy.class,
                                                                                  true, true, stack,
                                                                                  method, true);
                }
            }
        }
    }

    /*
     * Find the deployed interceptors with all the given interceptor binding types.
     * The reason why we can face multiple InterceptorBindings is because of the transitive
     * behaviour of &#064;InterceptorBinding. See section 9.1.1 of the CDI spec.
     */
    public Set<Interceptor<?>> findDeployedWebBeansInterceptor(Annotation[] interceptorBindingTypes)
    {
        Set<Interceptor<?>> set = new HashSet<Interceptor<?>>();

        Iterator<Interceptor<?>> it = webBeansContext.getInterceptorsManager().getInterceptors().iterator();

        List<Class<? extends Annotation>> bindingTypes = new ArrayList<Class<? extends Annotation>>();
        List<Annotation> listAnnot = new ArrayList<Annotation>();
        for (Annotation ann : interceptorBindingTypes)
        {
            bindingTypes.add(ann.annotationType());
            listAnnot.add(ann);
        }

        while (it.hasNext())
        {
            WebBeansInterceptorBeanPleaseRemove<?> interceptor = (WebBeansInterceptorBeanPleaseRemove<?>) it.next();

            if (interceptor.hasBinding(bindingTypes, listAnnot))
            {
                set.add(interceptor);
                set.addAll(interceptor.getMetaInceptors());
            }
        }

        return set;
    }
}
