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
package org.apache.webbeans.component.creation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Named;
import javax.inject.Scope;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.annotation.NamedLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.container.ExternalScope;
import org.apache.webbeans.event.EventUtil;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class info
 */
public class AbstractBeanCreator<T>
{
    /**Bean instance*/
    private final AbstractOwbBean<T> bean;    
    
    private Annotated annotated;
    
    private WebBeansContext webBeansContext;
    
    private String beanName;

    private Set<Annotation> qualifiers = new HashSet<Annotation>();
    
    private Set<Class<? extends Annotation>> stereotypes = new HashSet<Class<? extends Annotation>>();
    
    /**
     * Creates a bean instance.
     * 
     * @param bean bean instance
     * @param annotated
     */
    public AbstractBeanCreator(AbstractOwbBean<T> bean, Annotated annotated)
    {
        this.bean = bean;
        this.annotated = annotated;
        this.webBeansContext = bean.getWebBeansContext();
    }

    protected Set<Annotation> getQualifiers()
    {
        return qualifiers;
    }

    /**
     * {@inheritDoc}
     */
    public void checkCreateConditions()
    {
        //Sub-class can override this
    }

    /**
     * {@inheritDoc}
     */
    public void defineApiType()
    {
        Set<Type> types = annotated.getTypeClosure();
        bean.getTypes().addAll(types);
        Set<String> ignored = bean.getWebBeansContext().getOpenWebBeansConfiguration().getIgnoredInterfaces();
        for (Iterator<Type> i = bean.getTypes().iterator(); i.hasNext();)
        {
            Type t = i.next();
            if (t instanceof Class && ignored.contains(((Class<?>)t).getName()))
            {
                i.remove();
            }
        }
    }

    public void defineName(String name)
    {
        Annotation[] anns = AnnotationUtil.asArray(getAnnotated().getAnnotations());
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
            if (webBeansContext.getAnnotationManager().hasNamedOnStereoTypes(stereotypes))
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
                beanName = nameAnnot.value();
            }

        }

        if (isDefault)
        {
            beanName = name;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void defineQualifiers()
    {
        Annotation[] annotations = AnnotationUtil.asArray(annotated.getAnnotations());
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
                            throw new WebBeansConfigurationException("WebBeans definition class : " + method.getDeclaringClass().getName() + " @Qualifier : "
                                                                     + annotation.annotationType().getName()
                                                                     + " must have @NonBinding valued members for its array-valued and annotation valued members");
                        }
                    }
                }

                if (annotation.annotationType().equals(Named.class) && beanName != null)
                {
                    qualifiers.add(new NamedLiteral(beanName));
                }
                else
                {
                    qualifiers.add(annotation);
                }
            }
        }
        
        configureInheritedQualifiers();

        // No-binding annotation
        if (qualifiers.size() == 0 )
        {
            qualifiers.add(new DefaultLiteral());
        }
        else if(qualifiers.size() == 1)
        {
            Annotation annot = qualifiers.iterator().next();
            if(annot.annotationType().equals(Named.class))
            {
                qualifiers.add(new DefaultLiteral());
            }
        }
        
        //Add @Any support
        if(!hasAnyQualifier())
        {
            qualifiers.add(new AnyLiteral());
        }
        
    }

    protected void configureInheritedQualifiers()
    {
        // hook for subclasses
    }

    /**
     * Returns true if any binding exist
     * 
     * @param bean bean
     * @return true if any binding exist
     */
    private boolean hasAnyQualifier()
    {
        return AnnotationUtil.getAnnotation(qualifiers, Any.class) != null;
    }

    /**
     * {@inheritDoc}
     */
    public void defineScopeType(String errorMessage, boolean allowLazyInit)
    {
        Annotation[] annotations = AnnotationUtil.asArray(annotated.getAnnotations());
        boolean found = false;

        List<ExternalScope> additionalScopes = webBeansContext.getBeanManagerImpl().getAdditionalScopes();
        
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
                    throw new WebBeansConfigurationException("Not to define both @Scope and @NormalScope on bean : " + getBean());
                }
                
                if (found)
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }

                found = true;
                getBean().setImplScopeType(annotation);
            }
            else
            {
                if(pseudo != null)
                {
                    if (found)
                    {
                        throw new WebBeansConfigurationException(errorMessage);
                    }

                    found = true;
                    getBean().setImplScopeType(annotation);
                }
            }
        }

        if (!found)
        {
            defineDefaultScopeType(errorMessage, allowLazyInit);
        }
    }


    private void defineDefaultScopeType(String exceptionMessage, boolean allowLazyInit)
    {
        // Frist look for inherited scope
        IBeanInheritedMetaData metaData = null;
        if(getBean() instanceof InjectionTargetBean)
        {
            metaData = ((InjectionTargetBean<?>)getBean()).getInheritedMetaData();
        }
        boolean found = false;
        if (metaData != null)
        {
            Annotation inheritedScope = metaData.getInheritedScopeType();
            if (inheritedScope != null)
            {
                found = true;
                getBean().setImplScopeType(inheritedScope);
            }
        }

        if (!found)
        {
            Set<Class<? extends Annotation>> stereos = stereotypes;
            if (stereos.size() == 0)
            {
                getBean().setImplScopeType(new DependentScopeLiteral());

                if (allowLazyInit && getBean() instanceof ManagedBean && isPurePojoBean(webBeansContext, getBean().getBeanClass()))
                {
                    // take the bean as Dependent but we could lazily initialize it
                    // because the bean doesn't contains any CDI feature
                    ((ManagedBean) getBean()).setFullInit(false);
                }
            }
            else
            {
                Annotation defined = null;
                Set<Class<? extends Annotation>> anns = stereotypes;
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
                    getBean().setImplScopeType(defined);
                }
                else
                {
                    getBean().setImplScopeType(new DependentScopeLiteral());

                    if (allowLazyInit && getBean() instanceof ManagedBean && isPurePojoBean(webBeansContext, getBean().getBeanClass()))
                    {
                        // take the bean as Dependent but we could lazily initialize it
                        // because the bean doesn't contains any CDI feature
                        ((ManagedBean) getBean()).setFullInit(false);
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
    private boolean isPurePojoBean(WebBeansContext webBeansContext, Class<?> cls)
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
     * {@inheritDoc}
     */
    public void defineSerializable()
    {
        Asserts.assertNotNull(getBean(), "component parameter can not be null");
        if (ClassUtil.isClassAssignable(Serializable.class, getBean().getReturnType()))
        {
            getBean().setSerializable(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void defineStereoTypes()
    {
        Annotation[] anns = AnnotationUtil.asArray(annotated.getAnnotations());
        final AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        if (annotationManager.hasStereoTypeMetaAnnotation(anns))
        {
            Annotation[] steroAnns =
                annotationManager.getStereotypeMetaAnnotations(anns);

            for (Annotation stereo : steroAnns)
            {
                stereotypes.add(stereo.annotationType());
            }
        }
        
        // Adding inherited qualifiers
        IBeanInheritedMetaData inheritedMetaData = null;
        
        if(getBean() instanceof InjectionTargetBean)
        {
            inheritedMetaData = ((InjectionTargetBean<?>) getBean()).getInheritedMetaData();
        }
        
        if (inheritedMetaData != null)
        {
            Set<Annotation> inheritedTypes = inheritedMetaData.getInheritedStereoTypes();        
            for (Annotation inherited : inheritedTypes)
            {
                Set<Class<? extends Annotation>> qualifiers = stereotypes;
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
                    stereotypes.add(inherited.annotationType());
                }
            }
        }
    }
    
    protected <X> void addMethodInjectionPointMetaData(AnnotatedMethod<X> method)
    {
        List<InjectionPoint> injectionPoints = getBean().getWebBeansContext().getInjectionPointFactory().getMethodInjectionPointData(getBean(), method);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            getBean().addInjectionPoint(injectionPoint);
        }
    }
    
    protected void addImplicitComponentForInjectionPoint(InjectionPoint injectionPoint)
    {
        if(!WebBeansUtil.checkObtainsInjectionPointConditions(injectionPoint))
        {
            EventUtil.checkObservableInjectionPointConditions(injectionPoint);
        }        
    }

    /**
     * {@inheritDoc}
     */
    public AbstractOwbBean<T> getBean()
    {
        bean.setName(beanName);
        bean.getQualifiers().addAll(qualifiers);
        bean.getStereotypes().addAll(stereotypes);
        return bean;
    }

    protected Annotated getAnnotated()
    {
        return annotated;
    }
}
