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
package org.apache.webbeans.annotation;

import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Base class for AnnotationLiterals which have no members.
 * @param <T>
 */
public abstract class EmptyAnnotationLiteral<T extends Annotation> extends AnnotationLiteral<T>
{
    private Class<T> annotationType;

    protected EmptyAnnotationLiteral()
    {
        // Leave this constructor protected, because an EmptyAnnotationLiteral may never directly be instantiated
    }

    /**
     * Implemented for compatibility reasons with other cdi-api jar's.
     * See OWB-802.
     */
    @Override
    public Class<? extends Annotation> annotationType()
    {
        if (annotationType == null)
        {
            annotationType = getAnnotationType(getClass());
        }
        return annotationType;
    }

    /**
     * Implemented for performance reasons.
     * This is needed because an Annotation always returns 0 as hashCode
     * if there is no method in it.
     * Contrary to this the generic {@link javax.enterprise.util.AnnotationLiteral#hashCode()}
     * always does search for methods via reflection and only then returns 0.
     * Not very well performing ...
     * @return always 0
     */
    @Override
    public int hashCode()
    {
        return 0;
    }

    /**
     * Just checks whether the 2 classes have the same annotationType.
     * We do not need to dynamically evaluate the member values via reflection
     * as there are no members in this annotation at all.
     */
    @Override
    public boolean equals(Object other)
    {
        // implemented for performance reasons
        return Annotation.class.isInstance(other) &&
                Annotation.class.cast(other).annotationType().equals(annotationType());
    }

    private Class<T> getAnnotationType(Class<?> definedClazz)
    {
        Type superClazz = definedClazz.getGenericSuperclass();

        Class<T> clazz = null;

        if (superClazz.equals(Object.class))
        {
            throw new RuntimeException("Super class must be parametrized type!");
        }
        else if (superClazz instanceof ParameterizedType)
        {
            ParameterizedType paramType = (ParameterizedType) superClazz;
            Type[] actualArgs = paramType.getActualTypeArguments();

            if (actualArgs.length == 1)
            {
                //Actual annotation type
                Type type = actualArgs[0];

                if (type instanceof Class)
                {
                    clazz = (Class<T>) type;
                    return clazz;
                }
                else
                {
                    throw new RuntimeException("Not class type!");
                }
            }
            else
            {
                throw new RuntimeException("More than one parametric type!");
            }
        }
        else
        {
            return getAnnotationType((Class<?>) superClazz);
        }
    }
}
