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
package org.apache.webbeans.component.creation;

import org.apache.webbeans.component.ResourceProvider;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.ProviderBasedProducerFactory;
import org.apache.webbeans.portable.ResourceProducer;
import org.apache.webbeans.spi.api.ResourceReference;

import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Producer;

public class ResourceProducerFactory<P> extends ProviderBasedProducerFactory<P>
{
    private final AnnotatedField<? super P> member;
    private final Bean<P> owner;
    private final ResourceReference<?, ?> ref;

    public ResourceProducerFactory(boolean dependent, Bean<P> owner,
                                   ResourceProvider<P> provider, Class<P> clazz,
                                   WebBeansContext wbc, AnnotatedField<? super P> annotatedMember,
                                   ResourceReference<?, ?> resourceRef)
    {
        super(dependent, provider, clazz, wbc);
        this.member = annotatedMember;
        this.owner = owner;
        this.ref = resourceRef;
    }

    @Override
    public <T> Producer<T> createProducer(Bean<T> bean)
    {
        return webBeansContext.getWebBeansUtil().fireProcessProducerEvent(
                new ResourceProducer<T, P>(owner, member, webBeansContext, ref), member);
    }
}
