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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;


/**
 * Utility classes with respect to the class operations.
 *
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class ClassUtil
{
    public static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPERS_MAP;
    public static final Map<Class<?>, Object> DEFAULT_VALUES_MAP;

    static
    {
        Map<Class<?>, Class<?>> primitiveToWrappersMap = new HashMap<>();
        primitiveToWrappersMap.put(Integer.TYPE,Integer.class);
        primitiveToWrappersMap.put(Float.TYPE,Float.class);
        primitiveToWrappersMap.put(Double.TYPE,Double.class);
        primitiveToWrappersMap.put(Character.TYPE,Character.class);
        primitiveToWrappersMap.put(Long.TYPE,Long.class);
        primitiveToWrappersMap.put(Byte.TYPE,Byte.class);
        primitiveToWrappersMap.put(Short.TYPE,Short.class);
        primitiveToWrappersMap.put(Boolean.TYPE,Boolean.class);
        primitiveToWrappersMap.put(Void.TYPE,Void.class);
        PRIMITIVE_TO_WRAPPERS_MAP = Collections.unmodifiableMap(primitiveToWrappersMap);
        Map<Class<?>, Object> defaultValuesMap = new HashMap<>();
        defaultValuesMap.put(Integer.TYPE, 0);
        defaultValuesMap.put(Float.TYPE, 0F);
        defaultValuesMap.put(Double.TYPE, 0D);
        defaultValuesMap.put(Character.TYPE, '\u0000');
        defaultValuesMap.put(Long.TYPE, 0L);
        defaultValuesMap.put(Byte.TYPE, (byte) 0);
        defaultValuesMap.put(Short.TYPE, (short) 0);
        defaultValuesMap.put(Boolean.TYPE, Boolean.FALSE);
        DEFAULT_VALUES_MAP = Collections.unmodifiableMap(defaultValuesMap);
    }

    public static final Type[] NO_TYPES = new Type[0];

    /*
     * Private constructor
     */
    private ClassUtil()
    {
        throw new UnsupportedOperationException();
    }

    public static Class<?> getClassFromName(String name)
    {
        return getClassFromName(name, WebBeansUtil.getCurrentClassLoader(), true);
    }

    public static Class<?> getClassFromName(String name, ClassLoader providedLoader, boolean init)
    {
        Class<?> clazz;
        ClassLoader loader = providedLoader;
        try
        {
            clazz = Class.forName(name, init , loader);
            return clazz;

        }
        catch (ClassNotFoundException e)
        {
            try
            {
                loader = ClassUtil.class.getClassLoader(); 
                clazz = Class.forName(name, true , loader);

                return clazz;

            }
            catch (ClassNotFoundException e1)
            {
                try
                {
                    loader = ClassLoader.getSystemClassLoader();
                    clazz = Class.forName(name, true , loader);

                    return clazz;

                }
                catch (ClassNotFoundException e2)
                {
                    return null;
                }
            }
        }
    }

    /**
     * Check the class is inner or not
     * 
     * @param clazz to check
     * @return true or false
     */
    public static boolean isInnerClazz(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        return clazz.isMemberClass();
    }

    public static boolean isSame(Type type1, Type type2)
    {
        if ((type1 instanceof Class) && ((Class<?>)type1).isPrimitive())
        {
            type1 = PRIMITIVE_TO_WRAPPERS_MAP.get(type1);
        }
        if ((type2 instanceof Class) && ((Class<?>)type2).isPrimitive())
        {
            type2 = PRIMITIVE_TO_WRAPPERS_MAP.get(type2);
        }
        return type1 == type2;
    }

    public static Class<?> getPrimitiveWrapper(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        
        return PRIMITIVE_TO_WRAPPERS_MAP.get(clazz);

    }

    public static Object getDefaultValue(Class<?> type)
    {
        return DEFAULT_VALUES_MAP.get(type);
    }

    /**
     * Gets the class of the given type arguments.
     * <p>
     * If the given type {@link Type} parameters is an instance of the
     * {@link ParameterizedType}, it returns the raw type otherwise it return
     * the casted {@link Class} of the type argument.
     * </p>
     * 
     * @param type class or parametrized type
     * @return
     */
    public static Class<?> getClass(Type type)
    {
        return getClazz(type);
    }


    /**
     * Check method throws checked exception or not.
     * 
     * @param method method instance
     */
    public static boolean isMethodHasCheckedException(Method method)
    {
        Asserts.nullCheckForMethod(method);

        Class<?>[] et = method.getExceptionTypes();

        if (et.length > 0)
        {
            for (Class<?> type : et)
            {
                if (!Error.class.isAssignableFrom(type) && !RuntimeException.class.isAssignableFrom(type))
                {
                    return true;
                }
            }

        }

        return false;
    }

    /**
     * Call method on the instance with given arguments.
     * 
     * @param method method instance
     * @param instance object instance
     * @param args arguments
     * @return the method result
     */
    public static Object callInstanceMethod(Method method, Object instance, Object[] args)
    {
        Asserts.nullCheckForMethod(method);

        try
        {
            if (args == null)
            {
                args = new Object[] {};
            }
            return method.invoke(instance, args);

        }
        catch (Exception e)
        {
            throw new WebBeansException("Exception occurs in the method call with method : " + method.getName() + " in class : " + instance.getClass().getName(), e);
        }

    }

    private static Set<String> getObjectMethodNames()
    {
        if (objectMethodNames == null)
        {
            // not much synchronisation needed...
            Set<String> list = new HashSet<>();
            Class<?> clazz = Object.class;

            Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);
            for (Method method : methods)
            {
                list.add(method.getName());
            }
            objectMethodNames = list;
        }

        return objectMethodNames;
    }
    private static volatile Set<String> objectMethodNames;

    /**
     * collect all non-private, non-static and non-abstract methods from the given class.
     * This method removes any overloaded methods from the list automatically.
     * We also do skip bridge methods as they exist for and are handled solely
     * by the JVM itself.
     *
     * The returned Map contains the methods divided by the methodName as key in the map
     * following all the methods with the same methodName in a List.
     *
     * There is some special rule for package-private methods. Any non-visible
     * package-private method will get skipped and treated similarly to private methods.
     *
     * Note: we filter out the {@link Object#finalize()} method as users must not deal with it.
     * @param topClass the class to start with. Then move up the hierarchy
     * @param excludeFinalMethods whether final classes should get excluded from the result
     */
    public static List<Method> getNonPrivateMethods(Class<?> topClass, boolean excludeFinalMethods)
    {
        Map<String, List<Method>> methodMap = new HashMap<>();
        List<Method> allMethods = new ArrayList<>(10);

        Class<?> clazz = topClass;

        if (!clazz.isAnnotation() && clazz.isInterface())
        {
            addNonPrivateMethods(topClass, excludeFinalMethods, methodMap, allMethods, clazz);
            for (Class<?> parent : clazz.getInterfaces())
            {
                addNonPrivateMethods(topClass, excludeFinalMethods, methodMap, allMethods, parent);
            }
        }
        else
        {
            while (clazz != null)
            {
                addNonPrivateMethods(topClass, excludeFinalMethods, methodMap, allMethods, clazz);
                clazz = clazz.getSuperclass();
            }
        }

        return allMethods;
    }

    private static void addNonPrivateMethods(Class<?> topClass, boolean excludeFinalMethods,
                                             Map<String, List<Method>> methodMap, List<Method> allMethods,
                                             Class<?> clazz)
    {
        List<Method> temp = new ArrayList<>(Arrays.asList(clazz.getMethods()));
        for (Method method : clazz.getDeclaredMethods())
        {
            if (!temp.contains(method))
            {
                temp.add(method);
            }
        }

        for (Method method : temp)
        {
            if (allMethods.contains(method))
            {
                continue;
            }

            if (method.isBridge())
            {
                // we have no interest in generics bridge methods
                continue;
            }

            int modifiers = method.getModifiers();

            if (Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers))
            {
                continue;
            }
            if (excludeFinalMethods && Modifier.isFinal(modifiers))
            {
                continue;
            }

            if ("finalize".equals(method.getName()))
            {
                // we do not proxy finalize()
                continue;
            }

            // check for package-private methods from a different package
            if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers))
            {
                // private already got handled above, so we only had to check for not public nor protected
                // we cannot see those methods if they are not in the same package as the rootClazz
                if (!clazz.getPackage().getName().equals(topClass.getPackage().getName()))
                {
                    continue;
                }

            }

            List<Method> methods = methodMap.get(method.getName());
            if (methods == null)
            {
                methods = new ArrayList<>();
                methods.add(method);
                allMethods.add(method);
                methodMap.put(method.getName(), methods);
            }
            else
            {
                if (!isOverridden(methods, method))
                {
                    // method is not overridden, so add it
                    methods.add(method);
                    allMethods.add(method);
                }
            }
        }
    }

    /**
     * collect all abstract methods for the given class if the given class
     * is {@link Modifier#ABSTRACT}
     *
     * @param clazz {@link Class} to check
     *
     * @return {@link List} with all abstract methods of the given class or an empty list
     *         if the given class is not abstract or doesn't contain an abstract method
     */
    public static List<Method> getAbstractMethods(Class<?> clazz)
    {
        if (!Modifier.isAbstract(clazz.getModifiers()))
        {
            return Collections.emptyList();
        }

        List<Method> methods = getNonPrivateMethods(clazz, true);
        if (!methods.isEmpty())
        {
            methods.removeIf(method -> !Modifier.isAbstract(method.getModifiers()));
        }

        return methods;
    }

    /**
     * Checks, if the given {@link Class} declares the {@link Method} with the
     * given name und parameterTypes.
     *
     * @param clazz to check
     * @param name of the method
     * @param parameterTypes of the method
     *
     * @return {@code} true if the given class contains a method with the given name and parameterTypes,
     *         otherwise {@code false}
     */
    public static boolean isMethodDeclared(Class<?> clazz, String name, Class<?>... parameterTypes)
    {
        try
        {
            return clazz.getMethod(name, parameterTypes) != null;
        }
        catch (NoSuchMethodException e)
        {
            return false;
        }
    }

    /**
     * Checks, if the given {@link Class} implements the {@link Method} with the
     * given name und parameterTypes.
     * Returns {@code false} if the method is only a default method in the interface!
     *
     * @param clazz to check
     * @param interfase the Interface which declares the method
     * @param methodName of the method
     * @param parameterTypes of the method
     *
     * @return {@code} true if the given class contains a method with the given name and parameterTypes,
     *         otherwise {@code false}
     */
    public static boolean isMethodImplemented(Class<?> clazz, Class<?> interfase, String methodName, Class<?>... parameterTypes)
    {
        try
        {
            Method m = clazz.getMethod(methodName, parameterTypes);
            return m != null && m.getDeclaringClass() != interfase;
        }
        catch (NoSuchMethodException e)
        {
            return false;
        }
    }

    /**
     * Check if the method is already defined in a subclass
     * @param subclassMethods
     * @param superclassMethod
     */
    public static boolean isOverridden(List<Method> subclassMethods, Method superclassMethod)
    {
        for (Method m : subclassMethods)
        {
            if (isOverridden(m, superclassMethod))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * Checks if the given method if from Object.class
     * @param methodName
     * @return <code>true</code> if the given method is from Object.class (either directly or overloaded)
     */
    public static boolean isObjectMethod(String methodName)
    {
        return getObjectMethodNames().contains(methodName);
    }

    /**
     * Returns true if type is an instance of <code>ParameterizedType</code>
     * else otherwise.
     * 
     * @param type type of the artifact
     * @return true if type is an instance of <code>ParameterizedType</code>
     */
    public static boolean isParametrizedType(Type type)
    {
        return type instanceof ParameterizedType;
    }
    
    /**
     * Returns true if type is an instance of <code>WildcardType</code>
     * else otherwise.
     * 
     * @param type type of the artifact
     * @return true if type is an instance of <code>WildcardType</code>
     */    
    public static boolean isWildCardType(Type type)
    {
        Asserts.assertNotNull(type, "type");
        
        return type instanceof WildcardType;
    }
    
    public static boolean isUnboundedTypeVariable(Type type)
    {
        Asserts.assertNotNull(type, "type");
        
        if (type instanceof TypeVariable)
        {
            TypeVariable wc = (TypeVariable)type;
            Type[] upper = wc.getBounds();            
            
            
            if(upper.length > 1)
            {
                return false;
            }            
            else
            {
                Type arg = upper[0];
                if(!(arg instanceof Class))
                {
                    return false;
                }
                else
                {
                    Class<?> clazz = (Class<?>)arg;
                    if(!clazz.equals(Object.class))
                    {
                        return false;
                    }                    
                }
            }            
        }
        else
        {
            return false;
        }

        return true;
    }
    
    
    /**
     * Returns true if type is an instance of <code>TypeVariable</code>
     * else otherwise.
     * 
     * @param type type of the artifact
     * @return true if type is an instance of <code>TypeVariable</code>
     */    
    public static boolean isTypeVariable(Type type)
    {
        Asserts.assertNotNull(type, "type");

        return type instanceof TypeVariable;
    }
    

    /**
     * Return true if the class is not abstract and interface.
     *     
     * @param clazz class type
     * @return true if the class is not abstract and interface
     */
    public static boolean isConcrete(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        Integer modifier = clazz.getModifiers();

        return !Modifier.isAbstract(modifier) && !Modifier.isInterface(modifier);
    }
    
    
    /**
     * Returns true if rhs is assignable type
     * to the lhs, false otherwise.
     * 
     * @param lhs left hand side class
     * @param rhs right hand side class
     * @return true if rhs is assignable to lhs
     */
    public static boolean isClassAssignableFrom(Class<?> lhs, Class<?> rhs)
    {
        Asserts.assertNotNull(lhs, "lhs");
        Asserts.assertNotNull(rhs, "rhs");

        if(lhs.isPrimitive())
        {
            lhs = getPrimitiveWrapper(lhs);
        }
        
        if(rhs.isPrimitive())
        {
            rhs = getPrimitiveWrapper(rhs);
        }

        if (lhs.isAssignableFrom(rhs))
        {
            return true;
        }

        return false;
    }

    /**
     * Check bean type and required type.
     * <p>
     * Required type is a wildcard type.
     * </p>
     * @param beanTypeArg bean type
     * @param requiredTypeArg required type
     * @return true if condition satisfies
     * @since 1.1.1
     */
    public static boolean checkRequiredTypeIsWildCard(Type beanTypeArg, Type requiredTypeArg)
    {
        WildcardType wctRequiredTypeArg = (WildcardType)requiredTypeArg;
        Type upperBoundRequiredTypeArg =  wctRequiredTypeArg.getUpperBounds()[0];
        Type[] lowerBoundRequiredTypeArgs =  wctRequiredTypeArg.getLowerBounds();
        
        if(beanTypeArg instanceof Class)
        {
            Class<?> clazzBeanTypeArg = (Class<?>)beanTypeArg;
            if(upperBoundRequiredTypeArg instanceof Class)
            {
                //Check upper bounds
                Class<?> clazzUpperBoundTypeArg = (Class<?>)upperBoundRequiredTypeArg;
                if(clazzUpperBoundTypeArg != Object.class)
                {
                    if(!clazzUpperBoundTypeArg.isAssignableFrom(clazzBeanTypeArg))
                    {
                        return false;
                    }
                }

                //Check lower bounds
                if(lowerBoundRequiredTypeArgs.length > 0 &&  lowerBoundRequiredTypeArgs[0] instanceof Class)
                {
                    Class<?> clazzLowerBoundTypeArg = (Class<?>)lowerBoundRequiredTypeArgs[0];

                    if(clazzLowerBoundTypeArg != Object.class)
                    {
                        if(!clazzBeanTypeArg.isAssignableFrom(clazzLowerBoundTypeArg))
                        {
                            return false;
                        }
                    }
                }
            }
        }
        else if(isTypeVariable(beanTypeArg))
        {
            TypeVariable<?> tvBeanTypeArg = (TypeVariable<?>)beanTypeArg;
            Type tvBound = tvBeanTypeArg.getBounds()[0];
            
            if(tvBound instanceof Class)
            {
                Class<?> clazzTvBound = (Class<?>)tvBound;
                
                if(upperBoundRequiredTypeArg instanceof Class)
                {
                    Class<?> clazzUpperBoundTypeArg = (Class<?>)upperBoundRequiredTypeArg;                    
                    if(clazzUpperBoundTypeArg != Object.class && clazzTvBound != Object.class)
                    {
                        if(!clazzUpperBoundTypeArg.isAssignableFrom(clazzTvBound))
                        {   
                            return false;
                        }                       
                    }
                    
                    //Check lower bounds
                    if(lowerBoundRequiredTypeArgs.length > 0 &&  lowerBoundRequiredTypeArgs[0] instanceof Class)
                    {
                        Class<?> clazzLowerBoundTypeArg = (Class<?>)lowerBoundRequiredTypeArgs[0];
                        
                        if(clazzLowerBoundTypeArg != Object.class)
                        {
                            if(!clazzTvBound.isAssignableFrom(clazzLowerBoundTypeArg))
                            {
                                return false;
                            }                                    
                        }
                    }                    
                }                                    
            }
        }
        else if (beanTypeArg instanceof ParameterizedType)
        {
            final ParameterizedType pt = (ParameterizedType) beanTypeArg;
            if(pt.getRawType() instanceof Class && upperBoundRequiredTypeArg instanceof Class)
            {
                final Class<?> beanRawClass = (Class) pt.getRawType();

                //Check upper bounds
                Class<?> clazzUpperBoundTypeArg = (Class<?>)upperBoundRequiredTypeArg;
                if(clazzUpperBoundTypeArg != Object.class)
                {
                    if(!clazzUpperBoundTypeArg.isAssignableFrom(beanRawClass))
                    {
                        return false;
                    }
                }

                //Check lower bounds
                if(lowerBoundRequiredTypeArgs.length > 0 &&  lowerBoundRequiredTypeArgs[0] instanceof Class)
                {
                    Class<?> clazzLowerBoundTypeArg = (Class<?>)lowerBoundRequiredTypeArgs[0];

                    if(clazzLowerBoundTypeArg != Object.class && !beanRawClass.isAssignableFrom(clazzLowerBoundTypeArg))
                    {
                        return false;
                    }
                }
            }
        }
         
        return true;
    }

    /**
     * Returns declared type arguments if {@code type} is a
     * {@link ParameterizedType} instance, else an empty array.
     * Get the actual type arguments of a type.
     * @param type
     * @return array of type arguments available
     * @since 1.1.1
     */
    public static Type[] getActualTypeArguments(Type type)
    {
        Asserts.assertNotNull(type, "type");

        if (type instanceof ParameterizedType)
        {
            return ((ParameterizedType) type).getActualTypeArguments();

        }
        else
        {
            return NO_TYPES;
        }
    }

    /**
     * Return raw class type for given type.
     * @param type base type instance
     * @return class type for given type
     */
    public static Class<?> getClazz(Type type)
    {
        if(type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType)type;
            return (Class<?>)pt.getRawType();                
        }
        else if(type instanceof Class)
        {
            return (Class<?>)type;
        }
        else if(type instanceof GenericArrayType)
        {
            GenericArrayType arrayType = (GenericArrayType)type;
            return Array.newInstance(getClazz(arrayType.getGenericComponentType()), 0).getClass();
        }
        else if (type instanceof WildcardType)
        {
            WildcardType wildcardType = (WildcardType)type;
            Type[] bounds = wildcardType.getUpperBounds();
            if (bounds.length > 1)
            {
                throw new WebBeansConfigurationException("Illegal use of wild card type with more than one upper bound: " + wildcardType);
            }
            else if (bounds.length == 0)
            {
                return Object.class;
            }
            else
            {
                return getClass(bounds[0]);
            }
        }
        else if (type instanceof TypeVariable)
        {
            TypeVariable<?> typeVariable = (TypeVariable<?>)type;
            if (typeVariable.getBounds().length > 1)
            {
                throw new WebBeansConfigurationException("Illegal use of type variable with more than one bound: " + typeVariable);
            }
            else
            {
                Type[] bounds = typeVariable.getBounds();
                if (bounds.length == 0)
                {
                    return Object.class;
                }
                else
                {
                    return getClass(bounds[0]);
                }
            }
        }
        else
        {
            throw new WebBeansConfigurationException("Unsupported type " + type);
        }
    }

    /**
     * Return true if it does not contain type variable for wildcard type
     * false otherwise.
     * 
     * @param pType parameterized type
     * @return true if it does not contain type variable for wildcard type
     */
    public static boolean checkParametrizedType(ParameterizedType pType)
    {
        Asserts.assertNotNull(pType, "pType");
        
        Type[] types = pType.getActualTypeArguments();

        for (Type type : types)
        {
            if (type instanceof ParameterizedType)
            {
                return checkParametrizedType((ParameterizedType) type);
            }
            else if ((type instanceof TypeVariable) || (type instanceof WildcardType))
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Returns injection point raw type.
     * 
     * @param injectionPoint injection point definition
     * @return injection point raw type
     */
    public static Class<?> getRawTypeForInjectionPoint(InjectionPoint injectionPoint)
    {
        Class<?> rawType = null;
        Type type = injectionPoint.getType();
        
        if(type instanceof Class)
        {
            rawType = (Class<?>) type;
        }
        else if(type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType)type;            
            rawType = (Class<?>)pt.getRawType();                                                
        }
        
        return rawType;
    }

    /**
     * Check whether <code>superClassMethod</code> is overridden by <code>subClassMethod</code>.
     * @param subClassMethod potentially overriding
     * @param superClassMethod potentially overridden
     * @return true if overridden
     * @since 1.1.1
     */
    public static boolean isOverridden(Method subClassMethod, Method superClassMethod)
    {
        if (isSuperClass(superClassMethod.getDeclaringClass(), subClassMethod.getDeclaringClass())
                && subClassMethod.getName().equals(superClassMethod.getName())
                && Arrays.equals(subClassMethod.getParameterTypes(), superClassMethod.getParameterTypes()))
        {
            int modifiers = superClassMethod.getModifiers();
            if(Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers))
            {
                return false;
            }
            
            if(!Modifier.isProtected(modifiers) && !Modifier.isPublic(modifiers))
            {
                Class<?> superClass = superClassMethod.getDeclaringClass();
                Class<?> subClass = subClassMethod.getDeclaringClass();
                
                //Same package
                if(!subClass.getPackage().getName().equals(superClass.getPackage().getName()))
                {
                    return false;
                }
            }
            
            return true;
        }
        
        return false;
    }

    private static boolean isSuperClass(Class<?> superClass, Class<?> subClass)
    {
        return superClass.isAssignableFrom(subClass) && !superClass.equals(subClass);
    }

    public static boolean isRawClassEquals(Type ipType, Type apiType)
    {
        Class ipClass = getRawPrimitiveType(ipType);
        Class apiClass  = getRawPrimitiveType(apiType);

        if (ipClass == null || apiClass == null)
        {
            // we found some illegal types
            return false;
        }

        return ipClass.equals(apiClass);
    }

    private static Class getRawPrimitiveType(Type type)
    {
        if (type instanceof Class)
        {
            if (((Class) type).isPrimitive())
            {
                return getPrimitiveWrapper((Class) type);
            }
            return (Class) type;
        }

        if (type instanceof ParameterizedType)
        {
            return getRawPrimitiveType(((ParameterizedType) type).getRawType());
        }

        return null;
    }
}
