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
package org.apache.webbeans.util;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

public final class WebBeansAnnotatedTypeUtil
{
    
    /**
     * Check producer method is ok for deployment.
     * 
     * @param annotatedMethod producer method
     */
    public static <X> void checkProducerMethodForDeployment(AnnotatedMethod<X> annotatedMethod)
    {
        Asserts.assertNotNull(annotatedMethod, "annotatedMethod argument can not be null");

        if (annotatedMethod.isAnnotationPresent(Inject.class) || 
                annotatedMethod.isAnnotationPresent(Disposes.class) ||  
                annotatedMethod.isAnnotationPresent(Observes.class))
        {
            throw new WebBeansConfigurationException("Producer annotated method : " + annotatedMethod + " can not be annotated with"
                                                     + " @Initializer/@Destructor annotation or has a parameter annotated with @Disposes/@Observes");
        }
    }
    
    public static <X> void configureProducerSpecialization(AbstractOwbBean<X> bean,AnnotatedMethod<X> annotatedMethod)
    {
        List<AnnotatedParameter<X>> annotatedParameters = annotatedMethod.getParameters();
        List<Class<?>> parameters = new ArrayList<Class<?>>();
        for(AnnotatedParameter<X> annotatedParam : annotatedParameters)
        {
            parameters.add(ClassUtil.getClass(annotatedParam.getBaseType()));
        }
        
        Method superMethod = ClassUtil.getClassMethodWithTypes(annotatedMethod.getDeclaringType().getJavaClass().getSuperclass(), 
                annotatedMethod.getJavaMember().getName(), parameters);
        if (superMethod == null)
        {
            throw new WebBeansConfigurationException("Anontated producer method specialization is failed : " + annotatedMethod.getJavaMember().getName()
                                                     + " not found in super class : " + annotatedMethod.getDeclaringType().getJavaClass().getSuperclass().getName()
                                                     + " for annotated method : " + annotatedMethod);
        }
        
        if (!AnnotationUtil.hasAnnotation(superMethod.getAnnotations(), Produces.class))
        {
            throw new WebBeansConfigurationException("Anontated producer method specialization is failed : " + annotatedMethod.getJavaMember().getName()
                                                     + " found in super class : " + annotatedMethod.getDeclaringType().getJavaClass().getSuperclass().getName()
                                                     + " is not annotated with @Produces" + " for annotated method : " + annotatedMethod);
        }

        /* To avoid multiple invocations of setBeanName(), following code is delayed to
         * configSpecializedProducerMethodBeans() when checkSpecializations.
        Annotation[] anns = AnnotationUtil.getQualifierAnnotations(superMethod.getAnnotations());

        for (Annotation ann : anns)
        {
            bean.addQualifier(ann);
        }
        
        WebBeansUtil.configuredProducerSpecializedName(bean, annotatedMethod.getJavaMember(), superMethod);
        */
        
        bean.setSpecializedBean(true);        
    }
    
    /**
     * add the definitions for a &#x0040;Initializer method.
     */
    public static <X> void checkForInjectedInitializerMethod(AbstractInjectionTargetBean<X> component, AnnotatedMethod<X> annotatedMethod)
    {
        Method method = annotatedMethod.getJavaMember();
        
        TypeVariable<?>[] args = method.getTypeParameters();
        if(args.length > 0)
        {
            throw new WebBeansConfigurationException("Error in defining injected methods in annotated method : " + annotatedMethod+ 
                    ". Reason : Initializer methods must not be generic.");
        }
        
        if (annotatedMethod.isAnnotationPresent(Produces.class))
        {
            throw new WebBeansConfigurationException("Error in defining injected methods in annotated method : " + annotatedMethod+ 
            ". Reason : Initializer method can not be annotated with @Produces.");
        
        }

        AnnotationManager annotationManager = component.getWebBeansContext().getAnnotationManager();

        List<AnnotatedParameter<X>> annotatedParameters = annotatedMethod.getParameters();
        for (AnnotatedParameter<X> annotatedParameter : annotatedParameters)
        {
            annotationManager.checkForNewQualifierForDeployment(annotatedParameter.getBaseType(), annotatedMethod.getDeclaringType().getJavaClass(),
                    method.getName(), AnnotationUtil.getAnnotationsFromSet(annotatedParameter.getAnnotations()));

            if(annotatedParameter.isAnnotationPresent(Disposes.class) ||
                    annotatedParameter.isAnnotationPresent(Observes.class))
            {
                throw new WebBeansConfigurationException("Error in defining injected methods in annotated method : " + annotatedMethod+ 
                ". Reason : Initializer method parameters does not contain @Observes or @Dispose annotations.");
                
            }
        }
    }
    
    /**
     * CheckProducerMethodDisposal.
     * @param annotatedMethod disposal method
     */
    public static <X> void checkProducerMethodDisposal(AnnotatedMethod<X> annotatedMethod)
    {
        List<AnnotatedParameter<X>> parameters = annotatedMethod.getParameters();
        boolean found = false;
        for(AnnotatedParameter<X> parameter : parameters)
        {
            if(parameter.isAnnotationPresent(Disposes.class))
            {
                if(found)
                {
                    throw new WebBeansConfigurationException("Error in definining disposal method of annotated method : " + annotatedMethod
                            + ". Multiple disposes annotation.");
                }
                found = true;
            }
        }
        
        if(annotatedMethod.isAnnotationPresent(Inject.class) 
                || AnnotationUtil.hasAnnotatedMethodParameterAnnotation(annotatedMethod, Observes.class) 
                || annotatedMethod.isAnnotationPresent(Produces.class))
        {
            throw new WebBeansConfigurationException("Error in definining disposal method of annotated method : " + annotatedMethod
                    + ". Disposal methods  can not be annotated with" + " @Initializer/@Destructor/@Produces annotation or has a parameter annotated with @Observes.");
        }        
    }
    
    public static <X> void checkObserverMethodConditions(AnnotatedMethod<X> annotatedMethod, Class<?> clazz)
    {
        Asserts.assertNotNull(annotatedMethod, "annotatedMethod parameter can not be null");
        Asserts.nullCheckForClass(clazz);
        
        Method candidateObserverMethod = annotatedMethod.getJavaMember();
        
        if (AnnotationUtil.hasAnnotatedMethodMultipleParameterAnnotation(annotatedMethod, Observes.class))
        {
            throw new WebBeansConfigurationException("Observer method : " + candidateObserverMethod.getName() + " in class : " + clazz.getName()
                                                     + " can not define two parameters with annotated @Observes");
        }

        if (annotatedMethod.isAnnotationPresent(Produces.class) 
                || annotatedMethod.isAnnotationPresent(Inject.class))
        {
            throw new WebBeansConfigurationException("Observer method : " + candidateObserverMethod.getName() + " in class : " + clazz.getName()
                                                     + " can not annotated with annotation in the list {@Produces, @Initializer, @Destructor}");

        }

        if (AnnotationUtil.hasAnnotatedMethodParameterAnnotation(annotatedMethod, Disposes.class))
        {
            throw new WebBeansConfigurationException("Observer method : " + candidateObserverMethod.getName() + " in class : "
                                                     + clazz.getName() + " can not annotated with annotation @Disposes");
        }                
    }
}
