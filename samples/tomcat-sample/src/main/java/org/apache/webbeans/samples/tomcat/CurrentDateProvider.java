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
package org.apache.webbeans.samples.tomcat;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@SessionScoped // just to test @Initialized(SessionScoped.class)
public class CurrentDateProvider implements Serializable
{
    private @Inject Principal principal;
    
    @Produces
    public Date getCurrentDate()
    {
        return new Date();
    }
    
    public Principal getPrincipal()
    {
        return principal;
    }
}
