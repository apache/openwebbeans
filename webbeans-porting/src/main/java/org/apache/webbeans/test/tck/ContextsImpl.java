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

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.AbstractContext;
import org.apache.webbeans.context.RequestContext;
import org.jboss.cdi.tck.spi.Contexts;

public class ContextsImpl implements Contexts<AbstractContext>
{

    @Override
    public AbstractContext getRequestContext()
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();

        RequestContext currentContext = (RequestContext) webBeansContext.getContextsService().getCurrentContext(RequestScoped.class);

        return currentContext;
    }

    @Override
    public void setActive(AbstractContext context)
    {
        context.setActive(true);
        
    }

    @Override
    public void setInactive(AbstractContext context)
    {
        context.setActive(false);
    }

    @Override
    public AbstractContext getDependentContext()
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();

        return (AbstractContext) webBeansContext.getBeanManagerImpl().getContext(Dependent.class);
    }

    @Override
    public void destroyContext(AbstractContext context)
    {
        context.destroy();
    }

}
