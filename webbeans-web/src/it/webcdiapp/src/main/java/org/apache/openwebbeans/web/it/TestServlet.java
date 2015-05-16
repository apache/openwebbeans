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
package org.apache.openwebbeans.web.it;

import org.apache.openwebbeans.web.it.beans.ContextEventCounter;
import org.apache.openwebbeans.web.it.beans.RequestScopedBean;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/check/*")
public class TestServlet extends HttpServlet
{
    private static final long serialVersionUID = -8232635534522251153L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String uri = request.getRequestURI();
        String action = uri.substring(uri.lastIndexOf('/') + 1);

        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);

        if ("reset".equals(action))
        {
            request.getSession().invalidate();
            RequestScopedBean.resetCounter();
            ContextEventCounter.resetCounter();
        }
        else if ("events".equals(action))
        {
            response.getWriter().append(ContextEventCounter.info());
        }
        else
        {

            response.getWriter().append(RequestScopedBean.info());
        }
    }
}
