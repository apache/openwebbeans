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
package org.apache.webbeans.context;

import org.apache.webbeans.container.SerializableBeanVault;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import java.lang.annotation.Annotation;

/**
 *
 */
public class CustomPassivatingContextImpl implements Context
{
    protected final SerializableBeanVault sbv;
    protected final Context context;


    public CustomPassivatingContextImpl(SerializableBeanVault sbv, Context context)
    {
        this.sbv = sbv;
        this.context = context;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext)
    {
        return context.get(sbv.getSerializableBean(contextual), creationalContext);
    }

    @Override
    public <T> T get(Contextual<T> contextual)
    {
        return context.get(sbv.getSerializableBean(contextual));
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return context.getScope();
    }

    @Override
    public boolean isActive()
    {
        return context.isActive();
    }
}
