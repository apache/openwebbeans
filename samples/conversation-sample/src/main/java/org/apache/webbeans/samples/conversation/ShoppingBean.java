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
package org.apache.webbeans.samples.conversation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Conversation;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.inject.Produces;
import javax.faces.component.UIData;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ConversationScoped
public class ShoppingBean implements Serializable
{
    private static final long serialVersionUID = 1L;

    private @Inject Products products;
    
    private @Inject Conversation conversation;

    private List<Item> items = new ArrayList<>();
        
    private transient UIData uiTable;
    
    

    @PostConstruct
    public void init()
    {
        Item defaultItem = new Item();
        defaultItem.setName("Default Item");
        defaultItem.setPrice(1000L);
        
        items.add(defaultItem);
    }
    
    
    public String startShopping()
    {
        if (this.conversation.isTransient())
        {
            this.conversation.begin();
        }
        
        return null;
    }
    
    public String checkout()
    {
        if (!this.conversation.isTransient())
        {
            this.conversation.end();
        }
        
        return null;
    }
    
    public Conversation getConversation()
    {
        return this.conversation;
    }
    
    @Produces @Named("selectedItems")
    public List<Item> listSelectedItems()
    {
        return this.items;
    }
    
    public String buy()
    {
        Item item = (Item) uiTable.getRowData();
        this.items.add(item);
        
        return null;
    }
    
    @Produces @Named("allProducts")
    public List<Item> listAllProducts()
    {
        return this.products.getProducts();
    }

    /**
     * @return the uiTable
     */
    public UIData getUiTable()
    {
        return uiTable;
    }

    /**
     * @param uiTable the uiTable to set
     */
    public void setUiTable(UIData uiTable)
    {
        this.uiTable = uiTable;
    }
     
}
