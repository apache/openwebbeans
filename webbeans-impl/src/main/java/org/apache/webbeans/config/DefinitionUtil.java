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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.NormalScope;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Scope;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.annotation.NamedLiteral;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.container.ExternalScope;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.EventUtil;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Defines the web beans components common properties.
 */
@SuppressWarnings("unchecked")
public final class DefinitionUtil
{
    private final WebBeansContext webBeansContext;

    public DefinitionUtil(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }
    
    /**
     * Configures the web bean api types.
     * 
     * @param <T> generic class type
     * @param bean configuring web beans component
     * @param clazz bean implementation class
     */
    public static <T> void defineApiTypes(AbstractOwbBean<T> bean, Class<T> clazz)
    {
        //Looking for bean types
        Typed beanTypes = clazz.getAnnotation(Typed.class);
        if(beanTypes != null)
        {
            defineUserDefinedBeanTypes(bean, null, beanTypes);            
        }
        else
        {
            defineNormalApiTypes(bean, clazz);
        }
        removeIgnoredInterfaces(bean);
    }

    public static <T> void removeIgnoredInterfaces(AbstractOwbBean<T> bean)
    {
        Set<String> ignoredInterfaces = bean.getWebBeansContext().getOpenWebBeansConfiguration().getIgnoredInterfaces();
        for (Iterator<Type> i = bean.getTypes().iterator(); i.hasNext(); )
        {
            Type t = i.next();
            if (t instanceof Class && ignoredInterfaces.contains(((Class<?>) t).getName()))
            {
                i.remove();
            }
        }
    }


    private static <T> void defineNormalApiTypes(AbstractOwbBean<T> bean, Class<T> clazz)
    {
        bean.getTypes().add(Object.class);
        ClassUtil.setTypeHierarchy(bean.getTypes(), clazz);           
    }
    
    private static <T> void defineUserDefinedBeanTypes(AbstractOwbBean<T> bean, Type producerGenericReturnType, Typed beanTypes)
    {
        if(producerGenericReturnType != null)
        {
            defineNormalProducerMethodApi((AbstractProducerBean<T>)bean, producerGenericReturnType);
        }
        else
        {
            defineNormalApiTypes(bean, bean.getReturnType());
        }
        
        //@Type values
        Class<?>[] types = beanTypes.value();        
        
        //Normal api types
        Set<Type> apiTypes = bean.getTypes();
        //New api types
        Set<Type> newTypes = new HashSet<Type>();
        for(Class<?> type : types)
        {
            Type foundType = null;
            
            for(Type apiType : apiTypes)
            {
                if(ClassUtil.getClazz(apiType) == type)
                {
                    foundType = apiType;
                    break;
                }
            }
            
            if(foundType == null)
            {
                throw new WebBeansConfigurationException("@Type values must be in bean api types : " + bean.getTypes());
            }
            
            newTypes.add(foundType);
        }
        
        apiTypes.clear();
        apiTypes.addAll(newTypes);
        
        apiTypes.add(Object.class);
    }
    
    

    /**
     * Configures the producer method web bean api types.
     * 
     * @param <T> generic class type
     * @param producerBean configuring web beans component
     * @param type bean implementation class
     */
    public static <T> void defineProducerMethodApiTypes(AbstractProducerBean<T> producerBean, Type type, Annotation[] annots)
    {
        
        //Looking for bean types
        Typed beanTypes = AnnotationUtil.getAnnotation(annots, Typed.class);
        if(beanTypes != null)
        {
            defineUserDefinedBeanTypes(producerBean, type, beanTypes);
        }
        else
        {
            defineNormalProducerMethodApi(producerBean, type);
        }
        removeIgnoredInterfaces(producerBean);
    }
    
    public static <T> void defineNormalProducerMethodApi(AbstractProducerBean<T> producerBean, Type type)
    {
        Set<Type> types = producerBean.getTypes();
        types.add(Object.class);
        
        Class<?> clazz  = ClassUtil.getClazz(type);
        
        if (clazz != null && (clazz.isPrimitive() || clazz.isArray()))
        {
            types.add(clazz);
        }
        else
        {
            ClassUtil.setTypeHierarchy(producerBean.getTypes(), type);
        }                    
    }

    /**
     * Configure web beans component qualifier.
     * 
     * @param component configuring web beans component
     * @param annotations annotations
     */
    public <T> void defineQualifiers(AbstractOwbBean<T> component, Annotation[] annotations)
    {
        final AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        for (Annotation annotation : annotations)
        {
            Class<? extends Annotation> type = annotation.annotationType();

            if (annotationManager.isQualifierAnnotation(type))
            {
                Method[] methods = webBeansContext.getSecurityService().doPrivilegedGetDeclaredMethods(type);

                for (Method method : methods)
                {
                    Class<?> clazz = method.getReturnType();
                    if (clazz.isArray() || clazz.isAnnotation())
                    {
                        if (!AnnotationUtil.hasAnnotation(method.getDeclaredAnnotations(), Nonbinding.class))
                        {
                            throw new WebBeansConfigurationException("WebBeans definition class : " + component.getReturnType().getName() + " @Qualifier : "
                                                                     + annotation.annotationType().getName()
                                                                     + " must have @NonBinding valued members for its array-valued and annotation valued members");
                        }
                    }
                }

                if (annotation.annotationType().equals(Named.class) && component.getName() != null)
                {
                    component.addQualifier(new NamedLiteral(component.getName()));
                }
                else
                {
                    component.addQualifier(annotation);
                }
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
            Set<Annotation> inheritedTypes = inheritedMetaData.getInheritedQualifiers();
            for (Annotation inherited : inheritedTypes)
            {
                Set<Annotation> qualifiers = component.getQualifiers();
                boolean found = false;
                for (Annotation existQualifier : qualifiers)
                {
                    if (existQualifier.annotationType().equals(inherited.annotationType()))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    component.addQualifier(inherited);
                }
            }
        }
        

        // No-binding annotation
        if (component.getQualifiers().size() == 0 )
        {
            component.addQualifier(new DefaultLiteral());
        }
        else if(component.getQualifiers().size() == 1)
        {
            Annotation annot = component.getQualifiers().iterator().next();
            if(annot.annotationType().equals(Named.class))
            {
                component.addQualifier(new DefaultLiteral());
            }
        }
        
        //Add @Any support
        if(!AnnotationUtil.hasAnyQualifier(component))
        {
            component.addQualifier(new AnyLiteral());
        }
        
    }

    /**
     * Configure web beans component scope type.
     * 
     * @param <T> generic class type
     * @param component configuring web beans component
     * @param annotations annotations
     */
    public <T> void defineScopeType(AbstractOwbBean<T> component, Annotation[] annotations,
                                           String exceptionMessage, boolean allowLazyInit)
    {
        boolean found = false;

        List<ExternalScope> additionalScopes = component.getWebBeansContext().getBeanManagerImpl().getAdditionalScopes();
        
        for (Annotation annotation : annotations)
        {   
            Class<? extends Annotation> annotationType = annotation.annotationType();
            
            /*Normal scope*/
            Annotation var = annotationType.getAnnotation(NormalScope.class);
            /*Pseudo scope*/
            Annotation pseudo = annotationType.getAnnotation(Scope.class);
        
            if (var == null && pseudo == null)
            {
                // check for additional scopes registered via a CDI Extension
                for (ExternalScope additionalScope : additionalScopes)
                {
                    if (annotationType.equals(additionalScope.getScope()))
                    {
                        // create a proxy which implements the given annotation
                        Annotation scopeAnnotation = additionalScope.getScopeAnnotation();
    
                        if (additionalScope.isNormal())
                        {
                            var = scopeAnnotation;
                        }
                        else
                        {
                            pseudo = scopeAnnotation;
                        }
                    }
                }
            }
            
            if (var != null)
            {
                if(pseudo != null)
                {
                    throw new WebBeansConfigurationException("Not to define both @Scope and @NormalScope on bean : " + component);
                }
                
                if (found)
                {
                    throw new WebBeansConfigurationException(exceptionMessage);
                }

                found = true;
                component.setImplScopeType(annotation);
            }
            else
            {
                if(pseudo != null)
                {
                    if (found)
                    {
                        throw new WebBeansConfigurationException(exceptionMessage);
                    }

                    found = true;
                    component.setImplScopeType(annotation);
                }
            }
        }

        if (!found)
        {
            defineDefaultScopeType(component, exceptionMessage, allowLazyInit);
        }
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

    public void defineDefaultScopeType(OwbBean<?> component, String exceptionMessage, boolean allowLazyInit)
    {
        // Frist look for inherited scope
        IBeanInheritedMetaData metaData = null;
        if(component instanceof InjectionTargetBean)
        {
            metaData = ((InjectionTargetBean<?>)component).getInheritedMetaData();
        }
        boolean found = false;
        if (metaData != null)
        {
            Annotation inheritedScope = metaData.getInheritedScopeType();
            if (inheritedScope != null)
            {
                found = true;
                component.setImplScopeType(inheritedScope);
            }
        }

        if (!found)
        {
            Set<Class<? extends Annotation>> stereos = component.getStereotypes();
            if (stereos.size() == 0)
            {
                component.setImplScopeType(new DependentScopeLiteral());

                if (allowLazyInit && component instanceof ManagedBean && isPurePojoBean(component.getWebBeansContext(), component.getBeanClass()))
                {
                    // take the bean as Dependent but we could lazily initialize it
                    // because the bean doesn't contains any CDI feature
                    ((ManagedBean) component).setFullInit(false);
                }
            }
            else
            {
                Annotation defined = null;
                Set<Class<? extends Annotation>> anns = component.getStereotypes();
                for (Class<? extends Annotation> stero : anns)
                {
                    boolean containsNormal = AnnotationUtil.hasMetaAnnotation(stero.getDeclaredAnnotations(), NormalScope.class);
                    
                    if (AnnotationUtil.hasMetaAnnotation(stero.getDeclaredAnnotations(), NormalScope.class) ||
                            AnnotationUtil.hasMetaAnnotation(stero.getDeclaredAnnotations(), Scope.class))
                    {                        
                        Annotation next;
                        
                        if(containsNormal)
                        {
                            next = AnnotationUtil.getMetaAnnotations(stero.getDeclaredAnnotations(), NormalScope.class)[0];
                        }
                        else
                        {
                            next = AnnotationUtil.getMetaAnnotations(stero.getDeclaredAnnotations(), Scope.class)[0];
                        }

                        if (defined == null)
                        {
                            defined = next;
                        }
                        else
                        {
                            if (!defined.equals(next))
                            {
                                throw new WebBeansConfigurationException(exceptionMessage);
                            }
                        }
                    }
                }

                if (defined != null)
                {
                    component.setImplScopeType(defined);
                }
                else
                {
                    component.setImplScopeType(new DependentScopeLiteral());

                    if (allowLazyInit && component instanceof ManagedBean && isPurePojoBean(component.getWebBeansContext(), component.getBeanClass()))
                    {
                        // take the bean as Dependent but we could lazily initialize it
                        // because the bean doesn't contains any CDI feature
                        ((ManagedBean) component).setFullInit(false);
                    }
                }
            }
        }

    }

    /**
     * TODO this should get improved.
     * It might be enough to check for instanceof Produces and Decorates
     *
     *
     * Check if the bean uses CDI features
     * @param cls the Class to check
     * @return <code>false</code> if the bean uses CDI annotations which define other beans somewhere
     */
    private static boolean isPurePojoBean(WebBeansContext webBeansContext, Class<?> cls)
    {
        Class<?> superClass = cls.getSuperclass();

        if ( superClass == Object.class || !isPurePojoBean(webBeansContext, superClass))
        {
            return false;
        }

        Set<String> annotations = webBeansContext.getScannerService().getAllAnnotations(cls.getSimpleName());
        if (annotations != null)
        {
            for (String ann : annotations)
            {
                if (ann.startsWith("javax.inject") || ann.startsWith("javax.enterprise") || ann.startsWith("javax.interceptors"))
                {
                    return false;
                }
            }

        }

        return true;
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

    public <T> void addFieldInjectionPointMetaData(AbstractOwbBean<T> owner, Field field)
    {
        InjectionPoint injectionPoint = owner.getWebBeansContext().getInjectionPointFactory().getFieldInjectionPointData(owner, field);
        if (injectionPoint != null)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
        }
    }

    public <T> void addMethodInjectionPointMetaData(AbstractOwbBean<T> owner, Method method)
    {
        List<InjectionPoint> injectionPoints = owner.getWebBeansContext().getInjectionPointFactory().getMethodInjectionPointData(owner, method);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
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
    
    public <X> Set<ProducerMethodBean<?>> defineProducerMethods(InjectionTargetBean<X> bean, AnnotatedType<X> annotatedType)
    {
        Set<ProducerMethodBean<?>> producerComponents = new HashSet<ProducerMethodBean<?>>();
        Set<AnnotatedMethod<? super X>> annotatedMethods = annotatedType.getMethods();
        
        for(AnnotatedMethod annotatedMethod: annotatedMethods)
        {
            createProducerBeansFromAnnotatedType(bean, producerComponents, annotatedMethod, bean.getReturnType(), false);
        }
        
        return producerComponents;
    }
    
    private <X> void createProducerBeansFromAnnotatedType(InjectionTargetBean<X> bean, Set<ProducerMethodBean<?>> producerComponents,
                                                          AnnotatedMethod<X> annotatedMethod, Class<?> clazz, boolean isSpecializes)
    {
        boolean specializes = isSpecializes;
        Set<Annotation> annSet = annotatedMethod.getAnnotations();
        if (annSet == null || annSet.isEmpty())
        {
            return;
        }

        Annotation[] anns = annSet.toArray(new Annotation[annSet.size()]);
        
        List<AnnotatedParameter<X>> parameters = annotatedMethod.getParameters();
        // Producer Method
        if (AnnotationUtil.hasAnnotation(anns, Produces.class))
        {
            for(AnnotatedParameter<X> parameter : parameters)
            {
                Set<Annotation> paramAnnSet = parameter.getAnnotations();
                Annotation[] parameterAnns = paramAnnSet.toArray(new Annotation[parameter.getAnnotations().size()]);
                if (AnnotationUtil.hasAnnotation(anns, Inject.class) ||
                    AnnotationUtil.hasAnnotation(parameterAnns, Disposes.class) ||
                    AnnotationUtil.hasAnnotation(parameterAnns, Observes.class))
                {
                    throw new WebBeansConfigurationException("Producer Method Bean with name : " + annotatedMethod.getJavaMember().getName() + " in bean class : " + clazz
                                                             + " can not be annotated with @Initializer/@Destructor annotation "
                                                             + "or has a parameter annotated with @Disposes/@Observes");
                }
                
            }
            
            if (AnnotationUtil.hasAnnotation(anns, Specializes.class))
            {
                if (Modifier.isStatic(annotatedMethod.getJavaMember().getModifiers()))
                {
                    throw new WebBeansConfigurationException("Specializing producer method : " + annotatedMethod.getJavaMember().getName() + " in class : " + clazz.getName()
                                                             + " can not be static");
                }

                specializes = true;
            }

            ProducerMethodBean<?> newComponent = createProducerBeanFromAnnotatedType((Class<X>)annotatedMethod.getJavaMember().getReturnType(),
                                                                                     annotatedMethod, bean, specializes);
            if (newComponent != null)
            {
                producerComponents.add(newComponent);
                addMethodInjectionPointMetaData(newComponent, annotatedMethod.getJavaMember());
            }
        }

    }

    public <X> ProducerMethodBean<X> createProducerBeanFromAnnotatedType(Class<X> returnType, AnnotatedMethod<X> method,
                                                                                InjectionTargetBean<?> parent, boolean isSpecializes)
    {
        ProducerMethodBean<X> bean = new ProducerMethodBean<X>(parent, returnType);
        bean.setCreatorMethod(method.getJavaMember());

        if (isSpecializes)
        {
            WebBeansUtil.configureProducerSpecialization(bean, method.getJavaMember(), parent.getReturnType().getSuperclass());
        }

        if (returnType.isPrimitive())
        {
            bean.setNullable(false);
        }

        Set<Annotation> annSet = method.getAnnotations();
        Annotation[] anns = annSet.toArray(new Annotation[annSet.size()]);
        
        defineSerializable(bean);
        defineStereoTypes(bean, anns);

        defineProducerMethodApiTypes(bean, method.getBaseType(), anns);
        defineScopeType(bean, anns, "Bean producer method : " + method.getJavaMember().getName() + " in class "
                                                   + parent.getReturnType().getName() + " must declare default @Scope annotation", false);
        WebBeansUtil.checkProducerGenericType(bean,method.getJavaMember());        
        defineName(bean, anns, WebBeansUtil.getProducerDefaultName(method.getJavaMember().getName()));
        defineQualifiers(bean, anns);

        return bean;
    }
    
}
