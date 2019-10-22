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
package org.apache.webbeans.web.tests;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.web.context.WebContextsService;
import org.apache.webbeans.web.lifecycle.test.MockServletContext;
import org.junit.Assert;
import org.junit.Test;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.servlet.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.junit.Assert.fail;

public class WebContextsServiceTest
{
    /**
     * Without the fix it was failing this way:
     *
     * java.lang.NullPointerException
     * at org.apache.webbeans.web.context.WebContextsService.lazyStartSessionContext(WebContextsService.java:816)
     */
    @Test
    public void issue_OWB1124()
    {
        final WebBeansContext webBeansContext = new WebBeansContext(new HashMap<Class<?>, Object>(), new Properties()
        {{
            setProperty(ContextsService.class.getName(), WebContextsService.class.getName());
        }});

        try
        {
            webBeansContext.getBeanManagerImpl().getContext(SessionScoped.class);
            fail();
        }
        catch (final ContextNotActiveException cnae)
        {
            // ok
        }
    }

    /**
     * Without the fix, this test fails with the following exception:
     *
     * java.lang.ClassCastException: class org.apache.webbeans.web.tests.WebContextsServiceTest$ServletRequestWrapper cannot be cast to class javax.servlet.http.HttpServletRequest (org.apache.webbeans.web.tests.WebContextsServiceTest$ServletRequestWrapper and javax.servlet.http.HttpServletRequest are in unnamed module of loader 'app')
     *
     * 	at org.apache.webbeans.web.context.WebContextsService.initRequestContext(WebContextsService.java:360)
     * 	at org.apache.webbeans.web.context.WebContextsService.startContext(WebContextsService.java:313)
     * 	at org.apache.webbeans.web.tests.WebContextsServiceTest.fixClassCastException(WebContextsServiceTest.java:77)
     *
     * 	The key here is that ServletRequestEvent has a ServletRequest, as opposed to HttpServletRequest, and the cast in WebContextsService
     * 	fails when a ServletRequest that doesn't implement HttpServletRequest is passed in the ServletRequestEvent.
     */
    @Test
    public void fixClassCastException()
    {
        final WebBeansContext webBeansContext = new WebBeansContext(new HashMap<Class<?>, Object>(), new Properties()
        {{
            setProperty(ContextsService.class.getName(), WebContextsService.class.getName());
        }});

        final WebContextsService service = new WebContextsService(webBeansContext);
        final ServletRequestWrapper request = new ServletRequestWrapper();
        service.startContext(RequestScoped.class, new ServletRequestEvent(new MockServletContext(), request));
        final ServletRequest servletRequest = service.getRequestContext(false).getServletRequest();

        Assert.assertEquals(request, servletRequest);
    }


    public static class ServletRequestWrapper implements ServletRequest {

        @Override
        public AsyncContext getAsyncContext() {
            return null;
        }

        @Override
        public Object getAttribute(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return null;
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public int getContentLength() {
            return 0;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public DispatcherType getDispatcherType() {
            return null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return null;
        }

        @Override
        public String getLocalAddr() {
            return null;
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return null;
        }

        @Override
        public String getLocalName() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public String getParameter(String s) {
            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return null;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues(String s) {
            return new String[0];
        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return null;
        }

        @Override
        public String getRealPath(String s) {
            return null;
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public String getRemoteHost() {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String s) {
            return null;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public String getServerName() {
            return null;
        }

        @Override
        public int getServerPort() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public void removeAttribute(String s) {

        }

        @Override
        public void setAttribute(String s, Object o) {

        }

        @Override
        public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

        }

        @Override
        public AsyncContext startAsync() {
            return null;
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
            return null;
        }
    }
}
