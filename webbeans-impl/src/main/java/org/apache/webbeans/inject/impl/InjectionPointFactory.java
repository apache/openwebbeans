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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.annotation.NamedLiteral;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

public class InjectionPointFactory
{
    private final WebBeansContext webBeansContext;

    public InjectionPointFactory(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    public <X> Set<InjectionPoint> buildInjectionPoints(Bean<X> owner, AnnotatedType<X> annotatedType)
    {
        Set<InjectionPoint> injectionPoints = new HashSet<InjectionPoint>();
        for (AnnotatedConstructor<X> constructor: annotatedType.getConstructors())
        {
            if (constructor.isAnnotationPresent(Inject.class))
            {
                buildInjectionPoints(owner, constructor, injectionPoints);
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
            if (field.isAnnotationPresent(Inject.class))
            {
                injectionPoints.add(buildInjectionPoint(owner, field));
            }
        }
        for (AnnotatedMethod<? super X> method: annotatedType.getMethods())
        {
            if (method.isAnnotationPresent(Inject.class) && !Modifier.isStatic(method.getJavaMember().getModifiers()))
            {
                checkForInjectedInitializerMethod(method);
                buildInjectionPoints(owner, method, injectionPoints);
            }
        }
        return injectionPoints;
    }

    public <X> InjectionPoint buildInjectionPoint(Bean<?> owner, AnnotatedField<X> annotField)
    {
        Asserts.assertNotNull(annotField, "annotField parameter can not be null");

        Annotation[] annots = AnnotationUtil.asArray(annotField.getAnnotations());
        Annotation[] qualifierAnnots = webBeansContext.getAnnotationManager().getQualifierAnnotations(annots);

        //@Named update for injection fields!
        for (int i=0; i < qualifierAnnots.length; i++)
        {
            Annotation qualifier = qualifierAnnots[i];
            if (qualifier.annotationType().equals(Named.class))
            {
                Named named = (Named)qualifier;
                String value = named.value();

                if (value == null || value.equals(""))
                {
                    NamedLiteral namedLiteral = new NamedLiteral();
                    namedLiteral.setValue(annotField.getJavaMember().getName());
                    qualifierAnnots[i] = namedLiteral;
                }

                break;
            }
        }

        return new InjectionPointImpl(owner, annotField.getBaseType(), Arrays.asList(qualifierAnnots), annotField);
    }

    public <X> List<InjectionPoint> buildInjectionPoints(Bean<?> owner, AnnotatedCallable<X> callable)
    {
        List<InjectionPoint> lists = new ArrayList<InjectionPoint>();
        buildInjectionPoints(owner, callable, lists);
        return lists;
    }

    private <X> void buildInjectionPoints(Bean<?> owner, AnnotatedCallable<X> callable, Collection<InjectionPoint> lists)
    {
        Asserts.assertNotNull(callable, "callable parameter can not be null");

        List<AnnotatedParameter<X>> parameters = callable.getParameters();

        for (AnnotatedParameter<?> parameter : parameters)
        {
            //@Observes is not injection point type for method parameters
            if (parameter.getAnnotation(Observes.class) == null)
            {
                Annotation[] qualifierAnnots = webBeansContext.getAnnotationManager().getQualifierAnnotations(parameter.getAnnotations().toArray(new Annotation[0]));
                InjectionPoint point = new InjectionPointImpl(owner, parameter.getBaseType(), Arrays.asList(qualifierAnnots), parameter);
                lists.add(point);
            }
        }
    }

    public static InjectionPoint getPartialInjectionPoint(Bean<?> owner,Type type, AnnotatedParameter<?> parameter, Annotation...bindings)
    {
        return new InjectionPointImpl(owner, type, Arrays.asList(bindings), parameter);
    }

    /**
     * add the definitions for a &#x0040;Initializer method.
     */
    private void checkForInjectedInitializerMethod(AnnotatedMethod<?> annotatedMethod)
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
            annotationManager.checkForNewQualifierForDeployment(annotatedParameter.getBaseType(), annotatedMethod.getDeclaringType().getJavaClass(),
                    method.getName(), AnnotationUtil.asArray(annotatedParameter.getAnnotations()));

            if(annotatedParameter.isAnnotationPresent(Disposes.class) ||
                    annotatedParameter.isAnnotationPresent(Observes.class))
            {
                throw new WebBeansConfigurationException("Error in defining injected methods in annotated method : " + annotatedMethod+ 
                ". Reason : Initializer method parameters does not contain @Observes or @Dispose annotations.");
                
            }
        }
    }
}
