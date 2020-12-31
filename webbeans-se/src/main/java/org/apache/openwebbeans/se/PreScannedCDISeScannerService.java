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
package org.apache.openwebbeans.se;

import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.corespi.scanner.xbean.CdiArchive;
import org.apache.webbeans.corespi.scanner.xbean.OwbAnnotationFinder;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.xbean.finder.AnnotationFinder;

import java.net.URL;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class PreScannedCDISeScannerService extends CDISeScannerService
{
    @Override
    protected void configure()
    {
        // no-op
    }

    @Override
    protected void registerBeanArchives(final ClassLoader loader)
    {
        // no-op
    }

    @Override
    protected AnnotationFinder initFinder()
    {
        if (finder != null)
        {
            return finder;
        }

        // todo: support to read beanDeploymentUrls from the conf as well
        //       -> for now we use a full programmatic deployment (single in mem archive)
        final WebBeansContext webBeansContext = webBeansContext();
        final OpenWebBeansConfiguration conf = webBeansContext.getOpenWebBeansConfiguration();
        final String confKeyBase = getClass().getName() + ".";
        final String classes = conf.getProperty(confKeyBase + "classes");
        final ClassLoader loader = WebBeansUtil.getCurrentClassLoader();
        final Class<?>[] reflectClasses = Stream.of(classes.split(",")).map(it ->
        {
            try
            {
                return loader.loadClass(it);
            }
            catch (final ClassNotFoundException e)
            {
                throw new IllegalArgumentException("Can't load '" + it + "'", e);
            }
        }).toArray(Class[]::new);
        addClassesDeploymentUrl();
        final BeanArchiveService beanArchiveService = webBeansContext.getBeanArchiveService();
        archive = new CdiArchive(
                beanArchiveService, WebBeansUtil.getCurrentClassLoader(),
                emptyMap(), null, getAdditionalArchive());
        final Map.Entry<String, URL> deplUrl = getBeanDeploymentUrls().entrySet().iterator().next();
        archive.classesByUrl().put(
                deplUrl.getKey(),
                new CdiArchive.FoundClasses(
                        deplUrl.getValue(),
                        Stream.of(classes.split(",")).collect(toList()),
                        beanArchiveService.getBeanArchiveInformation(deplUrl.getValue())));
        finder = new OwbAnnotationFinder(reflectClasses);
        return finder;
    }

    @Override
    public void classes(final Class<?>[] classes)
    {
        // no-op
    }

    @Override
    public void packages(final boolean recursive, final Class<?>[] markers)
    {
        // no-op
    }

    @Override
    public void packages(final boolean recursive, final Package[] packages)
    {
        // no-op
    }
}
