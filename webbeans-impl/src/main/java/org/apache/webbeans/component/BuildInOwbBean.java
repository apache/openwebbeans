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
package org.apache.webbeans.component;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;

import javax.enterprise.context.spi.CreationalContext;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;

import org.apache.webbeans.config.BeansDeployer;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;

/**
 * Following 3 options are provided for vendor's build-in beans implementation:
 * 
 * 1. "none", means the build-in bean does not need a proxy wrapper.
 * 2. "default", means the build-in bean needs OWB-provided default proxy wrapper.
 * 3. A class name, which implements MethodHandler. This will allow vendor to 
 *    customize the serialization behavior.
 *    
 * The default values for 4 build-in beans are "default". Following property could
 * be used to change the default behavior:
 * 
 * Property Name:   org.apache.webbeans.component.BuildInOwbBean.property 
 * Sample values:   UserTransation:none;Principal:default;Validation:com.mycompany.ValidationProxyHandler;ValidationFactory:default
 *  
 * @author yingwang
 *
 * @param <T>
 */
public abstract class BuildInOwbBean<T> extends AbstractOwbBean<T>
{

    //Logger instance
    private final WebBeansLogger logger = WebBeansLogger.getLogger(BeansDeployer.class);

    
    private final HashMap<WebBeansType, String> proxyHandlerMap = new HashMap<WebBeansType, String>();

    
    public static final String BUILD_IN_BEAN_PROPERTY = "org.apache.webbeans.component.BuildInOwbBean.property";
    
    /**
     * none means the build-in bean instance does not need proxy wrapper. This is used
     * for the build-in beans from vendors that are already serializable. 
     */
    private static final String PROXY_HANDLER_VALUE_NONE="none";

    /**
     * default means the build-bin bean instance need a default proxy wrapper. And the
     * default proxy wrapper will get new instance from build in bean providers when
     * it is deserialized. 
     */
    private static final String PROXY_HANDLER_VALUE_DEFAULT="default";

    /**
     * Initialize build-in config.
     */
    private boolean initialized;

    /**
     * The handler class name.
     */
    protected String handlerClassName;
    
    /**
     * The handler class.
     */
    protected Class handlerClass;
    
    
    protected Constructor handlerContructor;
    
    /**
     * Parse the custom property.
     * 
     * @return true
     */
    protected  boolean initBuildInBeanConfig(WebBeansContext webBeansContext)
    {
        String s = webBeansContext.getOpenWebBeansConfiguration().getProperty(BUILD_IN_BEAN_PROPERTY);
        proxyHandlerMap.put(WebBeansType.USERTRANSACTION, PROXY_HANDLER_VALUE_DEFAULT);
        proxyHandlerMap.put(WebBeansType.PRINCIPAL, PROXY_HANDLER_VALUE_DEFAULT);
        proxyHandlerMap.put(WebBeansType.VALIDATION, PROXY_HANDLER_VALUE_DEFAULT);
        proxyHandlerMap.put(WebBeansType.VALIDATIONFACT, PROXY_HANDLER_VALUE_DEFAULT);
        if (s != null && !s.equalsIgnoreCase("default"))
        {
            int i;
            String name;
            String value;
            String mapStrings[] = s.split(";");
            for(i=0; i<mapStrings.length; i++)
            {
                name = null;
                value = null;
                String pair[] = mapStrings[i].trim().split(":");
                if (pair.length == 2) 
                {
                    name = pair[0].trim();
                    value = pair[1].trim();
                }
                if (name == null || value == null || value.equalsIgnoreCase(PROXY_HANDLER_VALUE_DEFAULT)) 
                {
                    continue;
                }
                if (name.contains("UserTransaction"))
                {
                    proxyHandlerMap.put(WebBeansType.USERTRANSACTION, value);
                } 
                else if (name.contains("Principal"))
                {
                    proxyHandlerMap.put(WebBeansType.PRINCIPAL, value);
                }
                else if (name.contains("Validation"))
                {
                    proxyHandlerMap.put(WebBeansType.VALIDATION, value);
                } 
                else if (name.contains("ValidationFactory"))
                {
                    proxyHandlerMap.put(WebBeansType.VALIDATIONFACT, value);
                }
            } 
        } 
        return true;
    }

    protected BuildInOwbBean(WebBeansType webBeanType)
    {
        this(webBeanType, null);
        initBuildInBeanConfig(getWebBeansContext());
    }

    @SuppressWarnings("unchecked")
    protected BuildInOwbBean(WebBeansType webBeansType, Class<T> returnType)
    {
        super(webBeansType, returnType, WebBeansContext.getInstance());
        initBuildInBeanConfig(getWebBeansContext());
        this.handlerClassName = proxyHandlerMap.get(this.getWebBeansType());
        if (handlerClassName.equalsIgnoreCase(PROXY_HANDLER_VALUE_NONE) ||
                handlerClassName.equalsIgnoreCase(PROXY_HANDLER_VALUE_DEFAULT)) 
        {
            return;
        }

        // initialize the custom proxy handler class and its constructor.
        AccessController.doPrivileged(new PrivilegedAction<T>() 
        {
            private BuildInOwbBean<T> buildinBean;
            
            public T run()
            {
                try 
                {
                    buildinBean.handlerClass = Class.forName(name);
                    buildinBean.handlerContructor = buildinBean.handlerClass.getConstructor(BuildInOwbBean.class, Object.class);
                    return null;
                } 
                catch (ClassNotFoundException e) 
                {
                    logger.error(e);
                } 
                catch (SecurityException e) 
                {
                    logger.error(e);
                } 
                catch (NoSuchMethodException e) 
                {
                    logger.error(e);
                }
                buildinBean.handlerClass = null;
                buildinBean.handlerContructor = null;
                return null;
            }
            
            protected PrivilegedAction<T> setBuildInBean(BuildInOwbBean<T> b) 
            {
                this.buildinBean = b;
                return this;
            }
            
        }.setBuildInBean(this));
    }
        
    /**
     * Create a dependent proxy wrapper around the actual build in bean instance.
     * 
     * @param actualInstance
     * @param creationalContext
     * @return
     */
    protected T createProxyWrapper(T actualInstance, CreationalContext<T> creationalContext)
    {
        if (handlerClassName.equals(PROXY_HANDLER_VALUE_NONE))
        {
            return actualInstance;
        }
        
        T proxy = (T)JavassistProxyFactory.getInstance().createBuildInBeanProxy(this);
        if (handlerClassName.equals(PROXY_HANDLER_VALUE_DEFAULT)) 
        {
            ((ProxyObject)proxy).setHandler(new BuildInBeanMethodHandler(this, actualInstance));
            return proxy;
        } 
        else if (handlerContructor != null)
        {
            try 
            {
                ((ProxyObject)proxy).setHandler( (MethodHandler)(handlerContructor.newInstance(this, actualInstance)));
                return proxy;
            } 
            catch (Exception e) 
            {
                logger.error(e);
            }
        }
        return null;
    }
    

    protected abstract T createActualInstance(CreationalContext<T> creationalContext);
    
    
    /**
     * The default build in bean handler. 
     * 
     * @author yingwang
     *
     * @param <T>
     */
    public static class BuildInBeanMethodHandler<T> implements MethodHandler, Serializable
    {

        /**
         * 
         */
        private static final long serialVersionUID = -2442900183095535369L;

        private BuildInOwbBean<T> bean;
        
        private T actualObject = null;
        
        //DO NOT REMOVE, used by failover and passivation.
        public BuildInBeanMethodHandler()
        {
        }

        public BuildInBeanMethodHandler(BuildInOwbBean<T> bean, T actualObject) 
        {
            this.bean = bean;
            this.actualObject = actualObject;
        }    
        
        @Override
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
        {
            if(proceed != null)       
            {
                return proceed.invoke(actualObject,args);
            } 
            else 
            {
                //interface method.
                return thisMethod.invoke(actualObject,args);
            }            
        }

        private  void writeObject(ObjectOutputStream s) throws IOException
        {
            s.writeLong(serialVersionUID);
            s.writeObject(bean.getId());
        }    
        
        private  void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
        {
            if(s.readLong() == serialVersionUID) 
            {
                String id = (String)s.readObject();
                WebBeansContext webBeansContext = WebBeansContext.currentInstance();
                bean = (BuildInOwbBean<T>)webBeansContext.getBeanManagerImpl().getPassivationCapableBean(id);
                // create new real instance after deserialized.
                actualObject = bean.createActualInstance(null);
            } 
            else 
            {
                throw new IOException("Serial version uid does not match.");
            }
        }    

    }
}
