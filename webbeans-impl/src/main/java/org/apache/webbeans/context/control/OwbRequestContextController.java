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

package org.apache.webbeans.context.control;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.intercept.RequestScopedBeanInterceptorHandler;
import org.apache.webbeans.spi.ContextsService;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.context.spi.Context;

public class OwbRequestContextController implements RequestContextController
{
    private final ContextsService contextsService;

    OwbRequestContextController(WebBeansContext context)
    {
        this.contextsService = context.getContextsService();
    }

    @Override
    public boolean activate()
    {
        final Context ctx = contextsService.getCurrentContext(RequestScoped.class, false);
        if (ctx == null || !ctx.isActive())
        {
            contextsService.startContext(RequestScoped.class, null);
            return true;
        }
        return false;
    }

    @Override
    public void deactivate() throws ContextNotActiveException
    {
        contextsService.endContext(RequestScoped.class, null);
        RequestScopedBeanInterceptorHandler.removeThreadLocals();
    }
}
