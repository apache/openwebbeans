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
package org.apache.webbeans.inject.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.webbeans.util.ClassUtil;

/**
 * Defines the model that is related with injection point decleration. Each
 * injection point decleration defined in the XML file defines the injection
 * point API type and Binding type annotations.
 * <p>
 * If injection point is a parametrized type, actual type arguments are defined
 * in the XML file. See specification for further details.
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class XMLInjectionPointModel
{
    /** Injection point raw/actual class type */
    private Class<?> injectionClassType;

    /** This injection point is parametrized */
    private boolean parametrized;

    /** Actual type arguments defined in the XML */
    private Type[] actualTypeArguments = new Type[0];

    /** Injection point binding types */
    private Set<Annotation> bindingTypes = new HashSet<Annotation>();

    /** This injection model is array */
    private boolean array;

    public XMLInjectionPointModel(Class<?> arrayElementType)
    {
        this.array = true;
        this.injectionClassType = arrayElementType;
    }

    /**
     * Creates new injection point model.
     * 
     * @param injectionClassType injection point class type
     * @param actualTypeArguments injection point actual type arguments
     */
    public XMLInjectionPointModel(Class<?> injectionClassType, Type[] actualTypeArguments)
    {
        this.injectionClassType = injectionClassType;

        if (actualTypeArguments != null && actualTypeArguments.length > 0)
        {
            this.actualTypeArguments = actualTypeArguments;
        }

        if (ClassUtil.isParametrized(this.injectionClassType))
        {
            this.parametrized = true;
        }
    }

    /**
     * Add new binding type annotation to the injection point.
     * 
     * @param bindingType new binding type annotation
     */
    public void addBindingType(Annotation bindingType)
    {
        this.bindingTypes.add(bindingType);
    }

    /**
     * Gets injection point class type.
     * 
     * @return the injectionClassType
     */
    public Class<?> getInjectionClassType()
    {
        return injectionClassType;
    }

    /**
     * Returns the injection point is parametrized type.
     * 
     * @return the parametrized
     */
    public boolean isParametrized()
    {
        return parametrized;
    }

    /**
     * Gets actual type arguments.
     * 
     * @return the actualTypeArguments
     */
    public Type[] getActualTypeArguments()
    {
        return actualTypeArguments;
    }

    /**
     * Gets unmodifiable binding types of the injection point.
     * 
     * @return the bindingTypes
     */
    public Set<Annotation> getBindingTypes()
    {
        return Collections.unmodifiableSet(this.bindingTypes);
    }

    /**
     * @return the array
     */
    public boolean isArray()
    {
        return array;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(actualTypeArguments);
        result = prime * result + ((bindingTypes == null) ? 0 : bindingTypes.hashCode());
        result = prime * result + ((injectionClassType == null) ? 0 : injectionClassType.hashCode());
        result = prime * result + (parametrized ? 1231 : 1237);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
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
        final XMLInjectionPointModel other = (XMLInjectionPointModel) obj;
        if (!Arrays.equals(actualTypeArguments, other.actualTypeArguments))
            return false;
        if (bindingTypes == null)
        {
            if (other.bindingTypes != null)
                return false;
        } else if (!bindingTypes.equals(other.bindingTypes))
            return false;
        if (injectionClassType == null)
        {
            if (other.injectionClassType != null)
                return false;
        } else if (!injectionClassType.equals(other.injectionClassType))
            return false;
        if (parametrized != other.parametrized)
            return false;
        return true;
    }
}