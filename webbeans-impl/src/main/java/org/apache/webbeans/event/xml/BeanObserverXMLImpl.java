/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.event.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;

public class BeanObserverXMLImpl<T> extends ObserverMethodImpl<T>
{
    private List<XMLInjectionPointModel> observersParameters = new ArrayList<XMLInjectionPointModel>();

    public BeanObserverXMLImpl(InjectionTargetBean<?> bean, Method observerMethod, boolean ifExist, 
                               Annotation[] observedQualifiers, Type observedType)
    {
        super(bean, observerMethod, ifExist, observedQualifiers, observedType);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.event.BeanObserverImpl#getMethodArguments(java.lang
     * .Object)
     */
    @Override
    protected List<Object> getMethodArguments(Object event)
    {
        List<Object> params = new ArrayList<Object>();
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        for (XMLInjectionPointModel model : observersParameters)
        {
            Set<Annotation> setBindingTypes = model.getBindingTypes();
            Annotation[] anns = new Annotation[setBindingTypes.size()];
            anns = setBindingTypes.toArray(anns);
            params.add(manager.getInstance(InjectionResolver.getInstance().implResolveByType(model.getInjectionGenericType(), anns).iterator().next(),null));
        }

        return params;
    }

    public void addXMLInjectionObservesParameter(XMLInjectionPointModel model)
    {
        this.observersParameters.add(model);
    }

}
