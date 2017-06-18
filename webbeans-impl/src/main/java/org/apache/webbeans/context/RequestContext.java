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

import java.util.HashMap;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Contextual;

import org.apache.webbeans.intercept.RequestScopedBeanInterceptorHandler;

/**
 * Request context implementation.
 *
 */
public class RequestContext extends AbstractContext
{
    private static final long serialVersionUID = -1030240915163272268L;

    /**
     * If a Session gets destroyed in a HttpRequest then we store the session away
     * and only destroy it at the end of the request.
     */
    private SessionContext propagatedSessionContext;

    /**
     * if propagatedSessionContext != null the event instance to use (http session can be no more accessible)
     */
    private Object httpSession;

    /*
    * Constructor
    */
    public RequestContext()
    {
        super(RequestScoped.class);
    }

    @Override
    public void setComponentInstanceMap()
    {
        componentInstanceMap = new HashMap<>();
    }

    /**
     * The base object for the current RequestContext.
     * For a synthetic 'request' this is null. For a real http ServletRequest
     * this is the HttpServletRequest.
     * This is what gets used as payload for various
     * {@link javax.enterprise.context.Initialized} and
     * {@link javax.enterprise.context.Destroyed} events.
     *
     * This can be overloaded in web requests.
     * @return the ServletRequest or {@code null} for other kind of requests‚
     */
    public Object getRequestObject()
    {
        return null;
    }

    public void setPropagatedSessionContext(SessionContext propagatedSessionContext)
    {
        this.propagatedSessionContext = propagatedSessionContext;
    }

    /**
     * @return the SessionContext to get destroyed at the end of the request or {@code null} otherwise
     */
    public SessionContext getPropagatedSessionContext()
    {
        return propagatedSessionContext;
    }

    public Object getHttpSession()
    {
        return httpSession;
    }

    public void setHttpSession(Object httpSession)
    {
        this.httpSession = httpSession;
    }

    @Override
    public void destroy(Contextual<?> contextual)
    {
        super.destroy(contextual);
        RequestScopedBeanInterceptorHandler.removeThreadLocals();
    }
}
