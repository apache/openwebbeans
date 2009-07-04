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

    /** Binding annotations appeared on the injection point */
    private Set<Annotation> bindingAnnotations = new HashSet<Annotation>();

    /**
     * Creates new instance.
     * 
     * @param injectionClazz injection class type
     * @param actualTypeArguments actual type arguments
     * @param annotations binding annotations
     */
    InstanceImpl(Type injectionClazz, Annotation... annotations)
    {
        this.injectionClazz = injectionClazz;

        for (Annotation ann : annotations)
        {
            bindingAnnotations.add(ann);
        }
    }

    /**
     * Returns the bean instance with given binding annotations.
     * 
     * @param annotations binding annotations
     * @return bean instance
     */
    @SuppressWarnings("unchecked")
    public T get()
    {
        T instance = null;

        Annotation[] anns = new Annotation[this.bindingAnnotations.size()];
        anns = this.bindingAnnotations.toArray(anns);
        
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
        Annotation[] anns = new Annotation[this.bindingAnnotations.size()];
        anns = this.bindingAnnotations.toArray(anns);

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
    public Instance<T> select(Annotation... bindings)
    {
        Annotation[] newBindingsArray = getAdditionalBindings(bindings);
        InstanceImpl<T> newInstance = new InstanceImpl<T>(this.injectionClazz, newBindingsArray);

        return newInstance;
    }

    /**
     * Returns total binding types array
     * 
     * @param bindings additional bindings
     * @return total binding types array
     */
    private Annotation[] getAdditionalBindings(Annotation[] bindings)
    {
        AnnotationUtil.checkBindingTypeConditions(bindings);
        Set<Annotation> newBindings = new HashSet<Annotation>(this.bindingAnnotations);

        if (bindings != null && bindings.length > 0)
        {
            for (Annotation annot : bindings)
            {
                if (newBindings.contains(annot))
                {
                    throw new IllegalArgumentException("Duplicate Binding Exception, " + this.toString());
                }

                newBindings.add(annot);
            }
        }

        Annotation[] newBindingsArray = new Annotation[newBindings.size()];
        newBindingsArray = newBindings.toArray(newBindingsArray);
        
        return newBindingsArray;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... bindings)
    {
        AnnotationUtil.checkBindingTypeConditions(bindings);
        
        if(subtype == null)
        {
            subtype = (Class<U>)this.injectionClazz;
        }
        
        Annotation[] newBindings = getAdditionalBindings(bindings);
        
        InstanceImpl<U> newInstance = new InstanceImpl<U>(subtype, newBindings);
                    
        return newInstance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... bindings)
    {        
        return select(subtype.getRawType(), bindings);
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

        builder.append(",with binding annotations {");
        int i = 0;
        for (Annotation binding : this.bindingAnnotations)
        {
            if (i != 0)
            {
                builder.append(",");
            }

            builder.append(binding.toString());
        }

        builder.append("}");

        return builder.toString();
    }
}