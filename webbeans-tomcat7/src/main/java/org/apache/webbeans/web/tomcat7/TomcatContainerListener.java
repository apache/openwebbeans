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
package org.apache.webbeans.web.tomcat7;

import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.SessionContext;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.web.context.SessionContextManager;
import org.apache.webbeans.web.context.WebContextsService;

/**
 * Container listener that propagates the change of a session id (e. g. during an authentication through the container)
 * to the session context manager. Otherwise a new session context for the same session is created, because the session
 * id is used to identify the session context. <br/> Adapted from OpenEJB tomee/tomee-catalina and updated.
 *
 * @version $Rev$ $Date$
 */
public class TomcatContainerListener implements ContainerListener
{

    @Override
    public void containerEvent(ContainerEvent containerEvent)
    {
        if (Context.CHANGE_SESSION_ID_EVENT.equals(containerEvent.getType())
                && (containerEvent.getData() instanceof String[]))
        {
            String[] ids = (String[]) containerEvent.getData();

            if (ids.length >= 2)
            {
                WebBeansContext webBeansContext = WebBeansContext.currentInstance();
                ContextsService contextsService = webBeansContext.getContextsService();

                if (contextsService instanceof WebContextsService)
                {
                    WebContextsService webContextsService = (WebContextsService) contextsService;
                    SessionContextManager sessionContextManager = webContextsService.getSessionContextManager();

                    SessionContext sessionContext = sessionContextManager.getSessionContextWithSessionId(ids[0]);
                    if (sessionContext != null)
                    {
                        sessionContextManager.removeSessionContextWithSessionId(ids[0]);
                        sessionContextManager.addNewSessionContext(ids[1], sessionContext);
                    }
                }
            }
        }
    }
}
