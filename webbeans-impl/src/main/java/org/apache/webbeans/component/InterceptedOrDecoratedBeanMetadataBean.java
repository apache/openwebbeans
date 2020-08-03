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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.TypeLiteral;

import javax.enterprise.inject.Decorated;
import javax.enterprise.inject.Intercepted;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.BeanMetadataProducer;
import org.apache.webbeans.util.CollectionUtil;

/**
 * Implicit metadata bean definition.
 * 
 * @version $Rev: 1493478 $ $Date: 2013-06-16 11:19:42 +0200 (So, 16 Jun 2013) $
 */
public class InterceptedOrDecoratedBeanMetadataBean<T> extends BuiltInOwbBean<Bean<T>>
{

    @SuppressWarnings("serial")
    public InterceptedOrDecoratedBeanMetadataBean(WebBeansContext webBeansContext)
    {
        super(webBeansContext,
            WebBeansType.METADATA,
            new BeanAttributesImpl<>(
                CollectionUtil.<Type>unmodifiableSet(Bean.class, Contextual.class, Object.class),
                CollectionUtil.<Annotation>unmodifiableSet(new AnnotationLiteral<Intercepted>()
                {
                }, new AnnotationLiteral<Decorated>()
                {
                })),
            new TypeLiteral<Bean<T>>(){}.getRawType(),
            new SimpleProducerFactory<>(new BeanMetadataProducer<Bean<T>>()));
    }
    
    /* (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractOwbBean#isPassivationCapable()
     */
    @Override
    public boolean isPassivationCapable()
    {
        return true;
    }

    @Override
    public Class<?> proxyableType()
    {
        return null;
    }
}
