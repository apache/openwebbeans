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
package org.apache.webbeans.web.tomcat;

import java.util.LinkedList;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.tomcat.InstanceManager;
import org.apache.webbeans.servlet.WebBeansConfigurationListener;


/**
 * Context lifecycle listener.
 */
public class OpenWebBeansContextLifecycleListener implements LifecycleListener {

    /**
     * Start without a bean.xml file.
     */
    protected boolean startWithoutBeanXml = true;

    /**
     * @return the startWithoutBeanXml
     */
    public boolean getStartWithoutBeanXml() {
        return startWithoutBeanXml;
    }

    /**
     * @param startWithoutBeanXml the startWithoutBeanXml to set
     */
    public void setStartWithoutBeanXml(boolean startWithoutBeanXml) {
        this.startWithoutBeanXml = startWithoutBeanXml;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getSource() instanceof Context) {
            Context context = (Context) event.getSource();
            if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
                if (getStartWithoutBeanXml()
                        || context.getResources().getResource("/WEB-INF/beans.xml").exists()
                        || context.getResources().getResource("/WEB-INF/classes/META-INF/beans.xml").exists()) {
                    // Registering ELResolver with JSP container
                    System.setProperty("org.apache.webbeans.application.jsp", "true");
                    // Add Listeners
                    String[] oldListeners = context.findApplicationListeners();
                    LinkedList<String> listeners = new LinkedList<>();
                    listeners.addFirst(WebBeansConfigurationListener.class.getName());
                    for (String listener : oldListeners) {
                        listeners.add(listener);
                        context.removeApplicationListener(listener);
                    }
                    for (String listener : listeners) {
                        context.addApplicationListener(listener);
                    }
                    Pipeline pipeline = context.getPipeline();
                    // Add to the corresponding pipeline to get a notification once configure is done
                    if (pipeline instanceof Lifecycle) {
                        boolean contextLifecycleListenerFound = false;
                        for (LifecycleListener listener : ((Lifecycle) pipeline).findLifecycleListeners()) {
                            if (listener instanceof OpenWebBeansContextLifecycleListener) {
                                contextLifecycleListenerFound = true;
                            }
                        }
                        if (!contextLifecycleListenerFound) {
                            ((Lifecycle) pipeline).addLifecycleListener(this);
                        }
                    }
                    // Add security valve
                    boolean securityValveFound = false;
                    for (Valve valve : pipeline.getValves()) {
                        if (valve instanceof OpenWebBeansSecurityValve) {
                            securityValveFound = true;
                        }
                    }
                    if (!securityValveFound) {
                        pipeline.addValve(new OpenWebBeansSecurityValve());
                    }
                }
            }
        } else if (event.getSource() instanceof Pipeline && event.getType().equals(Lifecycle.START_EVENT)) {
            // This notification occurs once the configuration is fully done, including naming resources setup
            // Otherwise, the instance manager is not ready for creation
            Pipeline pipeline = (Pipeline) event.getSource();
            if (pipeline.getContainer() instanceof Context) {
                Context context = (Context) pipeline.getContainer();
                if (!(context.getInstanceManager() instanceof OpenWebBeansInstanceManager)) {
                    InstanceManager processor = context.getInstanceManager();
                    if (processor == null) {
                        processor = context.createInstanceManager();
                    }
                    InstanceManager custom = new OpenWebBeansInstanceManager(context.getLoader().getClassLoader(), processor);
                    context.setInstanceManager(custom);
                }
            }
        }
    }

}
