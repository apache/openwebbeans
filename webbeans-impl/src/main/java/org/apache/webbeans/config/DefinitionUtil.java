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
package org.apache.webbeans.config;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.EventUtil;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Defines the web beans components common properties.
 */
public final class DefinitionUtil
{
    private final WebBeansContext webBeansContext;

    public DefinitionUtil(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    public <T> void defineStereoTypes(OwbBean<?> component, Annotation[] anns)
    {
        final AnnotationManager annotationManager = component.getWebBeansContext().getAnnotationManager();
        if (annotationManager.hasStereoTypeMetaAnnotation(anns))
        {
            Annotation[] steroAnns =
                annotationManager.getStereotypeMetaAnnotations(anns);

            for (Annotation stereo : steroAnns)
            {
                component.addStereoType(stereo);
            }
        }
        
        // Adding inherited qualifiers
        IBeanInheritedMetaData inheritedMetaData = null;
        
        if(component instanceof InjectionTargetBean)
        {
            inheritedMetaData = ((InjectionTargetBean<?>) component).getInheritedMetaData();
        }
        
        if (inheritedMetaData != null)
        {
            Set<Annotation> inheritedTypes = inheritedMetaData.getInheritedStereoTypes();        
            for (Annotation inherited : inheritedTypes)
            {
                Set<Class<? extends Annotation>> qualifiers = component.getStereotypes();
                boolean found = false;
                for (Class<? extends Annotation> existQualifier : qualifiers)
                {
                    if (existQualifier.equals(inherited.annotationType()))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    component.addStereoType(inherited);
                }
            }
        }
        
    }

    /**
     * Configure web beans component name.
     * 
     * @param component configuring web beans component
     * @param defaultName default name of the web bean
     */
    public <T> void defineName(AbstractOwbBean<T> component, Annotation[] anns, String defaultName)
    {
        Named nameAnnot = null;
        boolean isDefault = false;
        for (Annotation ann : anns)
        {
            if (ann.annotationType().equals(Named.class))
            {
                nameAnnot = (Named) ann;
                break;
            }
        }

        if (nameAnnot == null) // no @Named
        {
            // Check for stereottype
            if (webBeansContext.getAnnotationManager().hasNamedOnStereoTypes(component))
            {
                isDefault = true;
            }

        }
        else
        // yes @Named
        {
            if (nameAnnot.value().equals(""))
            {
                isDefault = true;
            }
            else
            {
                component.setName(nameAnnot.value());
            }

        }

        if (isDefault)
        {
            component.setName(defaultName);
        }

    }

    /**
     * Configure bean instance interceptor stack.
     * @param bean bean instance
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
        bean.getWebBeansContext().getWebBeansInterceptorConfig().configure(bean, bean.getInterceptorStack());
    }

    /**
     * Defines decorator stack of given bean.
     * @param bean injection target bean
     */
    public void defineDecoratorStack(AbstractInjectionTargetBean<?> bean)
    {
        WebBeansDecoratorConfig.configureDecorators(bean);
    }

    public <T> void defineSerializable(AbstractOwbBean<T> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        if (ClassUtil.isClassAssignable(Serializable.class, component.getReturnType()))
        {
            component.setSerializable(true);
        }
    }

    public <T> void addConstructorInjectionPointMetaData(AbstractOwbBean<T> owner, Constructor<T> constructor)
    {
        List<InjectionPoint> injectionPoints = owner.getWebBeansContext().getInjectionPointFactory().getConstructorInjectionPointData(owner, constructor);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
        }
    }
    
    public void addImplicitComponentForInjectionPoint(InjectionPoint injectionPoint)
    {
        if(!WebBeansUtil.checkObtainsInjectionPointConditions(injectionPoint))
        {
            EventUtil.checkObservableInjectionPointConditions(injectionPoint);
        }        
    }
}
