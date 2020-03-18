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
package org.apache.openwebbeans.slf4j;

import org.slf4j.spi.LocationAwareLogger;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// mainly from cxf
class Slf4jLogger extends Logger
{
    private final org.slf4j.Logger logger;
    private final LocationAwareLogger locationAwareLogger;

    Slf4jLogger(final String name, final String resourceBundleName)
    {
        super(name, resourceBundleName);
        logger = org.slf4j.LoggerFactory.getLogger(name);
        if (LocationAwareLogger.class.isInstance(logger))
        {
            locationAwareLogger = LocationAwareLogger.class.cast(logger);
        }
        else
        {
            locationAwareLogger = null;
        }
    }

    @Override
    public void log(final LogRecord record)
    {
        if (isLoggable(record.getLevel()))
        {
            doLog(record);
        }
    }

    @Override
    public void log(final Level level, final String msg)
    {
        if (isLoggable(level))
        {
            doLog(new LogRecord(level, msg));
        }
    }

    @Override
    public void log(final Level level, final String msg, final Object param1)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setParameters(new Object[]{param1});
            doLog(lr);
        }
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setParameters(params);
            doLog(lr);
        }
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setThrown(thrown);
            doLog(lr);
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            doLog(lr);
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Object param1)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setParameters(new Object[]{param1});
            doLog(lr);
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, Object[] params)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setParameters(params);
            doLog(lr);
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Throwable thrown)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setThrown(thrown);
            doLog(lr);
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            doLog(lr, bundleName);
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
                      final String bundleName, final String msg, final Object param1)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setParameters(new Object[]{param1});
            doLog(lr, bundleName);
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
                      final String bundleName, final String msg, Object[] params)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setParameters(params);
            doLog(lr, bundleName);
        }
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
                      final String bundleName, final String msg, final Throwable thrown)
    {
        if (isLoggable(level))
        {
            final LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setThrown(thrown);
            doLog(lr, bundleName);
        }
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod)
    {
        if (isLoggable(Level.FINER))
        {
            logp(Level.FINER, sourceClass, sourceMethod, "ENTRY");
        }
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1)
    {
        if (isLoggable(Level.FINER))
        {
            logp(Level.FINER, sourceClass, sourceMethod, "ENTRY {0}", param1);
        }
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params)
    {
        if (isLoggable(Level.FINER))
        {
            final String msg = "ENTRY";
            if (params == null)
            {
                logp(Level.FINER, sourceClass, sourceMethod, msg);
                return;
            }
            final StringBuilder builder = new StringBuilder(msg);
            for (int i = 0; i < params.length; i++)
            {
                builder.append(" {");
                builder.append(i);
                builder.append('}');
            }
            logp(Level.FINER, sourceClass, sourceMethod, builder.toString(), params);
        }
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod)
    {
        if (isLoggable(Level.FINER))
        {
            logp(Level.FINER, sourceClass, sourceMethod, "RETURN");
        }
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result)
    {
        if (isLoggable(Level.FINER))
        {
            logp(Level.FINER, sourceClass, sourceMethod, "RETURN {0}", result);
        }
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown)
    {
        if (isLoggable(Level.FINER))
        {
            final LogRecord lr = new LogRecord(Level.FINER, "THROW");
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setThrown(thrown);
            doLog(lr);
        }
    }

    @Override
    public void severe(final String msg)
    {
        if (isLoggable(Level.SEVERE))
        {
            doLog(new LogRecord(Level.SEVERE, msg));
        }
    }

    @Override
    public void warning(final String msg)
    {
        if (isLoggable(Level.WARNING))
        {
            doLog(new LogRecord(Level.WARNING, msg));
        }
    }

    @Override
    public void info(final String msg)
    {
        if (isLoggable(Level.INFO))
        {
            doLog(new LogRecord(Level.INFO, msg));
        }
    }

    @Override
    public void config(final String msg)
    {
        if (isLoggable(Level.CONFIG))
        {
            doLog(new LogRecord(Level.CONFIG, msg));
        }
    }

    @Override
    public void fine(final String msg)
    {
        if (isLoggable(Level.FINE))
        {
            doLog(new LogRecord(Level.FINE, msg));
        }
    }

    @Override
    public void finer(final String msg)
    {
        if (isLoggable(Level.FINER))
        {
            doLog(new LogRecord(Level.FINER, msg));
        }
    }

    @Override
    public void finest(final String msg)
    {
        if (isLoggable(Level.FINEST))
        {
            doLog(new LogRecord(Level.FINEST, msg));
        }
    }

    @Override
    public void setLevel(final Level newLevel)
    {
        // no-op
    }

    private void doLog(final LogRecord lr)
    {
        lr.setLoggerName(getName());
        final String rbname = getResourceBundleName();
        if (rbname != null)
    {
            lr.setResourceBundleName(rbname);
            lr.setResourceBundle(getResourceBundle());
        }
        internalLog(lr);
    }

    private void doLog(final LogRecord lr, final String rbname)
    {
        lr.setLoggerName(getName());
        if (rbname != null)
        {
            lr.setResourceBundleName(rbname);
            lr.setResourceBundle(loadResourceBundle(rbname));
        }
        internalLog(lr);
    }

    private void internalLog(final LogRecord record)
    {
        final Filter filter = getFilter();
        if (filter != null && !filter.isLoggable(record))
        {
            return;
        }
        final String msg = formatMessage(record);
        internalLogFormatted(msg, record);
    }

    private String formatMessage(final LogRecord record)
    {
        final ResourceBundle catalog = record.getResourceBundle();
        String format = record.getMessage();
        if (catalog != null)
        {
            try
            {
                format = catalog.getString(record.getMessage());
            }
            catch (MissingResourceException ex)
            {
                format = record.getMessage();
            }
        }
        try
        {
            final Object[] parameters = record.getParameters();
            if (parameters == null || parameters.length == 0)
            {
                return format;
            }
            if (format.contains("{0") || format.contains("{1")
                    || format.contains("{2") || format.contains("{3"))
            {
                return java.text.MessageFormat.format(format, parameters);
            }
            return format;
        }
        catch (final Exception ex)
        {
            return format;
        }
    }

    private static ResourceBundle loadResourceBundle(final String resourceBundleName)
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (null != cl)
        {
            try
            {
                return ResourceBundle.getBundle(resourceBundleName, Locale.getDefault(), cl);
            }
            catch (final MissingResourceException e)
            {
                // no-op
            }
        }
        cl = ClassLoader.getSystemClassLoader();
        if (null != cl)
        {
            try
            {
                return ResourceBundle.getBundle(resourceBundleName, Locale.getDefault(), cl);
            }
            catch (final MissingResourceException e)
            {
                // no-op
            }
        }
        return null;
    }

    @Override
    public Level getLevel()
    {
        if (logger.isTraceEnabled())
        {
            return Level.FINEST;
        }
        else if (logger.isDebugEnabled())
        {
            return Level.FINER;
        }
        else if (logger.isInfoEnabled())
        {
            return Level.INFO;
        }
        else if (logger.isWarnEnabled())
        {
            return Level.WARNING;
        }
        else if (logger.isErrorEnabled())
        {
            return Level.SEVERE;
        }
        return Level.OFF;
    }

    @Override
    public boolean isLoggable(final Level level)
    {
        final int i = level.intValue();
        if (i == Level.OFF.intValue())
        {
            return false;
        }
        else if (i >= Level.SEVERE.intValue())
        {
            return logger.isErrorEnabled();
        }
        else if (i >= Level.WARNING.intValue())
        {
            return logger.isWarnEnabled();
        }
        else if (i >= Level.INFO.intValue())
        {
            return logger.isInfoEnabled();
        }
        else if (i >= Level.FINER.intValue())
        {
            return logger.isDebugEnabled();
        }
        return logger.isTraceEnabled();
    }

    private void internalLogFormatted(final String msg, final LogRecord record)
    {
        final Level level = record.getLevel();
        final Throwable t = record.getThrown();
        final Handler[] targets = getHandlers();
        if (targets != null)
        {
            for (Handler h : targets)
            {
                h.publish(record);
            }
        }
        if (!getUseParentHandlers())
        {
            return;
        }

        if (Level.FINE.equals(level))
        {
            if (locationAwareLogger == null)
            {
                logger.debug(msg, t);
            }
            else
            {
                locationAwareLogger.log(null, Logger.class.getName(), LocationAwareLogger.DEBUG_INT, msg, null, t);
            }
        }
        else if (Level.INFO.equals(level))
        {
            if (locationAwareLogger == null)
            {
                logger.info(msg, t);
            }
            else
            {
                locationAwareLogger.log(null, Logger.class.getName(), LocationAwareLogger.INFO_INT, msg, null, t);
            }
        }
        else if (Level.WARNING.equals(level))
        {
            if (locationAwareLogger == null)
            {
                logger.warn(msg, t);
            }
            else
            {
                locationAwareLogger.log(null, Logger.class.getName(), LocationAwareLogger.WARN_INT, msg, null, t);
            }
        }
        else if (Level.FINER.equals(level))
        {
            if (locationAwareLogger == null)
            {
                logger.trace(msg, t);
            }
            else
            {
                locationAwareLogger.log(null, Logger.class.getName(), LocationAwareLogger.DEBUG_INT, msg, null, t);
            }
        }
        else if (Level.FINEST.equals(level))
        {
            if (locationAwareLogger == null)
            {
                logger.trace(msg, t);
            }
            else
            {
                locationAwareLogger.log(null, Logger.class.getName(), LocationAwareLogger.TRACE_INT, msg, null, t);
            }
        }
        else if (Level.ALL.equals(level))
        {
            if (locationAwareLogger == null)
            {
                logger.error(msg, t);
            }
            else
            {
                locationAwareLogger.log(null, Logger.class.getName(), LocationAwareLogger.ERROR_INT, msg, null, t);
            }
        }
        else if (Level.SEVERE.equals(level))
        {
            if (locationAwareLogger == null)
            {
                logger.error(msg, t);
            }
            else
            {
                locationAwareLogger.log(null, Logger.class.getName(), LocationAwareLogger.ERROR_INT, msg, null, t);
            }
        }
        else if (Level.CONFIG.equals(level))
        {
            if (locationAwareLogger == null)
            {
                logger.debug(msg, t);
            }
            else
            {
                locationAwareLogger.log(null, Logger.class.getName(), LocationAwareLogger.DEBUG_INT, msg, null, t);
            }
        }
    }
}
