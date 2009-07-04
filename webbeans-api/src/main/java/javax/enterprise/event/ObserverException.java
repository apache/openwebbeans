/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package javax.enterprise.event;

/**
 * Exception related with observers.
 * 
 * @version $Rev$ $Date$
 *
 */
public class ObserverException extends RuntimeException
{
    private static final long serialVersionUID = -6392759733390664652L;

    /**
     * Creates new exception.
     */
    public ObserverException()
    {

    }

    /**
     * Creates new exception with message.
     * 
     * @param message exception message
     */
    public ObserverException(String message)
    {
        super(message);
    }

    /**
     * Creates new exception with cause.
     * 
     * @param cause exception cause
     */
    public ObserverException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Creates new exception with given message and cause.
     * 
     * @param message exception message
     * @param cause exception cause
     */
    public ObserverException(String message, Throwable cause)
    {
        super(message, cause);
    }

}