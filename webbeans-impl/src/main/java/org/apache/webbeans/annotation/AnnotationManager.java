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
package org.apache.webbeans.annotation;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.deployment.stereotype.IStereoTypeModel;
import org.apache.webbeans.exception.WebBeansConfigurationException;


import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ArrayUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.interceptor.InterceptorBinding;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages annotation usage by classes in this application.
 */
public final class AnnotationManager
{
    private Map<Class<? extends Annotation>, Boolean> checkedQualifierAnnotations =
        new ConcurrentHashMap<>();
    private Map<Class<? extends Annotation>, Boolean> checkedStereotypeAnnotations =
        new ConcurrentHashMap<>();

    private ConcurrentMap<Class<?>, Optional<Method>> repeatableMethodCache = new ConcurrentHashMap<>();

    private final BeanManagerImpl beanManagerImpl;
    private final WebBeansContext webBeansContext;

    private final boolean strictValidation;

    // No instantiate

    public AnnotationManager(WebBeansContext context)
    {
        webBeansContext = context;
        beanManagerImpl = context.getBeanManagerImpl();
        strictValidation = context.getOpenWebBeansConfiguration().strictDynamicValidation();
    }

    public Annotation getDeclaredScopeAnnotation(Class<?> beanClass)
    {
        for (Annotation annotation : beanClass.getDeclaredAnnotations())
        {
            if (beanManagerImpl.isScope(annotation.annotationType()))
            {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link javax.interceptor.InterceptorBinding} or an InterceptorBinding
     * registered via {@link javax.enterprise.inject.spi.BeforeBeanDiscovery}.
     * False otherwise.
     *
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link javax.interceptor.InterceptorBinding}, false otherwise
     */
    public boolean isInterceptorBindingAnnotation(Class<? extends Annotation> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        return clazz.isAnnotationPresent(InterceptorBinding.class)
               || webBeansContext.getInterceptorsManager().hasInterceptorBindingType(clazz);
    }


    /**
     * This method searches for all direct and indirect annotations which
     * represent an {@link InterceptorBinding}.
     * InterceptorBindings in stereotypes will also be found!
     *
     * @return the effective interceptor annotations of the array of given annotations
     */
    public Set<Annotation> getInterceptorAnnotations(Set<Annotation> typeAnns)
    {
        // use a map to ensure that every annotation type is present only once
        Map<Class<? extends Annotation>, Annotation> bindings = new HashMap<>();

        Annotation[] anns = getInterceptorBindingMetaAnnotations(typeAnns);

        for (Annotation ann : anns)
        {
            Annotation oldBinding = bindings.get(ann.annotationType());
            if (oldBinding != null && !AnnotationUtil.isCdiAnnotationEqual(oldBinding, ann))
            {
                throw new WebBeansConfigurationException("Illegal interceptor binding: annotation of type "
                        + ann.annotationType().getName()
                        + " is present twice with diffenent values: "
                        + oldBinding.toString() + " and " + ann.toString());
            }
            bindings.put(ann.annotationType(), ann);
        }

        // check for stereotypes _explicitly_ declared on the bean class (not inherited)
        Annotation[] stereoTypes = getStereotypeMetaAnnotations(typeAnns.toArray(new Annotation[typeAnns.size()]));
        Map<Class<? extends Annotation>, Annotation> annotationsFromSteretypes = new HashMap<>();
        for (Annotation stereoType : stereoTypes)
        {
            if (hasInterceptorBindingMetaAnnotation(stereoType.annotationType().getDeclaredAnnotations()))
            {
                Annotation[] steroInterceptorBindings = getInterceptorBindingMetaAnnotations(stereoType.annotationType().getDeclaredAnnotations());

                for (Annotation ann : steroInterceptorBindings)
                {
                    Annotation oldBinding = bindings.get(ann.annotationType());

                    // annotations which are declared on bean class overrides the one
                    // declared in stereotypes
                    if (oldBinding == null)
                    {
                        bindings.put(ann.annotationType(), ann);
                    }
                    else
                    {
                        if (annotationsFromSteretypes.containsKey(ann.annotationType()) && !AnnotationUtil.isCdiAnnotationEqual(oldBinding, ann))
                        {
                            throw new WebBeansConfigurationException("Illegal interceptor binding: annotation of type "
                                    + ann.annotationType().getName()
                                    + " is present twice with diffenent values: "
                                    + oldBinding.toString() + " and " + ann.toString());
                        }
                    }

                    annotationsFromSteretypes.put(ann.annotationType(), ann);
                }
            }
        }

        return new HashSet<>(bindings.values());
    }

    /**
     * If any Annotations in the input is an interceptor binding annotation type then return
     * true, false otherwise.
     *
     * @param anns array of Annotations to check
     * @return true if one or moe of the input annotations are an interceptor binding annotation
     *         type false otherwise
     */
    public boolean hasInterceptorBindingMetaAnnotation(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, Asserts.PARAM_NAME_ANNOTATION);

        for (Annotation ann : anns)
        {
            if (isInterceptorBindingAnnotation(ann.annotationType()))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Collect the interceptor bindings from an array of annotations, including
     * transitively defined interceptor bindings.
     * @param anns An array of annotations
     * @return an array of interceptor binding annotations, including the input and any transitively declared annotations
     */
    public Annotation[] getInterceptorBindingMetaAnnotations(Set<Annotation> anns)
    {
        return getInterceptorBindingMetaAnnotations(AnnotationUtil.asArray(anns));
    }

    /**
     * Collect the interceptor bindings from an array of annotations, including
     * transitively defined interceptor bindings.
     * @param anns An array of annotations
     * @return an array of interceptor binding annotations, including the input and any transitively declared annotations
     */
    public Annotation[] getInterceptorBindingMetaAnnotations(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, Asserts.PARAM_NAME_ANNOTATION);
        List<Annotation> interAnns = new ArrayList<>();

        for (Annotation ann : anns)
        {
            if (ann.annotationType().getName().startsWith("java.lang."))
            {
                continue;
            }
            if (isInterceptorBindingAnnotation(ann.annotationType()))
            {
                interAnns.add(ann);

                //check for transitive
                Annotation[] transitives = getInterceptorBindingMetaAnnotations(ann.annotationType().getDeclaredAnnotations());

                Collections.addAll(interAnns, transitives);
            }
        }

        Annotation[] ret = new Annotation[interAnns.size()];
        ret = interAnns.toArray(ret);

        return ret;
    }


    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link javax.inject.Qualifier} false otherwise.
     *
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link javax.inject.Qualifier} false otherwise
     */
    public boolean isQualifierAnnotation(Class<? extends Annotation> clazz)
    {
        Boolean checkedAnnotationResult = checkedQualifierAnnotations.get(clazz);

        if (checkedAnnotationResult != null)
        {
            return checkedAnnotationResult;
        }

        boolean result = false;

        Asserts.nullCheckForClass(clazz);
        if (clazz.isAnnotationPresent(Qualifier.class))
        {
            result = true;
        }
        else if(beanManagerImpl.getAdditionalQualifiers().contains(clazz))
        {
            result = true;
        }

        checkedQualifierAnnotations.put(clazz, result);

        return result;
    }

    public <X> Annotation[] getAnnotatedMethodFirstParameterQualifierWithGivenAnnotation(
            AnnotatedMethod<X> annotatedMethod, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(annotatedMethod, "annotatedMethod");
        Asserts.nullCheckForClass(clazz);

        List<Annotation> list = new ArrayList<>();
        List<AnnotatedParameter<X>> parameters = annotatedMethod.getParameters();
        for(AnnotatedParameter<X> parameter : parameters)
        {
            if(parameter.isAnnotationPresent(clazz))
            {
                Annotation[] anns = AnnotationUtil.asArray(parameter.getAnnotations());
                for(Annotation ann : anns)
                {
                    if(isQualifierAnnotation(ann.annotationType()))
                    {
                        list.add(ann);
                    }
                }
            }
        }

        Annotation[] finalAnns = new Annotation[list.size()];
        finalAnns = list.toArray(finalAnns);

        return finalAnns;
    }


    /**
     * Gets the method first found parameter qualifiers.
     *
     * @param method method
     * @param clazz checking annotation
     * @return annotation array
     */
    public Annotation[] getMethodFirstParameterQualifierWithGivenAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method");
        Asserts.nullCheckForClass(clazz);

        Annotation[][] parameterAnns = method.getParameterAnnotations();
        List<Annotation> list = new ArrayList<>();
        Annotation[] result;

        for (Annotation[] parameters : parameterAnns)
        {
            boolean found = false;
            for (Annotation param : parameters)
            {
                Class<? extends Annotation> btype = param.annotationType();
                if (btype.equals(clazz))
                {
                    found = true;
                    continue;
                }

                if (isQualifierAnnotation(btype))
                {
                    list.add(param);
                }

            }

            if (found)
            {
                result = new Annotation[list.size()];
                result = list.toArray(result);
                return result;
            }
        }
        result = AnnotationUtil.EMPTY_ANNOTATION_ARRAY;
        return result;
    }

    public Annotation[] getQualifierAnnotations(Annotation... annotations)
    {
        Set<Annotation> qualifiers = getQualifierAnnotations(Arrays.asList(annotations));
        return qualifiers.toArray(new Annotation[qualifiers.size()]);
    }

    /**
     * Gets the array of qualifier annotations on the given array.
     *
     * @param anns annotation array
     * @return array containing qualifier anns
     */
    public Set<Annotation> getQualifierAnnotations(Collection<Annotation> anns)
    {
        Asserts.assertNotNull(anns, Asserts.PARAM_NAME_ANNOTATION);

        if (anns.isEmpty())
        {
            return DefaultLiteral.SET;
        }

        Set<Annotation> set = new HashSet<>();

        for (Annotation annot : anns)
        {
            if (isQualifierAnnotation(annot.annotationType()))
            {
                set.add(annot);
            }
        }

        //Add the default qualifier if no others exist.  Section 3.10, OWB-142///
        if(set.isEmpty())
        {
            return DefaultLiteral.SET;
        }

        return set;
    }

    public void checkQualifierConditions(Annotation... qualifierAnnots)
    {
        if (qualifierAnnots == null || qualifierAnnots.length == 0)
        {
            return;
        }

        if (qualifierAnnots.length == 1)
        {
            // performance hack to avoid Set creation
            checkQualifierConditions(qualifierAnnots[0]);
            return;
        }

        Set<Annotation> annSet = ArrayUtil.asSet(qualifierAnnots);

        //check for duplicate annotations
        if (qualifierAnnots.length != annSet.size())
        {
            throw new IllegalArgumentException("Qualifier annotations can not contain duplicate qualifiers:"
                                               + Arrays.toString(qualifierAnnots));
        }

        checkQualifierConditions(annSet);
    }

    /**
     * This function obviously cannot check for duplicate annotations.
     * So this must have been done before!
     * @param qualifierAnnots
     */
    public void checkQualifierConditions(Set<Annotation> qualifierAnnots)
    {
        Set<Class<? extends Annotation>> usedQualifiers = strictValidation ? new HashSet<>(qualifierAnnots.size()) : null;

        for (Annotation ann : qualifierAnnots)
        {
            if (usedQualifiers != null && usedQualifiers.contains(ann.annotationType()))
            {
                if (ann.annotationType().getAnnotation(Repeatable.class) == null)
                {
                    throw new IllegalArgumentException("Qualifier list must not contain multiple annotations or the same non-Repeatable type: "
                        + ann.annotationType().getName());
                }
            }
            if (usedQualifiers != null)
            {
                usedQualifiers.add(ann.annotationType());
            }

            checkQualifierConditions(ann);
        }
    }

    public void checkQualifierConditions(Annotation ann)
    {
        if (ann == DefaultLiteral.INSTANCE || ann == AnyLiteral.INSTANCE ||
            ann.annotationType().equals(Default.class) || ann.annotationType().equals(Any.class) ||
            ann.annotationType().equals(Named.class))
        {
            // special performance boost for some known Qualifiers
            return;
        }

        AnnotatedType annotatedType = webBeansContext.getBeanManagerImpl().getAdditionalAnnotatedTypeQualifiers().get(ann.annotationType());
        if (annotatedType == null)
        {
            Iterator<AnnotatedType> annotatedTypes = (Iterator)webBeansContext.getBeanManagerImpl().getAnnotatedTypes(ann.annotationType()).iterator();
            if (annotatedTypes.hasNext())
            {
                annotatedType = annotatedTypes.next();
                // TODO what to do here, if we have more than one?
            }
        }
        if (annotatedType != null)
        {
            Set<AnnotatedMethod> methods = annotatedType.getMethods();

            for (AnnotatedMethod method : methods)
            {
                Type baseType = method.getBaseType();
                Class<?> clazz = ClassUtil.getClass(baseType);
                if (clazz.isArray() || clazz.isAnnotation())
                {
                    if (!AnnotationUtil.hasAnnotation(method.getAnnotations(), Nonbinding.class))
                    {
                        throw new WebBeansConfigurationException("WebBeans definition class : " + method.getJavaMember().getDeclaringClass().getName() + " @Qualifier : "
                                                                 + ann.annotationType().getName()
                                                                 + " must have @NonBinding valued members for its array-valued and annotation valued members");
                    }
                }
            }
        }
        else
        {
            Method[] methods = webBeansContext.getSecurityService().doPrivilegedGetDeclaredMethods(ann.annotationType());

            for (Method method : methods)
            {
                Class<?> clazz = method.getReturnType();
                if (clazz.isArray() || clazz.isAnnotation())
                {
                    if (!AnnotationUtil.hasAnnotation(method.getDeclaredAnnotations(), Nonbinding.class))
                    {
                        throw new WebBeansConfigurationException("@Qualifier : " + ann.annotationType().getName()
                                                             + " must have @NonBinding valued members for its array-valued and annotation valued members");
                    }
                }
            }
        }

        if (!isQualifierAnnotation(ann.annotationType()))
        {
            throw new IllegalArgumentException("Qualifier annotations must be annotated with @Qualifier");
        }
    }

    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link javax.enterprise.inject.Stereotype} false otherwise.
     *
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link javax.enterprise.inject.Stereotype} false otherwise
     */
    public boolean isStereoTypeAnnotation(Class<? extends Annotation> clazz)
    {
        return isStereoTypeAnnotation(clazz, new HashSet<>());
    }
    
    private boolean isStereoTypeAnnotation(Class<? extends Annotation> clazz, Set<Class<? extends Annotation>> checkedAnnotations)
    {
        Asserts.nullCheckForClass(clazz);

        Boolean checkedAnnotationResult = checkedStereotypeAnnotations.get(clazz);

        if (checkedAnnotationResult != null)
        {
            return checkedAnnotationResult;
        }

        boolean result = false;

        if (clazz.isAnnotationPresent(Stereotype.class) || webBeansContext.getStereoTypeManager().getStereoTypeModel(clazz.getName()) != null)
        {
            result = true;
        }
        else
        {
            for (Annotation annotation: clazz.getAnnotations())
            {
                if (checkedAnnotations.contains(annotation.annotationType()))
                {
                    continue;
                }
                checkedAnnotations.add(annotation.annotationType());
                if (isStereoTypeAnnotation(annotation.annotationType(), checkedAnnotations))
                {
                    result = true;
                    break;
                }
            }
        }

        checkedStereotypeAnnotations.put(clazz, result);

        return result;
    }

    public boolean hasStereoTypeMetaAnnotation(Set<Class<? extends Annotation>> anns)
    {
        Asserts.assertNotNull(anns, Asserts.PARAM_NAME_ANNOTATION);

        for (Class<? extends Annotation> ann : anns)
        {
            if (isStereoTypeAnnotation(ann))
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasStereoTypeMetaAnnotation(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, Asserts.PARAM_NAME_ANNOTATION);

        for (Annotation ann : anns)
        {
            if (isStereoTypeAnnotation(ann.annotationType()))
            {
                return true;
            }
        }

        return false;
    }

    public Annotation[] getStereotypeMetaAnnotations(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, Asserts.PARAM_NAME_ANNOTATION);
        List<Annotation> interAnns = new ArrayList<>();

        for (Annotation ann : anns)
        {
            if (isStereoTypeAnnotation(ann.annotationType()))
            {
                interAnns.add(ann);

                //check for transitive
                Annotation[] transitives = getTransitiveStereoTypes(ann.annotationType().getDeclaredAnnotations());
                Collections.addAll(interAnns, transitives);
            }
        }

        Annotation[] ret = new Annotation[interAnns.size()];
        ret = interAnns.toArray(ret);

        return ret;
    }

    /**
     * Same like {@link #getStereotypeMetaAnnotations(java.util.Set)} but with an array
     */
    public Set<Class<? extends Annotation>> getStereotypeMetaAnnotations(Set<Class<? extends Annotation>> stereotypes)
    {
        Asserts.assertNotNull(stereotypes, Asserts.PARAM_NAME_ANNOTATION);
        Set<Class<? extends Annotation>> interAnns = new HashSet<>();

        for (Class<? extends Annotation> ann : stereotypes)
        {
            if (isStereoTypeAnnotation(ann))
            {
                interAnns.add(ann);

                //check for transitive
                Annotation[] transitives = getTransitiveStereoTypes(ann.getDeclaredAnnotations());

                for(Annotation transitive : transitives)
                {
                    interAnns.add(transitive.annotationType());
                }
            }
        }
        return interAnns;
    }

    private Annotation[] getTransitiveStereoTypes(Annotation[] anns)
    {
        return getStereotypeMetaAnnotations(anns);
    }

    /**
     * Returns bean stereotypes.
     * @return bean stereotypes
     */
    public Set<Class<? extends Annotation>> getStereotypes(Set<Class<? extends Annotation>> anns)
    {
        Asserts.assertNotNull(anns, Asserts.PARAM_NAME_ANNOTATION);
        if (hasStereoTypeMetaAnnotation(anns))
        {
            return getStereotypeMetaAnnotations(anns);
        }

        return Collections.emptySet();
    }

    /**
     * Returns true if name exists,false otherwise.
     * @return true if name exists
     */
    public boolean hasNamedOnStereoTypes(Set<Class<? extends Annotation>> stereotypes)
    {
        Set<Class<? extends Annotation>> types = getStereotypes(stereotypes);

        for (Class<? extends Annotation> ann : types)
        {
            if (AnnotationUtil.hasClassAnnotation(ann, Named.class))
            {
                return true;
            }
            IStereoTypeModel model = webBeansContext.getStereoTypeManager().getStereoTypeModel(ann.getName());
            if (model != null && model.isNamed())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Validates that given class obeys stereotype model
     * defined by the specification.
     * @param clazz stereotype class
     */
    public void checkStereoTypeClass(Class<? extends Annotation> clazz, Annotation...annotations)
    {
        Asserts.nullCheckForClass(clazz);

        boolean scopeTypeFound = false;
        for (Annotation annotation : annotations)
        {
            Class<? extends Annotation> annotType = annotation.annotationType();

            if (annotType.isAnnotationPresent(NormalScope.class) || annotType.isAnnotationPresent(Scope.class))
            {
                if (scopeTypeFound)
                {
                    throw new WebBeansConfigurationException("@StereoType annotation can not contain more " +
                            "than one @Scope/@NormalScope annotation");
                }
                else
                {
                    scopeTypeFound = true;
                }
            }
            else if (annotType.equals(Named.class))
            {
                Named name = (Named) annotation;
                if (!name.value().equals(""))
                {
                    throw new WebBeansConfigurationException("@StereoType annotation can not define @Named " +
                            "annotation with value");
                }
            }
        }

        checkedStereotypeAnnotations.remove(clazz);
    }

    public void checkInterceptorResolverParams(Annotation... interceptorBindings)
    {
        if (interceptorBindings == null || interceptorBindings.length == 0)
        {
            throw new IllegalArgumentException("Manager.resolveInterceptors() method parameter interceptor bindings " +
                    "array argument can not be empty");
        }

        Set<Class<? extends Annotation>> usedInterceptors = strictValidation ? new HashSet<>(interceptorBindings.length) : null;
        for (Annotation interceptorBinding : interceptorBindings)
        {
            if (!isInterceptorBindingAnnotation(interceptorBinding.annotationType()))
            {
                throw new IllegalArgumentException("Manager.resolveInterceptors() method parameter interceptor" +
                        " bindings array can not contain other annotation that is not @InterceptorBinding");
            }

            if (usedInterceptors != null && usedInterceptors.contains(interceptorBinding.annotationType()))
            {
                if (interceptorBinding.annotationType().getAnnotation(Repeatable.class) == null)
                {
                    throw new IllegalArgumentException("InterceptorBinding list must not contain multiple annotations or the same non-Repeatable type: "
                        + interceptorBinding.annotationType().getName());
                }
            }
            if (usedInterceptors != null)
            {
                usedInterceptors.add(interceptorBinding.annotationType());
            }
        }
    }

    public void checkDecoratorResolverParams(Set<Type> apiTypes, Annotation... qualifiers)
    {
        checkQualifiersParams(apiTypes, qualifiers);
        Annotation old = null;
        for (Annotation qualifier : qualifiers)
        {
            if (old == null)
            {
                old = qualifier;
            }
            else
            {
                if (old.annotationType().equals(qualifier.annotationType()))
                {
                    throw new IllegalArgumentException("Manager.resolveDecorators() method parameter qualifiers " +
                            "array argument can not define duplicate qualifier annotation with name : @" +
                            old.annotationType().getName());
                }

                old = qualifier;
            }
        }

    }

    public void checkQualifiersParams(Set<Type> apiTypes, Annotation... qualifiers)
    {
        if (apiTypes == null || apiTypes.isEmpty())
        {
            throw new IllegalArgumentException("method parameter api types argument can not be empty");
        }

        for (Annotation qualifier : qualifiers)
        {
            if (!isQualifierAnnotation(qualifier.annotationType()))
            {
                throw new IllegalArgumentException("Manager.resolveDecorators() method parameter qualifiers array " +
                        "can not contain other annotation that is not @Qualifier");
            }
        }

    }


    /**
     * Check conditions for the new binding.
     * @param annotations annotations
     * @return Annotation[] with all binding annotations
     * @throws WebBeansConfigurationException if New plus any other binding annotation is set
     */
    public Annotation[] checkForNewQualifierForDeployment(Type type, Class<?> clazz, String name,
                                                                 Annotation[] annotations)
    {
        Asserts.assertNotNull(type, "Type argument");
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(annotations, "Annotations argument");

        Annotation[] as = getQualifierAnnotations(annotations);
        for (Annotation a : annotations)
        {
            if (a.annotationType().equals(New.class))
            {
                if (as.length > 1)
                {
                    throw new WebBeansConfigurationException("@New binding annotation can not have any binding "
                                                             + "annotation in class : " + clazz.getName()
                                                             + " in field/method : " + name);
                }
            }
        }

        return as;
    }

    /**
     * Configures the name of the producer method for specializing the parent.
     *
     * @param component producer method component
     * @param method specialized producer method
     * @param superMethod overriden super producer method
     */
    public boolean isSuperMethodNamed(AbstractOwbBean<?> component,
                                                            Method method,
                                                            Method superMethod)
    {
        Asserts.assertNotNull(component,"component");
        Asserts.assertNotNull(method,"method");
        Asserts.assertNotNull(superMethod,"superMethod");

        boolean hasName = false;
        if(AnnotationUtil.hasMethodAnnotation(superMethod, Named.class))
        {
            hasName = true;
        }
        else
        {
            Annotation[] anns = getStereotypeMetaAnnotations(superMethod.getAnnotations());
            for(Annotation ann : anns)
            {
                if(ann.annotationType().isAnnotationPresent(Stereotype.class))
                {
                    hasName = true;
                    break;
                }
            }
        }

        if(hasName)
        {
            if(AnnotationUtil.hasMethodAnnotation(method, Named.class))
            {
                throw new WebBeansConfigurationException("Specialized method : " + method.getName() + " in class : "
                        + component.getReturnType().getName() + " may not define @Named annotation");
            }
        }

        return hasName;
    }

    @SuppressWarnings("unchecked")
    public <X> Method getDisposalWithGivenAnnotatedMethod(AnnotatedType<X> annotatedType, Type beanType, Annotation[] qualifiers)
    {
        Set<AnnotatedMethod<? super X>> annotatedMethods = webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(annotatedType);

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
                        Type type = AnnotationUtil.getFirstAnnotatedParameter(annotatedMethod, Disposes.class).getBaseType();
                        Annotation[] annots = getAnnotatedMethodFirstParameterQualifierWithGivenAnnotation(annotatedMethod, Disposes.class);

                        if(type.equals(beanType))
                        {
                            for(Annotation qualifier : qualifiers)
                            {
                                if(qualifier.annotationType() != Default.class)
                                {
                                    for(Annotation ann :annots)
                                    {
                                        if(!AnnotationUtil.isCdiAnnotationEqual(qualifier, ann))
                                        {
                                            return null;
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

    /**
     * JavaEE components can not inject {@link javax.enterprise.inject.spi.InjectionPoint}.
     * @param clazz javaee component class info
     * @throws WebBeansConfigurationException exception if condition is not applied
     */
    public void checkInjectionPointForInjectInjectionPoint(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        Field[] fields = webBeansContext.getSecurityService().doPrivilegedGetDeclaredFields(clazz);
        for(Field field : fields)
        {
            if(field.getAnnotation(Inject.class) != null)
            {
                if(field.getType() == InjectionPoint.class)
                {
                    Annotation[] anns = getQualifierAnnotations(field.getDeclaredAnnotations());
                    if (AnnotationUtil.hasAnnotation(anns, Default.class))
                    {
                        throw new WebBeansConfigurationException("Java EE Component class :  " + clazz + " can not inject InjectionPoint");
                    }
                }
            }
        }
    }
    
    public void clearCaches()
    {
        repeatableMethodCache.clear();
    }

    public Optional<Method> getRepeatableMethod(Class<?> type)
    {
        return repeatableMethodCache.computeIfAbsent(type, it -> Optional.ofNullable(resolveRepeatableMethod(it)));
    }
        
    protected Method resolveRepeatableMethod(Class<?> type)
    {
        Method value;
        try
        {
            value = type.getMethod("value");
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
        if (!value.getReturnType().isArray())
        {
            return null;
        }
        Class<?> componentType = value.getReturnType().getComponentType();
        Repeatable repeatable = componentType.getAnnotation(Repeatable.class);
        if (repeatable == null || repeatable.value() != type)
        {
            return null;
        }
        return value;
    }
    
    
}
