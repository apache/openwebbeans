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

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 */
public class OwbStandaloneContainer implements DeployableContainer<OwbStandaloneConfiguration>
{
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class<OwbStandaloneConfiguration> getConfigurationClass()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setup(OwbStandaloneConfiguration owbStandaloneConfiguration)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void start() throws LifecycleException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void stop() throws LifecycleException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ProtocolDescription getDefaultProtocol()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void undeploy(Archive<?> archive) throws DeploymentException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deploy(Descriptor descriptor) throws DeploymentException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void undeploy(Descriptor descriptor) throws DeploymentException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
