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
package org.apache.webbeans.el;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

public class ELContextStore
{
    private ELContext elContext;
    
    private Map<Bean<?>, CreationalStore> dependentObjects = new HashMap<Bean<?>, CreationalStore>();
    
    private static class CreationalStore
    {
        private Object object;
        
        private CreationalContext<?> creational;
        
        public CreationalStore(Object object, CreationalContext<?> creational)
        {
            this.object = object;
            this.creational = creational;
        }

        /**
         * @return the object
         */
        public Object getObject()
        {
            return object;
        }

        /**
         * @return the creational
         */
        public CreationalContext<?> getCreational()
        {
            return creational;
        }
        
        
    }
    
    public ELContextStore(ELContext context)
    {
        this.elContext = context;
    }

    public void addDependent(Bean<?> bean, Object dependent, CreationalContext<?> creationalContext)
    {
        this.dependentObjects.put(bean, new CreationalStore(dependent,creationalContext));
    }
    
    public Object getDependent(Bean<?> bean)
    {
        if(this.dependentObjects.containsKey(bean))
        {
            return this.dependentObjects.get(bean).getObject();
        }
        
        return null;
    }
    
    public boolean isExist(Bean<?> bean)
    {
        return this.dependentObjects.containsKey(bean);
    }
    
    @SuppressWarnings("unchecked")
    public void destroy()
    {
        Set<Bean<?>> beans = this.dependentObjects.keySet();
        for(Bean<?> bean : beans)
        {
            Bean<Object> o = (Bean<Object>)bean;
            CreationalStore store = this.dependentObjects.get(bean);
            o.destroy(o, (CreationalContext<Object>)store.getCreational());
        }
        
        this.dependentObjects.clear();
        this.elContext = null;
    }
    
    public ELContext getELContext()
    {
        return this.elContext;
    }
}
