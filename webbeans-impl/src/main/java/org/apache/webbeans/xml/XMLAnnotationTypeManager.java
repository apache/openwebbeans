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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.XMLStereoTypeModel;
import org.w3c.dom.Element;

public class XMLAnnotationTypeManager
{
    private Set<Class<? extends Annotation>> xmlBindingTypes = new CopyOnWriteArraySet<Class<? extends Annotation>>();
    
    private Set<Class<? extends Annotation>> xmlResources = new CopyOnWriteArraySet<Class<? extends Annotation>>();

    private Map<Class<? extends Annotation>, Set<Annotation>> xmlInterceptorBindingTypes = new ConcurrentHashMap<Class<? extends Annotation>, Set<Annotation>>();

    private Set<Class<? extends Annotation>> xmlStereoTypes = new CopyOnWriteArraySet<Class<? extends Annotation>>();
    private final StereoTypeManager manager;
    private final WebBeansContext webBeansContext;

    public XMLAnnotationTypeManager(WebBeansContext webBeansContext)
    {

        this.webBeansContext = webBeansContext;
        manager = this.webBeansContext.getStereoTypeManager();
    }

    public static XMLAnnotationTypeManager getInstance()
    {
        return WebBeansContext.getInstance().getxMLAnnotationTypeManager();
    }

    @Deprecated
    public void addBindingType(Class<? extends Annotation> bindingType)
    {
        xmlBindingTypes.add(bindingType);
    }

    public void addResource(Class<? extends Annotation> resource)
    {
        xmlResources.add(resource);
    }

    public boolean hasBindingType(Class<? extends Annotation> bindingType)
    {
        if (xmlBindingTypes.contains(bindingType))
        {
            return true;
        }

        return false;
    }

    public boolean hasResource(Class<? extends Annotation> resource)
    {
        if (xmlResources.contains(resource))
        {
            return true;
        }

        return false;
    }

    public void addStereoType(Class<? extends Annotation> stereoType, Element decleration, String name, String errorMessage)
    {
        webBeansContext.getAnnotationManager().checkStereoTypeClass(stereoType, stereoType.getDeclaredAnnotations());

        XMLStereoTypeModel model = new XMLStereoTypeModel(decleration, name, errorMessage);
        manager.addStereoTypeModel(model);

        xmlStereoTypes.add(stereoType);
    }

    public boolean hasStereoType(Class<? extends Annotation> stereoType)
    {
        if (xmlStereoTypes.contains(stereoType))
        {
            return true;
        }

        return false;
    }

    public void addInterceotorBindingTypeInheritAnnotation(Class<? extends Annotation> bindingType, Annotation inherit)
    {
        Set<Annotation> inherits = xmlInterceptorBindingTypes.get(bindingType);
        if (inherits == null)
        {
            inherits = new HashSet<Annotation>();
            inherits.add(inherit);
            xmlInterceptorBindingTypes.put(bindingType, inherits);
        }
        else
        {
            inherits.add(inherit);
        }
    }
    
    public void addInterceotorBindingTypeInheritAnnotation(Class<? extends Annotation> bindingType, Annotation... inheritsArray)
    {
        Set<Annotation> inherits = xmlInterceptorBindingTypes.get(bindingType);
        if (inherits == null)
        {
            inherits = new HashSet<Annotation>();
            
            for(Annotation ann : inheritsArray)
            {
                inherits.add(ann);
            }
            
            xmlInterceptorBindingTypes.put(bindingType, inherits);
        }
        else
        {
            for(Annotation ann : inheritsArray)
            {
                inherits.add(ann);
            }

        }
    }    

    public boolean hasInterceptorBindingType(Class<? extends Annotation> bindingType)
    {
        if (xmlInterceptorBindingTypes.keySet().contains(bindingType))
        {
            return true;
        }

        return false;
    }

    public Set<Class<? extends Annotation>> getBindingTypes()
    {
        return Collections.unmodifiableSet(xmlBindingTypes);
    }

    public Set<Class<? extends Annotation>> getResources()
    {
        return Collections.unmodifiableSet(xmlResources);
    }

    public Set<Annotation> getInterceptorBindingTypeInherites(Class<? extends Annotation> interceptorBindingType)
    {
        return Collections.unmodifiableSet(xmlInterceptorBindingTypes.get(interceptorBindingType));
    }

    public Set<Class<? extends Annotation>> getInterceptorBindingTypes()
    {
        return Collections.unmodifiableSet(xmlInterceptorBindingTypes.keySet());
    }

    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return Collections.unmodifiableSet(xmlStereoTypes);
    }

}
