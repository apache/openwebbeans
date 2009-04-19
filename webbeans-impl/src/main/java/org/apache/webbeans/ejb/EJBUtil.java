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
package org.apache.webbeans.ejb;

import javax.ejb.MessageDriven;
import javax.ejb.Stateful;
import javax.ejb.Stateless;

import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

/**
 * Utility classes related with the EJB based web beans components.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class EJBUtil
{
    /*
     * Private constructor
     */
    private EJBUtil()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Check the given class is an EJB sesion class or not.
     * <p>
     * EJB class means, it is annotated with {@link Stateless} or
     * {@link Stateful} annotations.
     * </p>
     * 
     * @param clazz class instance
     * @return true or false
     */
    public static boolean isEJBSessionClass(Class<?> clazz)
    {
        return (AnnotationUtil.isAnnotationExistOnClass(clazz, Stateless.class) || AnnotationUtil.isAnnotationExistOnClass(clazz, Stateful.class));
    }
    
    public static boolean isEJBSessionStatefulClass(Class<?> clazz)
    {
        return (AnnotationUtil.isAnnotationExistOnClass(clazz, Stateful.class));
    }
    
    //TODO EJB 3.1
    public static boolean isEJBSingletonClass(Class<?> clazz)
    {
        return false;
    }
    
    
    public static boolean isEJBSessionStateless(Class<?> clazz)
    {
        return (AnnotationUtil.isAnnotationExistOnClass(clazz, Stateless.class));
    }    
    
    /**
     * Check the given class is an EJB is MDB class or not.
     * <p>
     * EJB class means, it is annotated with {@link MessageDriven} annotations.
     * </p>
     * 
     * @param clazz class instance
     * @return true or false
     */
    public static boolean isEJBMessageDrivenClass(Class<?> clazz)
    {
        return (AnnotationUtil.isAnnotationExistOnClass(clazz, MessageDriven.class));
    }

    /**
     * True if class is an ejb.
     * 
     * @param clazz class check for ejb
     * @return true if ejb
     */
    public static boolean isEJBClass(Class<?> clazz)
    {
        return (isEJBSessionClass(clazz) || isEJBMessageDrivenClass(clazz)) ? true : false;
    }

    /**
     * Return true if it is defined in the ejb-jar.xml false otherwise.
     * 
     * @param clazzName class name
     * @return true if it is defined in the ejb-jar.xml
     */
    public static boolean isDefinedInXML(String clazzName)
    {
        Asserts.assertNotNull(clazzName, "clazzName parameter can not be null");

        return false;
    }

}
