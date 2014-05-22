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
package org.apache.webbeans.component;

import javax.enterprise.context.ApplicationScoped;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.AnnotationUtil;

/**
 * Extension service bean definition.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> type info
 */
public class ExtensionBean<T> extends BuiltInOwbBean<T>
{
    /**
     * Creates a new extesion bean.
     * 
     * @param returnType return type
     * @param webBeansContext
     */
    public ExtensionBean(WebBeansContext webBeansContext, Class<T> returnType)
    {
        super(webBeansContext,
                WebBeansType.EXTENSION,
                new BeanAttributesImpl<T>(
                        webBeansContext.getAnnotatedElementFactory().getAnnotatedType(returnType).getTypeClosure(),
                        AnnotationUtil.DEFAULT_AND_ANY_ANNOTATION,
                        ApplicationScoped.class),
                returnType,
                false,
                new ExtensionProducerFactory<T>(webBeansContext.getAnnotatedElementFactory().getAnnotatedType(returnType), webBeansContext));
    }
}
