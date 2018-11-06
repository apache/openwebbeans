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
package org.apache.webbeans.web.tomcat7;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.security.Principal;

import org.apache.webbeans.corespi.security.SimpleSecurityService;

public class TomcatSecurityService extends SimpleSecurityService
{

    private final Principal proxy = Principal.class.cast(Proxy.newProxyInstance(
            TomcatSecurityService.class.getClassLoader(),
            new Class<?>[]{Principal.class, Unwrap.class}, (proxy, method, args) ->
            {
                try
                {
                    final Principal principal = TomcatSecurityFilter.getPrincipal();
                    if (principal == null)
                    {
                        return null;
                    }
                    if (Unwrap.class == method.getDeclaringClass())
                    {
                        return principal;
                    }
                    return method.invoke(principal, args);
                }
                catch (final InvocationTargetException ite)
                {
                    throw ite.getTargetException();
                }
            }));

    @Override
    public Principal getCurrentPrincipal()
    {
        return proxy;
    }

    public interface Unwrap
    {
        Principal get();
    }
}
