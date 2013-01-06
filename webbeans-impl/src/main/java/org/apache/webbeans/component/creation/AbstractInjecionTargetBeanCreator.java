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

import static org.apache.webbeans.util.InjectionExceptionUtils.throwUnsatisfiedResolutionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.ResourceBean;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.spi.api.ResourceReference;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansAnnotatedTypeUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation of {@link InjectionTargetBeanCreator}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class type
 */
public abstract class AbstractInjecionTargetBeanCreator<T> extends AbstractBeanCreator<T> implements InjectionTargetBeanCreator<T>
{    
    
    private WebBeansContext webBeansContext;

    /**
     * Creates a new instance.
     * 
     * @param bean bean instance
     */
    public AbstractInjecionTargetBeanCreator(AbstractInjectionTargetBean<T> bean)
    {
        super(bean, bean.getAnnotatedType());
        webBeansContext = bean.getWebBeansContext();
    }
        
    /**
     * {@inheritDoc}
     */
    public void defineName()
    {
        defineName(WebBeansUtil.getManagedBeanDefaultName(getAnnotated().getJavaClass().getSimpleName()));
    }

    /**
     * {@inheritDoc}
     */
    public void defineDisposalMethods()
    {
        final AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        Set<AnnotatedMethod<? super T>> annotatedMethods = getAnnotated().getMethods();    
        ProducerMethodBean<?> previous = null;
        for (AnnotatedMethod<? super T> annotatedMethod : annotatedMethods)
        {
            Method declaredMethod = annotatedMethod.getJavaMember();
            AnnotatedMethod<T> annt = (AnnotatedMethod<T>)annotatedMethod;
            List<AnnotatedParameter<T>> parameters = annt.getParameters();
            boolean found = false;
            for(AnnotatedParameter<T> parameter : parameters)
            {
                if(parameter.isAnnotationPresent(Disposes.class))
                {
                    found = true;
                    break;
                }
            }
            
            if(found)
            {
                WebBeansAnnotatedTypeUtil.checkProducerMethodDisposal(annotatedMethod);
                Type type = AnnotationUtil.getAnnotatedMethodFirstParameterWithAnnotation(annotatedMethod, Disposes.class);
                Annotation[] annot = annotationManager.getAnnotatedMethodFirstParameterQualifierWithGivenAnnotation(annotatedMethod, Disposes.class);

                InjectionResolver injectionResolver = webBeansContext.getBeanManagerImpl().getInjectionResolver();

                Set<Bean<?>> set = injectionResolver.implResolveByType(type, annot);
                if (set.isEmpty())
                {
                    throwUnsatisfiedResolutionException(type, declaredMethod, annot);
                }
                
                Bean<?> foundBean = set.iterator().next();
                ProducerMethodBean<?> pr = null;

                if (foundBean == null || !(foundBean instanceof ProducerMethodBean))
                {
                    throwUnsatisfiedResolutionException(annotatedMethod.getDeclaringType().getJavaClass(), declaredMethod, annot);
                }

                pr = (ProducerMethodBean<?>) foundBean;

                if (previous == null)
                {
                    previous = pr;
                }
                else
                {
                    // multiple same producer
                    if (previous.equals(pr))
                    {
                        throw new WebBeansConfigurationException("There are multiple disposal method for the producer method : " + pr.getCreatorMethod().getName() + " in class : "
                                                                 + annotatedMethod.getDeclaringType().getJavaClass());
                    }
                }

                Method producerMethod = pr.getCreatorMethod();
                //Disposer methods and producer methods must be in the same class
                if(!producerMethod.getDeclaringClass().getName().equals(declaredMethod.getDeclaringClass().getName()))
                {
                    throw new WebBeansConfigurationException("Producer method component of the disposal method : " + declaredMethod.getName() + " in class : "
                                                             + annotatedMethod.getDeclaringType().getJavaClass() + " must be in the same class!");
                }
                
                pr.setDisposalMethod(declaredMethod);

                addMethodInjectionPointMetaData(annotatedMethod);
                
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void defineInjectedFields()
    {
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        Set<AnnotatedField<? super T>> annotatedFields = getAnnotated().getFields();   
        for(AnnotatedField<? super T> annotatedField: annotatedFields)
        {
            if(Modifier.isPublic(annotatedField.getJavaMember().getModifiers()) && !annotatedField.isStatic())
            {
                if(webBeansContext.getBeanManagerImpl().isNormalScope(getBean().getScope()))
                {
                    throw new WebBeansConfigurationException("If bean has a public field, bean scope must be defined as @Scope. Bean is : "
                            + getBean().toString());
                }
            }                
            
            if(!annotatedField.isAnnotationPresent(Inject.class))
            {
                continue;
            }

            if (annotatedField.isAnnotationPresent(Produces.class))
            {
                throw new WebBeansConfigurationException("Injection fields can not be annotated with @Produces");
            }
            
            Field field = annotatedField.getJavaMember();
            Annotation[] anns = AnnotationUtil.getAnnotationsFromSet(annotatedField.getAnnotations());
            if(Modifier.isPublic(field.getModifiers()))
            {
                if(!getBean().getScope().equals(Dependent.class))
                {
                    throw new WebBeansConfigurationException("Error in annotated field : " + annotatedField
                                                    +" while definining injected field. If bean has a public modifier injection point, bean scope must be defined as @Dependent");
                }
            }

            Annotation[] qualifierAnns = annotationManager.getQualifierAnnotations(anns);

            if (qualifierAnns.length > 0)
            {
                if (qualifierAnns.length > 0)
                {
                    annotationManager.checkForNewQualifierForDeployment(annotatedField.getBaseType(), annotatedField.getDeclaringType().getJavaClass(), field.getName(), anns);
                }

                int mod = field.getModifiers();
                
                if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod))
                {
                    getBean().addInjectedField(field);
                    addFieldInjectionPointMetaData(annotatedField);                                
                }
            }                                    
        }
    }

    /**
     * {@inheritDoc}
     */
    public void defineInjectedMethods()
    {
        Set<AnnotatedMethod<? super T>> annotatedMethods = getAnnotated().getMethods();
        
        for (AnnotatedMethod<? super T> annotatedMethod : annotatedMethods)
        {            
            boolean isInitializer = annotatedMethod.isAnnotationPresent(Inject.class);            

            if (isInitializer)
            {
                //Do not support static
                if(annotatedMethod.isStatic())
                {
                    continue;
                }
                
                WebBeansAnnotatedTypeUtil.checkForInjectedInitializerMethod(getBean(), (AnnotatedMethod<T>)annotatedMethod);
            }
            else
            {
                continue;
            }

            Method method = annotatedMethod.getJavaMember();
            
            if (!Modifier.isStatic(method.getModifiers()))
            {
                getBean().addInjectedMethod(method);
                addMethodInjectionPointMetaData(annotatedMethod);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<ObserverMethod<?>> defineObserverMethods()
    {   
        Set<ObserverMethod<?>> definedObservers = new HashSet<ObserverMethod<?>>();
        Set<AnnotatedMethod<? super T>> annotatedMethods = getAnnotated().getMethods();    
        for (AnnotatedMethod<? super T> annotatedMethod : annotatedMethods)
        {
            AnnotatedMethod<T> annt = (AnnotatedMethod<T>)annotatedMethod;
            List<AnnotatedParameter<T>> parameters = annt.getParameters();
            boolean found = false;
            for(AnnotatedParameter<T> parameter : parameters)
            {
                if(parameter.isAnnotationPresent(Observes.class))
                {
                    found = true;
                    break;
                }
            }
            
            if(found)
            {
                WebBeansAnnotatedTypeUtil.checkObserverMethodConditions(annotatedMethod, annotatedMethod.getDeclaringType().getJavaClass());
                if (getBean().getScope().equals(Dependent.class))
                {
                    //Check Reception
                     AnnotationUtil.getAnnotatedMethodFirstParameterWithAnnotation(annotatedMethod, Observes.class);
                    
                     Observes observes = AnnotationUtil.getAnnotatedMethodFirstParameterAnnotation(annotatedMethod, Observes.class);
                     Reception reception = observes.notifyObserver();
                     if(reception.equals(Reception.IF_EXISTS))
                     {
                         throw new WebBeansConfigurationException("Dependent Bean : " + getBean() + " can not define observer method with @Receiver = IF_EXIST");
                     }
                }
                
                //Add method
                getBean().addObservableMethod(annotatedMethod.getJavaMember());

                //Add injection point data
                addMethodInjectionPointMetaData(annotatedMethod);
                
                //Looking for ObserverMethod
                ObserverMethod<?> definedObserver = webBeansContext.getBeanManagerImpl().getNotificationManager().getObservableMethodForAnnotatedMethod(annotatedMethod, getBean());
                if(definedObserver != null)
                {
                    definedObservers.add(definedObserver);
                }
            }
        }
        
        return definedObservers;
    }

    /**
     * {@inheritDoc}
     */
    public Set<ProducerFieldBean<?>> defineProducerFields()
    {
        DefinitionUtil definitionUtil = webBeansContext.getDefinitionUtil();
        Set<ProducerFieldBean<?>> producerBeans = new HashSet<ProducerFieldBean<?>>();
        Set<AnnotatedField<? super T>> annotatedFields = getAnnotated().getFields();        
        for(AnnotatedField<? super T> annotatedField: annotatedFields)
        {
            if(annotatedField.isAnnotationPresent(Produces.class) && annotatedField.getDeclaringType().equals(getAnnotated()))
            {
                Type genericType = annotatedField.getBaseType();
                
                if(ClassUtil.isTypeVariable(genericType))
                {
                    throw new WebBeansConfigurationException("Producer annotated field : " + annotatedField + " can not be Wildcard type or Type variable");
                }
                if(ClassUtil.isParametrizedType(genericType))
                {
                    if(!ClassUtil.checkParametrizedType((ParameterizedType)genericType))
                    {
                        throw new WebBeansConfigurationException("Producer annotated field : " + annotatedField + " can not be Wildcard type or Type variable");
                    }
                }
                
                Annotation[] anns = AnnotationUtil.getAnnotationsFromSet(annotatedField.getAnnotations());
                Field field = annotatedField.getJavaMember();
                
                //Producer field for resource
                Annotation resourceAnnotation = AnnotationUtil.hasOwbInjectableResource(anns);                
                //Producer field for resource
                if(resourceAnnotation != null)
                {                    
                    //Check for valid resource annotation
                    //WebBeansUtil.checkForValidResources(annotatedField.getDeclaringType().getJavaClass(), field.getType(), field.getName(), anns);
                    if(!Modifier.isStatic(field.getModifiers()))
                    {
                        ResourceReference<T, Annotation> resourceRef = new ResourceReference<T, Annotation>(getBean().getBeanClass(), field.getName(),
                                                                                                            (Class<T>)field.getType(), resourceAnnotation);
                        
                        //Can not define EL name
                        if(annotatedField.isAnnotationPresent(Named.class))
                        {
                            throw new WebBeansConfigurationException("Resource producer annotated field : " + annotatedField + " can not define EL name");
                        }
                        
                        ResourceBeanCreator<T, Annotation> resourceBeanCreator
                            = new ResourceBeanCreator<T, Annotation>(getBean(), resourceRef, annotatedField);
                        ResourceBean<T, Annotation> resourceBean = resourceBeanCreator.getBean();
                        
                        resourceBean.getTypes().addAll(annotatedField.getTypeClosure());
                        resourceBeanCreator.defineQualifiers();
                        resourceBean.setImplScopeType(new DependentScopeLiteral());
                        resourceBean.setProducerField(field);
                        
                        producerBeans.add(resourceBean);                                            
                    }
                }
                else
                {
                    ProducerFieldBeanCreator<T> producerFieldBeanCreator = new ProducerFieldBeanCreator<T>(getBean(), annotatedField);
                    ProducerFieldBean<T> producerFieldBean = producerFieldBeanCreator.getBean();
                    producerFieldBean.setProducerField(field);
                    
                    if (producerFieldBean.getReturnType().isPrimitive())
                    {
                        producerFieldBean.setNullable(false);
                    }                    

                    producerFieldBeanCreator.defineSerializable();
                    producerFieldBeanCreator.defineStereoTypes();
                    webBeansContext.getWebBeansUtil().setBeanEnableFlagForProducerBean(getBean(), producerFieldBean, anns);
                    if (producerFieldBean.getReturnType().isArray())
                    {
                        // TODO this special handling should not be necessary, seems to be a bug in the tck
                        producerFieldBean.getTypes().add(Object.class);
                        producerFieldBean.getTypes().add(producerFieldBean.getReturnType());
                    }
                    else
                    {
                        producerFieldBean.getTypes().addAll(annotatedField.getTypeClosure());
                    }
                    producerFieldBeanCreator.defineScopeType("Annotated producer field: " + annotatedField +  "must declare default @Scope annotation", false);
                    webBeansContext.getWebBeansUtil().checkUnproxiableApiType(producerFieldBean,
                                                                                             producerFieldBean.getScope());
                    WebBeansUtil.checkProducerGenericType(producerFieldBean,annotatedField.getJavaMember());
                    producerFieldBeanCreator.defineQualifiers();
                    producerFieldBeanCreator.defineName(WebBeansUtil.getProducerDefaultName(annotatedField.getJavaMember().getName()));
                    
                    producerBeans.add(producerFieldBean);
                }
            }
        }
        
        return producerBeans;
    }

    /**
     * {@inheritDoc}
     */
    public Set<ProducerMethodBean<?>> defineProducerMethods()
    {
        DefinitionUtil definitionUtil = webBeansContext.getDefinitionUtil();
        Set<ProducerMethodBean<?>> producerBeans = new HashSet<ProducerMethodBean<?>>();
        Set<AnnotatedMethod<? super T>> annotatedMethods = getAnnotated().getMethods();
        
        for(AnnotatedMethod<? super T> annotatedMethod: annotatedMethods)
        {
            if(annotatedMethod.isAnnotationPresent(Produces.class) && annotatedMethod.getDeclaringType().equals(getAnnotated()))
            {
                WebBeansAnnotatedTypeUtil.checkProducerMethodForDeployment(annotatedMethod);
                boolean specialize = false;
                if(annotatedMethod.isAnnotationPresent(Specializes.class))
                {
                    if (annotatedMethod.isStatic())
                    {
                        throw new WebBeansConfigurationException("Specializing annotated producer method : " + annotatedMethod + " can not be static");
                    }
                    
                    specialize = true;
                }
                
                ProducerMethodBeanCreator<T> producerMethodBeanCreator = new ProducerMethodBeanCreator<T>(getBean(), annotatedMethod);
                ProducerMethodBean<T> producerMethodBean = producerMethodBeanCreator.getBean();
                producerMethodBean.setCreatorMethod(annotatedMethod.getJavaMember());
                
                if(specialize)
                {
                    WebBeansAnnotatedTypeUtil.configureProducerSpecialization(producerMethodBean, (AnnotatedMethod<T>)annotatedMethod);
                }
                
                if (ClassUtil.getClass(annotatedMethod.getBaseType()).isPrimitive())
                {
                    producerMethodBean.setNullable(false);
                }
                
                producerMethodBeanCreator.defineSerializable();
                producerMethodBeanCreator.defineStereoTypes();
                webBeansContext.getWebBeansUtil().setBeanEnableFlagForProducerBean(getBean(),
                                                                                   producerMethodBean,
                                                                                   AnnotationUtil.getAnnotationsFromSet(annotatedMethod.getAnnotations()));

                if (producerMethodBean.getReturnType().isArray())
                {
                    // TODO this special handling should not be necessary, seems to be a bug in the tck
                    producerMethodBean.getTypes().add(Object.class);
                    producerMethodBean.getTypes().add(producerMethodBean.getReturnType());
                }
                else
                {
                    producerMethodBean.getTypes().addAll(annotatedMethod.getTypeClosure());
                }
                producerMethodBeanCreator.defineScopeType("Annotated producer method : " + annotatedMethod
                                                          +  "must declare default @Scope annotation", false);
                webBeansContext.getWebBeansUtil().checkUnproxiableApiType(producerMethodBean,
                                                                                         producerMethodBean.getScope());
                WebBeansUtil.checkProducerGenericType(producerMethodBean,annotatedMethod.getJavaMember());
                producerMethodBeanCreator.defineName(WebBeansUtil.getProducerDefaultName(annotatedMethod.getJavaMember().getName()));
                producerMethodBeanCreator.defineQualifiers();
                
                producerMethodBeanCreator.addMethodInjectionPointMetaData(annotatedMethod);
                producerBeans.add(producerMethodBean);
                
            }
            
        }
        
        return producerBeans;
    }
    
    private <X> void addFieldInjectionPointMetaData(AnnotatedField<X> annotField)
    {
        InjectionPoint injectionPoint = webBeansContext.getInjectionPointFactory().getFieldInjectionPointData(getBean(), annotField);
        if (injectionPoint != null)
        {
            webBeansContext.getDefinitionUtil().addImplicitComponentForInjectionPoint(injectionPoint);
            getBean().addInjectionPoint(injectionPoint);
        }
    }

    /**
     * Return type-safe bean instance.
     */
    public AbstractInjectionTargetBean<T> getBean()
    {
        return (AbstractInjectionTargetBean<T>)super.getBean();
    }

    protected AnnotatedType<T> getAnnotated()
    {
        return (AnnotatedType<T>) super.getAnnotated();
    }
}
