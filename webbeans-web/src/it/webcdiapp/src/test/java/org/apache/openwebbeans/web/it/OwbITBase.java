/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openwebbeans.web.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.webbeans.util.StringUtil;
import org.junit.Assert;

/**
 */
public class OwbITBase
{
    private static final Logger log = Logger.getLogger(OwbITBase.class.getName());

    private static String host = null;
    private static String port = null;
    private static String contextPath = null;

    private static String baseUrl = null;


    protected String getPageUrl(String path)
    {
        return getPageUrl(path, null);
    }

    protected String getPageUrl(String path, Properties params)
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

    protected String getBaseUrl()
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

            String contextPath = getContextPath();
            if (contextPath != null)
            {
                sb.append(contextPath).append("/");
            }

            baseUrl = sb.toString();
        }


        return baseUrl;
    }


    protected String getHost()
    {
        if (host == null)
        {
            host = getConfiguration("owb.it.host", "http://localhost");
        }
        return host;
    }

    protected String getPort()
    {
        if (port == null)
        {
            port = getConfiguration("owb.it.port", "8089");
        }
        return port;
    }

    protected String getContextPath()
    {
        if (contextPath == null)
        {
            contextPath = getConfiguration("owb.it.contextPath", "webbeanswebCdiApp");
        }
        return contextPath;
    }

    protected String getConfiguration(String key, String defaultValue)
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

    protected String httpGet(DefaultHttpClient client, String page, int expectedHttpCode) throws IOException
    {
        String pageUrl = getPageUrl(page);
        log.info("Sending GET: " + pageUrl);

        HttpGet getRequest = new HttpGet(pageUrl);
        HttpResponse response = client.execute(getRequest);
        Assert.assertNotNull(response);

        int statusCode = response.getStatusLine().getStatusCode();

        HttpEntity httpEntity = response.getEntity();
        Assert.assertNotNull(httpEntity);

        StringBuffer sb = new StringBuffer();

        InputStream inputStream = null;
        try
        {
            inputStream = httpEntity.getContent();
            Assert.assertNotNull(inputStream);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                sb.append(line).append('\n');
            }
        }
        finally {
            if (inputStream != null)
            {
                inputStream.close();
            }
        }

        String content = sb.toString();
        log.info("  status=" + statusCode + " content: " + content);

        Assert.assertEquals(expectedHttpCode, statusCode);
        getRequest.releaseConnection();
        return content.trim();
    }
}
