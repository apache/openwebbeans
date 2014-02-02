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

    /** Either an excluded class or an excluded .* path */
    private List<String> excludedClasses = null;

    /** Exclude all subpackages (exclude= .**) */
    private List<String> excludedPackages = null;



    @Override
    public BeanDiscoveryMode getBeanDiscoveryMode()
    {
        return beanDiscoveryMode;
    }

    @Override
    public boolean isClassExcluded(String clazz)
    {
        boolean isExcluded = isPackageExcluded(clazz);

        if  (!isExcluded && excludedClasses != null)
        {
            for (String excludedClass : excludedClasses)
            {
                if (clazz.startsWith(excludedClass))
                {
                    if (clazz.length() > excludedClass.length())
                    {
                        int lastDotPosition = clazz.lastIndexOf('.');
                        if (lastDotPosition > excludedClass.length())
                        {
                            continue;
                        }
                    }
                    isExcluded = true;
                    break;
                }
            }
        }

        return isExcluded;
    }

    @Override
    public boolean isPackageExcluded(String packageName)
    {
        if (excludedPackages != null)
        {
            for (String excludedPackage : excludedPackages)
            {
                /*X TODO
                 * For 'org.apache.foo.**'
                 * the spec currently also excludes the package
                 * 'org.apache.foobar'
                 * Currently trying to clarify this.
                 */
                if (packageName.startsWith(excludedPackage))
                {
                    return true;
                }
            }
        }

        return false;
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

    public void addClassExclude(String classOrPath)
    {
        if (excludedClasses == null)
        {
            excludedClasses = new ArrayList<String>();
        }

        excludedClasses.add(classOrPath);
    }

    public void addPackageExclude(String packageName)
    {
        if (excludedPackages == null)
        {
            excludedPackages = new ArrayList<String>();
        }

        excludedPackages.add(packageName);
    }
}
