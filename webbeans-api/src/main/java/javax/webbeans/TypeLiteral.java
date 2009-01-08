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
package javax.webbeans;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * *********************************************************************************************
 * This class and its method signatures is taken from the unpublished Web Beans
 * API related code from the <a
 * href="http://anonsvn.jboss.org/repos/webbeans/">Web Beans RI svn</a>
 * ********************************************************************************************
 */
public abstract class TypeLiteral<T>
{

    private Type actualType;

    protected TypeLiteral()
    {
        Class<?> typeLiteralSubclass = getTypeLiteralSubclass(this.getClass());
        if (typeLiteralSubclass == null)
        {
            throw new RuntimeException(getClass() + " is not a subclass of TypeLiteral");
        }
        actualType = getTypeParameter(typeLiteralSubclass);
        if (actualType == null)
        {
            throw new RuntimeException(getClass() + " is missing type parameter in TypeLiteral");
        }
    }

    public final Type getType()
    {
        return actualType;
    }

    @SuppressWarnings("unchecked")
    public final Class<T> getRawType()
    {
        Type type = getType();
        if (type instanceof Class)
        {
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType)
        {
            return (Class<T>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof GenericArrayType)
        {
            return (Class<T>) Object[].class;
        } else
        {
            throw new RuntimeException("Illegal type");
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<?> getTypeLiteralSubclass(Class<?> clazz)
    {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass.equals(TypeLiteral.class))
        {
            return clazz;
        } else if (superclass.equals(Object.class))
        {
            return null;
        } else
        {
            return (getTypeLiteralSubclass(superclass));
        }
    }

    @SuppressWarnings("unchecked")
    private static Type getTypeParameter(Class<?> superclass)
    {
        Type type = superclass.getGenericSuperclass();
        if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getActualTypeArguments().length == 1)
            {
                return parameterizedType.getActualTypeArguments()[0];
            }
        }
        return null;
    }

    public static void main(String[] args)
    {
        TypeLiteral<List<String>> s = new TypeLiteral<List<String>>()
        {
        };
        System.out.println(s.getRawType()); // List
        System.out.println(s.getType()); // List<String>
    }

    // TODO: equals(), hashCode()
}