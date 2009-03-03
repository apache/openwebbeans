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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.context.CreationalContext;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.inject.InjectableMethods;

public class XMLInjectableMethods<T> extends InjectableMethods<T>
{
    private List<XMLInjectionPointModel> injectionPointModelList = new ArrayList<XMLInjectionPointModel>();

    public XMLInjectableMethods(Method m, Object instance, AbstractComponent<?> owner, List<XMLInjectionPointModel> injectionPointModelList,CreationalContext<?> creationalContext)
    {
        super(m, instance, owner,creationalContext);
        this.injectionPointModelList = injectionPointModelList;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.inject.InjectableMethods#doInjection()
     */
    @Override
    @SuppressWarnings("unchecked")
    public T doInjection()
    {
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
            if (!m.isAccessible())
            {
                m.setAccessible(true);
            }

            return (T) m.invoke(instance, list.toArray());

        }
        catch (Exception e)
        {
            // no-op
            e.printStackTrace();
        }
        return null;
    }
}