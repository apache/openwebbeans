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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.decorator.Delegate;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.DecoratorBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.OwbParametrizedTypeImpl;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;


/**
 * Bean builder for {@link org.apache.webbeans.component.InterceptorBean}s.
 */
public class DecoratorBeanBuilder<T>
{
    private static Logger logger = WebBeansLoggerFacade.getLogger(DecoratorBeanBuilder.class);

    protected final WebBeansContext webBeansContext;
    protected final AnnotatedType<T> annotatedType;
    protected final BeanAttributesImpl<T> beanAttributes;

    /**
     * The Types the decorator itself implements
     */
    private Set<Type> decoratedTypes;

    /**
     * The Type of the &#064;Delegate injection point.
     */
    private Type delegateType;

    /**
     * The Qualifiers of the &#064;Delegate injection point.
     */
    private Set<Annotation> delegateQualifiers;

    private final Set<String> ignoredDecoratorInterfaces;

    public DecoratorBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType, BeanAttributesImpl<T> beanAttributes)
    {
        Asserts.assertNotNull(webBeansContext, "webBeansContext may not be null");
        Asserts.assertNotNull(annotatedType, "annotated type may not be null");
        Asserts.assertNotNull(beanAttributes, "beanAttributes may not be null");
        this.webBeansContext = webBeansContext;
        this.annotatedType = annotatedType;
        this.beanAttributes = beanAttributes;
        decoratedTypes = new HashSet<Type>(beanAttributes.getTypes());
        ignoredDecoratorInterfaces = getIgnoredDecoratorInterfaces();
    }

    private Set<String> getIgnoredDecoratorInterfaces()
    {
        return webBeansContext.getOpenWebBeansConfiguration().getIgnoredInterfaces();
    }

    /**
     * If this method returns <code>false</code> the {@link #getBean()} method must not get called.
     *
     * @return <code>true</code> if the Decorator is enabled and a Bean should get created
     */
    public boolean isDecoratorEnabled()
    {
        return webBeansContext.getDecoratorsManager().isDecoratorEnabled(annotatedType.getJavaClass());
    }

    protected void checkDecoratorConditions()
    {
        if (beanAttributes.getScope() != Dependent.class)
        {
            if(logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, OWBLogConst.WARN_0005_1, annotatedType.getJavaClass().getName());
            }
        }

        if (beanAttributes.getName() != null)
        {
            if(logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, OWBLogConst.WARN_0005_2, annotatedType.getJavaClass().getName());
            }
        }

        if (annotatedType.isAnnotationPresent(Alternative.class))
        {
            if(logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, OWBLogConst.WARN_0005_3, annotatedType.getJavaClass().getName());
            }
        }


        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "Configuring decorator class : [{0}]", annotatedType.getJavaClass());
        }

        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for(AnnotatedMethod<?> method : methods)
        {
            for (AnnotatedParameter<?> parameter : method.getParameters())
            {
                if (parameter.isAnnotationPresent(Produces.class))
                {
                    throw new WebBeansConfigurationException("Interceptor class : " + annotatedType.getJavaClass()
                            + " can not have producer methods but it has one with name : "
                            + method.getJavaMember().getName());
                }
            }
        }
    }

    public void defineDecoratorRules()
    {
        checkDecoratorConditions();

        defineDecoratedTypes();
    }

    private void defineDecoratedTypes()
    {
        // remove them first to avoid to loop over them
        decoratedTypes.remove(Object.class);
        decoratedTypes.remove(java.io.Serializable.class); /* 8.1 */

        Type beanClass = annotatedType.getJavaClass();
        do
        {
            final Class<?> clazz = ClassUtil.getClass(beanClass);
            final Type toRemove;
            if (ClassUtil.isDefinitionContainsTypeVariables(beanClass))
            {
                final OwbParametrizedTypeImpl pt = new OwbParametrizedTypeImpl(clazz.getDeclaringClass(), clazz);
                final TypeVariable<?>[] tvs = clazz.getTypeParameters();
                for(TypeVariable<?> tv : tvs)
                {
                    pt.addTypeArgument(tv);
                }
                toRemove = pt;
                //X TODO generic support setDecoratorGenericType(pt);
            }
            else
            {
                toRemove = beanClass;
                //X TODO generic support setDecoratorGenericType(beanClass);
            }

            final Iterator<Type> iterator = decoratedTypes.iterator();
            while (iterator.hasNext())
            {
                final Type next = iterator.next();

                // if raw class is the same and is assignable (generics handling)
                if (ClassUtil.getClass(next) == clazz && ClassUtil.isAssignable(toRemove, next))
                {
                    iterator.remove();
                }
            }

            beanClass = clazz.getGenericSuperclass();
        } while (beanClass != Object.class);


        for (Iterator<Type> i = decoratedTypes.iterator(); i.hasNext(); )
        {
            Type t = i.next();
            if (t instanceof Class<?> && ignoredDecoratorInterfaces.contains(((Class) t).getName()))
            {
                i.remove();
            }
        }

    }

    private void defineDelegate(Set<InjectionPoint> injectionPoints)
    {
        boolean found = false;
        InjectionPoint ipFound = null;
        for(InjectionPoint ip : injectionPoints)
        {
            if(ip.getAnnotated().isAnnotationPresent(Delegate.class))
            {
                if(!found)
                {
                    found = true;
                    ipFound = ip;
                }
                else
                {
                    throw new WebBeansConfigurationException("Decorators must have a one @Delegate injection point. " +
                            "But the decorator bean : " + toString() + " has more than one");
                }
            }
        }


        if(ipFound == null)
        {
            throw new WebBeansConfigurationException("Decorators must have a one @Delegate injection point." +
                    "But the decorator bean : " + toString() + " has none");
        }

        if(!(ipFound.getMember() instanceof Constructor))
        {
            AnnotatedElement element = (AnnotatedElement)ipFound.getMember();
            if(!element.isAnnotationPresent(Inject.class))
            {
                String message = "Error in decorator : "+ toString() + ". The delegate injection point must be an injected field, " +
                        "initializer method parameter or bean constructor method parameter.";

                throw new WebBeansConfigurationException(message);
            }
        }

        delegateType = ipFound.getType();
        delegateQualifiers = ipFound.getQualifiers();

        for (Type decType : decoratedTypes)
        {
            if (!(ClassUtil.getClass(decType)).isAssignableFrom(ClassUtil.getClass(delegateType)))
            {
                throw new WebBeansConfigurationException("Decorator : " + toString() + " delegate attribute must implement all of the decorator decorated types" +
                        ", but decorator type " + decType + " is not assignable from delegate type of " + delegateType);
            }
            else
            {
                if(ClassUtil.isParametrizedType(decType) && ClassUtil.isParametrizedType(delegateType))
                {
                    if(!delegateType.equals(decType))
                    {
                        throw new WebBeansConfigurationException("Decorator : " + toString() + " generic delegate attribute must be same with decorated type : " + decType);
                    }
                }
            }
        }
    }

    public DecoratorBean<T> getBean()
    {
        DecoratorBean<T> decorator = new DecoratorBean<T>(webBeansContext, WebBeansType.MANAGED, annotatedType, beanAttributes, annotatedType.getJavaClass());
        decorator.setEnabled(webBeansContext.getDecoratorsManager().isDecoratorEnabled(annotatedType.getJavaClass()));

        // we can only do this after the bean injection points got scanned
        defineDelegate(decorator.getInjectionPoints());
        decorator.setDecoratorInfo(decoratedTypes, delegateType, delegateQualifiers);

        return decorator;
    }

    protected List<AnnotatedMethod<?>> getPostConstructMethods()
    {
        List<AnnotatedMethod<?>> postConstructMethods = new ArrayList<AnnotatedMethod<?>>();
        collectPostConstructMethods(annotatedType.getJavaClass(), postConstructMethods);
        return postConstructMethods;
    }

    private void collectPostConstructMethods(Class<?> type, List<AnnotatedMethod<?>> postConstructMethods)
    {
        if (type == null)
        {
            return;
        }
        collectPostConstructMethods(type.getSuperclass(), postConstructMethods);
        for (AnnotatedMethod<?> annotatedMethod: annotatedType.getMethods())
        {
            if (annotatedMethod.getJavaMember().getDeclaringClass() == type
                && annotatedMethod.isAnnotationPresent(PostConstruct.class)
                && annotatedMethod.getParameters().isEmpty())
            {
                postConstructMethods.add(annotatedMethod);
            }
        }
    }

    protected List<AnnotatedMethod<?>> getPreDestroyMethods()
    {
        List<AnnotatedMethod<?>> preDestroyMethods = new ArrayList<AnnotatedMethod<?>>();
        collectPreDestroyMethods(annotatedType.getJavaClass(), preDestroyMethods);
        return preDestroyMethods;
    }

    private void collectPreDestroyMethods(Class<?> type, List<AnnotatedMethod<?>> preDestroyMethods)
    {
        if (type == null)
        {
            return;
        }
        collectPreDestroyMethods(type.getSuperclass(), preDestroyMethods);
        for (AnnotatedMethod<?> annotatedMethod: annotatedType.getMethods())
        {
            if (annotatedMethod.getJavaMember().getDeclaringClass() == type
                && annotatedMethod.isAnnotationPresent(PreDestroy.class)
                && annotatedMethod.getParameters().isEmpty())
            {
                preDestroyMethods.add(annotatedMethod);
            }
        }
    }
}
