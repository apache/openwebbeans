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

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class ConsumerBean {

    @Inject
    @Named("name1")
    private String name1;

    @Inject
    @Named("name2")
    private String name2;

    @Inject
    @Named("name3")
    private boolean name3;

    @Inject
    @Named("name4")
    private String name4;

    @Inject
    @Named("name5")
    private String name5;

    @Inject
    @Named("name6")
    private boolean name6;

    public String getName1() {
        return name1;
    }

    public String getName2() {
        return name2;
    }

    public boolean isName3() {
        return name3;
    }

    public String getName4() {
        return name4;
    }

    public String getName5() {
        return name5;
    }

    public boolean isName6() {
        return name6;
    }
}
