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
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Set;

import javax.el.ELResolver;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observer;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;


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
     * Returns a bean instance reference for the given bean.
     * 
     * @param bean bean that its reference is getting
     * @param beanType bean api type that is implemented by the proxy
     * @param ctx creational context is used to destroy any object with scope <code>@Dependent</code>
     * @return bean reference
     * @throws IllegalArgumentException if given bean type is not api type of the given bean object
     */
    public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> ctx);
    
    /**
     * Gets injection point bean reference.
     * 
     * @param injectionPoint injection point definition
     * @param ctx creational context that is passed to the {@link Bean#create(CreationalContext)} method
     * @return bean reference
     * @throws UnsatisfiedResolutionException if no bean found for the given injection point
     * @throws AmbiguousResolutionException if more than one bean found
     */
    public Object getInjectableReference(InjectionPoint injectionPoint, CreationalContext<?> ctx);
    
    /**
     * Returns a new creational context implementation.
     * 
     * @return new creational context
     */
    public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual);
    
    /**
     * Returns set of beans that their api types contain
     * given bean type and given qualifiers.
     * 
     * <p>
     * If no qualifier is given, <code>@Current</code> is assumed.
     * </p>
     * 
     * @param beanType required bean type
     * @param qualifiers required qualifiers
     * @return set of beans
     * @throws IllegalArgumentException given bean type is a {@link TypeVariable}
     * @throws IllegalArgumentException given qualifier annotation is not a qualifier
     * @throws IllegalArgumentException same qualifier is given
     */
    public Set<Bean<?>> getBeans(Type beanType, Annotation... qualifiers);
        
    /**
     * Returns set of beans with given name.
     * 
     * @param name name of the bean
     * @return set of beans with given name
     */
    public Set<Bean<?>> getBeans(String name);    
    
    /**
     * Returns bean's most specialized bean object.
     * 
     * @param <X> bean class type info
     * @param bean bean object
     * @return bean's most specialized bean object
     */
    public <X> Bean<? extends X> getMostSpecializedBean(Bean<X> bean);
    
    /**
     * Returns passivation capable bean given id.
     * 
     * @param id bean id
     * @return passivation capable bean given id
     */
    public Bean<?> getPassivationCapableBean(String id);
    
    /**
     * Returns a bean object that is resolved according to the type safe resolution rules.
     * 
     * @param <X> bean class info
     * @param beans set of beans
     * @return bean that is resolved according to the type safe resolution rules
     * @throws AmbiguousResolutionException if ambigious exists
     */
    public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans);
        
    /**
     * Fires an event with given even object and qualifiers.
     * 
     * @param event observer event object
     * @param qualifiers event qualifiers
     * @throws IllegalArgumentException event object contains a {@link TypeVariable}
     * @throws IllegalArgumentException given qualifier annotation is not a qualifier
     * @throws IllegalArgumentException same qualifier is given
     */
    public void fireEvent(Object event, Annotation... qualifiers);
    
    /**
     * Returns a set of observers that observe for given event type and qualifiers
     * 
     * @param <T> observer type info
     * @param event observer event type
     * @param qualifiers event qualifiers
     * @return set of observers that observe for given event type and qualifiers
     * @throws IllegalArgumentException event object contains a {@link TypeVariable}
     * @throws IllegalArgumentException given qualifier annotation is not a qualifier
     * @throws IllegalArgumentException same qualifier is given
     */
    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... qualifiers);
    
    /**
     * Returns set of observer methods.
     * 
     * @param <T> event type
     * @param event even object
     * @param qualifiers event qualifiers
     * @return set of observer methods
     */
    public <T> Set<ObserverMethod<?,T>> resolveObserverMethods(T event, Annotation... qualifiers);
    
    /**
     * Returns a list of decorator.
     * 
     * @param types bean types of the decorated bean
     * @param qualifiers decorated bean qualifiers
     * @return list of decorator
     * @throws IllegalArgumentException given qualifier annotation is not a qualifier
     * @throws IllegalArgumentException same qualifier is given
     * @throws IllegalArgumentException if types is empty set
     */
    List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... qualifiers);
    
    /**
     * Returns a list of interceptor.
     * 
     * @param type interception type
     * @param interceptorBindings interceptor bindings
     * @return list of interceptor
     * @throws IllegalArgumentException given binding annotation is not a binding
     * @throws IllegalArgumentException same binding is given
     * @throws IllegalArgumentException binding is not an interceptor binding
     */
    List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings);
    
    /**
     * Validates injection point.
     * 
     * @param injectionPoint injection point
     * @throws InjectionException if problem exist
     */
    public void validate(InjectionPoint injectionPoint);
    
    /**
     * Returns true if given type is a scope type, false otherwise.
     * 
     * @param annotationType annotation type
     * @return true if given type is a scope type, false otherwise
     */
    public boolean isScope(Class<? extends Annotation> annotationType);
    
    /**
     * Returns true if given type is a normal scope type, false otherwise.
     * 
     * @param annotationType annotation type
     * @return true if given type is a scope type, false otherwise
     */
    public boolean isNormalScope(Class<? extends Annotation> annotationType);
    
    /**
     * Returns true if given type is a passivating scope type, false otherwise.
     * 
     * @param annotationType annotation type
     * @return true if given type is a scope type, false otherwise
     */
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType);    
    

    /**
     * Returns true if given type is a qualifier, false otherwise.
     * 
     * @param annotationType annotation type
     * @return true if given type is a qualifier, false otherwise
     */    
    public boolean isQualifier(Class<? extends Annotation> annotationType);
    
    /**
     * Returns true if given type is a interceptor binding, false otherwise.
     * 
     * @param annotationType annotation type
     * @return true if given type is a interceptor binding, false otherwise
     */        
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType);
    
        
    /**
     * Returns true if given type is a stereotype type, false otherwise.
     * 
     * @param annotationType annotation type
     * @return true if given type is a stereotype, false otherwise
     */
    public boolean isStereotype(Class<? extends Annotation> annotationType);
        
    /**
     * Returns a set of meta-annotations that are defined on the binding
     * 
     * @param qualifier binding class
     * @return a set of meta-annotations that are defined on the binding
     */
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> qualifier);
    
    /**
     * Returns a set of meta-annotations that are defined on the stereotype type.
     * 
     * @param stereotype stereotype type class
     * @return a set of meta-annotations that are defined on the stereotype type
     */
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype);

    /**
     * Returns a context with given scope type.
     * 
     * @param scope scope type class type
     * @return a context with given scope type
     */
    public Context getContext(Class<? extends Annotation> scope);
    
    /**
     * Returns el resolver.
     * 
     * @return el resolver
     */
    public ELResolver getELResolver();
    
    /**
     * Returns a {@link AnnotatedType} instance for the given
     * class.
     * 
     * @param <T> class type
     * @param type class
     * @return a {@link AnnotatedType} instance
     */
    public <T> AnnotatedType<T> createAnnotatedType(Class<T> type);
    
    /**
     * Creates a new instance of injection target.
     * 
     * @param <T> bean type
     * @param type annotated type
     * @return injection target
     */
    public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type);
}