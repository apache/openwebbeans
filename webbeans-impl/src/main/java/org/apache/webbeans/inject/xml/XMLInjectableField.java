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
package org.apache.webbeans.inject.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.util.SecurityUtil;

public class XMLInjectableField extends InjectableField
{
    private XMLInjectionPointModel injectionPointModel = null;

    public XMLInjectableField(Field field, Object instance, AbstractOwbBean<?> owner, XMLInjectionPointModel model,CreationalContext<?> creationalContext)
    {
        super(field, instance, owner,creationalContext);
        this.injectionPointModel = model;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.inject.InjectableField#doInjection()
     */
    @Override
    public Object doInjection()
    {
        Annotation[] anns = new Annotation[this.injectionPointModel.getBindingTypes().size()];
        anns = this.injectionPointModel.getBindingTypes().toArray(anns);

        try
        {
            if (!field.isAccessible())
            {
                SecurityUtil.doPrivilegedSetAccessible(field, true);
            }

            
            //field.set(instance, inject(this.injectionPointModel.getInjectionGenericType(), anns));

        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }

        return null;
    }
}