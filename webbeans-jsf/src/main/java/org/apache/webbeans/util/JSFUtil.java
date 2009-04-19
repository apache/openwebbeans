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
package org.apache.webbeans.util;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

public final class JSFUtil
{
    private JSFUtil()
    {

    }

    public static FacesContext getCurrentFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }

    public static ExternalContext getExternalContext()
    {
        return getCurrentFacesContext().getExternalContext();
    }

    public static HttpSession getSession()
    {
        return (HttpSession) getExternalContext().getSession(true);
    }

    public static boolean isPostBack()
    {
        return getCurrentFacesContext().getRenderKit().getResponseStateManager().isPostback(getCurrentFacesContext());
    }

    public static String getViewId()
    {
        return getCurrentFacesContext().getViewRoot().getViewId();
    }

    public static ViewHandler getViewHandler()
    {
        return getCurrentFacesContext().getApplication().getViewHandler();
    }

    public static Application getApplication()
    {
        return getCurrentFacesContext().getApplication();
    }
    
    public static void addInfoMessage(String message)
    {
        FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,message,"");
        getCurrentFacesContext().addMessage(null, fm);
    }

    public static String getRedirectViewId(String redirectId)
    {
        Asserts.assertNotNull(redirectId, "redirectId parameter can not be null");
        String path = getExternalContext().getRequestContextPath();

        int index = redirectId.indexOf(path);

        return redirectId.substring(index + path.length(), redirectId.length());
    }

    public static UIViewRoot getViewRoot()
    {
        return getCurrentFacesContext().getViewRoot();
    }

    public static String getConversationId()
    {
        UIViewRoot viewRoot = JSFUtil.getViewRoot();
        HtmlInputHidden conversationId = (HtmlInputHidden) viewRoot.findComponent("javax_webbeans_ConversationId");

        if (conversationId != null)
        {
            return conversationId.getValue().toString();
        }

        return null;
    }
}
