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
package org.apache.webbeans.jsf;



import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.web.context.WebConversationService;

public class JsfConversationService extends WebConversationService
{

    public JsfConversationService(WebBeansContext webBeansContext)
    {
        super(webBeansContext);
    }

    @Override
    public String getConversationId()
    {
        String conversationId = JSFUtil.getConversationId();
        if (conversationId == null || conversationId.length() == 0)
        {
            // try to get the conversationId from the servlet
            conversationId = super.getConversationId();
        }
        if (conversationId == null || conversationId.length() == 0)
        {
            return null;
        }
        if ("none".equals(JSFUtil.getConversationPropagation()))
        {
            // explicit 'blocking' of conversationPropagation
            return null;
        }
        return conversationId;
    }

}
