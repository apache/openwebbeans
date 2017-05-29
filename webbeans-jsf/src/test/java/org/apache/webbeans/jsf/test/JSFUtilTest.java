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
package org.apache.webbeans.jsf.test;

import javax.faces.component.UIViewRoot;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;
import org.apache.webbeans.jsf.JSFUtil;
import org.junit.Assert;
import org.junit.Test;

public class JSFUtilTest extends TestCase
{
    @Test
    public void testCidUrlUpdate()
    {
        String withQuery = "/context/file.get?name=hipo";
        String withQueryWithPath = "/context/file.get?name=hipo#same";
        String withoutQuery = "/context/file.get";
        String withoutQueryWithPath="/context/file.get#same";
        String withContext = "/context";
        String withContextWithPath = "/context#same";
        String withQueryWithContext = "/context?hiho=hi";
        String withQueryWithContextWithPath="/context?hiho=hi#same";
        
        String cid = "1";
        
        String path = JSFUtil.getRedirectViewIdWithCid(withQuery, cid);        
        Assert.assertEquals("/context/file.get?cid=1&name=hipo", path);
        
        path = JSFUtil.getRedirectViewIdWithCid(withQueryWithPath, cid);        
        Assert.assertEquals("/context/file.get?cid=1&name=hipo#same", path);
        
        path = JSFUtil.getRedirectViewIdWithCid(withoutQuery, cid);        
        Assert.assertEquals("/context/file.get?cid=1", path);
        
        path = JSFUtil.getRedirectViewIdWithCid(withoutQueryWithPath, cid);        
        Assert.assertEquals("/context/file.get?cid=1#same", path);
        
        path = JSFUtil.getRedirectViewIdWithCid(withContext, cid);        
        Assert.assertEquals("/context?cid=1", path);
        
        path = JSFUtil.getRedirectViewIdWithCid(withContextWithPath, cid);
        Assert.assertEquals("/context?cid=1#same", path);
        
        path = JSFUtil.getRedirectViewIdWithCid(withQueryWithContext, cid);
        Assert.assertEquals("/context?cid=1&hiho=hi", path);
        
        path = JSFUtil.getRedirectViewIdWithCid(withQueryWithContextWithPath, cid);
        Assert.assertEquals("/context?cid=1&hiho=hi#same", path);
    }




    @Test
    public void testGetConversationPropagation() {

        String string = JSFUtil.getConversationPropagation();

        assertNull(string);

    }


    @Test
    public void testGetViewRoot() {

        UIViewRoot uIViewRoot = JSFUtil.getViewRoot();

        assertNull(uIViewRoot);

    }


    @Test
    public void testGetRedirectViewIdWithCidReturningNonEmptyString() {

        String string = JSFUtil.getRedirectViewIdWithCid("=V[m", "=V[m");

        assertEquals("=V[m?cid==V[m", string);

    }


    @Test
    public void testGetRedirectViewIdWithCidWithNonEmptyStringOne() {

        String string = JSFUtil.getRedirectViewIdWithCid("asd", "f?cid==V[m");

        assertEquals("asd?cid=f?cid==V[m", string);

    }


    @Test
    public void testGetRedirectViewIdWithCidWithNonEmptyStringTwo() {

        String string = JSFUtil.getRedirectViewIdWithCid("#", "#");

        assertEquals("?cid=##", string);

    }


    @Test
    public void testGetSession() {

        HttpSession httpSession = JSFUtil.getSession();

        assertNull(httpSession);

    }


    @Test
    public void testGetConversationId() {

        String string = JSFUtil.getConversationId();

        assertNull(string);

    }



}
