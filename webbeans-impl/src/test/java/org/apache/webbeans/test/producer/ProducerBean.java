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
package org.apache.webbeans.test.producer;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import java.net.URI;

public class ProducerBean {

    @Produces
    @Named
    public String name1() {
        return "name1";
    }

    @Produces
    @Named
    public String getName2() {
        return "name2";
    }

    @Produces
    @Named
    public boolean isName3() {
        return true;
    }

    @Produces
    @Named("name4")
    public String producesName4() {
        return "name4";
    }

    @Produces
    @Named("name5")
    public String getName5() {
        return "name5";
    }

    @Produces
    @Named("name6")
    public boolean isName6() {
        return true;
    }

    @Produces
    public URI createUri()
    {
        return URI.create("http://invalid.invalid");
    }
}
