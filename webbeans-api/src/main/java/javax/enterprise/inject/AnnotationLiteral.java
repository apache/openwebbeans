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
package javax.enterprise.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.ExecutionException;

/**
 * 
 *
 * @param <T> wrapped annotation class
 * @version $Rev$ $Date$
 */
@SuppressWarnings("unchecked")
public abstract class AnnotationLiteral<T extends Annotation> implements Annotation
{

    private Class<T> annotationType;

    protected AnnotationLiteral()
    {
        this.annotationType = getAnnotationType(getClass());

    }

    @Override
    public Class<? extends Annotation> annotationType()
    {
        return annotationType;
    }

    protected Class<T> getAnnotationType(Class<?> definedClazz)
    {
        Type superClazz = definedClazz.getGenericSuperclass();
        Class<T> clazz = null;

        if (superClazz instanceof ParameterizedType)
        {
            ParameterizedType paramType = (ParameterizedType) superClazz;
            Type[] actualArgs = paramType.getActualTypeArguments();

            if (actualArgs.length == 1)
            {
                Type type = actualArgs[0];
                if (type instanceof Class)
                {
                    clazz = (Class<T>) type;
                    return clazz;
                }
                else
                {
                    throw new ExecutionException("Not class type");
                }

            }
            else
            {
                throw new ExecutionException("More than one parametric type");
            }

        }
        else
        {
            // Look for super, maybe inner
            return getAnnotationType((Class<?>) superClazz);
        }

    }

    @Override
    public boolean equals(Object other)
    {
        Method[] methods = this.annotationType.getDeclaredMethods();

        if (other instanceof Annotation)
        {
            Annotation annotOther = (Annotation) other;
            if (this.annotationType().equals(annotOther.annotationType()))
            {
                for (Method method : methods)
                {
                    Object value = callMethod(this, method);
                    Object annotValue = callMethod(annotOther, method);

                    if (value != null && annotValue != null)
                    {
                        if (!value.equals(annotValue))
                        {
                            return false;
                        }
                    }
                    else if ((value == null && annotValue != null) || (value != null && annotValue == null))
                    {
                        return false;
                    }

                }
                return true;
            }
        }

        return false;
    }

    protected Object callMethod(Object instance, Method method)
    {
        boolean access = method.isAccessible();

        try
        {

            if (!method.isAccessible())
            {
                method.setAccessible(true);
            }

            return method.invoke(instance, new Object[]
                    {
                    });

        }
        catch (Exception e)
        {
            throw new ExecutionException("Exception in method call : " + method.getName());
        }
        finally
        {
            method.setAccessible(access);
        }


    }

    @Override
    public int hashCode()
    {
        Method[] methods = this.annotationType.getDeclaredMethods();

        int hashCode = 0;
        for (Method method : methods)
        {
            // Member name
            int name = 127 * method.getName().hashCode();

            // Member value
            int value = callMethod(this, method).hashCode();
            hashCode += name ^ value;
        }
        return hashCode;
    }

    @Override
    public String toString()
    {
        Method[] methods = this.annotationType.getDeclaredMethods();

        StringBuilder sb = new StringBuilder("@" + annotationType().getName() + "(");
        int lenght = methods.length;

        for (int i = 0; i < lenght; i++)
        {
            // Member name
            sb.append(methods[i].getName()).append("=");

            // Member value
            sb.append(callMethod(this, methods[i]));

            if (i < lenght - 1)
            {
                sb.append(",");
            }
        }

        sb.append(")");

        return sb.toString();
    }
}