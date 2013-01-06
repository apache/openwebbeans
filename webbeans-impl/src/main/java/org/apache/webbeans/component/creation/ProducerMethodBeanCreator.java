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

import javax.enterprise.inject.spi.AnnotatedMethod;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.util.ClassUtil;

public class ProducerMethodBeanCreator<T> extends AbstractProducerBeanCreator<T>
{

    public ProducerMethodBeanCreator(InjectionTargetBean<T> parent, AnnotatedMethod<? super T> annotatedMethod)
    {
        super(new ProducerMethodBean<T>(parent, (Class<T>)ClassUtil.getClass(annotatedMethod.getBaseType())), annotatedMethod);
    }

    /**
     * {@inheritDoc}
     */
    public ProducerMethodBean<T> getBean()
    {
        return (ProducerMethodBean<T>)super.getBean();
    }
}
