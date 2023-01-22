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
package org.apache.webbeans.test.injection.generics;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

@Typed
public class Bar<A, B> {

    @Inject
    //X @GenericQualifier
    private Baz<A> baz;

    @Inject
    @GenericQualifier
    private A a;

    @Inject
    private Baz<List<B>> bBazList;

    private A[] aArray;
    private Baz<A> aBazEvent;
    private A aObserverInjectionPoint;

    @Inject
    public void setAArray(A[] aArray) {
        this.aArray = aArray;
    }

    public void observeBaz(@Observes Baz<A> baz, @GenericQualifier A a) {
        this.aBazEvent = baz;
        this.aObserverInjectionPoint = a;
    }

    public Baz<A> getBaz() {
        return this.baz;
    }

    public Baz<List<B>> getBBazList() {
        return this.bBazList;
    }

    public A getA() {
        return this.a;
    }

    public A[] getAArray() {
        return this.aArray;
    }

    public Baz<A> getABazEvent() {
        return this.aBazEvent;
    }

    public A getAObserverInjectionPoint() {
        return this.aObserverInjectionPoint;
    }
}