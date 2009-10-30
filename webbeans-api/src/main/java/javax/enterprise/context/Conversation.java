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

/**
 * Defines the conversation instance contract for using
 * in the {@link ConversationScoped} webbeans components.
 * 
 * <p>
 * Please see the <b>8.5.4 Conversation context lifecycle</b> of the specification.
 * </p>
 * 
 * @see ConversationScoped
 */
public interface Conversation
{
    /**
     * Starts new conversation.
     */
    public void begin();

    /**
     * Starts new conversation with the given id.
     * 
     * @param id conversation id.
     */
    public void begin(String id);

    /**
     * Ends of the conversation.
     */
    public void end();

    /**
     * @return <code>true</code> if conversation is transient, 
     *         <code>false</code> if it is a long running conversation.
     */
    public boolean isTransient();

    /**
     * Gets conversation id.
     * 
     * @return conversation id
     */
    public String getId();

    /**
     * Returns conversation time out.
     * 
     * @return conversation timeout
     */
    public long getTimeout();

    /**
     * Sets conversation timeout in ms.
     * 
     * @param milliseconds timeout of the conversation
     */
    public void setTimeout(long milliseconds);

}
