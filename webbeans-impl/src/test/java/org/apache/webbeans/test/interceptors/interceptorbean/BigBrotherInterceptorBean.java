/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.interceptors.interceptorbean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.interceptor.InvocationContext;
import org.apache.webbeans.annotation.DefaultLiteral;

public class BigBrotherInterceptorBean implements Interceptor<BigBrotherInterceptor>
{
    // it's good performance practice to keep the sets static as they are requested tons of times!
    public static final Set<Type> TYPES = Set.of(BigBrotherInterceptor.class);
    public static final Set<Annotation> QUALIFIERS = Set.of(DefaultLiteral.INSTANCE);
    public static final Set<Annotation> INTERCEPTOR_BINDINGS = Set.of(new AnnotationLiteral<BigBrothered>() {});

    public BigBrotherInterceptorBean(int totallyUselessParamJustToNotHaveADefaultCt)
    {
        // all fine ;)
    }

    @Override
    public Set<Annotation> getInterceptorBindings()
    {
        return INTERCEPTOR_BINDINGS;
    }

    @Override
    public boolean intercepts(InterceptionType type)
    {
        return InterceptionType.AROUND_INVOKE.equals(type);
    }

    @Override
    public Object intercept(InterceptionType type, BigBrotherInterceptor instance, InvocationContext ctx) throws Exception
    {
        return instance.invoke(ctx);
    }

    @Override
    public Class<?> getBeanClass()
    {
        return BigBrotherInterceptor.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return Collections.emptySet();
    }

    @Override
    public BigBrotherInterceptor create(CreationalContext<BigBrotherInterceptor> creationalContext)
    {
        return new BigBrotherInterceptor(42);
    }

    @Override
    public void destroy(BigBrotherInterceptor instance, CreationalContext<BigBrotherInterceptor> creationalContext)
    {
        // no op
    }

    @Override
    public Set<Type> getTypes()
    {
        return TYPES;
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return QUALIFIERS;
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return Dependent.class;
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative()
    {
        return false;
    }
}
