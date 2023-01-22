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
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.event.EventUtil;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.OwbCustomObjectInputStream;
import org.apache.webbeans.util.WebBeansUtil;

public class InjectionPointImpl implements InjectionPoint, Serializable
{
    private static final long serialVersionUID = 1047233127758068484L;

    private Set<Annotation> qualifierAnnotations;
    
    private Bean<?> ownerBean;
    
    private Member injectionMember;
    
    private Type injectionType;
    
    private Annotated annotated;
    
    private boolean transientt;
    
    private boolean delegate;

    InjectionPointImpl(Bean<?> ownerBean, Collection<Annotation> qualifiers, AnnotatedField<?> annotatedField)
    {
        this(ownerBean, annotatedField.getBaseType(), qualifiers, annotatedField,
                annotatedField.getJavaMember(), annotatedField.isAnnotationPresent(Delegate.class),
                annotatedField.getJavaMember() == null? false : Modifier.isTransient(annotatedField.getJavaMember().getModifiers()));
    }
    
    InjectionPointImpl(Bean<?> ownerBean, Collection<Annotation> qualifiers, AnnotatedParameter<?> parameter)
    {
        this(ownerBean, parameter.getBaseType(), qualifiers, parameter, parameter.getDeclaringCallable().getJavaMember(), parameter.isAnnotationPresent(Delegate.class), false);
    }

    /**
     * This constructor is used to create a 'virtual' InjectionPoint.
     * This is needed if an InjectionPoint was needed during a programmatic lookup or 3rd party beans.
     */
    InjectionPointImpl(Bean<?> bean)
    {
        Asserts.assertNotNull(bean, "bean");
        this.ownerBean = bean;
        this.injectionType = bean.getBeanClass();
        this.qualifierAnnotations = bean.getQualifiers() == null ?
                Collections.<Annotation>emptySet() :
                Collections.unmodifiableSet(new HashSet<>(bean.getQualifiers()));
        this.annotated = null;
        this.injectionMember = null;
        this.delegate = false;
        this.transientt = false;
    }
    
    public InjectionPointImpl(Bean<?> ownerBean, Type type, Collection<Annotation> qualifiers, Annotated annotated, Member member, boolean delegate, boolean isTransient)
    {
        if (type == null)
        {
            throw new IllegalArgumentException("type is null");
        }
        if (member == null)
        {
            throw new IllegalArgumentException("member is null");
        }
        Asserts.assertNotNull(qualifiers, "qualifiers");
        this.ownerBean = ownerBean;
        injectionType = type;
        qualifierAnnotations = Collections.unmodifiableSet(new HashSet<>(qualifiers));
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


        if (ownerBean != null)
        {
            //Write the owning bean class
            out.writeObject(ownerBean.getBeanClass());

            // and it's Qualifiers
            Set<Annotation> qualifiers = ownerBean.getQualifiers();
            writeQualifiers(out, qualifiers);
        }
        else
        {
            if (annotated instanceof AnnotatedMember)
            {
                Class<?> beanClass = ((AnnotatedMember) annotated).getDeclaringType().getJavaClass();
                out.writeObject(beanClass);
                out.writeObject('~');
            }
            else
            {
                throw new NotSerializableException("Cannot detect bean class of InjetionPoint");
            }
        }


        if(injectionMember instanceof Field)
        {
            out.writeByte(0);
            out.writeUTF(injectionMember.getName());
            writeQualifiers(out, qualifierAnnotations);
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

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream inp) throws IOException, ClassNotFoundException
    {

        ObjectInputStream in = new OwbCustomObjectInputStream(inp, WebBeansUtil.getCurrentClassLoader());

        Class<?> ownerBeanClass = (Class<?>)in.readObject();
        Set<Annotation> ownerQualifiers = readQualifiers(in);

        WebBeansContext webBeansContext = WebBeansContext.currentInstance();
        AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();

        //process annotations
        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();
        Set<Bean<?>> beans = beanManager.getBeans(ownerBeanClass,
                                                  ownerQualifiers.toArray(new Annotation[ownerQualifiers.size()]));
        ownerBean = beanManager.resolve(beans);

        // determine type of injection point member (0=field, 1=method, 2=constructor) and read...
        int c = in.readByte();
        if(c == 0)
        {
            String fieldName = in.readUTF();
            Field field = webBeansContext.getSecurityService().doPrivilegedGetDeclaredField(ownerBeanClass, fieldName);

            injectionMember = field;
            
            AnnotatedType<?> annotatedType = annotatedElementFactory.newAnnotatedType(ownerBeanClass);
            annotated = annotatedElementFactory.newAnnotatedField(field, annotatedType);
            injectionType = field.getGenericType();

            qualifierAnnotations = readQualifiers(in);
        }
        else if(c == 1)
        {
            String methodName = in.readUTF();
            Class<?>[] parameters = (Class<?>[])in.readObject();
            
            Method method = webBeansContext.getSecurityService().doPrivilegedGetDeclaredMethod(ownerBeanClass, methodName, parameters);
            injectionMember = method;
            
            AnnotatedType<?> annotatedType = annotatedElementFactory.newAnnotatedType(ownerBeanClass);
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
                injectionMember = ownerBeanClass.getConstructor(parameters);

            }
            catch(NoSuchMethodException e)
            {
                injectionMember = null;
            }

            AnnotatedType<Object> annotatedType = (AnnotatedType<Object>)annotatedElementFactory.newAnnotatedType(ownerBeanClass);
            AnnotatedConstructor<Object> am =  annotatedElementFactory
                                            .newAnnotatedConstructor((Constructor<Object>) injectionMember,annotatedType);
            List<AnnotatedParameter<Object>> annParameters = am.getParameters();

            annotated = annParameters.get(in.readByte());
            injectionType = annotated.getBaseType();
        }

        delegate = in.readBoolean();
        transientt = in.readBoolean();
    }

    private void writeQualifiers(ObjectOutputStream out, Set<Annotation> qualifiers) throws IOException
    {
        for (Annotation qualifier : qualifiers)
        {
            out.writeObject('-'); // throw-away delimiter so alternating annotations don't get swallowed in the read.
            out.writeObject(qualifier);
        }

        // terminate character
        out.writeObject('~');
    }

    private Set<Annotation> readQualifiers(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        Set<Annotation> qualifiers = new HashSet<>();
        while(!in.readObject().equals('~'))   // read throw-away '-' or '~' terminal delimiter.
        {
            Annotation ann = (Annotation) in.readObject();  // now read the annotation.
            qualifiers.add(ann);
        }
        return qualifiers;
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
