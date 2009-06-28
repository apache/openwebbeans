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
package org.apache.webbeans.inject.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.ejb.EJBUtil;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableConstructor;
import org.apache.webbeans.util.Asserts;

/**
 * Defines the injectable constructor.
 * @param <T> type of the constructor
 */
public class XMLInjectableConstructor<T> extends InjectableConstructor<T>
{
    /**Constructor parameter injection models defined in the xml*/
    private List<XMLInjectionPointModel> injectionPointModelList = new ArrayList<XMLInjectionPointModel>();

    /**
     * Defines new <code>XMLInjectableConstructor</code> instance.
     * @param constructor bean constructor
     * @param owner constructor owner beans
     */
    public XMLInjectableConstructor(Constructor<T> constructor, AbstractComponent<?> owner,CreationalContext<?> creationalContext)
    {
        super(constructor, owner,creationalContext);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.inject.InjectableConstructor#doInjection()
     */
    @Override
    public T doInjection()
    {
        T instance = null;
        List<Object> list = new ArrayList<Object>();

        Iterator<XMLInjectionPointModel> it = this.injectionPointModelList.iterator();
        while (it.hasNext())
        {
            XMLInjectionPointModel model = it.next();
            Annotation[] anns = new Annotation[model.getBindingTypes().size()];
            anns = model.getBindingTypes().toArray(anns);

            //list.add(inject(model.getInjectionGenericType(), anns));
        }

        try
        {
            if (!EJBUtil.isEJBSessionClass(con.getDeclaringClass()))
            {
                if(!con.isAccessible())
                {
                    con.setAccessible(true);
                }
                
                instance = con.newInstance(list.toArray());
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new WebBeansException(e);
        }

        return instance;
    }

    /**
     * @return the constructor
     */
    public Constructor<T> getConstructor()
    {
        return con;
    }
    
    /**
     * Adds new constructor parameter injection model.
     * @param model new injection point model for constructor parameter
     */
    public void addInjectionPointModel(XMLInjectionPointModel model)
    {
        Asserts.assertNotNull(model, "model parameter can not be null");
        this.injectionPointModelList.add(model);
    }
}