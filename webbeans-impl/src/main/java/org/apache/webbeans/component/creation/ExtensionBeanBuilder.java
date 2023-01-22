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
package org.apache.webbeans.component.creation;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;

import org.apache.webbeans.component.ExtensionBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.Asserts;

public class ExtensionBeanBuilder<T extends Extension>
{
    protected final WebBeansContext webBeansContext;
    protected final AnnotatedType<T> annotatedType;
    protected final T extension;

    public ExtensionBeanBuilder(WebBeansContext webBeansContext, T extension)
    {
        Asserts.assertNotNull(webBeansContext, Asserts.PARAM_NAME_WEBBEANSCONTEXT);
        Asserts.assertNotNull(extension, "extension");
        this.webBeansContext = webBeansContext;
        annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType((Class<T>)extension.getClass());
        this.extension = extension;
    }

    public AnnotatedType<T> getAnnotatedType()
    {
        return annotatedType;
    }

    public T getExtension()
    {
        return extension;
    }

    public ExtensionBean<T> buildBean()
    {
        return new ExtensionBean<>(webBeansContext, annotatedType.getJavaClass());
    }
}
