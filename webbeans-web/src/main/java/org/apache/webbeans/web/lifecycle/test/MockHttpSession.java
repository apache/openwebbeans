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
package org.apache.webbeans.web.lifecycle.test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unchecked")
public class MockHttpSession implements HttpSession
{
    private static final AtomicInteger ID = new AtomicInteger(0);

    private final String id;

    public MockHttpSession()
    {
        id = Integer.toString(ID.incrementAndGet());
    }

    @Override
    public Object getAttribute(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration getAttributeNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getCreationTime()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public long getLastAccessedTime()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxInactiveInterval()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ServletContext getServletContext()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void invalidate()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isNew()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeAttribute(String arg0)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setAttribute(String arg0, Object arg1)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMaxInactiveInterval(int arg0)
    {
        // TODO Auto-generated method stub

    }

}
