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
package org.apache.webbeans.test.portable.javaee;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

public class MockInstance
{
    private @Inject SampleBean sample;
    
    private SampleBean viaMethod;
    
    private @Inject BeanManager beanManager;
    
    @Inject
    public void init(SampleBean sample)
    {
        this.viaMethod = sample;
    }

    public SampleBean getSample()
    {
        return sample;
    }

    public void setSample(SampleBean sample)
    {
        this.sample = sample;
    }

    public SampleBean getViaMethod()
    {
        return viaMethod;
    }

    public void setViaMethod(SampleBean viaMethod)
    {
        this.viaMethod = viaMethod;
    }

    public BeanManager getBeanManager()
    {
        return beanManager;
    }

    public void setBeanManager(BeanManager beanManager)
    {
        this.beanManager = beanManager;
    }
    
    
}
