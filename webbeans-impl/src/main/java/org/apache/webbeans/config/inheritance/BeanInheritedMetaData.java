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
package org.apache.webbeans.config.inheritance;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Set;

import javax.enterprise.context.ScopeType;
import javax.enterprise.inject.BindingType;
import javax.enterprise.inject.deployment.DeploymentType;
import javax.enterprise.inject.stereotype.Stereotype;
import javax.interceptor.InterceptorBindingType;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.util.AnnotationUtil;

public class BeanInheritedMetaData<T> extends AbstractBeanInheritedMetaData<T>
{
    public BeanInheritedMetaData(AbstractComponent<T> component)
    {
        super(component, component.getReturnType().getSuperclass());
    }

    
    protected void setInheritedBindingTypes()
    {
        if(this.inheritedClazz != Object.class)
        {
            setInheritedTypes(getInheritedBindingTypes(), this.inheritedClazz, BindingType.class);
        }        
    }

     
    protected void setInheritedDeploymentType()
    {
        if(this.inheritedClazz != Object.class)
        {
            setInheritedType(this.inheritedClazz, DeploymentType.class);
        }
        
    }

    
    protected void setInheritedInterceptorBindingTypes()
    {
        if(this.inheritedClazz != Object.class)
        {
            setInheritedTypes(getInheritedInterceptorBindingTypes(), this.inheritedClazz, InterceptorBindingType.class);
        }        
        
    }

    
    protected void setInheritedScopeType()
    {
        if(this.inheritedClazz != Object.class)
        {
            setInheritedType(this.inheritedClazz, ScopeType.class);
        }
    }

    
    protected void setInheritedStereoTypes()
    {
        if(this.inheritedClazz != Object.class)
        {
            setInheritedTypes(getInheritedStereoTypes(), this.inheritedClazz, Stereotype.class);
        }        
        
    }
    
    private void setInheritedType(Class<?> inheritedClass, Class<? extends Annotation> annotationType)
    {
        Annotation[] inheritedAnnotations = AnnotationUtil.getMetaAnnotations(inheritedClass.getDeclaredAnnotations(), annotationType);
        
        if(inheritedAnnotations.length > 0)
        {
            if(inheritedAnnotations[0].annotationType().isAnnotationPresent(Inherited.class))
            {
                Annotation annotation = inheritedAnnotations[0];
                
                if(annotationType.equals(ScopeType.class))
                {
                    this.inheritedScopeType = annotation;
                }
                else if(annotationType.equals(DeploymentType.class))
                {
                    this.inheritedDeploymentType = annotation;
                }                
            }
        }
        else
        {
            if(hasSuperType(inheritedClass))
            {
                setInheritedType(inheritedClass.getSuperclass(), annotationType);
            }
        }
        
    }
    
    private void setInheritedTypes(Set<Annotation> types, Class<?> inheritedClass, Class<? extends Annotation> annotationType)
    {
        Annotation[] annotations = AnnotationUtil.getMetaAnnotations(inheritedClass.getDeclaredAnnotations(), annotationType);
        
        for(Annotation annotation : annotations)
        {
            if(!types.contains(annotation))
            {
                if(AnnotationUtil.isAnnotationExistOnClass(annotation.annotationType(), Inherited.class))
                {
                    types.add(annotation);   
                }
            }
        }
        
        if(hasSuperType(inheritedClass))
        {
            setInheritedTypes(types, inheritedClass.getSuperclass(), annotationType);    
        }        
    }
    

    private boolean hasSuperType(Class<?> clazz)
    {
        return (clazz.getSuperclass() != Object.class) ? true : false;
    }
}
