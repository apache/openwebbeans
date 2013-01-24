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

import javax.decorator.Delegate;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
import org.apache.webbeans.portable.AnnotatedConstructorImpl;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.util.ClassUtil;


/**
 * Bean builder for {@link org.apache.webbeans.component.InterceptorBean}s.
 */
public class DecoratorBeanBuilder<T> extends AbstractInjectionTargetBeanBuilder<T, DecoratorBean<T>>
{
    private static Logger logger = WebBeansLoggerFacade.getLogger(DecoratorBeanBuilder.class);

    private AnnotatedConstructor<T> constructor;

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
        super(webBeansContext, annotatedType, beanAttributes);
        decoratedTypes = new HashSet<Type>(beanAttributes.getTypes());
        ignoredDecoratorInterfaces = getIgnoredDecoratorInterfaces();
    }

    private Set<String> getIgnoredDecoratorInterfaces()
    {
        Set<String> result = new HashSet<String>(webBeansContext.getOpenWebBeansConfiguration().getIgnoredInterfaces());
        return result;
    }


    public void defineConstructor()
    {
        constructor = getBeanConstructor();
    }

    /**
     * If this method returns <code>false</code> the {@link #getBean()} method must not get called.
     *
     * @return <code>true</code> if the Decorator is enabled and a Bean should get created
     */
    public boolean isDecoratorEnabled()
    {
        return webBeansContext.getDecoratorsManager().isDecoratorEnabled(getBeanType());
    }

    protected void checkDecoratorConditions()
    {
        if(getBeanAttributes().getScope() != Dependent.class)
        {
            if(logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, OWBLogConst.WARN_0005_1, getBeanType().getName());
            }
        }

        if(getBeanAttributes().getName() != null)
        {
            if(logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, OWBLogConst.WARN_0005_2, getBeanType().getName());
            }
        }

/*X TODO enable again
        if(isAlternative())
        {
            if(logger.isLoggable(Level.WARNING))
            {
                logger.log(Level.WARNING, OWBLogConst.WARN_0005_3, getBeanType().getName());
            }
        }
*/


        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "Configuring decorator class : [{0}]", getBeanType());
        }

        Set<AnnotatedMethod<? super T>> methods = getAnnotated().getMethods();
        for(AnnotatedMethod method : methods)
        {
            List<AnnotatedParameter> parms = method.getParameters();
            for (AnnotatedParameter parameter : parms)
            {
                if (parameter.isAnnotationPresent(Produces.class))
                {
                    throw new WebBeansConfigurationException("Interceptor class : " + getBeanType()
                            + " can not have producer methods but it has one with name : "
                            + method.getJavaMember().getName());
                }
            }
        }
    }

    public void defineDecoratorRules()
    {
        checkDecoratorConditions();

        defineConstructor();
        defineInjectedMethods();
        defineInjectedFields();

        defineDecoratedTypes();
    }

    private void defineDecoratedTypes()
    {
        Class<T> beanClass = getBeanType();

        // determine a safe Type for for a later BeanManager.getReference(...)
        if (ClassUtil.isDefinitionContainsTypeVariables(beanClass))
        {
            OwbParametrizedTypeImpl pt = new OwbParametrizedTypeImpl(beanClass.getDeclaringClass(),beanClass);
            TypeVariable<?>[] tvs = beanClass.getTypeParameters();
            for(TypeVariable<?> tv : tvs)
            {
                pt.addTypeArgument(tv);
            }
            decoratedTypes.remove(pt);
            //X TODO generic support setDecoratorGenericType(pt);
        }
        else
        {
            decoratedTypes.remove(beanClass);
            //X TODO generic support setDecoratorGenericType(beanClass);
        }

        /* drop any non-interface bean types */
        Type superClass = beanClass.getGenericSuperclass();
        while (superClass != Object.class)
        {
            decoratedTypes.remove(superClass);
            superClass = superClass.getClass().getGenericSuperclass();
        }
        decoratedTypes.remove(Object.class);
        decoratedTypes.remove(java.io.Serializable.class); /* 8.1 */


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

    @Override
    protected InjectionTarget<T> buildInjectionTarget(AnnotatedType<T> annotatedType, Set<InjectionPoint> points,
                                                      WebBeansContext webBeansContext, List<AnnotatedMethod<?>> postConstructMethods, List<AnnotatedMethod<?>> preDestroyMethods)
    {
        InjectionTarget<T> injectionTarget = super.buildInjectionTarget(annotatedType, points, webBeansContext, postConstructMethods, preDestroyMethods);

        if (Modifier.isAbstract(annotatedType.getJavaClass().getModifiers()))
        {
            injectionTarget = new AbstractDecoratorInjectionTarget(annotatedType, points, webBeansContext, postConstructMethods, preDestroyMethods);
        }
        return injectionTarget;
    }

    @Override
    protected DecoratorBean<T> createBean(Class<T> beanClass, boolean enabled)
    {
        DecoratorBean<T> decorator = new DecoratorBean<T>(webBeansContext, WebBeansType.MANAGED, getAnnotated(), getBeanAttributes(), beanClass);
        decorator.setEnabled(enabled);
        return decorator;
    }

    @Override
    public DecoratorBean<T> getBean()
    {
        DecoratorBean<T> decorator = super.getBean();

        // we can only do this after the bean injection points got scanned
        defineDelegate(decorator.getInjectionPoints());
        decorator.setDecoratorInfo(decoratedTypes, delegateType, delegateQualifiers);

        return decorator;
    }

    /**
     * Helper class to swap out the constructor for the proxied subclass.
     */
    private static class AbstractDecoratorInjectionTarget<T> extends InjectionTargetImpl<T>
    {
        private Class<T> proxySubClass = null;

        private AbstractDecoratorInjectionTarget(AnnotatedType<T> annotatedType, Set<InjectionPoint> points, WebBeansContext webBeansContext,
                                                 List<AnnotatedMethod<?>> postConstructMethods, List<AnnotatedMethod<?>> preDestroyMethods)
        {
            super(annotatedType, points, webBeansContext, postConstructMethods, preDestroyMethods);
        }

        @Override
        protected AnnotatedConstructor<T> createConstructor()
        {
            // create proxy subclass
            ClassLoader classLoader = this.getClass().getClassLoader();
            Class<T> classToProxy = annotatedType.getJavaClass();

            proxySubClass = webBeansContext.getSubclassProxyFactory().createImplementedSubclass(classLoader, classToProxy);

            //X TODO what about @Inject constructors?
            Constructor<T> ct = webBeansContext.getWebBeansUtil().getNoArgConstructor(proxySubClass);
            return new AnnotatedConstructorImpl<T>(webBeansContext, ct, annotatedType);
        }

    }
}
