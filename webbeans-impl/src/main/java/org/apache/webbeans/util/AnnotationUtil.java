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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.webbeans.BindingType;
import javax.webbeans.DuplicateBindingTypeException;
import javax.webbeans.NonBinding;

import org.apache.webbeans.xml.XMLAnnotationTypeManager;



/**
 * Utility class related with {@link Annotation} operations.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class AnnotationUtil
{
	//No instantiate
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
	public static boolean isMethodHasAnnotation(Method method, Class<? extends Annotation> clazz)
	{
		Asserts.assertNotNull(method, "Method argument can not be null");
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		
		Annotation[] anns = method.getDeclaredAnnotations();
		for(Annotation annotation : anns)
		{
			if(annotation.annotationType().equals(clazz))
			{
				return true;
			}
		}
		
		return false;
		
	}
	
	
	/**
	 * Check given annotation exist in the any parameter of the given method.
	 * Return true if exist false otherwise.
	 * @param method method 
	 * @param annotation checking annotation
	 * @return true or false
	 */
	public static boolean isMethodParameterAnnotationExist(Method method, Class<? extends Annotation> clazz)
	{
		Asserts.assertNotNull(method, "Method argument can not be null");
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		
		Annotation[][] parameterAnns = method.getParameterAnnotations();		
		
		for(Annotation[] parameters : parameterAnns)
		{
			for(Annotation param : parameters)
			{
				Class<? extends Annotation> btype = param.annotationType();
				if(btype.equals(clazz))
				{
					return true;
				}
			}
			
		}
		return false;
	}
	
	public static Type[] getMethodParameterGenericTypesWithGivenAnnotation(Method method, Class<? extends Annotation> clazz)
	{
		Asserts.assertNotNull(method, "Method argument can not be null");
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		
		List<Type> list = new ArrayList<Type>();
		Type[] result = null;
		
		Annotation[][] parameterAnns = method.getParameterAnnotations();		
		Type[] genericTypes = method.getGenericParameterTypes();
		
		int i = 0;
		for(Annotation[] parameters : parameterAnns)
		{
			for(Annotation param : parameters)
			{
				Class<? extends Annotation> btype = param.annotationType();
				if(btype.equals(clazz))
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
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		
		List<Type> list = new ArrayList<Type>();
		Type[] result = null;
		
		Annotation[][] parameterAnns = constructor.getParameterAnnotations();		
		Type[] genericTypes = constructor.getGenericParameterTypes();
		
		int i = 0;
		for(Annotation[] parameters : parameterAnns)
		{
			for(Annotation param : parameters)
			{
				Class<? extends Annotation> btype = param.annotationType();
				if(btype.equals(clazz))
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
	 * Check given annotation exist in the multiple parameter of the given method.
	 * Return true if exist false otherwise.
	 * 
	 * @param method method 
	 * @param annotation checking annotation
	 * @return true or false
	 */
	public static boolean isMethodMultipleParameterAnnotationExist(Method method, Class<? extends Annotation> clazz)
	{
		Asserts.assertNotNull(method, "Method argument can not be null");
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		
		Annotation[][] parameterAnns = method.getParameterAnnotations();		
		
		boolean found = false;
		
		for(Annotation[] parameters : parameterAnns)
		{
			for(Annotation param : parameters)
			{
				
				if(param.annotationType().equals(clazz))
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
			
		}
		return false;
	}
	
	/**
	 * Gets the method first found parameter type that is annotated 
	 * with the given annotation.
	 *
	 * @param method method 
	 * @param annotation checking annotation
	 * @return type
	 */
	public static Type getMethodFirstParameterWithAnnotation(Method method, Class<? extends Annotation> clazz)
	{
		Asserts.assertNotNull(method, "Method argument can not be null");
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		
		Annotation[][] parameterAnns = method.getParameterAnnotations();
		Type[] params = method.getGenericParameterTypes();
		
		int index = 0;
		for(Annotation[] parameters : parameterAnns)
		{
			for(Annotation param : parameters)
			{
				Class<? extends Annotation> btype = param.annotationType();
				if(btype.equals(clazz))
				{
					return params[index];
				}
			}
			
			index++;
			
		}
		return null;
	}
	
	public static Class<?> getMethodFirstParameterTypeClazzWithAnnotation(Method method, Class<? extends Annotation> clazz)
	{
		Type type = getMethodFirstParameterWithAnnotation(method, clazz);
		
		if(type instanceof ParameterizedType)
		{
			return (Class<?>)((ParameterizedType)type).getRawType();
		}
		else
		{
			return (Class<?>)type;
		}
	}
	
	/**
	 * Gets the method first found parameter binding types.
	 *
	 * @param method method 
	 * @param annotation checking annotation
	 * @return annotation array
	 */
	public static Annotation[] getMethodFirstParameterBindingTypesWithGivenAnnotation(Method method, Class<? extends Annotation> clazz)
	{
		Asserts.assertNotNull(method, "Method argument can not be null");
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		
		Annotation[][] parameterAnns = method.getParameterAnnotations();
		List<Annotation> list = new ArrayList<Annotation>();
		Annotation[] result = null;
		
		int index = 0;
		for(Annotation[] parameters : parameterAnns)
		{
			boolean found = false;
			for(Annotation param : parameters)
			{
				Class<? extends Annotation> btype = param.annotationType();
				if(btype.equals(clazz))
				{
					found = true;
					continue;
				}
				
				if(btype.isAnnotationPresent(BindingType.class))
				{
					list.add(param);
				}
				
			}
			
			if(found)
			{
				result = new Annotation[list.size()];
				result = list.toArray(result);
				return result;
			}
			
			index++;
			
		}
		result = new Annotation[0];
		return result;
	}
	
	
	
	
	/**
	 * Check given annotation cross ref exist in the any parameter of the given method.
	 * Return true if exist false otherwise.
	 * @param method method 
	 * @param annotation checking annotation
	 * @return true or false
	 */
	public static boolean isMethodParameterAnnotationCrossRefExist(Method method, Class<? extends Annotation> clazz)
	{
		Asserts.assertNotNull(method, "Method argument can not be null");
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		
		Annotation[][] parameterAnns = method.getParameterAnnotations();		
		
		for(Annotation[] parameters : parameterAnns)
		{
			for(Annotation param : parameters)
			{
				Annotation[] btype = param.annotationType().getAnnotations();
				
				for(Annotation b : btype)
				{
					if(b.annotationType().equals(clazz))
					{
						return true;
					}
				}
			}
			
		}
		return false;
	}
	
	/**
	 * Returns true if the injection point binding type and {@link NonBinding} member values are
	 * equal to the given member annotation.
	 * 
	 * @param clazz annotation class
	 * @param src component binding type annotation
	 * @param member annotation for querying the binding type
	 * @return true or false
	 */
	public static boolean isAnnotationMemberExist(Class<? extends Annotation> clazz,Annotation src, Annotation member)
	{
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		Asserts.assertNotNull(src, "Src argument can not be null");
		Asserts.assertNotNull(member, "Member argument can not be null");
		
		if(!src.annotationType().equals(member.annotationType()))
		{
			return false;
		}
		
		Method[] methods =  clazz.getDeclaredMethods();
		
		List<String> list = new ArrayList<String>();
		
		for(Method method : methods)
		{
			Annotation[] annots = method.getDeclaredAnnotations();
			
			if(annots.length > 0)
			{
				for(Annotation annot : annots)
				{
					if(!annot.annotationType().equals(NonBinding.class))
					{
						list.add(method.getName());
					}
				}
				
			}
			else
			{
				list.add(method.getName());
			}
		}
		
		return checkEquality(src.toString(), member.toString(), list);
	
	}
	
	/**
	 * Check that given two annotation values are equal or not.
	 * 
	 * @param src annotation toString method
	 * @param member annotation toString method
	 * @param arguments annotation member values with {@link NonBinding} annoations.
	 * @return true or false
	 */
	private static boolean checkEquality(String src, String member,List<String> arguments)
	{
		if((checkEquBuffer(src, arguments).toString().equals(checkEquBuffer(member, arguments).toString())))
				return true;
		return false;
	}
	
	/*
	 * Private check method
	 */
	private static StringBuffer checkEquBuffer(String src, List<String> arguments)
	{
		int index = src.indexOf('(');
		
		String sbstr = src.substring(index+1,src.length()-1);
		
		StringBuffer srcBuf = new StringBuffer();

		StringTokenizer tok = new StringTokenizer(sbstr,",");
		while(tok.hasMoreTokens())
		{
			String token = tok.nextToken();
			
			StringTokenizer tok2 = new StringTokenizer(token,"=");
			while(tok2.hasMoreElements())
			{
				String tt = tok2.nextToken();
				if(arguments.contains(tt.trim()))
				{
					srcBuf.append(tt);
					srcBuf.append("=");
					
					if(tok2.hasMoreElements())
						srcBuf.append(tok2.nextToken());
				}
			}
			
		}
	
		return srcBuf;
	}
	
	/**
	 * Gets the array of binding annotations on the given array.
	 * 
	 * @param annotations annotation array
	 * @return array containing binding type anns
	 */
	public static Annotation[] getBindingAnnotations(Annotation...annotations)
	{
		Asserts.assertNotNull(annotations, "Annotations argument can not be null");
		
		Set<Annotation> set = new HashSet<Annotation>();
		
		for(Annotation annot : annotations)
		{
			if(annot.annotationType().isAnnotationPresent(BindingType.class))
			{
				set.add(annot);
			}
		}
		
		Annotation[] a = new Annotation[set.size()];
		a = set.toArray(a);
		
		return a;
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
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		Asserts.assertNotNull(annotation, "Annotation argument can not be null");
		
		Method[] methods =  clazz.getDeclaredMethods();
		List<Method> list = new ArrayList<Method>();
		Method[] rMethod = null;
		
		for(Method m : methods)
		{
			if(isMethodParameterAnnotationExist(m, annotation))
			{
				list.add(m);
			}
		}
		
		rMethod = new Method[list.size()];
		rMethod = list.toArray(rMethod);
		
		return rMethod;
	}
	
	/**
	 * Gets array of methods that has given annotation type.
	 * 
	 * @param clazz class for check
	 * @param annotation for check
	 * @return array of methods
	 */
	public static Method[] getMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		Asserts.assertNotNull(annotation, "Annotation argument can not be null");
		
		Method[] methods =  clazz.getDeclaredMethods();
		List<Method> list = new ArrayList<Method>();
		Method[] rMethod = null;
		
		for(Method m : methods)
		{
			if(isMethodHasAnnotation(m, annotation))
			{
				list.add(m);
			}
		}
		
		rMethod = new Method[list.size()];
		rMethod = list.toArray(rMethod);
		
		return rMethod;
	}
	
	
	/**
	 * Check whether or not class contains the given annotation.
	 * 
	 * @param clazz class instance
	 * @param annotation annotation class
	 * @return return true or false
	 */
	public static boolean isAnnotationExistOnClass(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		Asserts.assertNotNull(annotation, "Annotation argument can not be null");
		
		Annotation a = clazz.getAnnotation(annotation);
		
		if(a != null)
		{
			return true;
		}
		
		return false;
	}
	

	public static boolean isMetaAnnotationExist(Annotation[] anns, Class<? extends Annotation> metaAnnotation)
	{
		Asserts.assertNotNull(anns, "Anns argument can not be null");
		Asserts.assertNotNull(metaAnnotation, "MetaAnnotation argument can not be null");
		
		for(Annotation annot : anns)
		{
			if(annot.annotationType().isAnnotationPresent(metaAnnotation))
			{
				return true;
			}
		}
		
		return false;
		
	}
	
	public static boolean isAnnotationExist(Annotation[] anns, Class<? extends Annotation> annotation)
	{
		Asserts.assertNotNull(anns, "anns argument can not be null");
		Asserts.assertNotNull(annotation, "annotation argument can not be null");
		
		for(Annotation annot : anns)
		{
			if(annot.annotationType().equals(annotation))
			{
				return true;
			}
		}
		
		return false;
		
	}
	
	

	public static Annotation[] getMetaAnnotations(Annotation[] anns, Class<? extends Annotation> metaAnnotation)
	{
		List<Annotation> annots = new ArrayList<Annotation>();
		Annotation[] result = null;
		Asserts.assertNotNull(anns, "Anns argument can not be null");
		Asserts.assertNotNull(metaAnnotation, "MetaAnnotation argument can not be null");

		for(Annotation annot : anns)
		{
			if(annot.annotationType().isAnnotationPresent(metaAnnotation))
			{
				annots.add(annot);
			}
		}
		
		result = new Annotation[annots.size()];
		result = annots.toArray(result);
		
		return result;
	}
	
	public static Field[] getClazzFieldsWithGivenAnnotation(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		Field[] fields = clazz.getDeclaredFields();
		List<Field> list = new ArrayList<Field>();
		
		if(fields.length != 0)
		{
			for(Field field : fields)
			{
				if(field.isAnnotationPresent(annotation))
				{
					list.add(field);
				}
			}
		}
		
		fields = new Field[list.size()];
		fields = list.toArray(fields);
		
		return fields;
	}
	
	public static void checkBindingTypeConditions(Annotation...bindignTypeAnnots)
	{
		Annotation before = null;
		
		for(Annotation ann : bindignTypeAnnots)
		{
			if(!ann.annotationType().isAnnotationPresent(BindingType.class))
			{
				throw new IllegalArgumentException("Binding annotations must be annotated with @BindingType");
			}
			
			if(before == null)
			{
				before = ann;
			}
			else
			{
				if(before.equals(ann))
				{
					throw new DuplicateBindingTypeException("Binding annotations can not contain duplicate binding : @" + before.annotationType().getName());
				}
				else
				{
					before = ann;
				}
			}
		}
	}
	
	public static boolean isBindingAnnotation(Class<? extends Annotation> clazz)
	{
		Asserts.assertNotNull(clazz, "clazz parameter can not be null");
		if(clazz.isAnnotationPresent(BindingType.class))
		{
			return true;
		}
		else
		{
			XMLAnnotationTypeManager manager = XMLAnnotationTypeManager.getInstance();
			if(manager.isBindingTypeExist(clazz))
			{
				return true;
			}
		}
		
		return false;
	}
	
}
