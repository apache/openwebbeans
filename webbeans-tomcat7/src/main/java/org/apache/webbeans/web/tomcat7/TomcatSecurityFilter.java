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

import java.io.IOException;
import java.security.Principal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;


/**
 * Filter which sets the UserPrincipal into a ThreadLocal
 * to make it injectable via a CDI Producer
 */
public class TomcatSecurityFilter implements Filter
{
    private static ThreadLocal<Principal> principal = new ThreadLocal<Principal>();
    
    public static Principal getPrincipal()
    {
        return principal.get();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        try
        {
            if(request instanceof HttpServletRequest)
            {
                Principal p = ((HttpServletRequest)request).getUserPrincipal();
                if(p != null)
                {
                    principal.set(p);
                }
            }

            // continue with the request
            chain.doFilter(request, response);
        }
        finally
        {
            if(principal.get() != null)
            {
                principal.remove();
            }
        }
    }

    @Override
    public void destroy()
    {
        // nothing to do
    }
}
