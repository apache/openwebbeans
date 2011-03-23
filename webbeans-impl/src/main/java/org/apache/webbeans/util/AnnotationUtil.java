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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.Nonbinding;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Utility class related with {@link Annotation} operations.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class AnnotationUtil
{
    public static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    
    // No instantiate
    private AnnotationUtil()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Check given annotation exist on the method.
     * 
     * @param method method
     * @param clazz annotation class
     * @return true or false
     */
    public static boolean hasMethodAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Annotation[] anns = method.getDeclaredAnnotations();
        for (Annotation annotation : anns)
        {
            if (annotation.annotationType().equals(clazz))
            {
                return true;
            }
        }

        return false;

    }


    /**
     * Check given annotation exist in the any parameter of the given method.
     * Return true if exist false otherwise.
     * 
     * @param method method
     * @param clazz checking annotation
     * @return true or false
     */
    public static boolean hasMethodParameterAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.nullCheckForClass(clazz);

        Annotation[][] parameterAnns = method.getParameterAnnotations();

        for (Annotation[] parameters : parameterAnns)
        {
            for (Annotation param : parameters)
            {
                Class<? extends Annotation> btype = param.annotationType();
                if (btype.equals(clazz))
                {
                    return true;
                }
            }

        }
        return false;
    }
    
    public static <X> boolean hasAnnotatedMethodParameterAnnotation(AnnotatedMethod<X> annotatedMethod, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(annotatedMethod, "annotatedMethod argument can not be null");
        Asserts.nullCheckForClass(clazz);

        List<AnnotatedParameter<X>> parameters = annotatedMethod.getParameters();
        for(AnnotatedParameter<X> parameter : parameters)
        {
            if(parameter.isAnnotationPresent(clazz))
            {
                return true;
            }
        }
        
        return false;
    }
    

    public static Type[] getMethodParameterGenericTypesWithGivenAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.nullCheckForClass(clazz);

        List<Type> list = new ArrayList<Type>();
        Type[] result;

        Annotation[][] parameterAnns = method.getParameterAnnotations();
        Type[] genericTypes = method.getGenericParameterTypes();

        int i = 0;
        for (Annotation[] parameters : parameterAnns)
        {
            for (Annotation param : parameters)
            {
                Class<? extends Annotation> btype = param.annotationType();
                if (btype.equals(clazz))
                {
                    list.add(genericTypes[i]);
                    break;
                }
            }

            i++;

        }

        result = new Type[list.size()];
        result = list.toArray(result);

        return result;
    }

    public static Type[] getConstructorParameterGenericTypesWithGivenAnnotation(Constructor<?> constructor, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(constructor, "constructor argument can not be null");
        Asserts.nullCheckForClass(clazz);

        List<Type> list = new ArrayList<Type>();
        Type[] result;

        Annotation[][] parameterAnns = constructor.getParameterAnnotations();
        Type[] genericTypes = constructor.getGenericParameterTypes();

        int i = 0;
        for (Annotation[] parameters : parameterAnns)
        {
            for (Annotation param : parameters)
            {
                Class<? extends Annotation> btype = param.annotationType();
                if (btype.equals(clazz))
                {
                    list.add(genericTypes[i]);
                    break;
                }
            }

            i++;

        }

        result = new Type[list.size()];
        result = list.toArray(result);

        return result;
    }

    /**
     * Check given annotation exist in the multiple parameter of the given
     * method. Return true if exist false otherwise.
     * 
     * @param method method
     * @param clazz checking annotation
     * @return true or false
     */
    public static boolean hasMethodMultipleParameterAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.nullCheckForClass(clazz);

        Annotation[][] parameterAnns = method.getParameterAnnotations();

        boolean found = false;

        for (Annotation[] parameters : parameterAnns)
        {
            for (Annotation param : parameters)
            {

                if (param.annotationType().equals(clazz))
                {
                    if (!found)
                    {
                        found = true;
                    }
                    else
                    {
                        return true;
                    }
                }
            }

        }
        return false;
    }
    
    public static <X> boolean hasAnnotatedMethodMultipleParameterAnnotation(AnnotatedMethod<X> annotatedMethod, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(annotatedMethod, "annotatedMethod argument can not be null");
        Asserts.nullCheckForClass(clazz);

        boolean found = false;
        
        List<AnnotatedParameter<X>> parameters = annotatedMethod.getParameters();
        for(AnnotatedParameter<X> parameter : parameters)
        {
            if(parameter.isAnnotationPresent(clazz))
            {
                if(!found)
                {
                    found = true;
                }
                else
                {
                    return true;   
                }                
            }
        }
        
        
        return false;
    }
    

    /**
     * Gets the method first found parameter type that is annotated with the
     * given annotation.
     * 
     * @param method method
     * @param clazz checking annotation
     * @return type
     */
    public static Type getMethodFirstParameterWithAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.nullCheckForClass(clazz);

        Annotation[][] parameterAnns = method.getParameterAnnotations();
        Type[] params = method.getGenericParameterTypes();

        int index = 0;
        for (Annotation[] parameters : parameterAnns)
        {
            for (Annotation param : parameters)
            {
                Class<? extends Annotation> btype = param.annotationType();
                if (btype.equals(clazz))
                {
                    return params[index];
                }
            }

            index++;

        }
        return null;
    }
    
    public static <X> Type getAnnotatedMethodFirstParameterWithAnnotation(AnnotatedMethod<X> annotatedMethod, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(annotatedMethod, "annotatedMethod argument can not be null");
        Asserts.nullCheckForClass(clazz);

        List<AnnotatedParameter<X>> parameters = annotatedMethod.getParameters();
        for(AnnotatedParameter<X> parameter : parameters)
        {
            if(parameter.isAnnotationPresent(clazz))
            {
                return parameter.getBaseType();
            }
        }
        
        return null;
    }

    @Deprecated
    public static <X> Annotation[] getAnnotatedMethodFirstParameterQualifierWithGivenAnnotation(AnnotatedMethod<X> annotatedMethod, Class<? extends Annotation> clazz)
    {
        return WebBeansContext.getInstance().getAnnotationManager().getAnnotatedMethodFirstParameterQualifierWithGivenAnnotation(annotatedMethod, clazz);
    }
    
    public static Class<?> getMethodFirstParameterTypeClazzWithAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Type type = getMethodFirstParameterWithAnnotation(method, clazz);

        if (type instanceof ParameterizedType)
        {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        else
        {
            return (Class<?>) type;
        }
    }

    /**
     * Gets the method first found parameter qualifiers.
     *
     * @param method method
     * @param clazz checking annotation
     * @return annotation array
     */
    @Deprecated
    public static Annotation[] getMethodFirstParameterQualifierWithGivenAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        return WebBeansContext.getInstance().getAnnotationManager().getMethodFirstParameterQualifierWithGivenAnnotation(method, clazz);
    }

    /**
     * Get the Type of the method parameter which has the given annotation
     * @param method which need to be scanned
     * @param clazz the annotation to scan the method parameters for
     * @return the Type of the method parameter which has the given annotation, or <code>null</code> if not found.
     */
    public static Type getTypeOfParameterWithGivenAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.nullCheckForClass(clazz);

        Annotation[][] parameterAnns = method.getParameterAnnotations();
        Type result = null;

        int index = 0;
        for (Annotation[] parameters : parameterAnns)
        {
            boolean found = false;
            for (Annotation param : parameters)
            {
                Class<? extends Annotation> btype = param.annotationType();
                if (btype.equals(clazz))
                {
                    found = true;
                    //Adding Break instead of continue
                    break;
                }
            }

            if (found)
            {
                result = method.getGenericParameterTypes()[index];
                break;
            }

            index++;

        }
        return result;
    }

    /**
     * Gets the method first found parameter annotation with given type.
     * 
     * @param method method
     * @param clazz checking annotation
     * @return annotation
     */
    public static <T extends Annotation> T getMethodFirstParameterAnnotation(Method method, Class<T> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.nullCheckForClass(clazz);

        Annotation[][] parameterAnns = method.getParameterAnnotations();

        for (Annotation[] parameters : parameterAnns)
        {
            for (Annotation param : parameters)
            {
                Class<? extends Annotation> btype = param.annotationType();
                if (btype.equals(clazz))
                {
                    return clazz.cast(param);
                }

            }

        }

        return null;
    }    
    
    public static <X,T extends Annotation> T getAnnotatedMethodFirstParameterAnnotation(AnnotatedMethod<X> annotatedMethod, Class<T> clazz)
    {
        Asserts.assertNotNull(annotatedMethod, "annotatedMethod argument can not be null");
        Asserts.nullCheckForClass(clazz);
        
        
        List<AnnotatedParameter<X>> parameters = annotatedMethod.getParameters();
        for(AnnotatedParameter<X> parameter : parameters)
        {
            if(parameter.isAnnotationPresent(clazz))
            {
                return clazz.cast(parameter.getAnnotation(clazz));
            }
        }
        
        return null;
    }    
    

    /**
     * Check given annotation cross ref exist in the any parameter of the given
     * method. Return true if exist false otherwise.
     * 
     * @param method method
     * @param clazz checking annotation
     * @return true or false
     */
    public static boolean hasMethodParameterAnnotationCrossRef(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.nullCheckForClass(clazz);

        Annotation[][] parameterAnns = method.getParameterAnnotations();

        for (Annotation[] parameters : parameterAnns)
        {
            for (Annotation param : parameters)
            {
                Annotation[] btype = param.annotationType().getDeclaredAnnotations();

                for (Annotation b : btype)
                {
                    if (b.annotationType().equals(clazz))
                    {
                        return true;
                    }
                }
            }

        }
        return false;
    }

    /**
     * Checks if the given qualifiers are equal.
     *
     * Qualifiers are equal if they have the same annotationType and all their
     * methods, except those annotated with @Nonbinding, return the same value.
     *
     * @param qualifier1
     * @param qualifier2
     * @return
     */
    public static boolean isQualifierEqual(Annotation qualifier1, Annotation qualifier2)
    {
        Asserts.assertNotNull(qualifier1, "qualifier1 argument can not be null");
        Asserts.assertNotNull(qualifier2, "qualifier2 argument can not be null");

        Class<? extends Annotation> qualifier1AnnotationType
                = qualifier1.annotationType();

        // check if the annotationTypes are equal
        if (qualifier1AnnotationType == null
                || !qualifier1AnnotationType.equals(qualifier2.annotationType()))
        {
            return false;
        }

        // check the values of all qualifier-methods
        // except those annotated with @Nonbinding
        List<Method> bindingQualifierMethods
                = getBindingQualifierMethods(qualifier1AnnotationType);

        for (Method method : bindingQualifierMethods)
        {
            Object value1 = callMethod(qualifier1, method);
            Object value2 = callMethod(qualifier2, method);

            if (!checkEquality(value1, value2))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Quecks if the two values are equal.
     *
     * @param value1
     * @param value2
     * @return
     */
    private static boolean checkEquality(Object value1, Object value2)
    {
        if ((value1 == null && value2 != null) ||
            (value1 != null && value2 == null))
        {
            return false;
        }

        if (value1 == null && value2 == null)
        {
            return true;
        }

        // now both values are != null

        Class<?> valueClass = value1.getClass();

        if (!valueClass.equals(value2.getClass()))
        {
            return false;
        }

        if (valueClass.isPrimitive())
        {
            // primitive types can be checked with ==
            return value1 == value2;
        }
        else if (valueClass.isArray())
        {
            Class<?> arrayType = valueClass.getComponentType();

            if (arrayType.isPrimitive())
            {
                if (Long.TYPE == arrayType)
                {
                    return Arrays.equals(((long[]) value1), (long[]) value2);
                }
                else if (Integer.TYPE == arrayType)
                {
                    return Arrays.equals(((int[]) value1), (int[]) value2);
                }
                else if (Short.TYPE == arrayType)
                {
                    return Arrays.equals(((short[]) value1), (short[]) value2);
                }
                else if (Double.TYPE == arrayType)
                {
                    return Arrays.equals(((double[]) value1), (double[]) value2);
                }
                else if (Float.TYPE == arrayType)
                {
                    return Arrays.equals(((float[]) value1), (float[]) value2);
                }
                else if (Boolean.TYPE == arrayType)
                {
                    return Arrays.equals(((boolean[]) value1), (boolean[]) value2);
                }
                else if (Byte.TYPE == arrayType)
                {
                    return Arrays.equals(((byte[]) value1), (byte[]) value2);
                }
                else if (Character.TYPE == arrayType)
                {
                    return Arrays.equals(((char[]) value1), (char[]) value2);
                }
                return false;
            }
            else
            {
                return Arrays.equals(((Object[]) value1), (Object[]) value2);
            }
        }
        else
        {
            return value1.equals(value2);
        }
    }

    /**
     * Calls the given method on the given instance.
     * Used to determine the values of annotation instances.
     *
     * @param instance
     * @param method
     * @return
     */
    private static Object callMethod(Object instance, Method method)
    {
        boolean accessible = method.isAccessible();

        try
        {
            if (!accessible )
            {
                doPrivilegedSetAccessible(method, true);
            }

            return method.invoke(instance, EMPTY_OBJECT_ARRAY);
        }
        catch (Exception e)
        {
            throw new WebBeansException("Exception in method call : " + method.getName(), e);
        }
        finally
        {
            // reset accessible value
            doPrivilegedSetAccessible(method, accessible);
        }
    }

    private static Object doPrivilegedSetAccessible(AccessibleObject obj, boolean flag)
    {
        AccessController.doPrivileged(new PrivilegedActionForAccessibleObject(obj, flag));
        return null;
    }

    private static class PrivilegedActionForAccessibleObject implements PrivilegedAction<Object>
    {

        private AccessibleObject object;

        private boolean flag;

        protected PrivilegedActionForAccessibleObject(AccessibleObject object, boolean flag)
        {
            this.object = object;
            this.flag = flag;
        }

        public Object run()
        {
            object.setAccessible(flag);
            return null;
        }
    }

    /**
     * Return a List of all methods of the qualifier,
     * which are not annotated with @Nonbinding.
     *
     * @param qualifierAnnotationType
     * @return
     */
    private static List<Method> getBindingQualifierMethods(
            Class<? extends Annotation> qualifierAnnotationType)
    {
        Method[] qualifierMethods = SecurityUtil
                .doPrivilegedGetDeclaredMethods(qualifierAnnotationType);

        if (qualifierMethods.length > 0)
        {
            List<Method> bindingMethods = new ArrayList<Method>();

            for (Method qualifierMethod : qualifierMethods)
            {
                Annotation[] qualifierMethodAnnotations
                        = qualifierMethod.getDeclaredAnnotations();

                if (qualifierMethodAnnotations.length > 0)
                {
                    // look for @Nonbinding
                    boolean nonbinding = false;

                    for (Annotation qualifierMethodAnnotation : qualifierMethodAnnotations)
                    {
                        if (Nonbinding.class.equals(
                                qualifierMethodAnnotation.annotationType()))
                        {
                            nonbinding = true;
                            break;
                        }
                    }

                    if (!nonbinding)
                    {
                        // no @Nonbinding found - add to list
                        bindingMethods.add(qualifierMethod);
                    }
                }
                else
                {
                    // no method-annotations - add to list
                    bindingMethods.add(qualifierMethod);
                }
            }

            return bindingMethods;
        }

        // annotation has no methods
        return Collections.emptyList();
    }

    /**
     * Gets the array of qualifier annotations on the given array.
     *
     * @param annotations annotation array
     * @return array containing qualifier anns
     */
    @Deprecated
    public static Annotation[] getQualifierAnnotations(Annotation... annotations)
    {
        return WebBeansContext.getInstance().getAnnotationManager().getQualifierAnnotations(annotations);
    }


    /**
     * Gets array of methods that has parameter with given annotation type.
     * 
     * @param clazz class for check
     * @param annotation for check
     * @return array of methods
     */
    public static Method[] getMethodsWithParameterAnnotation(Class<?> clazz, Class<? extends Annotation> annotation)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(annotation, "Annotation argument can not be null");
        List<Method> list = new ArrayList<Method>();

        do
        {
            Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);

            for (Method m : methods)
            {
                if (hasMethodParameterAnnotation(m, annotation))
                {
                    list.add(m);
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null && clazz != Object.class);

        Method[] rMethod = list.toArray(new Method[list.size()]);

        return rMethod;
    }


    /**
     * Check whether or not class contains the given annotation.
     * 
     * @param clazz class instance
     * @param annotation annotation class
     * @return return true or false
     */
    public static boolean hasClassAnnotation(Class<?> clazz, Class<? extends Annotation> annotation)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(annotation, "Annotation argument can not be null");

        try
        {
            Annotation a = clazz.getAnnotation(annotation);

            if (a != null)
            {
                return true;
            }
        }
        catch (ArrayStoreException e)
        {
            //log this?  It is probably already logged in AnnotatedElementFactory
        }

        return false;
    }

    public static boolean hasMetaAnnotation(Annotation[] anns, Class<? extends Annotation> metaAnnotation)
    {
        Asserts.assertNotNull(anns, "Anns argument can not be null");
        Asserts.assertNotNull(metaAnnotation, "MetaAnnotation argument can not be null");

        for (Annotation annot : anns)
        {
            if (annot.annotationType().isAnnotationPresent(metaAnnotation))
            {
                return true;
            }
        }

        return false;

    }

    public static boolean hasAnnotation(Annotation[] anns, Class<? extends Annotation> annotation)
    {
        return getAnnotation(anns, annotation) != null;
    }

    /**
     * get the annotation of the given type from the array. 
     * @param anns
     * @param annotation
     * @return the Annotation with the given type or <code>null</code> if no such found.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getAnnotation(Annotation[] anns, Class<T> annotation)
    {
        Asserts.assertNotNull(anns, "anns argument can not be null");
        Asserts.assertNotNull(annotation, "annotation argument can not be null");
        for (Annotation annot : anns)
        {
            if (annot.annotationType().equals(annotation))
            {
                return (T)annot;
            }
        }

        return null;
    }

    /**
     * Returns a subset of annotations that are annotated with the specified meta-annotation
     * 
     * @param anns
     * @param metaAnnotation
     * @return
     */
    public static Annotation[] getMetaAnnotations(Annotation[] anns, Class<? extends Annotation> metaAnnotation)
    {
        List<Annotation> annots = new ArrayList<Annotation>();
        Annotation[] result;
        Asserts.assertNotNull(anns, "Anns argument can not be null");
        Asserts.assertNotNull(metaAnnotation, "MetaAnnotation argument can not be null");

        for (Annotation annot : anns)
        {
            if (annot.annotationType().isAnnotationPresent(metaAnnotation))
            {
                annots.add(annot);
            }
        }

        result = new Annotation[annots.size()];
        result = annots.toArray(result);

        return result;
    }


    /**
     * Returns true if any binding exist
     * 
     * @param bean bean
     * @return true if any binding exist
     */
    public static boolean hasAnyQualifier(Bean<?> bean)
    {
        Asserts.assertNotNull(bean, "bean parameter can not be null");
        Set<Annotation> qualifiers = bean.getQualifiers();

        for(Annotation ann : qualifiers)
        {
            if(ann.annotationType().equals(Any.class))
            {
                return true;
            }
        }

        return false;
    }

    
    public static Annotation hasOwbInjectableResource(Annotation[] annotations)
    {
        for (Annotation anno : annotations)
        {
            for(String name : WebBeansConstants.OWB_INJECTABLE_RESOURCE_ANNOTATIONS)
            {
                if(anno.annotationType().getName().equals(name))
                {
                    return anno;
                }
            }
        }        
        
        return null;        
    }

    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link javax.interceptor.InterceptorBinding} false otherwise.
     *
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link javax.interceptor.InterceptorBinding} false otherwise
     */
    @Deprecated
    public static boolean isInterceptorBindingAnnotation(Class<? extends Annotation> clazz)
    {
        return WebBeansContext.getInstance().getAnnotationManager().isInterceptorBindingAnnotation(clazz);
    }

    /**
     * If any Annotations in the input is an interceptor binding annotation type then return
     * true, false otherwise.
     *
     * @param anns array of Annotations to check
     * @return true if one or moe of the input annotations are an interceptor binding annotation
     *         type false otherwise
     */
    @Deprecated
    public static boolean hasInterceptorBindingMetaAnnotation(Annotation[] anns)
    {
        return WebBeansContext.getInstance().getAnnotationManager().hasInterceptorBindingMetaAnnotation(anns);
    }

    /**
     * Collect the interceptor bindings from an array of annotations, including
     * transitively defined interceptor bindings.
     * @param anns An array of annotations
     * @return an array of interceptor binding annotations, including the input and any transitively declared annotations
     */
    @Deprecated
    public static Annotation[] getInterceptorBindingMetaAnnotations(Annotation[] anns)
    {
        return WebBeansContext.getInstance().getAnnotationManager().getInterceptorBindingMetaAnnotations(anns);
    }

    @Deprecated
    public static Annotation[] getStereotypeMetaAnnotations(Annotation[] anns)
    {
        return WebBeansContext.getInstance().getAnnotationManager().getStereotypeMetaAnnotations(anns);
    }

    @Deprecated
    public static boolean hasStereoTypeMetaAnnotation(Annotation[] anns)
    {
        return WebBeansContext.getInstance().getAnnotationManager().hasStereoTypeMetaAnnotation(anns);
    }

    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link javax.enterprise.inject.Stereotype} false otherwise.
     *
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link javax.enterprise.inject.Stereotype} false otherwise
     */
    @Deprecated
    public static boolean isStereoTypeAnnotation(Class<? extends Annotation> clazz)
    {
        return WebBeansContext.getInstance().getAnnotationManager().isStereoTypeAnnotation(clazz);
    }

    /**
     * If the bean extends generic class via Realizes
     * annotation, realized based producer methods, fields and observer
     * methods qualifier is
     *
     * <ul>
     *  <li>Qualifiers on the definitions</li>
     *  <li>Plus class qualifiers</li>
     *  <li>Minus generic class qualifiers</li>
     * </ul>
     *
     * @param clazz realized definition class
     * @param anns binding annotations array
     */
    @Deprecated
    public static Annotation[] getRealizesGenericAnnotations(Class<?> clazz, Annotation[] anns)
    {
        return WebBeansContext.getInstance().getAnnotationManager().getRealizesGenericAnnotations(clazz, anns);
    }

    public static Annotation[] getAnnotationsFromSet(Set<Annotation> set)
    {
        if(set != null)
        {
            Annotation[] anns = new Annotation[set.size()];
            anns = set.toArray(anns);
            
            return anns;
        }
        
        return new Annotation[0];
    }
}
