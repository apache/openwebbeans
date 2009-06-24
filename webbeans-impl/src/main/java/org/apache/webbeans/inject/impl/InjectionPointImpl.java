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

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.ClassUtil;

class InjectionPointImpl implements InjectionPoint
{
    private Set<Annotation> bindingAnnotations = new HashSet<Annotation>();
    
    private Set<Annotation> annotations = new HashSet<Annotation>();
    
    private Bean<?> ownerBean;
    
    private Member injectionMember;
    
    private Type injectionType;
    
    InjectionPointImpl(Bean<?> ownerBean, Type type, Member member)
    {
        //Check for injection point type variable
        if(ClassUtil.isTypeVariable(type))
        {
            throw new WebBeansConfigurationException("Injection point in bean : " + ownerBean + " can not define Type Variable generic type");
        }
        
        this.ownerBean = ownerBean;
        this.injectionMember = member;
        this.injectionType = type;
    }
    
    void addBindingAnnotation(Annotation bindingannotation)
    {
        this.bindingAnnotations.add(bindingannotation);        
    }
    
    void addAnnotation(Annotation annotation)
    {
        this.annotations.add(annotation);
    }
    
    
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationType)
    {
        
        for(Annotation ann : this.annotations)
        {
            if(ann.annotationType().equals(annotationType))
            {
                return (T)ann;
            }
        }
        
        return null;
    }

    public Annotation[] getAnnotations()
    {
        Annotation[] ann = new Annotation[this.annotations.size()];
        ann = this.annotations.toArray(ann);
        
        return ann;
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

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType)
    {
        for(Annotation ann : this.annotations)
        {
            if(ann.annotationType().equals(annotationType))
            {
                return true;
            }
        }
        
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