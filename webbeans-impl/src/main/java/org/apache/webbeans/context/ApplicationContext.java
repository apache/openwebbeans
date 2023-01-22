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
package org.apache.webbeans.context;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.Contextual;

import org.apache.webbeans.component.BuiltInOwbBean;

/**
 * Application context implementation.
 * 
 */
public class ApplicationContext extends AbstractContext
{
    private static final long serialVersionUID = -8254441824647652312L;

    /**
     * used to prevent us firing the Destroyed event twice.
     * We cannot just use setActive(false) as this would trash &#064;BeforeShutdown
     */
    private boolean destroyed;

    public ApplicationContext()
    {
        super(ApplicationScoped.class);
    }

    @Override
    public void setComponentInstanceMap()
    {
        componentInstanceMap = new ConcurrentHashMap<>();
    }


    /**
     * By default a Context destroys all it's Contextual Instances.
     * But for the ApplicationContext we only need to destroy custom beans and _not_ Extensions and internal beans
     */
    @Override
    public void destroy()
    {
        Set<Contextual<?>> keySet = new HashSet<>(componentInstanceMap.keySet());
        for (Contextual<?> contextual: keySet)
        {
            if (contextual instanceof BuiltInOwbBean)
            {
                // we do NOT yet dextroy our internal beans as we probably need them for BeforeShutdown still.
                continue;
            }

            destroyInstance(contextual);
        }

        destroyed = true;
    }

    /**
     * This method should only get called at container shutdown.
     * It will destroy the Extensions as well
     */
    public void destroySystemBeans()
    {
        super.destroy();
        setActive(false);
    }

    /**
     * @return @{code true} if custom beans already got destroyed
     */
    public boolean isDestroyed()
    {
        return destroyed;
    }
}
