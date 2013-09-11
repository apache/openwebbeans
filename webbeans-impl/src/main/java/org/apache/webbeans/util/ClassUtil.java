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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.exception.inject.DefinitionException;

/**
 * Utility classes with respect to the class operations.
 *
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class ClassUtil
{
    public static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPERS_MAP = new HashMap<Class<?>, Class<?>>();

    static
    {
        PRIMITIVE_TO_WRAPPERS_MAP.put(Integer.TYPE,Integer.class);
        PRIMITIVE_TO_WRAPPERS_MAP.put(Float.TYPE,Float.class);
        PRIMITIVE_TO_WRAPPERS_MAP.put(Double.TYPE,Double.class);
        PRIMITIVE_TO_WRAPPERS_MAP.put(Character.TYPE,Character.class);
        PRIMITIVE_TO_WRAPPERS_MAP.put(Long.TYPE,Long.class);
        PRIMITIVE_TO_WRAPPERS_MAP.put(Byte.TYPE,Byte.class);
        PRIMITIVE_TO_WRAPPERS_MAP.put(Short.TYPE,Short.class);
        PRIMITIVE_TO_WRAPPERS_MAP.put(Boolean.TYPE,Boolean.class);
        PRIMITIVE_TO_WRAPPERS_MAP.put(Void.TYPE,Void.class);
    }

    /*
     * Private constructor
     */
    private ClassUtil()
    {
        throw new UnsupportedOperationException();
    }

    public static Class<?> getClassFromName(String name)
    {
        Class<?> clazz;
        ClassLoader loader;
        try
        {
            loader = WebBeansUtil.getCurrentClassLoader();
            clazz = Class.forName(name, true , loader);
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
        Asserts.assertNotNull(instance, "instance parameter can not be null");

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
            Set<String> list = new HashSet<String>();
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
    private static volatile Set<String> objectMethodNames= null;

    /**
     * collect all non-private, non-static and non-abstract methods from the given class.
     * This method removes any overloaded methods from the list automatically.
     *
     * The returned Map contains the methods divided by the methodName as key in the map
     * following all the methods with the same methodName in a List.
     *
     * There is some special rule for package-private methods. Any non-visible
     * package-private method will get skipped and treated similarly to private methods.
     *
     * Note: we filter out the {@link Object#finalize()} method as users must not deal with it
     */
    public static List<Method> getNonPrivateMethods(Class<?> rootClazz, boolean noFinalMethods)
    {
        Map<String, List<Method>> methodMap = new HashMap<String, List<Method>>();
        List<Method> allMethods = new ArrayList<Method>(10);

        Class<?> clazz = rootClazz;

        while (clazz != null)
        {
            for (Method method : clazz.getDeclaredMethods())
            {
                final int modifiers = method.getModifiers();

                if (Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers))
                {
                    continue;
                }
                if (noFinalMethods && Modifier.isFinal(modifiers))
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
                    if (!clazz.getPackage().getName().equals(rootClazz.getPackage().getName()))
                    {
                        continue;
                    }

                }

                List<Method> methods = methodMap.get(method.getName());
                if (methods == null)
                {
                    methods = new ArrayList<Method>();
                    methods.add(method);
                    allMethods.add(method);
                    methodMap.put(method.getName(), methods);
                }
                else
                {
                    if (isOverridden(methods, method))
                    {
                        // method is overridden in superclass, so do nothing
                    }
                    else
                    {
                        // method is not overridden, so add it
                        methods.add(method);
                        allMethods.add(method);
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }

        return allMethods;
    }

    /**
     * Check if the method is already defined in a subclass
     * @param subclassMethods
     * @param superclassMethod
     */
    public static boolean isOverridden(final List<Method> subclassMethods, final Method superclassMethod)
    {
        for (final Method m : subclassMethods)
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
        Asserts.assertNotNull(type, "type parameter can not be null");

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
        Asserts.assertNotNull(type, "type parameter can not be null");
        
        return type instanceof WildcardType;
    }
    
    public static boolean isUnboundedTypeVariable(Type type)
    {
        Asserts.assertNotNull(type, "type parameter can not be null");
        
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
        Asserts.assertNotNull(type, "type parameter can not be null");

        return type instanceof TypeVariable;
    }
    

    /**
     * Returna true if the class is not abstract and interface.
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
     * Checks that event is applicable
     * for the given observer type.
     * @param eventType event type
     * @param observerType observer type
     * @return true if event is applicable
     */
    public static boolean checkEventTypeAssignability(Type eventType, Type observerType)
    {
        //Observer type is a TypeVariable
        if(isTypeVariable(observerType))
        {
            Class<?> eventClass = getClass(eventType);
                        
            TypeVariable<?> tvBeanTypeArg = (TypeVariable<?>)observerType;
            Type tvBound = tvBeanTypeArg.getBounds()[0];
            
            if(tvBound instanceof Class)
            {
                Class<?> clazzTvBound = (Class<?>)tvBound;                
                if(clazzTvBound.isAssignableFrom(eventClass))
                {
                    return true;
                }                    
            }
        }
        //Both of them are ParametrizedType
        else if(observerType instanceof ParameterizedType && eventType instanceof ParameterizedType)
        {
            return isAssignableForParametrized((ParameterizedType)eventType, (ParameterizedType)observerType);
        }
        //Observer is class and Event type is Parametrized
        else if(observerType instanceof Class && eventType instanceof ParameterizedType)
        {
            Class<?> clazzBeanType = (Class<?>)observerType;
            ParameterizedType ptEvent = (ParameterizedType)eventType;
            Class<?> eventClazz = (Class<?>)ptEvent.getRawType();
            
            if(isClassAssignable(clazzBeanType, eventClazz))
            {
                return true;
            }
            
            return false;            
        }
        //Both of them is class type
        else if(observerType instanceof Class && eventType instanceof Class)
        {
            return isClassAssignable((Class<?>)observerType, (Class<?>) eventType);
        }
        
        return false;
    }
    
    
    /**
     * Returns true if rhs is assignable type
     * to the lhs, false otherwise.
     * 
     * @param lhs left hand side class
     * @param rhs right hand side class
     * @return true if rhs is assignable to lhs
     */
    public static boolean isClassAssignable(Class<?> lhs, Class<?> rhs)
    {
        Asserts.assertNotNull(lhs, "lhs parameter can not be null");
        Asserts.assertNotNull(rhs, "rhs parameter can not be null");

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
     * Returns true if given bean's api type is injectable to
     * injection point required type.
     * 
     * @param beanType bean parametrized api type
     * @param requiredType injection point parametrized api type
     * @return if injection is possible false otherwise
     */
    public static boolean isAssignableForParametrized(ParameterizedType beanType, ParameterizedType requiredType)
    {
        Class<?> beanRawType = (Class<?>) beanType.getRawType();
        Class<?> requiredRawType = (Class<?>) requiredType.getRawType();

        if (ClassUtil.isClassAssignable(requiredRawType,beanRawType))
        {
            //Bean api type actual type arguments
            Type[] beanTypeArgs = beanType.getActualTypeArguments();
            
            //Injection point type actual arguments
            Type[] requiredTypeArgs = requiredType.getActualTypeArguments();
            
            if(beanTypeArgs.length != requiredTypeArgs.length)
            {                
                return false;
            }
            else
            {
                return isAssignableForParametrizedCheckArguments(beanTypeArgs, requiredTypeArgs);
            }
        }

        return false;
    }
    
    /**
     * Check parametrized type actual type arguments.
     * @param beanTypeArgs bean type actual type arguments
     * @param requiredTypeArgs required type actual type arguments.
     * @return true if assignment applicable
     */
    private static boolean isAssignableForParametrizedCheckArguments(Type[] beanTypeArgs, Type[] requiredTypeArgs)
    {
        Type requiredTypeArg = null;
        Type beanTypeArg = null;
        int ok = 0;
        for(int i = 0; i< requiredTypeArgs.length;i++)
        {
            requiredTypeArg = requiredTypeArgs[i];
            beanTypeArg = beanTypeArgs[i];

            //Required type is parametrized and bean type is parametrized
            if(ClassUtil.isParametrizedType(requiredTypeArg) && ClassUtil.isParametrizedType(beanTypeArg))
            {
                if (checkBeanAndRequiredTypeIsParametrized(beanTypeArg, requiredTypeArg))
                {
                    ok++;
                }
            }
            //Required type is wildcard
            else if(ClassUtil.isWildCardType(requiredTypeArg))
            {
                if (checkRequiredTypeIsWildCard(beanTypeArg, requiredTypeArg))
                {
                    ok++;
                }
            }
            //Required type is actual type and bean type is type variable
            else if(requiredTypeArg instanceof Class && ClassUtil.isTypeVariable(beanTypeArg))
            {
                if (checkRequiredTypeIsClassAndBeanTypeIsVariable(beanTypeArg, requiredTypeArg))
                {
                    ok++;
                }
            }
            //Required type is Type variable and bean type is type variable
            else if(ClassUtil.isTypeVariable(requiredTypeArg) && ClassUtil.isTypeVariable(beanTypeArg))
            {
                if ( checkBeanTypeAndRequiredIsTypeVariable(beanTypeArg, requiredTypeArg))
                {
                    ok++;
                }
            }
            else if (requiredTypeArg instanceof ParameterizedType && beanTypeArg instanceof TypeVariable)
            {
                if (checkRequiredTypeIsParameterizedAndBeanTypeIsVariable(beanTypeArg, requiredTypeArg))
                {
                    ok++;
                }
            }
            //Both type is actual type
            else if((beanTypeArg instanceof Class) && (requiredTypeArg instanceof Class))
            {
                if(isClassAssignable((Class<?>)requiredTypeArg,(Class<?>)beanTypeArg))
                {
                    ok++;
                }
            }
            //Bean type is actual type and required type is type variable
            else if((beanTypeArg instanceof Class) && (ClassUtil.isTypeVariable(requiredTypeArg)))
            {
                if (checkRequiredTypeIsTypeVariableAndBeanTypeIsClass(beanTypeArg, requiredTypeArg))
                {
                    ok++;
                }
            }
        }

        return ok == requiredTypeArgs.length;
    }

    /**
     * Check parametrized bean type and parametrized
     * required types.
     * @param beanTypeArg parametrized bean type
     * @param requiredTypeArg parametrized required type
     * @return true if types are assignables
     * @since 1.1.1
     */
    public static boolean checkBeanAndRequiredTypeIsParametrized(Type beanTypeArg, Type requiredTypeArg)
    {
        ParameterizedType ptRequiredTypeArg = (ParameterizedType)requiredTypeArg;
        ParameterizedType ptBeanTypeArg = (ParameterizedType)beanTypeArg;
        
        //Equal raw types
        if(ptRequiredTypeArg.getRawType().equals(ptBeanTypeArg.getRawType()))
        {
            //Check arguments
            Type[] actualArgsRequiredType = ptRequiredTypeArg.getActualTypeArguments();
            Type[] actualArgsBeanType = ptRequiredTypeArg.getActualTypeArguments();
            
            if(actualArgsRequiredType.length > 0 && actualArgsBeanType.length == actualArgsRequiredType.length)
            {
                return isAssignableForParametrizedCheckArguments(actualArgsBeanType, actualArgsRequiredType);
            }
            else
            {
                return true;
            }
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
        else if(ClassUtil.isTypeVariable(beanTypeArg))
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
         
        return true;
    }
    
    /**
     * Checking bean type and required type.
     * <p>
     * Required type is class and bean type is
     * a type variable.
     * </p>
     * @param beanTypeArg bean type 
     * @param requiredTypeArg required type
     * @return true if condition satisfy
     */
    public static boolean checkRequiredTypeIsClassAndBeanTypeIsVariable(Type beanTypeArg, Type requiredTypeArg)
    {
        Class<?> clazzRequiredType = (Class<?>)requiredTypeArg;
        
        TypeVariable<?> tvBeanTypeArg = (TypeVariable<?>)beanTypeArg;
        //TODO respect other bounds
        Type tvBound = tvBeanTypeArg.getBounds()[0];
        
        if(tvBound instanceof Class)
        {
            Class<?> clazzTvBound = (Class<?>)tvBound;
            
            if(clazzTvBound != Object.class)
            {
                if(!clazzTvBound.isAssignableFrom(clazzRequiredType))
                {
                    return false;
                }                                    
            }            
        }
        
        return true;
    }

    public static boolean checkRequiredTypeIsParameterizedAndBeanTypeIsVariable(Type beanTypeArg, Type requiredTypeArg)
    {
        ParameterizedType requiredType = (ParameterizedType)requiredTypeArg;
        //TODO respect parameters of required type
        return checkRequiredTypeIsClassAndBeanTypeIsVariable(beanTypeArg, requiredType.getRawType());
    }
    
    public static boolean checkRequiredTypeIsTypeVariableAndBeanTypeIsClass(Type beanTypeArg, Type requiredTypeArg)
    {
        Class<?> clazzBeanType = (Class<?>)beanTypeArg;
        
        TypeVariable<?> tvRequiredTypeArg = (TypeVariable<?>)requiredTypeArg;
        Type tvBound = tvRequiredTypeArg.getBounds()[0];
        
        if(tvBound instanceof Class)
        {
            Class<?> clazzTvBound = (Class<?>)tvBound;
            
            if(clazzTvBound.isAssignableFrom(clazzBeanType))
            {
                return true;
            }                    
        }
        
        return false;
    }
    

    public static boolean checkBeanTypeAndRequiredIsTypeVariable(Type beanTypeArg, Type requiredTypeArg)
    {
        TypeVariable<?> tvBeanTypeArg = (TypeVariable<?>)beanTypeArg;
        Type tvBeanBound = tvBeanTypeArg.getBounds()[0];
        
        TypeVariable<?> tvRequiredTypeArg = (TypeVariable<?>)requiredTypeArg;
        Type tvRequiredBound = tvRequiredTypeArg.getBounds()[0];
        
        if(tvBeanBound instanceof Class && tvRequiredBound instanceof Class)
        {
            Class<?> clazzTvBeanBound = (Class<?>)tvBeanBound;
            Class<?> clazzTvRequiredBound = (Class<?>)tvRequiredBound;
            
            if(clazzTvBeanBound.isAssignableFrom(clazzTvRequiredBound))
            {
                return true;
            }                    
        }
        
        return false;
    }

    /**
     * Learn whether the specified class is defined with type parameters.
     * @param type to check
     * @return true if there are type parameters
     * @since 1.1.1
     */
    public static boolean isDefinitionContainsTypeVariables(Type type)
    {
        Class<?> clazz = ClassUtil.getClass(type);
        Asserts.nullCheckForClass(clazz);
        
        return (clazz.getTypeParameters().length > 0) ? true : false;
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
        Asserts.assertNotNull(type, "type parameter can not be null");

        if (type instanceof ParameterizedType)
        {
            return ((ParameterizedType) type).getActualTypeArguments();

        }
        else
        {
            return new Type[0];
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
            return getClazz(arrayType.getGenericComponentType());
        }
        else if (type instanceof WildcardType)
        {
            WildcardType wildcardType = (WildcardType)type;
            Type[] bounds = wildcardType.getUpperBounds();
            if (bounds.length > 1)
            {
                throw new DefinitionException("Illegal use of wild card type with more than one upper bound: " + wildcardType);
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
                throw new DefinitionException("Illegal use of type variable with more than one bound: " + typeVariable);
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
            throw new DefinitionException("Unsupported type " + type);
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
        Asserts.assertNotNull(pType, "pType argument can not be null");
        
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
}
