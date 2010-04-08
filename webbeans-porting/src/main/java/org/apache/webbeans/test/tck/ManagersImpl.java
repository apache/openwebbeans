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

import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.container.InjectableBeanManager;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.jboss.jsr299.tck.spi.Managers;
import org.jboss.testharness.api.DeploymentException;

public class ManagersImpl implements Managers
{
    private static InjectableBeanManager beanManager;
    
    public static void cleanUp()
    {
        beanManager = null;
    }
    
    public BeanManager getManager()
    {
        if(beanManager == null)
        {
            beanManager = new InjectableBeanManager();
        }
        
        return beanManager; 
    }

    public boolean isDefinitionError(DeploymentException deploymentException)
    {
        Throwable cause = deploymentException.getCause();
        
        if(DefinitionException.class.isAssignableFrom(cause.getClass()))
        {
            return true;
        }
        
        return false;
    }

    public boolean isDeploymentError(DeploymentException deploymentException)
    {
        Throwable cause = deploymentException.getCause();
        
        if(DeploymentException.class.isAssignableFrom(cause.getClass()))
        {
            return true;
        }
        
        return false;
    }

}
