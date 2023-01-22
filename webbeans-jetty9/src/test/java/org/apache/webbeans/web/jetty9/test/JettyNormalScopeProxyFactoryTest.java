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
package org.apache.webbeans.web.jetty9.test;

import java.io.*;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.web.jetty9.OwbConfiguration;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.session.*;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class JettyNormalScopeProxyFactoryTest
{
    private static final Logger log = Logger.getLogger(JettyNormalScopeProxyFactoryTest.class.getName());

    @Test
    public void checkDeserialisation() throws Exception
    {
        final File base = dir(new File("target/JettyNormalScopeProxyFactoryTest-" + System.nanoTime()));
        final File war = createWar(dir(new File(base, "test")), MyWrapper.class, MySessionScoped.class);

        String sessionId = null;
        FileSessionDataStore sessionDataStore = new FileSessionDataStore();
        sessionDataStore.setStoreDir(new File(base, "sessions"));
        for (final String expected : asList("init", "new"))
        {
            final Server jetty = new Server(0);
            Configuration.ClassList classList = Configuration.ClassList.serverDefault(jetty);
            classList.addBefore(
                    "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                    "org.eclipse.jetty.annotations.AnnotationConfiguration",
                    OwbConfiguration.class.getName()
            );
            WebAppContext ctx = new WebAppContext();
            ctx.setWar(war.getAbsolutePath());
            ctx.setContextPath("/test");
            ctx.setLogUrlOnStart(true);
            ctx.setConfigurationClasses(classList);
            SessionIdManager idmanager = new DefaultSessionIdManager(jetty);
            jetty.setSessionIdManager(idmanager);
            final SessionHandler sessionHandler = new SessionHandler();
            final SessionCache sessionCache = new DefaultSessionCache(sessionHandler);
            sessionCache.setSessionDataStore(sessionDataStore);
            sessionCache.setEvictionPolicy(900);
            sessionHandler.setSessionCache(sessionCache);
            ctx.setSessionHandler(sessionHandler);
            jetty.setHandler(ctx);

            jetty.start();

            try
            {
                Thread thread = Thread.currentThread();
                ClassLoader old = thread.getContextClassLoader();
                final ClassLoader webappLoader = ctx.getClassLoader();
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

                    HttpChannel channel = new HttpChannel(jetty.getConnectors()[0], new HttpConfiguration(), null, null)
                    {
                        @Override
                        public Server getServer()
                        {
                            return jetty;
                        }
                    };
                    Request request = new Request(channel, null);
                    request.setPathInfo("/test");
                    request.setContext(ctx.getServletContext());
                    request.setRequestedSessionId(sessionId);
                    request.setSessionHandler(ctx.getSessionHandler());
                    if (sessionId != null) {
                        // need to load the session into the request because we have a fake request
                        request.setSession(ctx.getSessionHandler().getSession(sessionId));
                    }

                    final ContextsService contextsService = WebBeansContext.currentInstance().getContextsService();
                    final ServletRequestEvent startParameter = new ServletRequestEvent(ctx.getServletContext(), request);
                    contextsService.startContext(RequestScoped.class, startParameter);

                    final HttpSession session = request.getSession();
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

                    sessionId = session.getId();
                    contextsService.endContext(RequestScoped.class, startParameter);

                    // don't do to not destroy the instance
                    // contextsService.endContext(SessionScoped.class, request.getSession());
                }
                catch (AssertionError e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    log.log(Level.SEVERE, "Exception during test execution", e);
//                    throw e;
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
                    jetty.stop();
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
