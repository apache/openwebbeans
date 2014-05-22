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
package org.apache.webbeans.proxy;

import java.lang.reflect.Method;

/**
 * An InterceptorHandler can be used to dynamically invoke methods on
 * an internally stored target instance.
 */
public interface InterceptorHandler
{

    /**
     * This method will get called whenever the proxy
     * of the intercepted instance will get invoked.
     * @param method Method which should get invoked
     * @param args original invocation parameters
     * @return the return value of the intercepted methos
     */
    public Object invoke(Method method, Object[] args);
}
