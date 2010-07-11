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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.inject.Inject;
import javax.inject.Named;
import javax.interceptor.Interceptor;

import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.ResourceBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.creation.AnnotatedTypeBeanCreatorImpl;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.spi.api.ResourceReference;

public final class WebBeansAnnotatedTypeUtil
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansAnnotatedTypeUtil.class);
    
    private WebBeansAnnotatedTypeUtil()
    {
        
    }
    
    public static <T> AnnotatedConstructor<T> getBeanConstructor(AnnotatedType<T> type)
    {
        Asserts.assertNotNull(type,"Type is null");
        AnnotatedConstructor<T> result = null;
        
        Set<AnnotatedConstructor<T>> annConsts = type.getConstructors();
        if(annConsts != null)
        {
            boolean found = false;
            boolean noParamConsIsDefined = false;
            for(AnnotatedConstructor<T> annConst : annConsts)
            {
                if(annConst.isAnnotationPresent(Inject.class))
                {
                    if (found == true)
                    {
                        throw new WebBeansConfigurationException("There are more than one constructor with @Inject annotation in annotation type : "
                                                                 + type);
                    }
                    
                    found = true;
                    result = annConst;
                }
                else
                {
                    if(!found && !noParamConsIsDefined)
                    {
                        List<AnnotatedParameter<T>> parameters = annConst.getParameters();
                        if(parameters != null && parameters.isEmpty())
                        {
                            result = annConst;
                            noParamConsIsDefined = true;
                        }                        
                    }
                }
            }
        }
        
        if (result == null)
        {
            throw new WebBeansConfigurationException("No constructor is found for the annotated type : " + type);
        }
        
        List<AnnotatedParameter<T>> parameters = result.getParameters();
        for(AnnotatedParameter<T> parameter : parameters)
        {
            if (parameter.isAnnotationPresent(Disposes.class))
            {
                throw new WebBeansConfigurationException("Constructor parameter annotations can not contain @Disposes annotation in annotated constructor : "
                                                         + result);
            }
            
            if(parameter.isAnnotationPresent(Observes.class))
            {
                throw new WebBeansConfigurationException("Constructor parameter annotations can not contain @Observes annotation in annotated constructor : " + result);
            }
            
        }

        
        return result;
    }
    
    public static <T> void addConstructorInjectionPointMetaData(AbstractOwbBean<T> owner, AnnotatedConstructor<T> constructor)
    {
        List<InjectionPoint> injectionPoints = InjectionPointFactory.getConstructorInjectionPointData(owner, constructor);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            DefinitionUtil.addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
        }
    }
    
    public static <T,X> void addMethodInjectionPointMetaData(OwbBean<T> owner, AnnotatedMethod<X> method)
    {
        List<InjectionPoint> injectionPoints = InjectionPointFactory.getMethodInjectionPointData(owner, method);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            DefinitionUtil.addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
        }
    }
    
    public static <T,X> void addFieldInjectionPointMetaData(AbstractOwbBean<T> owner, AnnotatedField<X> annotField)
    {
        owner.addInjectionPoint(InjectionPointFactory.getFieldInjectionPointData(owner, annotField));        
    }
    
    @SuppressWarnings("unchecked")
    public static <X> Set<ObserverMethod<?>> defineObserverMethods(AbstractInjectionTargetBean<X> bean,AnnotatedType<X> annotatedType)
    {
        Set<ObserverMethod<?>> definedObservers = new HashSet<ObserverMethod<?>>();
        Set<AnnotatedMethod<? super X>> annotatedMethods = annotatedType.getMethods();    
        for (AnnotatedMethod<? super X> annotatedMethod : annotatedMethods)
        {
            AnnotatedMethod<X> annt = (AnnotatedMethod<X>)annotatedMethod;
            List<AnnotatedParameter<X>> parameters = annt.getParameters();
            boolean found = false;
            for(AnnotatedParameter<X> parameter : parameters)
            {
                if(parameter.isAnnotationPresent(Observes.class))
                {
                    found = true;
                    break;
                }
            }
            
            if(found)
            {
                checkObserverMethodConditions(annotatedMethod, annotatedMethod.getDeclaringType().getJavaClass());
                if(bean.getScope().equals(Dependent.class))
                {
                    //Check Reception
                     AnnotationUtil.getAnnotatedMethodFirstParameterWithAnnotation(annotatedMethod, Observes.class);
                    
                     Observes observes = AnnotationUtil.getAnnotatedMethodFirstParameterAnnotation(annotatedMethod, Observes.class);
                     Reception reception = observes.notifyObserver();
                     if(reception.equals(Reception.IF_EXISTS))
                     {
                         throw new WebBeansConfigurationException("Dependent Bean : " + bean + " can not define observer method with @Receiver = IF_EXIST");
                     }
                }
                
                //Add method
                bean.addObservableMethod(annotatedMethod.getJavaMember());

                //Add injection point data
                addMethodInjectionPointMetaData(bean, annotatedMethod);
                
                //Looking for ObserverMethod
                ObserverMethod<?> definedObserver = NotificationManager.getInstance().getObservableMethodForAnnotatedMethod(annotatedMethod, bean);
                if(definedObserver != null)
                {
                    definedObservers.add(definedObserver);
                }
            }
        }
        
        return definedObservers;
    }
    
    @SuppressWarnings("unchecked")
    public static <X> void defineDisposalMethods(AbstractInjectionTargetBean<X> bean,AnnotatedType<X> annotatedType)
    {
        Set<AnnotatedMethod<? super X>> annotatedMethods = annotatedType.getMethods();    
        ProducerMethodBean<?> previous = null;
        for (AnnotatedMethod<? super X> annotatedMethod : annotatedMethods)
        {
            Method declaredMethod = annotatedMethod.getJavaMember();
            AnnotatedMethod<X> annt = (AnnotatedMethod<X>)annotatedMethod;
            List<AnnotatedParameter<X>> parameters = annt.getParameters();
            boolean found = false;
            for(AnnotatedParameter<X> parameter : parameters)
            {
                if(parameter.isAnnotationPresent(Disposes.class))
                {
                    found = true;
                    break;
                }
            }
            
            if(found)
            {
                checkProducerMethodDisposal(annotatedMethod);
                Type type = AnnotationUtil.getAnnotatedMethodFirstParameterWithAnnotation(annotatedMethod, Disposes.class);
                Annotation[] annot = AnnotationUtil.getAnnotatedMethodFirstParameterQualifierWithGivenAnnotation(annotatedMethod, Disposes.class);

                Set<Bean<?>> set = InjectionResolver.getInstance().implResolveByType(type, annot);
                if (set.isEmpty())
                {
                    throw new UnsatisfiedResolutionException("Producer method component of the disposal method : " + declaredMethod.getName() + 
                                  " in class : " + declaredMethod.getDeclaringClass().getName() + ". Cannot find bean " + type + " with qualifier "
                                  + Arrays.toString(annot));
                }
                
                Bean<?> foundBean = set.iterator().next();
                ProducerMethodBean<?> pr = null;

                if (foundBean == null || !(foundBean instanceof ProducerMethodBean))
                {
                    throw new UnsatisfiedResolutionException("Producer method component of the disposal method : " + declaredMethod.getName() + " in class : "
                                                             + annotatedMethod.getDeclaringType().getJavaClass() + "is not found");
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

                addMethodInjectionPointMetaData(bean, annotatedMethod);
                
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <X> void defineInjectedMethods(AbstractInjectionTargetBean<X> bean,AnnotatedType<X> annotatedType)
    {
        Set<AnnotatedMethod<? super X>> annotatedMethods = annotatedType.getMethods();
        
        for (AnnotatedMethod<? super X> annotatedMethod : annotatedMethods)
        {            
            boolean isInitializer = annotatedMethod.isAnnotationPresent(Inject.class);            

            if (isInitializer)
            {
                //Do not support static
                if(annotatedMethod.isStatic())
                {
                    continue;
                }
                
                checkForInjectedInitializerMethod(bean, (AnnotatedMethod<X>)annotatedMethod);
            }
            else
            {
                continue;
            }

            Method method = annotatedMethod.getJavaMember();
            
            if (!Modifier.isStatic(method.getModifiers()))
            {
                bean.addInjectedMethod(method);
                addMethodInjectionPointMetaData(bean, annotatedMethod);
            }
        }
        
        DefinitionUtil.defineInternalInjectedMethodsRecursively(bean, annotatedType.getJavaClass());
    }
    
    public static <X> void defineInjectedFields(AbstractInjectionTargetBean<X> bean,AnnotatedType<X> annotatedType)
    {
        Set<AnnotatedField<? super X>> annotatedFields = annotatedType.getFields();   
        boolean useOwbSpecificInjection = OpenWebBeansConfiguration.getInstance().isOwbSpecificFieldInjection();
        for(AnnotatedField<? super X> annotatedField: annotatedFields)
        {
            if(!useOwbSpecificInjection)
            {
                if(!annotatedField.isAnnotationPresent(Inject.class))
                {
                    continue;
                }                       
            }
            
            if (annotatedField.isAnnotationPresent(Produces.class) || annotatedField.isAnnotationPresent(Delegate.class))
            {
                continue;
            }
            
            
            Field field = annotatedField.getJavaMember();
            Annotation[] anns = AnnotationUtil.getAnnotationsFromSet(annotatedField.getAnnotations());
            if(ClassUtil.isPublic(field.getModifiers()))
            {
                if(!bean.getScope().equals(Dependent.class))
                {
                    throw new WebBeansConfigurationException("Error in annotated field : " + annotatedField
                                                    +" while definining injected field. If bean has a public modifier injection point, bean scope must be defined as @Dependent");
                }
            }                
            
            Annotation[] qualifierAnns = AnnotationUtil.getQualifierAnnotations(anns);

            if (qualifierAnns.length > 0)
            {
                if (qualifierAnns.length > 0)
                {
                    WebBeansUtil.checkForNewQualifierForDeployment(annotatedField.getBaseType(), annotatedField.getDeclaringType().getJavaClass(), field.getName(), anns);
                }

                int mod = field.getModifiers();
                
                if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod))
                {
                    bean.addInjectedField(field);
                    addFieldInjectionPointMetaData(bean, annotatedField);                                
                }
            }                                    
        }
        
        DefinitionUtil.defineInternalInjectedFieldsRecursively(bean, annotatedType.getJavaClass());
    }
    
    
    @SuppressWarnings("unchecked")
    public static <X> Set<ProducerFieldBean<?>> defineProducerFields(InjectionTargetBean<X> bean, AnnotatedType<X> annotatedType)
    {
        Set<ProducerFieldBean<?>> producerBeans = new HashSet<ProducerFieldBean<?>>();
        Set<AnnotatedField<? super X>> annotatedFields = annotatedType.getFields();        
        for(AnnotatedField<? super X> annotatedField: annotatedFields)
        {
            if(annotatedField.isAnnotationPresent(Produces.class))
            {
                Type genericType = annotatedField.getBaseType();
                
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
                    if(!ClassUtil.isStatic(field.getModifiers()))
                    {
                        ResourceReference<X,Annotation> resourceRef = new ResourceReference<X, Annotation>(bean.getBeanClass(), field.getName(),
                                                                                                           (Class<X>)field.getType(), resourceAnnotation);
                        
                        //Can not define EL name
                        if(annotatedField.isAnnotationPresent(Named.class))
                        {
                            throw new WebBeansConfigurationException("Resource producer annotated field : " + annotatedField + " can not define EL name");
                        }
                        
                        ResourceBean<X,Annotation> resourceBean = new ResourceBean((Class<X>)field.getType(),bean, resourceRef);
                        
                        resourceBean.getTypes().addAll(annotatedField.getTypeClosure());
                        DefinitionUtil.defineQualifiers(resourceBean, anns);                    
                        resourceBean.setImplScopeType(new DependentScopeLiteral());
                        resourceBean.setProducerField(field);
                        
                        producerBeans.add(resourceBean);                                            
                    }
                }
                else
                {
                    ProducerFieldBean<X> producerFieldBean = new ProducerFieldBean<X>(bean, (Class<X>)ClassUtil.getClass(annotatedField.getBaseType()));
                    producerFieldBean.setProducerField(field);
                    
                    if (ClassUtil.isPrimitive(ClassUtil.getClass(annotatedField.getBaseType())))
                    {
                        producerFieldBean.setNullable(false);
                    }                    
                    
                    DefinitionUtil.defineSerializable(producerFieldBean);
                    DefinitionUtil.defineStereoTypes(producerFieldBean, anns);
                    WebBeansUtil.setBeanEnableFlagForProducerBean(bean, producerFieldBean, anns);
                    Set<Type> types = annotatedField.getTypeClosure();
                    producerFieldBean.getTypes().addAll(types);
                    DefinitionUtil.defineScopeType(producerFieldBean, anns, "Annotated producer field: " + annotatedField +  "must declare default @Scope annotation");
                    WebBeansUtil.checkUnproxiableApiType(producerFieldBean, producerFieldBean.getScope());
                    WebBeansUtil.checkProducerGenericType(producerFieldBean,annotatedField.getJavaMember());        
                    DefinitionUtil.defineQualifiers(producerFieldBean, anns);
                    DefinitionUtil.defineName(producerFieldBean, anns, WebBeansUtil.getProducerDefaultName(annotatedField.getJavaMember().getName()));
                    
                    producerBeans.add(producerFieldBean);
                }
            }
        }
        
        return producerBeans;
    }
    
    
    @SuppressWarnings("unchecked")
    public static <X> Set<ProducerMethodBean<?>> defineProducerMethods(InjectionTargetBean<X> bean, AnnotatedType<X> annotatedType)
    {
        Set<ProducerMethodBean<?>> producerBeans = new HashSet<ProducerMethodBean<?>>();
        Set<AnnotatedMethod<? super X>> annotatedMethods = annotatedType.getMethods();
        
        for(AnnotatedMethod<? super X> annotatedMethod: annotatedMethods)
        {
            if(annotatedMethod.isAnnotationPresent(Produces.class))
            {
                checkProducerMethodForDeployment(annotatedMethod);
                boolean specialize = false;
                if(annotatedMethod.isAnnotationPresent(Specializes.class))
                {
                    if (annotatedMethod.isStatic())
                    {
                        throw new WebBeansConfigurationException("Specializing annotated producer method : " + annotatedMethod + " can not be static");
                    }
                    
                    specialize = true;
                }
                
                ProducerMethodBean<X> producerMethodBean = new ProducerMethodBean<X>(bean, (Class<X>)ClassUtil.getClass(annotatedMethod.getBaseType()));
                producerMethodBean.setCreatorMethod(annotatedMethod.getJavaMember());
                
                if(specialize)
                {
                    configureProducerSpecialization(producerMethodBean, (AnnotatedMethod<X>)annotatedMethod);
                }
                
                if (ClassUtil.isPrimitive(ClassUtil.getClass(annotatedMethod.getBaseType())))
                {
                    producerMethodBean.setNullable(false);
                }
                
                DefinitionUtil.defineSerializable(producerMethodBean);
                DefinitionUtil.defineStereoTypes(producerMethodBean, AnnotationUtil.getAnnotationsFromSet(annotatedMethod.getAnnotations()));
                WebBeansUtil.setBeanEnableFlagForProducerBean(bean, producerMethodBean, AnnotationUtil.getAnnotationsFromSet(annotatedMethod.getAnnotations()));
                
                Set<Type> types = annotatedMethod.getTypeClosure();
                producerMethodBean.getTypes().addAll(types);
                DefinitionUtil.defineScopeType(producerMethodBean,
                                               AnnotationUtil.getAnnotationsFromSet(annotatedMethod.getAnnotations()),
                                                                                    "Annotated producer method : " + annotatedMethod +  "must declare default @Scope annotation");
                WebBeansUtil.checkUnproxiableApiType(producerMethodBean, producerMethodBean.getScope());
                WebBeansUtil.checkProducerGenericType(producerMethodBean,annotatedMethod.getJavaMember());        
                DefinitionUtil.defineQualifiers(producerMethodBean, AnnotationUtil.getAnnotationsFromSet(annotatedMethod.getAnnotations()));
                DefinitionUtil.defineName(producerMethodBean,
                                          AnnotationUtil.getAnnotationsFromSet(annotatedMethod.getAnnotations()),
                                                                               WebBeansUtil.getProducerDefaultName(annotatedMethod.getJavaMember().getName()));
                
                addMethodInjectionPointMetaData(producerMethodBean, annotatedMethod);
                producerBeans.add(producerMethodBean);
                
            }
            
        }
        
        return producerBeans;
    }
    
    
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
    private static <X> void checkForInjectedInitializerMethod(AbstractInjectionTargetBean<X> component, AnnotatedMethod<X> annotatedMethod)
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
        
        List<AnnotatedParameter<X>> annotatedParameters = annotatedMethod.getParameters();
        for (AnnotatedParameter<X> annotatedParameter : annotatedParameters)
        {
            WebBeansUtil.checkForNewQualifierForDeployment(annotatedParameter.getBaseType(), annotatedMethod.getDeclaringType().getJavaClass(), 
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
     
    /**
     * Checks the implementation class for checking conditions.
     * 
     * @param type implementation class
     * @throws WebBeansConfigurationException if any configuration exception occurs
     */
    public static <X> void checkManagedBeanCondition(AnnotatedType<X> type) throws WebBeansConfigurationException
    {        
        int modifier = type.getJavaClass().getModifiers();

        if (type.isAnnotationPresent(Decorator.class) && type.isAnnotationPresent(Interceptor.class))
        {
            throw new WebBeansConfigurationException("Annotated type "+ type +  " may not annotated with both @Interceptor and @Decorator annotation");
        }

        if (!type.isAnnotationPresent(Decorator.class) && !type.isAnnotationPresent(Interceptor.class))
        {
            checkManagedWebBeansInterceptorConditions(type);
        }

        if (ClassUtil.isInterface(modifier))
        {
            throw new WebBeansConfigurationException("ManagedBean implementation class : " + type.getJavaClass().getName() + " may not defined as interface");
        }
    }
    
    public static <X> void checkManagedWebBeansInterceptorConditions(AnnotatedType<X> type)
    {
        Annotation[] anns = AnnotationUtil.getAnnotationsFromSet(type.getAnnotations());

        Class<?> clazz = type.getJavaClass();
        boolean hasClassInterceptors = false;
        if (AnnotationUtil.getInterceptorBindingMetaAnnotations(anns).length > 0)
        {
            hasClassInterceptors = true;
        }
        else
        {
            Annotation[] stereoTypes = AnnotationUtil.getStereotypeMetaAnnotations(anns);
            for (Annotation stero : stereoTypes)
            {
                if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(stero.annotationType().getDeclaredAnnotations()))
                {
                    hasClassInterceptors = true;
                    break;
                }
            }
        }
         
        if(ClassUtil.isFinal(clazz.getModifiers()) && hasClassInterceptors)
        {
            throw new WebBeansConfigurationException("Final managed bean class with name : " + clazz.getName() + " can not define any InterceptorBindings");
        }
        
        Set<AnnotatedMethod<? super X>> methods = type.getMethods();
        for(AnnotatedMethod<? super X> methodA : methods)
        {
            Method method = methodA.getJavaMember(); 
            int modifiers = method.getModifiers();
            if (!ClassUtil.isStatic(modifiers) && !ClassUtil.isPrivate(modifiers) && ClassUtil.isFinal(modifiers))
            {
                if (hasClassInterceptors)
                {
                    throw new WebBeansConfigurationException("Maanged bean class : " + clazz.getName()
                                                    + " can not define non-static, non-private final methods. Because it is annotated with at least one @InterceptorBinding");
                }

                if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(AnnotationUtil.getAnnotationsFromSet(methodA.getAnnotations())))
                {
                    throw new WebBeansConfigurationException("Method : " + method.getName() + "in managed bean class : " + clazz.getName()
                                                    + " can not be defined as non-static, non-private and final . Because it is annotated with at least one @InterceptorBinding");
                }
            }
            
        }
    }
    
    public static <T> ManagedBean<T> defineAbstractDecorator(AnnotatedType<T> type)
    {
        
        ManagedBean<T> bean = defineManagedBean(type);
        Class clazz = JavassistProxyFactory.getInstance().createAbstractDecoratorProxyClass(bean);
        bean.setConstructor(WebBeansUtil.defineConstructor(clazz));
        return bean;
    }
    
    public static <T> ManagedBean<T>  defineManagedBean(AnnotatedType<T> type)
    {
        Class<T> clazz = type.getJavaClass();
        
        ManagedBean<T> managedBean = new ManagedBean<T>(clazz,WebBeansType.MANAGED);    
        managedBean.setAnnotatedType(type);
        AnnotatedTypeBeanCreatorImpl<T> managedBeanCreator = new AnnotatedTypeBeanCreatorImpl<T>(managedBean);            
        managedBeanCreator.setAnnotatedType(type);
        
        managedBeanCreator.defineSerializable();

        //Define meta-data
        managedBeanCreator.defineStereoTypes();

        //Scope type
        managedBeanCreator.defineScopeType(logger.getTokenString(OWBLogConst.TEXT_MB_IMPL) + clazz.getName()
                                           + logger.getTokenString(OWBLogConst.TEXT_SAME_SCOPE));                                        
        //Check for Enabled via Alternative
        WebBeansUtil.setInjectionTargetBeanEnableFlag(managedBean);        
        managedBeanCreator.defineApiType();
        managedBeanCreator.checkCreateConditions();
        managedBeanCreator.defineQualifier();
        managedBeanCreator.defineName(WebBeansUtil.getManagedBeanDefaultName(clazz.getSimpleName()));
        managedBeanCreator.defineConstructor();            
        managedBeanCreator.defineProducerMethods();       
        managedBeanCreator.defineProducerFields();           
        managedBeanCreator.defineInjectedFields();
        managedBeanCreator.defineInjectedMethods();
        managedBeanCreator.defineObserverMethods();
        DefinitionUtil.defineDecoratorStack(managedBean);
        DefinitionUtil.defineBeanInterceptorStack(managedBean);
                                        
        managedBeanCreator.defineDisposalMethods();//Define disposal method after adding producers
        
        return managedBean;
    }
    
    /**
     * Return true if this annotated type represents a decorator.
     * @param annotatedType annotated type
     * @return true if decorator
     */
    public static boolean isAnnotatedTypeDecorator(AnnotatedType<?> annotatedType)
    {
        if(annotatedType.isAnnotationPresent(Decorator.class))
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Return true if this annotated type represents a decorator.
     * @param annotatedType annotated type
     * @return true if decorator
     */
    public static boolean isAnnotatedTypeDecoratorOrInterceptor(AnnotatedType<?> annotatedType)
    {
        if(isAnnotatedTypeDecorator(annotatedType) ||
                isAnnotatedTypeInterceptor(annotatedType))            
        {
            return true;
        }
        else if(InterceptorsManager.getInstance().isInterceptorEnabled(annotatedType.getJavaClass()))
        {
            return true;
        }
        else if(DecoratorsManager.getInstance().isDecoratorEnabled(annotatedType.getJavaClass()))
        {
            return true;
        }

        
        return false;
    }
    
    
    /**
     * Return true if this annotated type represents a decorator.
     * @param annotatedType annotated type
     * @return true if decorator
     */
    public static boolean isAnnotatedTypeInterceptor(AnnotatedType<?> annotatedType)
    {
        if(annotatedType.isAnnotationPresent(Interceptor.class))
        {
            return true;
        }
        
        return false;
    }
    
    
    /**
     * Define decorator bean.
     * @param <T> type info
     * @param annotatedType decorator class
     */
    public static <T> void defineDecorator(AnnotatedType<T> annotatedType)
    {
        if (DecoratorsManager.getInstance().isDecoratorEnabled(annotatedType.getJavaClass()))
        {
            ManagedBean<T> delegate = null;
            
            Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
            for(AnnotatedMethod<? super T> methodA : methods)
            {
                Method method = methodA.getJavaMember();
                if(AnnotationUtil.hasMethodAnnotation(method, Produces.class))
                {
                    throw new WebBeansConfigurationException("Decorator class : " + annotatedType.getJavaClass() + " can not have producer methods but it has one with name : "
                                                             + method.getName());
                }
                
                if(AnnotationUtil.hasMethodParameterAnnotation(method, Observes.class))
                {
                    throw new WebBeansConfigurationException("Decorator class : " + annotatedType.getJavaClass() + " can not have observer methods but it has one with name : "
                                                             + method.getName());
                }
                
            }
            
            if(Modifier.isAbstract(annotatedType.getJavaClass().getModifiers()))
            {
                delegate = defineAbstractDecorator(annotatedType);
            } 
            else 
            {
                delegate = defineManagedBean(annotatedType);
            }

            if (delegate != null)
            {
                WebBeansDecoratorConfig.configureDecoratorClass(delegate);
            }
            else
            {
                if (logger.wblWillLogTrace())
                {
                    logger.trace("Unable to configure decorator with class : [{0}]", annotatedType.getJavaClass());
                }
            }
        }
    }
    
    public static <T> void defineInterceptor(AnnotatedType<T> annotatedType)
    {
        Class<?> clazz = annotatedType.getJavaClass();
        if (InterceptorsManager.getInstance().isInterceptorEnabled(clazz))
        {
            ManagedBean<T> delegate = null;

            InterceptorUtil.checkAnnotatedTypeInterceptorConditions(annotatedType);
            delegate = defineManagedBean(annotatedType);

            if (delegate != null)
            {
                WebBeansInterceptorConfig.configureInterceptorClass(delegate, 
                        AnnotationUtil.getInterceptorBindingMetaAnnotations(annotatedType.getAnnotations().toArray(new Annotation[0])));
            }
            else
            {
                if (logger.wblWillLogTrace())
                {
                    logger.trace("Unable to configure interceptor with class : [{0}]", annotatedType.getJavaClass());
                }
            }
        }

    }    
    
    @SuppressWarnings("unchecked")
    public static <X> Method getDisposalWithGivenAnnotatedMethod(AnnotatedType<X> annotatedType, Type beanType, Annotation[] qualifiers)
    {
        Set<AnnotatedMethod<? super X>> annotatedMethods = annotatedType.getMethods();  
        
        if(annotatedMethods != null)
        {
            for (AnnotatedMethod<? super X> annotatedMethod : annotatedMethods)
            {
                AnnotatedMethod<X> annt = (AnnotatedMethod<X>)annotatedMethod;
                List<AnnotatedParameter<X>> parameters = annt.getParameters();
                if(parameters != null)
                {
                    boolean found = false;                    
                    for(AnnotatedParameter<X> parameter : parameters)
                    {
                        if(parameter.isAnnotationPresent(Disposes.class))
                        {
                            found = true;
                            break;
                        }
                    }
                    
                    if(found)
                    {
                        Type type = AnnotationUtil.getAnnotatedMethodFirstParameterWithAnnotation(annotatedMethod, Disposes.class);
                        Annotation[] annots = AnnotationUtil.getAnnotatedMethodFirstParameterQualifierWithGivenAnnotation(annotatedMethod, Disposes.class);
                        
                        if(type.equals(beanType))
                        {
                            for(Annotation qualifier : qualifiers)
                            {
                                if(qualifier.annotationType() != Default.class)
                                {
                                    for(Annotation ann :annots)
                                    {
                                        if(!AnnotationUtil.hasAnnotationMember(qualifier.annotationType(), qualifier, ann))
                                        {
                                            return null;
                                        }
                                        else
                                        {
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            return annotatedMethod.getJavaMember();
                        }                
                    }                                
                }
            }            
        }        
        return null;
        
    }
    
}
