/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.el10;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.el.ELContextStore;

public class EL10Resolver extends ELResolver
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
    @SuppressWarnings("unchecked")
    public Object getValue(ELContext context, Object obj, Object property) throws NullPointerException, PropertyNotFoundException, ELException
    {
        //Manager instance
        BeanManagerImpl manager = BeanManagerImpl.getManager();
        
        //Bean instance
        Object object = null;
        
        //Managed bean
        Bean<Object> bean = null;
        
        //Creational context for creating instance
        CreationalContext<Object> creationalContext = null;
        
        //Local store, set by the OwbELContextListener
        ELContextStore store = ELContextStore.LOCAL_CONTEXT.get();        
        if (obj == null)
        {                      
            //Name of the bean
            String name = (String) property;
            //Get beans
            Set<Bean<?>> beans = manager.getBeans(name);
            
            //Found?
            if(beans != null && !beans.isEmpty())
            {
                bean = (Bean<Object>)beans.iterator().next();
                creationalContext = manager.createCreationalContext(bean);                    
                //Already registered in store
                if(bean.getScope().equals(Dependent.class))
                {
                    object = store.getDependent(bean);
                }                    
            }
            
            //If no object found on the store
            if(object == null)
            {
                //Getting object
                object = manager.getInstanceByName(name,creationalContext);                
                if (object != null)
                {                    
                    context.setPropertyResolved(true);   
                    //Adding into store
                    store.addDependent(bean, object, creationalContext);
                }                    
            }
            //Object found on the store
            else
            {
                context.setPropertyResolved(true);                    
            }
            
        }

        return object;
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
