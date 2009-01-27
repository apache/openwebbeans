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
package org.apache.webbeans.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.context.Dependent;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.inject.manager.Bean;
import javax.inject.manager.Manager;

import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.DependentContext;

public class WebBeansELResolver extends ELResolver
{    

    @Override
    public Class<?> getCommonPropertyType(ELContext arg0, Object arg1)
    {
        return null;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext arg0, Object arg1)
    {
        return null;
    }

    @Override
    public Class<?> getType(ELContext arg0, Object arg1, Object arg2) throws NullPointerException, PropertyNotFoundException, ELException
    {
        return null;
    }

    @Override
    public Object getValue(ELContext context, Object obj, Object property) throws NullPointerException, PropertyNotFoundException, ELException
    {
        Manager manager = ManagerImpl.getManager();

        Object object = null;
        DependentContext dependentContext = null;
        Bean<?> bean = null;
        boolean isActiveSet = false;
        boolean isResolution = false;
        try
        {
            if (obj == null)
            {
                isResolution = true;

                dependentContext = (DependentContext) ManagerImpl.getManager().getContext(Dependent.class);

                if (!dependentContext.isActive())
                {
                    dependentContext.setActive(true);
                    isActiveSet = true;
                }

                String name = (String) property;
                object = manager.getInstanceByName(name);

                context.setPropertyResolved(true);
                bean = manager.resolveByName(name).iterator().next();

            }

        }
        finally
        {
            if (isResolution)
            {
                if (bean != null)
                {
                    destroyBean(bean, object);
                }

                if (isActiveSet)
                {
                    dependentContext.setActive(false);
                }
            }
        }

        return object;
    }

    @SuppressWarnings("unchecked")
    private <T> void destroyBean(Bean<?> bean, Object instance)
    {
        Bean<T> destroy = (Bean<T>) bean;

        if (destroy.getScopeType().equals(Dependent.class))
        {
            T inst = (T) instance;

            destroy.destroy(inst);

        }

    }

    @Override
    public boolean isReadOnly(ELContext arg0, Object arg1, Object arg2) throws NullPointerException, PropertyNotFoundException, ELException
    {
        return false;
    }

    @Override
    public void setValue(ELContext arg0, Object arg1, Object arg2, Object arg3) throws NullPointerException, PropertyNotFoundException, PropertyNotWritableException, ELException
    {

    }

}
