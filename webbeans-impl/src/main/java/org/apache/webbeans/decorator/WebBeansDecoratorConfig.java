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
package org.apache.webbeans.decorator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Decorator;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.decorator.xml.WebBeansXMLDecorator;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;
import org.apache.webbeans.logger.WebBeansLogger;

public final class WebBeansDecoratorConfig
{
    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansDecoratorConfig.class);

    private WebBeansDecoratorConfig()
    {

    }

    public static <T> void configureDecoratorClass(AbstractComponent<T> delegate)
    {
        logger.info("Configuring the Web Beans Annoatated Decorator Class : " + delegate.getReturnType().getName() + " started");

        WebBeansDecorator<T> decorator = new WebBeansDecorator<T>(delegate);

        logger.info("Configuring the Web Beans Annotated Decorator Class : " + delegate.getReturnType() + " ended");

        ManagerImpl.getManager().addDecorator(decorator);
    }

    public static void configureXMLDecoratorClass(AbstractComponent<Object> delegate, XMLInjectionPointModel model)
    {
        logger.info("Configuring the Web Beans XML based Decorator Class : " + delegate.getReturnType().getName() + " started");

        WebBeansXMLDecorator decorator = new WebBeansXMLDecorator(delegate, model);

        logger.info("Configuring the Web Beans XML based Decorator Class : " + delegate.getReturnType() + " ended");

        ManagerImpl.getManager().addDecorator(decorator);
    }

    public static void configureDecarotors(AbstractComponent<?> component, Object instance)
    {
        Set<Annotation> bindingTypes = component.getBindings();
        Annotation[] anns = new Annotation[bindingTypes.size()];
        anns = bindingTypes.toArray(anns);

        List<Decorator<?>> decoratorList = ManagerImpl.getManager().resolveDecorators(component.getTypes(), anns);
        Iterator<Decorator<?>> itList = decoratorList.iterator();

        while (itList.hasNext())
        {
            WebBeansDecorator<?> decorator = (WebBeansDecorator<?>) itList.next();

            Object decoratorInstance = ManagerImpl.getManager().getInstance(decorator);

            decorator.setInjections(decoratorInstance);
            decorator.setDelegate(decoratorInstance, instance);

            component.getDecoratorStack().add(decoratorInstance);
        }
    }

    private static Set<Decorator<?>> getWebBeansDecorators()
    {
        return Collections.unmodifiableSet(ManagerImpl.getManager().getDecorators());
    }

    public static Set<Decorator<?>> findDeployedWebBeansDecorator(Set<Type> apiType, Annotation... anns)
    {
        Set<Decorator<?>> set = new HashSet<Decorator<?>>();

        Iterator<Decorator<?>> it = getWebBeansDecorators().iterator();
        WebBeansDecorator<?> decorator = null;

        List<Class<? extends Annotation>> bindingTypes = new ArrayList<Class<? extends Annotation>>();
        Set<Annotation> listAnnot = new HashSet<Annotation>();
        for (Annotation ann : anns)
        {
            bindingTypes.add(ann.annotationType());
            listAnnot.add(ann);
        }

        if (listAnnot.isEmpty())
        {
            listAnnot.add(new CurrentLiteral());
        }

        while (it.hasNext())
        {
            decorator = (WebBeansDecorator<?>) it.next();

            if (decorator.isDecoratorMatch(apiType, listAnnot))
            {
                set.add(decorator);
            }
        }

        return set;

    }

}
