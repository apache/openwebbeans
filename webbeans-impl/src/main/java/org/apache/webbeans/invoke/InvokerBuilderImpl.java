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
package org.apache.webbeans.invoke;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.invoke.Invoker;
import jakarta.enterprise.invoke.InvokerBuilder;

import org.apache.webbeans.component.AbstractOwbBean;

/**
 * {@link InvokerBuilder} for portable extensions ({@code ProcessManagedBean} / {@code ProcessSessionBean}).
 */
public final class InvokerBuilderImpl<X> implements InvokerBuilder<Invoker<X, ?>>
{
    private final AbstractOwbBean<?> bean;
    private final AnnotatedType<X> annotatedBeanClass;
    private final AnnotatedMethod<? super X> annotatedMethod;
    private final Method javaMethod;
    private final boolean[] argumentLookup;
    private boolean instanceLookup;

    public InvokerBuilderImpl(AbstractOwbBean<?> bean, AnnotatedType<X> annotatedBeanClass,
                              AnnotatedMethod<? super X> annotatedMethod)
    {
        this.bean = bean;
        this.annotatedBeanClass = annotatedBeanClass;
        this.annotatedMethod = annotatedMethod;
        this.javaMethod = annotatedMethod.getJavaMember();
        this.argumentLookup = new boolean[this.javaMethod.getParameterCount()];
    }

    @Override
    public InvokerBuilder<Invoker<X, ?>> withInstanceLookup()
    {
        instanceLookup = true;
        return this;
    }

    @Override
    public InvokerBuilder<Invoker<X, ?>> withArgumentLookup(int position)
    {
        if (position < 0 || position >= argumentLookup.length)
        {
            throw new IllegalArgumentException("Invalid argument position: " + position);
        }
        argumentLookup[position] = true;
        return this;
    }

    @Override
    public Invoker<X, ?> build()
    {
        return new InvokerImpl<>(bean, annotatedBeanClass, annotatedMethod, javaMethod, instanceLookup, argumentLookup.clone());
    }

    /**
     * Validates {@code createInvoker} preconditions (CDI Full).
     */
    public static <X> void validateCreateInvoker(Bean<?> bean, AnnotatedType<X> annotatedBeanClass,
                                                 AnnotatedMethod<? super X> annotatedMethod)
    {
        if (bean instanceof Interceptor)
        {
            throw new DeploymentException("Cannot build invoker for an interceptor: " + bean);
        }
        if (bean instanceof Decorator)
        {
            throw new DeploymentException("Cannot build invoker for a decorator: " + bean);
        }
        Method method = annotatedMethod.getJavaMember();
        if (Modifier.isPrivate(method.getModifiers()))
        {
            throw new DeploymentException("Cannot build invoker for a private method: " + annotatedMethod);
        }
        if (Object.class.equals(method.getDeclaringClass()) && !"toString".equals(method.getName()))
        {
            throw new DeploymentException("Cannot build invoker for a method declared on java.lang.Object: " + annotatedMethod);
        }
        if (!methodBelongsToBean(annotatedBeanClass, annotatedMethod))
        {
            throw new DeploymentException("Method does not belong to bean " + bean + ": " + annotatedMethod);
        }
    }

    private static <X> boolean methodBelongsToBean(AnnotatedType<X> annotatedBeanClass, AnnotatedMethod<? super X> annotatedMethod)
    {
        Method target = annotatedMethod.getJavaMember();
        for (AnnotatedMethod<? super X> candidate : annotatedBeanClass.getMethods())
        {
            if (candidate.getJavaMember().equals(target))
            {
                return true;
            }
        }
        return false;
    }
}
