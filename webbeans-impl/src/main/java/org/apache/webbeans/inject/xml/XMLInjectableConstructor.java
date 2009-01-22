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

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.ejb.EJBUtil;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableConstructor;
import org.apache.webbeans.util.Asserts;

public class XMLInjectableConstructor<T> extends InjectableConstructor<T>
{
    private List<XMLInjectionPointModel> injectionPointModelList = new ArrayList<XMLInjectionPointModel>();

    public XMLInjectableConstructor(Constructor<T> constructor, AbstractComponent<?> owner)
    {
        super(constructor, owner);
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

            list.add(inject(model.getInjectionClassType(), model.getActualTypeArguments(), anns));
        }

        try
        {
            if (!EJBUtil.isEJBSessionClass(con.getDeclaringClass()))
            {
                instance = con.newInstance(list.toArray());

                // if(getInjectionOwnerComponent().getScopeType().equals(Dependent.class))
                // {
                //					
                // }
                // else
                // {
                // instance = (T)
                // JavassistProxyFactory.createNewProxyInstance(con.getDeclaringClass(),
                // con.getParameterTypes(), list.toArray(),
                // getInjectionOwnerComponent());
                // }
            }

        }
        catch (Throwable e)
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

    public void addInjectionPointModel(XMLInjectionPointModel model)
    {
        Asserts.assertNotNull(model, "model parameter can not be null");
        this.injectionPointModelList.add(model);
    }
}
