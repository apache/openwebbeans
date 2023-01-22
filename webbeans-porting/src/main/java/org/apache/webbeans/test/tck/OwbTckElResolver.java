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
package org.apache.webbeans.test.tck;

import java.util.Set;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.el22.WebBeansELResolver;

/**
 * Exist for TCK standalone EL resolver.
 * <p>
 * In standalone case, there is no JSF 
 * related artifacts, therefore {@link WebBeansELResolver}
 * does not work.
 * </p>
 */
public class OwbTckElResolver extends WebBeansELResolver
{
    /**
     * {@inheritDoc}
     */    
    @Override
    @SuppressWarnings("unchecked")
    public Object getValue(ELContext context, Object obj, Object property) throws NullPointerException, ELException
    {
        //Bean instance
        Object contextualInstance = null;
        ELContextStore elContextStore = null;
        if (obj == null)
        {
            //Name of the bean
            String name = (String) property;
            //Local store, create if not exist
            elContextStore = ELContextStore.getInstance(true);

            contextualInstance = elContextStore.findBeanByName(name);

            if(contextualInstance != null)
            {
                context.setPropertyResolved(true);
                
                return contextualInstance;
            }

            //Manager instance
            BeanManagerImpl manager = WebBeansContext.getInstance().getBeanManagerImpl();

            //Get beans
            Set<Bean<?>> beans = manager.getBeans(name);

            //Found?
            if(beans != null && !beans.isEmpty())
            {
                //Managed bean
                Bean<Object> bean = (Bean<Object>)beans.iterator().next();

                if(bean.getScope().equals(Dependent.class))
                {
                    contextualInstance = getDependentContextualInstance(manager, elContextStore, context, bean);
                }
                else
                {
                    // now we check for NormalScoped beans
                    contextualInstance = getNormalScopedContextualInstance(manager, elContextStore, context, bean, name);
                }
            }
        }
        
        return contextualInstance;
    }

}
