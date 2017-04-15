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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.webbeans.util.StringUtil;
import org.junit.Assert;
import org.junit.Test;


/**
 * Integration Test to check cross context forwards.
 * Note that this requires to additionally setup the
 * WebBeansConfigurationFilter in web.xml with
 * dispatcher FORWARD and INCLUDE.
 * See the web.xml of webapp b for more details
 */
public class ForwardIT
{
    private static final Logger log = Logger.getLogger(ForwardIT.class.getName());

    private static String host = null;
    private static String port = null;
    private static String contextPath = null;

    private static String baseUrl = null;


    @Test
    public void testCrossContextForward() throws Exception
    {
        // GET http://localhost:8089/a/forward.jsp

        String response = httpGet("/a/forward.jsp", HttpServletResponse.SC_OK);
        Assert.assertTrue(response.contains("All fine!"));
    }

    @Test
    public void testSameContextForward() throws Exception
    {
        // GET http://localhost:8089/b/forward.jsp

        String response = httpGet("/b/forward.jsp", HttpServletResponse.SC_OK);
        Assert.assertTrue(response.contains("All fine!"));
    }



    private String getPageUrl(String path)
    {
        return getPageUrl(path, null);
    }

    private String getPageUrl(String path, Properties params)
    {
        String baseUrl = getBaseUrl();
        StringBuilder sb = new StringBuilder(baseUrl);
        if (!baseUrl.endsWith("/") && !path.startsWith("/"))
        {
            sb.append('/');
        }
        sb.append(path.startsWith("/") ? path.substring(1) : path);

        if (params != null)
        {
            boolean containsParams = path.contains("?");
            for (Map.Entry paramEntry : params.entrySet())
            {
                sb.append(containsParams ? "&" : "?");
                sb.append(paramEntry.getKey()).append("=").append(paramEntry.getValue());
                containsParams = true;
            }
        }

        return sb.toString();
    }

    private String getBaseUrl()
    {
        if (baseUrl == null)
        {
            StringBuilder sb = new StringBuilder(getHost());
            String port = getPort();
            if (port != null)
            {
                sb.append(':').append(port);
            }

            sb.append('/');

            baseUrl = sb.toString();
        }


        return baseUrl;
    }


    private String getHost()
    {
        if (host == null)
        {
            host = getConfiguration("owb.it.host", "http://localhost");
        }
        return host;
    }

    private String getPort()
    {
        if (port == null)
        {
            port = getConfiguration("owb.it.port", "8089");
        }
        return port;
    }


    private String getConfiguration(String key, String defaultValue)
    {
        String val = System.getProperty(key);
        if (StringUtil.isBlank(val))
        {
            val = System.getenv(key);
        }
        if (StringUtil.isBlank(val))
        {
            val = defaultValue;
        }

        return val;
    }

    private String httpGet(String page, int expectedHttpCode) throws IOException
    {
        String pageUrl = getPageUrl(page);
        log.info("Sending GET: " + pageUrl);


        URL target = new URL(pageUrl);
        HttpURLConnection con = (HttpURLConnection) target.openConnection();
        con.setRequestMethod("GET");

        int statusCode = con.getResponseCode();

        StringBuffer sb = new StringBuffer();

        try (InputStream is = con.getInputStream())
        {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                sb.append(line).append('\n');
            }
        }

        String content = sb.toString();
        log.info("  status=" + statusCode + " content: " + content);

        Assert.assertEquals(expectedHttpCode, statusCode);
        return content.trim();
    }


}
