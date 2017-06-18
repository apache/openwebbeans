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

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.configurator.BeanConfigurator;
import javax.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.creation.BeanAttributesBuilder;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.GenericsUtil;

//X TODO finish. Impossible to implement right now as the spec is ambiguous
//X TODO producer part
public class BeanConfiguratorImpl<T> implements BeanConfigurator<T>
{
    private final WebBeansContext webBeansContext;

    private Class<?> beanClass;
    private String passivationId;
    private Class<? extends Annotation> scope = Dependent.class;
    private String name;
    private boolean alternative;

    private Set<Type> typeClosures = new LinkedHashSet<>();
    private Set<InjectionPoint> injectionPoints = new HashSet<>();
    private Set<Annotation> qualifiers = new HashSet<>();
    private Set<Class<? extends Annotation>> stereotypes = new HashSet<>();

    private Function<CreationalContext<?>, ?> createWithCallback;
    private BiConsumer<T, CreationalContext<T>> destroyWithCallback;

    private Function<Instance<Object>, ?> produceWithCallback;
    private BiConsumer<T, Instance<Object>> disposeWithCallback;

    public BeanConfiguratorImpl(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    @Override
    public BeanConfigurator<T> beanClass(Class<?> beanClass)
    {
        this.beanClass = beanClass;
        return this;
    }

    @Override
    public BeanConfigurator<T> addInjectionPoint(InjectionPoint injectionPoint)
    {
        this.injectionPoints.add(injectionPoint);
        return this;
    }

    @Override
    public BeanConfigurator<T> addInjectionPoints(InjectionPoint... injectionPoints)
    {
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            this.injectionPoints.add(injectionPoint);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addInjectionPoints(Set<InjectionPoint> injectionPoints)
    {
        this.injectionPoints.addAll(injectionPoints);
        return this;
    }

    @Override
    public BeanConfigurator<T> injectionPoints(InjectionPoint... injectionPoints)
    {
        this.injectionPoints.clear();
        addInjectionPoints(injectionPoints);
        return this;
    }

    @Override
    public BeanConfigurator<T> injectionPoints(Set<InjectionPoint> injectionPoints)
    {
        this.injectionPoints.clear();
        addInjectionPoints(injectionPoints);
        return this;
    }

    @Override
    public BeanConfigurator<T> id(String id)
    {
        this.passivationId = id;
        return this;
    }

    @Override
    public <U extends T> BeanConfigurator<U> createWith(Function<CreationalContext<U>, U> callback)
    {
        this.createWithCallback = (Function) callback;
        return (BeanConfigurator) this;
    }

    @Override
    public <U extends T> BeanConfigurator<U> produceWith(Function<Instance<Object>, U> callback)
    {
        this.produceWithCallback = callback;
        return (BeanConfigurator) this;
    }

    @Override
    public BeanConfigurator<T> destroyWith(BiConsumer<T, CreationalContext<T>> callback)
    {
        this.destroyWithCallback = callback;
        return this;
    }

    @Override
    public BeanConfigurator<T> disposeWith(BiConsumer<T, Instance<Object>> callback)
    {
        this.disposeWithCallback = callback;
        return this;
    }

    @Override
    public <U extends T> BeanConfigurator<U> read(AnnotatedType<U> type)
    {
        read(BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(type).build());
        return (BeanConfigurator<U>) this;
    }

    @Override
    public BeanConfigurator<T> read(BeanAttributes<?> beanAttributes)
    {
        this.stereotypes.addAll(beanAttributes.getStereotypes());
        this.scope = beanAttributes.getScope();
        this.name = beanAttributes.getName();
        this.alternative = beanAttributes.isAlternative();
        types(beanAttributes.getTypes());
        qualifiers(beanAttributes.getQualifiers());
        stereotypes(beanAttributes.getStereotypes());

        return this;
    }

    @Override
    public BeanConfigurator<T> addType(Type type)
    {
        this.typeClosures.add(type);
        return this;
    }

    @Override
    public BeanConfigurator<T> addType(TypeLiteral<?> typeLiteral)
    {
        this.typeClosures.add(typeLiteral.getType());
        return this;
    }

    @Override
    public BeanConfigurator<T> addTypes(Type... types)
    {
        for (Type type : types)
        {
            addType(type);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addTypes(Set<Type> types)
    {
        for (Type type : types)
        {
            addType(type);

        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addTransitiveTypeClosure(Type type)
    {
        Set<Type> typeClosure = GenericsUtil.getTypeClosure(type, type);
        addTypes(typeClosure);
        return this;
    }

    @Override
    public BeanConfigurator<T> types(Type... types)
    {
        this.typeClosures.clear();
        addTypes(types);
        return this;
    }

    @Override
    public BeanConfigurator<T> types(Set<Type> types)
    {
        this.typeClosures.clear();
        addTypes(types);
        return this;
    }

    @Override
    public BeanConfigurator<T> scope(Class<? extends Annotation> scope)
    {
        this.scope = scope;
        return this;
    }

    @Override
    public BeanConfigurator<T> addQualifier(Annotation qualifier)
    {
        this.qualifiers.add(qualifier);
        return this;
    }

    @Override
    public BeanConfigurator<T> addQualifiers(Annotation... qualifiers)
    {
        for (Annotation qualifier : qualifiers)
        {
            this.qualifiers.add(qualifier);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addQualifiers(Set<Annotation> qualifiers)
    {
        this.qualifiers.addAll(qualifiers);
        return this;
    }

    @Override
    public BeanConfigurator<T> qualifiers(Annotation... qualifiers)
    {
        this.qualifiers.clear();
        addQualifiers(qualifiers);
        return this;
    }

    @Override
    public BeanConfigurator<T> qualifiers(Set<Annotation> qualifiers)
    {
        this.qualifiers.clear();
        addQualifiers(qualifiers);
        return this;
    }

    @Override
    public BeanConfigurator<T> addStereotype(Class<? extends Annotation> stereotype)
    {
        this.stereotypes.add(stereotype);
        return this;
    }

    @Override
    public BeanConfigurator<T> addStereotypes(Set<Class<? extends Annotation>> stereotypes)
    {
        for (Class<? extends Annotation> stereotype : stereotypes)
        {
            this.stereotypes.add(stereotype);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> stereotypes(Set<Class<? extends Annotation>> stereotypes)
    {
        this.stereotypes.clear();
        addStereotypes(stereotypes);
        return this;
    }

    @Override
    public BeanConfigurator<T> name(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public BeanConfigurator<T> alternative(boolean alternative)
    {
        this.alternative = alternative;
        return this;
    }

    public Bean<?> getBean()
    {
        return new ConstructedBean();
    }

    /**
     * 1:1 with the BeanConfigurator.
     */
    public class ConstructedBean implements OwbBean<T>, PassivationCapable
    {
        private final Class<T> returnType;
        private final boolean dependent;
        private boolean specialized;
        private boolean enabled = true;

        public ConstructedBean()
        {
            //X TODO calculate return type from the typeClosures properly
            this.returnType = beanClass != null ? Class.class.cast(beanClass) : (typeClosures.isEmpty() ? null :
                    Class.class.cast(typeClosures.stream().filter(Class.class::isInstance).findFirst().orElse(null)));

            dependent = !webBeansContext.getBeanManagerImpl().isNormalScope(scope);

            if (createWithCallback == null && produceWithCallback == null)
            {
                WebBeansConfigurationException e = new WebBeansConfigurationException("Either a createCallback or a produceCallback must be set " + toString());
                webBeansContext.getBeanManagerImpl().getErrorStack().pushError(e);
            }
            if (createWithCallback != null && produceWithCallback != null)
            {
                WebBeansConfigurationException e = new WebBeansConfigurationException("Only exactly one of createCallback and produceCallback must be set " + toString());
                webBeansContext.getBeanManagerImpl().getErrorStack().pushError(e);
            }
        }

        @Override
        public Set<Type> getTypes()
        {
            return typeClosures;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints()
        {
            return injectionPoints;
        }

        @Override
        public Set<Annotation> getQualifiers()
        {
            return qualifiers;
        }

        @Override
        public Producer<T> getProducer()
        {
            //X TODO
            return null;
        }

        @Override
        public Class<?> getBeanClass()
        {
            return beanClass == null ? returnType : beanClass;
        }

        @Override
        public Class<? extends Annotation> getScope()
        {
            return scope;
        }

        @Override
        public WebBeansType getWebBeansType()
        {
            return WebBeansType.CONFIGURED;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public Class<T> getReturnType()
        {
            return returnType;
        }

        @Override
        public T create(CreationalContext<T> context)
        {
            return (T) createWithCallback.apply(context);
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes()
        {
            return stereotypes;
        }

        @Override
        public void setSpecializedBean(boolean specialized)
        {
            this.specialized = specialized;
        }

        @Override
        public boolean isAlternative()
        {
            return alternative;
        }

        @Override
        public boolean isNullable()
        {
            return false;
        }

        @Override
        public void destroy(T instance, CreationalContext<T> context)
        {
            destroyWithCallback.accept(instance, context);
        }

        @Override
        public boolean isSpecializedBean()
        {
            return specialized;
        }

        @Override
        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        @Override
        public boolean isEnabled()
        {
            return enabled;
        }

        @Override
        public String getId()
        {
            return passivationId;
        }

        @Override
        public boolean isPassivationCapable()
        {
            return passivationId != null;
        }

        @Override
        public boolean isDependent()
        {
            return dependent;
        }

        @Override
        public WebBeansContext getWebBeansContext()
        {
            return webBeansContext;
        }

        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            String simpleName = getReturnType().getSimpleName();
            builder.append(simpleName);
            builder.append(", WebBeansType:").append(getWebBeansType()).append(", Name:").append(getName());
            builder.append(", API Types:[");

            int size = getTypes().size();
            int index = 1;
            for(Type clazz : getTypes())
            {
                if(clazz instanceof Class)
                {
                    builder.append(((Class<?>)clazz).getName());
                }
                else
                {
                    ParameterizedType parameterizedType = (ParameterizedType) clazz;
                    Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                    builder.append(rawType.getName());
                    builder.append("<");
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length > 0)
                    {
                        for (Type actualType : actualTypeArguments)
                        {
                            if (Class.class.isInstance(actualType))
                            {
                                builder.append(Class.class.cast(actualType).getName().replace("java.lang.", ""));
                            }
                            else
                            {
                                builder.append(actualType);
                            }
                            builder.append(",");
                        }
                    }
                    builder.delete(builder.length() - 1, builder.length());
                    builder.append(">");

                }

                if(index < size)
                {
                    builder.append(",");
                }

                index++;
            }

            builder.append("], ");
            builder.append("Qualifiers:[");

            size = getQualifiers().size();
            index = 1;
            for(Annotation ann : getQualifiers())
            {
                builder.append(ann.annotationType().getName());

                if(index < size)
                {
                    builder.append(",");
                }

                index++;
            }

            builder.append("]");

            return builder.toString();

        }
    }
}
