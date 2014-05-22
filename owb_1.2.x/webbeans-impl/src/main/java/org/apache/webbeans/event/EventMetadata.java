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
package org.apache.webbeans.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * This is a preview to CDI 1.1
 * when we implement CDI 1.1 this interface can be removed
 */
public interface EventMetadata
{

    /**
     * Get the qualifiers for which event payload was fired.
     */
    public Set<Annotation> getQualifiers();

    /**
     * Get the {@link InjectionPoint} from which the event fired, or
     * <code>null</code> if it was fired from
     * {@link javax.enterprise.inject.spi.BeanManager#fireEvent(Object, Annotation...)};
     */
    public InjectionPoint getInjectionPoint();

    /**
     * Returns the resolved event {@link Type}.
     */
    public Type getType();
}
