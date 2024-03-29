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
package org.apache.webbeans.decorator;

import java.util.Comparator;

import jakarta.enterprise.inject.spi.Decorator;

import org.apache.webbeans.config.WebBeansContext;

public class DecoratorComparator<T> implements Comparator<Decorator<T>>
{

    /** default serial version UID */
    private static final long serialVersionUID = 1L;

    private final WebBeansContext webBeansContext;

    public DecoratorComparator(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    @Override
    public int compare(Decorator<T> o1, Decorator<T> o2)
    {
        if (o1.equals(o2))
        {
            return 0;
        }
        else
        {
            Class<?> o1Clazz = o1.getBeanClass();
            Class<?> o2Clazz = o2.getBeanClass();

            return webBeansContext.getDecoratorsManager().compare(o1Clazz, o2Clazz);
        }
    }

}
