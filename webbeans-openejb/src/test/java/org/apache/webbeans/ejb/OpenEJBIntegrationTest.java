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
package org.apache.webbeans.ejb;

import java.lang.reflect.Field;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.webbeans.ejb.resource.ResourceFactory;
import org.junit.Test;


public class OpenEJBIntegrationTest extends TestCase{

    @Test
    public void testIntegration() throws Exception 
    {
        Properties p = new Properties();
        p.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.openejb.client.LocalInitialContextFactory");
       
        p.put("movieDatabase", "new://Resource?type=DataSource");
        p.put("movieDatabase.JdbcDriver", "org.hsqldb.jdbcDriver");
        p.put("movieDatabase.JdbcUrl", "jdbc:hsqldb:mem:moviedb");

        p.put("movieDatabaseUnmanaged", "new://Resource?type=DataSource");
        p.put("movieDatabaseUnmanaged.JdbcDriver", "org.hsqldb.jdbcDriver");
        p.put("movieDatabaseUnmanaged.JdbcUrl", "jdbc:hsqldb:mem:moviedb");
        p.put("movieDatabaseUnmanaged.JtaManaged", "false");
        
        new InitialContext(p);

        ResourceFactory rf = ResourceFactory.getInstance();
        
        Class<TestBean> c = TestBean.class;
        Field field = c.getField("emf");
        EntityManagerFactory emf = (EntityManagerFactory) rf.getResource(field.getType(), field.getDeclaredAnnotations());
        Assert.assertNotNull( emf );
        
        field = c.getField("em");
        EntityManager em = (EntityManager) rf.getResource(field.getType(), field.getDeclaredAnnotations());
        Assert.assertNotNull( em );
    }

    public class TestBean 
    {
    	public @PersistenceUnit(unitName="TestUnit") EntityManagerFactory emf;
    	public @PersistenceContext(unitName="TestUnit") EntityManager em;
    }
}
