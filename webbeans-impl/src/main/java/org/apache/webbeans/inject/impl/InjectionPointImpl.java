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
package org.apache.webbeans.inject.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

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
    
    private void writeObject(java.io.ObjectOutputStream op) throws IOException
    {
        ObjectOutputStream out = new ObjectOutputStream(op);

        //Write the owning bean class
        out.writeObject(this.ownerBean.getBeanClass());

        Set<Annotation> qualifiers = this.ownerBean.getQualifiers();
        for(Annotation qualifier : qualifiers)
        {
            out.writeObject(new Character('-')); // throw-away delimiter so alternating annotations don't get swallowed in the read.
            out.writeObject(qualifier);
            
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
        out.flush();
        
    }
    
    public class CustomObjectInputStream extends ObjectInputStream
    {
        private ClassLoader classLoader;

        public CustomObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException
        {
            super(in);
            this.classLoader = classLoader;
        }
        
        protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException
        {
            return Class.forName(desc.getName(), false, this.classLoader);
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream inp) throws IOException, ClassNotFoundException
    {

        ObjectInputStream in = new CustomObjectInputStream(inp, WebBeansUtil.getCurrentClassLoader());

        Class<?> beanClass = (Class<?>)in.readObject();
        Set<Annotation> anns = new HashSet<Annotation>();
        WebBeansContext webBeansContext = WebBeansContext.currentInstance();
        AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();

        while(!in.readObject().equals(new Character('~')))   // read throw-away '-' or '~' terminal delimiter.
        {
            Annotation ann = (Annotation) in.readObject();  // now read the annotation.
            anns.add(ann);
        }
        
        //process annotations
        this.ownerBean = webBeansContext.getBeanManagerImpl().getBeans(beanClass, anns.toArray(new Annotation[anns.size()])).iterator().next();
        this.qualifierAnnotations = anns;
        
        // determine type of injection point member (0=field, 1=method, 2=constructor) and read...
        int c = in.readByte();
        if(c == 0)
        {
            String fieldName = in.readUTF();
            Field field = ClassUtil.getFieldWithName(beanClass, fieldName);
            
            this.injectionMember = field;
            
            AnnotatedType<?> annotated = annotatedElementFactory.newAnnotatedType(beanClass);
            this.annotated = annotatedElementFactory.newAnnotatedField(field, annotated);
            this.injectionType = field.getGenericType();
            
        }
        else if(c == 1)
        {
            String methodName = in.readUTF();
            Class<?>[] parameters = (Class<?>[])in.readObject();
            
            Method method = ClassUtil.getDeclaredMethod(beanClass, methodName, parameters);
            this.injectionMember = method;
            
            AnnotatedType<?> annotated = annotatedElementFactory.newAnnotatedType(beanClass);
            AnnotatedMethod<Object> am =  (AnnotatedMethod<Object>)annotatedElementFactory.
                                    newAnnotatedMethod((Method)this.injectionMember ,annotated);
            List<AnnotatedParameter<Object>> annParameters = am.getParameters();
            
            this.annotated = annParameters.get(in.readByte());            
            this.injectionType = this.annotated.getBaseType();
            
        }
        else if(c == 2)
        {
            Class<?>[] parameters = (Class<?>[])in.readObject();            
            this.injectionMember = ClassUtil.getConstructor(beanClass, parameters);

            AnnotatedType<Object> annotated = (AnnotatedType<Object>)annotatedElementFactory.newAnnotatedType(beanClass);
            AnnotatedConstructor<Object> am =  (AnnotatedConstructor<Object>)annotatedElementFactory
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