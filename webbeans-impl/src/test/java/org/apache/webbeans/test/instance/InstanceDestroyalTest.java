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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.TransientReference;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test how the destroyal of {@code Instance<T>} works.
 */
public class InstanceDestroyalTest extends AbstractUnitTest {

    @Test
    public void testDependentDestroy() {
        startContainer(SysInfoHolder.class, SysInfoProducer.class, SysInfo.class, System.class);

        SysInfoHolder sysInfoHolder = getInstance(SysInfoHolder.class);

        // only trigger the resolving once!
        List<SysInfo> sysInfos = sysInfoHolder.getSysInfos().stream()
                .collect(Collectors.toList());

        SysInfo transientSysInfo = sysInfos.stream()
                .filter(sysInfo -> "transient".equals(sysInfo.getName()))
                .findFirst().get();

        // transient sysInfo got cleaned up immediately
        Assert.assertTrue(transientSysInfo.amIClean());

        SysInfo storedSysInfo = sysInfos.stream()
                .filter(sysInfo -> "stored".equals(sysInfo.getName()))
                .findFirst().get();

        // transient sysInfo MUST NOT get cleaned up immediately
        Assert.assertFalse(storedSysInfo.amIClean());

        Assert.assertEquals(2, SysInfo.counter.get());

        endContext(RequestScoped.class);

        Assert.assertEquals(0, SysInfo.counter.get());
    }

    @Test
    public void testDependentManualDestroy() {
        startContainer(SysInfoHolder.class, SysInfoProducer.class, SysInfo.class, System.class);

        SysInfoHolder sysInfoHolder = getInstance(SysInfoHolder.class);

        // only trigger the resolving once!
        List<SysInfo> sysInfos = sysInfoHolder.getSysInfos().stream()
                .collect(Collectors.toList());

        SysInfo storedSysInfo = sysInfos.stream()
                .filter(sysInfo -> "stored".equals(sysInfo.getName()))
                .findFirst().get();

        // transient sysInfo MUST NOT get cleaned up immediately
        Assert.assertFalse(storedSysInfo.amIClean());

        Assert.assertEquals(2, SysInfo.counter.get());

        sysInfoHolder.getSysInfos().destroy(storedSysInfo);

        Assert.assertEquals(1, SysInfo.counter.get());

        endContext(RequestScoped.class);

        Assert.assertEquals(0, SysInfo.counter.get());
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

        @Produces
        @Dependent
        public SysInfo createTransientSysInfo(@TransientReference @System("dummy") SysInfo dummySysInfo) {
            dummySysInfo.setName("transient");

            return dummySysInfo;
        }

        @Produces
        @Dependent
        public SysInfo createStoredSysInfo(@System("dummy") SysInfo dummySysInfo) {
            dummySysInfo.setName("stored");

            return dummySysInfo;
        }
    }


    @Qualifier
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface System {
        String value();
    }

    @Dependent
    @System("dummy")
    public static class SysInfo {

        private static AtomicInteger counter = new AtomicInteger(0);

        private boolean imClean = false;
        private String name = "dummy";

        @PostConstruct
        public void countMeIn() {
            counter.incrementAndGet();
        }

        public boolean amIClean() {
            return imClean;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCounter() {
            return counter.get();
        }

        @PreDestroy
        public void cleanMeUp() {
            counter.decrementAndGet();
            imClean = true;
        }

    }
}
