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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.GenericsUtil;

/**
 * Abstract implementation of the {@link Annotated} contract.
 * 
 * @version $Rev$ $Date$
 */
abstract class AbstractAnnotated implements Annotated
{
    /**Base type of an annotated element*/
    private final Type baseType;
    
    /**Type closures*/
    private Set<Type> typeClosures = null;

    /**Set of annotations*/
    private Set<Annotation> annotations = new HashSet<Annotation>();

    private final WebBeansContext webBeansContext;
    
    /**
     * Creates a new annotated element.
     *
     * @param webBeansContext our WebBeansContext
     * @param baseType annotated element type
     */
    protected AbstractAnnotated(WebBeansContext webBeansContext, Type baseType)
    {
        if (webBeansContext == null)
        {
            throw new NullPointerException("no WebBeansContext");
        }
        if (baseType == null)
        {
            throw new NullPointerException("no base type");
        }
        this.baseType = baseType;
        this.webBeansContext = webBeansContext;
    }

    /**
     * Adds new annotation to set.
     * 
     * @param annotation new annotation
     */
    protected void addAnnotation(Annotation annotation)
    {
        annotations.add(annotation);
    }

    protected WebBeansContext getWebBeansContext()
    {
        return webBeansContext;
    }

    /**
     * Adds new annotation to set.
     * 
     * @param annotations new annotations
     */
    protected void setAnnotations(Annotation[] annotations)
    {        
        this.annotations.clear();
        Collections.addAll(this.annotations, annotations);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationType)
    {
        for(Annotation ann : annotations)
        {
            if(ann.annotationType().equals(annotationType))
            {
                return (T)ann;
            }
        }
        
        return null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Annotation> getAnnotations()
    {
        return annotations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getBaseType()
    {
        return baseType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Type> getTypeClosure()
    {
        if (typeClosures == null)
        {
            initTypeClosure();
        }
        return typeClosures;
    }

    protected abstract Class<?> getOwningClass();
    protected abstract Class<?> getDeclaringClass();

    private synchronized void initTypeClosure()
    {
        if (typeClosures == null)
        {
            typeClosures = extractTypeClojure(baseType);
            Set<String> ignoredInterfaces = webBeansContext.getOpenWebBeansConfiguration().getIgnoredInterfaces();
            if (!ignoredInterfaces.isEmpty())
            {
                for (Iterator<Type> i = typeClosures.iterator(); i.hasNext(); )
                {
                    Type t = i.next();
                    if (t instanceof Class && ignoredInterfaces.contains(((Class<?>) t).getName()))
                    {
                        i.remove();
                    }
                }
            }
        }
    }

    protected Set<Type> extractTypeClojure(final Type baseType)
    {
        return GenericsUtil.getTypeClosure(baseType, getOwningClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType)
    {
        for(Annotation ann : annotations)
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
        return ("Base Type : " + baseType.toString() + ",")
                + "Type Closures : " + typeClosures + ","
                + "Annotations : " + annotations.toString();
    }
}
