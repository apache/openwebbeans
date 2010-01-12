/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.jsf.scopes;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.faces.bean.ViewScoped;

import org.apache.webbeans.config.OpenWebBeansConfiguration;

/**
 * This small extension adds support for various JSF 2 scopes
 * TODO: this should be moved to an own module because this will
 * currently hinder webbeans-jsf to run in a JSF-1 application!
 */
public class Jsf2ScopesExtension implements Extension {

    public void addViewScoped(@Observes BeforeBeanDiscovery beforeBeanDiscovery)
    {
        if(OpenWebBeansConfiguration.getInstance().isUseJSF2Extensions())
        {
            beforeBeanDiscovery.addScope(ViewScoped.class, true, true);   
        }        
    }
    
    public void registerViewContext(@Observes AfterBeanDiscovery afterBeanDiscovery)
    {
        if(OpenWebBeansConfiguration.getInstance().isUseJSF2Extensions())
        {
            afterBeanDiscovery.addContext(new ViewScopedContext());   
        }
    }
}