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
package org.apache.webbeans.jsf12.plugin;


import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.spi.plugins.AbstractOwbJsfPlugin;
import org.apache.webbeans.util.ClassUtil;

public class OpenWebBeansJsfPlugin extends AbstractOwbJsfPlugin
{
    /** {@inheritDoc} */
    public void isManagedBean( Class<?> clazz ) throws WebBeansConfigurationException 
    {
        if (ClassUtil.isClassAssignable(UIComponent.class, clazz))
        {
            throw new WebBeansConfigurationException("Bean implementation class : " + clazz.getName() 
                                                     + " can not implement JSF UIComponent");
        }
    }

    @Override
    public boolean isOwbApplication()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if(facesContext == null)
        {
            throw new IllegalStateException("FacesContext is null");
        }
        
        ExternalContext ext = facesContext.getExternalContext();
        ServletContext servletContext = (ServletContext) ext.getContext();
        Object attribute = servletContext.getAttribute(OpenWebBeansConfiguration.PROPERTY_OWB_APPLICATION);
        
        return attribute != null ? true : false;
    }
    
    
    
}
