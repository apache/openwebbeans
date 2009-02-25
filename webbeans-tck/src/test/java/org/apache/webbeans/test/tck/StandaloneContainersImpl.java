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

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.List;

import org.jboss.jsr299.tck.api.DeploymentException;
import org.jboss.jsr299.tck.spi.StandaloneContainers;

public class StandaloneContainersImpl implements StandaloneContainers {

    public void cleanup() {
        // TODO Auto-generated method stub

    }

    public void deploy( Iterable<Class<?>> classes ) throws DeploymentException {
        // TODO Auto-generated method stub

    }

    public void deploy( List<Class<? extends Annotation>> enabledDeploymentTypes, Iterable<Class<?>> classes )
            throws DeploymentException {
        // TODO Auto-generated method stub

    }

    public void deploy( Iterable<Class<?>> classes, Iterable<URL> beansXmls ) throws DeploymentException {
        // TODO Auto-generated method stub

    }

    public void setup() {
        // TODO Auto-generated method stub

    }

    public void undeploy() {
        // TODO Auto-generated method stub

    }

}
