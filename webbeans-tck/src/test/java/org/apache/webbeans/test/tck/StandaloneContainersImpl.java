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

import javax.inject.Production;

import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.test.tck.mock.TCKManager;
import org.apache.webbeans.test.tck.mock.TCKWebBeansContainerDeployer;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;
import org.jboss.jsr299.tck.api.DeploymentException;
import org.jboss.jsr299.tck.spi.StandaloneContainers;

public class StandaloneContainersImpl implements StandaloneContainers
{
    private TCKWebBeansContainerDeployer tckContainerDeployer = null;
    private TCKManager tckManager = null;

    public void cleanup()
    {
        this.tckContainerDeployer = null;
        tckManager = null;
    }

    public void deploy(Iterable<Class<?>> classes) throws DeploymentException
    {
        Iterator<Class<?>> it = classes.iterator();
        while(it.hasNext())
        {
            tckContainerDeployer.addBeanClass(it.next());
        }
        
        tckContainerDeployer.deploy(null);
    }


    public void deploy(Iterable<Class<?>> classes, Iterable<URL> beansXmls) throws DeploymentException
    {
        Iterator<Class<?>> it = classes.iterator();
        while(it.hasNext())
        {
            tckContainerDeployer.addBeanClass(it.next());
        }
        
        Iterator<URL> itUrl = beansXmls.iterator();
        while(itUrl.hasNext())
        {
            tckContainerDeployer.addBeanXml(itUrl.next());
        }
        
        tckContainerDeployer.deploy(null);
    }

    public void setup()
    {
        this.tckContainerDeployer = new TCKWebBeansContainerDeployer(new WebBeansXMLConfigurator());
        this.tckManager = TCKManager.getInstance();
        
        DeploymentTypeManager.getInstance().addNewDeploymentType(Production.class, 1);
    }

    public void undeploy()
    {
        this.tckManager.clear();
        this.tckContainerDeployer.clear();
        
    }

}
