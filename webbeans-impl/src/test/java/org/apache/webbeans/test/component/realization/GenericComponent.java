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
package org.apache.webbeans.test.component.realization;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Current;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.apache.webbeans.test.annotation.binding.Binding1;
import org.apache.webbeans.test.annotation.binding.Binding2;
import org.apache.webbeans.test.component.UserComponent;
import org.apache.webbeans.test.component.library.BookShop;
import org.apache.webbeans.test.event.LoggedInEvent;

public abstract class GenericComponent
{
    @Produces @Binding2 @SessionScoped UserComponent component;
    
    @Produces
    @RequestScoped
    @Binding1
    protected BookShop getBookShop(@Current BookShop bookShop)
    {
        return bookShop;
    }
    
    protected void dispose(@Binding1 @Disposes BookShop bookShop)
    {
        
    }
    
    protected void observe(@Current @Observes LoggedInEvent event)
    {
        
    }
    
    public void setUserComponent(UserComponent component)
    {
        this.component = component;
    }

}
