/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.newtests.proxy.beans;

/**
 * This class just exists as container for some inner classes.
 */
public class DummyBean {
    public static class SomeInnerClass
    {
        private String val;

        public SomeInnerClass(String val) {
            this.val = val;
        }

        public String getVal()
        {
            return val;
        }
    }

    public static class SomeInnerException extends Exception
    {
        public SomeInnerException() {
            super();
        }

        public SomeInnerException(String message) {
            super(message);
        }

        public SomeInnerException(String message, Throwable cause) {
            super(message, cause);
        }

        public SomeInnerException(Throwable cause) {
            super(cause);
        }
    }
}
