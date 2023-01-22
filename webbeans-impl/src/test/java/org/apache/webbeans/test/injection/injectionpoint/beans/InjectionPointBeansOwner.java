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
package org.apache.webbeans.test.injection.injectionpoint.beans;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

public class InjectionPointBeansOwner {

    @Inject
    private ConstructorInjectionPointOwner constructorInjection;

    @Inject
    private FieldInjectionPointOwner fieldInjection;

    @Inject
    private MethodInjectionPointOwner methodInjection;

    @Inject
    private ProducerMethodInjectionPointOwner producerMethodInjection;

    @Inject
    private Instance<ConstructorInjectionPointOwner> constructorInjectionInstance;

    @Inject
    private Instance<FieldInjectionPointOwner> fieldInjectionInstance;

    @Inject
    private Instance<MethodInjectionPointOwner> methodInjectionInstance;

    @Inject
    private Instance<ProducerMethodInjectionPointOwner> producerMethodInjectionInstance;

    @Inject
    private Event<StringBuilder> observerInjection;

    @Inject
    private Event<StringBuffer> parameterizedObserverInjection;
    
    public String getConstructorInjectionName() {
        return constructorInjection.getName();
    }
    
    public String getFieldInjectionName() {
        return fieldInjection.getName();
    }
    
    public String getMethodInjectionName() {
        return methodInjection.getName();
    }
    
    public String getProducerMethodInjectionName() {
        return producerMethodInjection.getName();
    }
    
    public String getConstructorInjectionInstanceName() {
        return constructorInjectionInstance.get().getName();
    }
    
    public String getFieldInjectionInstanceName() {
        return fieldInjectionInstance.get().getName();
    }
    
    public String getMethodInjectionInstanceName() {
        return methodInjectionInstance.get().getName();
    }
    
    public String getProducerMethodInjectionInstanceName() {
        return producerMethodInjectionInstance.get().getName();
    }

    public String getObserverInjectionName() {
        StringBuilder name = new StringBuilder();
        observerInjection.fire(name);
        return name.toString();
    }

    public String getParameterizedObserverInjectionName() {
        StringBuffer name = new StringBuffer();
        parameterizedObserverInjection.fire(name);
        return name.toString();
    }
}
