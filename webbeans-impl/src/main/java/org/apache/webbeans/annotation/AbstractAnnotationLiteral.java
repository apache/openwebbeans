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

public class AbstractAnnotationLiteral<T extends Annotation> extends AnnotationLiteral<T>
{
    private Class<T> annotationType;

    protected AbstractAnnotationLiteral()
    {
        this.annotationType = getAnnotationType(getClass());
    }

    /**
     * Implemented for compatibility reasons with other cdi-api jar's.
     * See OWB-802.
     */
    @Override
    public Class<? extends Annotation> annotationType()
    {
        return annotationType;
    }

    @Override
    public int hashCode()
    {
        // implemented for performance reasons
        // currently this is needed because AnnotationLiteral always returns 0 as hashCode
        return 0;
    }

    @Override
    public boolean equals(final Object other)
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
