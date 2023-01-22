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
package org.superbiz.conversation;

import jakarta.enterprise.inject.spi.CDI;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Test Servlet to run our conversation handling tests
 */
@WebServlet(urlPatterns = "/conversation/*")
public class ConversationITServlet extends HttpServlet
{
    private static final Logger log = Logger.getLogger(ConversationITServlet.class.getName());

    private ConversationalShoppingCart shoppingCart;
    private SessionUser sessionUser;


    @Override
    public void init() throws ServletException
    {
        shoppingCart = CDI.current().select(ConversationalShoppingCart.class).get();
        sessionUser = CDI.current().select(SessionUser.class).get();
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String uri = request.getRequestURI();
        String action = uri.substring(uri.lastIndexOf('/') + 1);

        response.setContentType("text/plain");

        if ("info".equals(action))
        {
            // nothing to do
        }
        else if ("invalidateSession".equals(action))
        {
            request.getSession().invalidate();
        }
        else if ("invalidateSessionAfterBeanAccess".equals(action))
        {
            // first touch the bean, then invalidate in the same request
            shoppingCart.getContent();
            request.getSession().invalidate();
        }
        else if ("set".equals(action))
        {
            String content = request.getParameter("content");
            shoppingCart.setContent(content);
        }
        else if ("begin".equals(action))
        {
            shoppingCart.getConversation().begin();
        }
        else if ("end".equals(action))
        {
            shoppingCart.getConversation().end();
        }
        // user actions
        else if ("setUser".equals(action))
        {
            String name = request.getParameter("name");
            sessionUser.setName(name);
        }
        else
        {
            response.getWriter().append("error - unknown command");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        String msg = shoppingCart.toString() + "/" + sessionUser.toString();
        log.info("action = " + action + " shoppingCart=" + shoppingCart + " user=" + sessionUser);
        response.getWriter().append(msg);
        response.setStatus(HttpServletResponse.SC_OK);
    }

}
