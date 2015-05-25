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
package org.apache.webbeans.intercept;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.util.HashMap;


/**
 * <p>This is a {@link javax.inject.Provider} especially
 * made for &#064;SessionScoped beans used in web applications.</p>
 * 
 * <p>Since there is only one single contextual instance of an &#064;SessionScoped bean per thread,
 * we can simply cache this instance inside our bean. We only need to reload this instance
 * if it is null or if the thread ends.</p>
 */
public class SessionScopedBeanInterceptorHandler extends NormalScopedBeanInterceptorHandler
{
    /**default serial id*/
    private static final long serialVersionUID = 1L;

    /**
     * Cached bean instance for each thread
     */
    private static ThreadLocal<HashMap<Bean<?>, Object>> cachedInstances = new ThreadLocal<HashMap<Bean<?>, Object>>();


    public static void removeThreadLocals()
    {
        cachedInstances.set(null);
        cachedInstances.remove();
    }

    /**
     * Creates a new handler.
     */
    public SessionScopedBeanInterceptorHandler(BeanManager beanManager, Bean<?> bean)
    {
        super(beanManager, bean);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getContextualInstance()
    {
        HashMap<Bean<?>, Object> beanMap = cachedInstances.get();
        if (beanMap == null)
        {
            beanMap = new HashMap<Bean<?>, Object>();
            cachedInstances.set(beanMap);
        }

        Object cachedInstance = beanMap.get(bean);
        if (cachedInstance == null)
        {

            cachedInstance = super.getContextualInstance();
            beanMap.put(bean, cachedInstance);
        }

        return cachedInstance;
    }

}
