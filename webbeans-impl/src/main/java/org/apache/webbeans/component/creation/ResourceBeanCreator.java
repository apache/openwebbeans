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

import java.lang.annotation.Annotation;

import javax.enterprise.inject.spi.AnnotatedField;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ResourceBean;
import org.apache.webbeans.spi.api.ResourceReference;

public class ResourceBeanCreator<T, R extends Annotation> extends ProducerFieldBeanCreator<T>
{

    public ResourceBeanCreator(InjectionTargetBean<T> parent, ResourceReference<T, R> resourceRef, AnnotatedField<? super T> annotatedField)
    {
        super(new ResourceBean<T, R>((Class<T>)annotatedField.getJavaMember().getType(), parent, resourceRef), annotatedField);
    }

    public ResourceBean<T, R> getBean()
    {
        return (ResourceBean<T, R>) super.getBean();
    }
}
