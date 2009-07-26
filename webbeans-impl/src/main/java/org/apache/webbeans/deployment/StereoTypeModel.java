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
package org.apache.webbeans.deployment;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ScopeType;
import javax.enterprise.inject.deployment.DeploymentType;

import org.apache.webbeans.deployment.stereotype.IStereoTypeModel;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;

/**
 * Default implementation of the {@link IStereoTypeModel} contract.
 * 
 * @version $Rev$ $Date$
 *
 */
public class StereoTypeModel implements IStereoTypeModel
{
    /**Name of the stereotype model. It is usd for registering model with StereoTypeManager*/
    private String name;

    /**Default deployment type*/
    private Annotation defaultDeploymentType;

    /**Default scope type*/
    private Annotation defaultScopeType;

    /**Interceptor Bindings*/
    private Set<Annotation> interceptorBindingTypes = new HashSet<Annotation>();

    /**Inherit StereoType annotations*/
    private Set<Annotation> inherits = new HashSet<Annotation>();

    /**
     * Creates a new instance of the stereotype model for
     * given class.
     * @param clazz stereotype type
     */
    public StereoTypeModel(Class<?> clazz)
    {
        this.name = clazz.getName();

        if (AnnotationUtil.isMetaAnnotationExist(clazz.getDeclaredAnnotations(), DeploymentType.class))
        {
            this.defaultDeploymentType = AnnotationUtil.getMetaAnnotations(clazz.getDeclaredAnnotations(), DeploymentType.class)[0];
        }

        if (AnnotationUtil.isMetaAnnotationExist(clazz.getDeclaredAnnotations(), ScopeType.class))
        {
            this.defaultScopeType = AnnotationUtil.getMetaAnnotations(clazz.getDeclaredAnnotations(), ScopeType.class)[0];
        }

        if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(clazz.getDeclaredAnnotations()))
        {
            Annotation[] ibs = AnnotationUtil.getInterceptorBindingMetaAnnotations(clazz.getDeclaredAnnotations());
            for (Annotation ann : ibs)
            {
                this.interceptorBindingTypes.add(ann);
            }
        }

        if (AnnotationUtil.isStereoTypeMetaAnnotationExist(clazz.getDeclaredAnnotations()))
        {
            Annotation[] isy = AnnotationUtil.getStereotypeMetaAnnotations(clazz.getDeclaredAnnotations());

            Target outerStereo = clazz.getAnnotation(Target.class);
            for (Annotation is : isy)
            {
                Target innerStereo = is.annotationType().getAnnotation(Target.class);

                ElementType[] innerValues = innerStereo.value();
                ElementType[] outerValues = outerStereo.value();

                for (ElementType innerValue : innerValues)
                {
                    if (innerValue.equals(ElementType.METHOD) || innerValue.equals(ElementType.FIELD))
                    {
                        for (ElementType outerValue : outerValues)
                        {
                            if (outerValue.equals(ElementType.TYPE))
                            {
                                throw new WebBeansConfigurationException("Inherited StereoType with class name : " + clazz.getName() + " must have compatible @Target annotation with Stereotype class name : " + clazz.getName());
                            }
                        }
                    }
                    else if (innerValue.equals(ElementType.TYPE))
                    {
                        for (ElementType outerValue : outerValues)
                        {
                            if (outerValue.equals(ElementType.METHOD) || outerValue.equals(ElementType.FIELD))
                            {
                                throw new WebBeansConfigurationException("Inherited StereoType with class name : " + clazz.getName() + " must have compatible @Target annotation with Stereotype class name : " + clazz.getName());
                            }
                        }
                    }
                }

                this.inherits.add(is);

            }

        }

    }
    
    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public Annotation getDefaultDeploymentType()
    {
        return defaultDeploymentType;
    }

    /**
     * {@inheritDoc}
     */
    public Annotation getDefaultScopeType()
    {
        return defaultScopeType;
    }

    /**
     * {@inheritDoc}
     */    
    public Set<Annotation> getInterceptorBindingTypes()
    {
        return this.interceptorBindingTypes;
    }

    /**
     * {@inheritDoc}
     */    
    public Set<Annotation> getInheritedStereoTypes()
    {
        return this.inherits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (!(obj instanceof StereoTypeModel))
        {
            return false;   
        }

        if(obj == null)
        {
            return false;
        }
        
        StereoTypeModel model = (StereoTypeModel) obj;

        return model.name.equals(this.name);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }
}