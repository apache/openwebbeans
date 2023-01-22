/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.events.observer;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class BeanA extends Superclass
{

    public static void observeTestEvent(@Observes StaticTestEvent testEvent) {
        testEvent.addInvocation(BeanA.class.getSimpleName());
    }

    private void observeTestEvent(@Observes PrivateTestEvent testEvent) {
        testEvent.addInvocation(getBeanName());
    }

    @Override
    protected void observeTestEvent(@Observes TestEvent testEvent)
    {
        testEvent.addInvocation(getBeanName());
    }
}
