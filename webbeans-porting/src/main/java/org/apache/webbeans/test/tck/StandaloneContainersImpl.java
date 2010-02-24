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
package org.apache.webbeans.test.tck;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.assembler.classic.EjbJarInfo;
import org.apache.openejb.assembler.classic.ProxyFactoryInfo;
import org.apache.openejb.assembler.classic.SecurityServiceInfo;
import org.apache.openejb.assembler.classic.SingletonSessionContainerInfo;
import org.apache.openejb.assembler.classic.StatefulSessionContainerInfo;
import org.apache.openejb.assembler.classic.StatelessSessionContainerInfo;
import org.apache.openejb.assembler.classic.TransactionServiceInfo;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.config.EjbModule;
import org.apache.openejb.core.ivm.naming.InitContextFactory;
import org.apache.openejb.jee.EjbJar;
import org.apache.openejb.jee.SingletonBean;
import org.apache.openejb.jee.StatefulBean;
import org.apache.openejb.jee.StatelessBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.lifecycle.DefaultLifecycle;
import org.apache.webbeans.lifecycle.test.MockHttpSession;
import org.apache.webbeans.lifecycle.test.MockServletContextEvent;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.test.tck.mock.TCKMetaDataDiscoveryImpl;
import org.jboss.testharness.api.DeploymentException;
import org.jboss.testharness.spi.StandaloneContainers;

public class StandaloneContainersImpl implements StandaloneContainers
{
    private DefaultLifecycle lifeCycle = null;
    
    private MockServletContextEvent servletContextEvent;
    
    private MockHttpSession mockHttpSession;
    
    private DeploymentException excpetion;
        
    public void deployInternal(Iterable<Class<?>> classes) throws DeploymentException
    {
        initializeContexts();
        
        TCKMetaDataDiscoveryImpl discovery = (TCKMetaDataDiscoveryImpl)ServiceLoader.getService(ScannerService.class);
        
        this.lifeCycle = new DefaultLifecycle();
        
        try
        {
            Iterator<Class<?>> it = classes.iterator();
            while(it.hasNext())
            {
                discovery.addBeanClass(it.next());
            }
            
            this.lifeCycle.applicationStarted(servletContextEvent);
            
        }catch(Throwable e)
        {
            e.printStackTrace();
            this.excpetion = new DeploymentException("Standalone Container Impl.",e);
            throw this.excpetion;
        }
        
    }


    public boolean deployInternal(Iterable<Class<?>> classes, Iterable<URL> beansXmls)
    {
        try
        {
            initializeContexts();
            
            TCKMetaDataDiscoveryImpl discovery = (TCKMetaDataDiscoveryImpl)ServiceLoader.getService(ScannerService.class);
            
            this.lifeCycle = new DefaultLifecycle();
            
            Iterator<Class<?>> it = classes.iterator();
            while(it.hasNext())
            {
                discovery.addBeanClass(it.next());
            }
            
            Iterator<URL> itUrl = beansXmls.iterator();
            while(itUrl.hasNext())
            {
                discovery.addBeanXml(itUrl.next());
            }
            
            this.lifeCycle.applicationStarted(servletContextEvent);            
        }
        catch(Throwable e)
        {
            e.printStackTrace();
            this.excpetion = new DeploymentException("Standalone Container Impl.",e);
            
            return false;
        }
        
        return true;
    }

    
    private void initializeContexts()
    {
        this.mockHttpSession = new MockHttpSession();
        
        ContextFactory.initRequestContext(null);
        ContextFactory.initSessionContext(mockHttpSession);
        ContextFactory.initConversationContext(null);
        
        this.servletContextEvent = new MockServletContextEvent();        
    }
    
    public void setup()
    {
        
    }
    
    public void cleanup()
    {

    }
    

    public void undeploy()
    {
        this.lifeCycle.applicationEnded(this.servletContextEvent);

        ContextFactory.destroyRequestContext(null);
        ContextFactory.destroySessionContext(this.mockHttpSession);
        ContextFactory.destroyConversationContext();
     }

    public DeploymentException getDeploymentException()
    {
        return this.excpetion;
    }

    protected BeanManager getBeanManager()
    {
        return BeanManagerImpl.getManager();
    }


    @Override
    public void deploy(Collection<Class<?>> classes) throws DeploymentException
    {                
        setUp(classes);
        deployInternal(classes);
    }


    @Override
    public boolean deploy(Collection<Class<?>> classes, Collection<URL> xmls)
    {
        setUp(classes);
        return deployInternal(classes, xmls);
    }
    
    private void setUp(Collection<Class<?>> classes){
        
        try
        {
            ConfigurationFactory config = new ConfigurationFactory();
            Assembler assembler = new Assembler();

            assembler.createProxyFactory(config.configureService(ProxyFactoryInfo.class));
            assembler.createTransactionManager(config.configureService(TransactionServiceInfo.class));
            assembler.createSecurityService(config.configureService(SecurityServiceInfo.class));

            assembler.createContainer(config.configureService(StatelessSessionContainerInfo.class));
            assembler.createContainer(config.configureService(StatefulSessionContainerInfo.class));
            assembler.createContainer(config.configureService(SingletonSessionContainerInfo.class));

            EjbJarInfo ejbJar = config.configureApplication(buildTestApp(classes));

            assembler.createApplication(ejbJar);

            System.setProperty("openejb.validation.output.level", "VERBOSE");
            Properties properties = new Properties(System.getProperties());
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitContextFactory.class.getName());
            new InitialContext(properties);            
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private EjbModule buildTestApp(Collection<Class<?>> classes)
    {
        EjbJar ejbJar = new EjbJar();
        ejbJar.setId(this.getClass().getName());
        
        for(Class<?> clazz : classes)
        {
            if(isSingleton(clazz))
            {
                ejbJar.addEnterpriseBean(new SingletonBean(clazz));
            }
            if(isStateless(clazz))
            {
                ejbJar.addEnterpriseBean(new StatelessBean(clazz));
            }
            
            if(isStatefull(clazz))
            {
                ejbJar.addEnterpriseBean(new StatefulBean(clazz));
            }
        }
        
        
        return new EjbModule(ejbJar);
        
    }

    private boolean isSingleton(Class<?> clazz)
    {
        return clazz.isAnnotationPresent(Singleton.class) ? true : false;
    }
    
    private boolean isStateless(Class<?> clazz)
    {
        return clazz.isAnnotationPresent(Stateless.class) ? true : false;
    }
    
    private boolean isStatefull(Class<?> clazz)
    {
        return clazz.isAnnotationPresent(Stateful.class) ? true : false;
    }    
}
