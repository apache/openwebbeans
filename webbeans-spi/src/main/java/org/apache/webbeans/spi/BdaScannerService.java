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

import java.util.Map;
import java.util.Set;

/**
 * A BdaScannerService is a ScannerService with more
 * fine grained information.
 * It knows all the jars and classpath URLs and the classes they contain
 */
public interface BdaScannerService extends ScannerService
{

    /**
     * Scan all the classpath which should be handled by this BeanManager
     * and return the Map of information about each found classpath entry.
     * The key is the {@link org.apache.webbeans.spi.BeanArchiveService.BeanArchiveInformation}
     * of the scanned classpath, and the value is the Set of found bean classes.
     */
    public Map<BeanArchiveService.BeanArchiveInformation, Set<Class<?>>> getBeanClassesPerBda();
}
