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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.webbeans.config.OwbParametrizedTypeImpl;

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
            return isAssignableFrom(isDelegate, (GenericArrayType)requiredType, beanType);
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
            throw new IllegalArgumentException("Unsupported type " + injectionPointType.getClass());
        }
    }

    private static boolean isAssignableFrom(boolean isDelegate, ParameterizedType injectionPointType, Class<?> beanType)
    {
        return isAssignableFrom(isDelegate, injectionPointType.getRawType(), beanType);
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
                for (Type upperBound: ((TypeVariable) beanTypeArgument).getBounds())
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

    private static boolean isAssignableFrom(boolean isDelegate, GenericArrayType injectionPointType, Type beanType)
    {
        throw new UnsupportedOperationException("Not yet implementeds");
    }

    private static boolean isAssignableFrom(boolean isDelegate, WildcardType injectionPointType, Type beanType)
    {
        for (Type bounds: injectionPointType.getLowerBounds())
        {
            if (!isAssignableFrom(isDelegate, beanType, bounds))
            {
                return false;
            }
        }
        for (Type bounds: injectionPointType.getUpperBounds())
        {
            if (!isAssignableFrom(isDelegate, bounds, beanType))
            {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Resolves the actual type of the specified field for the type hierarchy specified by the given subclass
     */
    public static Type resolveType(Class<?> subclass, Field field)
    {
        return resolveType(field.getGenericType(), new TypeVariableResolver(subclass, field.getDeclaringClass()));
    }

    /**
     * Resolves the actual return type of the specified method for the type hierarchy specified by the given subclass
     */
    public static Type resolveReturnType(Class<?> subclass, Method method)
    {
        return resolveType(method.getGenericReturnType(), new TypeVariableResolver(subclass, method.getDeclaringClass()));
    }

    /**
     * Resolves the actual parameter types of the specified constructor for the type hierarchy specified by the given subclass
     */
    public static Type[] resolveParameterTypes(Class<?> subclass, Constructor<?> constructor)
    {
        return resolveTypes(constructor.getGenericParameterTypes(), new TypeVariableResolver(subclass, constructor.getDeclaringClass()));
    }

    /**
     * Resolves the actual parameter types of the specified method for the type hierarchy specified by the given subclass
     */
    public static Type[] resolveParameterTypes(Class<?> subclass, Method method)
    {
        return resolveTypes(method.getGenericParameterTypes(), new TypeVariableResolver(subclass, method.getDeclaringClass()));
    }

    /**
     * Resolves the actual type of the specified type for the type hierarchy specified by the given subclass
     */
    public static Type resolveType(Type type, Class<?> subclass, Member member)
    {
        return resolveType(type, new TypeVariableResolver(subclass, member.getDeclaringClass()));
    }

    private static Type resolveType(Type type, TypeVariableResolver resolver)
    {
        if (type instanceof Class)
        {
            return type;
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            Type[] resolvedTypes = resolveTypes(parameterizedType.getActualTypeArguments(), resolver);
            return new OwbParametrizedTypeImpl(parameterizedType.getOwnerType(), parameterizedType.getRawType(), resolvedTypes);
        }
        else if (type instanceof TypeVariable)
        {
            TypeVariable<?> variable = (TypeVariable<?>)type;
            return resolver.resolve(variable);
        }
        else if (type instanceof WildcardType)
        {
            WildcardType wildcardType = (WildcardType) type;
            if (wildcardType.getLowerBounds().length > 0)
            {
                return type;
            }
            Type[] resolvedTypes = resolveTypes(wildcardType.getUpperBounds(), resolver);
            return resolveType(getMostSpecificType(getRawTypes(resolvedTypes, resolver), resolvedTypes), resolver);
        }
        else if (type instanceof GenericArrayType)
        {
            Type componentType = resolveType(((GenericArrayType)type).getGenericComponentType(), resolver);
            Class<?> componentClass = getRawType(componentType, resolver);
            return Array.newInstance(componentClass, 0).getClass();
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type " + type.getClass().getName());
        }
    }
    
    public static Type[] resolveTypes(Type[] types, TypeVariableResolver resolution)
    {
        Type[] resolvedTypeArguments = new Type[types.length];
        for (int i = 0; i < types.length; i++)
        {
            resolvedTypeArguments[i] = resolveType(types[i], resolution);
        }
        return resolvedTypeArguments;
    }

    public static Set<Type> getTypeClosure(Type type, Class<?> owningClass, Class<?> declaringClass)
    {
        Set<Type> typeClosure = new HashSet<Type>();
        typeClosure.add(Object.class);
        fillTypeHierarchy(typeClosure, type, new TypeVariableResolver(owningClass, declaringClass));
        return typeClosure;
    }

    private static void fillTypeHierarchy(Set<Type> set, Type type, TypeVariableResolver resolver)
    {
        if (type == null)
        {
           return;
        }
        Type resolvedType = GenericsUtil.resolveType(type, resolver);
        set.add(resolvedType);
        Class<?> resolvedClass = GenericsUtil.getRawType(resolvedType, resolver);
        if (resolvedClass.getSuperclass() != null)
        {
            fillTypeHierarchy(set, resolvedClass.getGenericSuperclass(), resolver.add(resolvedClass));
        }
        for (Type interfaceType: resolvedClass.getGenericInterfaces())
        {
            fillTypeHierarchy(set, interfaceType, resolver.add(resolvedClass, interfaceType));
        }
    }

    static <T> Class<T> getRawType(Type type, TypeVariableResolver resolver)
    {
        if (type instanceof Class)
        {
            return (Class<T>)type;
        }
        else if (type instanceof ParameterizedType)
        {
            return getRawType(((ParameterizedType) type).getRawType(), resolver);
        }
        else if ((type instanceof TypeVariable) || (type instanceof WildcardType) || (type instanceof GenericArrayType))
        {
            Type resolvedType = resolveType(type, resolver);
            if (resolvedType instanceof TypeVariable)
            {
                TypeVariable<?> variable = (TypeVariable<?>)resolvedType;
                return getRawType(resolveType(getRawType(variable.getBounds(), resolver), resolver), resolver);
            }
            else
            {
                return getRawType(resolvedType, resolver);
            }
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type " + type.getClass().getName());
        }
    }

    private static Type getRawType(Type[] types, TypeVariableResolver resolver)
    {
        Class<?>[] rawTypes = getRawTypes(types, resolver);
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

    private static <T> Class<T>[] getRawTypes(Type[] types, TypeVariableResolver resolver)
    {
        Class<T>[] rawTypes = new Class[types.length];
        for (int i = 0; i < types.length; i++)
        {
            rawTypes[i] = getRawType(types[i], resolver);
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

    /**
     * resolves actual types of a TypeVariable for a specific type hierarchy
     */
    private static class TypeVariableResolver
    {
        private List<TypeVariableDeclaration> declarations = new ArrayList<TypeVariableDeclaration>();

        private TypeVariableResolver(List<TypeVariableDeclaration> implementation)
        {
            this.declarations = implementation;
        }

        public TypeVariableResolver(Class<?> subclass, Class<?> declaringClass)
        {
            declarations.add(new TypeVariableDeclaration(subclass, subclass.getGenericSuperclass()));
            while (declaringClass != subclass && declaringClass.isAssignableFrom(subclass))
            {
                subclass = subclass.getSuperclass();
                declarations.add(new TypeVariableDeclaration(subclass, subclass.getGenericSuperclass()));
            }
        }

        public Type resolve(TypeVariable<?> variable)
        {
            if (declarations.size() < 2)
            {
                return variable;
                //X TODO better handling needed: return getRawType(variable.getBounds(), this);
            }
            int hierarchyIndex = declarations.size() - 1;
            TypeVariableDeclaration typeVariableImplementation = declarations.get(hierarchyIndex);
            TypeVariable<?>[] typeParameters = typeVariableImplementation.getDeclaredTypeParameters();
            int typeIndex = -1;
            for (int i = 0; i < typeParameters.length; i++)
            {
                if (variable.getName().equals(typeParameters[i].getName()))
                {
                    typeIndex = i;
                    break;
                }
            }
            if (typeIndex == -1)
            {
                // type erasure
                return Object.class;
            }
            TypeVariableDeclaration declaration = declarations.get(hierarchyIndex - 1);
            Type genericClass = declaration.getAssignment();
            if (genericClass instanceof ParameterizedType)
            {
                ParameterizedType classType = (ParameterizedType)genericClass;
                return resolveType(classType.getActualTypeArguments()[typeIndex], remove());
            }
            else
            {
                TypeVariable<?>[] typeVariables = declaration.getDeclaredTypeParameters();
                if (typeVariables.length > typeIndex)
                {
                    return resolveType(typeVariables[typeIndex], remove());
                }
                else
                {
                    return Object.class; //type erasure
                }
            }
        }

        public TypeVariableResolver add(Class<?> type)
        {
            return add(type, type.getGenericSuperclass());
        }

        public TypeVariableResolver add(Class<?> declaringClass, Type assignment)
        {
            List<TypeVariableDeclaration> declarations = new ArrayList<TypeVariableDeclaration>(this.declarations);
            declarations.add(new TypeVariableDeclaration(declaringClass, assignment));
            return new TypeVariableResolver(declarations);
        }

        public TypeVariableResolver remove()
        {
            List<TypeVariableDeclaration> declarations = new ArrayList<TypeVariableDeclaration>(this.declarations);
            declarations.remove(declarations.size() - 1);
            return new TypeVariableResolver(declarations);
        }
    }
    
    /**
     * A declaration of type variables along with its assignments
     */
    private static class TypeVariableDeclaration
    {
        private Class<?> declaringClass;
        private Type assignment;
        
        public TypeVariableDeclaration(Class<?> declaringClass, Type assignment)
        {
            this.declaringClass = declaringClass;
            this.assignment = assignment;
        }

        public Type getAssignment()
        {
            return assignment;
        }

        public TypeVariable<?>[] getDeclaredTypeParameters()
        {
            return declaringClass.getTypeParameters();
        }
    }

    private static class TypeErasureException extends Exception
    {
        public TypeErasureException()
        {
            super("generic type information not available");
        }
    }
}
