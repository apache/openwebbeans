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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Named;
import javax.annotation.NonBinding;
import javax.context.ScopeType;
import javax.event.Fires;
import javax.event.Observes;
import javax.inject.DeploymentType;
import javax.inject.Disposes;
import javax.inject.Initializer;
import javax.inject.Produces;
import javax.inject.Specializes;
import javax.inject.UnsatisfiedDependencyException;
import javax.inject.manager.Bean;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.Component;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.component.ObservesMethodsOwner;
import org.apache.webbeans.component.ProducerComponentImpl;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.event.EventUtil;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.intercept.ejb.EJBInterceptorConfig;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Defines the web beans components common properties.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
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

        if (!found && (component instanceof ProducerComponentImpl))
        {
            ProducerComponentImpl<?> p = (ProducerComponentImpl<?>) component;
            component.setType(p.getParent().getType());
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

    public static Set<ProducerComponentImpl<?>> defineProducerMethods(AbstractComponent<?> component)
    {
        Set<ProducerComponentImpl<?>> producerComponents = new HashSet<ProducerComponentImpl<?>>();

        Class<?> clazz = component.getReturnType();
        Method[] declaredMethods = clazz.getDeclaredMethods();
        boolean isSpecializes = false;
        for (Method declaredMethod : declaredMethods)
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
                    producerComponents.add(newComponent);
                }
            }
        }

        return producerComponents;

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

    public static <T> void defineDisposalMethods(AbstractComponent<T> component)
    {
        Class<?> clazz = component.getReturnType();

        Method[] methods = AnnotationUtil.getMethodsWithParameterAnnotation(clazz, Disposes.class);

        ProducerComponentImpl<?> previous = null;
        for (Method declaredMethod : methods)
        {
            WebBeansUtil.checkProducerMethodDisposal(declaredMethod, clazz.getName());

            Type type = AnnotationUtil.getMethodFirstParameterWithAnnotation(declaredMethod, Disposes.class);
            Annotation[] annot = AnnotationUtil.getMethodFirstParameterBindingTypesWithGivenAnnotation(declaredMethod, Disposes.class);

            Set<Bean<T>> set = InjectionResolver.getInstance().implResolveByType(ClassUtil.getFirstRawType(type), ClassUtil.getActualTypeArguements(type), annot);
            ProducerComponentImpl<?> pr = (ProducerComponentImpl<?>) set.iterator().next();

            if (pr == null)
            {
                throw new UnsatisfiedDependencyException("Producer method component of the disposal method : " + declaredMethod.getName() + " in class : " + clazz.getName() + "is not found");
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
        }
    }

    public static <T> void defineInjectedFields(ComponentImpl<T> component)
    {
        Class<T> clazz = component.getReturnType();

        WebBeansUtil.checkObservableFieldsConditions(clazz);

        Field[] fields = clazz.getDeclaredFields();
        if (fields.length != 0)
        {
            for (Field field : fields)
            {
                Annotation[] anns = field.getAnnotations();

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
                    }
                }
                
            }
        }

    }

    public static <T> void defineInjectedMethods(ComponentImpl<T> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");

        Class<T> clazz = component.getReturnType();
        Method[] methods = AnnotationUtil.getMethodsWithAnnotation(clazz, Initializer.class);
        if (methods.length != 0)
        {
            for (Method method : methods)
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
                    if (!Modifier.isStatic(method.getModifiers()))
                    {
                        component.addInjectedMethod(method);
                    }

                }
                else
                {
                    throw new WebBeansConfigurationException("Initializer method : " + method.getName() + " in class : " + clazz.getName() + " can not be annotated with @Produces or @Destructor");
                }
            }
        }
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

        for (Method candidateMethod : candidateMethods)
        {
            EventUtil.checkObserverMethodConditions(candidateMethod, clazz);
            component.addObservableMethod(candidateMethod);
        }

        manager.addObservableComponentMethods(component);

    }

    public static <T> void defineSerializable(AbstractComponent<T> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        if (ClassUtil.isAssignable(Serializable.class, component.getReturnType()))
        {
            component.setSerializable(true);
        }
    }
}