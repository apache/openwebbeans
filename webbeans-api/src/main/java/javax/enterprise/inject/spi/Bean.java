/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package javax.enterprise.inject.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;

public abstract class Bean<T> implements Contextual<T>
{
    private final BeanManager manager;

    protected Bean(BeanManager manager)
    {
        this.manager = manager;
    }

    protected BeanManager getManager()
    {
        return manager;
    }

    public abstract Set<Type> getTypes();

    public abstract Set<Annotation> getBindings();

    public abstract Class<? extends Annotation> getScopeType();

    public abstract Class<? extends Annotation> getDeploymentType();

    public abstract String getName();

    public abstract boolean isSerializable();

    public abstract boolean isNullable();

    public abstract Set<InjectionPoint> getInjectionPoints();
    
    public abstract Class<?> getBeanClass();

}