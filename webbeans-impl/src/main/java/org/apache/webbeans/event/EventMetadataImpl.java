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
package org.apache.webbeans.event;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.TypeLiteral;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.config.OwbParametrizedTypeImpl;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.ArrayUtil;

public class EventMetadataImpl implements EventMetadata, Serializable
{
    private static final long serialVersionUID = -6401875861180683988L;
 
    private final Type type;
    private final Type selectType;
    private final InjectionPoint injectionPoint;
    private final Set<Annotation> qualifiers;
    private transient WebBeansContext webBeansContext;

    public EventMetadataImpl(Type selectType, Type type, InjectionPoint injectionPoint, Annotation[] qualifiers, WebBeansContext context)
    {
        context.getAnnotationManager().checkQualifierConditions(qualifiers);
        this.selectType = selectType;
        this.type = type;
        this.injectionPoint = injectionPoint;
        this.webBeansContext = context;
        Set<Annotation> completeQualifiers;
        if (qualifiers.length == 0)
        {
            completeQualifiers = Collections.<Annotation>singleton(AnyLiteral.INSTANCE);
        }
        else
        {
            completeQualifiers = new HashSet<>(Arrays.asList(qualifiers));
            if (completeQualifiers.size() != qualifiers.length)
            {
                throw new IllegalArgumentException("duplicate qualifier");
            }

            if (!completeQualifiers.contains(AnyLiteral.INSTANCE))
            {
                completeQualifiers.add(AnyLiteral.INSTANCE);
            }
        }
        this.qualifiers = Collections.unmodifiableSet(completeQualifiers);
    }

    @Override
    public Type getType()
    {
        if (selectType != null
                && ParameterizedType.class.isInstance(selectType))
        {
            ParameterizedType parameterizedType = ParameterizedType.class.cast(selectType);
            Type rawType = parameterizedType.getRawType();
            if (rawType == type)
            {
                return selectType;
            }
            else if (rawType == List.class && type == ArrayList.class)
            {
                return new OwbParametrizedTypeImpl(parameterizedType.getOwnerType(), type, parameterizedType.getActualTypeArguments());
            }
            else if (rawType == Set.class && type == HashSet.class)
            {
                return new OwbParametrizedTypeImpl(parameterizedType.getOwnerType(), type, parameterizedType.getActualTypeArguments());
            }
            else if (rawType == Map.class && (type == HashMap.class || type == ConcurrentHashMap.class || type == TreeMap.class))
            {
                return new OwbParametrizedTypeImpl(parameterizedType.getOwnerType(), type, parameterizedType.getActualTypeArguments());
            } // TODO: better handling of these kind of types, the idea is to check selectType is a parent of type and param number is ==
        }
        return type;
    }

    public Type validatedType()
    {
        return selectType != null? selectType : type;
    }

    @Override
    public InjectionPoint getInjectionPoint()
    {
        return injectionPoint;
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return qualifiers;
    }

    public EventMetadataImpl select(Annotation... bindings)
    {
        return select(type, bindings);
    }
    
    public EventMetadataImpl select(TypeLiteral<?> subtype, Annotation... bindings)
    {
        webBeansContext.getWebBeansUtil().checkTypeVariables(subtype);
        return select(subtype.getType(), bindings);
    }

    public EventMetadataImpl select(Type subtype, Annotation... bindings)
    {
        webBeansContext.getAnnotationManager().checkQualifierConditions(bindings);
        Set<Annotation> newQualifiers = ArrayUtil.asSet(bindings);
        newQualifiers.addAll(qualifiers);
        return new EventMetadataImpl(type, subtype, injectionPoint, newQualifiers.toArray(new Annotation[newQualifiers.size()]), webBeansContext);
    }
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        webBeansContext = WebBeansContext.currentInstance();
    }
}
