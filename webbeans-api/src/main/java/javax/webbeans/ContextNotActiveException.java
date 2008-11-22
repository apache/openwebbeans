/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package javax.webbeans;

import javax.webbeans.manager.Context;

/**
 * If the {@link Context} is not avalaible in the time of
 * web beans component getting, this exception is thrown.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class ContextNotActiveException extends ExecutionException
{

	private static final long serialVersionUID = 4783816486073845333L;

	public ContextNotActiveException(String message)
	{
		super(message);
	}
	
	public ContextNotActiveException(Throwable e)
	{
		super(e);
	}
	
	public ContextNotActiveException(String message, Throwable e)
	{
		super(message, e);
	}
}
