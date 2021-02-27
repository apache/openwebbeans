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


import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.StringBuilder;
import java.net.URL;

/**
 * Simple requests to the tomcat installation
 */
public class OwbTomcatPluginIT
{
    @Test
    public void testTomcatRequest() throws Exception
    {

        {
            // Get the response
            String response = getResponse("http://localhost:9082/owbtomcat7it/test.test");
            Assert.assertEquals(":thumb_up:", response);
        }
        {
            String response = getResponse("http://localhost:9082/owbtomcat7it/test.test?action=setRequest&val=3500");
            Assert.assertEquals("3600", response);
        }

        {
            String response = getResponse("http://localhost:9082/owbtomcat7it/test.test?action=setSession&val=500");
            Assert.assertEquals("500", response);
        }
    }

    private String getResponse(String url) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader rd = new BufferedReader(
                new InputStreamReader(new URL(url).openStream())))
        {

            String line = "";
            while ((line = rd.readLine()) != null)
            {
                builder.append(line);
            }
        }
        return builder.toString();
    }

}
