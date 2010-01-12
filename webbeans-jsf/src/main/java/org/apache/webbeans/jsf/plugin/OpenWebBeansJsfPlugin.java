/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.jsf.plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.faces.component.UIComponent;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.plugins.AbstractOpenWebBeansPlugin;
import org.apache.webbeans.util.ClassUtil;

public class OpenWebBeansJsfPlugin extends AbstractOpenWebBeansPlugin 
{

    /** {@inheritDoc} */
    public void startUp() throws WebBeansConfigurationException 
    {
        // nothing to do
    }

    /** {@inheritDoc} */
    public void shutDown() throws WebBeansConfigurationException 
    {
        // nothing to do
    }

    /** {@inheritDoc} */
    public void isManagedBean( Class<?> clazz ) throws WebBeansConfigurationException 
    {
        if (ClassUtil.isAssignable(UIComponent.class, clazz))
        {
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() 
                                                     + " can not implement JSF UIComponent");
        }
    }

    /** {@inheritDoc} */
    public void checkForValidResources(Type type, Class<?> clazz, String name, Annotation[] annotations)
    {
        // nothing to do
    }

    /** {@inheritDoc} */
    public Object injectResource(Type type, Annotation[] annotations)
    {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isResourceAnnotation(Class<? extends Annotation> annotationClass)
    {
        return false;
    }

}
