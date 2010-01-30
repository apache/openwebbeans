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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.decorator.xml.WebBeansXMLDecorator;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;
import org.apache.webbeans.logger.WebBeansLogger;

public final class WebBeansDecoratorConfig
{
    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansDecoratorConfig.class);

    private WebBeansDecoratorConfig()
    {

    }

    public static <T> void configureDecoratorClass(AbstractBean<T> delegate)
    {
        logger.info(OWBLogConst.INFO_0011, new Object[]{logger.getTokenString(OWBLogConst.TEXT_ANNO_CLASS), delegate.getReturnType().getName()});

        WebBeansDecorator<T> decorator = new WebBeansDecorator<T>(delegate);

        logger.info(OWBLogConst.INFO_0012, new Object[]{logger.getTokenString(OWBLogConst.TEXT_ANNO_CLASS), delegate.getReturnType()});

        BeanManagerImpl.getManager().addDecorator(decorator);
    }

    public static <T> void configureXMLDecoratorClass(AbstractBean<T> delegate, XMLInjectionPointModel model)
    {
        logger.info(OWBLogConst.INFO_0011, new Object[]{logger.getTokenString(OWBLogConst.TEXT_XML_CLASS), delegate.getReturnType().getName()});

        WebBeansXMLDecorator<T> decorator = new WebBeansXMLDecorator<T>(delegate, model);

        logger.info(OWBLogConst.INFO_0012, new Object[]{logger.getTokenString(OWBLogConst.TEXT_XML_CLASS), delegate.getReturnType()});

        BeanManagerImpl.getManager().addDecorator(decorator);
    }

    public static void configureDecarotors(AbstractBean<?> component)
    {
        Set<Annotation> qualifiers = component.getQualifiers();
        Annotation[] anns = new Annotation[qualifiers.size()];
        anns = qualifiers.toArray(anns);

        List<Decorator<?>> decoratorList = BeanManagerImpl.getManager().resolveDecorators(component.getTypes(), anns);
        Iterator<Decorator<?>> itList = decoratorList.iterator();

        while (itList.hasNext())
        {
            WebBeansDecorator<?> decorator = (WebBeansDecorator<?>) itList.next();            
            component.getDecorators().add(decorator);            
        }
    }
    
    public static List<Object> getDecoratorStack(AbstractBean<?> component, Object instance, Object delegate)
    {
        List<Object> decoratorStack = new ArrayList<Object>();
        List<Decorator<?>> decoratorList = component.getDecorators();        
        Iterator<Decorator<?>> itList = decoratorList.iterator();
        BeanManager manager = BeanManagerImpl.getManager();
        while (itList.hasNext())
        {
            WebBeansDecorator<?> decorator = (WebBeansDecorator<?>) itList.next();
            CreationalContext<?> creationalContext = manager.createCreationalContext(decorator);
            Object decoratorInstance = manager.getReference(decorator, decorator.getBeanClass(), creationalContext);
            
            decorator.setInjections(decoratorInstance, creationalContext);
            decorator.setDelegate(decoratorInstance, delegate);
            decoratorStack.add(decoratorInstance);
        }

        return decoratorStack;
    }

    private static Set<Decorator<?>> getWebBeansDecorators()
    {
        return Collections.unmodifiableSet(BeanManagerImpl.getManager().getDecorators());
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
            listAnnot.add(new DefaultLiteral());
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
