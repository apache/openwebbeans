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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.decorator.Decorates;
import javax.enterprise.context.ScopeType;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Initializer;
import javax.enterprise.inject.Named;
import javax.enterprise.inject.NonBinding;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.deployment.DeploymentType;
import javax.enterprise.inject.deployment.Specializes;
import javax.enterprise.inject.deployment.Standard;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.annotation.ProductionLiteral;
import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.BaseBean;
import org.apache.webbeans.component.IBeanHasParent;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
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
    
    public static <T> Class<? extends Annotation> defineDeploymentType(AbstractBean<T> component, Annotation[] beanAnnotations, String errorMessage)
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

        if (!found)
        {
            // Look inherited meta-data deployment type
            IBeanInheritedMetaData metadata = component.getInheritedMetaData();
            if (metadata != null)
            {
                if (metadata.getInheritedDeploymentType() != null)
                {
                    component.setType(metadata.getInheritedDeploymentType());
                    found = true;
                }
            }
        }

        // Still not set
        if (!found)
        {
            // Look stereotype
            Annotation result = WebBeansUtil.getMaxPrecedenceSteroTypeDeploymentType(component);

            if (result == null)
            {
                //From parent
                if (!found && (component instanceof IBeanHasParent))
                {
                    @SuppressWarnings("unchecked")
                    IBeanHasParent<T> child = (IBeanHasParent<T>) component;
                    component.setType(child.getParent().getType());
                }
                
                else
                {
                    component.setType(new ProductionLiteral());
                    found = true;    
                }
                
            }
            else
            {
                component.setType(result);
                found = true;
            }
        }
                
        if(component.getDeploymentType().equals(Standard.class))
        {
            throw new WebBeansConfigurationException("WebBeans " + component + " may not declare deployment type @Standard");
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
    public static <T> void defineApiTypes(AbstractBean<T> component, Class<T> clazz)
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
    public static <T> void defineProducerMethodApiTypes(AbstractBean<T> component, Type type)
    {
        Set<Type> types = component.getTypes();
        types.add(Object.class);
        
        Class<?> clazz  = ClassUtil.getClazz(type);
        
        if (clazz.isPrimitive() || clazz.isArray())
        {
            types.add(clazz);

        }
        else
        {
            ClassUtil.setTypeHierarchy(component.getTypes(), type);
        }
    }

    /**
     * Configure web beans component binding type.
     * 
     * @param component configuring web beans component
     * @param annotations annotations
     */
    public static <T> void defineBindingTypes(AbstractBean<T> component, Annotation[] annotations)
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
                        if (!AnnotationUtil.isAnnotationExist(method.getDeclaredAnnotations(), NonBinding.class))
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
        
        // Adding inherited binding types
        IBeanInheritedMetaData inheritedMetaData = component.getInheritedMetaData();
        if (inheritedMetaData != null)
        {
            Set<Annotation> inheritedTypes = inheritedMetaData.getInheritedBindingTypes();
            for (Annotation inherited : inheritedTypes)
            {
                Set<Annotation> bindings = component.getBindings();
                boolean found = false;
                for (Annotation existBinding : bindings)
                {
                    if (existBinding.annotationType().equals(inherited.annotationType()))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    component.addBindingType(inherited);
                }
            }
        }
        

        // No-binding annotation
        if (component.getBindings().size() == 0)
        {
            component.addBindingType(new CurrentLiteral());
        }
        
        //Add @Any support
        if(!AnnotationUtil.isAnyBindingExist(component))
        {
        	component.addBindingType(new AnyLiteral());
        }
        	 
    }

    /**
     * Configure web beans component scope type.
     * 
     * @param <T> generic class type
     * @param component configuring web beans component
     * @param annotations annotations
     */
    public static <T> void defineScopeType(AbstractBean<T> component, Annotation[] annotations, String exceptionMessage)
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

    public static <T> void defineStereoTypes(BaseBean<?> component, Annotation[] anns)
    {
        if (AnnotationUtil.isStereoTypeMetaAnnotationExist(anns))
        {
            Annotation[] steroAnns = AnnotationUtil.getStereotypeMetaAnnotations(anns);

            for (Annotation stereo : steroAnns)
            {
                component.addStereoType(stereo);
            }
        }
        
        // Adding inherited binding types
        IBeanInheritedMetaData inheritedMetaData = component.getInheritedMetaData();
        if (inheritedMetaData != null)
        {
            Set<Annotation> inheritedTypes = inheritedMetaData.getInheritedStereoTypes();        
            for (Annotation inherited : inheritedTypes)
            {
                Set<Class<? extends Annotation>> bindings = component.getStereotypes();
                boolean found = false;
                for (Class<? extends Annotation> existBinding : bindings)
                {
                    if (existBinding.equals(inherited.annotationType()))
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

    public static void defineDefaultScopeType(BaseBean<?> component, String exceptionMessage)
    {
        // Frist look for inherited scope
        IBeanInheritedMetaData metaData = component.getInheritedMetaData();
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
                    if (AnnotationUtil.isMetaAnnotationExist(stero.getDeclaredAnnotations(), ScopeType.class))
                    {
                        Annotation next = AnnotationUtil.getMetaAnnotations(stero.getDeclaredAnnotations(), ScopeType.class)[0];

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
    public static <T> void defineName(AbstractBean<T> component, Annotation[] anns, String defaultName)
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
     * Defines the set of {@link ProducerFieldBean} components.
     * 
     * @param component producer field owner component
     * @return the set of producer field components
     */
    public static Set<ProducerFieldBean<?>> defineProduerFields(AbstractBean<?> component)
    {
        Set<ProducerFieldBean<?>> producerFields = new HashSet<ProducerFieldBean<?>>();
        Field[] fields = component.getReturnType().getDeclaredFields();

        // From normal class
        createProducerField(component, producerFields, fields);

        // From @Realizations
        fields = new Field[0];

        return producerFields;
    }

    private static void createProducerField(AbstractBean<?> component, Set<ProducerFieldBean<?>> producerFields, Field[] fields)
    {
        for (Field field : fields)
        {            
            // Producer field
            if (AnnotationUtil.isAnnotationExist(field.getDeclaredAnnotations(), Produces.class))
            {
                Type genericType = field.getGenericType();
                
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


        boolean isSpecializes = false;

        // This methods defined in the class
        for (Method declaredMethod : declaredMethods)
        {
            createProducerComponents(component, producerComponents, declaredMethod, clazz, isSpecializes);
        }


        return producerComponents;

    }

    private static <T> void createProducerComponents(AbstractBean<T> component, Set<ProducerMethodBean<?>> producerComponents, Method declaredMethod, Class<?> clazz, boolean isSpecializes)
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

            ProducerMethodBean<?> newComponent = createProducerComponent(declaredMethod.getReturnType(), declaredMethod, component, isSpecializes);

            if (newComponent != null)
            {
                producerComponents.add(newComponent);
                addMethodInjectionPointMetaData(newComponent, declaredMethod);
            }
        }

    }

    public static <T> ProducerMethodBean<T> createProducerComponent(Class<T> returnType, Method method, AbstractBean<?> parent, boolean isSpecializes)
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

        Class<? extends Annotation> deploymentType = DefinitionUtil.defineDeploymentType(component, method.getDeclaredAnnotations(), "There are more than one @DeploymentType annotation in the component class : " + component.getReturnType().getName());

        // Check if the deployment type is enabled.
        if (!DeploymentTypeManager.getInstance().isDeploymentTypeEnabled(deploymentType))
        {
            return null;
        }

        Annotation[] methodAnns = method.getDeclaredAnnotations();

        DefinitionUtil.defineProducerMethodApiTypes(component, method.getGenericReturnType());
        DefinitionUtil.defineScopeType(component, methodAnns, "WebBeans producer method : " + method.getName() + " in class " + parent.getReturnType().getName() + " must declare default @ScopeType annotation");        
        WebBeansUtil.checkProducerGenericType(component,method);        
        DefinitionUtil.defineBindingTypes(component, methodAnns);
        DefinitionUtil.defineName(component, methodAnns, WebBeansUtil.getProducerDefaultName(method.getName()));

        //Drop from the specification
        //WebBeansUtil.checkSteroTypeRequirements(component, methodAnns, "WebBeans producer method : " + method.getName() + " in class : " + parent.getReturnType().getName());

        return component;
    }

    private static <T> ProducerFieldBean<T> createProducerFieldComponent(Class<T> returnType, Field field, AbstractBean<?> parent)
    {
        ProducerFieldBean<T> component = new ProducerFieldBean<T>(parent, returnType);
        
        //Producer field for resource
        if(AnnotationUtil.hasResourceAnnotation(field.getDeclaredAnnotations()))
        {
            //Check for valid resource annotation
            WebBeansUtil.checkForValidResources(field.getGenericType(), field.getType(), field.getName(), field.getDeclaredAnnotations());
            
            //Can not define EL name
            if(field.isAnnotationPresent(Named.class))
            {
                throw new WebBeansConfigurationException("Resource producer field : " + field + " can not define EL name");
            }
            
            component.setProducerField(field);
            component.addApiType(Object.class);
            component.addApiType(field.getType());
            defineBindingTypes(component, field.getDeclaredAnnotations());
            component.setImplScopeType(new DependentScopeLiteral());
            
            return component;
        }
        
        component.setProducerField(field);

        if (returnType.isPrimitive())
        {
            component.setNullable(false);
        }

        defineSerializable(component);
        defineStereoTypes(component, field.getDeclaredAnnotations());

        Class<? extends Annotation> deploymentType = DefinitionUtil.defineDeploymentType(component, field.getDeclaredAnnotations(), "There are more than one @DeploymentType annotation in the component class : " + component.getReturnType().getName());

        // Check if the deployment type is enabled.
        if (!DeploymentTypeManager.getInstance().isDeploymentTypeEnabled(deploymentType))
        {
            return null;
        }

        Annotation[] fieldAnns = field.getDeclaredAnnotations();

        DefinitionUtil.defineProducerMethodApiTypes(component, returnType);
        DefinitionUtil.defineScopeType(component, fieldAnns, "WebBeans producer method : " + field.getName() + " in class " + parent.getReturnType().getName() + " must declare default @ScopeType annotation");
        WebBeansUtil.checkProducerGenericType(component,field);
        DefinitionUtil.defineBindingTypes(component, fieldAnns);
        DefinitionUtil.defineName(component, fieldAnns, field.getName());

        return component;
    }

    public static <T> void defineDisposalMethods(AbstractBean<T> component)
    {
        Class<?> clazz = component.getReturnType();

        Method[] methods = AnnotationUtil.getMethodsWithParameterAnnotation(clazz, Disposes.class);

        // From Normal
        createDisposalMethods(component, methods, clazz);

    }

    private static <T> void createDisposalMethods(AbstractBean<T> component, Method[] methods, Class<?> clazz)
    {
        ProducerMethodBean<?> previous = null;
        for (Method declaredMethod : methods)
        {

            WebBeansUtil.checkProducerMethodDisposal(declaredMethod, clazz.getName());

            Type type = AnnotationUtil.getMethodFirstParameterWithAnnotation(declaredMethod, Disposes.class);
            Annotation[] annot = AnnotationUtil.getMethodFirstParameterBindingTypesWithGivenAnnotation(declaredMethod, Disposes.class);


            Set<Bean<?>> set = InjectionResolver.getInstance().implResolveByType(type, annot);
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

    @SuppressWarnings("unchecked")
    private static <T> void defineInternalInjectedFieldsRecursively(AbstractInjectionTargetBean<T> component, Class<T> clazz)
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

    private static <T> void defineInternalInjectedFields(AbstractInjectionTargetBean<T> component, Class<T> clazz, boolean fromSuperClazz)
    {

        Field[] fields = clazz.getDeclaredFields();
        if (fields.length != 0)
        {
            for (Field field : fields)
            {
                
            	//TODO Check TEsts
            	/*
            	 *
                if(ClassUtil.isPublic(field.getModifiers()))
                {
                    if(!component.getScopeType().equals(Dependent.class))
                    {
                        throw new WebBeansConfigurationException("If bean has a public modifier injection point, bean scope type must be defined as @Dependent");
                    }
                }            	 
            	 */
                
                Annotation[] anns = field.getDeclaredAnnotations();

                // Injected fields can not be @Decorates or @Produces
                if (AnnotationUtil.isAnnotationExist(anns, Produces.class) || AnnotationUtil.isAnnotationExist(anns, Decorates.class))
                {
                    continue;
                }

                Annotation[] bindingAnns = AnnotationUtil.getBindingAnnotations(anns);

                if (bindingAnns.length > 0)
                {
                    if (bindingAnns.length > 0)
                    {
                        WebBeansUtil.checkForNewBindingForDeployment(field.getGenericType(), clazz, field.getName(), anns);
                    }

                    int mod = field.getModifiers();
                    
                    if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod))
                    {
                        if (!fromSuperClazz)
                        {
                            component.addInjectedField(field);
                            
                            addFieldInjectionPointMetaData(component, field);
                        }
                        else
                        {
                            // Check that field is already exist
                            Set<Field> definedInjectedFields = component.getInjectedFields();
                            boolean defined = false;
                            for (Field defineInjectedField : definedInjectedFields)
                            {
                                if (defineInjectedField.getName().equals(field.getName()) && defineInjectedField.getType().equals(field.getType()))
                                {
                                    defined = true;
                                    break;
                                }
                            }
                            
                            if(!defined)
                            {
                                component.addInjectedField(field);
                                addFieldInjectionPointMetaData(component, field);                                
                            }
                        }
                    }
                }

            }
        }

    }

    public static <T> void defineInjectedMethods(AbstractInjectionTargetBean<T> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");

        Class<T> clazz = component.getReturnType();

        // From component class definition
        defineInternalInjectedMethods(component, clazz, false);

        // From inheritance hierarchy
        defineInternalInjectedMethodsRecursively(component, clazz);
    }

    @SuppressWarnings("unchecked")
    private static <T> void defineInternalInjectedMethodsRecursively(AbstractInjectionTargetBean<T> component, Class<T> clazz)
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
            boolean isInitializer = AnnotationUtil.isMethodHasAnnotation(method, Initializer.class);
            
            boolean isResource = AnnotationUtil.isMethodHasResourceAnnotation(method);

            if (isInitializer && isResource)
            {
                throw new WebBeansConfigurationException("Found Initializer and resource injection at the same time for the method : " + method.getName() + " in class : " + clazz.getName());
            }

            if (isInitializer)
            {
                if(ClassUtil.isStatic(method.getModifiers()))
                {
                    throw new WebBeansConfigurationException("Initializer method : " + method.getName() + " in class : " + clazz.getName() + " can not be static!");
                }
                
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
                if (!fromInherited)
                {
                    component.addInjectedMethod(method);
                    addMethodInjectionPointMetaData(component, method);
                }
                else
                {
                    Set<Method> injectedMethods = component.getInjectedMethods();
                    boolean defined = false;
                    for (Method definedInjectedMethod : injectedMethods)
                    {
                        if (definedInjectedMethod.getName().equals(method.getName()) && Arrays.equals(definedInjectedMethod.getParameterTypes(), method.getParameterTypes()))
                        {
                            defined = true;
                            break;
                        }
                    }
                    
                    if(!defined)
                    {
                        component.addInjectedMethod(method);
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
            throw new WebBeansConfigurationException("Initializer method : " + method.getName() + " in class : " + clazz.getName() + " can not be annotated with @Produces");
        }
    }

    /**
     * add the definitions for a &#x0040;Initializer method.
     */
    private static <T> void checkForValidResourceMethod(AbstractInjectionTargetBean<T> component, Class<T> clazz, Method method)
    {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length != 1)
        {
            throw new WebBeansConfigurationException("Resource method : " + method.getName() + " in class : " + clazz.getName() + " must only have exactly 1 parameter with a valid resource type");
        }

        Annotation[] anns = method.getDeclaredAnnotations();
        WebBeansUtil.checkForValidResources(parameterTypes[0], clazz, method.getName(), anns);
    }

    public static void defineSimpleWebBeanInterceptorStack(AbstractBean<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can no be null");

        // @javax.interceptor.Interceptors
        EJBInterceptorConfig.configure(component.getReturnType(), component.getInterceptorStack());

        // @javax.webbeans.Interceptor
        WebBeansInterceptorConfig.configure(component, component.getInterceptorStack());
    }

    public static void defineWebBeanDecoratorStack(AbstractBean<?> component, Object object)
    {
        WebBeansDecoratorConfig.configureDecarotors(component, object);
    }

    public static <T> void defineObserverMethods(InjectionTargetBean<T> component, Class<T> clazz)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        Asserts.nullCheckForClass(clazz);

        NotificationManager manager = NotificationManager.getInstance();

        Method[] candidateMethods = AnnotationUtil.getMethodsWithParameterAnnotation(clazz, Observes.class);

        // From normal
        createObserverMethods(component, clazz, candidateMethods);

        manager.addObservableComponentMethods(component);

    }

    @SuppressWarnings("unchecked")
    private static <T> void createObserverMethods(InjectionTargetBean<T> component, Class<?> clazz, Method[] candidateMethods)
    {

        for (Method candidateMethod : candidateMethods)
        {

            EventUtil.checkObserverMethodConditions(candidateMethod, clazz);
            component.addObservableMethod(candidateMethod);

            addMethodInjectionPointMetaData((AbstractBean<T>) component, candidateMethod);
        }

    }

    public static <T> void defineSerializable(AbstractBean<T> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        if (ClassUtil.isAssignable(Serializable.class, component.getReturnType()))
        {
            component.setSerializable(true);
        }
    }

    public static <T> void addFieldInjectionPointMetaData(AbstractBean<T> owner, Field field)
    {
        InjectionPoint injectionPoint = InjectionPointFactory.getFieldInjectionPointData(owner, field);
        if (injectionPoint != null)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
        }
    }

    public static <T> void addMethodInjectionPointMetaData(AbstractBean<T> owner, Method method)
    {
        List<InjectionPoint> injectionPoints = InjectionPointFactory.getMethodInjectionPointData(owner, method);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            owner.addInjectionPoint(injectionPoint);
        }
    }

    public static <T> void addConstructorInjectionPointMetaData(AbstractBean<T> owner, Constructor<T> constructor)
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
            WebBeansUtil.addInjectedImplicitInstanceComponent(injectionPoint);
        }
        
        else if(EventUtil.checkObservableInjectionPointConditions(injectionPoint))
        {            
            WebBeansUtil.addInjectedImplicitEventComponent(injectionPoint);
        }
    }
}