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

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.util.TypeLiteral;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.MetadataProducer;
import org.apache.webbeans.util.CollectionUtil;

/**
 * Implicit metadata bean definition.
 * 
 * @version $Rev: 1493478 $ $Date: 2013-06-16 11:19:42 +0200 (So, 16 Jun 2013) $
 */
public class DecoratorMetadataBean<T> extends BuiltInOwbBean<Decorator<T>>
{

    @SuppressWarnings("serial")
    public DecoratorMetadataBean(WebBeansContext webBeansContext)
    {
        super(webBeansContext,
              WebBeansType.METADATA,
              new BeanAttributesImpl<Decorator<T>>(CollectionUtil.<Type>unmodifiableSet(Decorator.class, Object.class)),
              new TypeLiteral<Decorator<T>>(){}.getRawType(),
              false,
              new SimpleProducerFactory<Decorator<T>>(new MetadataProducer<Decorator<T>>()));
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
