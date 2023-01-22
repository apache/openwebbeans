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

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;

/**
 * Implementation of {@link AnnotatedConstructor} interface.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> class info
 */
public class AnnotatedConstructorImpl<X> extends AbstractAnnotatedCallable<X> implements AnnotatedConstructor<X>
{
    /**
     * Create a ew instance.
     * 
     * @param javaMember constructor
     */
    public AnnotatedConstructorImpl(WebBeansContext webBeansContext, Constructor<X> javaMember, AnnotatedType<X> declaringType)
    {        
        super(webBeansContext, javaMember.getDeclaringClass(), javaMember, declaringType);
        setAnnotations(findAnnotations(javaMember));
        setAnnotatedParameters(GenericsUtil.resolveParameterTypes(declaringType.getJavaClass(), javaMember), javaMember.getParameterAnnotations());
    }

    public AnnotatedConstructorImpl(WebBeansContext webBeansContext, AnnotatedConstructor<X> annotatedConstructor, AnnotatedType<X> declaringType)
    {
        super(webBeansContext, annotatedConstructor.getBaseType(), annotatedConstructor.getJavaMember(), declaringType);

        getAnnotations().addAll(annotatedConstructor.getAnnotations());
        for (AnnotatedParameter<X> annotatedParameter : annotatedConstructor.getParameters())
        {
            getParameters().add(new AnnotatedParameterImpl<>(webBeansContext, annotatedParameter.getBaseType(), this, annotatedParameter.getPosition()));
        }
    }

    private static Annotation[] findAnnotations(Constructor<?> javaMember)
    {
        // I really don't like this code, can make thing not respecting java like
        // class A {  A() { super("yep"); } class B { @Foo public B() {} B(String value) {} }
        // but TCKs mandate it
        if (javaMember.getParameterTypes().length == 0)
        {
            Class<?> clazz = javaMember.getDeclaringClass();
            Map<Class<?>, Annotation> annotations = new HashMap<>();
            for (Annotation a : javaMember.getDeclaredAnnotations())
            {
                annotations.put(a.annotationType(), a);
            }

            Class<?> current = clazz.getSuperclass();
            while (current != null && current != Object.class)
            {
                Constructor<?> parentCons;
                try
                {
                    parentCons = current.getConstructor();
                }
                catch (Throwable e)
                {
                    break;
                }

                for (Annotation a : parentCons.getAnnotations())
                {
                    Class<? extends Annotation> annotationType = a.annotationType();
                    if (!annotations.containsKey(annotationType) && annotationType.getAnnotation(Inherited.class) != null)
                    {
                        annotations.put(annotationType, a);
                    }
                }
                current = current.getSuperclass();
            }
            return annotations.values().toArray(new Annotation[annotations.size()]);
        }

        return javaMember.getDeclaredAnnotations();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Constructor<X> getJavaMember()
    {
        return Constructor.class.cast(javaMember);
    }
    
    public String toString()
    {
        return "Annotated Constructor," + super.toString();
    }

}
