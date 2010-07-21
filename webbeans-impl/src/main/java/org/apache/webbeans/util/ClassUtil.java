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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.PrivilegedActionException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Provider;

import org.apache.webbeans.config.BeanTypeSetResolver;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;

/**
 * Utility classes with respect to the class operations.
 *
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public final class ClassUtil
{
    public static final Map<Class<?>, Object> PRIMITIVE_CLASS_DEFAULT_VALUES = new HashMap<Class<?>, Object>();;

    public static final Set<Class<?>> VALUE_TYPES = new HashSet<Class<?>>();

    public static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPERS_MAP = new HashMap<Class<?>, Class<?>>();

    public static final String WEBBEANS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    
    public static final Object[] OBJECT_EMPTY = new Object[0];
    
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(ClassUtil.class);

    static
    { 
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(Integer.class, Integer.MIN_VALUE);
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(Float.class, Float.MIN_VALUE);
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(Double.class, Double.MIN_VALUE);
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(Character.class, Character.MIN_VALUE);
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(String.class, new String());
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(BigDecimal.class, BigDecimal.ZERO);
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(BigInteger.class, BigInteger.ZERO);
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(Long.class, Long.MIN_VALUE);
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(Byte.class, Byte.MIN_VALUE);
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(Short.class, Short.MIN_VALUE);
        PRIMITIVE_CLASS_DEFAULT_VALUES.put(Boolean.class, Boolean.FALSE);

        VALUE_TYPES.add(String.class);
        VALUE_TYPES.add(Date.class);
        VALUE_TYPES.add(Calendar.class);
        VALUE_TYPES.add(Class.class);
        VALUE_TYPES.add(List.class);
        VALUE_TYPES.add(Enum.class);
        VALUE_TYPES.add(java.sql.Date.class);
        VALUE_TYPES.add(Time.class);
        VALUE_TYPES.add(Timestamp.class);
        VALUE_TYPES.add(BigDecimal.class);
        VALUE_TYPES.add(BigInteger.class);
        
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

    /**
     * Check the parametrized type actual arguments equals with the class type
     * variables at the injection point.
     * 
     * @param variables type variable
     * @param types type
     * @return
     */
    public static boolean checkEqual(TypeVariable<?>[] variables, Type[] types)
    {
        Asserts.assertNotNull(variables, "variables parameter can not be null");
        Asserts.assertNotNull(types, "types parameter can not be null");

        for (TypeVariable<?> variable : variables)
        {
            for (Type type : types)
            {
                if (type instanceof TypeVariable)
                {
                    TypeVariable<?> t = ((TypeVariable<?>) type);
                    if (t.getGenericDeclaration().equals(variable.getGenericDeclaration()))
                    {
                        if (t.getName().equals(variable.getName()))
                        {
                            continue;
                        }
                        else
                        {
                            return false;
                        }

                    }
                    else
                    {
                        return false;
                    }
                }
            }
        }

        return true;

    }
    
    public static Object newInstance(Class<?> clazz)
    {
        try
        {
            if(System.getSecurityManager() != null)
            {
                return SecurityUtil.doPrivilegedObjectCreate(clazz);
            }            
            
            return clazz.newInstance();
            
        }
        catch(Exception e)
        {
            Throwable cause = e;
            if(e instanceof PrivilegedActionException)
            {
                cause = ((PrivilegedActionException)e).getCause();
            }
            
            String error = "Error is occured while creating an instance of class : " + clazz.getName(); 
            logger.error(error, cause);
            throw new WebBeansException(error,cause); 
        
        }
    }

    public static Class<?> getClassFromName(String name)
    {
        Class<?> clazz = null;
        ClassLoader loader = null;
        try
        {
            loader = WebBeansUtil.getCurrentClassLoader();
            clazz = Class.forName(name, true , loader);
            //X clazz = loader.loadClass(name);
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
     * Check final modifier.
     * 
     * @param modifier modifier
     * @return true or false
     */
    public static boolean isFinal(Integer modifier)
    {
        Asserts.nullCheckForModifier(modifier);

        return Modifier.isFinal(modifier);
    }

    /**
     * Check abstract modifier.
     * 
     * @param modifier modifier
     * @return true or false
     */
    public static boolean isAbstract(Integer modifier)
    {
        Asserts.nullCheckForModifier(modifier);

        return Modifier.isAbstract(modifier);
    }

    /**
     * Check interface modifier.
     * 
     * @param modifier modifier
     * @return true or false
     */
    public static boolean isInterface(Integer modifier)
    {
        Asserts.nullCheckForModifier(modifier);

        return Modifier.isInterface(modifier);
    }

    /**
     * Check for class that has a final method or not.
     * 
     * @param clazz check methods of it
     * @return true or false
     */
    public static boolean hasFinalMethod(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);
        for (Method m : methods)
        {
            if (isFinal(m.getModifiers()))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Check the class is inner or not
     * 
     * @param modifier modifier
     * @return true or false
     */
    public static boolean isInnerClazz(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        return clazz.isMemberClass();
    }

    /**
     * Check the modifier contains static keyword.
     * 
     * @param modifier modifier
     * @return true or false
     */
    public static boolean isStatic(Integer modifier)
    {
        Asserts.nullCheckForModifier(modifier);

        return Modifier.isStatic(modifier);
    }

    /**
     * Check the modifier contains static keyword.
     * 
     * @param modifier modifier
     * @return true or false
     */
    public static boolean isPublic(Integer modifier)
    {
        Asserts.nullCheckForModifier(modifier);

        return Modifier.isPublic(modifier);
    }

    /**
     * Check the modifier contains static keyword.
     * 
     * @param modifier modifier
     * @return true or false
     */
    public static boolean isPrivate(Integer modifier)
    {
        Asserts.nullCheckForModifier(modifier);

        return Modifier.isPrivate(modifier);
    }

    /**
     * Gets the Java Standart Class default value.
     * 
     * @param <T> parametrized type
     * @param clazz class instance
     * @return default value of the class
     */
    public static <T> T defaultJavaValues(Class<T> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        Set<Class<?>> keySet = PRIMITIVE_CLASS_DEFAULT_VALUES.keySet();
        Iterator<Class<?>> it = keySet.iterator();
        while (it.hasNext())
        {
            Class<?> obj = it.next();
            if (clazz.equals(obj))
            {
                return (T) PRIMITIVE_CLASS_DEFAULT_VALUES.get(obj);
            }
        }

        return null;

    }
    
    public static Class<?>  getPrimitiveWrapper(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        
        return PRIMITIVE_TO_WRAPPERS_MAP.get(clazz);

    }
    
    public static Class<?> getWrapperPrimitive(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        
        Set<Class<?>> keySet = PRIMITIVE_TO_WRAPPERS_MAP.keySet();
        
        for(Class<?> key : keySet)
        {
             if(PRIMITIVE_TO_WRAPPERS_MAP.get(key).equals(clazz))
             {
                 return key;
             }
        }
        
        return null;
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
     * Gets the declared methods of the given class.
     * 
     * @param clazz class instance
     * @return the declared methods
     */
    public static Method[] getDeclaredMethods(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        return SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);
    }

    /**
     * Check that method has any formal arguments.
     * 
     * @param method method instance
     * @return true or false
     */
    public static boolean isMethodHasParameter(Method method)
    {
        Asserts.nullCheckForMethod(method);

        Class<?>[] types = method.getParameterTypes();
        if (types.length != 0)
        {
            return true;
        }

        return false;
    }

    /**
     * Gets the return type of the method.
     * 
     * @param method method instance
     * @return return type
     */
    public static Class<?> getReturnType(Method method)
    {
        Asserts.nullCheckForMethod(method);
        return method.getReturnType();
    }

    /**
     * Check method throws checked exception or not.
     * 
     * @param method method instance
     * @return trur or false
     */
    public static boolean isMethodHasCheckedException(Method method)
    {
        Asserts.nullCheckForMethod(method);

        Class<?>[] et = method.getExceptionTypes();

        if (et.length > 0)
        {
            for (Class<?> type : et)
            {
                if (Error.class.isAssignableFrom(type) || RuntimeException.class.isAssignableFrom(type))
                {
                    return false;
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
     * Check method throws Exception or not.
     * 
     * @param method method instance
     * @return trur or false
     */
    public static boolean isMethodHasException(Method method)
    {
        Asserts.nullCheckForMethod(method);

        Class<?>[] et = method.getExceptionTypes();

        if (et.length == 1)
        {
            if (et[0].equals(Exception.class))
            {
                return true;
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
            throw new WebBeansException("Exception occurs in the method call with method : " + method.getName() + " in class : " + instance.getClass().getName());
        }

    }

    public static List<Class<?>> getSuperClasses(Class<?> clazz, List<Class<?>> list)
    {
        Asserts.nullCheckForClass(clazz);

        Class<?> sc = clazz.getSuperclass();
        if (sc != null)
        {
            list.add(sc);
            getSuperClasses(sc, list);
        }

        return list;

    }

    public static Class<?>[] getMethodParameterTypes(Method method)
    {
        Asserts.nullCheckForMethod(method);
        return method.getParameterTypes();
    }

    public static List<String> getObjectMethodNames()
    {
        if (objectMethodNames == null)
        {
            // not much syncronisation needed...
            List<String> list = new ArrayList<String>();
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
    private static List objectMethodNames= null;
    


    public static boolean isObjectMethod(String methodName)
    {
        Asserts.assertNotNull(methodName, "methodName parameter can not be null");
        return getObjectMethodNames().contains(methodName);
    }

    public static boolean isMoreThanOneMethodWithName(String methodName, Class<?> clazz)
    {
        Asserts.assertNotNull(methodName, "methodName parameter can not be null");
        Asserts.nullCheckForClass(clazz);

        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);
        int i = 0;
        for (Method m : methods)
        {
            if (m.getName().equals(methodName))
            {
                i++;
            }
        }

        if (i > 1)
        {
            return true;
        }

        return false;

    }

    public static <T> Constructor<T> isContaintNoArgConstructor(Class<T> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        try
        {
            return SecurityUtil.doPrivilegedGetDeclaredConstructor(clazz, new Class<?>[] {});

        }
        catch (Exception e)
        {
            return null;
        }

    }

    /**
     * Check the modifiers contains the public keyword.
     * 
     * @param modifs modifiers
     * @return true or false
     */
    public static boolean isPublic(int modifs)
    {
        return Modifier.isPublic(modifs);
    }

    /**
     * Gets java package if exist.
     * 
     * @param packageName package name
     * @return the package with given name
     */
    public Package getPackage(String packageName)
    {
        Asserts.assertNotNull(packageName, "packageName parameter can not be null");

        return Package.getPackage(packageName);
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
        if (type instanceof ParameterizedType)
        {
            return true;
        }

        return false;
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
        
        if (type instanceof WildcardType)
        {
            return true;
        }

        return false;
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

        if (type instanceof TypeVariable)
        {
            return true;
        }

        return false;

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

        if (!isAbstract(modifier) && !isInterface(modifier))
        {
            return true;
        }

        return false;
    }

    /**
     * Returns class constructor array.
     * 
     * @param <T> class type arfument
     * @param clazz class that is searched for constructor.
     * @return class constructor array
     */
    public static <T> Constructor<T>[] getConstructors(Class<T> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        
        return (Constructor<T>[])SecurityUtil.doPrivilegedGetDeclaredConstructors(clazz);
    }

    /**
     * Returns true if class has a default constructor.
     * 
     * @param <T> type argument of class
     * @param clazz class type
     * @return true if class has a default constructor.
     */
    public static <T> boolean hasDefaultConstructor(Class<T> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        
        try
        {
            SecurityUtil.doPrivilegedGetDeclaredConstructor(clazz, new Class<?>[] {});
        }
        catch (SecurityException e)
        {
            throw new WebBeansException(e);
        }
        catch (NoSuchMethodException e)
        {
            return false;
        }

        return true;
    }
    
    /**
     * See specification 5.2.3.
     * @param beanType bean type
     * @param requiredType required type
     * @return true if assignable
     */
    public static boolean isAssignable(Type beanType, Type requiredType)
    {
        Asserts.assertNotNull(beanType, "beanType parameter can not be null");
        Asserts.assertNotNull(requiredType, "requiredType parameter can not be null");
        
        //Bean and required types are ParametrizedType
        if (beanType instanceof ParameterizedType && requiredType instanceof ParameterizedType)
        {
            return isAssignableForParametrized((ParameterizedType) beanType, (ParameterizedType) requiredType);
        }
        //Both type is class type
        else if (beanType instanceof Class && requiredType instanceof Class)
        {
            Class<?> clzBeanType = (Class<?>)beanType;
            Class<?> clzReqType = (Class<?>)requiredType;
            
            if(clzBeanType.isPrimitive())
            {
                clzBeanType = getPrimitiveWrapper(clzBeanType);
            }
            
            if(clzReqType.isPrimitive())
            {
                clzReqType = getPrimitiveWrapper(clzReqType);
            }
            
            return clzReqType.equals(clzBeanType);
        }
        //Bean type is Parametrized and required type is class type
        else if(beanType instanceof ParameterizedType && requiredType instanceof Class)
        {
            boolean ok = true;
            ParameterizedType ptBean = (ParameterizedType)beanType;
            Class<?> clazzBeanType = (Class<?>)ptBean.getRawType();
            Class<?> clazzReqType = (Class<?>)requiredType;
            if(isClassAssignable(clazzReqType, clazzBeanType ))
            {
                Type[]  beanTypeArgs = ptBean.getActualTypeArguments();               
                for(Type actual : beanTypeArgs)
                {
                    if(!ClassUtil.isUnboundedTypeVariable(actual))
                    {
                        if(actual instanceof Class)
                        {
                            Class<?> clazz = (Class<?>)actual;
                            if(clazz.equals(Object.class))
                            {
                                continue;
                            }
                            else
                            {
                                ok = false;
                                break;
                            }
                        }
                        else
                        {
                            ok = false;
                            break;
                        }
                    }
                }                
            }
            else
            {
                ok = false;
            }
            
            
            return ok;
        }
        //Bean type is class and required type is parametrized
        else if(beanType instanceof Class && requiredType instanceof ParameterizedType)
        {
            Class<?> clazzBeanType = (Class<?>)beanType;
            ParameterizedType ptReq = (ParameterizedType)requiredType;
            Class<?> clazzReqType = (Class<?>)ptReq.getRawType();
            
            if(Provider.class.isAssignableFrom(clazzReqType) ||
                    Event.class.isAssignableFrom(clazzReqType))
            {
                if(isClassAssignable(clazzReqType, clazzBeanType))
                {
                    return true;
                }    
            }
                        
            return false;
        }
        else
        {
            return false;
        }
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
        for(int i = 0; i< requiredTypeArgs.length;i++)
        {
            requiredTypeArg = requiredTypeArgs[i];
            beanTypeArg = beanTypeArgs[i];
            
            //Required type is parametrized and bean type is parametrized
            if(ClassUtil.isParametrizedType(requiredTypeArg) && ClassUtil.isParametrizedType(beanTypeArg))
            {
                return checkBeanAndRequiredTypeisParametrized(beanTypeArg, requiredTypeArg);
            }
            //Required type is wildcard
            else if(ClassUtil.isWildCardType(requiredTypeArg))
            {
                return checkRequiredTypeisWildCard(beanTypeArg, requiredTypeArg);
            }
            //Required type is actual type and bean type is type variable
            else if(requiredTypeArg instanceof Class && ClassUtil.isTypeVariable(beanTypeArg))
            {
                return checkRequiredTypeIsClassAndBeanTypeIsVariable(beanTypeArg, requiredTypeArg);
            }
            //Required type is Type variable and bean type is type variable
            else if(ClassUtil.isTypeVariable(requiredTypeArg) && ClassUtil.isTypeVariable(beanTypeArg))
            {
                return checkBeanTypeAndRequiredIsTypeVariable(beanTypeArg, requiredTypeArg);
            }      
            
            //Both type is actual type
            else if((beanTypeArg instanceof Class) && (requiredTypeArg instanceof Class))
            {
                if(isClassAssignable((Class<?>)requiredTypeArg,(Class<?>)beanTypeArg))
                {
                    return true;
                }
            }
            //Bean type is actual type and required type is type variable
            else if((beanTypeArg instanceof Class) && (ClassUtil.isTypeVariable(requiredTypeArg)))
            {
                return checkRequiredTypeIsTypeVariableAndBeanTypeIsClass(beanTypeArg, requiredTypeArg);
            }
        }
        
        return false;
    }
    
    /**
     * Check parametrized bean type and parametrized
     * required types.
     * @param beanTypeArg parametrized bean type
     * @param requiredTypeArg parametrized required type
     * @return true if types are assignables
     */
    public static boolean checkBeanAndRequiredTypeisParametrized(Type beanTypeArg, Type requiredTypeArg)
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
     * @return true if contdition satisfies
     */
    public static boolean checkRequiredTypeisWildCard(Type beanTypeArg, Type requiredTypeArg)
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


    public static boolean classHasFieldWithName(Class<?> clazz, String fieldName)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(fieldName, "fieldName parameter can not be null");
        try
        {
            SecurityUtil.doPrivilegedGetDeclaredField(clazz, fieldName);
        }
        catch (SecurityException e)
        {
            // we must throw here!
            throw new WebBeansException(e);
        }
        catch (NoSuchFieldException e2)
        {
            return false;
        }

        return true;
    }

    public static boolean classHasMoreThanOneFieldWithName(Class<?> clazz, String fieldName)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(fieldName, "fieldName parameter can not be null");

        Field[] fields = SecurityUtil.doPrivilegedGetDeclaredFields(clazz);
        boolean ok = false;
        for (Field field : fields)
        {
            if (field.getName().equals(fieldName))
            {
                if (ok)
                {
                    return true;
                }
                else
                {
                    ok = true;
                }
            }
        }

        return false;
    }

    public static Field getFieldWithName(Class<?> clazz, String fieldName)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(fieldName, "fieldName parameter can not be null");
        try
        {

            return SecurityUtil.doPrivilegedGetDeclaredField(clazz,fieldName);

        }
        catch (SecurityException e)
        {
            // we must throw here!
            throw new WebBeansException(e);
        }
        catch (NoSuchFieldException e2)
        {
            return null;
        }

    }

    /**
     * @param clazz webbeans implementation class
     * @param methodName name of the method that is searched
     * @param parameterTypes parameter types of the method(it can be subtype of
     *            the actual type arguments of the method)
     * @return the list of method that satisfies the condition
     */
    public static List<Method> getClassMethodsWithTypes(Class<?> clazz, String methodName, List<Class<?>> parameterTypes)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(methodName, "methodName parameter can not be null");
        Asserts.assertNotNull(parameterTypes, "parameterTypes parameter can not be null");

        List<Method> methodList = new ArrayList<Method>();

        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);

        int j = 0;
        for (Method method : methods)
        {
            if (method.getName().equals(methodName))
            {
                Class<?>[] defineTypes = method.getParameterTypes();

                if (defineTypes.length != parameterTypes.size())
                {
                    continue;
                }

                boolean ok = true;

                if (parameterTypes != null && parameterTypes.size() > 0)
                {
                    ok = false;
                }

                if (!ok)
                {
                    for (Class<?> defineType : defineTypes)
                    {
                        if (defineType.isAssignableFrom(parameterTypes.get(j)))
                        {
                            ok = true;
                        }
                        else
                        {
                            ok = false;
                        }

                        j++;
                    }
                }

                if (ok)
                {
                    methodList.add(method);
                }
            }

        }

        return methodList;
    }

    public static Method getClassMethodWithTypes(Class<?> clazz, String methodName, List<Class<?>> parameterTypes)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(methodName, "methodName parameter can not be null");
        Asserts.assertNotNull(parameterTypes, "parameterTypes parameter can not be null");

        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);

        int j = 0;
        for (Method method : methods)
        {
            if (method.getName().equals(methodName))
            {
                if (parameterTypes != null && parameterTypes.size() > 0)
                {
                    Class<?>[] defineTypes = method.getParameterTypes();

                    if (defineTypes.length != parameterTypes.size())
                    {
                        continue;
                    }

                    boolean ok = false;

                    for (Class<?> defineType : defineTypes)
                    {
                        if (defineType.equals(parameterTypes.get(j)))
                        {
                            ok = true;
                        }
                        else
                        {
                            ok = false;
                        }
                    }

                    if (ok)
                    {
                        return method;
                    }
                }
                else
                {
                    return method;
                }
            }
        }

        return null;
    }

    public static boolean hasMethodWithName(Class<?> clazz, String methodName)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(methodName, "methodName parameter can not be null");

        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);

        for (Method method : methods)
        {
            if (method.getName().equals(methodName))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isPrimitive(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        return clazz.isPrimitive();
    }

    public static boolean isPrimitiveWrapper(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        return PRIMITIVE_TO_WRAPPERS_MAP.containsValue(clazz);

    }

    public static boolean isArray(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        return clazz.isArray();
    }

    public static boolean isEnum(Class<?> clazz)
    {
        return clazz.isEnum();
    }

    public static boolean isInValueTypes(Class<?> clazz)
    {
        boolean result = VALUE_TYPES.contains(clazz);

        if (!result)
        {
            result = clazz.isPrimitive();
        }

        if (!result)
        {
            if (Enum.class.isAssignableFrom(clazz))
            {
                return true;
            }
        }

        return result;
    }

    /**
     * Gets the primitive/wrapper value of the parsed {@link String} parameter.
     * 
     * @param type primitive or wrapper of the primitive type
     * @param value value of the type
     * @return the parse of the given {@link String} value into the
     *         corresponding value, if any exception occurs, returns null as the
     *         value.
     */
    public static Object isValueOkForPrimitiveOrWrapper(Class<?> type, String value)
    {
        if (type.equals(Integer.TYPE) || type.equals(Integer.class))
        {
            return Integer.valueOf(value);
        }

        if (type.equals(Float.TYPE) || type.equals(Float.class))
        {
            return Float.valueOf(value);
        }

        if (type.equals(Double.TYPE) || type.equals(Double.class))
        {
            return Double.valueOf(value);
        }

        if (type.equals(Character.TYPE) || type.equals(Character.class))
        {
            return value.toCharArray()[0];
        }

        if (type.equals(Long.TYPE) || type.equals(Long.class))
        {
            return Long.valueOf(value);
        }

        if (type.equals(Byte.TYPE) || type.equals(Byte.class))
        {
            return Byte.valueOf(value);
        }

        if (type.equals(Short.TYPE) || type.equals(Short.class))
        {
            return Short.valueOf(value);
        }

        if (type.equals(Boolean.TYPE) || type.equals(Boolean.class))
        {
            return Boolean.valueOf(value);
        }

        return null;
    }

    public static Enum isValueOkForEnum(Class clazz, String value)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(value, "value parameter can not be null");

        return Enum.valueOf(clazz, value);
    }

    public static Date isValueOkForDate(String value) throws ParseException
    {
        try
        {
            Asserts.assertNotNull(value, "value parameter can not be null");
            return DateFormat.getDateTimeInstance().parse(value);

        }
        catch (ParseException e)
        {
            // Check for simple date format
            SimpleDateFormat format = new SimpleDateFormat(WEBBEANS_DATE_FORMAT);

            return format.parse(value);
        }
    }

    public static Calendar isValueOkForCalendar(String value) throws ParseException
    {
        Calendar calendar = null;

        Asserts.assertNotNull(value, "value parameter can not be null");
        Date date = isValueOkForDate(value);

        if (date == null)
        {
            return null;
        }
        else
        {
            calendar = Calendar.getInstance();
            calendar.setTime(date);
        }

        return calendar;
    }

    public static Object isValueOkForBigDecimalOrInteger(Class<?> type, String value)
    {
        Asserts.assertNotNull(type);
        Asserts.assertNotNull(value);

        if (type.equals(BigInteger.class))
        {
            return new BigInteger(value);
        }
        else if (type.equals(BigDecimal.class))
        {
            return new BigDecimal(value);
        }
        else
        {
            return new WebBeansException(new IllegalArgumentException("Argument is not valid"));
        }
    }

    public static boolean isDefinitionConstainsTypeVariables(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        
        return (clazz.getTypeParameters().length > 0) ? true : false;
    }
    
    
    public static TypeVariable<?>[] getTypeVariables(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        
        return clazz.getTypeParameters();
    }

    public static Type[] getActualTypeArguements(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        if (clazz.getGenericSuperclass() instanceof ParameterizedType)
        {
            return ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments();

        }
        else
        {
            return new Type[0];
        }
    }

    public static Type[] getActualTypeArguements(Type type)
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

    public static Class<?> getFirstRawType(Type type)
    {
        Asserts.assertNotNull(type, "type argument can not be null");

        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;
            return (Class<?>) pt.getRawType();
        }

        return (Class<?>) type;
    }

    public static Set<Type> setTypeHierarchy(Set<Type> set, Type clazz)
    {
        BeanTypeSetResolver resolver = new BeanTypeSetResolver(clazz);
        resolver.startConfiguration();
        set.addAll(resolver.getHierarchy());
        
        return set;
    }

    
    
    /**
     * Return raw class type for given type.
     * @param type base type instance
     * @return class type for given type
     */
    public static Class<?> getClazz(Type type)
    {
        Class<?> raw = null;
        
        if(type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType)type;
            raw = (Class<?>)pt.getRawType();                
        }
        else if(type instanceof Class)
        {
            raw = (Class<?>)type;
        }
        else if(type instanceof GenericArrayType)
        {
            GenericArrayType arrayType = (GenericArrayType)type;
            raw = getClazz(arrayType.getGenericComponentType());
        }
        
        return raw;
    }
    
    //For Ejb API Type
    public static Set<Type> setClassTypeHierarchy(Set<Type> set, Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        set.add(clazz);

        Class<?> sc = clazz.getSuperclass();

        if (sc != null)
        {
            setTypeHierarchy(set, sc);
        }

        return set;
    }
    

    public static Set<Type> setInterfaceTypeHierarchy(Set<Type> set, Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);

        Class<?>[] interfaces = clazz.getInterfaces();

        for (Class<?> cl : interfaces)
        {
            set.add(cl);

            setTypeHierarchy(set, cl);
        }

        return set;
    }

    public static Type[] getGenericSuperClassTypeArguments(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        Type type = clazz.getGenericSuperclass();

        if (type != null)
        {
            if (type instanceof ParameterizedType)
            {
                ParameterizedType pt = (ParameterizedType) type;

                //if (checkParametrizedType(pt))
                //{
                    return pt.getActualTypeArguments();
                //}
            }
        }

        return new Type[0];

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

    public static boolean isFirstParametricTypeArgGeneric(ParameterizedType type)
    {
        Asserts.assertNotNull(type, "type parameter can not be null");
        
        Type[] args = type.getActualTypeArguments();
        
        if(args.length == 0)
        {
            return false;
        }
        
        Type arg = args[0];

        if ((arg instanceof TypeVariable) || (arg instanceof WildcardType))
        {
            return true;
        }

        return false;
    }

    public static List<Type[]> getGenericSuperInterfacesTypeArguments(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        List<Type[]> list = new ArrayList<Type[]>();

        Type[] types = clazz.getGenericInterfaces();
        for (Type type : types)
        {
            if (type instanceof ParameterizedType)
            {
                ParameterizedType pt = (ParameterizedType) type;

                //if (checkParametrizedType(pt))
                //{
                    list.add(pt.getActualTypeArguments());
                //}
            }
        }

        return list;
    }

    public static Field getFieldWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(annotation, "annotation parameter can not be null");

        Field[] fields = SecurityUtil.doPrivilegedGetDeclaredFields(clazz);
        for (Field field : fields)
        {
            if (AnnotationUtil.hasAnnotation(field.getAnnotations(), annotation))
            {
                return field;
            }

        }

        return null;

    }
    
    public static Field[] getFieldsWithType(Class<?> clazz, Type type)
    {
        Asserts.nullCheckForClass(clazz);
        Asserts.assertNotNull(type, "type parameter can not be null");

        List<Field> fieldsWithType = new ArrayList<Field>();
        Field[] fields = SecurityUtil.doPrivilegedGetDeclaredFields(clazz);
        for (Field field : fields)
        {
            if(field.getType().equals(type))
            {
                fieldsWithType.add(field);
            }
        }

        return fieldsWithType.toArray(new Field[0]);

    }
    

    public static boolean checkForTypeArguments(Class<?> src, Type[] typeArguments, Class<?> target)
    {
        Asserts.assertNotNull(src, "src parameter can not be null");
        Asserts.assertNotNull(typeArguments, "typeArguments parameter can not be null");
        Asserts.assertNotNull(target, "target parameter can not be null");

        Type[] types = getGenericSuperClassTypeArguments(target);

        boolean found = false;

        if (Arrays.equals(typeArguments, types))
        {
            return true;
        }
        else
        {
            Class<?> superClazz = target.getSuperclass();
            if (superClazz != null)
            {
                found = checkForTypeArguments(src, typeArguments, superClazz);
            }
        }

        if (!found)
        {
            List<Type[]> list = getGenericSuperInterfacesTypeArguments(target);
            if (!list.isEmpty())
            {
                Iterator<Type[]> it = list.iterator();
                while (it.hasNext())
                {
                    types = it.next();
                    if (Arrays.equals(typeArguments, types))
                    {
                        found = true;
                        break;
                    }
                }

            }
        }

        if (!found)
        {
            Class<?>[] superInterfaces = target.getInterfaces();
            for (Class<?> inter : superInterfaces)
            {
                found = checkForTypeArguments(src, typeArguments, inter);
                if (found)
                {
                    break;
                }
            }
        }

        return found;
    }
    
    public static void setField(Object instance, Field field, Object value)
    {
        Asserts.assertNotNull(instance);
        Asserts.assertNotNull(field);
        
        if(!field.isAccessible())
        {
            SecurityUtil.doPrivilegedSetAccessible(field, true);
        }
        
        try
        {
            field.set(instance, value);
        }
        catch (IllegalArgumentException e)
        {
            throw new WebBeansException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new WebBeansException(e);
        }
        
    }
 
    public static Throwable getRootException(Throwable throwable)
    {
        if(throwable.getCause() == null)
        {
            return throwable;
        }
        else
        {
            return getRootException(throwable.getCause());
        }
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

    public static boolean isOverriden(Method subClassMethod, Method superClassMethod)
    {
        if (subClassMethod.getName().equals(superClassMethod.getName()) && Arrays.equals(subClassMethod.getParameterTypes(), superClassMethod.getParameterTypes()))
        {
            int modifiers = superClassMethod.getModifiers();
            if(Modifier.isPrivate(modifiers))
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
    
    public static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>[] parameters)
    {
        try
        {
            return SecurityUtil.doPrivilegedGetDeclaredMethod(clazz,methodName, parameters);
            
        }
        catch(NoSuchMethodException e)
        {
            return null;
        }
    }
    
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>[] parameterTypes)
    {
        try
        {
            return clazz.getConstructor(parameterTypes);
            
        }
        catch(NoSuchMethodException e)
        {
            return null;
        }
    }    
}
