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

/*
 * These are for use of JDK util logging.
 */
import java.util.logging.Logger;
import java.util.logging.Level;
 
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
    /** Log level mappings from OWB DEBUG, TRACE, INFO, WARN, ERROR, FATAL to whatever log
     *  levels the currently loaded logger supports (i.e. JUL provides FINEST, FINER, FINE,
     *  CONFIG, INFO, WARNING, SEVERE).
     */
    /* JDK util logger mappings */
    public final static Level WBL_DEBUG = Level.FINER;
    public final static Level WBL_TRACE = Level.FINE;
    public final static Level WBL_INFO = Level.INFO;
    public final static Level WBL_WARN = Level.WARNING;
    public final static Level WBL_ERROR = Level.SEVERE;
    public final static Level WBL_FATAL = Level.SEVERE;
       
    /** Inner logger object to log actual log messages */
    private Logger logger = null;
    private ResourceBundle wbBundle = null;
    private Class<?> caller = null;
    
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
        wbLogger.caller = clazz;
        Logger inLogger = Logger.getLogger(clazz.getName(),"openwebbeans/Messages");
        wbLogger.setLogger(inLogger);

        return wbLogger;
    }

    private void wblLog(Level log_level, String messageKey)
    {
        checkNullLogger();
        logger.logp(log_level, this.caller.getName(), Thread.currentThread().getStackTrace()[4].getMethodName(), messageKey);
    }

    private void wblLog(Level log_level, String messageKey, Object... args)
    {
        checkNullLogger();
        logger.logp(log_level, this.caller.getName(), Thread.currentThread().getStackTrace()[4].getMethodName(), messageKey, args);
    }

    private void wblLog(Level log_level, String messageKey, Throwable e)
    {
        checkNullLogger();
        logger.logp(log_level, this.caller.getName(), Thread.currentThread().getStackTrace()[4].getMethodName(), messageKey, e);
    }

    private void wblLog(Level log_level, Throwable e, String messageKey, Object... args)
    {
        checkNullLogger();
        logger.logp(log_level, this.caller.getName(), Thread.currentThread().getStackTrace()[3].getMethodName(), constructMessage(messageKey, args), e);
    }    
    
    public void fatal(String messageKey)
    {
        this.wblLog(WebBeansLogger.WBL_FATAL, messageKey);
    }

    public void fatal(String messageKey, Object args[])
    {
        this.wblLog(WebBeansLogger.WBL_FATAL, messageKey, args);
    }

    public void fatal(String messageKey, Throwable e)
    {
        this.wblLog(WebBeansLogger.WBL_FATAL, messageKey, e);
    }

    public void error(Throwable e)
    {
        this.wblLog(WebBeansLogger.WBL_ERROR, "", e);
    }

    public void error(String messageKey)
    {
        this.wblLog(WebBeansLogger.WBL_ERROR, messageKey);
    }

    public void error(String messageKey, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_ERROR, messageKey, args);
    }
    
    public void error(String messageKey, Throwable e)
    {
        this.wblLog(WebBeansLogger.WBL_ERROR, messageKey, e);
    }

    public void error(String messageKey, Throwable e, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_ERROR, messageKey, args, e);
    }

    public void warn(String messageKey)
    {
        this.wblLog(WebBeansLogger.WBL_WARN, messageKey);
    }

    public void warn(String messageKey, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_WARN, messageKey, args);
    }

    public void warn(String messageKey, Throwable e)
    {
        this.wblLog(WebBeansLogger.WBL_WARN, messageKey, e);
    }

    public void info(String messageKey)
    {
    	this.wblLog(WebBeansLogger.WBL_INFO, messageKey);
    }

    public void info(String messageKey, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_INFO, messageKey, args);
    }
    
    public void info(String messageKey, Throwable e)
    {
        this.wblLog(WebBeansLogger.WBL_INFO, messageKey, e);
    }

    public void debug(String messageKey)
    {
        this.wblLog(WebBeansLogger.WBL_DEBUG, messageKey);
    }

    public void debug(String messageKey, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_DEBUG, messageKey, args);
    }

    public void debug(String messageKey, Throwable e)
    {
        this.wblLog(WebBeansLogger.WBL_DEBUG, messageKey, e);
    }

    public void trace(String messageKey)
    {
        this.wblLog(WebBeansLogger.WBL_TRACE, messageKey);
    }

    public void trace(String messageKey, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_TRACE, messageKey, args);
    }

    public void trace(String messageKey, Throwable e)
    {
        this.wblLog(WebBeansLogger.WBL_TRACE, messageKey, e);
    }

    private String constructMessage(String messageKey, Object... args)
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
