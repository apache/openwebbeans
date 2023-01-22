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
package org.apache.webbeans.test.producer.beans;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.Assert;

/**
 * The bean which contains a few producer methods which are protected and private.
 * We use a NormalScope which means the container gets a proxy which it needs to
 * unwrap on the fly.
 */
@RequestScoped
public class SampleProducerOwner
{
    private @Inject SomeUserBean user;

    @Produces
    protected ProtectedProducedBean createProtected()
    {
        // this is only available if proper injection got performed
        Assert.assertEquals("Hans", user.getName());

        return new ProtectedProducedBean();
    }

    @Produces
    private PrivateProducedBean createPrivate()
    {
        // this is only available if proper injection got performed
        Assert.assertEquals("Hans", user.getName());

        return new PrivateProducedBean();
    }
}
