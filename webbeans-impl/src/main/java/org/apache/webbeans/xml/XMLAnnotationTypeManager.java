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

import org.apache.webbeans.config.WebBeansContext;

/**
 * TODO This class actually has nothing to do with XML anymore!
 * @deprecated The addInterceotorBindingTypeInheritAnnotation should get moved to the BeanManagerImpl or similar place.
 */
public class XMLAnnotationTypeManager
{
    private Map<Class<? extends Annotation>, Set<Annotation>> xmlInterceptorBindingTypes = new ConcurrentHashMap<Class<? extends Annotation>, Set<Annotation>>();


    public XMLAnnotationTypeManager(WebBeansContext webBeansContext)
    {
    }

    public static XMLAnnotationTypeManager getInstance()
    {
        return WebBeansContext.getInstance().getXMLAnnotationTypeManager();
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


    public Set<Annotation> getInterceptorBindingTypeInherites(Class<? extends Annotation> interceptorBindingType)
    {
        return Collections.unmodifiableSet(xmlInterceptorBindingTypes.get(interceptorBindingType));
    }

}
