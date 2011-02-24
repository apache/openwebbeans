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
package org.apache.webbeans.logger;

/*
 * These are for use of JDK util logging.
 */
import java.util.logging.Logger;
import java.util.logging.Level;
 
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
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
public final class WebBeansLogger implements Serializable, Externalizable
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
    private transient Logger logger = null;
    private transient ResourceBundle wbBundle = null;
    private Class<?> caller = null;
    private Locale locale = null;
    
    /** Private constructor */
    public WebBeansLogger()
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

    /**
     * Gets the new web beans logger instance.
     * 
     * @param clazz own the return logger
     * @param desiredLocale Locale used to select the Message resource bundle. 
     * @return new logger
     */
    public static WebBeansLogger getLogger(Class<?> clazz, Locale desiredLocale)
    {
        WebBeansLogger wbLogger = new WebBeansLogger();
        wbLogger.caller = clazz;
        wbLogger.locale = desiredLocale;
        Logger inLogger = Logger.getLogger(clazz.getName(), ResourceBundle.getBundle("openwebbeans/Messages", desiredLocale).toString());
        wbLogger.setLogger(inLogger);

        return wbLogger;
    }

    private void wblLog(Level level, String messageKey)
    {
        if (logger.isLoggable(level))
        {
            logger.logp(level, this.caller.getName(), Thread.currentThread().getStackTrace()[3].getMethodName(), messageKey);
        }
    }

    private void wblLog(Level level, String messageKey, Object... args)
    {
        if (logger.isLoggable(level))
        {
            logger.logp(level, this.caller.getName(), Thread.currentThread().getStackTrace()[3].getMethodName(), messageKey, args);
        }
    }

    private void wblLog(Level level, Throwable e, String messageKey)
    {
        if (logger.isLoggable(level))
        {
            logger.logp(level, this.caller.getName(), Thread.currentThread().getStackTrace()[3].getMethodName(), messageKey, e);
        }
    }

    private void wblLog(Level level, Throwable e, String messageKey, Object... args)
    {
        if (logger.isLoggable(level))
        {
            logger.logp(level, this.caller.getName(), Thread.currentThread().getStackTrace()[3].getMethodName(), constructMessage(messageKey, args), e);
        }
    }
    
    public void fatal(String messageKey)
    {
        this.wblLog(WebBeansLogger.WBL_FATAL, messageKey);
    }

    public void fatal(String messageKey, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_FATAL, messageKey, args);
    }

    public void fatal(Throwable e, String messageKey)
    {
        this.wblLog(WebBeansLogger.WBL_FATAL, e, messageKey);
    }

    public void fatal(Throwable e, String messageKey, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_FATAL, e, messageKey, args);
    }


    public void error(Throwable e)
    {
        this.wblLog(WebBeansLogger.WBL_ERROR, e, "");
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
        this.wblLog(WebBeansLogger.WBL_ERROR, e, messageKey);
    }

    public void error(String messageKey, Throwable e, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_ERROR, e, messageKey, args);
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
        this.wblLog(WebBeansLogger.WBL_WARN, e, messageKey);
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
        this.wblLog(WebBeansLogger.WBL_INFO, e, messageKey);
    }

    public void debug(String messageKey)
    {
        this.wblLog(WebBeansLogger.WBL_DEBUG, messageKey);
    }

    public void debug(String messageKey, Throwable e)
    {
        this.wblLog(WebBeansLogger.WBL_DEBUG, e, messageKey);
    }

    public void debug(String messageKey, Object... args)
    {
        this.wblLog(WebBeansLogger.WBL_DEBUG, messageKey, args);
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
        this.wblLog(WebBeansLogger.WBL_TRACE, e, messageKey);
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

        return strVal;
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

    public boolean wblWillLogFatal()
    {
        return (logger.isLoggable(WebBeansLogger.WBL_FATAL));
    }
    
    public boolean wblWillLogError()
    {
        return (logger.isLoggable(WebBeansLogger.WBL_ERROR));
    }
    
    public boolean wblWillLogWarn()
    {
        return (logger.isLoggable(WebBeansLogger.WBL_WARN));
    }
    
    public boolean wblWillLogInfo()
    {
        return (logger.isLoggable(WebBeansLogger.WBL_INFO));
    }
    
    public boolean wblWillLogDebug()
    {
        return (logger.isLoggable(WebBeansLogger.WBL_DEBUG));
    }
    
    public boolean wblWillLogTrace()
    {
        return (logger.isLoggable(WebBeansLogger.WBL_TRACE));
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException 
    {
        out.writeObject(caller);
        out.writeObject(locale);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException 
    {
        caller = (Class<?>)in.readObject();
        locale = (Locale)in.readObject();
        Logger inLogger = null;
        if (locale == null) 
        {
            inLogger = Logger.getLogger(caller.getName(),"openwebbeans/Messages");
        } 
        else
        {
            inLogger = Logger.getLogger(caller.getName(), ResourceBundle.getBundle("openwebbeans/Messages", locale).toString());
        }
        this.setLogger(inLogger);
    }
}
