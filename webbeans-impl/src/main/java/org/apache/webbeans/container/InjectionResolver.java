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
package org.apache.webbeans.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.component.InstanceComponentImpl;
import org.apache.webbeans.component.ObservableComponentImpl;
import org.apache.webbeans.component.ProducerComponentImpl;
import org.apache.webbeans.component.ProducerFieldComponent;
import org.apache.webbeans.container.activity.ActivityManager;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.inject.NullableDependencyException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

@SuppressWarnings("unchecked")
public class InjectionResolver
{
    private ManagerImpl manager;
    
    public InjectionResolver(ManagerImpl manager)
    {
        this.manager = manager;

    }

    public static InjectionResolver getInstance()
    {
        InjectionResolver instance = ActivityManager.getInstance().getCurrentActivity().getInjectionResolver();
        
        return instance;
    }
    
    /**
     * Check the type of the injection point.
     * <p>
     * Injection point type can not be wildcard or type variable type.
     * </p>
     * 
     * @param injectionPoint injection point
     * @throws WebBeansConfigurationException if not obey the rule
     */
    public void checkInjectionPointType(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        if(type instanceof Class)
        {
            return;
        }
        else if(type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType)type;
            
            if(!ClassUtil.checkParametrizedType(pt))
            {
                throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " can not contain generic definitions!");
            }                                                                                    
        }
        else
        {
            throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " can not contain generic definitions!");
        }
        
    }

    /**
     * Check that bean exist in the deployment for given
     * injection point definition.
     * 
     * @param injectionPoint injection point
     * @throws If bean is not avialable in the current deployment for given injection
     */
    public void checkInjectionPoints(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        Class<?> clazz = null;
        
        Type[] args = new Type[0];
        
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;

            if (!ClassUtil.checkParametrizedType(pt))
            {
                throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " type can not be defined as Typevariable or Wildcard type!");
            }
            
            args = pt.getActualTypeArguments();

            clazz = (Class<?>) pt.getRawType();
        }
        else
        {
            clazz = (Class<?>) type;
        }
        
        Annotation[] bindingTypes = new Annotation[injectionPoint.getBindings().size()];
        bindingTypes = injectionPoint.getBindings().toArray(bindingTypes);
        
        Set<Bean<Object>> beanSet = implResolveByType(clazz, args ,bindingTypes);
        
        ResolutionUtil.checkResolvedBeans(beanSet, clazz, bindingTypes);
        
        Bean<Object> bean = beanSet.iterator().next();
        
        if(clazz.isPrimitive())
        {
            if(bean.isNullable())
            {
                throw new NullableDependencyException("Injection point type : " + injectionPoint + " type is primitive but resolved bean can have nullable objects!");
            }
        }
        
    }
    

    public Bean<Object> getInjectionPointBean(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        Class<?> clazz = null;
        
        Type[] args = new Type[0];
        
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;

            if (!ClassUtil.checkParametrizedType(pt))
            {
                throw new WebBeansConfigurationException("Injection point : " + injectionPoint + " can not defined type variable or wildcard");
            }
            
            args = pt.getActualTypeArguments();

            clazz = (Class<?>) pt.getRawType();
        }
        else
        {
            clazz = (Class<?>) type;
        }
        
        Annotation[] bindingTypes = new Annotation[injectionPoint.getBindings().size()];
        bindingTypes = injectionPoint.getBindings().toArray(bindingTypes);
        
        Set<Bean<Object>> beanSet = implResolveByType(clazz, args ,bindingTypes);
        
        ResolutionUtil.checkResolvedBeans(beanSet, clazz);
        
        return beanSet.iterator().next();
        
    }    
        
    public Set<Bean<?>> implResolveByName(String name)
    {
        Asserts.assertNotNull(name, "name parameter can not be null");

        Set<Bean<?>> resolvedComponents = new HashSet<Bean<?>>();
        
        Bean<?> resolvedComponent = null;
        
        Set<Bean<?>> deployedComponents = this.manager.getBeans();

        Iterator<Bean<?>> it = deployedComponents.iterator();
        while (it.hasNext())
        {
            Bean<?> component = it.next();

            if (component.getName() != null)
            {
                if (component.getName().equals(name))
                {
                    if (resolvedComponent == null)
                    {
                        resolvedComponent = component;
                        resolvedComponents.add(resolvedComponent);
                    }
                    else
                    {
                        if (DeploymentTypeManager.getInstance().comparePrecedences(component.getDeploymentType(), resolvedComponent.getDeploymentType()) > 0)
                        {
                            resolvedComponents.clear();
                            resolvedComponent = component;
                            resolvedComponents.add(resolvedComponent);
                        }
                        else if (DeploymentTypeManager.getInstance().comparePrecedences(component.getDeploymentType(), resolvedComponent.getDeploymentType()) == 0)
                        {
                            resolvedComponents.add(component);
                        }
                    }
                }
            }
        }

        return resolvedComponents;
    }

    /**
     * Resolution by type.
     * 
     * @param <T> bean type info
     * @param apiType injection point api type
     * @param actualTypeArguments actual type arguments if parameterized type
     * @param binding binding type of the injection point
     * @return set of resolved beans
     */
    public <T> Set<Bean<T>> implResolveByType(Class<?> apiType, Type[] actualTypeArguments, Annotation... binding)
    {
        Asserts.assertNotNull(apiType, "apiType parameter can not be null");
        Asserts.assertNotNull(binding, "binding parameter can not be null");
        
        if(apiType.isPrimitive())
        {
            apiType = ClassUtil.getPrimitiveWrapper(apiType);
        }

        boolean currentBinding = false;
        boolean returnAll = false;

        if (binding.length == 0)
        {
            binding = new Annotation[1];
            binding[0] = new CurrentLiteral();
            currentBinding = true;
        }

        Set<Bean<T>> results = new HashSet<Bean<T>>();
        Set<Bean<?>> deployedComponents = this.manager.getBeans();

        if (apiType.equals(Object.class) && currentBinding)
        {
            returnAll = true;
        }

        Iterator<Bean<?>> it = deployedComponents.iterator();

        while (it.hasNext())
        {
            Bean<?> component = it.next();

            if (returnAll)
            {
                results.add((Bean<T>) component);
                continue;
            }

            else
            {
                Set<Type> componentApiTypes = component.getTypes();
                Iterator<Type> itComponentApiTypes = componentApiTypes.iterator();
                while (itComponentApiTypes.hasNext())
                {
                    Class<?> componentApiType = (Class<?>)itComponentApiTypes.next();
                    
                    if(componentApiType.isPrimitive())
                    {
                        componentApiType = ClassUtil.getPrimitiveWrapper(componentApiType);
                    }

                    if (actualTypeArguments.length > 0)
                    {
                        Type[] actualArgs = null;
                        
                        if (ClassUtil.isAssignable(apiType, componentApiType))
                        {
                            if (ProducerComponentImpl.class.isAssignableFrom(component.getClass()))
                            {
                                actualArgs = ((ProducerComponentImpl<?>) component).getActualTypeArguments();
                                if (Arrays.equals(actualArgs, actualTypeArguments))
                                {
                                    results.add((Bean<T>) component);
                                    break;
                                }

                            }
                            
                            else if(component instanceof ObservableComponentImpl)
                            {
                                ObservableComponentImpl<?, ?> observableComponent = (ObservableComponentImpl<?, ?>)component;
                                Class<?> eventType = (Class<?>)actualTypeArguments[0];
                                if(eventType.equals(observableComponent.getEventType()))
                                {
                                    results.add((Bean<T>) component);
                                    break;
                                }
                            }
                            else if(component instanceof InstanceComponentImpl)
                            {
                                InstanceComponentImpl<?> instanceComponent = (InstanceComponentImpl<?>)component;
                                actualArgs = instanceComponent.getActualTypeArguments();
                                
                                if (Arrays.equals(actualArgs, actualTypeArguments))
                                {
                                    results.add((Bean<T>) component);
                                    break;
                                }                                                                
                            }
                            else if(component instanceof ProducerFieldComponent)
                            {
                                ProducerFieldComponent<?> pf = (ProducerFieldComponent<?>)component;
                                actualArgs = pf.getActualTypeArguments();
                                
                                if (Arrays.equals(actualArgs, actualTypeArguments))
                                {
                                    results.add((Bean<T>) component);
                                    break;
                                }
                                
                            }

                            else
                            {
                                actualArgs = ClassUtil.getGenericSuperClassTypeArguments(componentApiType);
                                if (Arrays.equals(actualArgs, actualTypeArguments))
                                {
                                    results.add((Bean<T>) component);
                                    break;
                                }
                                else
                                {
                                    List<Type[]> listActualArgs = ClassUtil.getGenericSuperInterfacesTypeArguments(componentApiType);
                                    Iterator<Type[]> itListActualArgs = listActualArgs.iterator();
                                    while (itListActualArgs.hasNext())
                                    {
                                        actualArgs = itListActualArgs.next();

                                        if (Arrays.equals(actualArgs, actualTypeArguments))
                                        {
                                            results.add((Bean<T>) component);
                                            break;
                                        }

                                    }
                                }

                            }

                        }
                    }
                    else
                    {
                        if (ClassUtil.isAssignable(apiType, componentApiType))
                        {
                            results.add((Bean<T>) component);
                            break;
                        }
                    }
                }
            }
        }

        results = findByBindingType(results, binding);

        
        if (results != null && !results.isEmpty())
        {
            results = findByPrecedence(results);
        }
        

        return results;
    }

    private <T> Set<Bean<T>> findByPrecedence(Set<Bean<T>> result)
    {
        Bean<T> resolvedComponent = null;
        Iterator<Bean<T>> it = result.iterator();
        Set<Bean<T>> res = new HashSet<Bean<T>>();

        while (it.hasNext())
        {
            Bean<T> component = it.next();

            if (resolvedComponent == null)
            {
                resolvedComponent = component;
                res.add(resolvedComponent);
            }
            else
            {
                DeploymentTypeManager typeManager = DeploymentTypeManager.getInstance();

                if (typeManager.comparePrecedences(component.getDeploymentType(), resolvedComponent.getDeploymentType()) < 0)
                {
                    continue;
                }
                else if (typeManager.comparePrecedences(component.getDeploymentType(), resolvedComponent.getDeploymentType()) > 0)
                {
                    res.clear();
                    resolvedComponent = component;
                    res.add(resolvedComponent);

                }
                else
                {
                    res.add(component);
                }
            }
        }

        return res;
    }

    private <T> Set<Bean<T>> findByBindingType(Set<Bean<T>> remainingSet, Annotation... annotations)
    {
        Iterator<Bean<T>> it = remainingSet.iterator();
        Set<Bean<T>> result = new HashSet<Bean<T>>();

        while (it.hasNext())
        {
            Bean<T> component = it.next();
            Set<Annotation> bTypes = component.getBindings();

            int i = 0;
            for (Annotation annot : annotations)
            {
                Iterator<Annotation> itBindingTypes = bTypes.iterator();
                while (itBindingTypes.hasNext())
                {
                    Annotation bindingType = itBindingTypes.next();
                    if (annot.annotationType().equals(bindingType.annotationType()))
                    {
                        if (AnnotationUtil.isAnnotationMemberExist(bindingType.annotationType(), bindingType, annot))
                        {
                            i++;
                        }
                    }

                }
            }

            if (i == annotations.length)
            {
                result.add(component);
                i = 0;
            }

        }

        return result;
    }
}