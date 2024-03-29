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
package org.apache.webbeans.test.portable.events.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.inject.spi.ProcessObserverMethod;

public class AddObserverMethodExtension implements Extension
{
    public static class MyEvent
    {
        
    }
    
    public static class MyBeanExtension implements Extension
    {
        public static ObserverMethod<MyEvent> observerMethod;
        
        public void observer(@Observes ProcessObserverMethod<MyEvent, AddObserverMethodExtension> event)
        {
            observerMethod = event.getObserverMethod();
            
        }
    }
    
    public void observer(@Observes AfterBeanDiscovery event)
    {
        event.addObserverMethod(new ObserverMethod<MyEvent>(){

            @Override
            public Class<?> getBeanClass()
            {
                return AddObserverMethodExtension.class;
            }

            @Override
            public Set<Annotation> getObservedQualifiers()
            {
                return Collections.emptySet();
            }

            @Override
            public Type getObservedType()
            {
                return MyEvent.class;
            }

            @Override
            public Reception getReception()
            {
                return Reception.ALWAYS;
            }

            @Override
            public TransactionPhase getTransactionPhase()
            {
                return null;
            }

            @Override
            public void notify(MyEvent event)
            {
                
            }
            
        });
    }

}
