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
package org.apache.webbeans.inject.instance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.TypeLiteral;
import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.ResolutionUtil;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;

/**
 * Implements the {@link Instance} interface.
 * 
 * @param <T> specific instance type
 */
class InstanceImpl<T> implements Instance<T>
{
    /** Injected class type */
    private Type injectionClazz;

    /** Qualifier annotations appeared on the injection point */
    private Set<Annotation> qualifierAnnotations = new HashSet<Annotation>();

    /**
     * Creates new instance.
     * 
     * @param injectionClazz injection class type
     * @param actualTypeArguments actual type arguments
     * @param annotations qualifier annotations
     */
    InstanceImpl(Type injectionClazz, Annotation... annotations)
    {
        this.injectionClazz = injectionClazz;

        for (Annotation ann : annotations)
        {
            qualifierAnnotations.add(ann);
        }
    }

    /**
     * Returns the bean instance with given qualifier annotations.
     * 
     * @param annotations qualifier annotations
     * @return bean instance
     */
    @SuppressWarnings("unchecked")
    public T get()
    {
        T instance = null;

        Annotation[] anns = new Annotation[this.qualifierAnnotations.size()];
        anns = this.qualifierAnnotations.toArray(anns);
        
        Set<Bean<?>> beans = resolveBeans();

        ResolutionUtil.checkResolvedBeans(beans, ClassUtil.getClazz(this.injectionClazz),anns);

        Bean<?> bean = beans.iterator().next();
        instance = (T)BeanManagerImpl.getManager().getInstance(bean);

        return instance;
    }

    /**
     * Returns set of resolved beans.
     * 
     * @return set of resolved beans
     */
    private Set<Bean<?>> resolveBeans()
    {
        Annotation[] anns = new Annotation[this.qualifierAnnotations.size()];
        anns = this.qualifierAnnotations.toArray(anns);

        InjectionResolver resolver = InjectionResolver.getInstance();
        Set<Bean<?>> beans = resolver.implResolveByType(this.injectionClazz, anns);
        
        return beans;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAmbiguous()
    {
        Set<Bean<?>> beans = resolveBeans();
        
        return beans.size() > 1 ? true : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnsatisfied()
    {
        Set<Bean<?>> beans = resolveBeans();
        
        return beans.size() == 0 ? true : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instance<T> select(Annotation... qualifiers)
    {
        Annotation[] newQualifiersArray = getAdditionalQualifiers(qualifiers);
        InstanceImpl<T> newInstance = new InstanceImpl<T>(this.injectionClazz, newQualifiersArray);

        return newInstance;
    }

    /**
     * Returns total qualifiers array
     * 
     * @param qualifiers additional qualifiers
     * @return total qualifiers array
     */
    private Annotation[] getAdditionalQualifiers(Annotation[] qualifiers)
    {
        AnnotationUtil.checkQualifierConditions(qualifiers);
        Set<Annotation> newQualifiers = new HashSet<Annotation>(this.qualifierAnnotations);

        if (qualifiers != null && qualifiers.length > 0)
        {
            for (Annotation annot : qualifiers)
            {
                if (newQualifiers.contains(annot))
                {
                    throw new IllegalArgumentException("Duplicate Qualifier Exception, " + this.toString());
                }

                newQualifiers.add(annot);
            }
        }

        Annotation[] newQualifiersArray = new Annotation[newQualifiers.size()];
        newQualifiersArray = newQualifiers.toArray(newQualifiersArray);
        
        return newQualifiersArray;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers)
    {
        AnnotationUtil.checkQualifierConditions(qualifiers);
        
        Type sub = subtype;
        
        if(sub == null)
        {
            sub = this.injectionClazz;
        }
        
        Annotation[] newQualifiers = getAdditionalQualifiers(qualifiers);
        
        InstanceImpl<U> newInstance = new InstanceImpl<U>(sub, newQualifiers);
                    
        return newInstance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers)
    {        
        return select(subtype.getRawType(), qualifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator()
    {
        Set<Bean<?>> beans = resolveBeans();
        Set<T> instances = new HashSet<T>();
        for(Bean<?> bean : beans)
        {
            T instance = (T)BeanManagerImpl.getManager().getInstance(bean);
            instances.add(instance);
        }
        
        return instances.iterator();
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Instance<");
        builder.append(ClassUtil.getClazz(this.injectionClazz).getName());
        builder.append(">");

        builder.append(",with qualifier annotations {");
        int i = 0;
        for (Annotation qualifier : this.qualifierAnnotations)
        {
            if (i != 0)
            {
                builder.append(",");
            }

            builder.append(qualifier.toString());
        }

        builder.append("}");

        return builder.toString();
    }
}