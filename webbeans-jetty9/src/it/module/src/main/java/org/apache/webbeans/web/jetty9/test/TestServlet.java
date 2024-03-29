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
package org.apache.webbeans.web.jetty9.test;

import jakarta.inject.Inject;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

/**
 * Dummy Servlet which just checks whether CDI injection works
 */
public class TestServlet implements Servlet
{

    @Inject
    private TestBean tb;

    @Inject
    private ServletContext context;

    public void destroy()
    {
        // nothing to do
    }

    public void init(ServletConfig config) throws ServletException
    {
        // nothing to do
    }

    public ServletConfig getServletConfig()
    {
        return null;
    }

    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
    {
        if (tb == null || tb.getI() != 4711)
        {
            throw new RuntimeException("CDI Injection does not work!");
        }
        if (context == null)
        {
            throw new RuntimeException("CDI Injection missing servlet context!");
        }
        if (tb.getRequest() == null)
        {
            throw new RuntimeException("CDI Injection missing servlet request!");
        }
    }

    public String getServletInfo()
    {
        return null;
    }
}
