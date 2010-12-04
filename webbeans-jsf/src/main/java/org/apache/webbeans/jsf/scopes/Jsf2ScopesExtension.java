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
package org.apache.webbeans.jsf.scopes;

import java.lang.annotation.Annotation;

import javax.enterprise.context.spi.Context;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.ClassUtil;

/**
 * This small extension adds support for various JSF 2 scopes
 * TODO: this should be moved to an own module because this will
 * currently hinder webbeans-jsf to run in a JSF-1 application!
 */
public class Jsf2ScopesExtension implements Extension 
{
    public WebBeansLogger logger = WebBeansLogger.getLogger(Jsf2ScopesExtension.class); 
    
    public void addViewScoped(@Observes BeforeBeanDiscovery beforeBeanDiscovery)
    {
        if(WebBeansContext.getInstance().getOpenWebBeansConfiguration().isUseJSF2Extensions())
        {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> clazz = (Class<? extends Annotation>)ClassUtil.getClassFromName("javax.faces.bean.ViewScoped");
            beforeBeanDiscovery.addScope(clazz, true, true);   
        }        
    }
    
    public void registerViewContext(@Observes AfterBeanDiscovery afterBeanDiscovery)
    {
        if(WebBeansContext.getInstance().getOpenWebBeansConfiguration().isUseJSF2Extensions())
        {
            try
            {
                Context context = (Context)ClassUtil.getClassFromName("org.apache.webbeans.jsf.scopes.ViewScopedContext").newInstance();
                afterBeanDiscovery.addContext(context);   
                
            }
            catch(Exception e)
            {
                logger.error(e);
            }
        }
    }
}