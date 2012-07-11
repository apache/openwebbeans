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
package org.apache.webbeans.context.type;


/**
 * Defines the enumaration of the standart context types in the web beans
 * container.
 * 
 * <p>
 * 
 * Standart context types,
 * <ul>
 *  <li>Dependent Context</li>
 *  <li>Request Context</li>
 *  <li>Session Context</li>
 *  <li>Application Context</li>
 *  <li>Conversation Context</li>
 *  <li>Singleton Context</li>
 * </ul>
 * 
 * </p>
 * 
 */
public enum ContextTypes
{
    REQUEST(0), 
    SESSION(1), 
    APPLICATION(2), 
    CONVERSATION(3), 
    DEPENDENT(4),
    SINGLETON(5);

    private int cardinal;

    ContextTypes(int cardinal)
    {
        this.cardinal = cardinal;
    }

    public int getCardinal()
    {
        return cardinal;
    }
}
