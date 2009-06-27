/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.portable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

/**
 * Implementation of the {@link AnnotatedType} interface.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> class type
 */
class AnnotatedTypeImpl<X> extends AbstractAnnotated implements AnnotatedType<X>
{
    /**Annotated class*/
    private Class<X> annotatedClass;
    
    /**Constructors*/
    private Set<AnnotatedConstructor<X>> constructors = new HashSet<AnnotatedConstructor<X>>();
    
    /**Fields*/
    private Set<AnnotatedField<? super X>> fields = new HashSet<AnnotatedField<? super X>>();
    
    /**Methods*/
    private Set<AnnotatedMethod<? super X>> methods = new HashSet<AnnotatedMethod<? super X>>();
    
    /**
     * Creates a new instance.
     * 
     * @param annotatedClass class
     */
    AnnotatedTypeImpl(Class<X> annotatedClass)
    {
        super(annotatedClass);        
        this.annotatedClass = annotatedClass;        
    }
    
    /**
     * Adds new annotated constructor.
     * 
     * @param constructor new constructor
     */
    void addAnnotatedConstructor(AnnotatedConstructor<X> constructor)
    {
        this.constructors.add(constructor);
    }
    
    /**
     * Adds new annotated field.
     * 
     * @param constructor new field
     */
    void addAnnotatedField(AnnotatedField<? super X> field)
    {
        this.fields.add(field);
    }

    /**
     * Adds new annotated method.
     * 
     * @param constructor new method
     */
    void addAnnotatedMethod(AnnotatedMethod<? super X> method)
    {
        this.methods.add(method);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AnnotatedConstructor<X>> getConstructors()
    {
        return Collections.unmodifiableSet(this.constructors);
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public Set<AnnotatedField<? super X>> getFields()
    {
        return Collections.unmodifiableSet(this.fields);
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public Class<X> getJavaClass()
    {
        return this.annotatedClass;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public Set<AnnotatedMethod<? super X>> getMethods()
    {
        return Collections.unmodifiableSet(this.methods);
    }

}