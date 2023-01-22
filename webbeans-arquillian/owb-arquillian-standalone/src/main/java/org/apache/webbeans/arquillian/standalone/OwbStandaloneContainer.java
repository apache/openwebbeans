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
package org.apache.webbeans.arquillian.standalone;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import jakarta.enterprise.inject.spi.BeanManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

/**
 */
public class OwbStandaloneContainer implements DeployableContainer<OwbStandaloneConfiguration>
{
    private static final Logger LOG = Logger.getLogger(OwbStandaloneContainer.class.getName());


    @Inject
    @DeploymentScoped
    private InstanceProducer<ContainerLifecycle> lifecycleProducer;

    @Inject
    @DeploymentScoped
    private InstanceProducer<BeanManager> beanManagerProducer;

    private OwbArquillianSingletonService singletonService;
    private WebBeansContext webBeansContext;

    private final ThreadLocal<ClassLoader> originalLoader = new ThreadLocal<>();
    private boolean useOnlyArchiveResources;
    private Collection<String> useOnlyArchiveResourcesExcludes = new ArrayList<>();

    @Override
    public Class<OwbStandaloneConfiguration> getConfigurationClass()
    {
        return OwbStandaloneConfiguration.class;
    }

    @Override
    public ProtocolDescription getDefaultProtocol()
    {
        return new ProtocolDescription("Local");
    }

    @Override
    public void setup(OwbStandaloneConfiguration owbStandaloneConfiguration)
    {
        LOG.fine("OpenWebBeans Arquillian setup started");

        singletonService = new OwbArquillianSingletonService(owbStandaloneConfiguration.properties());
        WebBeansFinder.setSingletonService(singletonService);

        useOnlyArchiveResources = owbStandaloneConfiguration.isUseOnlyArchiveResources();
        if (useOnlyArchiveResources && owbStandaloneConfiguration.getUseOnlyArchiveResourcesExcludes() != null)
        {
            useOnlyArchiveResourcesExcludes = Arrays.asList(owbStandaloneConfiguration.getUseOnlyArchiveResourcesExcludes().split(","));
        }

    }

    @Override
    public void start() throws LifecycleException
    {
        LOG.fine("OpenWebBeans Arquillian starting");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException
    {
        singletonService.initOwb();

        webBeansContext = WebBeansContext.getInstance();

        LOG.fine("OpenWebBeans Arquillian starting deployment");

        ContainerLifecycle lifecycle = webBeansContext.getService(ContainerLifecycle.class);

        lifecycleProducer.set(lifecycle);

        OwbArquillianScannerService dummyScannerService = (OwbArquillianScannerService) webBeansContext.getScannerService();
        dummyScannerService.setArchive(archive);

        ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
        originalLoader.set(parentLoader);
        Thread.currentThread().setContextClassLoader(new OwbSWClassLoader(parentLoader, archive, useOnlyArchiveResources, useOnlyArchiveResourcesExcludes));

        lifecycle.startApplication(null);

        // finally make the BeanManager available to Arquillian, but only if the boot succeeded
        beanManagerProducer.set(lifecycle.getBeanManager());

        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException
    {
        LOG.fine("OpenWebBeans Arquillian undeploying");

        OwbArquillianScannerService dummyScannerService = (OwbArquillianScannerService) webBeansContext.getScannerService();
        dummyScannerService.clear();
        ContainerLifecycle lifecycle = lifecycleProducer.get();
        if (lifecycle != null)
        {
            // end the session lifecycle

            lifecycle.stopApplication(null);
        }

        ClassLoader current = Thread.currentThread().getContextClassLoader();
        if (OwbSWClassLoader.class.isInstance(current))
        { // should be the case
            try
            {
                OwbSWClassLoader.class.cast(current).close();
            }
            catch (IOException e)
            {
                // no-op
            }
        }
        Thread.currentThread().setContextClassLoader(originalLoader.get());
        originalLoader.remove();
    }

    @Override
    public void stop() throws LifecycleException
    {
        LOG.fine("OpenWebBeans Arquillian stopping");
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException
    {
        throw new UnsupportedOperationException("Deployment of Descriptors is not supported in owb-arquillian-standalone!");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException
    {
        throw new UnsupportedOperationException("Deployment of Descriptors is not supported in owb-arquillian-standalone!");
    }


}
