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
package org.apache.webbeans.test.tck.mock;

import org.apache.webbeans.corespi.scanner.AbstractMetaDataDiscovery;
import org.apache.webbeans.corespi.scanner.xbean.CdiArchive;
import org.apache.webbeans.util.Asserts;
import org.apache.xbean.finder.AnnotationFinder;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class TCKMetaDataDiscoveryImpl extends AbstractMetaDataDiscovery
{
    private Collection<Class<?>> classes = new ArrayList<Class<?>>();

    public TCKMetaDataDiscoveryImpl()
    {
        super();
    }
    
    @Override
    protected void configure()
    {
    }

    @Override
    protected AnnotationFinder initFinder()
    {
        if (finder != null)
        {
            return finder;
        }

        archive = new CdiArchive(classes);
        finder = new AnnotationFinder(archive);
        return finder;
    }

    public void addBeanClass(Class<?> clazz)
    {
        classes.add(clazz);
    }
    
    public void addBeanXml(URL url)
    {
        Asserts.assertNotNull(url);
        addWebBeansXmlLocation(url);
    }
    
}
