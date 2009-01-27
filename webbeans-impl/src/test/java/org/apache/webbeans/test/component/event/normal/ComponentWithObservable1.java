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
package org.apache.webbeans.test.component.event.normal;

import java.lang.annotation.Annotation;

import javax.context.RequestScoped;
import javax.event.Event;
import javax.event.Fires;
import javax.inject.Current;
import javax.inject.Production;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.test.event.LoggedInEvent;

@RequestScoped
@Production
@Current
public class ComponentWithObservable1
{
    private @Fires
    Event<LoggedInEvent> event;

    public void afterLoggedIn()
    {
        LoggedInEvent loggedIn = new LoggedInEvent("Gurkan");

        Annotation[] anns = new Annotation[1];
        anns[0] = new CurrentLiteral();

        event.fire(loggedIn, anns);
    }

}
