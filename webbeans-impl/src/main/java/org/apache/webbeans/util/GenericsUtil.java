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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.webbeans.config.OwbGenericArrayTypeImpl;
import org.apache.webbeans.config.OwbParametrizedTypeImpl;
import org.apache.webbeans.config.OwbTypeVariableImpl;
import org.apache.webbeans.config.OwbWildcardTypeImpl;

/**
 * Utility classes for generic type operations.
 */
public final class GenericsUtil
{
    public static boolean satisfiesDependency(boolean isDelegate, Type injectionPointType, Type beanType)
    {
        if (beanType instanceof TypeVariable || beanType instanceof WildcardType || beanType instanceof GenericArrayType)
        {
            return isAssignableFrom(isDelegate, injectionPointType, beanType);
        }
        else
        {
            Type injectionPointRawType = injectionPointType instanceof ParameterizedType? ((ParameterizedType)injectionPointType).getRawType(): injectionPointType;
            Type beanRawType = beanType instanceof ParameterizedType? ((ParameterizedType)beanType).getRawType(): beanType;
            
            if  (ClassUtil.isSame(injectionPointRawType, beanRawType))
            {
                return isAssignableFrom(isDelegate, injectionPointType, beanType);
            }
        }

        return false;
    }

    /**
     * 5.2.3 and 5.2.4
     */
    public static boolean isAssignableFrom(boolean isDelegate, Type requiredType, Type beanType)
    {
        if (requiredType instanceof Class)
        {
            return isAssignableFrom(isDelegate, (Class<?>)requiredType, beanType);
        }
        else if (requiredType instanceof ParameterizedType)
        {
            return isAssignableFrom(isDelegate, (ParameterizedType)requiredType, beanType);
        }
        else if (requiredType instanceof TypeVariable)
        {
            return isAssignableFrom(isDelegate, (TypeVariable<?>)requiredType, beanType);
        }
        else if (requiredType instanceof GenericArrayType)
        {
            return Class.class.isInstance(beanType) && Class.class.cast(beanType).isArray()
                    && isAssignableFrom(isDelegate, (GenericArrayType)requiredType, beanType);
        }
        else if (requiredType instanceof WildcardType)
        {
            return isAssignableFrom(isDelegate, (WildcardType)requiredType, beanType);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type " + requiredType.getClass());
        }
    }

    private static boolean isAssignableFrom(boolean isDelegate, Class<?> injectionPointType, Type beanType)
    {
        if (beanType instanceof Class)
        {
            return isAssignableFrom(isDelegate, injectionPointType, (Class<?>)beanType);
        }
        else if (beanType instanceof TypeVariable)
        {
            return isAssignableFrom(isDelegate, injectionPointType, (TypeVariable<?>)beanType);
        }
        else if (beanType instanceof ParameterizedType)
        {
            return isAssignableFrom(isDelegate, injectionPointType, (ParameterizedType)beanType);
        }
        else if (beanType instanceof GenericArrayType)
        {
            return isAssignableFrom(isDelegate, injectionPointType, (GenericArrayType)beanType);
        }
        else if (beanType instanceof WildcardType)
        {
            return isAssignableFrom(isDelegate, (Type)injectionPointType, (WildcardType)beanType);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type " + injectionPointType.getClass());
        }
    }

    private static boolean isAssignableFrom(boolean isDelegate, Class<?> injectionPointType, Class<?> beanType)
    {
        return ClassUtil.isClassAssignable(injectionPointType, beanType);
    }

    private static boolean isAssignableFrom(boolean isDelegate, Class<?> injectionPointType, TypeVariable<?> beanType)
    {
        for (Type bounds: beanType.getBounds())
        {
            if (isAssignableFrom(isDelegate, injectionPointType, bounds))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * CDI Spec. 5.2.4: "A parameterized bean type is considered assignable to a raw required type
     * if the raw types are identical and all type parameters of the bean type are either unbounded type variables or java.lang.Object." 
     */
    private static boolean isAssignableFrom(boolean isDelegate, Class<?> injectionPointType, ParameterizedType beanType)
    {
        if (beanType.getRawType() != injectionPointType)
        {
            return false; //raw types don't match
        }
        for (Type typeArgument: beanType.getActualTypeArguments())
        {
            if (typeArgument == Object.class)
            {
                continue;
            }
            if (!(typeArgument instanceof TypeVariable))
            {
                return false; //neither object nor type variable
            }
            TypeVariable<?> typeVariable = (TypeVariable<?>)typeArgument;
            for (Type bounds: typeVariable.getBounds())
            {
                if (bounds != Object.class)
                {
                    return false; //bound type variable
                }
            }
        }
        return true;
    }

    private static boolean isAssignableFrom(boolean isDelegate, Class<?> injectionPointType, GenericArrayType beanType)
    {
        if (!injectionPointType.isArray())
        {
            return false;
        }
        return isAssignableFrom(isDelegate, injectionPointType.getComponentType(), beanType.getGenericComponentType());
    }
    
    private static boolean isAssignableFrom(boolean isDelegate, Type injectionPointType, WildcardType beanType)
    {
        for (Type bounds: beanType.getLowerBounds())
        {
            if (!isAssignableFrom(isDelegate, bounds, injectionPointType))
            {
                return false;
            }
        }
        for (Type bounds: beanType.getUpperBounds())
        {
            if (isAssignableFrom(isDelegate, injectionPointType, bounds))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isAssignableFrom(boolean isDelegate, ParameterizedType injectionPointType, Type beanType)
    {
        if (beanType instanceof Class)
        {
            return isAssignableFrom(isDelegate, injectionPointType, (Class<?>)beanType);
        }
        else if (beanType instanceof TypeVariable)
        {
            return isAssignableFrom(isDelegate, injectionPointType, (TypeVariable<?>)beanType);
        }
        else if (beanType instanceof ParameterizedType)
        {
            return isAssignableFrom(isDelegate, injectionPointType, (ParameterizedType)beanType);
        }
        else if (beanType instanceof WildcardType)
        {
            return isAssignableFrom(isDelegate, (Type)injectionPointType, (WildcardType)beanType);
        }
        else if (beanType instanceof GenericArrayType)
        {
            return false;
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type " + beanType.getClass());
        }
    }

    private static boolean isAssignableFrom(boolean isDelegate, ParameterizedType injectionPointType, Class<?> beanType)
    {
        Class<?> rawInjectionPointType = getRawType(injectionPointType);
        if (rawInjectionPointType.equals(beanType))
        {
            return true;
        }
        if (!rawInjectionPointType.isAssignableFrom(beanType))
        {
            return false;
        }
        if (beanType.getSuperclass() != null && isAssignableFrom(isDelegate, injectionPointType, beanType.getGenericSuperclass()))
        {
            return true;
        }
        for (Type genericInterface: beanType.getGenericInterfaces())
        {
            if (isAssignableFrom(isDelegate, injectionPointType, genericInterface))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isAssignableFrom(boolean isDelegate, ParameterizedType injectionPointType, TypeVariable<?> beanType)
    {
        for (Type bounds: beanType.getBounds())
        {
            if (isAssignableFrom(isDelegate, injectionPointType, bounds))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * CDI Spec. 5.2.4
     */
    private static boolean isAssignableFrom(boolean isDelegate, ParameterizedType injectionPointType, ParameterizedType beanType)
    {
        if (injectionPointType.getRawType() != beanType.getRawType())
        {
            return false;
        }
        Type[] injectionPointTypeArguments = injectionPointType.getActualTypeArguments();
        Type[] beanTypeArguments = beanType.getActualTypeArguments();
        for (int i = 0; i < injectionPointTypeArguments.length; i++)
        {
            Type injectionPointTypeArgument = injectionPointTypeArguments[i];
            Type beanTypeArgument = beanTypeArguments[i];

            // for this special case it's actually an 'assignable to', thus we swap the params, see CDI-389
            // but this special rule does not apply to Delegate injection points...
            if (!isDelegate &&
                injectionPointTypeArgument instanceof Class &&
                beanTypeArgument instanceof TypeVariable)
            {
                for (Type upperBound: ((TypeVariable<?>)beanTypeArgument).getBounds())
                {
                    if (!isAssignableFrom(isDelegate, upperBound, injectionPointTypeArgument))
                    {
                        return false;
                    }
                }

            }
            else if (!isAssignableFrom(isDelegate, injectionPointTypeArgument, beanTypeArgument))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isAssignableFrom(boolean isDelegate, TypeVariable<?> injectionPointType, Type beanType)
    {
        for (Type bounds: injectionPointType.getBounds())
        {
            if (!isAssignableFrom(isDelegate, bounds, beanType))
            {
                return false;
            }
        }
        return true;
    }

    // rules are a bit different when in an array so we handle ParameterizedType manually (not reusing isAssignableFrom)
    private static boolean isAssignableFrom(boolean isDelegate, GenericArrayType injectionPointType, Type beanType)
    {
        final Type genericComponentType = injectionPointType.getGenericComponentType();
        final Class componentType = Class.class.cast(beanType).getComponentType();
        if (Class.class.isInstance(genericComponentType))
        {
            return Class.class.cast(genericComponentType).isAssignableFrom(componentType);
        }
        if (ParameterizedType.class.isInstance(genericComponentType))
        {
            return isAssignableFrom(isDelegate, ParameterizedType.class.cast(genericComponentType).getRawType(), componentType);
        }
        return isAssignableFrom(isDelegate, genericComponentType, componentType);
    }

    private static boolean isAssignableFrom(boolean isDelegate, WildcardType injectionPointType, Type beanType)
    {
        for (Type bounds: injectionPointType.getLowerBounds())
        {
            if (!isAssignableFrom(isDelegate, beanType, bounds))
            {
                return false;
            }
            /*
            Set<Type> beanTypeClosure = getTypeClosure(beanType);
            boolean isAssignable = false;
            for (Type beanSupertype: beanTypeClosure)
            {
                if (isAssignableFrom(isDelegate, beanSupertype, bounds))
                {
                    isAssignable = true;
                    break;
                }
            }
            if (!isAssignable)
            { 
                return false;
            }
            */
        }
        for (Type bounds: injectionPointType.getUpperBounds())
        {
            Set<Type> beanTypeClosure = getTypeClosure(beanType);
            boolean isAssignable = false;
            for (Type beanSupertype: beanTypeClosure)
            {
                if (isAssignableFrom(isDelegate, bounds, beanSupertype)
                    || (Class.class.isInstance(bounds)
                        && ParameterizedType.class.isInstance(beanSupertype)
                        && bounds == ParameterizedType.class.cast(beanSupertype).getRawType()))
                {
                    isAssignable = true;
                    break;
                }
            }
            if (!isAssignable)
            {
                return false;
            }
        }
        return true;
    }
    
    /**
     * @return <tt>true</tt>, if the specified type declaration contains an unresolved type variable.
     */
    public static boolean containsTypeVariable(Type type)
    {
        if (type instanceof Class)
        {
            return false;
        }
        else if (type instanceof TypeVariable)
        {
            return true;
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            return containTypeVariable(parameterizedType.getActualTypeArguments());
        }
        else if (type instanceof WildcardType)
        {
            WildcardType wildcardType = (WildcardType)type;
            return containTypeVariable(wildcardType.getUpperBounds()) || containTypeVariable(wildcardType.getLowerBounds());
        }
        else if (type instanceof GenericArrayType)
        {
            GenericArrayType arrayType = (GenericArrayType)type;
            return containsTypeVariable(arrayType.getGenericComponentType());
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type " + type.getClass().getName());
        }

    }
    
    public static boolean containTypeVariable(Collection<? extends Type> types)
    {
        return containTypeVariable(types.toArray(new Type[types.size()]));
    }
    
    public static boolean containTypeVariable(Type[] types)
    {
        for (Type type: types)
        {
            if (containsTypeVariable(type))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Resolves the actual type of the specified field for the type hierarchy specified by the given subclass
     */
    public static Type resolveType(Class<?> subclass, Field field)
    {
        return resolveType(field.getGenericType(), subclass);
    }

    /**
     * Resolves the actual return type of the specified method for the type hierarchy specified by the given subclass
     */
    public static Type resolveReturnType(Class<?> subclass, Method method)
    {
        return resolveType(method.getGenericReturnType(), subclass);
    }

    /**
     * Resolves the actual parameter types of the specified constructor for the type hierarchy specified by the given subclass
     */
    public static Type[] resolveParameterTypes(Class<?> subclass, Constructor<?> constructor)
    {
        return resolveTypes(constructor.getGenericParameterTypes(), subclass);
    }

    /**
     * Resolves the actual parameter types of the specified method for the type hierarchy specified by the given subclass
     */
    public static Type[] resolveParameterTypes(Class<?> subclass, Method method)
    {
        return resolveTypes(method.getGenericParameterTypes(), subclass);
    }

    /**
     * Resolves the actual type of the specified type for the type hierarchy specified by the given subclass
     */
    public static Type resolveType(Type type, Class<?> subclass, Member member)
    {
        return resolveType(type, subclass);
    }

    public static Type resolveType(Type type, Type actualType)
    {
        if (type instanceof Class)
        {
            return type;
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType)type;

            Type[] resolvedTypeArguments;
            if (Enum.class.equals(parameterizedType.getRawType()))
            {
                // Enums derive from themselves, which would create an infinite loop
                // we directly escape the loop if we detect this.
                resolvedTypeArguments = new Type[]{new OwbWildcardTypeImpl(new Type[]{Enum.class}, ClassUtil.NO_TYPES)};
            }
            else
            {
                resolvedTypeArguments = resolveTypes(parameterizedType.getActualTypeArguments(), actualType);

            }

            return new OwbParametrizedTypeImpl(parameterizedType.getOwnerType(), parameterizedType.getRawType(), resolvedTypeArguments);
        }
        else if (type instanceof TypeVariable)
        {
            TypeVariable<?> variable = (TypeVariable<?>)type;
            return resolveTypeVariable(variable, actualType);
        }
        else if (type instanceof WildcardType)
        {
            WildcardType wildcardType = (WildcardType)type;
            Type[] upperBounds = resolveTypes(wildcardType.getUpperBounds(), actualType);
            Type[] lowerBounds = resolveTypes(wildcardType.getLowerBounds(), actualType);
            return new OwbWildcardTypeImpl(upperBounds, lowerBounds);
        }
        else if (type instanceof GenericArrayType)
        {
            GenericArrayType arrayType = (GenericArrayType)type;
            return createArrayType(resolveType(arrayType.getGenericComponentType(), actualType));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type " + type.getClass().getName());
        }
    }
    
    public static Type[] resolveTypes(Type[] types, Type actualType)
    {
        Type[] resolvedTypeArguments = new Type[types.length];
        for (int i = 0; i < types.length; i++)
        {
            resolvedTypeArguments[i] = resolveType(types[i], actualType);
        }
        return resolvedTypeArguments;
    }

    public static Set<Type> getTypeClosure(Class<?> type)
    {
        return getTypeClosure(type, type);
    }

    public static Set<Type> getTypeClosure(Type actualType)
    {
        return getTypeClosure(actualType, actualType);
    }

    /**
     * Returns the type closure for the specified parameters.
     * <h3>Example 1:</h3>
     * <p>
     * Take the following classes:
     * </p>
     * <code>
     * public class Foo<T> {
     *   private T t;
     * }
     * public class Bar extends Foo<Number> {
     * }
     * </code>
     * <p>
     * To get the type closure of T in the context of Bar (which is {Number.class, Object.class}), you have to call this method like
     * </p>
     * <code>
     * GenericUtil.getTypeClosure(Foo.class.getDeclaredField("t").getType(), Bar.class, Foo.class);
     * </code>
     * <h3>Example 2:</h3>
     * <p>
     * Take the following classes:
     * </p>
     * <code>
     * public class Foo<T> {
     *   private T t;
     * }
     * public class Bar<T> extends Foo<T> {
     * }
     * </code>
     * <p>
     * To get the type closure of Bar<T> in the context of Foo<Number> (which are besides Object.class the <tt>ParameterizedType</tt>s Bar<Number> and Foo<Number>),
     * you have to call this method like
     * </p>
     * <code>
     * GenericUtil.getTypeClosure(Foo.class, new TypeLiteral<Foo<Number>>() {}.getType(), Bar.class);
     * </code>
     * 
     * @param type the type to get the closure for
     * @param actualType the context to bind type variables
     * @return the type closure
     */
    public static Set<Type> getTypeClosure(Type type, Type actualType)
    {
        Class<?> rawType = getRawType(type);
        Class<?> actualRawType = getRawType(actualType);
        if (rawType.isAssignableFrom(actualRawType) && rawType != actualRawType)
        {
            return getTypeClosure(actualType, type);
        }
        if (hasTypeParameters(type))
        {
            type = getParameterizedType(type);
        }
        Set<Type> typeClosure = new HashSet<Type>();
        typeClosure.add(Object.class);
        fillTypeHierarchy(typeClosure, type, actualType);
        return typeClosure;
    }

    private static void fillTypeHierarchy(Set<Type> set, Type type, Type actualType)
    {
        if (type == null)
        {
           return;
        }
        Type resolvedType = GenericsUtil.resolveType(type, actualType);
        set.add(resolvedType);
        Class<?> resolvedClass = GenericsUtil.getRawType(resolvedType, actualType);
        if (resolvedClass.getSuperclass() != null)
        {
            fillTypeHierarchy(set, resolvedClass.getGenericSuperclass(), resolvedType);
        }
        for (Type interfaceType: resolvedClass.getGenericInterfaces())
        {
            fillTypeHierarchy(set, interfaceType, resolvedType);
        }
    }

    public static boolean hasTypeParameters(Type type)
    {
        if (type instanceof Class)
        {
            Class<?> classType = (Class<?>)type;
            return classType.getTypeParameters().length > 0;
        }
        return false;
    }

    public static ParameterizedType getParameterizedType(Type type)
    {
        if (type instanceof ParameterizedType)
        {
            return (ParameterizedType)type;
        }
        else if (type instanceof Class)
        {
            Class<?> classType = (Class<?>)type;
            return new OwbParametrizedTypeImpl(classType.getDeclaringClass(), classType, classType.getTypeParameters());
        }
        else
        {
            throw new IllegalArgumentException(type.getClass().getSimpleName() + " is not supported");
        }
    }

    public static <T> Class<T> getRawType(Type type)
    {
        return getRawType(type, null);
    }

    static <T> Class<T> getRawType(Type type, Type actualType)
    {
        if (type instanceof Class)
        {
            return (Class<T>)type;
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            return getRawType(parameterizedType.getRawType(), actualType);
        }
        else if (type instanceof TypeVariable)
        {
            TypeVariable<?> typeVariable = (TypeVariable<?>)type;
            Type mostSpecificType = getMostSpecificType(getRawTypes(typeVariable.getBounds(), actualType), typeVariable.getBounds());
            return getRawType(mostSpecificType, actualType);
        }
        else if (type instanceof WildcardType)
        {
            WildcardType wildcardType = (WildcardType)type;
            Type mostSpecificType = getMostSpecificType(getRawTypes(wildcardType.getUpperBounds(), actualType), wildcardType.getUpperBounds());
            return getRawType(mostSpecificType, actualType);
        }
        else if (type instanceof GenericArrayType)
        {
            GenericArrayType arrayType = (GenericArrayType)type;
            return getRawType(createArrayType(getRawType(arrayType.getGenericComponentType(), actualType)), actualType);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type " + type.getClass().getName());
        }
    }

    private static Type getRawType(Type[] types, Type actualType, Type declaringType)
    {
        Class<?>[] rawTypes = getRawTypes(types, actualType);
        Class<?>[] classTypes = getClassTypes(rawTypes);
        if (classTypes.length > 0)
        {
            return getMostSpecificType(classTypes, types);
        }
        else
        {
            return getMostSpecificType(rawTypes, types);
        }
    }

    private static <T> Class<T>[] getRawTypes(Type[] types)
    {
        return getRawTypes(types, null);
    }

    private static <T> Class<T>[] getRawTypes(Type[] types, Type actualType)
    {
        Class<T>[] rawTypes = new Class[types.length];
        for (int i = 0; i < types.length; i++)
        {
            rawTypes[i] = getRawType(types[i], actualType);
        }
        return rawTypes;
    }

    private static Type getMostSpecificType(Class<?>[] types, Type[] genericTypes)
    {
        Class<?> mostSpecificType = types[0];
        int mostSpecificIndex = 0;
        for (int i = 0; i < types.length; i++) 
        {
            if (mostSpecificType.isAssignableFrom(types[i]))
            {
                mostSpecificType = types[i];
                mostSpecificIndex = i;
            }
        }
        return genericTypes[mostSpecificIndex];
    }

    private static Class<?>[] getClassTypes(Class<?>[] rawTypes)
    {
        List<Class<?>> classTypes = new ArrayList<Class<?>>();
        for (Class<?> rawType : rawTypes)
        {
            if (!rawType.isInterface())
            {
                classTypes.add(rawType);
            }
        }
        return classTypes.toArray(new Class[classTypes.size()]);
    }

    private static Type resolveTypeVariable(TypeVariable<?> variable, Type actualType)
    {
        if (actualType == null)
        {
            return variable;
        }
        Class<?> declaringClass = getDeclaringClass(variable.getGenericDeclaration());
        Class<?> actualClass = getRawType(actualType);
        if (actualClass == declaringClass)
        {
            return resolveTypeVariable(variable, variable.getGenericDeclaration(), getParameterizedType(actualType));
        }
        else if (actualClass.isAssignableFrom(declaringClass))
        {
            Class<?> directSubclass = getDirectSubclass(declaringClass, actualClass);
            Type[] typeArguments = resolveTypeArguments(directSubclass, actualType);
            Type directSubtype = new OwbParametrizedTypeImpl(directSubclass.getDeclaringClass(), directSubclass, typeArguments);
            return resolveTypeVariable(variable, directSubtype);
        }
        else // if (declaringClass.isAssignableFrom(actualClass))
        { 
            Type genericSuperclass = getGenericSuperclass(actualClass, declaringClass);
            if (genericSuperclass == null)
            {
                return variable;
            }
            else if (genericSuperclass instanceof Class)
            {
                // special handling for type erasure
                Class<?> superclass = (Class<?>)genericSuperclass;
                genericSuperclass = new OwbParametrizedTypeImpl(superclass.getDeclaringClass(), superclass, getRawTypes(superclass.getTypeParameters()));
            }
            else
            {
                ParameterizedType genericSupertype = getParameterizedType(genericSuperclass);
                Type[] typeArguments = resolveTypeArguments(getParameterizedType(actualType), genericSupertype);
                genericSuperclass = new OwbParametrizedTypeImpl(genericSupertype.getOwnerType(), genericSupertype.getRawType(), typeArguments);
            }
            Type resolvedType = resolveTypeVariable(variable, genericSuperclass);
            if (resolvedType instanceof TypeVariable)
            {
                TypeVariable<?> resolvedTypeVariable = (TypeVariable<?>)resolvedType;
                TypeVariable<?>[] typeParameters = actualClass.getTypeParameters();
                for (int i = 0; i < typeParameters.length; i++)
                {
                    if (typeParameters[i].getName().equals(resolvedTypeVariable.getName()))
                    {
                        resolvedType = getParameterizedType(actualType).getActualTypeArguments()[i];
                        break;
                    }
                }
            }
            return resolvedType;
        }
    }

    private static Class<?> getDeclaringClass(GenericDeclaration declaration)
    {
        if (declaration instanceof Class)
        {
            return (Class<?>)declaration;
        }
        else if (declaration instanceof Member)
        {
            return ((Member)declaration).getDeclaringClass();
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type " + declaration.getClass());
        }
    }

    private static Type resolveTypeVariable(TypeVariable<?> variable, GenericDeclaration declaration, ParameterizedType type)
    {
        int index = getIndex(declaration, variable);
        if (declaration instanceof Class)
        {
            if (index >= 0)
            {
                return type.getActualTypeArguments()[index];
            }
            else
            {
                index = getIndex(type, variable);
                if (index >= 0)
                {
                    return declaration.getTypeParameters()[index];
                }
            }
        }
        else
        {
            Type[] resolvedBounds = resolveTypes(declaration.getTypeParameters()[index].getBounds(), type);
            return OwbTypeVariableImpl.createTypeVariable(variable, resolvedBounds);
        }
        return variable;
    }
    
    private static int getIndex(GenericDeclaration declaration, TypeVariable<?> variable)
    {
        Type[] typeParameters = declaration.getTypeParameters();
        for (int i = 0; i < typeParameters.length; i++)
        {
            if (typeParameters[i] instanceof TypeVariable)
            {
                TypeVariable<?> variableArgument = (TypeVariable<?>)typeParameters[i];
                if (variableArgument.getName().equals(variable.getName()))
                {
                    return i;
                }
            }
        }
        return -1;
    }
    
    private static int getIndex(ParameterizedType type, TypeVariable<?> variable)
    {
        Type[] actualTypeArguments = type.getActualTypeArguments();
        for (int i = 0; i < actualTypeArguments.length; i++)
        {
            if (actualTypeArguments[i] instanceof TypeVariable)
            {
                TypeVariable<?> variableArgument = (TypeVariable<?>)actualTypeArguments[i];
                if (variableArgument.getName().equals(variable.getName()))
                {
                    return i;
                }
            }
        }
        return -1;
    }

    private static Class<?> getDirectSubclass(Class<?> declaringClass, Class<?> actualClass)
    {
        if (actualClass.isInterface())
        {
            Class<?> subclass = declaringClass;
            for (Class<?> iface: declaringClass.getInterfaces())
            {
                if (iface == actualClass)
                {
                    return subclass;
                }
                if (actualClass.isAssignableFrom(iface))
                {
                    subclass = iface;
                }
            }
            return getDirectSubclass(subclass, actualClass);
        }
        else
        {
            Class<?> directSubclass = declaringClass;
            while (directSubclass.getSuperclass() != actualClass)
            {
                directSubclass = directSubclass.getSuperclass();
            }
            return directSubclass;
        }
    }

    private static Type getGenericSuperclass(Class<?> subclass, Class<?> superclass)
    {
        if (!superclass.isInterface())
        {
            return subclass.getGenericSuperclass();
        }
        else
        {
            for (Type genericInterface: subclass.getGenericInterfaces())
            {
                if (getRawType(genericInterface) == superclass)
                {
                    return genericInterface;
                }
            }
        }
        return superclass;
    }

    private static Type[] resolveTypeArguments(Class<?> subclass, Type supertype)
    {
        if (supertype instanceof ParameterizedType)
        {
            ParameterizedType parameterizedSupertype = (ParameterizedType)supertype;
            return resolveTypeArguments(subclass, parameterizedSupertype);
        }
        else
        {
            return subclass.getTypeParameters();
        }
    }

    private static Type[] resolveTypeArguments(Class<?> subclass, ParameterizedType parameterizedSupertype)
    {
        Type genericSuperclass = getGenericSuperclass(subclass, getRawType(parameterizedSupertype));
        if (!(genericSuperclass instanceof ParameterizedType))
        {
            return subclass.getTypeParameters();
        }
        ParameterizedType parameterizedSuperclass = (ParameterizedType)genericSuperclass;
        Type[] typeParameters = subclass.getTypeParameters();
        Type[] actualTypeArguments = parameterizedSupertype.getActualTypeArguments();
        return resolveTypeArguments(parameterizedSuperclass, typeParameters, actualTypeArguments);
    }

    private static Type[] resolveTypeArguments(ParameterizedType subtype, ParameterizedType parameterizedSupertype)
    {
        return resolveTypeArguments(getParameterizedType(getRawType(subtype)), parameterizedSupertype.getActualTypeArguments(), subtype.getActualTypeArguments());
    }

    private static Type[] resolveTypeArguments(ParameterizedType parameterizedType, Type[] typeParameters, Type[] actualTypeArguments)
    {
        Type[] resolvedTypeArguments = new Type[typeParameters.length];
        for (int i = 0; i < typeParameters.length; i++)
        {
            resolvedTypeArguments[i] = resolveTypeArgument(parameterizedType, typeParameters[i], actualTypeArguments);
        }
        return resolvedTypeArguments;
    }

    private static Type resolveTypeArgument(ParameterizedType parameterizedType, Type typeParameter, Type[] actualTypeArguments)
    {
        if (typeParameter instanceof TypeVariable)
        {
            TypeVariable<?> variable = (TypeVariable<?>)typeParameter;
            int index = getIndex(parameterizedType, variable);
            if (index == -1)
            {
                return typeParameter;
            }
            else
            {
                return actualTypeArguments[index];
            }
        }
        else if (typeParameter instanceof GenericArrayType)
        {
            GenericArrayType array = (GenericArrayType)typeParameter;
            return createArrayType(resolveTypeArgument(parameterizedType, array.getGenericComponentType(), actualTypeArguments));
        }
        else
        {
            return typeParameter;
        }
    }
    
    private static Type createArrayType(Type componentType)
    {
        if (componentType instanceof Class)
        {
            return Array.newInstance((Class<?>)componentType, 0).getClass();
        }
        else
        {
            return new OwbGenericArrayTypeImpl(componentType);
        }
    }
}
