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
package org.apache.webbeans.test.bean;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.SerializableBeanVault;
import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.Producer;

import static org.junit.Assert.assertTrue;

public class SerializableBeanEqualTest {
    public static class MyContextual<T> implements OwbBean<T>, PassivationCapable, Serializable {
        public Producer<T> getProducer() {
            return null;
        }

        public WebBeansType getWebBeansType() {
            return null;
        }

        public Class<T> getReturnType() {
            return null;
        }

        public void setSpecializedBean(boolean specialized) {
            // no-op
        }

        public boolean isSpecializedBean() {
            return false;
        }

        public void setEnabled(boolean enabled) {
            // no-op
        }

        public boolean isEnabled() {
            return true;
        }

        public boolean isPassivationCapable() {
            return true;
        }

        public boolean isDependent() {
            return false;
        }

        public WebBeansContext getWebBeansContext() {
            return null;
        }

        @Override
        public T create(CreationalContext<T> context) {
            return null;
        }

        @Override
        public void destroy(T instance, CreationalContext<T> context) {

        }

        @Override
        public String getId() {
            return "test";
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return null;
        }

        @Override
        public Class<?> getBeanClass() {
            return null;
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public Set<Type> getTypes() {
            return null;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return null;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return null;
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || getId().equals(PassivationCapable.class.cast(obj).getId());
        }
    }

    @Test
    public void areEquals() {
        final MyContextual original = new MyContextual();
        final Contextual<?> bean = new SerializableBeanVault().getSerializableBean(original);
        assertTrue(bean.equals(original));
        assertTrue(original.equals(bean));
        assertTrue(original.equals(original));
        assertTrue(bean.equals(bean));
    }
}
