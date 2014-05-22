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
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.event.EventUtil;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.util.Asserts;
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
    
    InjectionPointImpl(Bean<?> ownerBean, Collection<Annotation> qualifiers, AnnotatedField<?> annotatedField)
    {
        this(ownerBean, annotatedField.getBaseType(), qualifiers, annotatedField,
                annotatedField.getJavaMember(), annotatedField.isAnnotationPresent(Delegate.class), Modifier.isTransient(annotatedField.getJavaMember().getModifiers()));
    }
    
    InjectionPointImpl(Bean<?> ownerBean, Collection<Annotation> qualifiers, AnnotatedParameter<?> parameter)
    {
        this(ownerBean, parameter.getBaseType(), qualifiers, parameter, parameter.getDeclaringCallable().getJavaMember(), parameter.isAnnotationPresent(Delegate.class), false);
    }

    /**
     * This constructor is used to create a 'virtual' InjectionPoint.
     * This is needed if an InjectionPoint was needed during a programmatic lookup.
     */
    InjectionPointImpl(Type type, Collection<Annotation> qualifiers)
    {
        this(null, type, qualifiers, null, null, false, false);
    }
    
    private InjectionPointImpl(Bean<?> ownerBean, Type type, Collection<Annotation> qualifiers, Annotated annotated, Member member, boolean delegate, boolean isTransient)
    {
        Asserts.assertNotNull(type, "required type may not be null");
        Asserts.assertNotNull(qualifiers, "qualifiers may not be null");
        this.ownerBean = ownerBean;
        injectionType = type;
        qualifierAnnotations = Collections.unmodifiableSet(new HashSet<Annotation>(qualifiers));
        this.annotated = annotated;
        injectionMember = member;
        this.delegate = delegate;
        transientt = isTransient;
        if(!WebBeansUtil.checkObtainsInjectionPointConditions(this))
        {
            EventUtil.checkObservableInjectionPointConditions(this);
        }        
    }
    
    @Override
    public Bean<?> getBean()
    {
        return ownerBean;
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        
        return qualifierAnnotations;
    }

    @Override
    public Member getMember()
    {
        return injectionMember;
    }

    @Override
    public Type getType()
    {
        
        return injectionType;
    }

    
    @Override
    public Annotated getAnnotated()
    {
        return annotated;
    }

    @Override
    public boolean isDelegate()
    {
        return delegate;
    }

    @Override
    public boolean isTransient()
    {
        return transientt;
    }
    
    private void writeObject(java.io.ObjectOutputStream op) throws IOException
    {
        ObjectOutputStream out = new ObjectOutputStream(op);

        //Write the owning bean class
        out.writeObject(ownerBean.getBeanClass());

        Set<Annotation> qualifiers = ownerBean.getQualifiers();
        for(Annotation qualifier : qualifiers)
        {
            out.writeObject(Character.valueOf('-')); // throw-away delimiter so alternating annotations don't get swallowed in the read.
            out.writeObject(qualifier);
            
        }
        
        out.writeObject(Character.valueOf('~'));
        
        if(injectionMember instanceof Field)
        {
            out.writeByte(0);
            out.writeUTF(injectionMember.getName());
        }
        
        if(injectionMember instanceof Method)
        {
            out.writeByte(1);
            out.writeUTF(injectionMember.getName());
            Method method = (Method) injectionMember;
            Class<?>[] parameters = method.getParameterTypes();
            out.writeObject(parameters);
            
            AnnotatedParameter<?> ap = (AnnotatedParameter<?>) annotated;
            out.writeByte(ap.getPosition());
            
        }
        
        if(injectionMember instanceof Constructor)
        {
            out.writeByte(2);
            Constructor<?> constr = (Constructor<?>) injectionMember;
            Class<?>[] parameters = constr.getParameterTypes();
            out.writeObject(parameters);
            
            AnnotatedParameter<?> ap = (AnnotatedParameter<?>) annotated;
            out.writeByte(ap.getPosition());
            
        }
        
        out.writeBoolean(delegate);
        out.writeBoolean(transientt);
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
        
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException
        {
            return Class.forName(desc.getName(), false, classLoader);
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

        while(!in.readObject().equals('~'))   // read throw-away '-' or '~' terminal delimiter.
        {
            Annotation ann = (Annotation) in.readObject();  // now read the annotation.
            anns.add(ann);
        }
        
        //process annotations
        ownerBean = webBeansContext.getBeanManagerImpl().getBeans(beanClass, anns.toArray(new Annotation[anns.size()])).iterator().next();
        qualifierAnnotations = anns;
        
        // determine type of injection point member (0=field, 1=method, 2=constructor) and read...
        int c = in.readByte();
        if(c == 0)
        {
            String fieldName = in.readUTF();
            Field field = webBeansContext.getSecurityService().doPrivilegedGetDeclaredField(beanClass, fieldName);

            injectionMember = field;
            
            AnnotatedType<?> annotatedType = annotatedElementFactory.newAnnotatedType(beanClass);
            annotated = annotatedElementFactory.newAnnotatedField(field, annotatedType);
            injectionType = field.getGenericType();
            
        }
        else if(c == 1)
        {
            String methodName = in.readUTF();
            Class<?>[] parameters = (Class<?>[])in.readObject();
            
            Method method = webBeansContext.getSecurityService().doPrivilegedGetDeclaredMethod(beanClass, methodName, parameters);
            injectionMember = method;
            
            AnnotatedType<?> annotatedType = annotatedElementFactory.newAnnotatedType(beanClass);
            AnnotatedMethod<Object> am =  (AnnotatedMethod<Object>)annotatedElementFactory.
                                    newAnnotatedMethod((Method) injectionMember,annotatedType);
            List<AnnotatedParameter<Object>> annParameters = am.getParameters();

            annotated = annParameters.get(in.readByte());
            injectionType = annotated.getBaseType();
            
        }
        else if(c == 2)
        {
            Class<?>[] parameters = (Class<?>[])in.readObject();            
            try
            {
                injectionMember = beanClass.getConstructor(parameters);

            }
            catch(NoSuchMethodException e)
            {
                injectionMember = null;
            }

            AnnotatedType<Object> annotatedType = (AnnotatedType<Object>)annotatedElementFactory.newAnnotatedType(beanClass);
            AnnotatedConstructor<Object> am =  annotatedElementFactory
                                            .newAnnotatedConstructor((Constructor<Object>) injectionMember,annotatedType);
            List<AnnotatedParameter<Object>> annParameters = am.getParameters();

            annotated = annParameters.get(in.readByte());
            injectionType = annotated.getBaseType();
        }

        delegate = in.readBoolean();
        transientt = in.readBoolean();
         
    }


    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        if(injectionMember instanceof Constructor)
        {
            Constructor<?> constructor = (Constructor<?>) injectionMember;
            buffer.append("Constructor Injection Point, constructor name :  ").append(constructor.getName()).append(", Bean Owner : [").append(ownerBean).append("]");
        }
        else if(injectionMember instanceof Method)
        {
            Method method = (Method) injectionMember;
            buffer.append("Method Injection Point, method name :  ").append(method.getName()).append(", Bean Owner : [").append(ownerBean).append("]");
            
        }
        else if(injectionMember instanceof Field)
        {
            Field field = (Field) injectionMember;
            buffer.append("Field Injection Point, field name :  ").append(field.getName()).append(", Bean Owner : [").append(ownerBean).append("]");            
        }
        
        return buffer.toString();
    }
}
