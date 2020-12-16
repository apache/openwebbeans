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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import javax.enterprise.inject.spi.Annotated;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.GenericsUtil;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

/**
 * Abstract implementation of the {@link Annotated} contract.
 * 
 * @version $Rev$ $Date$
 */
public abstract class AbstractAnnotated implements Annotated
{
    /**Base type of an annotated element*/
    private final Type baseType;
    
    /**Type closures*/
    private Set<Type> typeClosures;

    /**Set of annotations*/
    private Set<Annotation> annotations = new HashSet<>();
    private Set<Class<?>> repeatables = new HashSet<>();

    private final WebBeansContext webBeansContext;
    
    /**
     * Creates a new annotated element.
     *
     * @param webBeansContext our WebBeansContext
     * @param baseType annotated element type
     */
    protected AbstractAnnotated(WebBeansContext webBeansContext, Type baseType)
    {
        Asserts.assertNotNull(webBeansContext, Asserts.PARAM_NAME_WEBBEANSCONTEXT);
        Asserts.assertNotNull(baseType, "base type");
        
        this.baseType = baseType;
        this.webBeansContext = webBeansContext;
    }

    /**
     * Copy consturctor
     *
     * @param webBeansContext current {@link WebBeansContext}
     * @param annotated to copy
     */
    protected AbstractAnnotated(WebBeansContext webBeansContext, Annotated annotated)
    {
        this.baseType = annotated.getBaseType();
        this.webBeansContext = webBeansContext;

        this.typeClosures = annotated.getTypeClosure();
        this.annotations.addAll(annotated.getAnnotations());
    }

    protected void buildRepeatableAnnotations(Set<Annotation> annotations)
    {
        if (annotations.isEmpty())
        {
            return;
        }

        List<Annotation> repeatables = null;
        for (Annotation annotation : annotations)
        {
            Class<?> type = annotation.annotationType();
            Optional<Method> repeatableMethod = webBeansContext.getAnnotationManager().getRepeatableMethod(type);
            if (repeatableMethod.isPresent())
            {
                try
                {
                    if (repeatables == null)
                    {
                        repeatables = new ArrayList<>();
                    }
                    Annotation[] repeatableAnns =
                        (Annotation[]) repeatableMethod.orElseThrow(IllegalStateException::new).invoke(annotation);
                    for (Annotation repeatableAnn : repeatableAnns)
                    {
                        repeatables.add(repeatableAnn);
                    }
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    WebBeansLoggerFacade.getLogger(AbstractAnnotated.class)
                            .log(Level.FINER, "Problem while handling repeatable Annotations "
                                    + annotation.annotationType());
                }
            }
        }
        if (repeatables != null && !repeatables.isEmpty())
        {
            this.repeatables.addAll(repeatables.stream().map(Annotation::annotationType).collect(toList()));
            this.annotations.addAll(repeatables);
        }
    }

    public Set<Class<?>> getRepeatables()
    {
        return repeatables;
    }

    /**
     * Adds new annotation to set.
     * 
     * @param annotation new annotation
     */
    public void addAnnotation(Annotation annotation)
    {
        annotations.add(annotation);
        buildRepeatableAnnotations(singleton(annotation));
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
        clearAnnotations();
        Collections.addAll(this.annotations, annotations);
        buildRepeatableAnnotations(this.annotations);
    }

    public void clearAnnotations()
    {
        annotations.clear();
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
                typeClosures.removeIf(t -> t instanceof Class && ignoredInterfaces.contains(((Class<?>) t).getName()));
            }
        }
    }

    protected Set<Type> extractTypeClojure(Type baseType)
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
