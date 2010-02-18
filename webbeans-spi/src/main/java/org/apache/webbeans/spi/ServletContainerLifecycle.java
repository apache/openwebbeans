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


/**
 * Interface for any CDI Container Lifecycles which are meant to be used for Servlets.
 * Additionally to the default ContainerLifecycle this allows starting and stopping sessions
 * and requests.
 * 
 * This may be used to access servlet related lifecycle implementations in e.g. test containers.
 * 
 * @version $Rev$ $Date$
 */
public interface ServletContainerLifecycle extends ContainerLifecycle {

    /**
     * start a new session context
     * @param event most probably a HttpSessionEvent
     */
    public void sessionStarted(Object event);
    
    /**
     * end the session context
     * @param event most probably a HttpSessionEvent
     */
    public void sessionEnded(Object event);
    
    /**
     * start a new request context 
     * @param event most probably a ServletRequestEvent
     */
    public void requestStarted(Object event);
    
    /**
     * stop the current request context
     */
    public void requestEnded(Object event);
    
}
