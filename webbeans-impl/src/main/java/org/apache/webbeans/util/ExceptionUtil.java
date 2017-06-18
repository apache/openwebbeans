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

import org.apache.webbeans.exception.helper.DescriptiveException;

public abstract class ExceptionUtil
{
    private ExceptionUtil()
    {
        // prevent instantiation
    }

    /**
     * Throws the given Exception as RuntimeException
     * @return null; this is just for IDEs to allow them detect the end of the control flow
     */
    public static RuntimeException throwAsRuntimeException(Throwable throwable)
    {
        //Attention: helper which allows to use a trick to throw
        // a catched checked exception without a wrapping exception
        new ExceptionHelper<RuntimeException>().throwException(throwable);
        return null; //not needed due to the helper trick, but it's easier for using it
    }


    /**
     * This is for debugging/logging purpose only!
     * @return The stack trace of the current Thread.
     */
    public static String currentStack()
    {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder(500);
        for (StackTraceElement ste : stackTrace)
        {
            sb.append("  ").append(ste.getClassName()).append("#").append(ste.getMethodName()).append(":").append(ste.getLineNumber()).append('\n');
        }
        return sb.toString();
    }

    public static RuntimeException addInformation(RuntimeException e, String additionalinfo)
    {
        if (e instanceof DescriptiveException)
        {
            ((DescriptiveException) e).addInformation(additionalinfo);
        }

        return e;
    }

    @SuppressWarnings("unchecked")
    private static class ExceptionHelper<T extends Throwable>
    {
        private void throwException(Throwable exception) throws T
        {
            try
            {
                //exception-type is only checked at compile-time
                throw (T) exception;
            }
            catch (ClassCastException e)
            {
                //doesn't happen with existing JVMs! - if that changes the local ClassCastException needs to be ignored
                //-> throw original exception
                if (e.getStackTrace()[0].toString().contains(getClass().getName()))
                {
                    if (exception instanceof RuntimeException)
                    {
                        throw (RuntimeException) exception;
                    }
                    throw new RuntimeException(exception);
                }
                //if the exception to throw is a ClassCastException, throw it
                throw e;
            }
        }
    }
}
