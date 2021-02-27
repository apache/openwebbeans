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
package org.apache.webbeans.web.tomcat7.test;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Dummy Servlet which just checks whether CDI injection works
 */
public class TestServlet implements Servlet
{

    @Inject
    private TestBean injectedTestBean;

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
        if (injectedTestBean != null)
        {
            throw new RuntimeException("That's unexpected, we are in fatwar mode so cannot activate the tomcat7 " +
                    "plugin and yet injection is working!");
        }
        // now grab the beans by hand
        TestBean tb = CDI.current().select(TestBean.class).get();

        ServletContext context = CDI.current().select(ServletContext.class).get();

        if (tb == null || tb.getI() != 4711)
        {
            throw new RuntimeException("CDI Lookup does not work!");
        }
        if (context == null)
        {
            throw new RuntimeException("CDI Lookup missing servlet context!");
        }
        if (tb.getRequest() == null)
        {
            throw new RuntimeException("CDI Injection missing servlet request!");
        }

        TestRequestBean requestBean = CDI.current().select(TestRequestBean.class).get();
        TestSessionBean sessionBean = CDI.current().select(TestSessionBean.class).get();


        String action = req.getParameter("action");
        if ("setRequest".equals(action))
        {
            requestBean.setI(Integer.parseInt(req.getParameter("val")));
            requestBean.setI(requestBean.getI() + 100);
            res.getWriter().write("" + requestBean.getI());
            return;
        }

        if ("setSession".equals(action))
        {
            sessionBean.setI(Integer.parseInt(req.getParameter("val")));
            res.getWriter().write("" + sessionBean.getI());
            return;
        }

        if ("getSession".equals(action))
        {
            res.getWriter().write(sessionBean.getI());
            return;
        }

        if ("clearSession".equals(action))
        {
            ((HttpServletRequest)req).getSession(true).invalidate();
        }


        res.getWriter().write(":thumb_up:");
    }

    public String getServletInfo()
    {
        return null;
    }
}
