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
package org.apache.webbeans.ejb.util;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;

import org.apache.webbeans.ejb.component.EjbComponentImpl;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;

/**
 * @version $Rev$ $Date$
 */
public final class EjbDefinitionUtility
{
    private EjbDefinitionUtility()
    {
        
    }

    public static void defineApiType(EjbComponentImpl<?> ejbComponent)
    {
        Class<?> ejbClass = ejbComponent.getReturnType();        
        
        Type[] businessInterfacesDefined = ejbClass.getGenericInterfaces();
        
        List<Type> businessInterfacesList = new ArrayList<Type>();
        
        for(Type businessInterfacesDefinedInArray : businessInterfacesDefined)
        {
            if(!(businessInterfacesDefinedInArray.equals(Serializable.class) || businessInterfacesDefinedInArray.equals(Externalizable.class)))
            {
                businessInterfacesList.add(businessInterfacesDefinedInArray);   
            }
        }
        
        businessInterfacesDefined = new Type[businessInterfacesList.size()];
        businessInterfacesDefined = businessInterfacesList.toArray(businessInterfacesDefined);
        
        if(businessInterfacesDefined.length == 0)
        {            
            Annotation localAnnotation = AnnotationUtil.getAnnotation(ejbClass.getDeclaredAnnotations(), Local.class);
                         
            if(localAnnotation != null)
            {
                Local local = (Local)localAnnotation;
                Class<?>[] localInterfaces = local.value();
                
                for(Class<?> localInterface : localInterfaces)
                {
                    if(!ClassUtil.isParametrized(localInterface))
                    {
                        ClassUtil.setTypeHierarchy(ejbComponent.getTypes(), localInterface);
                        
                        EjbUtility.configureEjbBusinessMethods(ejbComponent, localInterface);
                    }
                }
            }
            else
            {
                defineLocalClassType(ejbComponent);
            }
            
        }
        else if(businessInterfacesDefined.length == 1)
        {
            Type businessInterface = businessInterfacesDefined[0];
            defineLocalBusinessInterfaceType(ejbComponent, businessInterface);
        }
        else
        {
            for(Type busType : businessInterfacesDefined)
            {
                defineLocalBusinessInterfaceType(ejbComponent, busType);
            }            
        }
        
        ejbComponent.getTypes().add(Object.class);
    }
    
    private static void defineLocalBusinessInterfaceType(EjbComponentImpl<?> ejbComponent, Type type)
    {
        Class<?> businessInterfaceClass = EjbClassUtility.getLocalInterfaceClass(type);
        
        if(businessInterfaceClass != null)
        {
            if(!AnnotationUtil.isAnnotationExistOnClass(businessInterfaceClass, Remote.class))
            {                    
                ClassUtil.setTypeHierarchy(ejbComponent.getTypes(), businessInterfaceClass);
                
                EjbUtility.configureEjbBusinessMethods(ejbComponent, businessInterfaceClass);
            }
        }                                
    }
    
    private static void defineLocalClassType(EjbComponentImpl<?> ejbComponent)
    {
        Class<?> clazz = ejbComponent.getReturnType();
        
        if(!ClassUtil.isParametrized(clazz))
        {
            ClassUtil.setClassTypeHierarchy(ejbComponent.getTypes(), clazz);
            
            EjbUtility.configureEjbBusinessMethods(ejbComponent, clazz);
        }
        
    }
}
