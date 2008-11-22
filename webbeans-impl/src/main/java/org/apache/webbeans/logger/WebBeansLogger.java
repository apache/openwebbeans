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
package org.apache.webbeans.logger;

import org.apache.log4j.Logger;

/**
 * Wrapper class around the log4j logger class to include some checks
 * before the logs are actually written.
 * 
 * <p>
 * Actually, it is a thin layer on the log4j {@link Logger} implementation.
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class WebBeansLogger
{
	/**Inner logger object to log actual log messages*/
	private Logger logger = null;

	/**Private constructor*/
	private WebBeansLogger()
	{

	}
	

	/**
	 * Gets the new web beans logger instance.
	 * @param clazz own the return logger
	 * @return new logger
	 */
	public static WebBeansLogger getLogger(Class<?> clazz)
	{
		WebBeansLogger wbLogger = new WebBeansLogger();
		Logger inLogger = Logger.getLogger(clazz);
		
		wbLogger.setLogger(inLogger);
		
		return wbLogger;
	}

	public void fatal(String message)
	{
		checkNullLogger();
		logger.fatal(message);
	}
	
	public void fatal(String message,Throwable e)
	{
		checkNullLogger();
		logger.fatal(message,e);
		
	}
	
	public void error(Throwable e)
	{
		checkNullLogger();
		logger.error(e);
	}
	

	public void error(String message)
	{
		checkNullLogger();
		logger.error(message);
	}
	
	public void error(String message,Throwable e)
	{
		checkNullLogger();
		logger.error(message,e);
		
	}
	
	public void warn(String message)
	{
		checkNullLogger();
		logger.warn(message);
	}
	
	public void warn(String message,Throwable e)
	{
		checkNullLogger();
		logger.warn(message,e);
	}
	
	public void info(String message)
	{
		checkNullLogger();
		if(logger.isInfoEnabled())
			logger.info(message);
	}
	
	public void info(String message,Throwable e)
	{
		checkNullLogger();
		if(logger.isInfoEnabled())
			logger.info(message,e);
	}
	
	public void debug(String message)
	{
		checkNullLogger();
		if(logger.isDebugEnabled())
			logger.debug(message);
	}
	
	public void debug(String message,Throwable e)
	{
		checkNullLogger();
		if(logger.isDebugEnabled())
			logger.debug(message,e);
	}
	
	public void trace(String message)
	{
		checkNullLogger();
		if(logger.isTraceEnabled())
			logger.trace(message);
	}
	
	public void trace(String message,Throwable e)
	{
		checkNullLogger();
		if(logger.isTraceEnabled())
			logger.trace(message,e);
	}
	
	/**
	 * Sets the logger
	 * @param logger new logger instance
	 */
	public void setLogger(Logger logger)
	{
		this.logger = logger;
	}
	
	private void checkNullLogger()
	{
		if(this.logger == null)
		{
			throw new NullPointerException("Logger can not be null");
		}
	}
}
