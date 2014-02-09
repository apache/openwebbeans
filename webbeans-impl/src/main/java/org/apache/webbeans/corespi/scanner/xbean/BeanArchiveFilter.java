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
package org.apache.webbeans.corespi.scanner.xbean;

import java.util.List;

import org.apache.webbeans.spi.BeanArchiveService.BeanArchiveInformation;
import org.apache.webbeans.spi.BeanArchiveService.BeanDiscoveryMode;
import org.apache.xbean.finder.filter.Filter;

/**
 * Filter which knows about BeanArchive scan modes
 */
public class BeanArchiveFilter implements Filter
{
    private final BeanArchiveInformation beanArchiveInfo;
    private final boolean scanNone;

    private List<String> urlClasses;

    public BeanArchiveFilter(BeanArchiveInformation beanArchiveInfo, List<String> urlClasses)
    {
        this.beanArchiveInfo = beanArchiveInfo;
        this.urlClasses = urlClasses;
        BeanDiscoveryMode discoveryMode = beanArchiveInfo.getBeanDiscoveryMode();

        scanNone = BeanDiscoveryMode.NONE.equals(discoveryMode);
    }

    @Override
    public boolean accept(String name)
    {
        if (scanNone)
        {
            return false;
        }

        if (beanArchiveInfo.isClassExcluded(name))
        {
            return false;
        }

            urlClasses.add(name);
            return true;
    }

}
