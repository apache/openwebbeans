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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.util.AnnotationUtil;

public class EventMetadataImpl implements EventMetadata
{

    private final Type type;
    private final InjectionPoint injectionPoint;
    private final Set<Annotation> qualifiers;
    
    public EventMetadataImpl(Type type, InjectionPoint injectionPoint, Annotation... qualifiers)
    {
        this.type = type;
        this.injectionPoint = injectionPoint;
        Set<Annotation> completeQualifiers;
        if (qualifiers.length == 0)
        {
            completeQualifiers = AnnotationUtil.DEFAULT_AND_ANY_ANNOTATION;
        }
        else
        {
            completeQualifiers = new HashSet<Annotation>(Arrays.asList(qualifiers));
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

    public Type getType()
    {
        return type;
    }
    
    public InjectionPoint getInjectionPoint()
    {
        return injectionPoint;
    }

    public Set<Annotation> getQualifiers()
    {
        return qualifiers;
    }
}
