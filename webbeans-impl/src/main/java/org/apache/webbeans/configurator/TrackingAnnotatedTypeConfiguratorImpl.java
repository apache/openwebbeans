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
package org.apache.webbeans.configurator;

import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.configurator.AnnotatedConstructorConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedFieldConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedParameterConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import org.apache.webbeans.portable.AnnotatedTypeImpl;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TrackingAnnotatedTypeConfiguratorImpl<T> implements AnnotatedTypeConfigurator<T>
{

    private final AnnotatedTypeConfiguratorImpl<T> delegate;
    private final List<String> actions = new ArrayList<>();

    public TrackingAnnotatedTypeConfiguratorImpl(final AnnotatedTypeConfiguratorImpl<T> delegate)
    {
        this.delegate = delegate;
    }

    public String getPassivationId()
    {
        return actions.stream().collect(Collectors.joining(">>"));
    }

    @Override
    public final boolean equals(final Object o)
    {
        if (!(o instanceof TrackingAnnotatedTypeConfiguratorImpl))
        {
            return false;
        }

        final TrackingAnnotatedTypeConfiguratorImpl<?> that = (TrackingAnnotatedTypeConfiguratorImpl<?>) o;
        return getPassivationId().equals(that.getPassivationId());
    }

    @Override
    public int hashCode()
    {
        return getPassivationId().hashCode();
    }

    @Override
    public AnnotatedType<T> getAnnotated()
    {
        return delegate.getAnnotated();
    }

    @Override
    public AnnotatedTypeConfigurator<T> add(Annotation annotation)
    {
        actions.add("+@" + annotation.annotationType());
        delegate.add(annotation);
        return this;
    }

    @Override
    public AnnotatedTypeConfigurator<T> remove(Predicate<Annotation> predicate)
    {
        delegate.remove(a ->
        {
           if (predicate.test((Annotation) a))
           {
               actions.add("-@" + ((Annotation) a).annotationType());
               return true;
           }
           return false;
        });
        return this;
    }

    @Override
    public AnnotatedTypeConfigurator<T> removeAll()
    {
        actions.add("--");
        delegate.removeAll();
        return this;
    }

    @Override
    public Set<AnnotatedMethodConfigurator<? super T>> methods()
    {
        return delegate.methods().stream()
                       .map(m -> new TrackingAnnotatedMethodConfiguratorImpl<>(m, actions))
                       .collect(Collectors.toSet());
    }

    @Override
    public Stream<AnnotatedMethodConfigurator<? super T>> filterMethods(Predicate<AnnotatedMethod<? super T>> predicate)
    {
        return delegate.filterMethods(a ->
        {
            if (predicate.test(a))
            {
                actions.add("-m@" + a);
                return true;
            }
            return false;
        });
    }

    @Override
    public Set<AnnotatedFieldConfigurator<? super T>> fields()
    {
        return delegate.fields().stream()
                       .map(f -> new TrackingAnnotatedFieldConfiguratorImpl<>(f, actions))
                       .collect(Collectors.toSet());
    }

    @Override
    public Stream<AnnotatedFieldConfigurator<? super T>> filterFields(Predicate<AnnotatedField<? super T>> predicate)
    {
        return delegate.filterFields(a ->
        {
            if (predicate.test(a))
            {
                actions.add("-f@" + a);
                return true;
            }
            return false;
        });
    }

    @Override
    public Set<AnnotatedConstructorConfigurator<T>> constructors()
    {
        return delegate.constructors().stream()
                       .map(c -> new TrackingAnnotatedConstructorConfiguratorImpl<>(c, actions))
                       .collect(Collectors.toSet());
    }

    @Override
    public Stream<AnnotatedConstructorConfigurator<T>> filterConstructors(Predicate<AnnotatedConstructor<T>> predicate)
    {
        return delegate.filterConstructors(a ->
        {
            if (predicate.test(a))
            {
                actions.add("-c@" + a);
                return true;
            }
            return false;
        });
    }


    public AnnotatedTypeImpl<T> getNewAnnotatedType()
    {
        return delegate.getNewAnnotatedType();
    }

    public static class TrackingAnnotatedFieldConfiguratorImpl<T> implements AnnotatedFieldConfigurator<T>
    {
        private final AnnotatedFieldConfigurator<T> delegate;
        private final List<String> actions;

        public TrackingAnnotatedFieldConfiguratorImpl(final AnnotatedFieldConfigurator<T> delegate,
                                                      final List<String> actions)
        {
            this.delegate = delegate;
            this.actions = actions;
            this.actions.add("\n\t");
        }

        @Override
        public final boolean equals(final Object o)
        {
            if (!(o instanceof TrackingAnnotatedFieldConfiguratorImpl))
            {
                return false;
            }

            final TrackingAnnotatedFieldConfiguratorImpl<?> that = (TrackingAnnotatedFieldConfiguratorImpl<?>) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode()
        {
            return delegate.hashCode();
        }

        @Override
        public String toString()
        {
            return "TrackingField(" + delegate + ")";
        }

        @Override
        public AnnotatedField<T> getAnnotated()
        {
            return delegate.getAnnotated();
        }

        @Override
        public AnnotatedFieldConfigurator<T> removeAll()
        {
            actions.add("--");
            delegate.removeAll();
            return this;
        }

        @Override
        public AnnotatedFieldConfigurator<T> remove(final Predicate<Annotation> predicate)
        {
            delegate.remove(a ->
            {
                if (predicate.test(a))
                {
                    actions.add("-@" + a);
                    return true;
                }
                return false;
            });
            return this;
        }

        @Override
        public AnnotatedFieldConfigurator<T> add(final Annotation annotation)
        {
            actions.add("+@" + annotation.annotationType());
            delegate.add(annotation);
            return this;
        }
    }

    public static class TrackingAnnotatedMethodConfiguratorImpl<T> implements AnnotatedMethodConfigurator<T>
    {
        private final AnnotatedMethodConfigurator<T> delegate;
        private final List<String> actions;

        public TrackingAnnotatedMethodConfiguratorImpl(final AnnotatedMethodConfigurator<T> delegate,
                                                       final List<String> actions)
        {
            this.delegate = delegate;
            this.actions = actions;
            this.actions.add("\n\t");
        }

        @Override
        public final boolean equals(final Object o)
        {
            if (!(o instanceof TrackingAnnotatedMethodConfiguratorImpl))
            {
                return false;
            }

            final TrackingAnnotatedMethodConfiguratorImpl<?> that = (TrackingAnnotatedMethodConfiguratorImpl<?>) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode()
        {
            return delegate.hashCode();
        }

        @Override
        public String toString()
        {
            return "TrackingMethod(" + delegate + ")";
        }

        @Override
        public AnnotatedMethodConfigurator<T> add(final Annotation annotation)
        {
            actions.add("+@" + annotation.annotationType());
            delegate.add(annotation);
            return this;
        }

        @Override
        public Stream<AnnotatedParameterConfigurator<T>> filterParams(final Predicate<AnnotatedParameter<T>> predicate)
        {
            return delegate.filterParams(a ->
            {
                if (predicate.test(a))
                {
                    actions.add("-p@" + a);
                    return true;
                }
                return false;
            });
        }

        @Override
        public AnnotatedMethod<T> getAnnotated()
        {
            return delegate.getAnnotated();
        }

        @Override
        public List<AnnotatedParameterConfigurator<T>> params()
        {
            return delegate.params().stream()
                           .map(p -> new TrackingAnnotatedParameterConfiguratorImpl(p, actions))
                           .map(p -> (AnnotatedParameterConfigurator<T>) p)
                           .collect(Collectors.toList());
        }

        @Override
        public AnnotatedMethodConfigurator<T> remove(final Predicate<Annotation> predicate)
        {
            delegate.remove(a ->
            {
                if (predicate.test(a))
                {
                    actions.add("-@" + a.toString());
                    return true;
                }
                return false;
            });
            return this;
        }

        @Override
        public AnnotatedMethodConfigurator<T> removeAll()
        {
            actions.add("--");
            delegate.removeAll();
            return this;
        }
    }

    public static class TrackingAnnotatedConstructorConfiguratorImpl<T> implements AnnotatedConstructorConfigurator<T>
    {
        private final AnnotatedConstructorConfigurator<T> delegate;
        private final List<String> actions;

        public TrackingAnnotatedConstructorConfiguratorImpl(final AnnotatedConstructorConfigurator<T> delegate,
                                                            final List<String> actions)
        {
            this.delegate = delegate;
            this.actions = actions;
            this.actions.add("\n\t");
        }

        @Override
        public final boolean equals(final Object o)
        {
            if (!(o instanceof TrackingAnnotatedConstructorConfiguratorImpl))
            {
                return false;
            }

            final TrackingAnnotatedConstructorConfiguratorImpl<?> that =
                (TrackingAnnotatedConstructorConfiguratorImpl<?>) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode()
        {
            return delegate.hashCode();
        }

        @Override
        public String toString()
        {
            return "TrackingConstructor(" + delegate + ")";
        }

        @Override
        public AnnotatedConstructor<T> getAnnotated()
        {
            return delegate.getAnnotated();
        }

        @Override
        public AnnotatedConstructorConfigurator<T> add(final Annotation annotation)
        {
            actions.add("+@" + annotation.annotationType());
            delegate.add(annotation);
            return this;
        }

        @Override
        public AnnotatedConstructorConfigurator<T> remove(final Predicate<Annotation> predicate)
        {
            actions.add("-@" + predicate.toString());
            delegate.remove(predicate);
            return this;
        }

        @Override
        public AnnotatedConstructorConfigurator<T> removeAll()
        {
            actions.add("--");
            delegate.removeAll();
            return this;
        }

        @Override
        public List<AnnotatedParameterConfigurator<T>> params()
        {
            return delegate.params().stream()
                           .map(p -> new TrackingAnnotatedParameterConfiguratorImpl(p, actions))
                            .map(p -> (AnnotatedParameterConfigurator<T>) p)
                           .collect(Collectors.toList());
        }

        @Override
        public Stream<AnnotatedParameterConfigurator<T>> filterParams(final Predicate<AnnotatedParameter<T>> predicate)
        {
            return delegate.filterParams(a ->
             {
                if (predicate.test(a))
                {
                    actions.add("-p@" + a.toString());
                    return true;
                }
                return false;
            });
        }
    }

    public static class TrackingAnnotatedParameterConfiguratorImpl<T> implements AnnotatedParameterConfigurator<T>
    {
        private final AnnotatedParameterConfigurator<T> delegate;
        private final List<String> actions;

        public TrackingAnnotatedParameterConfiguratorImpl(final AnnotatedParameterConfigurator<T> delegate,
                                                          final List<String> actions)
        {
            this.delegate = delegate;
            this.actions = actions;
            this.actions.add("\n\t");
        }

        @Override
        public final boolean equals(final Object o)
        {
            if (!(o instanceof TrackingAnnotatedParameterConfiguratorImpl))
            {
                return false;
            }

            final TrackingAnnotatedParameterConfiguratorImpl<?> that =
                (TrackingAnnotatedParameterConfiguratorImpl<?>) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode()
        {
            return delegate.hashCode();
        }

        @Override
        public String toString()
        {
            return "TrackingParameter(" + delegate + ")";
        }

        @Override
        public AnnotatedParameterConfigurator<T> add(final Annotation annotation)
        {
            actions.add("+@" + annotation.annotationType());
            delegate.add(annotation);
            return this;
        }

        @Override
        public AnnotatedParameter<T> getAnnotated()
        {
            return delegate.getAnnotated();
        }

        @Override
        public AnnotatedParameterConfigurator<T> remove(final Predicate<Annotation> predicate)
        {
            delegate.remove(a ->
            {
                if (predicate.test(a))
                {
                    actions.add("-p@" + a.toString());
                    return true;
                }
                return false;
            });
            return this;
        }

        @Override
        public AnnotatedParameterConfigurator<T> removeAll()
        {
            actions.add("--");
            delegate.removeAll();
            return this;
        }
    }
}