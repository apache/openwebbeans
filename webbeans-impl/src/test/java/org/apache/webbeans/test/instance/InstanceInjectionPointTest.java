/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.instance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test that InjectionPoint works in case of {@code Instance<T>}.
 */
public class InstanceInjectionPointTest extends AbstractUnitTest {

    @Test
    public void testInstanceInjectionPointHandling() {
        startContainer(SysInfoHolder.class, SysInfoProducer.class, System.class);

        SysInfoProducer.injectionPoints.clear();

        SysInfoHolder sysInfoHolder = getInstance(SysInfoHolder.class);
        Assert.assertNotNull(sysInfoHolder);
        List<SysInfo> sysInfos = sysInfoHolder.getSysInfos().stream().collect(Collectors.toList());
        Assert.assertNotNull(sysInfos);
        Assert.assertEquals(1, SysInfoProducer.injectionPoints.size());

        InjectionPoint injectionPoint = SysInfoProducer.injectionPoints.get(0);
        AnnotatedField af = (AnnotatedField) injectionPoint.getAnnotated();

        Assert.assertEquals("sysInfos", af.getJavaMember().getName());
        Assert.assertEquals(SysInfoHolder.class, af.getJavaMember().getDeclaringClass());
    }


    @RequestScoped
    public static  class SysInfoHolder {


        private @Inject @Any Instance<SysInfo> sysInfos;

        public Instance<SysInfo> getSysInfos() {
            return sysInfos;
        }
    }

    @ApplicationScoped
    public static class SysInfoProducer {
        public static List<InjectionPoint> injectionPoints = new ArrayList<>();

        @Produces
        @Dependent
        public SysInfo createProductionSysInfo(InjectionPoint injectionPoint) {
            SysInfo sysInfo = new SysInfo();
            sysInfo.setName("production");

            injectionPoints.add(injectionPoint);

            return sysInfo;
        }
    }


    @Qualifier
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface System {
        String value();
    }

    @Dependent
    public static class SysInfo {
        private String name = "dummy";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


}
