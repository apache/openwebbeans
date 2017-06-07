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
package org.apache.webbeans.portable.events.discovery;

import javax.enterprise.inject.spi.Extension;

import org.apache.webbeans.configurator.AnnotatedTypeConfiguratorImpl;

/**
 * Hold information for lifecycle Events which can modify or add multiple
 * {@link javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator}
 */
public class AnnotatedTypeConfiguratorHolder<T>
{
    private final Extension extension;
    private final String id;
    private final AnnotatedTypeConfiguratorImpl<T> annotatedTypeConfigurator;

    public AnnotatedTypeConfiguratorHolder(Extension extension, String id, AnnotatedTypeConfiguratorImpl<T> annotatedTypeConfigurator)
    {
        this.extension = extension;
        this.id = id;
        this.annotatedTypeConfigurator = annotatedTypeConfigurator;
    }

    public Extension getExtension()
    {
        return extension;
    }

    public String getId()
    {
        return id;
    }

    public AnnotatedTypeConfiguratorImpl<T> getAnnotatedTypeConfigurator()
    {
        return annotatedTypeConfigurator;
    }
}
