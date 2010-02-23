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
package org.apache.webbeans.config;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Scope;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.api.ResourceReference;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.ResourceBean;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.ExternalScope;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.EventUtil;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.intercept.ejb.EJBInterceptorConfig;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Defines the web beans components common properties.
 */
@SuppressWarnings("unchecked")
public final class DefinitionUtil
{
    private DefinitionUtil()
    {

    }
    
    /**
     * Configures the web bean api types.
     * 
     * @param <T> generic class type
     * @param bean configuring web beans component
     * @param clazz bean implementation class
     */
    public static <T> void defineApiTypes(AbstractOwbBean<T> bean, Class<T> clazz)
    {
        Annotation[] annots = clazz.getDeclaredAnnotations();
        
        //Looking for bean types
        if(AnnotationUtil.hasAnnotation(annots, Typed.class))
        {
            Typed beanTypes = (Typed) AnnotationUtil.getAnnotation(annots, Typed.class);
            defineUserDefinedBeanTypes(bean, null, beanTypes);            
        }
        else
        {
            defineNormalApiTypes(bean, clazz);
        }        
    }
     
    
    private static <T> void defineNormalApiTypes(AbstractOwbBean<T> bean, Class<T> clazz)
    {
        bean.getTypes().add(Object.class);
        ClassUtil.setTypeHierarchy(bean.getTypes(), clazz);           
    }
    
    private static <T> void defineUserDefinedBeanTypes(AbstractOwbBean<T> bean, Type producerGenericReturnType, Typed beanTypes)
    {
        if(producerGenericReturnType != null)
        {
            defineNormalProducerMethodApi((AbstractProducerBean<T>)bean, producerGenericReturnType);
        }
        else
        {
            defineNormalApiTypes(bean, bean.getReturnType());
        }
        
        //@Type values
        Class<?>[] types = beanTypes.value();        
        
        //Normal api types
        Set<Type> apiTypes = bean.getTypes();
        //New api types
        Set<Type> newTypes = new HashSet<Type>();
        for(Class<?> type : types)
        {
            Type foundType = null;
            
            for(Type apiType : apiTypes)
            {
                if(ClassUtil.getClazz(apiType).equals(type))
                {
                    foundType = apiType;
                    break;
                }
            }
            
            if(foundType == null)
            {
                throw new WebBeansConfigurationException("@Type values must be in bean api types : " + bean.getTypes());
            }
            
            newTypes.add(foundType);
        }
        
        apiTypes.clear();
        apiTypes.addAll(newTypes);
        
        apiTypes.add(Object.class);
    }
    
    

    /**
     * Configures the producer method web bean api types.
     * 
     * @param <T> generic class type
     * @param producerBean configuring web beans component
     * @param type bean implementation class
     */
    public static <T> void defineProducerMethodApiTypes(AbstractProducerBean<T> producerBean, Type type, Annotation[] annots)
    {
        
        //Looking for bean types
        if(AnnotationUtil.hasAnnotation(annots, Typed.class))
        {
            Typed beanTypes = (Typed) AnnotationUtil.getAnnotation(annots, Typed.class);
            defineUserDefinedBeanTypes(producerBean, type, beanTypes);
        }
        
        else
        {
            defineNormalProducerMethodApi(producerBean, type);
        }        
    }
    
    private static <T> void defineNormalProducerMethodApi(AbstractProducerBean<T> producerBean, Type type)
    {
        Set<Type> types = producerBean.getTypes();
        types.add(Object.class);
        
        Class<?> clazz  = ClassUtil.getClazz(type);
        
        if (clazz != null && (clazz.isPrimitive() || clazz.isArray()))
        {
            types.add(clazz);

        }
        else
        {
            ClassUtil.setTypeHierarchy(producerBean.getTypes(), type);
        }                    
    }

    /**
     * Configure web beans component qualifier.
     * 
     * @param component configuring web beans component
     * @param annotations annotations
     */
    public static <T> void defineQualifiers(AbstractOwbBean<T> component, Annotation[] annotations)
    {
        boolean find = false;
        for (Annotation annotation : annotations)
        {
            Class<? extends Annotation> type = annotation.annotationType();

            if (AnnotationUtil.isQualifierAnnotation(type))
            {
                Method[] methods = type.getDeclaredMethods();

                for (Method method : methods)
                {
                    Class<?> clazz = method.getReturnType();
                    if (clazz.isArray() || clazz.isAnnotation())
                    {
                        if (!AnnotationUtil.hasAnnotation(method.getDeclaredAnnotations(), Nonbinding.class))
                        {
                            throw new WebBeansConfigurationException("WebBeans definition class : " + component.getReturnType().getName() + " @Qualifier : " + annotation.annotationType().getName() + " must have @NonBinding valued members for its array-valued and annotation valued members");
                        }
                    }
                }

                if (find == false)
                {
                    find = true;
                }

                component.addQualifier(annotation);
            }
        }
        
        // Adding inherited qualifiers
        IBeanInheritedMetaData inheritedMetaData = null;
        
        if(component instanceof InjectionTargetBean)
        {
            inheritedMetaData = ((InjectionTargetBean<?>) component).getInheritedMetaData();
        }
        
        if (inheritedMetaData != null)
        {
            Set<Annotation> inheritedTypes = inheritedMetaData.getInheritedQualifiers();
            for (Annotation inherited : inheritedTypes)
            {
                Set<Annotation> qualifiers = component.getQualifiers();
                boolean found = false;
                for (Annotation existQualifier : qualifiers)
                {
                    if (existQualifier.annotationType().equals(inherited.annotationType()))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    component.addQualifier(inherited);
                }
            }
        }
        

        // No-binding annotation
        if (component.getQualifiers().size() == 0 )
        {
            component.addQualifier(new DefaultLiteral());
        }
        else if(component.getQualifiers().size() == 1)
        {
            Annotation annot = component.getQualifiers().iterator().next();
            if(annot.annotationType().equals(Named.class))
            {
                component.addQualifier(new DefaultLiteral());
            }
        }
        
        //Add @Any support
        if(!AnnotationUtil.hasAnyQualifier(component))
        {
        	component.addQualifier(new AnyLiteral());
        }
        	 
    }

    /**
     * Configure web beans component scope type.
     * 
     * @param <T> generic class type
     * @param component configuring web beans component
     * @param annotations annotations
     */
    public static <T> void defineScopeType(AbstractOwbBean<T> component, Annotation[] annotations, String exceptionMessage)
    {
        boolean found = false;

        List<ExternalScope> additionalScopes = BeanManagerImpl.getManager().getAdditionalScopes();
        
        for (Annotation annotation : annotations)
        {   
            Class<? extends Annotation> annotationType = annotation.annotationType();
            
            /*Normal scope*/
            Annotation var = annotationType.getAnnotation(NormalScope.class);
            /*Pseudo scope*/
            Annotation pseudo = annotationType.getAnnotation(Scope.class);
        
            if (var == null && pseudo == null)
            {
                // check for additional scopes registered via a CDI Extension
                for (ExternalScope additionalScope : additionalScopes)
                {
                    if (annotationType.equals(additionalScope.getScope()))
                    {
                        // create a proxy which implements the given annotation
                        Annotation scopeAnnotation = additionalScope.getScopeAnnotation();
    
                        if (additionalScope.isNormal())
                        {
                            var = scopeAnnotation;
                        }
                        else
                        {
                            pseudo = scopeAnnotation;
                        }
                    }
                }
            }
            
            if (var != null)
            {
                if(pseudo != null)
                {
                    throw new WebBeansConfigurationException("Not to define both @Scope and @NormalScope on bean : " + component);
                }
                
                if (found)
                {
                    throw new WebBeansConfigurationException(exceptionMessage);
                }
                else
                {
                    found = true;
                    component.setImplScopeType(annotation);
                }
            }
            else
            {
                if(pseudo != null)
                {
                    if (found)
                    {
                        throw new WebBeansConfigurationException(exceptionMessage);
                    }
                    else
                    {
                        found = true;
                        component.setImplScopeType(annotation);
                    }                    
                }
            }
        }

        if (!found)
        {
            defineDefaultScopeType(component, exceptionMessage);
        }
    }

    public static <T> void defineStereoTypes(OwbBean<?> component, Annotation[] anns)
    {
        if (AnnotationUtil.hasStereoTypeMetaAnnotation(anns))
        {
            Annotation[] steroAnns = AnnotationUtil.getStereotypeMetaAnnotations(anns);

            for (Annotation stereo : steroAnns)
            {
                component.addStereoType(stereo);
            }
        }
        
        // Adding inherited qualifiers
        IBeanInheritedMetaData inheritedMetaData = null;
        
        if(component instanceof InjectionTargetBean)
        {
            inheritedMetaData = ((InjectionTargetBean<?>) component).getInheritedMetaData();
        }
        
        if (inheritedMetaData != null)
        {
            Set<Annotation> inheritedTypes = inheritedMetaData.getInheritedStereoTypes();        
            for (Annotation inherited : inheritedTypes)
            {
                Set<Class<? extends Annotation>> qualifiers = component.getStereotypes();
                boolean found = false;
                for (Class<? extends Annotation> existQualifier : qualifiers)
                {
                    if (existQualifier.equals(inherited.annotationType()))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    component.addStereoType(inherited);
                }
            }
        }
        
    }

    public static void defineDefaultScopeType(OwbBean<?> component, String exceptionMessage)
    {
        // Frist look for inherited scope
        IBeanInheritedMetaData metaData = null;
        if(component instanceof InjectionTargetBean)
        {
            metaData = ((InjectionTargetBean<?>)component).getInheritedMetaData();
        }
        boolean found = false;
        if (metaData != null)
        {
            Annotation inheritedScope = metaData.getInheritedScopeType();
            if (inheritedScope != null)
            {
                found = true;
                component.setImplScopeType(inheritedScope);
            }
        }

        if (!found)
        {
            Set<Class<? extends Annotation>> stereos = component.getStereotypes();
            if (stereos.size() == 0)
            {
                component.setImplScopeType(new DependentScopeLiteral());
            }
            else
            {
                Annotation defined = null;
                Set<Class<? extends Annotation>> anns = component.getStereotypes();
                for (Class<? extends Annotation> stero : anns)
                {
                    boolean containsNormal = AnnotationUtil.hasMetaAnnotation(stero.getDeclaredAnnotations(), NormalScope.class);
                    
                    if (AnnotationUtil.hasMetaAnnotation(stero.getDeclaredAnnotations(), NormalScope.class) ||
                            AnnotationUtil.hasMetaAnnotation(stero.getDeclaredAnnotations(), Scope.class))
                    {                        
                        Annotation next = null;
                        
                        if(containsNormal)
                        {
                            next = AnnotationUtil.getMetaAnnotations(stero.getDeclaredAnnotations(), NormalScope.class)[0];
                        }
                        else
                        {
                            next = AnnotationUtil.getMetaAnnotations(stero.getDeclaredAnnotations(), Scope.class)[0];
                        }

                        if (defined == null)
                        {
                            defined = next;
                        }
                        else
                        {
                            if (!defined.equals(next))
                            {
                                throw new WebBeansConfigurationException(exceptionMessage);
                            }
                        }
                    }
                }

                if (defined != null)
                {
                    component.setImplScopeType(defined);
                }
                else
                {
                    component.setImplScopeType(new DependentScopeLiteral());
                }
            }
        }

    }

    /**
     * Configure web beans component name.
     * 
     * @param component configuring web beans component
     * @param defaultName default name of the web bean
     */
    public static <T> void defineName(AbstractOwbBean<T> component, Annotation[] anns, String defaultName)
    {
        Named nameAnnot = null;
        boolean isDefault = false;
        for (Annotation ann : anns)
        {
            if (ann.annotationType().equals(Named.class))
            {
                nameAnnot = (Named) ann;
                break;
            }
        }

        if (nameAnnot == null) // no @Named
        {
            // Check for stereottype
            if (WebBeansUtil.hasNamedOnStereoTypes(component))
            {
                isDefault = true;
            }

        }
        else
        // yes @Named
        {
            if (nameAnnot.value().equals(""))
            {
                isDefault = true;
            }
            else
            {
                component.setName(nameAnnot.value());
            }

        }

        if (isDefault)
        {
            component.setName(defaultName);
        }

    }

    /**
     * Defines the set of {@link ProducerFieldBean} components.
     * 
     * @param component producer field owner component
     * @return the set of producer field components
     */
    public static Set<ProducerFieldBean<?>> defineProduerFields(InjectionTargetBean<?> component)
    {
        Set<ProducerFieldBean<?>> producerFields = new HashSet<ProducerFieldBean<?>>();
        Field[] fields = component.getReturnType().getDeclaredFields();
        createProducerField(component, producerFields, fields);

        return producerFields;
    }

    private static void createProducerField(InjectionTargetBean<?> component, Set<ProducerFieldBean<?>> producerFields, Field[] fields)
    {
        for (Field field : fields)
        {        
            Type genericType = field.getGenericType();
            
            // Producer field
            if (AnnotationUtil.hasAnnotation(field.getDeclaredAnnotations(), Produces.class))
            {                
                if(ClassUtil.isParametrizedType(genericType))
                {
                    if(!ClassUtil.checkParametrizedType((ParameterizedType)genericType))
                    {
                        throw new WebBeansConfigurationException("Producer field : " + field.getName() + " return type in class : " + 
                                field.getDeclaringClass().getName() + " can not be Wildcard type or Type variable");
                    }
                }
                
                ProducerFieldBean<?> newComponent = createProducerFieldComponent(field.getType(), field, component);

                if (newComponent != null)
                {
                    producerFields.add(newComponent);
                }                    
            }

        }

    }

    /**
     * Defines the {@link Bean} producer methods. Moreover, it configures the
     * producer methods with using the {@link Realizes} annotations.
     * 
     * @param component
     * @return the set of producer components
     * @throws WebBeansConfigurationException if any exception occurs
     */
    public static Set<ProducerMethodBean<?>> defineProducerMethods(AbstractInjectionTargetBean<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");

        Set<ProducerMethodBean<?>> producerComponents = new HashSet<ProducerMethodBean<?>>();

        Class<?> clazz = component.getReturnType();
        Method[] declaredMethods = clazz.getDeclaredMethods();

        // This methods defined in the class
        for (Method declaredMethod : declaredMethods)
        {
            createProducerComponents(component, producerComponents, declaredMethod, clazz);
        }
        
        return producerComponents;
    }

    private static <T> void createProducerComponents(InjectionTargetBean<T> component, Set<ProducerMethodBean<?>> producerComponents, Method declaredMethod, Class<?> clazz)
    {
        boolean isSpecializes = false;
        
        // Producer Method
        if (AnnotationUtil.hasMethodAnnotation(declaredMethod, Produces.class))
        {
            WebBeansUtil.checkProducerMethodForDeployment(declaredMethod, clazz.getName());

            if (AnnotationUtil.hasMethodAnnotation(declaredMethod, Specializes.class))
            {
                if (ClassUtil.isStatic(declaredMethod.getModifiers()))
                {
                    throw new WebBeansConfigurationException("Specializing producer method : " + declaredMethod.getName() + " in class : " + clazz.getName() + " can not be static");
                }

                isSpecializes = true;
            }

            ProducerMethodBean<?> newComponent = createProducerComponent(declaredMethod.getReturnType(), declaredMethod, component, isSpecializes);

            if (newComponent != null)
            {
                producerComponents.add(newComponent);
                addMethodInjectionPointMetaData(newComponent, declaredMethod);
            }
        }

    }

    public static <T> ProducerMethodBean<T> createProducerComponent(Class<T> returnType, Method method, InjectionTargetBean<?> parent, boolean isSpecializes)
    {
        ProducerMethodBean<T> component = new ProducerMethodBean<T>(parent, returnType);
        component.setCreatorMethod(method);

        if (isSpecializes)
        {
            WebBeansUtil.configureProducerSpecialization(component, method, parent.getReturnType().getSuperclass());
        }

        if (returnType.isPrimitive())
        {
            component.setNullable(false);
        }

        defineSerializable(component);
        defineStereoTypes(component, method.getDeclaredAnnotations());

        Annotation[] methodAnns = method.getDeclaredAnnotations();
        
        WebBeansUtil.setBeanEnableFlagForProducerBean(parent, component, methodAnns);

        DefinitionUtil.defineProducerMethodApiTypes(component, method.getGenericReturnType(), methodAnns);
        DefinitionUtil.defineScopeType(component, methodAnns, "WebBeans producer method : " + method.getName() + " in class " + parent.getReturnType().getName() + " must declare default @Scope annotation");
        WebBeansUtil.checkUnproxiableApiType(component, component.getScope());
        WebBeansUtil.checkProducerGenericType(component,method);        
        DefinitionUtil.defineQualifiers(component, methodAnns);
        DefinitionUtil.defineName(component, methodAnns, WebBeansUtil.getProducerDefaultName(method.getName()));

        return component;
    }

    private static <T> ProducerFieldBean<T> createProducerFieldComponent(Class<T> returnType, Field field, InjectionTargetBean<?> parent)
    {
        ProducerFieldBean<T> component = new ProducerFieldBean<T>(parent, returnType);
        
        //Producer field for resource
        Annotation resourceAnnotation = AnnotationUtil.hasOwbInjectableResource(field.getDeclaredAnnotations());
        if(resourceAnnotation != null)
        {
            //Check for valid resource annotation
            //WebBeansUtil.checkForValidResources(field.getDeclaringClass(), field.getType(), field.getName(), field.getDeclaredAnnotations());
            if(!ClassUtil.isStatic(field.getModifiers()))
            {
                ResourceReference<T,Annotation> resourceRef = new ResourceReference<T, Annotation>(field.getDeclaringClass(), field.getName(),returnType, resourceAnnotation);

                //Can not define EL name
                if(field.isAnnotationPresent(Named.class))
                {
                    throw new WebBeansConfigurationException("Resource producer field : " + field + " can not define EL name");
                }
                
                ResourceBean<T,Annotation> resourceBean = new ResourceBean(returnType,parent, resourceRef);
                
                defineProducerMethodApiTypes(resourceBean, field.getGenericType() , field.getDeclaredAnnotations());
                defineQualifiers(resourceBean, field.getDeclaredAnnotations());
                resourceBean.setImplScopeType(new DependentScopeLiteral());
                resourceBean.setProducerField(field);
                
                return resourceBean;                
            }
        }
        
        component.setProducerField(field);

        if (returnType.isPrimitive())
        {
            component.setNullable(false);
        }

        defineSerializable(component);
        defineStereoTypes(component, field.getDeclaredAnnotations());

        Annotation[] fieldAnns = field.getDeclaredAnnotations();
        
        WebBeansUtil.setBeanEnableFlagForProducerBean(parent, component, fieldAnns);

        DefinitionUtil.defineProducerMethodApiTypes(component, field.getGenericType(), fieldAnns);
        DefinitionUtil.defineScopeType(component, fieldAnns, "WebBeans producer method : " + field.getName() + " in class " + parent.getReturnType().getName() + " must declare default @Scope annotation");
        WebBeansUtil.checkUnproxiableApiType(component, component.getScope());
        WebBeansUtil.checkProducerGenericType(component,field);
        DefinitionUtil.defineQualifiers(component, fieldAnns);
        DefinitionUtil.defineName(component, fieldAnns, field.getName());

        return component;
    }

    public static <T> void defineDisposalMethods(AbstractOwbBean<T> component)
    {
        Class<?> clazz = component.getReturnType();

        Method[] methods = AnnotationUtil.getMethodsWithParameterAnnotation(clazz, Disposes.class);

        // From Normal
        createDisposalMethods(component, methods, clazz);

    }

    private static <T> void createDisposalMethods(AbstractOwbBean<T> component, Method[] methods, Class<?> clazz)
    {
        ProducerMethodBean<?> previous = null;
        for (Method declaredMethod : methods)
        {

            WebBeansUtil.checkProducerMethodDisposal(declaredMethod, clazz.getName());

            Type type = AnnotationUtil.getMethodFirstParameterWithAnnotation(declaredMethod, Disposes.class);
            Annotation[] annot = AnnotationUtil.getMethodFirstParameterQualifierWithGivenAnnotation(declaredMethod, Disposes.class);


            Set<Bean<?>> set = InjectionResolver.getInstance().implResolveByType(type, annot);
            if (set.isEmpty()) {
                throw new UnsatisfiedResolutionException("Producer method component of the disposal method : " + declaredMethod.getName() + 
                              " in class : " + clazz.getName() + ". Cannot find bean " + type + " with qualifier " + Arrays.toString(annot));
            }
            
            Bean<?> bean = set.iterator().next();
            ProducerMethodBean<?> pr = null;

            if (bean == null || !(bean instanceof ProducerMethodBean))
            {
                throw new UnsatisfiedResolutionException("Producer method component of the disposal method : " + declaredMethod.getName() + " in class : " + clazz.getName() + "is not found");
            }

            else
            {
                pr = (ProducerMethodBean<?>) bean;
            }

            if (previous == null)
            {
                previous = pr;
            }
            else
            {
                // multiple same producer
                if (previous.equals(pr))
                {
                    throw new WebBeansConfigurationException("There are multiple disposal method for the producer method : " + pr.getCreatorMethod().getName() + " in class : " + clazz.getName());
                }
            }

            Method producerMethod = pr.getCreatorMethod();
            //Disposer methods and producer methods must be in the same class
            if(!producerMethod.getDeclaringClass().getName().equals(declaredMethod.getDeclaringClass().getName()))
            {
                throw new WebBeansConfigurationException("Producer method component of the disposal method : " + declaredMethod.getName() + " in class : " + clazz.getName() + " must be in the same class!");
            }
            
            pr.setDisposalMethod(declaredMethod);

            addMethodInjectionPointMetaData(component, declaredMethod);

        }
    }

    public static <T> void defineInjectedFields(AbstractInjectionTargetBean<T> component)
    {
        Class<T> clazz = component.getReturnType();

        // From component
        defineInternalInjectedFields(component, clazz, false);

        // From inherited super class
        defineInternalInjectedFieldsRecursively(component, clazz);

    }

    public static <T> void defineInternalInjectedFieldsRecursively(AbstractInjectionTargetBean<T> component, Class<T> clazz)
    {
        // From inheritance
        Class<?> superClazz = clazz.getSuperclass();
        
        if (!superClazz.equals(Object.class))
        {
            // From super class
            defineInternalInjectedFields(component, (Class<T>) superClazz, true);

            // From super class type hierarchy
            defineInternalInjectedFieldsRecursively(component, (Class<T>) superClazz);
        }

    }

    public static <T> void defineInternalInjectedFields(AbstractInjectionTargetBean<T> component, Class<T> clazz, boolean fromSuperClazz)
    {

        Field[] fields = clazz.getDeclaredFields();
        boolean useOwbSpecificInjection = OpenWebBeansConfiguration.getInstance().isOwbSpecificFieldInjection();
            
        if (fields.length != 0)
        {
            for (Field field : fields)
            {
                if(!useOwbSpecificInjection)
                {
                    if(!field.isAnnotationPresent(Inject.class))
                    {
                        continue;
                    }                       
                }
                
                if(ClassUtil.isPublic(field.getModifiers()))
                {
                    if(!component.getScope().equals(Dependent.class))
                    {
                        throw new WebBeansConfigurationException("If bean has a public modifier injection point, bean scope must be defined as @Dependent");
                    }
                }                
                
                Annotation[] anns = field.getDeclaredAnnotations();

                // Injected fields can not be @Produces
                if (AnnotationUtil.hasAnnotation(anns, Produces.class))
                {
                    if(!useOwbSpecificInjection)
                    {
                        throw new WebBeansConfigurationException("Injection fields can not be annotated with @Produces");
                    }
                }

                Annotation[] qualifierAnns = AnnotationUtil.getQualifierAnnotations(anns);

                if (qualifierAnns.length > 0)
                {
                    if (qualifierAnns.length > 0)
                    {
                        WebBeansUtil.checkForNewQualifierForDeployment(field.getGenericType(), clazz, field.getName(), anns);
                    }

                    int mod = field.getModifiers();
                    
                    if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod))
                    {
                        if(fromSuperClazz)
                        {
                            component.addInjectedFieldToSuper(field);    
                        }
                        else
                        {
                            component.addInjectedField(field);
                        }
                        
                        addFieldInjectionPointMetaData(component, field);                                
                    }
                }                                    
            }
        }
    }

    public static <T> void defineInjectedMethods(AbstractInjectionTargetBean<T> bean)
    {
        Asserts.assertNotNull(bean, "bean parameter can not be null");

        Class<T> clazz = bean.getReturnType();

        // From bean class definition
        defineInternalInjectedMethods(bean, clazz, false);

        // From inheritance hierarchy
        defineInternalInjectedMethodsRecursively(bean, clazz);
    }

    public static <T> void defineInternalInjectedMethodsRecursively(AbstractInjectionTargetBean<T> component, Class<T> clazz)
    {
        // From inheritance
        Class<?> superClazz = clazz.getSuperclass();
        if (!superClazz.equals(Object.class))
        {
            // From super class
            defineInternalInjectedMethods(component, (Class<T>) superClazz, true);

            // From super class type hierarchy
            defineInternalInjectedMethodsRecursively(component, (Class<T>) superClazz);
        }

    }

    private static <T> void defineInternalInjectedMethods(AbstractInjectionTargetBean<T> component, Class<T> clazz, boolean fromInherited)
    {

        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods)
        {
            boolean isInitializer = AnnotationUtil.hasMethodAnnotation(method, Inject.class);
            
            if (isInitializer)
            {
                //Do not support static
                if(ClassUtil.isStatic(method.getModifiers()))
                {
                    continue;
                }
                
                checkForInjectedInitializerMethod(component, clazz, method);
            }
            else
            {
                continue;
            }

            if (!Modifier.isStatic(method.getModifiers()))
            {
                if (!fromInherited)
                {
                    component.addInjectedMethod(method);
                    addMethodInjectionPointMetaData(component, method);
                }
                else
                {                    
                    Method[] beanMethods = component.getReturnType().getDeclaredMethods();
                    boolean defined = false;
                    for (Method beanMethod : beanMethods)
                    {
                        if(ClassUtil.isOverriden(beanMethod, method))                        
                        {
                            defined = true;
                            break;
                        }
                    }
                    
                    if(!defined)
                    {
                        component.addInjectedMethodToSuper(method);
                        addMethodInjectionPointMetaData(component, method);                        
                    }
                }
            }
        }

    }

    /**
     * add the definitions for a &#x0040;Initializer method.
     */
    private static <T> void checkForInjectedInitializerMethod(AbstractInjectionTargetBean<T> component, Class<T> clazz, Method method)
    {
        TypeVariable<?>[] args = method.getTypeParameters();
        if(args.length > 0)
        {
            throw new WebBeansConfigurationException("Initializer methods must not be generic but method : " + method.getName() + " in bean class : " + clazz + " is defined as generic");
        }
        
        Annotation[][] anns = method.getParameterAnnotations();
        Type[] type = method.getGenericParameterTypes();
        for (int i = 0; i < anns.length; i++)
        {
            Annotation[] a = anns[i];
            Type t = type[i];
            WebBeansUtil.checkForNewQualifierForDeployment(t, clazz, method.getName(), a);
        }

        if (method.getAnnotation(Produces.class) == null)
        {
            WebBeansUtil.checkInjectedMethodParameterConditions(method, clazz);
        }
        else
        {
            throw new WebBeansConfigurationException("Initializer method : " + method.getName() + " in class : " + clazz.getName() + " can not be annotated with @Produces");
        }
    }

    /**
     * Configure bean instance interceptor stack.
     * @param bean bean instance
     */
    public static void defineBeanInterceptorStack(AbstractInjectionTargetBean<?> bean)
    {
        Asserts.assertNotNull(bean, "bean parameter can no be null");

        // @javax.interceptor.Interceptors
        EJBInterceptorConfig.configure(((AbstractOwbBean)bean).getReturnType(), bean.getInterceptorStack());

        // @javax.webbeans.Interceptor
        WebBeansInterceptorConfig.configure(bean, bean.getInterceptorStack());
    }

    public static void defineDecoratorStack(AbstractInjectionTargetBean<?> bean)
    {
        WebBeansDecoratorConfig.configureDecarotors((AbstractInjectionTargetBean<Object>)bean);
    }

    public static <T> Set<ObserverMethod<?>> defineObserverMethods(InjectionTargetBean<T> component, Class<T> clazz)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        Asserts.nullCheckForClass(clazz);

        NotificationManager manager = NotificationManager.getInstance();

        Method[] candidateMethods = AnnotationUtil.getMethodsWithParameterAnnotation(clazz, Observes.class);

        // From normal
        createObserverMethods(component, clazz, candidateMethods);

        return manager.addObservableComponentMethods(component);

    }

    private static <T> void createObserverMethods(InjectionTargetBean<T> component, Class<?> clazz, Method[] candidateMethods)
    {

        for (Method candidateMethod : candidateMethods)
        {

            EventUtil.checkObserverMethodConditions(candidateMethod, clazz);
            AbstractOwbBean<?> bean = (AbstractOwbBean<?>) component;
            if(bean.getScope().equals(Dependent.class))
            {
                //Check Reception
                if(EventUtil.isReceptionIfExist(candidateMethod))
                {
                    throw new WebBeansConfigurationException("Dependent Bean : " + bean + " can not define observer method with @Receiver = IF_EXIST");
                }
            }
            
            
            component.addObservableMethod(candidateMethod);

            addMethodInjectionPointMetaData((AbstractOwbBean<T>) component, candidateMethod);
        }

    }

    public static <T> void defineSerializable(AbstractOwbBean<T> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        if (ClassUtil.isClassAssignable(Serializable.class, component.getReturnType()))
        {
            component.setSerializable(true);
        }
    }

    public static <T> void addFieldInjectionPointMetaData(AbstractOwbBean<T> owner, Field field)
    {
        InjectionPoint injectionPoint = InjectionPointFactory.getFieldInjectionPointData(owner, field);
        if (injectionPoint != null)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
        }
    }

    public static <T> void addMethodInjectionPointMetaData(AbstractOwbBean<T> owner, Method method)
    {
        List<InjectionPoint> injectionPoints = InjectionPointFactory.getMethodInjectionPointData(owner, method);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
        }
    }

    public static <T> void addConstructorInjectionPointMetaData(AbstractOwbBean<T> owner, Constructor<T> constructor)
    {
        List<InjectionPoint> injectionPoints = InjectionPointFactory.getConstructorInjectionPointData(owner, constructor);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
        }
    }
    
    public static void addImplicitComponentForInjectionPoint(InjectionPoint injectionPoint)
    {
        if(WebBeansUtil.checkObtainsInjectionPointConditions(injectionPoint))
        {
            //Do nothing
        }        
        else if(EventUtil.checkObservableInjectionPointConditions(injectionPoint))
        {            
            //Do nothing
            //WebBeansUtil.addInjectedImplicitEventComponent(injectionPoint);
        }
    }
    
    public static <X> Set<ProducerMethodBean<?>> defineProducerMethods(InjectionTargetBean<X> bean, AnnotatedType<X> annotatedType)
    {
        Set<ProducerMethodBean<?>> producerComponents = new HashSet<ProducerMethodBean<?>>();
        Set<AnnotatedMethod<? super X>> annotatedMethods = annotatedType.getMethods();
        
        for(AnnotatedMethod annotatedMethod: annotatedMethods)
        {
            createProducerBeansFromAnnotatedType(bean, producerComponents, annotatedMethod, bean.getReturnType(), false);
        }
        
        return producerComponents;
    }
    
    private static <X> void createProducerBeansFromAnnotatedType(InjectionTargetBean<X> bean, Set<ProducerMethodBean<?>> producerComponents, AnnotatedMethod<X> annotatedMethod, Class<?> clazz, boolean isSpecializes)
    {
        Annotation[] anns = annotatedMethod.getAnnotations().toArray(new Annotation[0]);
        
        List<AnnotatedParameter<X>> parameters = annotatedMethod.getParameters();
        // Producer Method
        if (AnnotationUtil.hasAnnotation(anns, Produces.class))
        {
            for(AnnotatedParameter<X> parameter : parameters)
            {
                Annotation[] parameterAnns = parameter.getAnnotations().toArray(new Annotation[0]);
                if (AnnotationUtil.hasAnnotation(anns, Inject.class) || AnnotationUtil.hasAnnotation(parameterAnns, Disposes.class) || AnnotationUtil.hasAnnotation(parameterAnns, Observes.class))
                {
                    throw new WebBeansConfigurationException("Producer Method Bean with name : " + annotatedMethod.getJavaMember().getName() + " in bean class : " + clazz + " can not be annotated with" + " @Initializer/@Destructor annotation or has a parameter annotated with @Disposes/@Observes");
                }
                
            }
            
            if (AnnotationUtil.hasAnnotation(anns, Specializes.class))
            {
                if (ClassUtil.isStatic(annotatedMethod.getJavaMember().getModifiers()))
                {
                    throw new WebBeansConfigurationException("Specializing producer method : " + annotatedMethod.getJavaMember().getName() + " in class : " + clazz.getName() + " can not be static");
                }

                isSpecializes = true;
            }

            ProducerMethodBean<?> newComponent = createProducerBeanFromAnnotatedType((Class<X>)annotatedMethod.getJavaMember().getReturnType(), annotatedMethod, bean, isSpecializes);
            if (newComponent != null)
            {
                producerComponents.add(newComponent);
                addMethodInjectionPointMetaData(newComponent, annotatedMethod.getJavaMember());
            }
        }

    }

    public static <X> ProducerMethodBean<X> createProducerBeanFromAnnotatedType(Class<X> returnType, AnnotatedMethod<X> method, InjectionTargetBean<?> parent, boolean isSpecializes)
    {
        ProducerMethodBean<X> bean = new ProducerMethodBean<X>(parent, returnType);
        bean.setCreatorMethod(method.getJavaMember());

        if (isSpecializes)
        {
            WebBeansUtil.configureProducerSpecialization(bean, method.getJavaMember(), parent.getReturnType().getSuperclass());
        }

        if (returnType.isPrimitive())
        {
            bean.setNullable(false);
        }

        Annotation[] anns = method.getAnnotations().toArray(new Annotation[0]);
        
        defineSerializable(bean);
        defineStereoTypes(bean, anns);

        DefinitionUtil.defineProducerMethodApiTypes(bean, method.getBaseType(), anns);
        DefinitionUtil.defineScopeType(bean, anns, "Bean producer method : " + method.getJavaMember().getName() + " in class " + parent.getReturnType().getName() + " must declare default @Scope annotation");
        WebBeansUtil.checkProducerGenericType(bean,method.getJavaMember());        
        DefinitionUtil.defineQualifiers(bean, anns);
        DefinitionUtil.defineName(bean, anns, WebBeansUtil.getProducerDefaultName(method.getJavaMember().getName()));

        return bean;
    }
    
}