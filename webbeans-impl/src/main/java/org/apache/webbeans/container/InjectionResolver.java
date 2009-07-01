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
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.activity.ActivityManager;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.inject.NullableDependencyException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

/**
 * Injection point resolver class. 
 * 
 * <p>
 * It is a singleton class per ClassLoader per JVM. It is
 * responsible for resolbing the bean instances at the injection points for 
 * its bean manager.
 * </p>
 * 
 * @version $Rev$ $Date$
 * @see WebBeansFinder
 */
public class InjectionResolver
{
    /**Bean Manager*/
    private ManagerImpl manager;
    
    /**
     * Creates a new injection resolve for given bean manager.
     * 
     * @param manager bean manager
     */
    public InjectionResolver(ManagerImpl manager)
    {
        this.manager = manager;

    }

    /**
     * Returns bean manager injection resolver.
     * 
     * @return bean manager injection resolver
     * @see WebBeansFinder
     */
    public static InjectionResolver getInstance()
    {
        InjectionResolver instance = ActivityManager.getInstance().getCurrentActivity().getInjectionResolver();
        
        return instance;
    }
    
    /**
     * Check the type of the injection point.
     * <p>
     * Injection point type can not be {@link TypeVariable}.
     * </p>
     * 
     * @param injectionPoint injection point
     * @throws WebBeansConfigurationException if not obey the rule
     */
    public void checkInjectionPointType(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        //Check for injection point type variable
        if(ClassUtil.isTypeVariable(type))
        {
            throw new WebBeansConfigurationException("Injection point type : " + injectionPoint +  " can not define Type Variable generic type");
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
        
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;

            if (!ClassUtil.checkParametrizedType(pt))
            {
                throw new WebBeansConfigurationException("Injection point type : " + injectionPoint + " type can not be defined as Typevariable or Wildcard type!");
            }
            
            clazz = (Class<?>) pt.getRawType();
        }
        else
        {
            clazz = (Class<?>) type;
        }
        
        Annotation[] bindingTypes = new Annotation[injectionPoint.getBindings().size()];
        bindingTypes = injectionPoint.getBindings().toArray(bindingTypes);
        
        Set<Bean<?>> beanSet = implResolveByType(type ,bindingTypes);
        
        ResolutionUtil.checkResolvedBeans(beanSet, clazz, bindingTypes);
        
        Bean<?> bean = beanSet.iterator().next();
        
        if(clazz.isPrimitive())
        {
            if(bean.isNullable())
            {
                throw new NullableDependencyException("Injection point type : " + injectionPoint + " type is primitive but resolved bean can have nullable objects!");
            }
        }
        
    }
    

    /**
     * Returns bean for injection point.
     * 
     * @param injectionPoint injection point declaration
     * @return bean for injection point
     */
    public Bean<?> getInjectionPointBean(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        Class<?> clazz = null;
        
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;            
            clazz = (Class<?>) pt.getRawType();
        }
        else
        {
            clazz = (Class<?>) type;
        }
        
        Annotation[] bindingTypes = new Annotation[injectionPoint.getBindings().size()];
        bindingTypes = injectionPoint.getBindings().toArray(bindingTypes);
        
        Set<Bean<?>> beanSet = implResolveByType(type ,bindingTypes);
        
        ResolutionUtil.checkResolvedBeans(beanSet, clazz, bindingTypes);
        
        return beanSet.iterator().next();
        
    }    
        
    /**
     * Returns set of beans for given bean name.
     * 
     * @param name bean name
     * @return set of beans for given bean name
     */
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
        
        //Still Ambigious, check for specialization
        if(resolvedComponents.size() > 1)
        {
            //Check for specialization
            Set<Bean<?>> specializedComponents = findSpecializedForNameResolution(resolvedComponents);        
            if(specializedComponents.size() > 0)
            {
                return specializedComponents;
            }            
        }
                
        return resolvedComponents;
    }
     
    
    /**
     * Returns filtered set by specialization.
     * 
     * @param resolvedComponents result beans
     * @return filtered set by specialization
     */
    private Set<Bean<?>> findSpecializedForNameResolution(Set<Bean<?>> resolvedComponents)
    {
        Set<Bean<?>> specializedComponents = new HashSet<Bean<?>>(); 
        if(resolvedComponents.size() > 0)
        {
            for(Bean<?> bean : resolvedComponents)
            {
                AbstractComponent<?> component = (AbstractComponent<?>)bean;
                
                if(component.isSpecializedBean())
                {
                    specializedComponents.add(component);
                }
            }
        }
        
        return specializedComponents;
    }

    /**
     * Resolution by type.
     * 
     * @param <T> bean type info
     * @param injectionPointType injection point api type
     * @param injectionPointTypeArguments actual type arguments if parameterized type
     * @param binding binding type of the injection point
     * @return set of resolved beans
     */
    public Set<Bean<?>> implResolveByType(Type injectionPointType, Annotation... binding)
    {
        Asserts.assertNotNull(injectionPointType, "injectionPointType parameter can not be null");
        Asserts.assertNotNull(binding, "binding parameter can not be null");
        
        Set<Bean<?>> results = new HashSet<Bean<?>>();
        Set<Bean<?>> deployedComponents = this.manager.getBeans();

        boolean currentBinding = false;
        boolean returnAll = false;

        if (binding.length == 0)
        {
            binding = new Annotation[1];
            binding[0] = new CurrentLiteral();
            currentBinding = true;
        }
        
        if (injectionPointType.equals(Object.class) && currentBinding)
        {
            returnAll = true;
        }

        Iterator<Bean<?>> it = deployedComponents.iterator();

        while (it.hasNext())
        {
            Bean<?> component = it.next();

            if (returnAll)
            {
                results.add((Bean<?>) component);
                continue;
            }

            else
            {
                Set<Type> componentApiTypes = component.getTypes();
                Iterator<Type> itComponentApiTypes = componentApiTypes.iterator();
                while (itComponentApiTypes.hasNext())
                {
                    Type componentApiType = itComponentApiTypes.next();                    
                    
                    if(ClassUtil.isAssignable(componentApiType, injectionPointType))
                    {
                        results.add((Bean<?>) component);
                        break;                                            
                    }                    
                }
            }            
        }
 
        //Look for binding types
        results = findByBindingType(results, binding);
        
        //Look for precedence
        results = findByPrecedence(results);
        
        //Ambigious resulotion, check for specialization
        if(results.size() > 1)
        {
            //Look for specialization
            results = findBySpecialization(results);            
        }
        
        return results;
    }
    
    /**
     * Returns specialized beans if exists, otherwise return input result
     * 
     * @param <T> bean class type
     * @param result result beans
     * @return specialized beans if exists, otherwise return input result
     */
    public Set<Bean<?>> findBySpecialization(Set<Bean<?>> result)
    {
        Iterator<Bean<?>> it = result.iterator();
        Set<Bean<?>> res = new HashSet<Bean<?>>();
        
        while(it.hasNext())
        {
            AbstractComponent<?> component = (AbstractComponent<?>)it.next();
            if(component.isSpecializedBean())
            {
                res.add(component);
            }
        }
        
        if(res.size() > 0)
        {
            return res;
        }
        
        return result;
    }
    
    /**
     * Return filtered beans according to the deployment type precedence.
     * 
     * @param <T> bean class
     * @param result resulted beans
     * @return filtered beans according to the deployment type precedence
     */
    public Set<Bean<?>> findByPrecedence(Set<Bean<?>> result)
    {
        Bean<?> resolvedComponent = null;
        Iterator<Bean<?>> it = result.iterator();
        Set<Bean<?>> res = new HashSet<Bean<?>>();

        while (it.hasNext())
        {
            Bean<?> component = it.next();

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

    /**
     * Returns filtered bean set according to the binding types.
     * 
     * @param <T> bean class
     * @param remainingSet bean set for filtering by binding type
     * @param annotations binding types on injection point
     * @return filtered bean set according to the binding types
     */
    private Set<Bean<?>> findByBindingType(Set<Bean<?>> remainingSet, Annotation... annotations)
    {
        Iterator<Bean<?>> it = remainingSet.iterator();
        Set<Bean<?>> result = new HashSet<Bean<?>>();

        while (it.hasNext())
        {
            Bean<?> component = it.next();
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