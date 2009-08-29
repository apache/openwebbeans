/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
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

import javax.enterprise.inject.Any;
import javax.enterprise.inject.NonBinding;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.stereotype.Stereotype;

import javax.inject.Qualifier;
import javax.interceptor.InterceptorBindingType;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.plugins.OpenWebBeansPlugin;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;

/**
 * Utility class related with {@link Annotation} operations.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class AnnotationUtil
{
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
    public static boolean isMethodHasAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

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
     * Check if a resource annotation exist on the method.
     * 
     * @param method method
     * @return <code>true</code> if any resource annotation exists for the given method
     */
    public static boolean isMethodHasResourceAnnotation(Method method)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        
        Annotation[] anns = method.getDeclaredAnnotations();
        return hasResourceAnnotation(anns);
    }
    
    /**
     * Check given annotation exist in the any parameter of the given method.
     * Return true if exist false otherwise.
     * 
     * @param method method
     * @param annotation checking annotation
     * @return true or false
     */
    public static boolean isMethodParameterAnnotationExist(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

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

    public static Type[] getMethodParameterGenericTypesWithGivenAnnotation(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

        List<Type> list = new ArrayList<Type>();
        Type[] result = null;

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
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

        List<Type> list = new ArrayList<Type>();
        Type[] result = null;

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
     * @param annotation checking annotation
     * @return true or false
     */
    public static boolean isMethodMultipleParameterAnnotationExist(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

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

    /**
     * Gets the method first found parameter type that is annotated with the
     * given annotation.
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

                if (AnnotationUtil.isBindingAnnotation(btype))
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

            index++;

        }
        result = new Annotation[0];
        return result;
    }
    
    /**
     * Gets the method first found parameter annotation with given type.
     * 
     * @param method method
     * @param annotation checking annotation
     * @return annotation
     */
    public static <T extends Annotation> T getMethodFirstParameterAnnotation(Method method, Class<T> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

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

    /**
     * Check given annotation cross ref exist in the any parameter of the given
     * method. Return true if exist false otherwise.
     * 
     * @param method method
     * @param annotation checking annotation
     * @return true or false
     */
    public static boolean isMethodParameterAnnotationCrossRefExist(Method method, Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(method, "Method argument can not be null");
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");

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
     * Returns true if the injection point binding type and {@link NonBinding}
     * member values are equal to the given member annotation.
     * 
     * @param clazz annotation class
     * @param src component binding type annotation
     * @param member annotation for querying the binding type
     * @return true or false
     */
    public static boolean isAnnotationMemberExist(Class<? extends Annotation> clazz, Annotation src, Annotation member)
    {
        Asserts.assertNotNull(clazz, "Clazz argument can not be null");
        Asserts.assertNotNull(src, "Src argument can not be null");
        Asserts.assertNotNull(member, "Member argument can not be null");

        if (!src.annotationType().equals(member.annotationType()))
        {
            return false;
        }

        Method[] methods = clazz.getDeclaredMethods();

        List<String> list = new ArrayList<String>();

        for (Method method : methods)
        {
            Annotation[] annots = method.getDeclaredAnnotations();

            if (annots.length > 0)
            {
                for (Annotation annot : annots)
                {
                    if (!annot.annotationType().equals(NonBinding.class))
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
     * @param arguments annotation member values with {@link NonBinding}
     *            annoations.
     * @return true or false
     */
    private static boolean checkEquality(String src, String member, List<String> arguments)
    {
        if ((checkEquBuffer(src, arguments).toString().trim().equals(checkEquBuffer(member, arguments).toString().trim())))
            return true;
        return false;
    }

    /*
     * Private check method
     */
    private static StringBuffer checkEquBuffer(String src, List<String> arguments)
    {
        int index = src.indexOf('(');

        String sbstr = src.substring(index + 1, src.length() - 1);

        StringBuffer srcBuf = new StringBuffer();

        StringTokenizer tok = new StringTokenizer(sbstr, ",");
        while (tok.hasMoreTokens())
        {
            String token = tok.nextToken();

            StringTokenizer tok2 = new StringTokenizer(token, "=");
            while (tok2.hasMoreElements())
            {
                String tt = tok2.nextToken();
                if (arguments.contains(tt.trim()))
                {
                    srcBuf.append(tt);
                    srcBuf.append("=");

                    if (tok2.hasMoreElements())
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
    public static Annotation[] getBindingAnnotations(Annotation... annotations)
    {
        Asserts.assertNotNull(annotations, "Annotations argument can not be null");

        Set<Annotation> set = new HashSet<Annotation>();

        for (Annotation annot : annotations)
        {
            if (AnnotationUtil.isBindingAnnotation(annot.annotationType()))
            {
                set.add(annot);
            }
        }

        Annotation[] a = new Annotation[set.size()];
        a = set.toArray(a);

        return a;
    }

    /**
     * Gets the array of resource annotations on the given array.
     * 
     * @param annotations annotation array
     * @return array containing resource type anns
     */
    public static Annotation[] getResourceAnnotations(Annotation... annotations)
    {
        Asserts.assertNotNull(annotations, "Annotations argument can not be null");

        Set<Annotation> set = new HashSet<Annotation>();

        for (Annotation annot : annotations)
        {
            if (AnnotationUtil.isResourceAnnotation(annot.annotationType()))
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

        Method[] methods = clazz.getDeclaredMethods();
        List<Method> list = new ArrayList<Method>();
        Method[] rMethod = null;

        for (Method m : methods)
        {
            if (isMethodParameterAnnotationExist(m, annotation))
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

        Method[] methods = clazz.getDeclaredMethods();
        List<Method> list = new ArrayList<Method>();
        Method[] rMethod = null;

        for (Method m : methods)
        {
            if (isMethodHasAnnotation(m, annotation))
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

        if (a != null)
        {
            return true;
        }

        return false;
    }

    public static boolean isMetaAnnotationExist(Annotation[] anns, Class<? extends Annotation> metaAnnotation)
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

    public static boolean isAnnotationExist(Annotation[] anns, Class<? extends Annotation> annotation)
    {
        return getAnnotation(anns, annotation) != null;
    }

    /**
     * get the annotation of the given type from the array. 
     * @param anns
     * @param annotation
     * @return the Annotation with the given type or <code>null</code> if no such found.
     */
    public static Annotation getAnnotation(Annotation[] anns, Class<? extends Annotation> annotation)
    {
        Asserts.assertNotNull(anns, "anns argument can not be null");
        Asserts.assertNotNull(annotation, "annotation argument can not be null");
        for (Annotation annot : anns)
        {
            if (annot.annotationType().equals(annotation))
            {
                return annot;
            }
        }

        return null;
    }
    
    public static Annotation[] getMetaAnnotations(Annotation[] anns, Class<? extends Annotation> metaAnnotation)
    {
        List<Annotation> annots = new ArrayList<Annotation>();
        Annotation[] result = null;
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

    public static Field[] getClazzFieldsWithGivenAnnotation(Class<?> clazz, Class<? extends Annotation> annotation)
    {
        Field[] fields = clazz.getDeclaredFields();
        List<Field> list = new ArrayList<Field>();

        if (fields.length != 0)
        {
            for (Field field : fields)
            {
                if (field.isAnnotationPresent(annotation))
                {
                    list.add(field);
                }
            }
        }

        fields = new Field[list.size()];
        fields = list.toArray(fields);

        return fields;
    }

    public static void checkBindingTypeConditions(Annotation... bindignTypeAnnots)
    {
        Annotation before = null;

        for (Annotation ann : bindignTypeAnnots)
        {
            Method[] methods = ann.annotationType().getDeclaredMethods();

            for (Method method : methods)
            {
                Class<?> clazz = method.getReturnType();
                if (clazz.isArray() || clazz.isAnnotation())
                {
                    if (!AnnotationUtil.isAnnotationExist(method.getDeclaredAnnotations(), NonBinding.class))
                    {
                        throw new WebBeansConfigurationException("@BindingType : " + ann.annotationType().getName() + " must have @NonBinding valued members for its array-valued and annotation valued members");
                    }
                }
            }
            
            
            if (!AnnotationUtil.isBindingAnnotation(ann.annotationType()))
            {
                throw new IllegalArgumentException("Binding annotations must be annotated with @BindingType");
            }

            if (before == null)
            {
                before = ann;
            }
            else
            {
                if (before.equals(ann))
                {
                    throw new IllegalArgumentException("Binding annotations can not contain duplicate binding : @" + before.annotationType().getName());
                }
                else
                {
                    before = ann;
                }
            }
        }
    }

    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link BindingType} false otherwise.
     * 
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link BindingType} false otherwise
     */
    public static boolean isBindingAnnotation(Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(clazz, "clazz parameter can not be null");
        XMLAnnotationTypeManager manager = XMLAnnotationTypeManager.getInstance();
        if (manager.isBindingTypeExist(clazz))
        {
            return true;
        }
        else if (clazz.isAnnotationPresent(Qualifier.class))
        {
            return true;
        }

        return false;
    }
    
    
    /**
     * Returns true if any binding exist
     * 
     * @param bean bean
     * @return true if any binding exist
     */
    public static boolean isAnyBindingExist(Bean<?> bean)
    {
    	Asserts.assertNotNull(bean, "bean parameter can not be null");
    	Set<Annotation> bindings = bean.getBindings();
    	
    	for(Annotation ann : bindings)
    	{
    		if(ann.annotationType().equals(Any.class))
    		{
    			return true;
    		}
    	}
    	
    	return false;
    }

    /**
     * check if any of the given resources is a resource annotation
     * @see AnnotationUtil#isResourceAnnotation(Class)
     */
    public static boolean hasResourceAnnotation(Annotation[] annotations)
    {
        for (Annotation anno : annotations)
        {
            if (AnnotationUtil.isResourceAnnotation(anno.annotationType()))
            {
                return true;
            }
        }        
        
        return false;
    }

    /**
     * Returns true if the annotation is a valid WebBeans Resource,
     * a resource defined in common annotations JSR-250, a remote EJB
     * or a web service.
     * The following annotations indicate resources
     * <ol>
     * <li>&#x0040;CustomerDataservice</li>
     * <li>&#x0040;Resource</li>
     * <li>&#x0040;PersistenceContext</li>
     * <li>&#x0040;PersistenceUnit</li>
     * <li>&#x0040;EJB</li>
     * <li>&#x0040;WebServiceRef</li>
     * <li>&#x0040;</li>
     * </ol>
     * 
     * Please note that &#x0040;PersistenceContext(type=EXTENDED) 
     * is not supported for simple beans.
     * 
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link BindingType} false otherwise
     */
    public static boolean isResourceAnnotation(Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(clazz, "clazz parameter can not be null");

        List<OpenWebBeansPlugin> plugins = PluginLoader.getInstance().getPlugins();
        
        for (OpenWebBeansPlugin plugin : plugins)
        {
            if (plugin.isResourceAnnotation(clazz))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link InterceptorBindingType} false otherwise.
     * 
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link InterceptorBindingType} false otherwise
     */
    public static boolean isInterceptorBindingAnnotation(Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(clazz, "clazz parameter can not be null");
        XMLAnnotationTypeManager manager = XMLAnnotationTypeManager.getInstance();
        if (manager.isInterceptorBindingTypeExist(clazz))
        {
            return true;
        }
        else if (clazz.isAnnotationPresent(InterceptorBindingType.class))
        {
            return true;
        }

        return false;
    }

    /**
     * If candidate class has an interceptor binding annotation type then return
     * true, false otherwise.
     * 
     * @param clazz interceptor candidate class
     * @return true if candidate class has an interceptor binding annotation
     *         type false otherwise
     */
    public static boolean isInterceptorBindingMetaAnnotationExist(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, "anns parameter can not be null");

        for (Annotation ann : anns)
        {
            if (isInterceptorBindingAnnotation(ann.annotationType()))
            {
                return true;
            }
            else
            {
                continue;
            }
        }

        return false;
    }

    public static Annotation[] getInterceptorBindingMetaAnnotations(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, "anns parameter can not be null");
        List<Annotation> interAnns = new ArrayList<Annotation>();

        for (Annotation ann : anns)
        {
            if (isInterceptorBindingAnnotation(ann.annotationType()))
            {
                interAnns.add(ann);
            }
        }

        Annotation[] ret = new Annotation[interAnns.size()];
        ret = interAnns.toArray(ret);

        return ret;
    }

    public static Annotation[] getStereotypeMetaAnnotations(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, "anns parameter can not be null");
        List<Annotation> interAnns = new ArrayList<Annotation>();

        for (Annotation ann : anns)
        {
            if (isStereoTypeAnnotation(ann.annotationType()))
            {
                interAnns.add(ann);
            }
        }

        Annotation[] ret = new Annotation[interAnns.size()];
        ret = interAnns.toArray(ret);

        return ret;
    }

    public static boolean isStereoTypeMetaAnnotationExist(Annotation[] anns)
    {
        Asserts.assertNotNull(anns, "anns parameter can not be null");

        for (Annotation ann : anns)
        {
            if (isStereoTypeAnnotation(ann.annotationType()))
            {
                return true;
            }
            else
            {
                continue;
            }
        }

        return false;
    }

    /**
     * Returns true if the annotation is defined in xml or annotated with
     * {@link Stereotype} false otherwise.
     * 
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link Stereotype} false otherwise
     */
    public static boolean isStereoTypeAnnotation(Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(clazz, "clazz parameter can not be null");
        XMLAnnotationTypeManager manager = XMLAnnotationTypeManager.getInstance();
        if (manager.isStereoTypeExist(clazz))
        {
            return true;
        }
        else if (clazz.isAnnotationPresent(Stereotype.class))
        {
            return true;
        }

        return false;
    }
    
    /**
     * If the bean extends generic class via {@link Realizes}
     * annotation, realized based producer methods, fields and observer
     * methods binding type is 
     * 
     * <ul>
     *  <li>Binding types on the definitions</li>
     *  <li>Plus class binding types</li>
     *  <li>Minus generic class binding types</li>
     * </ul>
     * 
     * @param clazz realized definition class
     * @param anns binding annotations array
     */
    public static Annotation[] getRealizesGenericAnnotations(Class<?> clazz, Annotation[] anns)
    {
       Set<Annotation> setAnnots = new HashSet<Annotation>();
        
        for(Annotation definedAnn : anns)
        {
            setAnnots.add(definedAnn);
        }
        
        Annotation[] genericReliazesAnns = AnnotationUtil.getBindingAnnotations(clazz.getSuperclass().getDeclaredAnnotations());
        
        for(Annotation generic : genericReliazesAnns)
        {
            setAnnots.remove(generic);
        }
        
        genericReliazesAnns = AnnotationUtil.getBindingAnnotations(clazz.getDeclaredAnnotations());

        for(Annotation generic : genericReliazesAnns)
        {
            setAnnots.add(generic);
        }            
        
        Annotation[] annots = new Annotation[setAnnots.size()];
        annots = setAnnots.toArray(annots);
        
        return annots;
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
