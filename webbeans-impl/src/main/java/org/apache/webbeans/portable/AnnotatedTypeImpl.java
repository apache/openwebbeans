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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.ClassUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

/**
 * Implementation of the {@link AnnotatedType} interface.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> class type
 */
class AnnotatedTypeImpl<X> extends AbstractAnnotated implements AnnotatedType<X>
{
    /**parent class*/
    private final AnnotatedType<? super X> supertype;
    
    /**Annotated class*/
    private final Class<X> annotatedClass;
    
    /**Constructors*/
    private Set<AnnotatedConstructor<X>> constructors = null;
    
    /**Fields*/
    private Set<AnnotatedField<? super X>> fields = null;
    
    /**Methods*/
    private Set<AnnotatedMethod<? super X>> methods = null;
    
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
        
        setAnnotations(annotatedClass.getDeclaredAnnotations());
    }

    private synchronized void init()
    {
        if (constructors == null)
        {
            constructors = new HashSet<AnnotatedConstructor<X>>();
            fields = new HashSet<AnnotatedField<? super X>>();
            methods = new HashSet<AnnotatedMethod<? super X>>();

            Constructor<?>[] decCtxs = getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredConstructors(annotatedClass);

            for(Constructor<?> ct : decCtxs)
            {
                if (!ct.isSynthetic())
                {
                    AnnotatedConstructor<X> ac = new AnnotatedConstructorImpl<X>(getWebBeansContext(), (Constructor<X>) ct,this);
                    constructors.add(ac);
                }
            }
            if (constructors.isEmpty())
            {
                // must be implicit default constructor
                Constructor<X> constructor = getWebBeansContext().getSecurityService().doPrivilegedGetConstructor(annotatedClass);
                constructors.add(new AnnotatedConstructorImpl<X>(getWebBeansContext(), constructor, this));
            }

            Field[] decFields = getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredFields(annotatedClass);
            Method[] decMethods = getWebBeansContext().getSecurityService().doPrivilegedGetDeclaredMethods(annotatedClass);
            for(Field f : decFields)
            {
                if (!f.isSynthetic())
                {
                    AnnotatedField<X> af = new AnnotatedFieldImpl<X>(getWebBeansContext(), f, this);
                    fields.add(af);
                }
            }

            for(Method m : decMethods)
            {
                if (!m.isSynthetic() && !m.isBridge())
                {
                    AnnotatedMethod<X> am = new AnnotatedMethodImpl<X>(getWebBeansContext(), m,this);
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
    }

    /**
     * {@inheritDoc}
     */
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
        if (constructors == null)
        {
            init();
        }
        constructors.add(constructor);
    }
    
    /**
     * Adds new annotated field.
     * 
     * @param field new field
     */
    void addAnnotatedField(AnnotatedField<? super X> field)
    {
        if (constructors == null)
        {
            init();
        }
        fields.add(field);
    }

    /**
     * Adds new annotated method.
     * 
     * @param method new method
     */
    void addAnnotatedMethod(AnnotatedMethod<? super X> method)
    {
        if (constructors == null)
        {
            init();
        }
        methods.add(method);
    }    
    
    /**
     * {@inheritDoc}
     */
    public Set<AnnotatedConstructor<X>> getConstructors()
    {
        if (constructors == null)
        {
            init();
        }

        return Collections.unmodifiableSet(constructors);
    }

    /**
     * {@inheritDoc}
     */    
    public Set<AnnotatedField<? super X>> getFields()
    {
        if (constructors == null)
        {
            init();
        }

        return Collections.unmodifiableSet(fields);
    }

    /**
     * {@inheritDoc}
     */    
    public Set<AnnotatedMethod<? super X>> getMethods()
    {
        if (constructors == null)
        {
            init();
        }

        return Collections.unmodifiableSet(methods);
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
