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
package javax.enterprise.inject;

/**
 * If injection point is not resolved by the container, it throws
 * unsatisfied exception.
 * 
 * @version $Rev$ $Date$
 */
public class UnsatisfiedResolutionException extends ResolutionException
{
    private static final long serialVersionUID = 8476513814387375212L;

    public UnsatisfiedResolutionException()
    {
        super();
    }

    public UnsatisfiedResolutionException(String message)
    {
        super(message);
    }

    public UnsatisfiedResolutionException(Throwable e)
    {
        super(e);
    }

    public UnsatisfiedResolutionException(String message, Throwable e)
    {
        super(message, e);
    }

}
