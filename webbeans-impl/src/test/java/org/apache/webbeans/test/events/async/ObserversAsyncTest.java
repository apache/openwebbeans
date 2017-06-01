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
package org.apache.webbeans.test.events.async;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ObserversAsyncTest
{


    public static class VisitorCollectorEvent
    {
        private List<String> visitors = Collections.synchronizedList(new ArrayList<>());

        public void visiting(String visitor)
        {
            visitors.add(visitor);
        }

        public List<String> getVisitors()
        {
            return visitors;
        }
    }

    @RequestScoped
    public class Observer1
    {
        public void visit(@Observes @Priority(1) VisitorCollectorEvent visitorCollector)
        {
            sleep(10L);
            visitorCollector.visiting(getClass().getSimpleName());
        }
    }

    private void sleep(long time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException e)
        {
            // ignore
        }
    }
}
