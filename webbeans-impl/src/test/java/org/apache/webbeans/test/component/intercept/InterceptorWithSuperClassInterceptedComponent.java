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
package org.apache.webbeans.test.component.intercept;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jakarta.enterprise.context.RequestScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;


@RequestScoped
@Interceptors(value = { InterceptorWithSuperClass.class })
public class InterceptorWithSuperClassInterceptedComponent
{
    String[] s = null;

    public Object intercepted()
    {
        return s;
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception
    {
        System.out.println("In Interceptor Method");
        java.util.List<String> s = new ArrayList<String>();
        Map<String, Object> map = context.getContextData();
        Set<Entry<String, Object>> set = map.entrySet();
        for (Entry<String, Object> s2 : set) {
            s.add(s2.getKey());

        }

        this.s = new String[s.size()];
        this.s = s.toArray(this.s);

        return context.proceed();
    }

}
