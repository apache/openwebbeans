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
package org.apache.webbeans.component.creation;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.util.AnnotationUtil;

/**
 * Abstract implementation.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class info
 */
public class AbstractBeanCreator<T> implements BeanCreator<T>
{
    /**Bean instance*/
    private final AbstractOwbBean<T> bean;    
    
    private Annotated annotated;

    private final DefinitionUtil definitionUtil;
    
    /**
     * Creates a bean instance.
     * 
     * @param bean bean instance
     * @param beanAnnotations annotations
     */
    public AbstractBeanCreator(AbstractOwbBean<T> bean, Annotated annotated)
    {
        this.bean = bean;
        this.annotated = annotated;
        definitionUtil = bean.getWebBeansContext().getDefinitionUtil();
    }

    /**
     * {@inheritDoc}
     */
    public void checkCreateConditions()
    {
        //Sub-class can override this
    }

    /**
     * {@inheritDoc}
     */
    public void defineApiType()
    {
        Set<Type> types = annotated.getTypeClosure();
        bean.getTypes().addAll(types);
        Set<String> ignored = bean.getWebBeansContext().getOpenWebBeansConfiguration().getIgnoredInterfaces();
        for (Iterator<Type> i = bean.getTypes().iterator(); i.hasNext();)
        {
            Type t = i.next();
            if (t instanceof Class && ignored.contains(((Class<?>)t).getName()))
            {
                i.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void defineQualifier()
    {
        definitionUtil.defineQualifiers(bean, AnnotationUtil.getAnnotationsFromSet(annotated.getAnnotations()));
    }

    /**
     * {@inheritDoc}
     */
    public void defineScopeType(String errorMessage, boolean allowLazyInit)
    {
        definitionUtil.defineScopeType(bean, AnnotationUtil.getAnnotationsFromSet(annotated.getAnnotations()), errorMessage, false);
    }

    /**
     * {@inheritDoc}
     */
    public void defineSerializable()
    {
        definitionUtil.defineSerializable(bean);
    }

    /**
     * {@inheritDoc}
     */
    public void defineStereoTypes()
    {
        definitionUtil.defineStereoTypes(bean, AnnotationUtil.getAnnotationsFromSet(annotated.getAnnotations()));
    }
    
    protected <X> void addMethodInjectionPointMetaData(AnnotatedMethod<X> method)
    {
        List<InjectionPoint> injectionPoints = getBean().getWebBeansContext().getInjectionPointFactory().getMethodInjectionPointData(getBean(), method);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            getBean().getWebBeansContext().getDefinitionUtil().addImplicitComponentForInjectionPoint(injectionPoint);
            getBean().addInjectionPoint(injectionPoint);
        }
    }

    /**
     * {@inheritDoc}
     */
    public AbstractOwbBean<T> getBean()
    {
        return bean;
    }

    protected Annotated getAnnotated()
    {
        return annotated;
    }
}
