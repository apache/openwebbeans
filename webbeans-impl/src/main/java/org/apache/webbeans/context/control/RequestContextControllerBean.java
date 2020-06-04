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

package org.apache.webbeans.context.control;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.BuiltInOwbBean;
import org.apache.webbeans.component.SimpleProducerFactory;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.CollectionUtil;

import javax.enterprise.context.control.RequestContextController;

public class RequestContextControllerBean extends BuiltInOwbBean<RequestContextController>
{
    public RequestContextControllerBean(WebBeansContext webBeansContext)
    {
        super(webBeansContext,
                WebBeansType.MANAGER,
                new BeanAttributesImpl<>(CollectionUtil.unmodifiableSet(RequestContextController.class, Object.class),
                        AnnotationUtil.DEFAULT_AND_ANY_ANNOTATION_SET),
                RequestContextController.class,
                new SimpleProducerFactory<>(new RequestContextControllerProducer(webBeansContext)));
    }

    @Override
    public Class<?> proxyableType()
    {
        return RequestContextController.class;
    }
}
