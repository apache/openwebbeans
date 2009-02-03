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
import java.util.Arrays;
import java.util.List;

import javax.webbeans.manager.Manager;

import org.apache.log4j.Logger;
import org.apache.webbeans.test.mock.MockManager;
import org.jboss.webbeans.tck.api.Containers;

public class ContainersImpl implements Containers {

    private Logger logger = Logger.getLogger(ContainersImpl.class);

    /** {@inheritDoc} */
    public Manager deploy( Class<?>... classes ) {
        Iterable<Class<?>> webbeanClasses = Arrays.asList(classes);
        
        for (Class<?> webbeanClass : webbeanClasses)
        {
            logger.debug("registering WebBean class " + webbeanClass);
            
            //X TODO create beans!
            //X it is currently not clear when this is called and when it interferes with the functions from BeansImpl!

        }
        
        return MockManager.getInstance();
    }

    public Manager deploy( List<Class<? extends Annotation>> enabledDeploymentTypes, Class<?>... classes ) {
        //X TODO Auto-generated method stub
        return null;
    }

}
