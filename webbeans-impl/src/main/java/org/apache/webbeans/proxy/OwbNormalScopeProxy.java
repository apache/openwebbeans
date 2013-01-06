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


import java.io.Serializable;

/**
 * <p>Interface for all OpenWebBeans {@link javax.enterprise.context.NormalScope} Proxies.
 * A normalscoping proxy just resolves the underlying Contextual Instance
 * and directly invokes the target method onto it.</p>
 *
 * <p>Each <code>OwbNormalScopeProxy</code> contains a {@link javax.inject.Provider}
 * which returns the current Contextual Instance.</p>
 *
 * <p>This interface extends Serializable because every NormalScoped bean proxy must
 * be Serializable!</p>
 */
public interface OwbNormalScopeProxy extends Serializable
{
}
