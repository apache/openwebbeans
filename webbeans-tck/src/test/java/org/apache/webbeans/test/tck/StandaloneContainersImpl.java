/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.test.tck;

import java.net.URL;
import java.util.Iterator;

import org.apache.webbeans.lifecycle.WebBeansLifeCycle;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.spi.deployer.MetaDataDiscoveryService;
import org.apache.webbeans.test.mock.MockServletContextEvent;
import org.apache.webbeans.test.tck.mock.TCKMetaDataDiscoveryImpl;
import org.jboss.testharness.api.DeploymentException;
import org.jboss.testharness.spi.StandaloneContainers;

public class StandaloneContainersImpl implements StandaloneContainers
{
    private WebBeansLifeCycle lifeCycle = null;
    private MockServletContextEvent servletContextEvent;
        
    public void cleanup()
    {
        
    }

    public void deploy(Iterable<Class<?>> classes) throws DeploymentException
    {
        TCKMetaDataDiscoveryImpl discovery = (TCKMetaDataDiscoveryImpl)ServiceLoader.getService(MetaDataDiscoveryService.class);
        this.lifeCycle = new WebBeansLifeCycle();
        
        try
        {
            Iterator<Class<?>> it = classes.iterator();
            while(it.hasNext())
            {
                discovery.addBeanClass(it.next());
            }
            
            this.lifeCycle.applicationStarted(servletContextEvent);
            
        }catch(Exception e)
        {
            throw new DeploymentException(e.getMessage());
        }
        
    }


    public boolean deploy(Iterable<Class<?>> classes, Iterable<URL> beansXmls)
    {
        try
        {
            TCKMetaDataDiscoveryImpl discovery = (TCKMetaDataDiscoveryImpl)ServiceLoader.getService(MetaDataDiscoveryService.class);
            
            this.lifeCycle = new WebBeansLifeCycle();
            
            Iterator<Class<?>> it = classes.iterator();
            while(it.hasNext())
            {
                discovery.addBeanClass(it.next());
            }
            
            Iterator<URL> itUrl = beansXmls.iterator();
            while(itUrl.hasNext())
            {
                discovery.addBeanXml(itUrl.next());
            }
            
            this.lifeCycle.applicationStarted(servletContextEvent);            
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        return false;
    }

    public void setup()
    {
        this.servletContextEvent = new MockServletContextEvent();
    }

    public void undeploy()
    {
        this.lifeCycle.applicationEnded(this.servletContextEvent);
    }

    public DeploymentException getDeploymentException()
    {
        return new DeploymentException("StandaloneContainer");
    }

}
