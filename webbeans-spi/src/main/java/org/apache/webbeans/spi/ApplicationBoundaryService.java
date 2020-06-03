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

/**
 * This service allows EE containers to define an 'outer boundary' for their applications.
 *
 * E.g. if the class EntityManager resides in a shared container lib folder then any
 * CDI proxy for it should still at maximum use the Applications ClassLoader to load the proxy for it.
 * This needs to be done to ensure that we don't create a class leak by loading the class with the
 * container ClassLoader but are not able to throw this CL away.
 */
public interface ApplicationBoundaryService
{
    /**
     * Please note that the ApplicationClassLoader always have to 'see' the internal OWB classes
     * like BeanManagerImpl or the proxy marker interfaces (
     * @return The 'outermost' ClassLoader of that Application
     */
    ClassLoader getApplicationClassLoader();

    /**
     * @return the ClassLoader which shall get used to e.g. proxy that very class.
     */
    ClassLoader getBoundaryClassLoader(Class<?> classToProxy);

}
