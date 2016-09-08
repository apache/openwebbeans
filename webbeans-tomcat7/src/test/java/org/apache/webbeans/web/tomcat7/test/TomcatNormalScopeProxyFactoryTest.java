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
package org.apache.webbeans.web.tomcat7.test;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.web.tomcat7.ContextLifecycleListener;
import org.junit.Test;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletRequestEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class TomcatNormalScopeProxyFactoryTest
{
    private static final Logger log = Logger.getLogger(TomcatNormalScopeProxyFactoryTest.class.getName());

    @Test
    public void checkDeserialisation() throws Exception
    {
        final File base = dir(new File("target/TomcatNormalScopeProxyFactoryTest-" + System.nanoTime()));
        final File war = createWar(dir(new File(base, "test")), MyWrapper.class, MySessionScoped.class);

        String sessionId = null;
        for (final String expected : asList("init", "new"))
        {
            final Tomcat tomcat = new Tomcat();
            tomcat.setPort(0);
            tomcat.setBaseDir(base.getAbsolutePath());

            final Context ctx = tomcat.addContext("/test", war.getAbsolutePath());
            ctx.addLifecycleListener(new ContextLifecycleListener());

            // needed for Java9
            if (ctx instanceof StandardContext) {
                ((StandardContext) ctx).setClearReferencesRmiTargets(false);
            }

            tomcat.start();

            try
            {
                Thread thread = Thread.currentThread();
                ClassLoader old = thread.getContextClassLoader();
                final ClassLoader webappLoader = ctx.getLoader().getClassLoader();
                thread.setContextClassLoader(webappLoader);
                try
                {
                    // we don't want test type but webapp one...even if named the same
                    final Class<?> webapptype = webappLoader.loadClass(MySessionScoped.class.getName());
                    final Method setValue = webapptype.getMethod("setValue", String.class);
                    final Method getValue = webapptype.getMethod("getValue");

                    final Class<?> wrapperType = webappLoader.loadClass(MyWrapper.class.getName());
                    final Method m = wrapperType.getMethod("getProxy");

                    final BeanManager bm = CDI.current().getBeanManager();

                    final Response response = new Response();
                    response.setConnector(tomcat.getConnector());
                    response.setCoyoteResponse(new org.apache.coyote.Response());

                    final Request request = new Request();
                    request.setContext(ctx);
                    request.setResponse(response);
                    request.setRequestedSessionId(sessionId);

                    final ContextsService contextsService = WebBeansContext.currentInstance().getContextsService();
                    final ServletRequestEvent startParameter = new ServletRequestEvent(ctx.getServletContext(), request);
                    contextsService.startContext(RequestScoped.class, startParameter);
                    if (request.getSession() != null)
                    {
                        contextsService.startContext(SessionScoped.class, request.getSession());
                    }

                    {
                        //final Object bean = bm.getReference(bm.resolve(bm.getBeans(webapptype)), webapptype, null);
                        final Object bean = m.invoke(bm.getReference(bm.resolve(bm.getBeans(wrapperType)), wrapperType, null));
                        assertEquals(expected, getValue.invoke(bean));
                        setValue.invoke(bean, "new");
                        assertEquals("new", getValue.invoke(bean));
                    }

                    sessionId = request.getSession().getId();
                    contextsService.endContext(RequestScoped.class, startParameter);

                    // don't do to not destroy the instance
                    // contextsService.endContext(SessionScoped.class, request.getSession());
                }
                catch (Exception e) {
                    log.log(Level.SEVERE, "Exception during test execution", e);
                }
                finally
                {
                    thread.setContextClassLoader(old);
                }
            }
            finally
            {
                try
                {
                    tomcat.stop();
                }
                catch (Exception e)
                {
                    log.log(Level.SEVERE, "This _might_ happen on Java9 currently. I hope it gets soon fixed.", e);
                }

            }
        }
    }

    private static File createWar(final File test, final Class<?>... classes) throws IOException
    {
        for (final Class<?> clazz : classes)
        {
            final String name = clazz.getName().replace('.', '/') + ".class";
            final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
            if (is == null)
            {
                throw new IllegalArgumentException(name);
            }
            final File out = new File(test, "WEB-INF/classes/" + name);
            dir(out.getParentFile());
            final OutputStream os = new FileOutputStream(out);
            IOUtils.copy(is, os);
            is.close();
            os.close();
        }
        final Writer w = new FileWriter(new File(test, "WEB-INF/beans.xml"));
        w.write("<beans />");
        w.close();
        return test;
    }

    private static File dir(final File file)
    {
        file.mkdirs();
        return file;
    }
}
