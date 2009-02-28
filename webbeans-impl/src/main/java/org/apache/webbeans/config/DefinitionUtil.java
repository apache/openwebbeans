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
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Named;
import javax.annotation.NonBinding;
import javax.context.ScopeType;
import javax.decorator.Decorates;
import javax.event.Fires;
import javax.event.Observes;
import javax.inject.DeploymentType;
import javax.inject.Disposes;
import javax.inject.Initializer;
import javax.inject.Produces;
import javax.inject.Realizes;
import javax.inject.Specializes;
import javax.inject.UnsatisfiedDependencyException;
import javax.inject.manager.Bean;
import javax.inject.manager.InjectionPoint;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.Component;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.component.IComponentHasParent;
import org.apache.webbeans.component.ObservesMethodsOwner;
import org.apache.webbeans.component.ProducerComponentImpl;
import org.apache.webbeans.component.ProducerFieldComponent;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.deployment.DeploymentTypeManager;
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
public final class DefinitionUtil
{
    private DefinitionUtil()
    {

    }

    public static <T> Class<? extends Annotation> defineDeploymentType(AbstractComponent<T> component, Annotation[] beanAnnotations, String errorMessage)
    {
        boolean found = false;
        for (Annotation annotation : beanAnnotations)
        {
            // Component Type annotation is not null, if Component
            Annotation ct = annotation.annotationType().getAnnotation(DeploymentType.class);

            if (ct != null)
            {
                // Already found component type, too many component type ,throw
                // exception
                if (found == true)
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }
                else
                {
                    component.setType(annotation);// component type
                    found = true;
                }
            }
        }

        if (!found && (component instanceof IComponentHasParent))
        {
            IComponentHasParent child = (IComponentHasParent) component;
            component.setType(child.getParent().getType());
            found = true;
        }        

        if (!found)
        {

            component.setType(WebBeansUtil.getMaxPrecedenceSteroTypeDeploymentType(component));
        }

        return component.getDeploymentType();
    }

    /**
     * Configures the web bean api types.
     * 
     * @param <T> generic class type
     * @param component configuring web beans component
     * @param clazz bean implementation class
     */
    public static <T> void defineApiTypes(AbstractComponent<T> component, Class<T> clazz)
    {
        ClassUtil.setTypeHierarchy(component.getTypes(), clazz);
    }

    /**
     * Configures the producer method web bean api types.
     * 
     * @param <T> generic class type
     * @param component configuring web beans component
     * @param clazz bean implementation class
     */
    public static <T> void defineProducerMethodApiTypes(AbstractComponent<T> component, Class<T> clazz)
    {
        Set<Class<?>> types = component.getTypes();
        
        if (clazz.isPrimitive() || clazz.isArray())
        {
            types.add(clazz);
            types.add(Object.class);
            
            if(clazz.isPrimitive())
            {
                types.add(ClassUtil.getPrimitiveWrapper(clazz));              
            }
            
        }
        else
        {
            if(ClassUtil.isPrimitiveWrapper(clazz))
            {
                types.add(ClassUtil.getWrapperPrimitive(clazz));
            }
            
            ClassUtil.setTypeHierarchy(component.getTypes(), clazz);
        }
    }

    /**
     * Configure web beans component binding type.
     * 
     * @param component configuring web beans component
     * @param annotations annotations
     */
    public static <T> void defineBindingTypes(AbstractComponent<T> component, Annotation[] annotations)
    {
        boolean find = false;
        for (Annotation annotation : annotations)
        {
            Class<? extends Annotation> type = annotation.annotationType();

            if (AnnotationUtil.isBindingAnnotation(type))
            {
                Method[] methods = type.getDeclaredMethods();

                for (Method method : methods)
                {
                    Class<?> clazz = method.getReturnType();
                    if (clazz.isArray() || clazz.isAnnotation())
                    {
                        if (!AnnotationUtil.isAnnotationExist(method.getAnnotations(), NonBinding.class))
                        {
                            throw new WebBeansConfigurationException("WebBeans definition class : " + component.getReturnType().getName() + " @BindingType : " + annotation.annotationType().getName() + " must have @NonBinding valued members for its array-valued and annotation valued members");
                        }
                    }
                }

                if (find == false)
                {
                    find = true;
                }

                component.addBindingType(annotation);
            }
        }

        // No-binding annotation
        if (!find)
        {
            component.addBindingType(new CurrentLiteral());
        }
    }

    /**
     * Configure web beans component scope type.
     * 
     * @param <T> generic class type
     * @param component configuring web beans component
     * @param annotations annotations
     */
    public static <T> void defineScopeType(AbstractComponent<T> component, Annotation[] annotations, String exceptionMessage)
    {
        boolean found = false;

        for (Annotation annotation : annotations)
        {
            Annotation var = annotation.annotationType().getAnnotation(ScopeType.class);
            if (var != null)
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

        if (!found)
        {
            defineDefaultScopeType(component, exceptionMessage);
        }
    }

    public static <T> void defineStereoTypes(Component<?> component, Class<T> clazz)
    {
        Annotation[] anns = clazz.getAnnotations();

        if (AnnotationUtil.isStereoTypeMetaAnnotationExist(anns))
        {
            Annotation[] steroAnns = AnnotationUtil.getStereotypeMetaAnnotations(anns);

            for (Annotation stereo : steroAnns)
            {
                component.addStereoType(stereo);
            }
        }
    }

    public static void defineDefaultScopeType(Component<?> component, String exceptionMessage)
    {
        Set<Annotation> stereos = component.getStereotypes();
        if (stereos.size() == 0)
        {
            component.setImplScopeType(new DependentScopeLiteral());
        }
        else
        {
            Annotation defined = null;
            Set<Annotation> anns = component.getStereotypes();
            for (Annotation stero : anns)
            {
                if (AnnotationUtil.isMetaAnnotationExist(stero.annotationType().getAnnotations(), ScopeType.class))
                {
                    Annotation next = AnnotationUtil.getMetaAnnotations(stero.annotationType().getAnnotations(), ScopeType.class)[0];

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

    /**
     * Configure web beans component name.
     * 
     * @param component configuring web beans component
     * @param defaultName default name of the web bean
     */
    public static <T> void defineName(AbstractComponent<T> component, Annotation[] anns, String defaultName)
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
            if (WebBeansUtil.isNamedExistOnStereoTypes(component))
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
     * Defines the set of {@link ProducerFieldComponent} components.
     * 
     * @param component producer field owner component
     * @return the set of producer field components
     */
    public static Set<ProducerFieldComponent<?>> defineProduerFields(AbstractComponent<?> component)
    {
        Set<ProducerFieldComponent<?>> producerFields = new HashSet<ProducerFieldComponent<?>>();
        Field[] fields = component.getReturnType().getDeclaredFields();
        
        //From normal class
        createProducerFieldWithRealizations(component, producerFields, fields, false);
        
        //From @Realizations
       fields = new Field[0];
        
        if(component.getReturnType().getAnnotation(Realizes.class) != null)
        {
            fields = AnnotationUtil.getClazzFieldsWithGivenAnnotation(component.getReturnType().getSuperclass(), Produces.class);
            
            //from @Realizations
            createProducerFieldWithRealizations(component, producerFields, fields, true);
        }
        
        
        return producerFields;
    }
    
    private static void createProducerFieldWithRealizations(AbstractComponent<?> component,Set<ProducerFieldComponent<?>> producerFields, Field[] fields, boolean isRealizes)
    {
        for(Field field : fields)
        {
            if(isRealizes)
            {
                int modifiers = field.getModifiers();
                if(Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers))
                {
                    continue;
                }
            }
            
            //Producer field
            if(AnnotationUtil.isAnnotationExist(field.getAnnotations(), Produces.class))
            {
                ProducerFieldComponent<?> newComponent = createProducerFieldComponent(field.getType(), field, component);
                
                if (newComponent != null)
                {
                    if(isRealizes)
                    {
                        //Add Binding types from the parent and removes from the generic super class via @Realizes
                        Set<Annotation> fromParents = component.getBindings();
                        for(Annotation fromParent : fromParents)
                        {
                            newComponent.addBindingType(fromParent);
                        }
                        
                        //Removes the @BindingTypes from @Realizes
                        Annotation[] fromGenerics = AnnotationUtil.getBindingAnnotations(component.getReturnType().getSuperclass().getDeclaredAnnotations());
                        for(Annotation fromGeneric : fromGenerics)
                        {
                            newComponent.getBindings().remove(fromGeneric);
                        }
                        
                        //Deployment type is the same as parent
                        newComponent.setType(component.getType());
                        
                    }
                }
                
                
                if (newComponent != null)
                {
                    producerFields.add(newComponent);                    
                }
                
            }
            
        }
                
    }
    
    /**
     * Defines the {@link Bean} producer methods. Moreover,
     * it configures the producer methods with using the {@link Realizes} annotations.
     * 
     * @param component
     * @return the set of producer components
     * @throws WebBeansConfigurationException if any exception occurs
     */
    public static Set<ProducerComponentImpl<?>> defineProducerMethods(AbstractComponent<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        
        Set<ProducerComponentImpl<?>> producerComponents = new HashSet<ProducerComponentImpl<?>>();

        Class<?> clazz = component.getReturnType();
        Method[] declaredMethods = clazz.getDeclaredMethods();
        
        //From @Realizations
        Method[] realizedProducers = new Method[0];
        
        if(clazz.getAnnotation(Realizes.class) != null)
        {
            realizedProducers = AnnotationUtil.getMethodsWithAnnotation(clazz.getSuperclass(), Produces.class);
            
        }
        
        boolean isSpecializes = false;
        
        //This methods defined in the class
        for (Method declaredMethod : declaredMethods)
        {
            createProducerComponentsWithReliazes(component, producerComponents, declaredMethod, clazz, isSpecializes,false);
        }

        //This methods defined in the @Realizations generic class
        for (Method declaredMethod : realizedProducers)
        {
            int modifiers = declaredMethod.getModifiers();
            if(!Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers))
            {
                createProducerComponentsWithReliazes(component, producerComponents, declaredMethod, clazz.getSuperclass(), isSpecializes,true);   
            }
        }

        return producerComponents;

    }
    
    
    private static  <T>  void createProducerComponentsWithReliazes(AbstractComponent<T> component, Set<ProducerComponentImpl<?>> producerComponents, Method declaredMethod, Class<?> clazz,boolean isSpecializes,boolean isRealizes)
    {
        // Producer Method
        if (AnnotationUtil.isMethodHasAnnotation(declaredMethod, Produces.class))
        {
            WebBeansUtil.checkProducerMethodForDeployment(declaredMethod, clazz.getName());

            if (AnnotationUtil.isMethodHasAnnotation(declaredMethod, Specializes.class))
            {
                if (ClassUtil.isStatic(declaredMethod.getModifiers()))
                {
                    throw new WebBeansConfigurationException("Specializing producer method : " + declaredMethod.getName() + " in class : " + clazz.getName() + " can not be static");
                }

                isSpecializes = true;
            }

            Type[] observableTypes = AnnotationUtil.getMethodParameterGenericTypesWithGivenAnnotation(declaredMethod, Fires.class);
            EventUtil.checkObservableMethodParameterConditions(observableTypes, "method parameter", "method : " + declaredMethod.getName() + "in class : " + clazz.getName());

            ProducerComponentImpl<?> newComponent = createProducerComponent(declaredMethod.getReturnType(), declaredMethod, component, isSpecializes);
            if (newComponent != null)
            {
                if(isRealizes)
                {
                    newComponent.setFromRealizes(true);
                    
                    //Add Binding types from the parent and removes from the generic super class via @Realizes
                    Set<Annotation> fromParents = component.getBindings();
                    for(Annotation fromParent : fromParents)
                    {
                        newComponent.addBindingType(fromParent);
                    }
                    
                    //Removes the @BindingTypes from @Realizes
                    Annotation[] fromGenerics = AnnotationUtil.getBindingAnnotations(component.getReturnType().getSuperclass().getDeclaredAnnotations());
                    for(Annotation fromGeneric : fromGenerics)
                    {
                        newComponent.getBindings().remove(fromGeneric);
                    }
                    
                    //Deployment type is the same as parent
                    newComponent.setType(component.getType());
                    
                }
                
                producerComponents.add(newComponent);
                addMethodInjectionPointMetaData(newComponent, declaredMethod);                    
            }
        }
        
    }

    private static <T> ProducerComponentImpl<T> createProducerComponent(Class<T> returnType, Method method, AbstractComponent<?> parent, boolean isSpecializes)
    {
        ProducerComponentImpl<T> component = new ProducerComponentImpl<T>(parent, returnType);
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

        Class<? extends Annotation> deploymentType = DefinitionUtil.defineDeploymentType(component, method.getAnnotations(), "There are more than one @DeploymentType annotation in the component class : " + component.getReturnType().getName());

        // Check if the deployment type is enabled.
        if (!DeploymentTypeManager.getInstance().isDeploymentTypeEnabled(deploymentType))
        {
            return null;
        }

        Annotation[] methodAnns = method.getAnnotations();

        DefinitionUtil.defineProducerMethodApiTypes(component, returnType);
        DefinitionUtil.defineScopeType(component, methodAnns, "WebBeans producer method : " + method.getName() + " in class " + parent.getReturnType().getName() + " must declare default @ScopeType annotation");
        DefinitionUtil.defineBindingTypes(component, methodAnns);
        DefinitionUtil.defineName(component, methodAnns, WebBeansUtil.getProducerDefaultName(method.getName()));

        WebBeansUtil.checkSteroTypeRequirements(component, methodAnns, "WebBeans producer method : " + method.getName() + " in class : " + parent.getReturnType().getName());

        return component;
    }
    
    private static <T> ProducerFieldComponent<T> createProducerFieldComponent(Class<T> returnType, Field field, AbstractComponent<?> parent)
    {
        ProducerFieldComponent<T> component = new ProducerFieldComponent<T>(parent, returnType);
        component.setProducerField(field);


        if (returnType.isPrimitive())
        {
            component.setNullable(false);
        }

        defineSerializable(component);

        Class<? extends Annotation> deploymentType = DefinitionUtil.defineDeploymentType(component, field.getAnnotations(), "There are more than one @DeploymentType annotation in the component class : " + component.getReturnType().getName());

        // Check if the deployment type is enabled.
        if (!DeploymentTypeManager.getInstance().isDeploymentTypeEnabled(deploymentType))
        {
            return null;
        }

        Annotation[] fieldAnns = field.getAnnotations();

        DefinitionUtil.defineProducerMethodApiTypes(component, returnType);
        DefinitionUtil.defineScopeType(component, fieldAnns, "WebBeans producer method : " + field.getName() + " in class " + parent.getReturnType().getName() + " must declare default @ScopeType annotation");
        DefinitionUtil.defineBindingTypes(component, fieldAnns);
        DefinitionUtil.defineName(component, fieldAnns,field.getName());

        //WebBeansUtil.checkSteroTypeRequirements(component, fieldAnns, "WebBeans producer method : " + method.getName() + " in class : " + parent.getReturnType().getName());

        return component;
    }    

    public static <T> void defineDisposalMethods(AbstractComponent<T> component)
    {
        Class<?> clazz = component.getReturnType();

        Method[] methods = AnnotationUtil.getMethodsWithParameterAnnotation(clazz, Disposes.class);        

        Method[] genericMethods = new Method[0];
        if(clazz.getAnnotation(Realizes.class) != null)
        {
            genericMethods = AnnotationUtil.getMethodsWithParameterAnnotation(clazz.getSuperclass(), Disposes.class);
        }       
        
        //From Normal
        createDisposalMethodsWithRealizations(component, methods, clazz,false);
        
        //From @Realizations
        createDisposalMethodsWithRealizations(component, genericMethods, clazz.getSuperclass(),true);
     }
    
    
    private static <T> void createDisposalMethodsWithRealizations(AbstractComponent<T> component,Method[] methods, Class<?> clazz, boolean isRealizes)
    {
        ProducerComponentImpl<?> previous = null;
        for (Method declaredMethod : methods)
        {
            if(isRealizes)
            {
                int modifiers = declaredMethod.getModifiers();
                if(Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers))
                {
                    continue;
                }
            }
            
            WebBeansUtil.checkProducerMethodDisposal(declaredMethod, clazz.getName());

            Type type = AnnotationUtil.getMethodFirstParameterWithAnnotation(declaredMethod, Disposes.class);
            Annotation[] annot = AnnotationUtil.getMethodFirstParameterBindingTypesWithGivenAnnotation(declaredMethod, Disposes.class);
            
            if(isRealizes)
            {
                annot = AnnotationUtil.getRealizesGenericAnnotations(component.getReturnType(), annot);
            }
                        
            Set<Bean<T>> set = InjectionResolver.getInstance().implResolveByType(ClassUtil.getFirstRawType(type), ClassUtil.getActualTypeArguements(type), annot);
            Bean<T> bean =  set.iterator().next();
            ProducerComponentImpl<?> pr = null;
            
            if (bean == null || !(bean instanceof ProducerComponentImpl))
            {
                throw new UnsatisfiedDependencyException("Producer method component of the disposal method : " + declaredMethod.getName() + " in class : " + clazz.getName() + "is not found");
            }
            
            else
            {
                pr = (ProducerComponentImpl<?>)bean;
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

            pr.setDisposalMethod(declaredMethod);
            
            addMethodInjectionPointMetaData(component, declaredMethod);
            
        }
    }

    public static <T> void defineInjectedFields(ComponentImpl<T> component)
    {
        Class<T> clazz = component.getReturnType();

        if(!WebBeansUtil.checkObservableFieldsConditions(clazz))
        {
            WebBeansUtil.checkObtainsFieldConditions(clazz);   
        }

        Field[] fields = clazz.getDeclaredFields();
        if (fields.length != 0)
        {
            for (Field field : fields)
            {
                Annotation[] anns = field.getAnnotations();

                //Injected fields can not be @Decorates or @Produces
                if(AnnotationUtil.isAnnotationExist(anns, Produces.class) || 
                        AnnotationUtil.isAnnotationExist(anns, Decorates.class))
                {
                    return;
                }
                
                Annotation[] bindingAnns = AnnotationUtil.getBindingAnnotations(anns);
                Annotation[] resourceAnns = AnnotationUtil.getResourceAnnotations(anns);
                
                // bindingAnns and resourceAnns are not allowed at the same time!
                if (bindingAnns.length > 0 && resourceAnns.length > 0)
                {
                    throw new WebBeansConfigurationException("Found binding and resource injection at the same time for the field : " 
                                                             + field.getName() + " in class : " + clazz.getName());
                }
                
                // injected fields must either be resources or define binding types, 
                // otherwise it's binding &#x0040;Current.
                if (bindingAnns.length > 0 || resourceAnns.length > 0)
                {
                    if (bindingAnns.length > 0)
                    {
                        WebBeansUtil.checkForNewBindingForDeployment(field.getGenericType(), clazz, field.getName(), anns);
                    }
                    if (resourceAnns.length > 0)
                    {
                        WebBeansUtil.checkForValidResources(field.getGenericType(), clazz, field.getName(), anns);
                    }
                    
                    int mod = field.getModifiers();
                    if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod))
                    {
                        component.addInjectedField(field);
                        addFieldInjectionPointMetaData(component, field);
                    }
                }
                
            }
        }

    }

    public static <T> void defineInjectedMethods(ComponentImpl<T> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");

        Class<T> clazz = component.getReturnType();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods)
        {
            boolean isInitializer = AnnotationUtil.isMethodHasAnnotation(method, Initializer.class);
            boolean isResource    = AnnotationUtil.isMethodHasResourceAnnotation(method);
            
            if (isInitializer && isResource)
            {
                throw new WebBeansConfigurationException("Found Initializer and resource injection at the same time for the method : " 
                                                         + method.getName() + " in class : " + clazz.getName());
            }
            
            if (isInitializer)
            {
                checkForInjectedInitializerMethod(component, clazz, method);
            }
            else if (isResource)
            {
                checkForValidResourceMethod(component, clazz, method);
            }
            else
            {
                continue;
            }
         
            if (!Modifier.isStatic(method.getModifiers()))
            {
                component.addInjectedMethod(method);
                addMethodInjectionPointMetaData(component, method);
            }

        }
    }

    /**
     * add the definitions for a &#x0040;Initializer method.
     */
    private static <T> void checkForInjectedInitializerMethod(ComponentImpl<T> component, Class<T> clazz, Method method)
    {
        Annotation[][] anns = method.getParameterAnnotations();
        Type[] type = method.getGenericParameterTypes();
        for (int i = 0; i < anns.length; i++)
        {
            Annotation[] a = anns[i];
            Type t = type[i];
            WebBeansUtil.checkForNewBindingForDeployment(t, clazz, method.getName(), a);
        }

        if (method.getAnnotation(Produces.class) == null)
        {
            WebBeansUtil.checkInjectedMethodParameterConditions(method, clazz);
        }
        else
        {
            throw new WebBeansConfigurationException("Initializer method : " + method.getName() + " in class : " + clazz.getName() 
                                                     + " can not be annotated with @Produces or @Destructor");
        }
    }

    /**
     * add the definitions for a &#x0040;Initializer method.
     */
    private static <T> void checkForValidResourceMethod(ComponentImpl<T> component, Class<T> clazz, Method method)
    {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length != 1)
        {
            throw new WebBeansConfigurationException("Resource method : " + method.getName() + " in class : " + clazz.getName() 
                                                     + " must only have exactly 1 parameter with a valid resource type");
        }
        
        Annotation[] anns = method.getAnnotations();
        WebBeansUtil.checkForValidResources(parameterTypes[0], clazz, method.getName(), anns);
    }

    
    public static void defineSimpleWebBeanInterceptorStack(AbstractComponent<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can no be null");

        // @javax.interceptor.Interceptors
        EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());

        // @javax.webbeans.Interceptor
        WebBeansInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());
    }

    public static void defineWebBeanDecoratorStack(AbstractComponent<?> component, Object object)
    {
        WebBeansDecoratorConfig.configureDecarotors(component, object);
    }

    public static <T> void defineObserverMethods(ObservesMethodsOwner<T> component, Class<T> clazz)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        Asserts.nullCheckForClass(clazz);

        NotificationManager manager = NotificationManager.getInstance();
                
        Method[] candidateMethods = AnnotationUtil.getMethodsWithParameterAnnotation(clazz, Observes.class);

        //From @Relizations
        Method[] genericMethods = new Method[0];
        if(clazz.getAnnotation(Realizes.class) != null)
        {
            genericMethods = AnnotationUtil.getMethodsWithParameterAnnotation(clazz.getSuperclass(), Observes.class);
        }       
        
        //From normal
        createObserverMethodsWithRealizes(component, clazz, candidateMethods, false);
        
        //From @Realizations
        createObserverMethodsWithRealizes(component, clazz.getSuperclass(), genericMethods, true);
        
        manager.addObservableComponentMethods(component);

    }
    
    @SuppressWarnings("unchecked")
    private static <T> void createObserverMethodsWithRealizes(ObservesMethodsOwner<T> component,Class<?> clazz, Method[] candidateMethods, boolean isRealizes)
    {
        
        for (Method candidateMethod : candidateMethods)
        {
            if(isRealizes)
            {
                int modifiers = candidateMethod.getModifiers();
                if(Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers))
                {
                    continue;
                }
            }
                        
            EventUtil.checkObserverMethodConditions(candidateMethod, clazz);
            component.addObservableMethod(candidateMethod);
            component.setFromRealizes(isRealizes);
            
            addMethodInjectionPointMetaData((AbstractComponent<T>)component, candidateMethod);
        }
        
    }

    public static <T> void defineSerializable(AbstractComponent<T> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        if (ClassUtil.isAssignable(Serializable.class, component.getReturnType()))
        {
            component.setSerializable(true);
        }
    }
    
    public static <T> void addFieldInjectionPointMetaData(AbstractComponent<T> owner, Field field)
    {
        InjectionPoint injectionPoint = InjectionPointFactory.getFieldInjectionPointData(owner, field);
        if(injectionPoint != null)
        {
            owner.addInjectionPoint(injectionPoint);   
        }
    }
    
    public static <T> void addMethodInjectionPointMetaData(AbstractComponent<T> owner, Method method)
    {
        List<InjectionPoint> injectionPoints = InjectionPointFactory.getMethodInjectionPointData(owner, method);
        for(InjectionPoint injectionPoint : injectionPoints)
        {
            owner.addInjectionPoint(injectionPoint);
        }
    }
    
    public static <T> void addConstructorInjectionPointMetaData(AbstractComponent<T> owner, Constructor<T> constructor)
    {
        List<InjectionPoint> injectionPoints = InjectionPointFactory.getConstructorInjectionPointData(owner, constructor);
        for(InjectionPoint injectionPoint : injectionPoints)
        {
            owner.addInjectionPoint(injectionPoint);
        }
    }
}