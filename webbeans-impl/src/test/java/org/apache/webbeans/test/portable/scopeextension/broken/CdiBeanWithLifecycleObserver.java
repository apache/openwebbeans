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
package org.apache.webbeans.test.portable.scopeextension.broken;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;

/**
 * This is in fact a broken CDI bean as it implements
 * a CDI Container Lifecycle observer.
 * We must ensure that this bean doesn't get called during bootstrap.
 * The CDI spec says nothing about whether we must throw a DeploymentException
 * or not, so we leave this out.
 */
@Dependent
public class CdiBeanWithLifecycleObserver
{
    public static boolean beforeBeanDiscoveryCalled = false;
    public static boolean afterBeanDiscoveryCalled = false;

    private int meaningOfLife = 42;

    public int getMeaningOfLife()
    {
        return meaningOfLife;
    }

    public void setMeaningOfLife(int meaningOfLife)
    {
        this.meaningOfLife = meaningOfLife;
    }

    /**
     * This method must not be called by the container during bootstrap
     */
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd)
    {
        beforeBeanDiscoveryCalled = true;
    }

    /**
     * This method must not be called by the container during bootstrap
     */
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd)
    {
        afterBeanDiscoveryCalled = true;
    }
}
