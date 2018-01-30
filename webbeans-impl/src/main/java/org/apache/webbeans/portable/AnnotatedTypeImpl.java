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

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.config.WebBeansContext;

/**
 * Implementation of the {@link AnnotatedType} interface.
 *
 * @param <X> class type
 * @version $Rev$ $Date$
 */
public class AnnotatedTypeImpl<X>
    extends AbstractAnnotated
    implements AnnotatedType<X>
{
    /**
     * parent class
     */
    private final AnnotatedType<? super X> supertype;

    /**
     * Annotated class
     */
    private final Class<X> annotatedClass;

    private volatile State state;

    /**
     * Creates a new instance.
     *
     * @param annotatedClass class
     */
    AnnotatedTypeImpl(WebBeansContext webBeansContext, Class<X> annotatedClass, AnnotatedType<? super X> supertype)
    {
        super(webBeansContext, annotatedClass);
        this.supertype = supertype;
        this.annotatedClass = annotatedClass;

        if (supertype == null)
        {
            setAnnotations(annotatedClass.getDeclaredAnnotations());
        }
        else
        {
            BeanManager bm = webBeansContext.getBeanManagerImpl();
            Set<Class<? extends Annotation>> annotationTypes = new HashSet<>();
            List<Annotation> annotations = new ArrayList<>();
            boolean hasScope = false;
            for (Annotation annotation : annotatedClass.getDeclaredAnnotations())
            {
                if (bm.isScope(annotation.annotationType()))
                {
                    hasScope = true;
                }
                annotations.add(annotation);
                annotationTypes.add(annotation.annotationType());
            }
            for (Annotation annotation : supertype.getAnnotations())
            {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType.isAnnotationPresent(Inherited.class)
                    && !annotationTypes.contains(annotationType)
                    && (!bm.isScope(annotationType) || !hasScope))
                {
                    annotations.add(annotation);
                    annotationTypes.add(annotationType);
                }
            }
            setAnnotations(annotations.toArray(new Annotation[annotations.size()]));
        }
    }

    /**
     * Copy constructor
     *
     * @param webBeansContext actual {@link WebBeansContext}
     * @param otherAnnotatedType to copy
     */
    public AnnotatedTypeImpl(WebBeansContext webBeansContext, AnnotatedType otherAnnotatedType)
    {
        super(webBeansContext, otherAnnotatedType);
        this.annotatedClass = otherAnnotatedType.getJavaClass();

        //X TODO revisit!!
        if (otherAnnotatedType instanceof AnnotatedTypeImpl)
        {
            AnnotatedTypeImpl annotatedTypeImpl = (AnnotatedTypeImpl) otherAnnotatedType;
            this.supertype = annotatedTypeImpl.supertype;

            if (annotatedTypeImpl.state != null)
            {
                this.state = new State(annotatedTypeImpl.state);
            }
        }
        else
        {
            // X TODO
            this.supertype = null;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<X> getJavaClass()
    {
        return annotatedClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AnnotatedConstructor<X>> getConstructors()
    {
        return getState().constructors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AnnotatedField<? super X>> getFields()
    {
        return getState().fields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AnnotatedMethod<? super X>> getMethods()
    {
        return getState().methods;
    }

    @Override
    protected Class<?> getOwningClass()
    {
        return getJavaClass();
    }

    @Override
    protected Class<?> getDeclaringClass()
    {
        return getJavaClass();
    }

    private State getState()
    {
        State result = state;
        // Double check locking with standard optimization to avoid
        // extra reads on the volatile field 'state'
        if (result == null)
        {
            synchronized (this)
            {
                result = state;
                if (result == null)
                {
                    result = new State();
                    state = result;
                }
            }
        }

        return result;
    }

    private class State
    {

        /**
         * Constructors
         */
        private final Set<AnnotatedConstructor<X>> constructors;

        /**
         * Fields
         */
        private final Set<AnnotatedField<? super X>> fields;

        /**
         * Methods
         */
        private final Set<AnnotatedMethod<? super X>> methods;

        private State()
        {
            Constructor<?>[] decCtxs =
                getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredConstructors(annotatedClass);

            Set<AnnotatedConstructor<X>> constructors = new HashSet<>();
            Set<AnnotatedField<? super X>> fields = new HashSet<>();
            Set<AnnotatedMethod<? super X>> methods = new HashSet<>();

            this.constructors = Collections.unmodifiableSet(constructors);
            this.fields = Collections.unmodifiableSet(fields);
            this.methods = Collections.unmodifiableSet(methods);

            for (Constructor<?> ct : decCtxs)
            {
                if (!ct.isSynthetic())
                {
                    AnnotatedConstructor<X> ac =
                        new AnnotatedConstructorImpl<>(getWebBeansContext(), (Constructor<X>) ct,
                            AnnotatedTypeImpl.this);
                    constructors.add(ac);
                }
            }
            if (constructors.isEmpty())
            {
                // must be implicit default constructor
                Constructor<X> constructor =
                    getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredConstructor(annotatedClass);
                if (constructor != null)
                {
                    constructors.add(
                        new AnnotatedConstructorImpl<>(getWebBeansContext(), constructor, AnnotatedTypeImpl.this));
                }
            }

            Field[] decFields = getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredFields(annotatedClass);
            for (Field f : decFields)
            {
                if (!f.isSynthetic())
                {
                    AnnotatedField<X> af = new AnnotatedFieldImpl<>(getWebBeansContext(), f, AnnotatedTypeImpl.this);
                    fields.add(af);
                }
            }

            Method[] decMethods =
                    getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredMethods(annotatedClass);
            for (Method m : decMethods)
            {
                if (!m.isSynthetic() && !m.isBridge())
                {
                    AnnotatedMethod<X> am = new AnnotatedMethodImpl<>(getWebBeansContext(), m, AnnotatedTypeImpl.this);
                    methods.add(am);
                }
            }

            if (supertype != null)
            {
                for (AnnotatedField<? super X> field: supertype.getFields())
                {
                    fields.add(new AnnotatedFieldImpl<>(getWebBeansContext(), field.getJavaMember(), AnnotatedTypeImpl.this));
                }
                for (AnnotatedMethod<? super X> method : supertype.getMethods())
                {
                    methods.add(new AnnotatedMethodImpl<>(getWebBeansContext(), method.getJavaMember(), AnnotatedTypeImpl.this));
                }
            }

        }

        /**
         * Copy ct
         */
        private State(State otherState)
        {
            constructors = otherState.constructors.stream()
                .map(ac -> new AnnotatedConstructorImpl<>(getWebBeansContext(), ac, AnnotatedTypeImpl.this))
                .collect(Collectors.toSet());

            fields = otherState.fields.stream()
                .map(af -> new AnnotatedFieldImpl<>(getWebBeansContext(), af.getJavaMember(), AnnotatedTypeImpl.this))
                .collect(Collectors.toSet());

            methods = otherState.methods.stream()
                .map(am -> new AnnotatedMethodImpl<>(getWebBeansContext(), am, AnnotatedTypeImpl.this))
                .collect(Collectors.toSet());
        }
    }
}
