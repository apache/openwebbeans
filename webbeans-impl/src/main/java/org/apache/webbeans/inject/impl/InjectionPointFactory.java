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
package org.apache.webbeans.inject.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.event.Fires;
import javax.event.Observes;
import javax.inject.Obtains;
import javax.inject.manager.Bean;
import javax.inject.manager.InjectionPoint;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.inject.xml.XMLInjectionModelType;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

public class InjectionPointFactory
{
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
            injectionPoint = getGenericInjectionPoint(owner, anns, xmlInjectionModel.getInjectionGenericType(), xmlInjectionModel.getInjectionMember());
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
            return getGenericInjectionPoint(owner, annots, member.getGenericType(), member);   
        }        
        else
        {
            return null;
        }

    }
    
    private static boolean checkFieldApplicable(Annotation[] anns)
    {
        if(AnnotationUtil.isAnnotationExist(anns, Fires.class) || AnnotationUtil.isAnnotationExist(anns, Obtains.class))
        {
            return true;
        }
     
        return false;
    }

    private static InjectionPoint getGenericInjectionPoint(Bean<?> owner, Annotation[] annots, Type type, Member member)
    {
        InjectionPointImpl injectionPoint = null;

        Annotation[] bindingAnnots = AnnotationUtil.getBindingAnnotations(annots);
        injectionPoint = new InjectionPointImpl(owner, type, member);

        addAnnotation(injectionPoint, annots, false);
        addAnnotation(injectionPoint, bindingAnnots, true);

        return injectionPoint;

    }

    public static List<InjectionPoint> getMethodInjectionPointData(Bean<?> owner, Method member)
    {
        Asserts.assertNotNull(owner, "owner parameter can not be null");
        Asserts.assertNotNull(member, "member parameter can not be null");

        List<InjectionPoint> lists = new ArrayList<InjectionPoint>();

        Type[] types = member.getGenericParameterTypes();
        Annotation[][] annots = member.getParameterAnnotations();

        if (types.length > 0)
        {
            int i = 0;

            for (Type type : types)
            {
                Annotation[] annot = annots[i];

                if (annot.length == 0)
                {
                    annot = new Annotation[1];
                    annot[0] = new CurrentLiteral();
                }
                
                if (!checkMethodApplicable(annot))
                {
                    lists.add(getGenericInjectionPoint(owner, annot, type, member));
                }

                i++;
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

    public static InjectionPoint getPartialInjectionPoint(Bean<?> owner,Type type, Annotation...bindings)
    {
        InjectionPointImpl impl = new InjectionPointImpl(owner,type,null);
        
        for(Annotation annot : bindings)
        {
            impl.addBindingAnnotation(annot);
        }
        
        return impl;
        
    }
    
    public static List<InjectionPoint> getConstructorInjectionPointData(Bean<?> owner, Constructor<?> member)
    {
        Asserts.assertNotNull(owner, "owner parameter can not be null");
        Asserts.assertNotNull(member, "member parameter can not be null");

        List<InjectionPoint> lists = new ArrayList<InjectionPoint>();

        Type[] types = member.getGenericParameterTypes();
        Annotation[][] annots = member.getParameterAnnotations();

        if (types.length > 0)
        {
            int i = 0;

            for (Type type : types)
            {
                Annotation[] annot = annots[i];

                if (annot.length == 0)
                {
                    annot = new Annotation[1];
                    annot[0] = new CurrentLiteral();
                }

                lists.add(getGenericInjectionPoint(owner, annot, type, member));

                i++;
            }
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
            else
            {
                impl.addAnnotation(ann);
            }
        }
    }

}
