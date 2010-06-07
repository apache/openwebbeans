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
package org.apache.webbeans.inject.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

import org.apache.webbeans.annotation.NamedLiteral;
import org.apache.webbeans.inject.xml.XMLInjectionModelType;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

public class InjectionPointFactory
{
    /**
     * 
     * @param owner
     * @param xmlInjectionModel
     * @return
     * @deprecated
     */
    public static InjectionPoint getXMLInjectionPointData(Bean<?> owner, XMLInjectionPointModel xmlInjectionModel)
    {
        Asserts.assertNotNull(owner, "owner parameter can not be null");
        Asserts.assertNotNull(xmlInjectionModel, "xmlInjectionModel parameter can not be null");
        
        InjectionPoint injectionPoint = null;
        
        Set<Annotation> setAnns = xmlInjectionModel.getAnnotations();
        Annotation[] anns = new Annotation[setAnns.size()];
        anns = setAnns.toArray(anns);
        
        boolean available = true;
        
        if(xmlInjectionModel.getType().equals(XMLInjectionModelType.FIELD))
        {
            if(checkFieldApplicable(anns))
            {
               available = false; 
            }
        }
        else if(xmlInjectionModel.getType().equals(XMLInjectionModelType.METHOD))
        {
            if(checkMethodApplicable(anns))
            {
                available = false;
            }
        }
        
        if(available)
        {
            injectionPoint = getGenericInjectionPoint(owner, anns, xmlInjectionModel.getInjectionGenericType(), xmlInjectionModel.getInjectionMember(),null);
        }
        
        return injectionPoint;
    }

    public static InjectionPoint getFieldInjectionPointData(Bean<?> owner, Field member)
    {
        Asserts.assertNotNull(owner, "owner parameter can not be null");
        Asserts.assertNotNull(member, "member parameter can not be null");

        Annotation[] annots = null;
        annots = member.getAnnotations();
        
        if(!checkFieldApplicable(annots))
        {
            AnnotatedType<?> annotated = AnnotatedElementFactory.newAnnotatedType(member.getDeclaringClass());
            return getGenericInjectionPoint(owner, annots, member.getGenericType(), member, 
                    AnnotatedElementFactory.newAnnotatedField(member, annotated));   
        }        
        else
        {
            return null;
        }

    }
    
    public static <X> InjectionPoint getFieldInjectionPointData(Bean<?> owner, AnnotatedField<X> annotField)
    {
        Asserts.assertNotNull(owner, "owner parameter can not be null");
        Asserts.assertNotNull(annotField, "annotField parameter can not be null");
        Field member = annotField.getJavaMember();

        Annotation[] annots = AnnotationUtil.getAnnotationsFromSet(annotField.getAnnotations());
        
        if(!checkFieldApplicable(annots))
        {
            return getGenericInjectionPoint(owner, annots, annotField.getBaseType(), member, annotField);   
        }        
        else
        {
            return null;
        }

    }    
    
    private static boolean checkFieldApplicable(Annotation[] anns)
    {
//        if(AnnotationUtil.isAnnotationExist(anns, Fires.class) || AnnotationUtil.isAnnotationExist(anns, Obtains.class))
//        {
//            return true;
//        }
     
        return false;
    }

    /**
     * Gets injected point instance.
     * @param owner owner of the injection point
     * @param annots annotations of the injection point
     * @param type type of the injection point
     * @param member member of the injection point
     * @param annotated annotated instance of injection point
     * @return injection point instance
     */
    private static InjectionPoint getGenericInjectionPoint(Bean<?> owner, Annotation[] annots, Type type, Member member,Annotated annotated)
    {
        InjectionPointImpl injectionPoint = null;

        Annotation[] qualifierAnnots = AnnotationUtil.getQualifierAnnotations(annots);
        
        //@Named update for injection fields!
        if(member instanceof Field)
        {
            for(int i=0;i<qualifierAnnots.length;i++)
            {
                Annotation qualifier = qualifierAnnots[i];
                if(qualifier.annotationType().equals(Named.class))
                {
                    Named named = (Named)qualifier;
                    String value = named.value();
                    
                    if(value == null || value.equals(""))
                    {
                        NamedLiteral namedLiteral = new NamedLiteral();
                        namedLiteral.setValue(member.getName());
                        qualifierAnnots[i] = namedLiteral;
                    }
                    
                    break;
                }
            }            
        }    
        
        
        injectionPoint = new InjectionPointImpl(owner, type, member, annotated);
        
        if(AnnotationUtil.hasAnnotation(annots, Delegate.class))
        {
            injectionPoint.setDelegate(true);
        }
        
        if(Modifier.isTransient(member.getModifiers()))
        {
            injectionPoint.setTransient(true);
        }

        addAnnotation(injectionPoint, qualifierAnnots, true);

        return injectionPoint;

    }

    @SuppressWarnings("unchecked")
    public static List<InjectionPoint> getMethodInjectionPointData(Bean<?> owner, Method member)
    {
        Asserts.assertNotNull(owner, "owner parameter can not be null");
        Asserts.assertNotNull(member, "member parameter can not be null");

        List<InjectionPoint> lists = new ArrayList<InjectionPoint>();
        
        AnnotatedType<?> annotated = AnnotatedElementFactory.newAnnotatedType(member.getDeclaringClass());
        AnnotatedMethod method = AnnotatedElementFactory.newAnnotatedMethod(member, annotated);
        List<AnnotatedParameter<?>> parameters = method.getParameters();
        
        InjectionPoint point = null;
        
        for(AnnotatedParameter<?> parameter : parameters)
        {
            //@Observes is not injection point type for method parameters
            if(parameter.getAnnotation(Observes.class) == null)
            {
                point = getGenericInjectionPoint(owner, parameter.getAnnotations().toArray(new Annotation[parameter.getAnnotations().size()]), parameter.getBaseType(), member , parameter);
                lists.add(point);                
            }  
        }
        
        return lists;
    }
    
    public static <X> List<InjectionPoint> getMethodInjectionPointData(Bean<?> owner, AnnotatedMethod<X> method)
    {
        Asserts.assertNotNull(owner, "owner parameter can not be null");
        Asserts.assertNotNull(method, "method parameter can not be null");

        List<InjectionPoint> lists = new ArrayList<InjectionPoint>();

        List<AnnotatedParameter<X>> parameters = method.getParameters();
        
        InjectionPoint point = null;
        
        for(AnnotatedParameter<?> parameter : parameters)
        {
            //@Observes is not injection point type for method parameters
            if(parameter.getAnnotation(Observes.class) == null)
            {
                point = getGenericInjectionPoint(owner, parameter.getAnnotations().toArray(new Annotation[parameter.getAnnotations().size()]), parameter.getBaseType(), method.getJavaMember() , parameter);
                lists.add(point);                
            }  
        }
        
        return lists;
    }
    
    
    
    private static boolean checkMethodApplicable(Annotation[] annot)
    {
        for (Annotation observersAnnot : annot)
        {
            if (observersAnnot.annotationType().equals(Observes.class))
            {
                return true;
            }
        }
        
        return false;
        
    }

    public static InjectionPoint getPartialInjectionPoint(Bean<?> owner,Type type, Member member, Annotated annotated, Annotation...bindings)
    {
        InjectionPointImpl impl = new InjectionPointImpl(owner,type,member,annotated);
        
        
        for(Annotation annot : bindings)
        {
            impl.addBindingAnnotation(annot);
        }
        
        return impl;
        
    }
    
    public static <T> List<InjectionPoint> getConstructorInjectionPointData(Bean<T> owner, AnnotatedConstructor<T> constructor)
    {
        Asserts.assertNotNull(owner, "owner parameter can not be null");
        Asserts.assertNotNull(constructor, "constructor parameter can not be null");

        List<InjectionPoint> lists = new ArrayList<InjectionPoint>();

        List<AnnotatedParameter<T>> parameters = constructor.getParameters();
        
        InjectionPoint point = null;
        
        for(AnnotatedParameter<?> parameter : parameters)
        {
            point = getGenericInjectionPoint(owner, parameter.getAnnotations().toArray(new Annotation[parameter.getAnnotations().size()]), parameter.getBaseType(), constructor.getJavaMember() , parameter);
            lists.add(point);
        }
        
        return lists;
    }
    
    
    @SuppressWarnings("unchecked")
    public static List<InjectionPoint> getConstructorInjectionPointData(Bean<?> owner, Constructor<?> member)
    {
        Asserts.assertNotNull(owner, "owner parameter can not be null");
        Asserts.assertNotNull(member, "member parameter can not be null");

        List<InjectionPoint> lists = new ArrayList<InjectionPoint>();

        AnnotatedType<Object> annotated = (AnnotatedType<Object>)AnnotatedElementFactory.newAnnotatedType(member.getDeclaringClass());
        AnnotatedConstructor constructor = AnnotatedElementFactory.newAnnotatedConstructor((Constructor<Object>)member,annotated);
        List<AnnotatedParameter<?>> parameters = constructor.getParameters();
        
        InjectionPoint point = null;
        
        for(AnnotatedParameter<?> parameter : parameters)
        {
            point = getGenericInjectionPoint(owner, parameter.getAnnotations().toArray(new Annotation[parameter.getAnnotations().size()]), parameter.getBaseType(), member , parameter);
            lists.add(point);
        }
        
        return lists;
    }

    private static void addAnnotation(InjectionPointImpl impl, Annotation[] annots, boolean isBinding)
    {
        for (Annotation ann : annots)
        {
            if (isBinding)
            {
                impl.addBindingAnnotation(ann);
            }
        }
    }    

}
