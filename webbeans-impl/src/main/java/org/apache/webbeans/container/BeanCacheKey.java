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
package org.apache.webbeans.container;

import org.apache.webbeans.util.AnnotationUtil;

import javax.enterprise.util.Nonbinding;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

final class BeanCacheKey
{
    private Type type;
    private String path;
    private Annotation qualifier;
    private Annotation qualifiers[];
    private int hashCode = -1;


    public BeanCacheKey( Type type, String path, Annotation... qualifiers )
    {
        this.type = type;
        this.path = path;
        final int length = qualifiers.length;
        if (length == 0)
        {
            // do nothing
        }
        else if (length == 1)
        {
            qualifier = qualifiers[0];
        }
        else
        {
            // to save array creations, we only create an array, if we have more than one annotation
            this.qualifiers = new Annotation[length];
            // TBD: is the order of the qualifiers always the same?
            System.arraycopy(qualifiers, 0, this.qualifiers, 0, length);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        BeanCacheKey cacheKey = (BeanCacheKey) o;

        if (!type.equals(cacheKey.type))
        {
            return false;
        }
        if (qualifier != null ? !qualifier.equals(cacheKey.qualifier) : cacheKey.qualifier != null)
        {
            return false;
        }
        if (!Arrays.equals(qualifiers, cacheKey.qualifiers))
        {
            return false;
        }
        if (path != null ? !path.equals(cacheKey.path) : cacheKey.path != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        if (hashCode != -1)
        {
            return hashCode;
        }

        int result = getTypeHashCode(type);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (qualifier != null ? getQualifierHashCode(qualifier) : 0);
        if (qualifiers != null)
        {
            for (int i = 0; i < qualifiers.length; i++)
            {
                result = 31 * result + getQualifierHashCode(qualifiers[i]);
            }
        }
        hashCode = result;
        return hashCode;
    }

    /**
     * We need this method as some weird JVMs return 0 as hashCode for classes.
     * In that case we return the hashCode of the String.
     */
    private int getTypeHashCode(Type type)
    {
        int typeHash = type.hashCode();
        if (typeHash == 0 && type instanceof Class)
        {
            return ((Class)type).getName().hashCode();
            // the type.toString() is always the same: "java.lang.Class@<hexid>"
            //  return type.toString().hashCode();
        }

        return typeHash;
    }

    /**
     * Calculate the hashCode of a Qualifier
     */
    private int getQualifierHashCode(Annotation a)
    {
        Class annotationClass = getAnnotationClass(a.getClass());

        if (annotationClass == null)
        {
            return getTypeHashCode(a.getClass());
        }

        // the hashCode of an Annotation is calculated solely via the hashCodes
        // of it's members. If there are no members, it is 0.
        // thus we first need to get the annotation-class hashCode
        int hashCode = getTypeHashCode(annotationClass);

        // and now add the hashCode of all it's Nonbinding members
        // the following algorithm is defined by the Annotation class definition
        // see the JavaDoc for Annotation!
        // we only change it so far that we skip evaluating @Nonbinding members
        Method[] methods = annotationClass.getDeclaredMethods();

        for (Method method : methods)
        {
            if (method.isAnnotationPresent(Nonbinding.class))
            {
                continue;
            }

            // Member value
            Object object = callMethod(a, method);
            int value = 0;
            if(object.getClass().isArray())
            {
                Class<?> type = object.getClass().getComponentType();
                if(type.isPrimitive())
                {
                    if(Long.TYPE == type)
                    {
                        value = Arrays.hashCode((Long[]) object);
                    }
                    else if(Integer.TYPE == type)
                    {
                        value = Arrays.hashCode((Integer[])object);
                    }
                    else if(Short.TYPE == type)
                    {
                        value = Arrays.hashCode((Short[])object);
                    }
                    else if(Double.TYPE == type)
                    {
                        value = Arrays.hashCode((Double[])object);
                    }
                    else if(Float.TYPE == type)
                    {
                        value = Arrays.hashCode((Float[])object);
                    }
                    else if(Boolean.TYPE == type)
                    {
                        value = Arrays.hashCode((Long[])object);
                    }
                    else if(Byte.TYPE == type)
                    {
                        value = Arrays.hashCode((Byte[])object);
                    }
                    else if(Character.TYPE == type)
                    {
                        value = Arrays.hashCode((Character[])object);
                    }
                }
                else
                {
                    value = Arrays.hashCode((Object[])object);
                }
            }
            else
            {
                value = object.hashCode();
            }

            hashCode = 29 * hashCode + value;
            hashCode = 29 * hashCode + method.getName().hashCode();
            hashCode = 29 * hashCode + a.hashCode();
        }

        return hashCode;
    }

    private Class getAnnotationClass(Class a)
    {
        for (Class i : a.getInterfaces())
        {
            if (i.isAnnotation())
            {
                return i;
            }
        }
        return null;
    }

    /**
     * Helper method for calculating the hashCode of an annotation.
     */
    private Object callMethod(Object instance, Method method)
    {
        try
        {
            if (!method.isAccessible())
            {
                method.setAccessible(true);
            }

            return method.invoke(instance, AnnotationUtil.EMPTY_OBJECT_ARRAY);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception in method call : " + method.getName(), e);
        }

    }
}
