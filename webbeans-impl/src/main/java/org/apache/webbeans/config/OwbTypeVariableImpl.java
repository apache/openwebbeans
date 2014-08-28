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
package org.apache.webbeans.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

import org.apache.webbeans.util.Asserts;

public class OwbTypeVariableImpl<G extends GenericDeclaration> implements TypeVariable<G>
{

    private String name;
    private G genericDeclaration;
    private Type[] bounds;

    public OwbTypeVariableImpl(TypeVariable<G> typeVariable, Type... bounds)
    {
        name = typeVariable.getName();
        genericDeclaration = typeVariable.getGenericDeclaration();
        Asserts.assertNotNull(name);
        Asserts.assertNotNull(genericDeclaration);
        if (bounds == null || bounds.length == 0)
        {
            this.bounds = typeVariable.getBounds();
        }
        else
        {
            this.bounds = bounds;
        }
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public G getGenericDeclaration()
    {
        return genericDeclaration;
    }

    @Override
    public Type[] getBounds()
    {
        return bounds.clone();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
       return Arrays.hashCode(bounds) ^ name.hashCode() ^ genericDeclaration.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object object)
    {
       if (this == object)
       {
          return true;
       }
       else if (object instanceof TypeVariable)
       {
          TypeVariable<?> that = (TypeVariable<?>)object;
          return name.equals(that.getName()) && genericDeclaration.equals(that.getGenericDeclaration()) && Arrays.equals(bounds, that.getBounds());
       }
       else
       {
          return false;
       }
       
    }

    /** from JDK-1.8 thus no at-Override yet */
    public AnnotatedType[] getAnnotatedBounds()
    {
        //X TODO implement this properly
        return new AnnotatedType[0];
    }

    /** from JDK-1.8 thus no at-Override yet */
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
    {
        //X TODO implement this properly
        return null;
    }

    /** from JDK-1.8 thus no at-Override yet */
    public Annotation[] getAnnotations()
    {
        //X TODO implement this properly
        return new Annotation[0];
    }

    /** from JDK-1.8 thus no at-Override yet */
    public Annotation[] getDeclaredAnnotations()
    {
        //X TODO implement this properly
        return new Annotation[0];
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(name);
        if (bounds.length > 0)
        {
            buffer.append(" extends ");
            boolean first = true;
            for (Type bound: bounds)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    buffer.append(',');
                }
                buffer.append(' ').append(bound);
            }
        }
        return buffer.toString();
    }
}
