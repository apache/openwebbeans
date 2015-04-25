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

/**
 * Container listener that propagates the change of a session id (e. g. during an authentication through the container)
 * to the session context manager. Otherwise a new session context for the same session is created, because the session
 * id is used to identify the session context. <br/> Adapted from OpenEJB tomee/tomee-catalina and updated.
 *
 * @deprecated we do not use the sessionId anymore since owb-1.5.1, so we do not need any sessionId change listener
 */
public class TomcatContainerListener implements ContainerListener
{

    @Override
    public void containerEvent(ContainerEvent containerEvent)
    {
        // nothing to do anymore.
        // we do not use the sessionId anymore since owb-1.5.1
    }
}
