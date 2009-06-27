/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.inject.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

class InjectionPointImpl implements InjectionPoint
{
    private Set<Annotation> bindingAnnotations = new HashSet<Annotation>();
    
    private Bean<?> ownerBean;
    
    private Member injectionMember;
    
    private Type injectionType;
    
    private Annotated annotated;
    
    InjectionPointImpl(Bean<?> ownerBean, Type type, Member member, Annotated annotated)
    {
        this.ownerBean = ownerBean;
        this.injectionMember = member;
        this.injectionType = type;
        this.annotated = annotated;
    }
    
    void addBindingAnnotation(Annotation bindingannotation)
    {
        this.bindingAnnotations.add(bindingannotation);        
    }
    
    public Bean<?> getBean()
    {
        
        return this.ownerBean;
    }

    public Set<Annotation> getBindings()
    {
        
        return this.bindingAnnotations;
    }

    public Member getMember()
    {
        return this.injectionMember;
    }

    public Type getType()
    {
        
        return this.injectionType;
    }

    
    @Override
    public Annotated getAnnotated()
    {
        return annotated;
    }

    @Override
    public boolean isDelegate()
    {
        return false;
    }

    @Override
    public boolean isTransient()
    {
        return false;
    }
    

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        if(injectionMember instanceof Constructor)
        {
            Constructor<?> constructor = (Constructor<?>) this.injectionMember;
            buffer.append("Constructor Injection with name :  " + constructor.getName() + ownerBean.toString());
        }
        else if(injectionMember instanceof Method)
        {
            Method method = (Method)this.injectionMember;
            buffer.append("Method Injection with name :  " + method.getName() + ownerBean.toString());
            
        }
        else if(injectionMember instanceof Field)
        {
            Field field = (Field) this.injectionMember;
            buffer.append("Field Injection with name :  " + field.getName() + ownerBean.toString());            
        }
        
        return buffer.toString();
    }
}