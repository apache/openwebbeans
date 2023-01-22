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
package org.apache.webbeans.test.portable.addannotated.extension;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

import org.apache.webbeans.annotation.ApplicationScopeLiteral;
import org.apache.webbeans.annotation.RequestedScopeLiteral;


public class AddAdditionalAnnotatedTypeExtension implements Extension
{
    @RequestScoped
    public static class MyBean
    {

        @Produces
        @Dependent
        public String create()
        {
            return "dings";
        }
    }

    public static class MyConfigBean1
    {
        public String getId()
        {
            return "1";
        }
    }

    public static class MyConfigBean2
    {
        public String getId()
        {
            return "2";
        }
    }


    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm)
    {
        bbd.addAnnotatedType(bm.createAnnotatedType(MyBean.class), "modified");

        bbd.addAnnotatedType(MyConfigBean1.class, "hi1")
            .add(RequestedScopeLiteral.INSTANCE);

        bbd.addAnnotatedType(MyConfigBean2.class, "hi1")
            .add(ApplicationScopeLiteral.INSTANCE);
    }

}
