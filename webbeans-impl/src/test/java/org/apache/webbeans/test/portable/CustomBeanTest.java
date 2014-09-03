/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.portable;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Test that custom Bean<T> impls work
 */
public class CustomBeanTest
{


    public static class CustomBeanExtension implements Extension
    {
        public void addBean(@Observes AfterBeanDiscovery abd, BeanManager beanManager)
        {

        }
    }

    public static class MyBean implements Bean<DataSource>
    {
        @Override
        public DataSource create(CreationalContext<DataSource> context)
        {
            return new DataSource()
            {
                @Override
                public Connection getConnection() throws SQLException
                {
                    return null;
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException
                {
                    return null;
                }

                @Override
                public PrintWriter getLogWriter() throws SQLException
                {
                    return null;
                }

                @Override
                public void setLogWriter(PrintWriter out) throws SQLException
                {

                }

                @Override
                public void setLoginTimeout(int seconds) throws SQLException
                {

                }

                @Override
                public int getLoginTimeout() throws SQLException
                {
                    return 0;
                }

                @Override
                public Logger getParentLogger() throws SQLFeatureNotSupportedException
                {
                    return null;
                }

                @Override
                public <T> T unwrap(Class<T> iface) throws SQLException
                {
                    return null;
                }

                @Override
                public boolean isWrapperFor(Class<?> iface) throws SQLException
                {
                    return false;
                }
            };
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints()
        {
            return null;
        }

        @Override
        public Class<?> getBeanClass()
        {
            return null;
        }

        @Override
        public boolean isNullable()
        {
            return false;
        }

        @Override
        public Set<Type> getTypes()
        {
            return null;
        }

        @Override
        public Set<Annotation> getQualifiers()
        {
            return null;
        }

        @Override
        public Class<? extends Annotation> getScope()
        {
            return null;
        }

        @Override
        public String getName()
        {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes()
        {
            return null;
        }

        @Override
        public boolean isAlternative()
        {
            return false;
        }

        @Override
        public void destroy(DataSource instance, CreationalContext<DataSource> context)
        {

        }
    }
}
