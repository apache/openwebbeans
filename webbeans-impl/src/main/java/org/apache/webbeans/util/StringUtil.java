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
package org.apache.webbeans.util;

/**
 * a few static string utility methods
 */
public final class StringUtil
{
    private StringUtil()
    {
        // private utility class ct
    }

    /**
     * @return {@code true} if the given string is either {@code null} or an empty string
     */
    public static boolean isEmpty(String val)
    {
        return val == null || val.length() == 0;
    }

    /**
     * @return {@code true} if the given string is not null and has a content
     */
    public static boolean isNotEmpty(String val)
    {
        return val != null && val.length() > 0;
    }

    /**
     * @return {@code true} if the given string is either {@code null} or an empty or blank string
     */
    public static boolean isBlank(String val)
    {
        return val == null || val.length() == 0 || val.trim().length() == 0;
    }
}
