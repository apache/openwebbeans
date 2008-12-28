/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
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

import org.apache.webbeans.exception.WebBeansException;

/**
 * Utility classes with respect to the class operations.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public final class ClassUtil
{
	public static Map<Class<?>, Object> PRIMITIVE_CLASS_DEFAULT_VALUES = null;

	public static Set<Class<?>> VALUE_TYPES = new HashSet<Class<?>>();

	public static Set<Class<?>> PRIMITIVE_WRAPPERS = new HashSet<Class<?>>();
	
	public static Set<Class<?>> PRIMITIVES = new HashSet<Class<?>>();

	static
	{
		PRIMITIVE_CLASS_DEFAULT_VALUES = new HashMap<Class<?>, Object>();
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

		PRIMITIVES.add(Integer.TYPE);
		PRIMITIVES.add(Float.TYPE);
		PRIMITIVES.add(Double.TYPE);
		PRIMITIVES.add(Character.TYPE);
		PRIMITIVES.add(Long.TYPE);
		PRIMITIVES.add(Byte.TYPE);
		PRIMITIVES.add(Short.TYPE);
		PRIMITIVES.add(Boolean.TYPE);
		
		PRIMITIVE_WRAPPERS.add(Integer.class);
		PRIMITIVE_WRAPPERS.add(Float.class);
		PRIMITIVE_WRAPPERS.add(Double.class);
		PRIMITIVE_WRAPPERS.add(Character.class);
		PRIMITIVE_WRAPPERS.add(Long.class);
		PRIMITIVE_WRAPPERS.add(Byte.class);
		PRIMITIVE_WRAPPERS.add(Short.class);
		PRIMITIVE_WRAPPERS.add(Boolean.class);

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
	 * @param variables
	 *            type variable
	 * @param types
	 *            type
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
						} else
						{
							return false;
						}

					} else
					{
						return false;
					}
				}
			}
		}

		return true;

	}

	public static Class<?> getClassFromName(String name)
	{
		Class<?> clazz = null;

		try
		{
			ClassLoader loader = WebBeansUtil.getCurrentClassLoader();
			clazz = loader.loadClass(name);

			return clazz;

		} catch (ClassNotFoundException e)
		{
			try
			{
				clazz = ClassUtil.class.getClassLoader().loadClass(name);

				return clazz;

			} catch (ClassNotFoundException e1)
			{
				try
				{
					clazz = ClassLoader.getSystemClassLoader().loadClass(name);

					return clazz;

				} catch (ClassNotFoundException e2)
				{
					return null;
				}

			}
		}

	}

	/**
	 * Check final modifier.
	 * 
	 * @param modifier
	 *            modifier
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
	 * @param modifier
	 *            modifier
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
	 * @param modifier
	 *            modifier
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
	 * @param clazz
	 *            check methods of it
	 * @return true or false
	 */
	public static boolean hasFinalMethod(Class<?> clazz)
	{
		Asserts.nullCheckForClass(clazz);

		Method[] methods = clazz.getDeclaredMethods();
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
	 * @param modifier
	 *            modifier
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
	 * @param modifier
	 *            modifier
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
	 * @param modifier
	 *            modifier
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
	 * @param modifier
	 *            modifier
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
	 * @param <T>
	 *            parametrized type
	 * @param clazz
	 *            class instance
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

	/**
	 * Gets the class of the given type arguments.
	 * 
	 * <p>
	 * If the given type {@link Type} parameters is an instance of the
	 * {@link ParameterizedType}, it returns the raw type otherwise it return
	 * the casted {@link Class} of the type argument.
	 * </p>
	 * 
	 * @param type
	 *            class or parametrized type
	 * @return
	 */
	public static Class<?> getClass(Type type)
	{
		Asserts.assertNotNull(type, "type parameter can not be null");

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
	 * Gets the declared methods of the given class.
	 * 
	 * @param clazz
	 *            class instance
	 * @return the declared methods
	 */
	public static Method[] getDeclaredMethods(Class<?> clazz)
	{
		Asserts.nullCheckForClass(clazz);
		return clazz.getDeclaredMethods();
	}

	/**
	 * Check that method has any formal arguments.
	 * 
	 * @param method
	 *            method instance
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
	 * @param method
	 *            method instance
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
	 * @param method
	 *            method instance
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
				} else
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
	 * @param method
	 *            method instance
	 * @return trur or false
	 */
	public static boolean isMethodHasException(Method method)
	{
		Asserts.nullCheckForMethod(method);

		Class<?>[] et = method.getExceptionTypes();

		if (et.length == 1)
		{
			if (et[0].equals(Exception.class))
				return true;
		}

		return false;
	}

	/**
	 * Call method on the instance with given arguments.
	 * 
	 * @param method
	 *            method instance
	 * @param instance
	 *            object instance
	 * @param args
	 *            arguments
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

		} catch (Throwable e)
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
		List<String> list = new ArrayList<String>();
		Class<?> clazz = Object.class;

		Method[] methods = clazz.getMethods();
		for (Method method : methods)
		{
			if(!method.getName().equals("toString"))
			{
				list.add(method.getName());	
			}
		}

		return list;
	}

	public static boolean isObjectMethod(String methodName)
	{
		Asserts.assertNotNull(methodName, "methodName parameter can not be null");
		return getObjectMethodNames().contains(methodName);
	}

	public static boolean isMoreThanOneMethodWithName(String methodName, Class<?> clazz)
	{
		Asserts.assertNotNull(methodName, "methodName parameter can not be null");
		Asserts.nullCheckForClass(clazz);

		Method[] methods = clazz.getDeclaredMethods();
		int i = 0;
		for (Method m : methods)
		{
			if (m.getName().equals(methodName))
			{
				i++;
			}
		}

		if (i > 1)
			return true;

		return false;

	}

	public static <T> Constructor<T> isContaintNoArgConstructor(Class<T> clazz)
	{
		Asserts.nullCheckForClass(clazz);
		try
		{
			return clazz.getDeclaredConstructor(new Class<?>[] {});

		} catch (Exception e)
		{
			return null;
		}

	}

	/**
	 * Check the modifiers contains the public keyword.
	 * 
	 * @param modifs
	 *            modifiers
	 * @return true or false
	 */
	public static boolean isPublic(int modifs)
	{
		return Modifier.isPublic(modifs);
	}

	/**
	 * Gets java package if exist.
	 * 
	 * @param packageName
	 *            package name
	 * @return the package with given name
	 */
	public Package getPackage(String packageName)
	{
		Asserts.assertNotNull(packageName, "packageName parameter can not be null");

		return Package.getPackage(packageName);
	}

	public static boolean isParametrizedType(Type type)
	{
		Asserts.assertNotNull(type, "type parameter can not be null");
		if (type instanceof ParameterizedType)
		{
			return true;
		}

		return false;
	}

	public static boolean isConcrete(Class<?> clazz)
	{
		Asserts.nullCheckForClass(clazz);

		Integer modifier = clazz.getModifiers();

		if (!isAbstract(modifier) && !isInterface(modifier) && clazz.getEnclosingClass() == null)
		{
			return true;
		}

		return false;
	}


	public static <T> Constructor<T>[] getConstructors(Class<T> clazz)
	{
		Asserts.nullCheckForClass(clazz);
		return (Constructor<T>[]) clazz.getDeclaredConstructors();
	}

	public static <T> boolean isDefaultConstructorExist(Class<T> clazz)
	{
		Asserts.nullCheckForClass(clazz);
		try
		{
			clazz.getDeclaredConstructor(new Class<?>[] {});

		} catch (SecurityException e)
		{
			throw e;
		} catch (NoSuchMethodException e)
		{
			return false;
		}

		return true;
	}

	public static boolean isAssignable(Type lhs, Type rhs)
	{
		Asserts.assertNotNull(lhs, "lhs parameter can not be null");
		Asserts.assertNotNull(rhs, "rhs parameter can not be null");

		if (lhs instanceof ParameterizedType && rhs instanceof ParameterizedType)
		{
			return isAssignable((ParameterizedType) lhs, (ParameterizedType) rhs);
		} else
		{
			if (lhs instanceof Class && rhs instanceof Class)
			{
				return isAssignable((Class) lhs, (Class) rhs);
			} else
			{
				return false;
			}
		}
	}

	public static boolean isAssignable(Class<?> lhs, Class<?> rhs)
	{
		Asserts.assertNotNull(lhs, "lhs parameter can not be null");
		Asserts.assertNotNull(rhs, "rhs parameter can not be null");

		if (lhs.isAssignableFrom(rhs))
		{
			return true;
		}

		return false;
	}

	public static boolean isAssignableForParametrized(ParameterizedType lhs, ParameterizedType rhs)
	{
		Class<?> rowLhs = (Class<?>) lhs.getRawType();
		Class<?> rowRhs = (Class<?>) rhs.getRawType();

		if (isAssignable(rowLhs, rowRhs))
		{
			Type[] lhsArgs = lhs.getActualTypeArguments();
			Type[] rhsArgs = rhs.getActualTypeArguments();

			if (lhsArgs.equals(rhsArgs))
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

			clazz.getDeclaredField(fieldName);

		} catch (SecurityException e)
		{
			// we must throw here!
			throw new WebBeansException(e);
		} catch (NoSuchFieldException e2)
		{
			return false;
		}

		return true;
	}

	public static boolean classHasMoreThanOneFieldWithName(Class<?> clazz, String fieldName)
	{
		Asserts.nullCheckForClass(clazz);
		Asserts.assertNotNull(fieldName, "fieldName parameter can not be null");

		Field[] fields = clazz.getDeclaredFields();
		boolean ok = false;
		for (Field field : fields)
		{
			if (field.getName().equals(fieldName))
			{
				if (ok)
				{
					return true;
				} else
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

			return clazz.getDeclaredField(fieldName);

		} catch (SecurityException e)
		{
			// we must throw here!
			throw new WebBeansException(e);
		} catch (NoSuchFieldException e2)
		{
			return null;
		}

	}
	
	/**
	 * 
	 * @param clazz webbeans implementation class
	 * @param methodName name of the method that is searched
	 * @param parameterTypes parameter types of the method(it can be subtype of the actual type arguments of the method)
	 * @return the list of method that satisfies the condition
	 */
	public static List<Method> getClassMethodsWithTypes(Class<?> clazz, String methodName, List<Class<?>> parameterTypes)
	{
		Asserts.nullCheckForClass(clazz);
		Asserts.assertNotNull(methodName, "methodName parameter can not be null");
		Asserts.assertNotNull(parameterTypes, "parameterTypes parameter can not be null");

		List<Method> methodList = new ArrayList<Method>();

		Method[] methods = clazz.getDeclaredMethods();

		int j = 0;
		for (Method method : methods)
		{
			Class<?>[] defineTypes = method.getParameterTypes();

			if (defineTypes.length != parameterTypes.size())
			{
				continue;
			}

			boolean ok = false;
			for (Class<?> defineType : defineTypes)
			{
				if (defineType.isAssignableFrom(parameterTypes.get(j)))
				{
					ok = true;
				} else
				{
					ok = false;
				}
				
				j++;
			}

			if (ok)
			{
				methodList.add(method);
			}
		}

		return methodList;
	}

	public static Method getClassMethodWithTypes(Class<?> clazz, String methodName, List<Class<?>> parameterTypes)
	{
		Asserts.nullCheckForClass(clazz);
		Asserts.assertNotNull(methodName, "methodName parameter can not be null");
		Asserts.assertNotNull(parameterTypes, "parameterTypes parameter can not be null");

		Method[] methods = clazz.getDeclaredMethods();

		int j = 0;
		for (Method method : methods)
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
				} else
				{
					ok = false;
				}
			}

			if (ok)
			{
				return method;
			}
		}

		return null;
	}

	public static boolean isMethodExistWithName(Class<?> clazz, String methodName)
	{
		Asserts.nullCheckForClass(clazz);
		Asserts.assertNotNull(methodName, "methodName parameter can not be null");

		Method[] methods = clazz.getDeclaredMethods();

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
		
		return PRIMITIVE_WRAPPERS.contains(clazz);
		
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
			result = PRIMITIVES.contains(clazz);
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
	 * Gets the primitive/wrapper value of the parsed 
	 * {@link String} parameter.
	 * 
	 * @param type primitive or wrapper of the primitive type
	 * @param value value of the type
	 * 
	 * @return the parse of the given {@link String} 
	 * 		   value into the corresponding value, if
	 * 		   any exception occurs, returns null as the 
	 * 		   value.
	 */
	public static Object isValueOkForPrimitiveOrWrapper(Class<?> type, String value)
	{
		try
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

		} catch (Throwable e)
		{
			return null;
		}

		return null;
	}

	public static Enum isValueOkForEnum(Class clazz, String value)
	{
		Asserts.nullCheckForClass(clazz);
		Asserts.assertNotNull(value, "value parameter can not be null");

		try
		{
			return Enum.valueOf(clazz, value);

		} catch (Throwable e)
		{
			return null;
		}

	}

	public static Date isValueOkForDate(String value)
	{
		try
		{
			Asserts.assertNotNull(value, "value parameter can not be null");
			return DateFormat.getDateTimeInstance().parse(value);
		} catch (ParseException e)
		{
			return null;
		}

	}
	
	public static Object isValueOkForBigDecimalOrInteger(Class<?> type, String value)
	{
		Asserts.assertNotNull(type);
		Asserts.assertNotNull(value);
		
		try
		{
			if(type.equals(BigInteger.class))
			{
				return new BigInteger(value);
			}
			else if(type.equals(BigDecimal.class))
			{
				return new BigDecimal(value);
			}
			else
			{
				return new WebBeansException(new IllegalArgumentException("Argument is not valid"));
			}
			
			
		}catch(NumberFormatException e)
		{
			return null;
		}
		
	}

	public static boolean isParametrized(Class<?> clazz)
	{
		Asserts.nullCheckForClass(clazz);
		return (clazz.getTypeParameters().length > 0) ? true : false;
	}

	public static Type[] getActualTypeArguements(Class<?> clazz)
	{
		Asserts.nullCheckForClass(clazz);

		if (clazz.getGenericSuperclass() instanceof ParameterizedType)
		{
			return ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments();

		} else
		{
			return new Type[0];
		}
	}
	
	public static Type[] getActualTypeArguements(Type type)
	{
		Asserts.assertNotNull(type,"type parameter can not be null");

		if (type instanceof ParameterizedType)
		{
			return ((ParameterizedType)type).getActualTypeArguments();

		} else
		{
			return new Type[0];
		}
	}
	
	public static Class<?> getFirstRawType(Type type)
	{
		Asserts.assertNotNull(type,"type argument can not be null");
		
		if(type instanceof ParameterizedType)
		{
			ParameterizedType pt = (ParameterizedType) type;
			return (Class<?>)pt.getRawType();
		}
		
		return (Class<?>)type;
	}
	
	
	public static Set<Class<?>> setTypeHierarchy(Set<Class<?>> set, Class<?> clazz)
	{
		Asserts.nullCheckForClass(clazz);
		
		set.add(clazz);
		
		Class<?> sc = clazz.getSuperclass();
		
		if(sc != null)
		{
			setTypeHierarchy(set,sc);
		}
		
		Class<?>[] interfaces = clazz.getInterfaces();
		for(Class<?> cl : interfaces)
		{
			setTypeHierarchy(set,cl);
		}
		
		return set;
	}
	
	public static Set<Class<?>> setInterfaceTypeHierarchy(Set<Class<?>> set, Class<?> clazz)
	{
		Asserts.nullCheckForClass(clazz);
		
		Class<?>[] interfaces = clazz.getInterfaces();

		for(Class<?> cl : interfaces)
		{
			set.add(cl);
			
			setTypeHierarchy(set,cl);
		}
		
		return set;
	}
	

	public static Type[] getGenericSuperClassTypeArguments(Class<?> clazz)
	{
		Asserts.nullCheckForClass(clazz);
		Type type = clazz.getGenericSuperclass();
		
		if(type != null)
		{
			if(type instanceof ParameterizedType)
			{
				ParameterizedType pt = (ParameterizedType) type;
				
				if(checkParametrizedType(pt))
				{
					return pt.getActualTypeArguments();	
				}
			}
		}
		
		return new Type[0];
		
	}
	
	public static boolean checkParametrizedType(ParameterizedType pType)
	{
		Asserts.assertNotNull(pType,"pType argument can not be null");
		Type[] types = pType.getActualTypeArguments();
		
		for(Type type : types)
		{
			if(type instanceof ParameterizedType)
			{
				return checkParametrizedType((ParameterizedType)type);
			}
			else if((type instanceof TypeVariable) || (type instanceof WildcardType))
			{
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean isFirstParametricTypeArgGeneric(ParameterizedType type)
	{
		Asserts.assertNotNull(type, "type parameter can not be null");
		Type arg = type.getActualTypeArguments()[0];
		
		if((arg instanceof TypeVariable) || (arg instanceof WildcardType))
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
		for(Type type : types)
		{
			if(type instanceof ParameterizedType)
			{
				ParameterizedType pt = (ParameterizedType)type;		
				
				if(checkParametrizedType(pt))
				{
					list.add(pt.getActualTypeArguments());					
				}
			}
		}
		
		return list;
	}
	
	public static Field getFieldWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		Asserts.nullCheckForClass(clazz);
		Asserts.assertNotNull(annotation, "annotation parameter can not be null");
		
		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields)
		{
			if(AnnotationUtil.isAnnotationExist(field.getAnnotations(), annotation))
			{
				return field;
			}
			
		}
		
		return null;
		
	}
	
	public static boolean checkForTypeArguments(Class<?> src, Type[] typeArguments, Class<?> target)
	{
		Asserts.assertNotNull(src,"src parameter can not be null");
		Asserts.assertNotNull(typeArguments,"typeArguments parameter can not be null");
		Asserts.assertNotNull(target,"target parameter can not be null");
		
		Type[] types = getGenericSuperClassTypeArguments(target);		
		
		boolean found = false;
		
		if(Arrays.equals(typeArguments, types))
		{
			return true;
		}
		else
		{
			Class<?> superClazz = target.getSuperclass();
			if(superClazz != null)
			{
				found =  checkForTypeArguments(src, typeArguments, superClazz);	
			}	
		}
		
		if(!found)
		{
			List<Type[]> list = getGenericSuperInterfacesTypeArguments(target);
			if(!list.isEmpty())
			{
				Iterator<Type[]> it = list.iterator();
				while(it.hasNext())
				{
					types = it.next();
					if(Arrays.equals(typeArguments, types))
					{
						found = true;
						break;
					}
				}
				
			}
		}
		
		if(!found)
		{
			Class<?>[] superInterfaces = target.getInterfaces();
			for(Class<?> inter : superInterfaces)
			{
				found = checkForTypeArguments(src, typeArguments, inter);
				if(found)
				{
					break;
				}
			}
		}
		
		
		return found;
	}	
}
