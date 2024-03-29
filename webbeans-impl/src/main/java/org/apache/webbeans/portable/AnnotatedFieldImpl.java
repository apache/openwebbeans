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
package org.apache.webbeans.portable;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.GenericsUtil;

import java.lang.reflect.Field;

import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedType;

/**
 * Implementation of {@link AnnotatedField} interface.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X>
 */
public class AnnotatedFieldImpl<X> extends AbstractAnnotatedMember<X> implements AnnotatedField<X>
{

    /**
     * Creates a new instance
     * 
     * @param declaringType base type
     * @param javaMember field
     */
    AnnotatedFieldImpl(WebBeansContext webBeansContext, Field javaMember, AnnotatedType<X> declaringType)
    {
        super(webBeansContext, GenericsUtil.resolveType(declaringType.getJavaClass(), javaMember), javaMember,declaringType);
        
        setAnnotations(javaMember.getDeclaredAnnotations());
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public Field getJavaMember()
    {
        return Field.class.cast(javaMember);
    }
    
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotated Field '");
        builder.append(javaMember.getName());
        builder.append("', ");
        builder.append(super.toString());
        
        return builder.toString();
    }
    
}
