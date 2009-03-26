/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.sample.conversation;

import java.io.Serializable;

import javax.annotation.Named;
import javax.context.Conversation;
import javax.context.RequestScoped;
import javax.inject.Current;

import org.apache.webbeans.sample.model.conversation.ConversationModel;
import org.apache.webbeans.util.JSFUtil;

@RequestScoped
@Named
public class ShoppingCardBean implements Serializable
{
    private @Current Conversation conversation;
    
    private @Current ConversationModel model;
    
    private String book;
    
    public String startConversation()
    {
        conversation.begin();
        
        JSFUtil.addInfoMessage("Conversation with id : " + conversation.getId() + " is started");
        
        return null;
    }
    
    public String addNewBook()
    {
        model.getList().add(this.book);
        
        StringBuffer buffer = new StringBuffer("Your shopping card contents : [");
        
        for(String b : model.getList())
        {
            buffer.append("," + b);
        }
        
        JSFUtil.addInfoMessage("Your shopping card contents : " + buffer.toString() + "]");
        
        return null;
    }
    
    public String endConversation()
    {
        JSFUtil.addInfoMessage("Conversation with id : " + conversation.getId() + " is ended");
        
        conversation.end();        
        
        return null;
    }

    /**
     * @return the book
     */
    public String getBook()
    {
        return book;
    }

    /**
     * @param book the book to set
     */
    public void setBook(String book)
    {
        this.book = book;
    }

    
}
