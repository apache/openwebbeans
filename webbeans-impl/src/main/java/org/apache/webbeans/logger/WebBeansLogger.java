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
package org.apache.webbeans.logger;

import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Wrapper class around the log4j logger class to include some checks before the
 * logs are actually written.
 * <p>
 * Actually, it is a thin layer on the log4j {@link Logger} implementation.
 * </p>
 *
 * @version $Rev$ $Date$
 */
public final class WebBeansLogger
{
    /** Inner logger object to log actual log messages */
    private Logger logger = null;
    private ResourceBundle wbBundle = null;

    /** Private constructor */
    private WebBeansLogger()
    {
    	wbBundle = ResourceBundle.getBundle("openwebbeans/Messages");
    }

    /**
     * Gets the new web beans logger instance.
     * 
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

    public void fatal(String messageKey)
    {
        checkNullLogger();
        logger.fatal(getTokenString(messageKey));
    }

    public void fatal(String messageKey, Object args[])
    {
        checkNullLogger();
        logger.fatal(constructMessage(messageKey, args));
    }

    public void fatal(String messageKey, Throwable e)
    {
        checkNullLogger();
        logger.fatal(getTokenString(messageKey), e);

    }

    public void error(Throwable e)
    {
        checkNullLogger();
        logger.error(e);
    }

    public void error(String messageKey)
    {
        checkNullLogger();
        logger.error(getTokenString(messageKey));
    }

    public void error(String messageKey, Object args[])
    {
        checkNullLogger();
        logger.error(constructMessage(messageKey, args));
    }
    
    public void error(String messageKey, Throwable e)
    {
        checkNullLogger();
        logger.error(getTokenString(messageKey), e);

    }

    public void error(String messageKey, Object args[], Throwable e)
    {
        checkNullLogger();
        logger.error(constructMessage(messageKey, args), e);

    }

    public void warn(String messageKey)
    {
        checkNullLogger();
        logger.warn(getTokenString(messageKey));
    }

    public void warn(String messageKey, Object args[])
    {
        checkNullLogger();
        logger.warn(constructMessage(messageKey, args));
    }

    public void warn(String messageKey, Throwable e)
    {
        checkNullLogger();
        logger.warn(getTokenString(messageKey), e);
    }

    public void info(String messageKey)
    {
        checkNullLogger();
        if (logger.isInfoEnabled())
        {
            logger.info(getTokenString(messageKey));   
        }
    }

    public void info(String messageKey, Object args[])
    {
        checkNullLogger();
        if (logger.isInfoEnabled())
        {
            logger.info(constructMessage(messageKey, args));   
        }
    }
    
    public void info(String messageKey, Throwable e)
    {
        checkNullLogger();
        if (logger.isInfoEnabled())
        {
            logger.info(getTokenString(messageKey), e);   
        }
    }

    public void debug(String messageKey)
    {
        checkNullLogger();
        if (logger.isDebugEnabled())
        {
            logger.debug(getTokenString(messageKey));   
        }
    }

    public void debug(String messageKey, Object args[])
    {
        checkNullLogger();
        if (logger.isDebugEnabled())
        {
            logger.debug(constructMessage(messageKey, args));   
        }
    }

    public void debug(String messageKey, Throwable e)
    {
        checkNullLogger();
        if (logger.isDebugEnabled())
        {
            logger.debug(getTokenString(messageKey), e);   
        }
    }

    public void trace(String messageKey)
    {
        checkNullLogger();
        if (logger.isTraceEnabled())
        {
            logger.trace(getTokenString(messageKey));   
        }
    }

    public void trace(String messageKey, Object args[])
    {
        checkNullLogger();
        if (logger.isTraceEnabled())
        {
            logger.trace(constructMessage(messageKey, args));   
        }
    }

    public void trace(String messageKey, Throwable e)
    {
        checkNullLogger();
        if (logger.isTraceEnabled())
        {
            logger.trace(getTokenString(messageKey), e);   
        }
    }

    private String constructMessage(String messageKey, Object args[])
    {
    	MessageFormat msgFrmt;
    	String formattedString;
    	
    	msgFrmt = new MessageFormat(getTokenString(messageKey), Locale.getDefault());
    	formattedString = msgFrmt.format(args);
    	
    	return formattedString;
    }

    public String getTokenString(String messageKey)
    {
        String strVal = null;

        if (this.wbBundle == null)
        {
        	throw new NullPointerException("ResourceBundle can not be null");
        }
        try
        {
            strVal = wbBundle.getString(messageKey);
        }
        catch (MissingResourceException mre)
        {
        	strVal = null;
        }
        if (strVal == null)
        {
            return messageKey;
        }
        else
        {
            return strVal;
        }
    }

    /**
     * Sets the logger
     * 
     * @param logger new logger instance
     */
    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    private void checkNullLogger()
    {
        if (this.logger == null)
        {
            throw new NullPointerException("Logger can not be null");
        }
    }        
}
