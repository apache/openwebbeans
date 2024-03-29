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

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;

/**
 * Implementation of {@link AnnotatedCallable} interface.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> declaring class
 */
public abstract class AbstractAnnotatedCallable<X> extends AbstractAnnotatedMember<X> implements AnnotatedCallable<X>
{
    /**Annotated parameters*/
    private List<AnnotatedParameter<X>> annotatedParameters = new ArrayList<>();
    
    AbstractAnnotatedCallable(WebBeansContext webBeansContext, Type baseType, Member javaMember, AnnotatedType<X> declaringType)
    {
        super(webBeansContext, baseType,javaMember,declaringType);
    }
    
    protected void setAnnotatedParameters(Type[] genericParameterTypes,Annotation[][] parameterAnnotations)
    {
        int i = 0;
        
        for(Type genericParameter : genericParameterTypes)
        {
            AnnotatedParameterImpl<X> parameterImpl = new AnnotatedParameterImpl<>(getWebBeansContext(), genericParameter, this, i);
            parameterImpl.setAnnotations(parameterAnnotations[i]);
            
            addAnnotatedParameter(parameterImpl);
            
            i++;
        }
    }

    /**
     * Adds new annotated parameter.
     * 
     * @param parameter new annotated parameter
     */
    void addAnnotatedParameter(AnnotatedParameter<X> parameter)
    {
        annotatedParameters.add(parameter);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<AnnotatedParameter<X>> getParameters()
    {
        return annotatedParameters;
    }
    
    public String toString()
    {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append(",");
        builder.append("Annotated Parameters : [");
        for(AnnotatedParameter<X> parameter : annotatedParameters)
        {
            builder.append(parameter.toString());
        }
        builder.append("]");
        
        return builder.toString();
    }
 }
