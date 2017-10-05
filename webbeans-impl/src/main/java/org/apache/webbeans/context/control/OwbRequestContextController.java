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

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.context.spi.Context;

public class OwbRequestContextController implements RequestContextController
{
    private final WebBeansContext context;
    private Object startParam = null;

    OwbRequestContextController(WebBeansContext context)
    {
        this.context = context;
    }

    @Override
    public boolean activate()
    {
        if (startParam != null)
        {
            return false;
        }
        Context ctx = context.getContextsService().getCurrentContext(RequestScoped.class);
        if (ctx == null)
        {
            startParam = new Object();
            context.getContextsService().startContext(RequestScoped.class, startParam);
            return true;
        }
        return false;
    }

    @Override
    public void deactivate() throws ContextNotActiveException
    {
        if (startParam != null)
        {
            context.getContextsService().endContext(RequestScoped.class, startParam);
            startParam = null;
        }
    }
}
