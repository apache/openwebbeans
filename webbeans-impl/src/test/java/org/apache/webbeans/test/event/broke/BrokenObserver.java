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
package org.apache.webbeans.test.event.broke;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.ObserverMethod;


@SuppressWarnings("unchecked")
public class BrokenObserver implements ObserverMethod<BrokenEvent>
{

    @Override
    public void notify(BrokenEvent event)
    {

    }

    @Override
    public Class<?> getBeanClass() {
        return null;
    }

    @Override
    public Set<Annotation> getObservedQualifiers() {
        return null;
    }

    @Override
    public Type getObservedType() {
        return null;
    }

    @Override
    public Reception getReception() {
        return null;
    }

    @Override
    public TransactionPhase getTransactionPhase() {
        return null;
    }

}
