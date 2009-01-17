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
package javax.webbeans.manager;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.webbeans.InjectionPoint;
import javax.webbeans.NonBinding;
import javax.webbeans.Observer;
import javax.webbeans.TypeLiteral;

/**
 * Resolution of the components contract of the <b>Web Beans Container</b>.
 * There are two ways with regarding to the resolving components in the web
 * beans container, resolution by type and resolution by name.
 * <p>
 * When resolving at the injection point, the web beans container uses the api
 * type and binding type of the injected instance of the web bean component.
 * Each web beans component has to be enabled for being candidate in regarding
 * to resolution. Web Beans Container applies the following resolution procedure
 * in order;
 * </p>
 * <p>
 * <ul>
 * <li>Inspect the type of the injected point to find all web beans component
 * that has this API type.</li>
 * <li>From the candidates, it selects the all components that satisfies the
 * binding types of the injected point. If the injected point annotation has
 * some member values, then it selects the web beans components with binding
 * type (with {@link NonBinding} annotated member) that has same member values
 * with the injected annoation value.</li>
 * <li>If there are some components that has exactly the same binding type with
 * the injected point, container narrows the component set containing just those
 * components.</li>
 * <li>Examine the precedence type of the narrowed set of components and selects
 * the higher precedence of the components. Otherwise exception is thrown by the
 * container.</li>
 * </ul>
 * </p>
 * <p>
 * Resolution by name procedure is as follows;
 * </p>
 * <p>
 * <ul>
 * <li>Container selects the set of enabled web beans components that has the
 * given name</li>
 * <li>Container selects the higher precedence from the set using the component
 * type precedence</li>
 * <li>If exactly one component is remained, the resolution results in that
 * component, otherwise</li> exceptin is thrown by the container.
 * </ul>
 * </p>

 * @since 1.0
 */
public interface Manager
{
    public <T> Set<Bean<T>> resolveByType(Class<T> type, Annotation... bindings);

    public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> apiType, Annotation... bindingTypes);

    public <T> T getInstanceByType(Class<T> type, Annotation... bindingTypes);

    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes);

    public Set<Bean<?>> resolveByName(String name);
    
    public <T> T getInstanceToInject(InjectionPoint injectionPoint, CreationalContext<T> context);
    
    public Object getInstanceToInject(InjectionPoint injectionPoint);

    public Object getInstanceByName(String name);

    public <T> T getInstance(Bean<T> bean);

    public void fireEvent(Object event, Annotation... bindings);

    public Context getContext(Class<? extends Annotation> scopeType);

    public Manager addContext(Context context);

    public Manager addBean(Bean<?> bean);

    public Manager addInterceptor(Interceptor interceptor);

    public Manager addDecorator(Decorator decorator);

    public <T> Manager addObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings);

    public <T> Manager addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings);

    public <T> Manager removeObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings);

    public <T> Manager removeObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings);

    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings);

    public List<Interceptor> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings);

    public List<Decorator> resolveDecorators(Set<Class<?>> types, Annotation... bindingTypes);

}