/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.portable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

/**
 * Factory for {@link Annotated} elements.
 * 
 * @version $Rev$ $Date$
 */
public final class AnnotatedElementFactory
{
    private AnnotatedElementFactory()
    {
        // Emtpty
    }

    /**
     * Creates and configures new annotated type.
     * 
     * @param <X> class info
     * @param annotatedClass annotated class
     * @return new annotated type
     */
    public static <X> AnnotatedType<X> newAnnotatedType(Class<X> annotatedClass)
    {
        AnnotatedTypeImpl<X> annotatedType = new AnnotatedTypeImpl<X>(annotatedClass);
                
        return annotatedType;
    }

    /**
     * Creates and configures new annotated constructor.
     * 
     * @param <X> declaring class
     * @param constructor constructor
     * @return new annotated constructor
     */
    public static <X> AnnotatedConstructor<X> newAnnotatedConstructor(Constructor<X> constructor)
    {
        return new AnnotatedConstructorImpl<X>(constructor);
    }

    /**
     * Creates and configures new annotated field.
     * 
     * @param <X> declaring class
     * @param field field instance
     * @param declaringClass declaring class
     * @return new annotated field
     */
    public static <X> AnnotatedField<X> newAnnotatedField(Field field, Class<X> declaringClass)
    {
        return new AnnotatedFieldImpl<X>(field);
    }

    /**
     * Creates and configures new annotated method.
     * 
     * @param <X> declaring class
     * @param method annotated method
     * @param declaringClass declaring class info
     * @return new annotated method
     */
    public static <X> AnnotatedMethod<X> newAnnotatedMethod(Method method, Class<X> declaringClass)
    {
        return new AnnotatedMethodImpl<X>(method);
    }

}
