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
package org.apache.webbeans.service;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.LoaderService;
import org.apache.webbeans.util.WebBeansUtil;

import javax.enterprise.inject.spi.Extension;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * Default implementation which delegates to the s{@link ServiceLoader}.
 */
public class DefaultLoaderService implements LoaderService
{
    @Override
    public <T> List<T> load(Class<T> serviceType)
    {
        return load(serviceType, WebBeansUtil.getCurrentClassLoader());
    }

    @Override
    public <T> List<T> load(Class<T> serviceType, ClassLoader classLoader)
    {
        try
        {
            Stream<T> stream = StreamSupport.stream(ServiceLoader.load(serviceType, classLoader).spliterator(), false);
            if (Extension.class == serviceType)
            {
                return mapExtensions(stream).collect(toList());
            }
            // OWBPlugin
            return stream.collect(toList());
        }
        catch (Error error)
        {
            // WTF! ServiceLoader is cool, but THAT is utter crap: it throws some Errors!
            WebBeansLoggerFacade.getLogger(DefaultLoaderService.class)
                    .log(Level.SEVERE, "Problem while loading CDI Extensions", error);
            throw new WebBeansConfigurationException("Problem while loading CDI Extensions", error);
        }
    }

    // enables to easily extend the loader to customize extensions:
    // 1. filter some undesired extensions programmatically
    // 2. remap some extensions (to drop some events or wrap them for ex)
    protected  <T> Stream<T> mapExtensions(final Stream<T> stream)
    {
        return stream;
    }
}
