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
package org.apache.webbeans.xml;

import java.util.ArrayList;
import java.util.List;

import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.BeanArchiveService.BeanDiscoveryMode;

public class DefaultBeanArchiveInformation implements BeanArchiveService.BeanArchiveInformation
{
    private String version;
    private BeanDiscoveryMode beanDiscoveryMode;
    private List<String> interceptors = new ArrayList<String>();
    private List<String> decorators = new ArrayList<String>();
    private List<String> alternativeClasses = new ArrayList<String>();
    private List<String> alternativeStereotypes = new ArrayList<String>();


    @Override
    public BeanDiscoveryMode getBeanDiscoveryMode()
    {
        return beanDiscoveryMode;
    }

    @Override
    public boolean isExcluded(String classOrPath)
    {
        return false; //X TODO
    }

    @Override
    public String getVersion()
    {
        return version;
    }

    @Override
    public List<String> getInterceptors()
    {
        return interceptors;
    }

    @Override
    public List<String> getDecorators()
    {
        return decorators;
    }

    @Override
    public List<String> getAlternativeClasses()
    {
        return alternativeClasses;
    }

    @Override
    public List<String> getAlternativeStereotypes()
    {
        return alternativeStereotypes;
    }


    public void setVersion(String version)
    {
        this.version = version;
    }

    public void setBeanDiscoveryMode(BeanDiscoveryMode beanDiscoveryMode)
    {
        this.beanDiscoveryMode = beanDiscoveryMode;
    }

    public void setInterceptors(List<String> interceptors)
    {
        this.interceptors = interceptors;
    }

    public void setDecorators(List<String> decorators)
    {
        this.decorators = decorators;
    }

    public void setAlternativeClasses(List<String> alternativeClasses)
    {
        this.alternativeClasses = alternativeClasses;
    }

    public void setAlternativeStereotypes(List<String> alternativeStereotypes)
    {
        this.alternativeStereotypes = alternativeStereotypes;
    }
}
