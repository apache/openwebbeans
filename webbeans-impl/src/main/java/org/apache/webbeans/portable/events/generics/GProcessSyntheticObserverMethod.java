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
package org.apache.webbeans.portable.events.generics;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessSyntheticObserverMethod;

import org.apache.webbeans.config.WebBeansContext;


@SuppressWarnings("unchecked")
public class GProcessSyntheticObserverMethod extends GProcessObserverMethod implements ProcessSyntheticObserverMethod
{
    private final Extension source;

    public GProcessSyntheticObserverMethod(WebBeansContext webBeansContext,
                                           AnnotatedMethod<?> annotatedMethod,
                                           ObserverMethod<?> observerMethod,
                                           Extension source)
    {
        super(webBeansContext, annotatedMethod, observerMethod);
        this.source = source;
    }

    @Override
    public Extension getSource()
    {
        return source;
    }
}
