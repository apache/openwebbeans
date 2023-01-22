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
package org.apache.webbeans.inject.impl;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.annotation.NamedLiteral;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.portable.events.generics.GProcessInjectionPoint;
import org.apache.webbeans.spi.InjectionPointService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InjectionPointFactory
{
    private final WebBeansContext webBeansContext;
    private final InjectionPointService service;

    public InjectionPointFactory(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        this.service = webBeansContext.getService(InjectionPointService.class);
    }

    public <X> Set<InjectionPoint> buildInjectionPoints(Bean<X> owner, AnnotatedType<X> annotatedType)
    {
        Set<InjectionPoint> injectionPoints = new HashSet<>();
        boolean constructorFound = false;
        for (AnnotatedConstructor<X> constructor: annotatedType.getConstructors())
        {
            if (constructor.isAnnotationPresent(Inject.class))
            {
                if (constructorFound)
                {
                    throw new WebBeansConfigurationException("There are more than one constructor with @Inject annotation in annotation type : "
                            + annotatedType);
                }
                constructorFound = true;
                validateInitializerConstructor(constructor);
                buildInjectionPoints(owner, constructor, injectionPoints);
            }
        }
        if (!constructorFound)
        {
            final AnnotatedConstructor<X>[] cons = annotatedType.getConstructors().stream()
                    .filter(constructor -> constructor.getParameters().stream().anyMatch(service::hasInjection))
                    .toArray(AnnotatedConstructor[]::new);
            switch (cons.length)
            {
                case 0:
                    break;
                case 1:
                {
                    validateInitializerConstructor(cons[0]);
                    buildInjectionPoints(owner, cons[0], injectionPoints);
                    break;
                }
                default:
                    throw new WebBeansConfigurationException(
                            "There are more than one candidate constructor for injection in : " + annotatedType);
            }
        }
        for (AnnotatedField<? super X> field: annotatedType.getFields())
        {
            if (owner != null && Modifier.isPublic(field.getJavaMember().getModifiers()) && !field.isStatic())
            {
                if (webBeansContext.getBeanManagerImpl().isNormalScope(owner.getScope()))
                {
                    throw new WebBeansConfigurationException("If bean has a public field, bean scope must be defined as @Scope. Bean is : "
                            + owner.getBeanClass().getName());
                }
            }                
            if (service.hasInjection(field))
            {
                injectionPoints.add(buildInjectionPoint(owner, field));
            }
        }
        for (AnnotatedMethod<? super X> method: webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(annotatedType))
        {
            if (!Modifier.isStatic(method.getJavaMember().getModifiers()) && service.hasInjection(method))
            {
                validateInitializerMethod(method);
                buildInjectionPoints(owner, method, injectionPoints);
            }
        }
        return injectionPoints;
    }

    public <X> InjectionPoint buildInjectionPoint(Bean<?> owner, AnnotatedField<X> annotField, boolean fireEvent)
    {
        Asserts.assertNotNull(annotField, "annotField");

        Annotation[] annots = AnnotationUtil.asArray(annotField.getAnnotations());
        Annotation[] qualifierAnnots = webBeansContext.getAnnotationManager().getQualifierAnnotations(annots);

        //@Named update for injection fields!
        for (int i = 0; i < qualifierAnnots.length; i++)
        {
            Annotation qualifier = qualifierAnnots[i];
            if (qualifier.annotationType().equals(Named.class))
            {
                Named named = (Named) qualifier;
                String value = named.value();

                if (value == null || value.isEmpty())
                {
                    NamedLiteral namedLiteral = new NamedLiteral();
                    namedLiteral.setValue(annotField.getJavaMember().getName());
                    qualifierAnnots[i] = namedLiteral;
                }

                break;
            }
        }

        InjectionPoint injectionPoint = new InjectionPointImpl(owner, Arrays.asList(qualifierAnnots), annotField);

        if (fireEvent)
        {
            GProcessInjectionPoint event = webBeansContext.getWebBeansUtil().fireProcessInjectionPointEvent(injectionPoint);
            injectionPoint = event.getInjectionPoint();
            event.setStarted();
        }

        return injectionPoint;

    }

    public <X> InjectionPoint buildInjectionPoint(Bean<?> owner, AnnotatedField<X> annotField)
    {
        return buildInjectionPoint(owner, annotField, true);
    }

    public <X> InjectionPoint buildInjectionPoint(Bean<?> owner, AnnotatedParameter<X> parameter, boolean fireEvent)
    {
        Asserts.assertNotNull(parameter, "annotatedParameter");
        Set<Annotation> anns = parameter.getAnnotations();
        Annotation[] qualifierAnnots = webBeansContext.getAnnotationManager().getQualifierAnnotations(anns.toArray(new Annotation[anns.size()]));
        InjectionPointImpl injectionPoint = new InjectionPointImpl(owner, Arrays.asList(qualifierAnnots), parameter);
        if (fireEvent)
        {
            GProcessInjectionPoint event = webBeansContext.getWebBeansUtil().fireProcessInjectionPointEvent(injectionPoint);
            InjectionPoint ip = event.getInjectionPoint();
            event.setStarted();
            return ip;
        }
        return injectionPoint;
    }

    public <X> List<InjectionPoint> buildInjectionPoints(Bean<?> owner, AnnotatedCallable<X> callable)
    {
        List<InjectionPoint> lists = new ArrayList<>();
        buildInjectionPoints(owner, callable, lists);
        return lists;
    }

    private <X> void buildInjectionPoints(Bean<?> owner, AnnotatedCallable<X> callable, Collection<InjectionPoint> lists)
    {
        List<AnnotatedParameter<X>> parameters = callable.getParameters();

        for (AnnotatedParameter<?> parameter : parameters)
        {
            // @Observes and @ObservesAsync are not injection point type for method parameters
            if (parameter.getAnnotation(Observes.class) == null && parameter.getAnnotation(ObservesAsync.class) == null)
            {
                lists.add(buildInjectionPoint(owner, parameter, true));
            }
        }
    }

    public static InjectionPoint getPartialInjectionPoint(Bean<?> owner, AnnotatedParameter<?> parameter, Collection<Annotation> bindings)
    {
        return new InjectionPointImpl(owner, bindings, parameter);
    }

    /**
     * This method gets used for InjectionPoints needed during programmatic lookup.
     */
    public static InjectionPoint getVirtualInjectionPoint(Bean<?> bean)
    {
        return new InjectionPointImpl(bean);
    }

    private void validateInitializerConstructor(AnnotatedConstructor<?> constructor)
    {
        for (AnnotatedParameter<?> parameter: constructor.getParameters())
        {
            if (parameter.isAnnotationPresent(Disposes.class))
            {
                throw new WebBeansConfigurationException("Constructor parameter annotations can not contain @Disposes annotation in annotated constructor : " + constructor);
            }

            if(parameter.isAnnotationPresent(Observes.class) || parameter.isAnnotationPresent(ObservesAsync.class))
            {
                throw new WebBeansConfigurationException("Constructor parameter annotations can not contain @Observes nor @ObservesAsync annotation in annotated constructor : "
                                                         + constructor);
            }
        }
    }

    /**
     * add the definitions for a &#x0040;Initializer method.
     */
    private void validateInitializerMethod(AnnotatedMethod<?> annotatedMethod)
    {
        Method method = annotatedMethod.getJavaMember();
        
        TypeVariable<?>[] args = method.getTypeParameters();
        if(args.length > 0)
        {
            throw new WebBeansConfigurationException("Error in defining injected methods in annotated method : " + annotatedMethod+ 
                    ". Reason : Initializer methods must not be generic.");
        }
        
        if (annotatedMethod.isAnnotationPresent(Produces.class))
        {
            throw new WebBeansConfigurationException("Error in defining injected methods in annotated method : " + annotatedMethod+ 
            ". Reason : Initializer method can not be annotated with @Produces.");
        
        }

        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        for (AnnotatedParameter<?> annotatedParameter : annotatedMethod.getParameters())
        {
            if (annotatedParameter.isAnnotationPresent(Disposes.class) ||
                annotatedParameter.isAnnotationPresent(Observes.class) ||
                annotatedParameter.isAnnotationPresent(ObservesAsync.class))
            {
                throw new WebBeansConfigurationException("Error in defining injected methods in annotated method : " + annotatedMethod+ 
                ". Reason : Initializer method parameters does not contain @Observes, @ObservesAsync or @Dispose annotations.");
                
            }
        }
    }
}
