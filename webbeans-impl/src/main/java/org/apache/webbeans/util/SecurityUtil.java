package org.apache.webbeans.util;

import java.util.Set;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javassist.util.proxy.ProxyFactory;
 
public class SecurityUtil 
{		
	
		private final static int METHOD_CLASS_GETDECLAREDCONSTRUCTOR = 0x01; 
	
		private final static int METHOD_CLASS_GETDECLAREDCONSTRUCTORS = 0x02; 

		private final static int METHOD_CLASS_GETDECLAREDMETHOD = 0x03; 

		private final static int METHOD_CLASS_GETDECLAREDMETHODS = 0x04; 
		
		private final static int METHOD_CLASS_GETDECLAREDFIELD = 0x05;

		private final static int METHOD_CLASS_GETDECLAREDFIELDS = 0x06;

		@SuppressWarnings("unchecked")
		public static <T> Constructor<T> doPrivilegedGetDeclaredConstructor(Class<T> clazz, Class<?>... parameterTypes) throws NoSuchMethodException
		{
			ClassLoader ld = SecurityUtil.class.getClassLoader();
			AccessControlContext acc = AccessController.getContext();
			Object obj = AccessController.doPrivileged(
					new PrivilegedActionForClass(clazz, parameterTypes, METHOD_CLASS_GETDECLAREDCONSTRUCTOR));
			if (obj instanceof NoSuchMethodException) throw (NoSuchMethodException)obj;
			return (Constructor<T>)obj;
		}
		
		@SuppressWarnings("unchecked")
		public static <T> Constructor<T>[] doPrivilegedGetDeclaredConstructors(Class<T> clazz)
		{
			Object obj = AccessController.doPrivileged(
					new PrivilegedActionForClass(clazz, null, METHOD_CLASS_GETDECLAREDCONSTRUCTORS));
			return (Constructor<T>[])obj;
		}
		
		public static <T> Method doPrivilegedGetDeclaredMethod(Class<T> clazz, String name, Class<?>... parameterTypes)  throws NoSuchMethodException
		{
			Object obj = AccessController.doPrivileged(
					new PrivilegedActionForClass(clazz, new Object[] {name, parameterTypes}, METHOD_CLASS_GETDECLAREDMETHOD));
			if (obj instanceof NoSuchMethodException) throw (NoSuchMethodException)obj;
			return (Method)obj;			
		}

		public static <T> Method[] doPrivilegedGetDeclaredMethods(Class<T> clazz) 
		{
			Object obj = AccessController.doPrivileged(
					new PrivilegedActionForClass(clazz, null, METHOD_CLASS_GETDECLAREDMETHODS));
			return (Method[])obj;			
		}
		
		public static <T> Field doPrivilegedGetDeclaredField(Class<T> clazz, String name) throws NoSuchFieldException 
		{
			Object obj = AccessController.doPrivileged(
					new PrivilegedActionForClass(clazz, name, METHOD_CLASS_GETDECLAREDFIELD));
			if (obj instanceof NoSuchFieldException) throw (NoSuchFieldException)obj;
			return (Field)obj;
		}
		
		public static <T> Field[] doPrivilegedGetDeclaredFields(Class<T> clazz) 
		{
			Object obj = AccessController.doPrivileged(
					new PrivilegedActionForClass(clazz, null, METHOD_CLASS_GETDECLAREDFIELDS));
			return (Field[])obj;
		}
		
		protected static class PrivilegedActionForClass implements PrivilegedAction<Object> 
		{
			Class<?> clazz;
			
			Object parameters;
			
			int method;
			
			protected PrivilegedActionForClass(Class<?> clazz, Object parameters, int method) 
			{
				this.clazz = clazz;
				this.parameters = parameters;
				this.method = method;
			}
			
			public Object run()
			{
				try 
				{
					switch (method) 
					{
						case METHOD_CLASS_GETDECLAREDCONSTRUCTOR:
							return clazz.getDeclaredConstructor((Class<?>[])parameters);
						case METHOD_CLASS_GETDECLAREDCONSTRUCTORS:
							return clazz.getDeclaredConstructors();
						case METHOD_CLASS_GETDECLAREDMETHOD:
							String name = (String)((Object[])parameters)[0];
							Class<?>[] realParameters = (Class<?>[])((Object[])parameters)[1];
							return clazz.getDeclaredMethod(name, realParameters);
						case METHOD_CLASS_GETDECLAREDMETHODS:
							return clazz.getDeclaredMethods();
						case METHOD_CLASS_GETDECLAREDFIELD:
							return clazz.getDeclaredField((String)parameters);
						case METHOD_CLASS_GETDECLAREDFIELDS:
							return clazz.getDeclaredFields();
					}
				} 
				catch (Exception exception) 
				{
					return exception;
				}
				return null;
			}			
			
		}
		
		public static Object doPrivilegedSetAccessible(AccessibleObject obj, boolean flag) 
		{
			AccessController.doPrivileged(new PrivilegedActionForAccessibleObject(obj, flag));
			return null;
		};
		
		protected static class PrivilegedActionForAccessibleObject implements PrivilegedAction<Object> 
		{
			
			AccessibleObject object;
			
			boolean flag;
			
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
		
		
		public static Class<?> doPrivilegedCreateClass(ProxyFactory factory) 
		{
			Class<?> ret = (Class<?>)AccessController.doPrivileged(new PrivilegedActionForProxyFactory(factory));
			return ret;
		}
		
		protected static class PrivilegedActionForProxyFactory implements PrivilegedAction<Object>
		{
			ProxyFactory factory;
			
			protected PrivilegedActionForProxyFactory(ProxyFactory factory) 
			{
				this.factory = factory;
			}
			
			public Object run() 
			{
				return factory.createClass();
			}
		}
		
/*		
		public static <T> Set<T> doPrivilegedSetAdd(Set<T> set, T item)
		{
			AccessController.doPrivileged(new PrivilegedActionForSet<T>(set, item));
			return null;
		}
		
		protected static class PrivilegedActionForSet<T> implements PrivilegedAction<T>
		{
			Set<T> set;
			
			T item;
			
			protected PrivilegedActionForSet(Set<T> set, T item) 
			{
				this.set = set;
				this.item = item;
			}
			
			public T run() 
			{
				 set.add(item);
				 return null;
			}
		}
 		
		public static String doPrivilegedToString(java.lang.annotation.Annotation a) 
		{
			return (String)AccessController.doPrivileged(new PrivilegedActionForAnnotation(a));
		}
		
		protected static class PrivilegedActionForAnnotation implements PrivilegedAction<Object>
		{
			java.lang.annotation.Annotation annotation;
			
			protected PrivilegedActionForAnnotation(java.lang.annotation.Annotation annotation)
			{
				this.annotation = annotation;
			}
			
			public Object run() 
			{
				return annotation.toString();
			}
		}
*/	
		
}
