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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.ProducerComponentImpl;
import org.apache.webbeans.inject.xml.XMLInjectableMethods;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;
import org.apache.webbeans.util.Asserts;

public class XMLProducerComponentImpl<T> extends ProducerComponentImpl<T>
{
    private List<XMLInjectionPointModel> producerMethodParameters = new ArrayList<XMLInjectionPointModel>();

    private List<XMLInjectionPointModel> disposalMethodParameters = new ArrayList<XMLInjectionPointModel>();

    private Type[] actualTypeArguments = new Type[0];
    
    private CreationalContext<?> creationalContext;

    public XMLProducerComponentImpl(AbstractComponent<?> parent, Class<T> returnType)
    {
        super(parent, returnType);
        this.parent = parent;
    }

    public void addProducerMethodInjectionPointModel(XMLInjectionPointModel model)
    {
        Asserts.assertNotNull(model, "model parameter can not be null");
        this.producerMethodParameters.add(model);
    }

    public void addDisposalMethodInjectionPointModel(XMLInjectionPointModel model)
    {
        Asserts.assertNotNull(model, "model parameter can not be null");
        this.disposalMethodParameters.add(model);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.component.ProducerComponentImpl#getActualTypeArguments
     * ()
     */
    @Override
    public Type[] getActualTypeArguments()
    {
        return this.actualTypeArguments;
    }

    public void setActualTypeArguments(Type[] actualTypeArguments)
    {
        this.actualTypeArguments = actualTypeArguments;
    }

    protected void destroyInstance(T instance)
    {
        if (disposalMethod != null)
        {

            Object object = getParentInstance();

            XMLInjectableMethods<T> methods = new XMLInjectableMethods<T>(creatorMethod, object, this, this.disposalMethodParameters,this.creationalContext);
            methods.doInjection();

        }
    }

    protected T createInstance(CreationalContext<T> creationalContext)
    {
        this.creationalContext = creationalContext;
        
        T instance = null;
        Object parentInstance = getParentInstance();

        try
        {
            XMLInjectableMethods<T> methods = new XMLInjectableMethods<T>(creatorMethod, parentInstance, this, this.producerMethodParameters,creationalContext);
            instance = methods.doInjection();
        }
        finally
        {
            if (getParent().getScopeType().equals(Dependent.class))
            {
                destroyBean(getParent(), parentInstance);
            }
        }

        checkNullInstance(instance);
        checkScopeType();

        return instance;
    }

}
