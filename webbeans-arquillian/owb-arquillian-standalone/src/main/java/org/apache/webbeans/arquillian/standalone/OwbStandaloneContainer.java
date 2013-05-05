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

import javax.enterprise.inject.spi.BeanManager;

import java.util.logging.Logger;

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

        singletonService = new OwbArquillianSingletonService();
        WebBeansFinder.setSingletonService(singletonService);

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
        beanManagerProducer.set(lifecycle.getBeanManager());

        OwbArquillianScannerService dummyScannerService = (OwbArquillianScannerService) webBeansContext.getScannerService();
        dummyScannerService.setArchive(archive);

        lifecycle.startApplication(null);

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
    }

    @Override
    public void stop() throws LifecycleException
    {
        LOG.fine("OpenWebBeans Arquillian stopping");
        //To change body of implemented methods use File | Settings | File Templates.
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
