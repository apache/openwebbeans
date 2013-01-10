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

import static org.apache.webbeans.util.InjectionExceptionUtil.throwUnproxyableResolutionException;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
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
import org.apache.webbeans.annotation.NamedLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.ExternalScope;
import org.apache.webbeans.event.EventUtil;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.helper.ViolationMessageBuilder;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.SecurityUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class info
 */
public abstract class AbstractBeanBuilder<T>
{
    /**Bean instance*/
    private final AbstractOwbBean<T> bean;    
    
    private Annotated annotated;
    
    private WebBeansContext webBeansContext;
    
    private String beanName;
    
    private Class<? extends Annotation> scope;

    private Set<Annotation> qualifiers = new HashSet<Annotation>();
    
    private Set<Class<? extends Annotation>> stereotypes = new HashSet<Class<? extends Annotation>>();
    
    private boolean serializable = false;

    public AbstractBeanBuilder(AbstractOwbBean<T> bean, Annotated annotated)
    {
        this(bean, annotated, null);
    }

    /**
     * Creates a bean instance.
     * 
     * @param bean bean instance
     * @param annotated
     */
    public AbstractBeanBuilder(AbstractOwbBean<T> bean, Annotated annotated, Class<? extends Annotation> scopeType)
    {
        this.bean = bean;
        this.annotated = annotated;
        this.scope = scopeType;
        this.webBeansContext = bean.getWebBeansContext();
    }

    public Class<? extends Annotation> getScope()
    {
        return scope;
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
        
        defineInheritedQualifiers(qualifiers);

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

    protected void defineInheritedQualifiers(Set<Annotation> qualifiers)
    {
        // hook for subclasses
    }
    
    protected void defineInheritedStereotypes(Set<Class<? extends Annotation>> stereotypes)
    {
        // hook for subclasses
    }
    
    protected Class<? extends Annotation> defineInheritedScope()
    {
        // hook for subclasses
        return null;
    }


    /**
     * Returns true if any binding exist
     * 
     * @return true if any binding exist
     */
    private boolean hasAnyQualifier()
    {
        return AnnotationUtil.getAnnotation(qualifiers, Any.class) != null;
    }

    public void defineScopeType(String errorMessage)
    {
        defineScopeType(errorMessage, false);
    }

    /**
     * @deprecated as we need to get rid of allowLazyInit
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
                    throw new WebBeansConfigurationException("Not to define both @Scope and @NormalScope on bean : " + getBeanType().getName());
                }
                
                if (found)
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }

                found = true;
                scope = annotation.annotationType();
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
                    scope = annotation.annotationType();
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
        scope = defineInheritedScope();
        
        if (scope == null)
        {
            Set<Class<? extends Annotation>> stereos = stereotypes;
            if (stereos.size() == 0)
            {
                scope = Dependent.class;
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
                    scope = defined.annotationType();
                }
                else
                {
                    scope = Dependent.class;
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
     * Checks the unproxiable condition.
     * @throws WebBeansConfigurationException if bean is not proxied by the container
     */
    protected void checkUnproxiableApiType()
    {
        //Unproxiable test for NormalScoped beans
        if (webBeansContext.getWebBeansUtil().isScopeTypeNormal(scope))
        {
            ViolationMessageBuilder violationMessage = ViolationMessageBuilder.newViolation();

            Class<?> beanClass = getBeanType();
            
            if(!beanClass.isInterface() && beanClass != Object.class)
            {
                if(beanClass.isPrimitive())
                {
                    violationMessage.addLine("It isn't possible to proxy a primitive type (" + beanClass.getName(), ")");
                }

                if(beanClass.isArray())
                {
                    violationMessage.addLine("It isn't possible to proxy an array type (", beanClass.getName(), ")");
                }

                if(!violationMessage.containsViolation())
                {
                    if (Modifier.isFinal(beanClass.getModifiers()))
                    {
                        violationMessage.addLine(beanClass.getName(), " is a final class! CDI doesn't allow to proxy that.");
                    }

                    Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(beanClass);
                    for (Method m : methods)
                    {
                        int modifiers = m.getModifiers();
                        if (Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers) &&
                            !m.isSynthetic() && !m.isBridge())
                        {
                            violationMessage.addLine(beanClass.getName(), " has final method "+ m + " CDI doesn't allow to proxy that.");
                        }
                    }

                    Constructor<?> cons = webBeansContext.getWebBeansUtil().getNoArgConstructor(beanClass);
                    if (cons == null)
                    {
                        violationMessage.addLine(beanClass.getName(), " has no explicit no-arg constructor!",
                                "A public or protected constructor without args is required!");
                    }
                    else if (Modifier.isPrivate(cons.getModifiers()))
                    {
                        violationMessage.addLine(beanClass.getName(), " has a >private< no-arg constructor! CDI doesn't allow to proxy that.");
                    }
                }

                //Throw Exception
                if(violationMessage.containsViolation())
                {
                    throwUnproxyableResolutionException(violationMessage);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void defineSerializable()
    {
        if (ClassUtil.isClassAssignable(Serializable.class, getBeanType()))
        {
            serializable = true;
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
        defineInheritedStereotypes(stereotypes);
    }
    
    protected <X> void addMethodInjectionPointMetaData(AnnotatedMethod<X> method)
    {
        List<InjectionPoint> injectionPoints = webBeansContext.getInjectionPointFactory().getMethodInjectionPointData(getBean(), method);
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
        bean.setImplScopeType(scope);
        bean.getQualifiers().addAll(qualifiers);
        bean.getStereotypes().addAll(stereotypes);
        bean.setSerializable(serializable);
        return bean;
    }

    protected Annotated getAnnotated()
    {
        return annotated;
    }
    
    protected abstract Class<?> getBeanType();
}
