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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.testharness.api.DeploymentException;
import org.jboss.testharness.spi.Containers;


public class ContainersImpl implements Containers
{

  //  private Logger logger = Logger.getLogger(ContainersImpl.class);

    public void cleanup() throws IOException
    {

    }

    public boolean deploy(InputStream archive, String name)
    {
        try
        {
            if(archive.available() > 0)
            {
                File file = new File("target/container/" + name);
                FileOutputStream os = new FileOutputStream(file);            
                byte temp[] = new byte[512];
                
                while(archive.read(temp) != -1)
                {
                    os.write(temp);
                }            
   
            }
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return false;
    }

    public void setup() throws IOException
    {

    }

    public void undeploy(String name) throws IOException
    {

    }

    public DeploymentException getDeploymentException()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
