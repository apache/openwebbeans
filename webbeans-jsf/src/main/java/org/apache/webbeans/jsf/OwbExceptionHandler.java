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

import jakarta.enterprise.context.NonexistentConversationException;
import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import java.util.Iterator;

public class OwbExceptionHandler extends ExceptionHandlerWrapper
{
    private final ExceptionHandler delegate;

    public OwbExceptionHandler(ExceptionHandler exceptionHandler)
    {
        this.delegate = exceptionHandler;
    }

    @Override
    public ExceptionHandler getWrapped()
    {
        return delegate;
    }

    @Override
    public void handle() throws FacesException
    {
        Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator();
        while (i.hasNext())
        {
            ExceptionQueuedEvent event = i.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();

            // get the exception from context
            Throwable t = context.getException();
            if (NonexistentConversationException.class.isInstance(t))
            {
                i.remove();
                throw RuntimeException.class.cast(t);
            }
        }
        delegate.handle();
    }
}
