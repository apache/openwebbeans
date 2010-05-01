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

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.ClassUtil;

class InjectionPointImpl implements InjectionPoint, Serializable
{
    private static final long serialVersionUID = 1047233127758068484L;

    private Set<Annotation> qualifierAnnotations = new HashSet<Annotation>();
    
    private Bean<?> ownerBean;
    
    private Member injectionMember;
    
    private Type injectionType;
    
    private Annotated annotated;
    
    private boolean transientt;
    
    private boolean delegate;

    InjectionPointImpl(Bean<?> ownerBean, Type type, Member member, Annotated annotated)
    {
        this.ownerBean = ownerBean;
        this.injectionMember = member;
        this.injectionType = type;
        this.annotated = annotated;
    }
    
    void addBindingAnnotation(Annotation qualifierAnnotations)
    {
        this.qualifierAnnotations.add(qualifierAnnotations);
    }
    
    public Bean<?> getBean()
    {
        
        return this.ownerBean;
    }

    public Set<Annotation> getQualifiers()
    {
        
        return this.qualifierAnnotations;
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
        return this.delegate;
    }

    @Override
    public boolean isTransient()
    {
        return this.transientt;
    }
    
    void setDelegate(boolean delegate)
    {
        this.delegate = delegate;
    }
    
    void setTransient(boolean transientt)
    {
        this.transientt = transientt;
    }
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        out.writeObject(this.ownerBean.getBeanClass());
                
        Set<Annotation> annotations = this.ownerBean.getQualifiers();
        for(Annotation ann : annotations)
        {
            out.writeObject(ann.annotationType());
        }
        
        out.writeObject(new Character('~'));
        
        if(this.injectionMember instanceof Field)
        {
            out.writeByte(0);
            out.writeUTF(this.injectionMember.getName()); 
        }
        
        if(this.injectionMember instanceof Method)
        {
            out.writeByte(1);
            out.writeUTF(this.injectionMember.getName());
            Method method = (Method)this.injectionMember;
            Class<?>[] parameters = method.getParameterTypes();
            out.writeObject(parameters);
            
            AnnotatedParameter<?> ap = (AnnotatedParameter<?>)this.annotated;
            out.writeByte(ap.getPosition());
            
        }
        
        if(this.injectionMember instanceof Constructor)
        {
            out.writeByte(2);
            Constructor<?> constr = (Constructor<?>)this.injectionMember;
            Class<?>[] parameters = constr.getParameterTypes();
            out.writeObject(parameters);
            
            AnnotatedParameter<?> ap = (AnnotatedParameter<?>)this.annotated;
            out.writeByte(ap.getPosition());
            
        }
        
        out.writeBoolean(this.delegate);
        out.writeBoolean(this.transientt);
        
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        Class<?> beanClass = (Class<?>)in.readObject();
        Set<Annotation> anns = new HashSet<Annotation>();
        while(!in.readObject().equals(new Character('~')))
        {
            Class<? extends Annotation> ann = (Class<Annotation>) in.readObject();
            anns.add(JavassistProxyFactory.createNewAnnotationProxy(ann));
        }
        
        this.ownerBean = BeanManagerImpl.getManager().getBeans(beanClass, anns.toArray(new Annotation[0])).iterator().next();
        this.qualifierAnnotations = anns;
        
        int c = in.readByte();
        if(c == 0)
        {
            String fieldName = in.readUTF();
            Field field = ClassUtil.getFieldWithName(beanClass, fieldName);
            
            this.injectionMember = field;
            
            AnnotatedType<?> annotated = AnnotatedElementFactory.newAnnotatedType(beanClass);
            this.annotated = AnnotatedElementFactory.newAnnotatedField(field, annotated);
            this.injectionType = field.getGenericType();
            
        }
        else if(c == 1)
        {
            String methodName = in.readUTF();
            Class<?>[] parameters = (Class<?>[])in.readObject();
            
            Method method = ClassUtil.getDeclaredMethod(beanClass, methodName, parameters);
            this.injectionMember = method;
            
            AnnotatedType<?> annotated = AnnotatedElementFactory.newAnnotatedType(beanClass);
            AnnotatedMethod<Object> am =  (AnnotatedMethod<Object>)AnnotatedElementFactory.
                                    newAnnotatedMethod((Method)this.injectionMember ,annotated);
            List<AnnotatedParameter<Object>> annParameters = am.getParameters();
            
            this.annotated = annParameters.get(in.readByte());            
            this.injectionType = this.annotated.getBaseType();
            
        }
        else if(c == 2)
        {
            Class<?>[] parameters = (Class<?>[])in.readObject();            
            this.injectionMember = ClassUtil.getConstructor(beanClass, parameters);

            AnnotatedType<Object> annotated = (AnnotatedType<Object>)AnnotatedElementFactory.newAnnotatedType(beanClass);
            AnnotatedConstructor<Object> am =  (AnnotatedConstructor<Object>)AnnotatedElementFactory
                                            .newAnnotatedConstructor((Constructor<Object>)this.injectionMember,annotated);
            List<AnnotatedParameter<Object>> annParameters = am.getParameters();
            
            this.annotated = annParameters.get(in.readByte());            
            this.injectionType = this.annotated.getBaseType();
        }
        
        this.delegate = in.readBoolean();
        this.transientt = in.readBoolean();
         
    }


    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        if(injectionMember instanceof Constructor)
        {
            Constructor<?> constructor = (Constructor<?>) this.injectionMember;
            buffer.append("Constructor Injection Point, constructor name :  " + constructor.getName() + ", Bean Owner : ["+ ownerBean.toString() + "]");
        }
        else if(injectionMember instanceof Method)
        {
            Method method = (Method)this.injectionMember;
            buffer.append("Method Injection Point, method name :  " + method.getName() + ", Bean Owner : ["+ ownerBean.toString() + "]");
            
        }
        else if(injectionMember instanceof Field)
        {
            Field field = (Field) this.injectionMember;
            buffer.append("Field Injection Point, field name :  " + field.getName() + ", Bean Owner : ["+ ownerBean.toString() + "]");            
        }
        
        return buffer.toString();
    }
}