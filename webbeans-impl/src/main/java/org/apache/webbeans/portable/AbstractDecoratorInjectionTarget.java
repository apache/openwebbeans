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
package org.apache.webbeans.portable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.InjectableConstructor;

public class AbstractDecoratorInjectionTarget<T> extends InjectionTargetImpl<T>
{
    private Class<T> proxySubClass;

    public AbstractDecoratorInjectionTarget(AnnotatedType<T> annotatedType, Set<InjectionPoint> points, WebBeansContext webBeansContext,
                                             List<AnnotatedMethod<?>> postConstructMethods, List<AnnotatedMethod<?>> preDestroyMethods)
    {
        super(annotatedType, points, webBeansContext, postConstructMethods, preDestroyMethods);
    }

    @Override
    protected AnnotatedConstructor<T> createConstructor()
    {
        // create proxy subclass
        Class<T> classToProxy = annotatedType.getJavaClass();
        ClassLoader classLoader = webBeansContext.getApplicationBoundaryService().getBoundaryClassLoader(classToProxy);
        if (classLoader == null)
        {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        proxySubClass = webBeansContext.getSubclassProxyFactory().createImplementedSubclass(classLoader, annotatedType);

        Constructor<T> ct = (Constructor<T>) webBeansContext.getSecurityService().doPrivilegedGetDeclaredConstructors(proxySubClass)[0];
        Constructor<T> parentCtor;
        try
        {
            parentCtor = classToProxy.getConstructor(ct.getParameterTypes());
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException(e);
        }
        return new SubClassAnnotatedConstructorImpl<T>(webBeansContext, parentCtor, ct, annotatedType);
    }

    @Override
    protected SubClassAnnotatedConstructorImpl<T> getConstructor()
    {
        if (constructor == null)
        {
            constructor = createConstructor();
        }
        return (SubClassAnnotatedConstructorImpl<T>) constructor;
    }

    @Override
    protected T newInstance(CreationalContextImpl<T> creationalContext)
    {
        return new AbstractDecoratorInjectableConstructor<>(
            getConstructor().parentConstructor, getConstructor().getJavaMember(), this, creationalContext).doInjection();
    }

    public static class SubClassAnnotatedConstructorImpl<T> extends AnnotatedConstructorImpl<T>
    {
        private final Constructor<T> parentConstructor;

        public SubClassAnnotatedConstructorImpl(WebBeansContext webBeansContext,
                                                Constructor<T> parentConstructor,
                                                Constructor<T> javaMember,
                                                AnnotatedType<T> declaringType)
        {
            super(webBeansContext, javaMember, declaringType);
            this.parentConstructor = parentConstructor;
        }
    }

    public static class AbstractDecoratorInjectableConstructor<T> extends InjectableConstructor<T>
    {
        private final Constructor<T> parent;

        public AbstractDecoratorInjectableConstructor(Constructor<T> parentConstructor,
                                                      Constructor<T> cons, InjectionTarget<T> owner,
                                                      CreationalContextImpl<T> creationalContext)
        {
            super(cons, owner, creationalContext);
            this.parent = parentConstructor;
        }

        @Override
        protected List<InjectionPoint> getInjectionPoints(Member member)
        {
            List<InjectionPoint> injectionPoints = new ArrayList<>();
            for (InjectionPoint injectionPoint : owner.getInjectionPoints())
            {
                if (injectionPoint.getMember().equals(parent)) // we don't compare to the runtime subclass constructor
                {
                    injectionPoints.add(injectionPoint);
                }
            }
            return injectionPoints;
        }
    }
}
