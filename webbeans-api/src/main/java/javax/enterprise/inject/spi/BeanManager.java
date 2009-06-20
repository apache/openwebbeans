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

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observer;
import javax.enterprise.inject.TypeLiteral;


/**
 * The interface <code>Manager</code> provides operations for
 * obtaining the contextual instance of the webbeans. There are operations
 * related with;
 * 
 * <ul>
 *  <li>Adding new webbeans</li>
 *  <li>Adding new contexts</li>
 *  <li>Adding new decorators</li>
 *  <li>Adding new interceptors</li>
 *  <li>Firing the observer events</li>
 *  <li>Creating the instance of the contextual beans</li>
 *  <li>Resolution of beans, interceptors, decorators and observers</li>
 *  <li>Other utility methods etc..</li>
 * </ul>
 * 
 * <p>
 * There is always one root manager in the system. You can set the
 * current activity via call to the {@link BeanManager#setCurrent(Class)} method.
 * </p>
 * 
 * @version $Rev$ $Date$ 
 */
public interface BeanManager
{
    /**
     * Resolve beans with given api type and bindings.
     * 
     * @param apiType api type of the bean
     * @param bindings beans bindings
     * @return the set of webbeans
     */
    public <T> Set<Bean<T>> resolveByType(Class<T> apiType, Annotation... bindings);

    /**
     * Resolves beans with given {@link TypeLiteral} generic type
     * and binding types.
     * 
     * @param apiType bean api type
     * @param bindingTypes bean binding types
     * @return the set of resolved beans
     */
    public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> apiType, Annotation... bindingTypes);

    /**
     * Gets bean instance using its context. 
     * 
     * @param type type of the bean
     * @param bindingTypes binding types of the bean
     * @return the bean instance
     */
    public <T> T getInstanceByType(Class<T> type, Annotation... bindingTypes);

    /**
     * Gets instance of the bean from its context.
     * 
     * @param type generic type of bean using {@link TypeLiteral}
     * @param bindingTypes binding types
     * @return instance of the bean
     */
    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes);

    /**
     * Resolves the set of beans with given name.
     * 
     * @param name name of the bean
     * @return the set of resolved beans with given name
     */
    public Set<Bean<?>> resolveByName(String name);

    /**
     * Gets instance of the injection point.
     * <p>
     * See the specification section 5.7.1 Resolving dependencies.
     * </p>
     * 
     * @param injectionPoint injection point definition
     * @param context creational context instance
     * @return instance of the injection point or null
     */
    public <T> T getInstanceToInject(InjectionPoint injectionPoint, CreationalContext<?> context);

    public <T> T getInstanceToInject(InjectionPoint injectionPoint);

    public Object getInstanceByName(String name);

    public <T> T getInstance(Bean<T> bean);

    public void fireEvent(Object event, Annotation... bindings);

    public Context getContext(Class<? extends Annotation> scopeType);

    /**
     * Add new context to this activity.
     * 
     * @param context new context
     * @return the this activity
     */
    public BeanManager addContext(Context context);

    public BeanManager addBean(Bean<?> bean);

    /**
     * Adds new interceptor to this activity.
     * 
     * @param interceptor new interceptor
     * @return the manager instance that this interceptor is added
     */
    public BeanManager addInterceptor(Interceptor<?> interceptor);

    public BeanManager addDecorator(Decorator<?> decorator);

    public <T> BeanManager addObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings);

    public <T> BeanManager addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings);

    public <T> BeanManager removeObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings);

    public <T> BeanManager removeObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings);

    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings);

    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings);

    public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... bindingTypes);
    
    public BeanManager parse(InputStream xmlStream);
    
    public BeanManager createActivity();

    public BeanManager setCurrent(Class<? extends Annotation> scopeType);
    

}