/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.portable.events.discovery;

import java.util.Iterator;
import java.util.Stack;

import org.apache.webbeans.logger.WebBeansLogger;

/**
 * Error stack.
 * @version $Rev$ $Date$
 *
 */
public class ErrorStack
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(ErrorStack.class);
    
    public Stack<Throwable> errorStack = new Stack<Throwable>();
    
    public ErrorStack()
    {
        
    }
    
    public void pushError(Throwable e)
    {
        errorStack.addElement(e);
    }

    public Throwable[] popErrors()
    {
        Throwable[] list = new Throwable[this.errorStack.size()];
        list = this.errorStack.toArray(list);
        
        return list;
    }
    
    public void logErrors()
    {
        if(!this.errorStack.isEmpty())
        {
            Iterator<Throwable> it = this.errorStack.iterator();
            while(it.hasNext())
            {
                Throwable t = it.next();
                logger.error(t);
            }
        }
    }
    
    public void clear()
    {
        this.errorStack.clear();
    }
    
    public boolean hasErrors()
    {
        return !this.errorStack.isEmpty();
    }
}
