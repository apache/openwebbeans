/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.enterprise.inject;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


@SuppressWarnings("unchecked")
public abstract class TypeLiteral<T>
{
    private Type definedType;

    protected TypeLiteral()
    {
        this.definedType = getDefinedType(this.getClass());
    }

    public final Type getType()
    {
        return definedType;
    }

    public final Class<T> getRawType()
    {
        Class<T> rawType = null;

        if (this.definedType instanceof Class)
        {
            rawType = (Class<T>) this.definedType;
        }
        else if (this.definedType instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) this.definedType;
            rawType = (Class<T>) pt.getRawType();

        }
        else if (this.definedType instanceof GenericArrayType)
        {
            rawType = (Class<T>) Object[].class;
        }
        else
        {
            throw new RuntimeException("Illegal type for the Type Literal Class");
        }

        return rawType;
    }

    protected Type getDefinedType(Class<?> clazz)
    {
        Type type = null;

        if (clazz == null)
        {
            throw new RuntimeException("Class parameter clazz can not be null");
        }

        Type superClazz = clazz.getGenericSuperclass();

        if (superClazz.equals(Object.class))
        {
            throw new RuntimeException("Super class must be parametrized type");
        }
        else if (superClazz instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) superClazz;
            Type[] actualArgs = pt.getActualTypeArguments();

            if (actualArgs.length == 1)
            {
                type = actualArgs[0];

            }
            else
            {
                throw new RuntimeException("More than one parametric type");
            }

        }
        else
        {
            type = getDefinedType((Class<?>) superClazz);
        }

        return type;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((definedType == null) ? 0 : definedType.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TypeLiteral other = (TypeLiteral) obj;
        if (definedType == null)
        {
            if (other.definedType != null)
                return false;
        }
        else if (!definedType.equals(other.definedType))
            return false;
        return true;
    }

}