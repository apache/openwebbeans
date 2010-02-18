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
package org.apache.webbeans.spi;

import java.util.Properties;

import javax.enterprise.inject.spi.BeanManager;

/**
 * JSR-299 Container lifecycle.
 *  
 * @version $Rev$ $Date$
 *
 */
public interface ContainerLifecycle
{
    /**
     * Initialize lifecycle.
     * <p>
     * Implementors can configure their
     * initialization specific actions here.
     * </p>
     * @param properties any properties
     */
    public void init(Properties properties);
    
    /**
     * Starts container. It discovers all beans
     * in the deployment archive. 
     * <p>
     * For Java EE artifact deployment, it scans all classes
     * and libraries in the deployment archive. There are several
     * types of deployment arhives;
     * <ul>
     *  <li>EAR archive</li>
     *  <li>EJB archive</li>
     *  <li>WAR archive</li>
     *  <li>RAR archive</li>
     *  <li>Application client archive. <b>OPTIONAL</b></li> 
     * </ul>
     * </p>
     * 
     * <p>
     * Container uses metadata discovery SPI for scanning archives
     * and act accordingly. If there is an exception while starting,
     * it must abort the deployment and provides information to the
     * developer.
     * </p>
     * @param startupObject any startup object.
     * @throws Exception exception thrown by startup
     */
    public void start(Object startupObject) throws Exception;
    
    /**
     * Stops means that container removes all bean instances
     * it store, remove contexts and does necessary final actions.
     * @param endObject any onject provided by implementors
     */
    public void stop(Object endObject);
    
    /**
     * Gets deployment bean manager instance. There is 1-1 correspondence
     * between bean manager and deployment archive.
     * @return deployment {@link BeanManager} instance
     */
    public BeanManager getBeanManager();

}
