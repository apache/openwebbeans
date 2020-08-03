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
package org.apache.webbeans.component.third;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.util.SingleItemSet;

import javax.enterprise.inject.spi.BeanAttributes;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

class ThirdpartyBeanAttributesImpl<T> extends BeanAttributesImpl<T>
{
    private final Set<Annotation> qualifiers;

    ThirdpartyBeanAttributesImpl(BeanAttributes<T> beanAttributes)
    {
        super(beanAttributes);
        this.qualifiers = calculateQualifiers(beanAttributes);
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return qualifiers;
    }

    private Set<Annotation> calculateQualifiers(BeanAttributes<T> beanAttributes)
    {
        Set<Annotation> originalQualifiers = beanAttributes.getQualifiers();
        if (originalQualifiers != null && originalQualifiers.contains(AnyLiteral.INSTANCE))
        {
            return originalQualifiers;
        }

        if (originalQualifiers != null)
        {
            Set<Annotation> newQualifiers = new HashSet<>(originalQualifiers);
            newQualifiers.add(AnyLiteral.INSTANCE);
            return newQualifiers;
        }
        else
        {
            return new SingleItemSet<>(AnyLiteral.INSTANCE);
        }
    }
}
