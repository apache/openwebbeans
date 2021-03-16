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
package org.apache.webbeans.proxy;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.webbeans.custom.CustomProxyPackageMarker;
import org.apache.webbeans.custom.signed.CustomSignedProxyPackageMarker;
import org.apache.webbeans.exception.ProxyGenerationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;

public class Unsafe
{
    /**
     * contains the instance of sun.misc.Unsafe.
     * We use it for creating the proxy instance without fully
     * initializing the class.
     */
    private final Object unsafe;
    private final Object internalUnsafe;
    private Method unsafeAllocateInstance;
    private final AtomicReference<Method> unsafeDefineClass = new AtomicReference<>();

    // defineClass method on ClassLoader
    private volatile byte defineClassImpl = 0; // 0 = unset, 1 = classloader, 2 = lookup, 3 = unsafe (unlikely on all jvm)
    private volatile Method defineClassMethod = null;

    // java 16
    private volatile Method privateLookup;
    private Method defineClass;
    private MethodHandles.Lookup lookup;

    public Unsafe()
    {
        final Class<?> unsafeClass = getUnsafeClass();

        this.unsafe = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            try
            {
                Field field = unsafeClass.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return field.get(null);
            }
            catch (Exception e)
            {
                WebBeansLoggerFacade.getLogger(Unsafe.class)
                        .info("Cannot get sun.misc.Unsafe - will use newInstance() instead!");
                return null;
            }
        });
        this.internalUnsafe = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            try // j11, unwrap unsafe, it owns defineClass now and no more theUnsafe
            {
                final Field theInternalUnsafe = unsafeClass.getDeclaredField("theInternalUnsafe");
                theInternalUnsafe.setAccessible(true);
                return theInternalUnsafe.get(null);
            }
            catch (final Exception notJ11OrMore)
            {
                return unsafe;
            }
        });

        if (unsafe != null)
        {
            unsafeAllocateInstance = AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
                try
                {
                    Method mtd = unsafe.getClass().getDeclaredMethod("allocateInstance", Class.class);
                    mtd.setAccessible(true);
                    return mtd;
                }
                catch (Exception e)
                {
                    return null; // use newInstance()
                }
            });

            try
            {
                final Class<?> rootLoaderClass = Class.forName("java.lang.ClassLoader");
                rootLoaderClass.getDeclaredMethod(
                    "defineClass", new Class[] { String.class, byte[].class, int.class, int.class })
                    .setAccessible(true);
                rootLoaderClass.getDeclaredMethod(
                    "defineClass", new Class[] {
                            String.class, byte[].class, int.class, int.class, ProtectionDomain.class })
                    .setAccessible(true);
            }
            catch (final Exception e)
            {
                try // some j>8, since we have unsafe let's use it
                {
                    final Class<?> rootLoaderClass = Class.forName("java.lang.ClassLoader");
                    final Method objectFieldOffset = unsafe.getClass().getDeclaredMethod("objectFieldOffset", Field.class);
                    final Method putBoolean = unsafe.getClass().getDeclaredMethod("putBoolean", Object.class, long.class, boolean.class);
                    objectFieldOffset.setAccessible(true);
                    final long accOffset = Long.class.cast(objectFieldOffset.invoke(unsafe, AccessibleObject.class.getDeclaredField("override")));
                    putBoolean.invoke(unsafe, rootLoaderClass.getDeclaredMethod("defineClass",
                            new Class[]{String.class, byte[].class, int.class, int.class}),
                            accOffset, true);
                    putBoolean.invoke(unsafe, rootLoaderClass
                                    .getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class,
                                            int.class, int.class, ProtectionDomain.class}),
                            accOffset, true);
                }
                catch (final Exception ex)
                {
                    // no-op
                }
            }
        }
    }

    /**
     * The 'defineClass' method on the ClassLoader is protected, thus we need to invoke it via reflection.
     * @return the Class which got loaded in the classloader
     */
    public <T> Class<T> defineAndLoadClass(ClassLoader classLoader, String proxyName, byte[] proxyBytes,
                                           Class<?> parent)
            throws ProxyGenerationException
    {
        Class<?> definedClass = null;
        try
        {
            // CHECKSTYLE:OFF
            switch (defineClassImpl) {
                case 0: // unset
                case 1: // classloader
                {
                    if (defineClassMethod == null)
                    {
                        Method defineClassMethodTmp;
                        try
                        {
                            // defineClass is a final method on the abstract base ClassLoader
                            // thus we need to cache it only once
                            defineClassMethodTmp = ClassLoader.class.getDeclaredMethod(
                                    "defineClass", String.class, byte[].class, int.class, int.class);
                            if (!defineClassMethodTmp.isAccessible())
                            {
                                try
                                {
                                    defineClassMethodTmp.setAccessible(true);
                                    defineClassMethod = defineClassMethodTmp;
                                }
                                catch (final RuntimeException re)
                                {
                                    // likely j9 or not accessible via security, let's use unsafe or MethodHandle as fallbacks
                                }
                            }
                        }
                        catch (final NoSuchMethodException e)
                        {
                            // all fine, we just skip over from here
                        }
                    }

                    if (defineClassMethod != null)
                    {
                        try
                        {
                            definedClass = Class.class.cast(defineClassMethod.invoke(
                                    classLoader, proxyName, proxyBytes, 0, proxyBytes.length));
                            defineClassImpl = 1;
                            break;
                        }
                        catch (final Throwable t)
                        {
                            definedClass = handleLinkageError(t, proxyName, classLoader);
                            if (definedClass != null)
                            {
                                defineClassImpl = 1;
                                break;
                            }
                        }
                    }
                }
                case 2: // lookup
                {
                    if (privateLookup == null)
                    {
                        synchronized (this)
                        {
                            if (privateLookup == null)
                            {
                                try
                                {
                                    lookup = MethodHandles.lookup();
                                    defineClass = lookup.getClass().getMethod("defineClass", byte[].class);
                                    privateLookup = MethodHandles.class.getDeclaredMethod(
                                            "privateLookupIn", Class.class, MethodHandles.Lookup.class);
                                }
                                catch (final Exception re)
                                {
                                    // no-op
                                }
                            }
                        }
                    }

                    if (privateLookup != null)
                    {
                        try
                        {
                            final MethodHandles.Lookup lookupInstance = MethodHandles.Lookup.class.cast(
                                    privateLookup.invoke(
                                            null,
                                            proxyName.startsWith("org.apache.webbeans.custom.signed.") ?
                                                    CustomSignedProxyPackageMarker.class :
                                                    proxyName.startsWith("org.apache.webbeans.custom.") ?
                                                            CustomProxyPackageMarker.class : parent,
                                            lookup));
                            definedClass = (Class<T>) defineClass.invoke(lookupInstance, proxyBytes);
                            defineClassImpl = 2;
                            break;
                        }
                        catch (final Exception e)
                        {
                            definedClass = handleLinkageError(e, proxyName, classLoader);
                            if (definedClass != null)
                            {
                                defineClassImpl = 2;
                                break;
                            }
                        }
                    }
                }
                case 3: // unlikely - unsafe
                    try
                    {
                        definedClass = Class.class.cast(unsafeDefineClass().invoke(
                                internalUnsafe, proxyName, proxyBytes, 0, proxyBytes.length, classLoader, null));
                        defineClassImpl = 3;
                    }
                    catch (final Throwable t)
                    {
                        definedClass = handleLinkageError(t, proxyName, classLoader);
                    }
                    break;
                default:
                    throw new IllegalAccessError("Unknown defineclass impl: " + defineClassImpl);
            }

            // CHECKSTYLE:ON
            if (definedClass == null)
            {
                throw new IllegalStateException("Can't define proxy " + proxyName);
            }

            return (Class<T>) Class.forName(definedClass.getName(), true, classLoader);
        }
        catch (final Throwable e)
        {
            return onProxyGenerationError(e, proxyName, classLoader);
        }
    }

    private <T> Class<T> onProxyGenerationError(final Throwable throwable, final String name, final ClassLoader loader)
    {
        final Class<T> clazz = handleLinkageError(throwable, name, loader);
        if (clazz != null)
        {
            return clazz;
        }
        throw new ProxyGenerationException(
                throwable.getMessage() + (isJava16OrMore() ? "\n" +
                "On Java 16 you can set --add-exports java.base/jdk.internal.misc=ALL-UNNAMED on the JVM" : ""),
                throwable.getCause());
    }

    private <T> Class<T> handleLinkageError(final Throwable throwable, final String name, final ClassLoader loader)
    {
        if (LinkageError.class.isInstance(throwable) || LinkageError.class.isInstance(throwable.getCause()))
        {
            try
            {
                return (Class<T>) Class.forName(name.replace('/', '.'), true, loader);
            }
            catch (ClassNotFoundException e)
            {
                // default error handling
            }
        }
        return null;
    }

    private boolean isJava16OrMore()
    {
        final String version = System.getProperty("java.version", "-1");
        final int end = IntStream.of(version.indexOf('-'), version.indexOf('.'))
                .filter(i -> i > 0)
                .min()
                .orElseGet(version::length);
        try
        {
            return Integer.parseInt(version.substring(0, end)) >= 16;
        }
        catch (final NumberFormatException nfe)
        {
            return false;
        }
    }

    private Method unsafeDefineClass()
    {
        Method value = unsafeDefineClass.get();
        if (value == null)
        {
            synchronized (this)
            {
                final Class<?> unsafeClass = internalUnsafe.getClass();
                value = AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
                    try
                    {
                        return unsafeClass.getDeclaredMethod("defineClass",
                                String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class);
                    }
                    catch (final Exception e)
                    {
                        throw new IllegalStateException("Cannot get Unsafe.defineClass or equivalent", e);
                    }
                });
                unsafeDefineClass.compareAndSet(null, value);
            }
        }
        return value;
    }

    public <T> T unsafeNewInstance(Class<T> clazz)
    {
        try
        {
            if (unsafeAllocateInstance != null)
            {
                return (T) unsafeAllocateInstance.invoke(unsafe, clazz);
            }
            else
            {
                try
                {
                    return clazz.getConstructor().newInstance();
                }
                catch (final Exception e)
                {
                    throw new IllegalStateException("Failed to allocateInstance of Proxy class " + clazz.getName(), e);
                }
            }
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("Failed to allocateInstance of Proxy class " + clazz.getName(), e);
        }
        catch (InvocationTargetException e)
        {
            Throwable throwable = e.getTargetException() != null ? e.getTargetException() : e;
            throw new IllegalStateException("Failed to allocateInstance of Proxy class " + clazz.getName(),
                    throwable);
        }
    }

    private Class<?> getUnsafeClass()
    {
        try
        {
            return AccessController.doPrivileged((PrivilegedAction<Class<?>>) () ->
                    Stream.of(Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader())
                            .flatMap(classloader -> Stream.of("sun.misc.Unsafe", "jdk.internal.misc.Unsafe")
                            .flatMap(name ->
                            {
                                try
                                {
                                    return Stream.of(classloader.loadClass(name));
                                }
                                catch (final ClassNotFoundException e)
                                {
                                    return Stream.empty();
                                }
                            }))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Cannot get Unsafe")));
        }
        catch (final Exception e)
        {
            throw new IllegalStateException("Cannot get Unsafe class", e);
        }
    }
}
