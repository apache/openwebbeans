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
package org.apache.webbeans.test.concepts.alternatives.common;

import java.util.Iterator;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

public class SimpleInjectionTarget
{

    @Inject
    private SimpleInterface simpleInterface1;

    @Inject
    private Instance<SimpleInterface> simpleInterface2Instance;

    private SimpleInterface simpleInterface2;
    
    @PostConstruct
    public void initialize()
    {
        simpleInterface2 = simpleInterface2Instance.get();
    }
    
    public SimpleInterface getSimpleInterface1()
    {
        return simpleInterface1;
    }

    public SimpleInterface getSimpleInterface2()
    {
        return simpleInterface2;
    }

    public boolean isSimpleInterfaceAmbiguous() {
        return simpleInterface2Instance.isAmbiguous();
    }

    public Iterator<SimpleInterface> getSimpleInterfaceInstances() {
        return simpleInterface2Instance.iterator();
    }
}
