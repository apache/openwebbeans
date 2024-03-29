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
package org.apache.webbeans.spi;

/**
 * SPI interface to implement the proxy defining logic.
 * It enables to switch from unsafe to classloader logic for instance for java @gt;= 9.
 */
public interface DefiningClassService
{
    /**
     * Returns the classloader to use to define the given class.
     * @param forClass the proxied class
     * @return the classloader to use to define the class
     */
    ClassLoader getProxyClassLoader(Class<?> forClass);

    /**
     * Register the proxy class from its bytecode.
     * @param name the proxy name
     * @param bytecode the bytecode to "define"
     * @param proxiedClass the original class
     * @param <T> type of the class to proxy
     * @return the proxy class
     */
    <T> Class<T> defineAndLoad(String name, byte[] bytecode, Class<T> proxiedClass);
}
