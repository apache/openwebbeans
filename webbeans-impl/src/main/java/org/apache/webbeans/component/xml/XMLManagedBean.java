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
package org.apache.webbeans.component.xml;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.xml.XMLInjectableConstructor;
import org.apache.webbeans.inject.xml.XMLInjectableField;
import org.apache.webbeans.inject.xml.XMLInjectableMethods;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.Asserts;

public class XMLManagedBean<T> extends ManagedBean<T>
{
    /**Logger instance*/
    private static WebBeansLogger logger = WebBeansLogger.getLogger(XMLManagedBean.class);

    /** Constructor injection point decleration */
    private XMLInjectableConstructor<T> injectableConstructor = null;

    /** Injection points for fields */
    private Map<Field, XMLInjectionPointModel> injectableFields = new HashMap<Field, XMLInjectionPointModel>();

    /** Injection points for initializer methods */
    private Map<Method, List<XMLInjectionPointModel>> injectableMethods = new HashMap<Method, List<XMLInjectionPointModel>>();

    /** Initial field values of the webbean defined in the XML */
    private Map<Field, Object> fieldValues = new HashMap<Field, Object>();

    /**
     * Creates new XML defined webbeans component.
     * 
     * @param returnType type of the webbeans component
     */
    public XMLManagedBean(Class<T> returnType)
    {
        super(returnType);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.component.ComponentImpl#createInstance()
     */
    @Override
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        T instance = null;

        if (this.injectableConstructor == null)
        {
            instance = super.createInstance(creationalContext);
        }
        else
        {
            instance = this.injectableConstructor.doInjection();
            super.afterConstructor(instance,creationalContext);
        }

        /* Inject initial field values */
        if (instance != null)
        {
            injectFieldValues(instance);
        }

        return instance;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.component.ComponentImpl#injectFields(java.lang.Object
     * )
     */
    @Override
    public void injectFields(T instance,CreationalContext<T> creationalContext)
    {
        Set<Field> fieldSet = this.injectableFields.keySet();
        Iterator<Field> itField = fieldSet.iterator();

        while (itField.hasNext())
        {
            Field field = itField.next();
            XMLInjectionPointModel model = this.injectableFields.get(field);
            XMLInjectableField injectableField = new XMLInjectableField(field, instance, this, model,creationalContext);

            injectableField.doInjection();
        }
    }

    protected void injectFieldValues(T instance)
    {
        Set<Field> fieldSet = this.fieldValues.keySet();
        Iterator<Field> itField = fieldSet.iterator();

        while (itField.hasNext())
        {
            Field field = itField.next();
            if (!field.isAccessible())
            {
                field.setAccessible(true);
            }

            try
            {
                field.set(instance, this.fieldValues.get(field));

            }
            catch (IllegalArgumentException e)
            {
                logger.error("IllegalArgumentException is occured while calling the field : " + field.getName() + " on class " + instance.getClass().getName());
                throw new WebBeansException(e);

            }
            catch (IllegalAccessException e)
            {
                logger.error("IllegalAccessException is occured while calling the field : " + field.getName() + " on class " + instance.getClass().getName());
                throw new WebBeansException(e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.component.ComponentImpl#injectMethods(java.lang.Object
     * )
     */
    @Override
    public void injectMethods(T instance,CreationalContext<T> creationalContext)
    {
        Set<Method> methodSet = this.injectableMethods.keySet();
        Iterator<Method> itMethods = methodSet.iterator();
        while (itMethods.hasNext())
        {
            Method method = itMethods.next();
            List<XMLInjectionPointModel> listInjectionPointModel = this.injectableMethods.get(method);
            XMLInjectableMethods<T> injectableMethod = new XMLInjectableMethods<T>(method, instance, this, listInjectionPointModel,creationalContext);

            injectableMethod.doInjection();
        }

    }

    /**
     * Sets injection point for constructor.
     * 
     * @param constructor constructor injection point
     */
    public void setInjectableConstructor(XMLInjectableConstructor<T> constructor)
    {
        Asserts.assertNotNull(constructor, "constructor parameter can not be null");
        this.injectableConstructor = constructor;
    }

    /**
     * Adds new field injection point
     * 
     * @param field field injection point
     * @param model injection point model
     */
    public void addFieldInjectionPoint(Field field, XMLInjectionPointModel model)
    {
        Asserts.assertNotNull(field, "field parameter can not be null");
        Asserts.assertNotNull(model, "model parameter can not be null");

        this.injectableFields.put(field, model);
    }

    /**
     * Adds new method injection point
     * 
     * @param method method injection point
     * @param model injection point model
     */
    public void addMethodInjectionPoint(Method method, XMLInjectionPointModel model)
    {
        Asserts.assertNotNull(method, "method parameter can not be null");

        List<XMLInjectionPointModel> listModel = this.injectableMethods.get(method);
        if (listModel == null)
        {
            listModel = new ArrayList<XMLInjectionPointModel>();
            this.injectableMethods.put(method, listModel);
        }

        if (model != null)
        {
            listModel.add(model);
        }
    }

    /**
     * Add new field value.
     * 
     * @param name name of the field
     * @param value value of the field
     */
    public void addFieldValue(Field name, Object value)
    {
        fieldValues.put(name, value);
    }

    /**
     * @return the fieldValues
     */
    public Map<Field, Object> getFieldValues()
    {
        return fieldValues;
    }
}