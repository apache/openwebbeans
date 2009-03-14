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
package org.apache.webbeans.inject.instance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.inject.DuplicateBindingTypeException;
import javax.inject.Instance;
import javax.inject.Obtains;
import javax.inject.manager.Bean;


import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.container.ResolutionUtil;
import org.apache.webbeans.util.AnnotationUtil;

/**
 * Implements the {@link Instance} interface.
 * @param <T> injection type
 */
class InstanceImpl<T> implements Instance<T>
{
    /**Injected class type*/
    private Class<T> injectionClazz;
    
    /**Injected point actual type arguments*/
    private Type[] actualTypeArguments = new Type[0];
    
    /**Binding annotations appeared on the injection point*/
    private Set<Annotation> bindingAnnotations = new HashSet<Annotation>();
    
    /**
     * Creates new instance.
     * @param injectionClazz injection class type
     * @param actualTypeArguments actual type arguments
     * @param annotations binding annotations
     */
    InstanceImpl(Class<T> injectionClazz, Type[] actualTypeArguments, Annotation...annotations)
    {
        this.injectionClazz = injectionClazz;
        this.actualTypeArguments = actualTypeArguments;
        
        for(Annotation ann : annotations)
        {
            if(!ann.annotationType().equals(Obtains.class))
            {
                bindingAnnotations.add(ann);   
            }
        }
    }

    /**
     * Returns the bean instance with given binding annotations.
     * @param annotations binding annotations
     * @return bean instance
     */
    public T get(Annotation... annotations)
    {
        AnnotationUtil.checkBindingTypeConditions(annotations);
        
        if(annotations != null && annotations.length > 0)
        {
            for(Annotation annot : annotations)
            {
                if(this.bindingAnnotations.contains(annot))
                {
                    throw new DuplicateBindingTypeException("Duplicate Binding Exception, " + this.toString());
                }
                
                this.bindingAnnotations.add(annot);
            }
        }
        
        Annotation[] anns = new Annotation[this.bindingAnnotations.size()];
        anns = this.bindingAnnotations.toArray(anns);
        
        InjectionResolver resolver = InjectionResolver.getInstance();
        Set<Bean<T>> beans = resolver.implResolveByType(this.injectionClazz, this.actualTypeArguments, anns);
        
        ResolutionUtil.checkResolvedBeans(beans, this.injectionClazz);
        
        Bean<T> bean = beans.iterator().next();
        
        return ManagerImpl.getManager().getInstance(bean);
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Instance<");
        builder.append(this.injectionClazz.getName());
        builder.append(">");
        builder.append(" with actual type arguments {");
        
        int i = 0;
        for(Type type : this.actualTypeArguments)
        {
            Class<?> clazz = (Class<?>)type;
            
            if(i != 0)
            {
                builder.append(",");
            }
            
            builder.append(clazz.getName());            
        }
        
        builder.append("} with binding annotations {");
        i = 0;
        for(Annotation binding : this.bindingAnnotations)
        {
            if(i != 0)
            {
                builder.append(",");
            }
            
            builder.append(binding.toString());            
        }
        
        builder.append("}");
        
        return builder.toString();
    }   
}