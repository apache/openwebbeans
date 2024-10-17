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

import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanAttributes;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.config.BeansDeployer;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.InconsistentSpecializationException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;

/**
 * This class contains a few helpers for handling
 * &#064;Specializes.
 */
public class SpecializationUtil
{
    private final AlternativesManager alternativesManager;
    private final WebBeansUtil webBeansUtil;
    private final WebBeansContext webBeansContext;
    private final OpenWebBeansEjbPlugin ejbPlugin;

    public SpecializationUtil(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        this.alternativesManager = webBeansContext.getAlternativesManager();
        this.webBeansUtil = webBeansContext.getWebBeansUtil();
        this.ejbPlugin = webBeansContext.getPluginLoader().getEjbPlugin();
    }

    /**
     * This method iterates over all given BeanAttributes and removes those which are 'Specialised away'.
     * This methods gets invoked twice.
     * The first pass is over the plain scanned classes.
     * The second pass is for any specialised producer fields and methods.
     * We need to do this twice as producers of 'disabled beans' must not get taken into consideration.
     *
     * @param beanAttributesPerBda all annotatypes sliced by BDA
     * @param attributeProvider if not null provides bean attributes to be able to validate types contains superclass. Needed for producers.
     * @param notSpecializationOnly first pass/2nd pass. First one removes only root beans, second one handles inheritance even in @Spe
     */
    public void removeDisabledBeanAttributes(Map<BeanArchiveService.BeanArchiveInformation, Map<AnnotatedType<?>, BeansDeployer.ExtendedBeanAttributes<?>>> beanAttributesPerBda,
                                             BeanAttributesProvider attributeProvider,
                                             boolean notSpecializationOnly)
    {
        Set<AnnotatedType<?>> allAnnotatedTypes = getAllAnnotatedTypes(beanAttributesPerBda);

        if (allAnnotatedTypes != null && !allAnnotatedTypes.isEmpty())
        {
            // superClassList is used to handle the case: Car, CarToyota, Bus, SchoolBus, CarFord
            // for which case OWB should throw exception that both CarToyota and CarFord are
            // specialize Car.
            // see spec section 5.1.3
            Set<Class<?>> superClassList = new HashSet<>();

            // first let's find all superclasses of Specialized types
            Set<Class<?>> disabledClasses = new HashSet<>();
            for(AnnotatedType<?> annotatedType : allAnnotatedTypes)
            {
                if(annotatedType.getAnnotation(Specializes.class) != null && isEnabled(annotatedType))
                {
                    Class<?> specialClass = annotatedType.getJavaClass();
                    Class<?> superClass = specialClass.getSuperclass();
                    if (ejbPlugin != null && ejbPlugin.isSessionBean(superClass) && !ejbPlugin.isSessionBean(specialClass))
                    {
                        throw new WebBeansConfigurationException(specialClass + " specializes and EJB " + superClass + ". That's forbidden.");
                    }

                    if (attributeProvider != null)
                    {
                        BeanAttributes<?> ba = attributeProvider.get(annotatedType);
                        if (ba == null || !ba.getTypes().contains(superClass))
                        {
                            throw new WebBeansDeploymentException(new InconsistentSpecializationException("@Specializes class " + specialClass.getName()
                                    + " does not extend a bean with a valid bean constructor - removed with ProcessBeanAttribute"));
                        }
                    }

                    if(superClass.equals(Object.class))
                    {
                        throw new WebBeansDeploymentException(new WebBeansConfigurationException(WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0003)
                                + specialClass.getName() + WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0004)));
                    }
                    if (superClassList.contains(superClass))
                    {
                        // since CDI 1.1 we have to wrap this in a DeploymentException
                        throw new WebBeansDeploymentException(new InconsistentSpecializationException(WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0005) +
                                                                       superClass.getName()));
                    }
                    if (!containsAllSuperclassTypes(annotatedType, superClass, allAnnotatedTypes))
                    {
                        throw new WebBeansDeploymentException(new InconsistentSpecializationException("@Specialized Class : " + specialClass.getName()
                                                                          + " must have all bean types of its super class"));
                    }

                    AnnotatedType<?> superType = getAnnotatedTypeForClass(allAnnotatedTypes, superClass);
                    if (notSpecializationOnly)
                    {
                        if ((superType == null && !webBeansContext.findMissingAnnotatedType(superClass)) || (superType != null && !webBeansUtil.isConstructorOk(superType)))
                        {
                            throw new WebBeansDeploymentException(new InconsistentSpecializationException("@Specializes class " + specialClass.getName()
                                    + " does not extend a bean with a valid bean constructor"));
                        }

                        try
                        {
                            webBeansUtil.checkManagedBean(specialClass);
                        }
                        catch (WebBeansConfigurationException illegalBeanTypeException)
                        {
                            // this Exception gets thrown if the given class is not a valid bean type
                            throw new WebBeansDeploymentException(new InconsistentSpecializationException("@Specializes class " + specialClass.getName()
                                    + " does not extend a valid bean type", illegalBeanTypeException));
                        }
                    }

                    superClassList.add(superClass);

                    while (!superClass.equals(Object.class))
                    {
                        disabledClasses.add(superClass);
                        superClass = superClass.getSuperclass();
                    }
                }
            }

            // and now remove all AnnotatedTypes of those collected disabledClasses
            removeAllDisabledClasses(beanAttributesPerBda, disabledClasses);
        }
    }

    private void removeAllDisabledClasses(Map<BeanArchiveService.BeanArchiveInformation, Map<AnnotatedType<?>, BeansDeployer.ExtendedBeanAttributes<?>>> beanAttributesPerBda,
                                          Set<Class<?>> disabledClasses)
    {
        for (Map<AnnotatedType<?>, BeansDeployer.ExtendedBeanAttributes<?>> beanAttributeMap : beanAttributesPerBda.values())
        {
            Set<AnnotatedType<?>> toRemove = new HashSet<>();
            for (Map.Entry<AnnotatedType<?>, BeansDeployer.ExtendedBeanAttributes<?>> beanAttributesEntry : beanAttributeMap.entrySet())
            {
                if (disabledClasses.contains(beanAttributesEntry.getKey().getJavaClass()))
                {
                    toRemove.add(beanAttributesEntry.getKey());
                }
            }
            for (AnnotatedType<?> annotatedType : toRemove)
            {
                beanAttributeMap.remove(annotatedType);
            }
        }
    }

    private Set<AnnotatedType<?>> getAllAnnotatedTypes(
        Map<BeanArchiveService.BeanArchiveInformation, Map<AnnotatedType<?>, BeansDeployer.ExtendedBeanAttributes<?>>> beanAttributesPerBda)
    {
        Set<AnnotatedType<?>> allAnnotatedTypes = new HashSet<>(beanAttributesPerBda.size() * 50);
        for (Map<AnnotatedType<?>, BeansDeployer.ExtendedBeanAttributes<?>> annotatedTypeMap : beanAttributesPerBda.values())
        {
            allAnnotatedTypes.addAll(annotatedTypeMap.keySet());
        }
        return allAnnotatedTypes;
    }

    private boolean containsAllSuperclassTypes(AnnotatedType<?> annotatedType, Class<?> superClass, Collection<AnnotatedType<?>> annotatedTypes)
    {
        Typed typed = annotatedType.getAnnotation(Typed.class);
        if (typed != null)
        {
            List<Class<?>> typeList = Arrays.asList(typed.value());
            AnnotatedType<?> superType = getAnnotatedTypeForClass(annotatedTypes, superClass);
            if (superType != null)
            {
                Typed superClassTyped = superType.getAnnotation(Typed.class);
                Set<Type> superClassTypes;
                if (superClassTyped != null)
                {
                    superClassTypes = new HashSet<>(Arrays.asList(superClassTyped.value()));
                }
                else
                {
                    superClassTypes = superType.getTypeClosure();

                    // we can ignore Object.class in this case
                    superClassTypes.remove(Object.class);
                }

                return typeList.containsAll(superClassTypes);
            }
        }
        return true;
    }

    private AnnotatedType<?> getAnnotatedTypeForClass(Collection<AnnotatedType<?>> annotatedTypes, Class<?> clazz)
    {
        for (AnnotatedType<?> annotatedType : annotatedTypes)
        {
            if (annotatedType.getJavaClass().equals(clazz))
            {
                return annotatedType;
            }
        }

        return null;
    }

    /**
     * @return true if the AnnotatedType is an enabled Alternative or no alternative at all
     */
    private boolean isEnabled(AnnotatedType<?> annotatedType)
    {
        return annotatedType.getAnnotation(Alternative.class) == null ||
                alternativesManager.isAlternative(annotatedType.getJavaClass(), getAnnotationClasses(annotatedType));
    }

    private Set<Class<? extends Annotation>> getAnnotationClasses(AnnotatedType<?> annotatedType)
    {
        Set<Annotation> annotations = annotatedType.getAnnotations();
        if (annotations != null && !annotations.isEmpty())
        {
            Set<Class<? extends Annotation>> annotationClasses = new HashSet<>(annotations.size());
            for (Annotation annotation : annotations)
            {
                annotationClasses.add(annotation.annotationType());
            }

            return annotationClasses;
        }
        return Collections.emptySet();
    }

    public interface BeanAttributesProvider
    {
        <T> BeanAttributes<T> get(AnnotatedType<T> at);
    }
}
