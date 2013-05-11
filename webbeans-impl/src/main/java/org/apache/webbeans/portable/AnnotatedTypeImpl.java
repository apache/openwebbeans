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

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.ClassUtil;

/**
 * Implementation of the {@link AnnotatedType} interface.
 *
 * @param <X> class type
 * @version $Rev$ $Date$
 */
class AnnotatedTypeImpl<X>
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
    AnnotatedTypeImpl(WebBeansContext webBeansContext, Class<X> annotatedClass, AnnotatedTypeImpl<? super X> supertype)
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
            Set<Class<? extends Annotation>> annotationTypes = new HashSet<Class<? extends Annotation>>();
            List<Annotation> annotations = new ArrayList<Annotation>();
            for (Annotation annotation : annotatedClass.getDeclaredAnnotations())
            {
                annotations.add(annotation);
                annotationTypes.add(annotation.annotationType());
            }
            for (Annotation annotation : supertype.getAnnotations())
            {
                if (annotation.annotationType().isAnnotationPresent(Inherited.class) &&
                    !annotationTypes.contains(annotation.annotationType()))
                {
                    annotations.add(annotation);
                    annotationTypes.add(annotation.annotationType());
                }
            }
            setAnnotations(annotations.toArray(new Annotation[annotations.size()]));
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
     * Adds new annotated constructor.
     *
     * @param constructor new constructor
     */
    void addAnnotatedConstructor(AnnotatedConstructor<X> constructor)
    {
        getState().constructors.add(constructor);
    }

    /**
     * Adds new annotated field.
     *
     * @param field new field
     */
    void addAnnotatedField(AnnotatedField<? super X> field)
    {
        getState().fields.add(field);
    }

    /**
     * Adds new annotated method.
     *
     * @param method new method
     */
    void addAnnotatedMethod(AnnotatedMethod<? super X> method)
    {
        getState().methods.add(method);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AnnotatedConstructor<X>> getConstructors()
    {
        return Collections.unmodifiableSet(getState().constructors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AnnotatedField<? super X>> getFields()
    {
        return Collections.unmodifiableSet(getState().fields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AnnotatedMethod<? super X>> getMethods()
    {
        return Collections.unmodifiableSet(getState().methods);
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
        private final Set<AnnotatedConstructor<X>> constructors = new HashSet<AnnotatedConstructor<X>>();

        /**
         * Fields
         */
        private final Set<AnnotatedField<? super X>> fields = new HashSet<AnnotatedField<? super X>>();

        /**
         * Methods
         */
        private final Set<AnnotatedMethod<? super X>> methods = new HashSet<AnnotatedMethod<? super X>>();

        private State()
        {
            Constructor<?>[] decCtxs =
                getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredConstructors(annotatedClass);

            for (Constructor<?> ct : decCtxs)
            {
                if (!ct.isSynthetic())
                {
                    AnnotatedConstructor<X> ac =
                        new AnnotatedConstructorImpl<X>(getWebBeansContext(), (Constructor<X>) ct,
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
                        new AnnotatedConstructorImpl<X>(getWebBeansContext(), constructor, AnnotatedTypeImpl.this));
                }
            }

            Field[] decFields = getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredFields(annotatedClass);
            Method[] decMethods =
                getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredMethods(annotatedClass);
            for (Field f : decFields)
            {
                if (!f.isSynthetic())
                {
                    AnnotatedField<X> af = new AnnotatedFieldImpl<X>(getWebBeansContext(), f, AnnotatedTypeImpl.this);
                    fields.add(af);
                }
            }

            for (Method m : decMethods)
            {
                if (!m.isSynthetic() && !m.isBridge())
                {
                    AnnotatedMethod<X> am = new AnnotatedMethodImpl<X>(getWebBeansContext(), m, AnnotatedTypeImpl.this);
                    methods.add(am);
                }
            }

            if (supertype != null)
            {
                fields.addAll(supertype.getFields());
                for (AnnotatedMethod<? super X> method : supertype.getMethods())
                {
                    if (!isOverridden(method))
                    {
                        methods.add(method);
                    }
                }
            }

        }

        private boolean isOverridden(AnnotatedMethod<? super X> superclassMethod)
        {
            for (AnnotatedMethod<? super X> subclassMethod : methods)
            {
                if (ClassUtil.isOverridden(subclassMethod.getJavaMember(), superclassMethod.getJavaMember()))
                {
                    return true;
                }
            }
            return false;
        }
    }
}
