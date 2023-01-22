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
package org.apache.webbeans.corespi.se;

import jakarta.enterprise.context.spi.Context;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.SingletonContext;


public class DefaultContextsService extends BaseSeContextsService
{
    private static ThreadLocal<SingletonContext> singletonContext;

    static
    {
        singletonContext = new ThreadLocal<>();
    }

    public DefaultContextsService(final WebBeansContext webBeansContext)
    {
        super(webBeansContext);
    }

    @Override
    protected void destroyGlobalContexts()
    {
        SingletonContext singletonCtx = singletonContext.get();
        if (singletonCtx != null)
        {
            singletonCtx.destroy();
            singletonContext.set(null);
            singletonContext.remove();
        }
        super.destroyGlobalContexts();
    }

    @Override
    protected Context getCurrentSingletonContext()
    {        
        return singletonContext.get();
    }

    @Override
    protected void createSingletonContext()
    {
        final SingletonContext ctx = new SingletonContext();
        ctx.setActive(true);
        
        singletonContext.set(ctx);
    }

    @Override
    protected void destroySingletonContext()
    {
        if(singletonContext.get() != null)
        {
            singletonContext.get().destroy();   
        }

        singletonContext.set(null);
        singletonContext.remove();
    }
}
