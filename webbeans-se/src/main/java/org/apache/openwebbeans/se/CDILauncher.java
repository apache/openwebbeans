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
package org.apache.openwebbeans.se;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

public final class CDILauncher
{
    private CDILauncher()
    {
        // no-op
    }

    public static void main(final String[] args)
    {
        final SeContainerInitializer initializer = SeContainerInitializer.newInstance();
        final Config config = configure(initializer, args);
        try (final SeContainer container = initializer.initialize())
        {
            if (config.main != null)
            {
                executeMain(config, container);
            }
            // else the app can use other ways to execute code like @PreDestroy of a startup bean
        }
    }

    private static void executeMain(final Config config, final SeContainer container)
    {
        final BeanManager manager = container.getBeanManager();
        Set<Bean<?>> beans = manager.getBeans(config.main);
        if (beans == null || beans.isEmpty())
        {
            try
            {
                beans = manager.getBeans(Thread.currentThread().getContextClassLoader().loadClass(config.main));
            }
            catch (final ClassNotFoundException e)
            {
                throw new IllegalArgumentException("No bean '" + config.main + "' found");
            }
        }
        final Bean<?> bean = manager.resolve(beans);
        final CreationalContext<Object> context = manager.createCreationalContext(null);
        final Object reference = manager.getReference(bean, selectProxyType(bean.getTypes()), context);
        try
        {
            if (Runnable.class.isInstance(reference))
            {
                Runnable.class.cast(reference).run();
            }
            else // by reflection
            {
                // try a main method taking parameters
                // then just a main or run method
                Method method = null;
                try
                {
                    method = bean.getBeanClass()
                            .getMethod("main", String[].class);
                }
                catch (final Exception e)
                {
                    try
                    {
                        method = bean.getBeanClass()
                                .getMethod("main");
                    }
                    catch (final Exception e2)
                    {
                        try
                        {
                            method = bean.getBeanClass()
                                    .getMethod("main");
                        }
                        catch (final Exception e3)
                        {
                            throw new IllegalArgumentException(
                                    bean + " does not implements Runnable or has a public main method");
                        }
                    }
                }
                try
                {
                    final Object output = method.invoke(
                            Modifier.isStatic(method.getModifiers()) ? null : reference,
                            method.getParameterCount() == 1 ? new Object[] { config.args } : new Object[0]);
                    if (output != null)
                    {
                        System.out.println(output);
                    }
                }
                catch (final IllegalAccessException e)
                {
                    throw new IllegalStateException(e);
                }
                catch (final InvocationTargetException e)
                {
                    throw new IllegalStateException(e.getTargetException());
                }
            }
        }
        finally
        {
            if (!manager.isNormalScope(bean.getScope()))
            {
                context.release();
            }
        }
    }

    private static Class<?> selectProxyType(final Set<Type> types)
    {
        if (types.contains(Runnable.class))
        {
            return Runnable.class;
        }
        return Object.class;
    }

    private static Config configure(final SeContainerInitializer initializer, final String[] args)
    {
        String main = null;
        final Collection<String> remaining = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++)
        {
            final String current = args[i];
            if (current != null && current.startsWith("--openwebbeans."))
            {
                if (args.length <= i + 1)
                {
                    throw new IllegalArgumentException("Missing argument value for: '" + args[i] + "'");
                }
                if (current.equals("--openwebbeans.main"))
                {
                    if (main != null)
                    {
                        throw new IllegalArgumentException("Ambiguous main '" + main + "' vs '" + args[i + 1] + "'");
                    }
                    main = args[i + 1];
                }
                else
                {
                    initializer.addProperty(current.substring("--".length()), args[i + 1]);
                }
                i++;
            }
            else
            {
                remaining.add(current);
            }
        }
        return new Config(remaining.toArray(new String[0]), main);
    }

    private static class Config
    {
        private final String[] args;
        private final String main;

        private Config(final String[] args, final String main)
        {
            this.args = args;
            this.main = main;
        }
    }
}
