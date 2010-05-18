/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.jsf12;

import java.io.IOException;

import javax.enterprise.context.Conversation;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.webbeans.conversation.ConversationManager;

public class WebBeansJSFFilter implements Filter
{

    public void destroy()
    {

    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(servletResponse)
        {

            @Override
            public void sendRedirect(String location) throws IOException
            {
                String url = null;
                boolean ok = false;
                int indexOfQuery = location.indexOf('?');
                if (indexOfQuery > 0) 
                {
                    String queryString = location.substring(indexOfQuery);
                    // If the query string already has a cid parameter, return url directly.
                    if (queryString.contains("?cid=") || queryString.contains("&cid="))
                    {
                        url = location;
                        ok = true;
                    }
                }
                
                if(!ok)
                {
                    ConversationManager conversationManager = ConversationManager.getInstance();
                    Conversation conversation = conversationManager.getConversationBeanReference();
                    if (conversation != null && !conversation.isTransient())
                    {
                        url = JSFUtil.getRedirectViewIdWithCid(location, conversation.getId());
                    }                    
                }                
                                
                super.sendRedirect(url);                

            }

        };
                

        chain.doFilter(request, responseWrapper);
    }

    public void init(FilterConfig config) throws ServletException
    {

    }

}
