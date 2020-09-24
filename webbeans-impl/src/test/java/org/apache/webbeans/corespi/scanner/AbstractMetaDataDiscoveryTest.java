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
package org.apache.webbeans.corespi.scanner;

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.emptyMap;
import static org.apache.xbean.asm9.ClassWriter.COMPUTE_FRAMES;
import static org.apache.xbean.asm9.Opcodes.ACC_PUBLIC;
import static org.apache.xbean.asm9.Opcodes.ACC_SUPER;
import static org.apache.xbean.asm9.Opcodes.ALOAD;
import static org.apache.xbean.asm9.Opcodes.INVOKESPECIAL;
import static org.apache.xbean.asm9.Opcodes.RETURN;
import static org.apache.xbean.asm9.Opcodes.V1_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.MethodVisitor;
import org.apache.xbean.asm9.Type;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AbstractMetaDataDiscoveryTest
{
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void isAnonymous() throws Exception
    {
        final AbstractMetaDataDiscovery mock = new AbstractMetaDataDiscovery()
        {
            @Override
            protected void configure()
            {
                // no-op
            }
        };
        final Method mtd = AbstractMetaDataDiscovery.class.getDeclaredMethod("isAnonymous", String.class);
        mtd.setAccessible(true);
        assertFalse(Boolean.class.cast(mtd.invoke(mock, AbstractMetaDataDiscoveryTest.class.getName())));
        assertTrue(Boolean.class.cast(mtd.invoke(mock, AbstractMetaDataDiscoveryTest.class.getName() + "$1")));
        assertTrue(Boolean.class.cast(mtd.invoke(mock, AbstractMetaDataDiscoveryTest.class.getName() + "$1$2")));
        assertTrue(Boolean.class.cast(mtd.invoke(mock, AbstractMetaDataDiscoveryTest.class.getName() + "$15$222")));
    }

    @Test
    public void skipExtensionJarScanning() throws Exception
    {
        // we create a module with some scanned beans
        final URL scannedModule = createScannedModule();

        // we create another module with some elligible beans and an extension
        final URL extensionModule = createExtensionModule();

        final Thread thread = Thread.currentThread();
        final ClassLoader oldLoader = thread.getContextClassLoader();
        final URL[] urls = {scannedModule, extensionModule};
        try (final URLClassLoader loader = new URLClassLoader(urls, new ClassLoader() {
            @Override
            public Class<?> loadClass(final String name) throws ClassNotFoundException
            {
                return oldLoader.loadClass(name);
            }

            @Override
            public URL getResource(final String name)
            {
                return oldLoader.getResource(name);
            }

            @Override
            public Enumeration<URL> getResources(final String name) throws IOException
            {
                if ("META-INF".equals(name) || "".equals(name)) // scanning
                {
                    return emptyEnumeration();
                }
                return oldLoader.getResources(name);
            }
        })
        {
            @Override
            public URL[] getURLs()
            {
                return urls;
            }
        })
        {
            thread.setContextClassLoader(loader);

            // we disable extension jar scanning and start then
            // we start the container and check we scanned only first module
            final Properties config = new Properties();
            config.setProperty("org.apache.webbeans.scanExtensionJars", "false");
            config.setProperty("org.apache.webbeans.scanExclusionPaths", "/classes,/test-classes," +
                    "/xbean,/ham,/junit-,/junit5-,/debugger,/idea,/openwebbeans,/geronimo");
            final WebBeansContext context = new WebBeansContext(emptyMap(), config);
            final DefaultSingletonService singletonService = DefaultSingletonService.class.cast(
                    WebBeansFinder.getSingletonService());
            singletonService.register(loader, context);
            final ContainerLifecycle lifecycle = context.getService(ContainerLifecycle.class);
            lifecycle.startApplication(null);
            try
            {
                final BeanManager manager = context.getBeanManagerImpl();

                final Set<Bean<?>> foos = manager.getBeans(
                        loader.loadClass("org.apache.openwebbeans.generated.test.Foo"));
                assertEquals(1, foos.size());

                final Set<Bean<?>> bars = manager.getBeans(
                        loader.loadClass("org.apache.openwebbeans.generated.test.Bar"));
                assertTrue(bars.isEmpty());

                final Object myExtension = context.getExtensionLoader()
                        .getExtension(loader.loadClass("org.apache.openwebbeans.generated.test.MyExtension"));
                assertNotNull(myExtension);
            }
            finally
            {
                lifecycle.stopApplication(null);
                singletonService.clear(loader);
            }
        }
        finally
        {
            thread.setContextClassLoader(oldLoader);
        }
    }

    private URL createScannedModule() throws IOException
    {
        final File file = temp.newFile("test-scanned.jar");
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file)))
        {
            createBean(outputStream, "org/apache/openwebbeans/generated/test/Foo.class", null);
            outputStream.putNextEntry(new JarEntry("META-INF/beans.xml"));
            outputStream.closeEntry();
        }
        return file.toURI().toURL();
    }

    private URL createExtensionModule() throws IOException
    {
        final File file = temp.newFile("test-extension.jar");
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file)))
        {
            createBean(outputStream, "org/apache/openwebbeans/generated/test/Bar.class", null);
            createBean(outputStream, "org/apache/openwebbeans/generated/test/MyExtension.class", Extension.class);
            outputStream.putNextEntry(new JarEntry("META-INF/services/" + Extension.class.getName()));
            outputStream.write("org.apache.openwebbeans.generated.test.MyExtension".getBytes(StandardCharsets.UTF_8));
            outputStream.closeEntry();
        }
        return file.toURI().toURL();
    }

    private void createBean(final JarOutputStream outputStream, final String resource, final Class<?> itf)
            throws IOException
    {
        outputStream.putNextEntry(new JarEntry(resource));
        final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
        // make it count for annotated mode
        writer.visitAnnotation(Type.getDescriptor(ApplicationScoped.class), true).visitEnd();
        writer.visit(V1_8, ACC_PUBLIC + ACC_SUPER,
                resource.substring(0, resource.length() - ".class".length()), null,
                Type.getInternalName(Object.class), itf == null ? null : new String[]{ Type.getInternalName(itf) });
        writer.visitSource(resource.replace(".class", ".java"), null);
        final MethodVisitor constructor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        writer.visitEnd();
        outputStream.write(writer.toByteArray());
        outputStream.closeEntry();
    }
}
