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
package org.apache.webbeans.portable.events;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.ProcessBeanAttributes;

public class ProcessBeanAttributesImpl<T> extends EventBase implements ProcessBeanAttributes<T>
{
    private Annotated annotated;
    private BeanAttributes<T> attributes;
    private boolean veto = false;
    private Throwable definitionError = null;

    public ProcessBeanAttributesImpl(final Annotated annotated, final BeanAttributes<T> attributes)
    {
        this.annotated = annotated;
        this.attributes = attributes;
    }

    @Override
    public Annotated getAnnotated()
    {
        checkState();
        return annotated;
    }

    @Override
    public BeanAttributes<T> getBeanAttributes()
    {
        checkState();
        return attributes;
    }

    @Override
    public void setBeanAttributes(final BeanAttributes<T> tBeanAttributes)
    {
        checkState();
        attributes = tBeanAttributes;
    }

    @Override
    public void veto()
    {
        checkState();
        veto = true;
    }

    @Override
    public void addDefinitionError(final Throwable throwable)
    {
        checkState();
        definitionError = throwable;
    }

    public BeanAttributes<T> getAttributes()
    {
        return attributes;
    }

    public boolean isVeto()
    {
        return veto;
    }

    public Throwable getDefinitionError()
    {
        return definitionError;
    }
}
