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


/**
 * <p>This is a {@link org.apache.webbeans.proxy.MethodHandler} especially
 * made for &#064;ApplicationScoped beans.</p>
 * 
 * <p>Since there is only one single contextual instance of an &#064;ApplicationScoped bean,
 * we can simply cache this instance inside our bean. We only need to reload this instance
 * if it is null. This happens at the first usage and after the MethodHandler got deserialized</p>
 *
 * TODO: move caching to new InterceptorHandler logic
 */
public class ApplicationScopedBeanInterceptorHandler extends NormalScopedBeanInterceptorHandler
{
    /**default serial id*/
    private static final long serialVersionUID = 1L;

    /**
     * Cached bean instance. Please note that it is only allowed to
     * use this special proxy if you don't use OpenWebBeans in an EAR
     * scenario. In this case we must not cache &#064;ApplicationScoped
     * contextual instances because they could be injected into EJBs or other
     * shared instances which span over multiple web-apps.
     */
    private transient Object cachedInstance = null;


    public ApplicationScopedBeanInterceptorHandler(BeanManager beanManager, Bean<?> bean)
    {
        super(beanManager, bean);
    }

    /**
     * {@inheritDoc}
     */
    protected Object getContextualInstance()
    {
        if (cachedInstance == null) 
        {
            cachedInstance = super.getContextualInstance();
        }
        
        return cachedInstance;
    }

}
