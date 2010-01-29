/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.spi;

import java.net.URL;
import java.util.Set;


/**
 * This SPI is for abstracting the class scanning.  
 *
 * In a production environment Many different modules need to perform 
 * class scanning (EJB, JSF, JPA, ...). This SPI allows us to only have one 
 * central class scanner for the whole application server
 * which only performs the scanning once at startup of each WebApp.
 * 
 * @version $Rev$ $Date$
 */
public interface ScannerService
{
    /**
     * Any initializtion action that is
     * required by the implementors. 
     * @param object initialization object
     */
    public void init(Object object);
    
    /**
     * Perform the actual class scanning.
     * @throws WebBeansDeploymentException
     */
    public void scan();

    
    /**
     * Gets xml configuration files that are occured
     * in the deployment archives.
     * @return the locations of the beans.xml files. 
     */
    public Set<URL> getBeanXmls();
    
    /**
     * Gets beans classes that are found in the
     * deployment archives. 
     * @return bean classes
     */
    public Set<Class<?>> getBeanClasses();
}
