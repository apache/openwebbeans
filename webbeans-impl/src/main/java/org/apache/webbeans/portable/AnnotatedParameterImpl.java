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

import java.lang.reflect.Type;

import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedParameter;

/**
 * Implementation of {@link AnnotatedParameter} interface.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> declaring class info
 */
public class AnnotatedParameterImpl<X> extends AbstractAnnotated implements AnnotatedParameter<X>
{
    /**Declaring callable*/
    private final AnnotatedCallable<X> declaringCallable;
    
    /**Parameter position*/
    private final int position;
    
    AnnotatedParameterImpl(WebBeansContext webBeansContext, Type baseType, AnnotatedCallable<X> declaringCallable, int position)
    {
        super(webBeansContext, baseType);
        this.declaringCallable = declaringCallable;
        this.position = position;
    }


    /**
     * Copy ct for Configurators
     */
    AnnotatedParameterImpl(WebBeansContext webBeansContext, AnnotatedParameter<X> originalAnnotatedParameter, AnnotatedMethodImpl declaringCallable)
    {
        super(webBeansContext, originalAnnotatedParameter);
        this.declaringCallable = declaringCallable;
        this.position = originalAnnotatedParameter.getPosition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotatedCallable<X> getDeclaringCallable()
    {
        return declaringCallable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPosition()
    {
        return position;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotated Parameter");
        builder.append(",");
        builder.append(super.toString()).append(",");
        builder.append("Position : ").append(position);
        
        return builder.toString();
    }

    @Override
    protected Class<?> getOwningClass()
    {
        return declaringCallable.getDeclaringType().getJavaClass();
    }

    @Override
    protected Class<?> getDeclaringClass()
    {
        return declaringCallable.getJavaMember().getDeclaringClass();
    }
}
