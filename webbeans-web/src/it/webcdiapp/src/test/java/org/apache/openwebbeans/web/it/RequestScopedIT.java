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

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;

import org.junit.Assert;
import org.junit.Test;


public class RequestScopedIT extends OwbITBase
{

    @Test
    public void testRequestScoped() throws Exception
    {
        DefaultHttpClient client = new DefaultHttpClient();

        // GET http://localhost:8089/webbeanswebCdiApp/check

        String response = httpGet(client, "/check/reset", HttpServletResponse.SC_OK);
        response = httpGet(client, "/index.jsp", HttpServletResponse.SC_OK);
        response = httpGet(client, "/check", HttpServletResponse.SC_OK);
        Assert.assertEquals("2,2", response);

        response = httpGet(client, "/check/events", HttpServletResponse.SC_OK);
        Assert.assertNotNull(response);

        // application and session still running
        // for the session we got 2 full requests + 1 end after the reset + 1 start before the info gets rendered in the last request
        Assert.assertEquals("application:1/0\nsession:1/0\nrequest:3/3", response);
    }

}
