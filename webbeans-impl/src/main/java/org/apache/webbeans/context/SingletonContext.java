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
package org.apache.webbeans.context;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

/**
 * Application context implementation.
 * 
 */
public class SingletonContext extends AbstractContext
{
    private static final long serialVersionUID = -7600942820719243149L;

    public SingletonContext()
    {
        super(Singleton.class);
    }

    @Override
    public void setComponentInstanceMap()
    {
        componentInstanceMap = new ConcurrentHashMap<>();
    }
}
