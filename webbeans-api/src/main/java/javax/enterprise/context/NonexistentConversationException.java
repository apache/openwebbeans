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
package javax.enterprise.context;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * If a long running conversation cannot be restored, OWB will
 * assign a fresh conversation and throws this very Exception.
 * 
 * The user code is free to catch it and continue his work
 * with this new conversation.  
 * 
 * @see Context#get(Contextual, CreationalContext)
 * @see Context#get(Contextual)
 * @since 1.0 PFD2
 */
public class NonexistentConversationException extends ContextException
{

    private static final long serialVersionUID = 4783816486073845333L;
    
    /**
     * Creates a new exception with message.
     * 
     * @param message message
     */
    public NonexistentConversationException(String message)
    {
        super(message);
    }

    /**
     * Create a new exception with the root cause.
     * 
     * @param cause cause of the exception
     */
    public NonexistentConversationException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Creates a new exception with the given message and throwable cause.
     * 
     * @param message exception message
     * @param cause root cause of the exception
     */
    public NonexistentConversationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
