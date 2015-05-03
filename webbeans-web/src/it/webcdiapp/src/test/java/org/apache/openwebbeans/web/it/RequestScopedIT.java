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

import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RequestScopedIT extends OwbITBase
{

    @Test
    public void testRequestScoped() throws Exception
    {
        DefaultHttpClient client = new DefaultHttpClient();

        HttpGet resetGet = new HttpGet(getPageUrl("/check?reset=true"));
        HttpResponse response = client.execute(resetGet);
        resetGet.releaseConnection();

        HttpGet jspGet = new HttpGet(getPageUrl("/index.jsp"));
        checkResponse(client.execute(jspGet));
        jspGet.releaseConnection();

        HttpGet checkGet = new HttpGet(getPageUrl("/check"));
        response = client.execute(checkGet);
        checkResponse(response);

        HttpEntity httpEntity = response.getEntity();
        Assert.assertNotNull(httpEntity);

        InputStream content = null;
        try
        {
            content = httpEntity.getContent();
            Assert.assertNotNull(content);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(content));
            String result = bufferedReader.readLine();
            Assert.assertNotNull(result);
            Assert.assertEquals("2,2", result);
        }
        finally {
            if (content != null)
            {
                content.close();
            }
        }


        checkGet.releaseConnection();
    }

    private void checkResponse(HttpResponse response)
    {
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusLine());
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

}
