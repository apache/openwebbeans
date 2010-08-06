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
package org.apache.webbeans.spi;

import javax.servlet.http.HttpSession;

public interface FailOverService 
{
    /**
     * Used for tracking the origin of serialized bean instances. 
     * 
     * @return
     */
    public String getJVMId();
    
    /**
     * The session attribute name used to store the bean instances bag
     *    
     * @return
     */
    public String getFailOverAttributeName();
    
    public boolean isSupportFailOver();
    
    public boolean isSupportPassivation();
    
    public void enableFailOverSupport(boolean flag);

    public void enablePassivationSupport(boolean flag);

    /**
     * Inform the service that a session is idle. Invoked when we finish
     * a request.
     * 
     * @param session
     */
    public void sessionIsIdle(HttpSession session);

    /**
     * Inform the service that a session will be active. Invoked when 
     * a request is received. 
     * 
     * @param session
     */
    public void sessionIsInUse(HttpSession session);
    
    /**
     * Invoked when we try to restore cdi bean instances. Invoked when
     * a request is finished.
     * 
     * @param session
     */
    public void restoreBeans(HttpSession session);
    
    /**
     * Container is going to actively passivate a session.
     * 
     * @param session
     */
    public void sessionWillPassivate(HttpSession session);
}
