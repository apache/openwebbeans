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
package org.apache.webbeans.event;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.portable.events.discovery.ExtensionAware;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import java.lang.reflect.InvocationTargetException;

public class ContainerEventObserverMethodImpl<T> extends ObserverMethodImpl<T>
{
    public ContainerEventObserverMethodImpl(final AbstractOwbBean<?> bean, final AnnotatedMethod<T> annotatedObserverMethod,
                                            final AnnotatedParameter<T> annotatedObservesParameter)
    {
        super(bean, annotatedObserverMethod, annotatedObservesParameter);
    }

    @Override
    protected void invoke(final Object object, final Object[] args) throws IllegalAccessException, InvocationTargetException
    {
        ExtensionAware extensionAware = null;
        if (args.length > 0)
        {
            if (ExtensionAware.class.isInstance(args[0]))
            {
                extensionAware = ExtensionAware.class.cast(args[0]);
                extensionAware.setExtension(object);
            }
        }
        super.invoke(object, args);
        if (extensionAware != null)
        {
            ExtensionAware.class.cast(extensionAware).setExtension(null);
        }
    }
}
