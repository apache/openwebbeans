/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.test.component.disposal;

import javax.persistence.EntityManager;
import javax.webbeans.Disposes;
import javax.webbeans.Named;
import javax.webbeans.Produces;
import javax.webbeans.RequestScoped;
import javax.webbeans.SessionScoped;

import org.apache.webbeans.test.annotation.binding.Binding1;
import org.apache.webbeans.test.mock.MockEntityManager;

@SessionScoped
public class Disposal1
{
    public static boolean disposeCall = false;

    public Disposal1()
    {

    }

    @Produces
    @Binding1
    @Named
    @RequestScoped
    public EntityManager createEntityManager()
    {
        return new MockEntityManager();
    }

    public void dispose(@Disposes @Binding1 EntityManager em)
    {
        disposeCall = true;
        em.close();
    }
}