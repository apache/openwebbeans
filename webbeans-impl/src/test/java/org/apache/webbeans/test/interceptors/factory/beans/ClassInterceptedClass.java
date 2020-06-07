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
package org.apache.webbeans.test.interceptors.factory.beans;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;

import org.apache.webbeans.test.proxy.beans.DummyBean;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.Transactional;

/**
 * A simple class which is class-level intercepted
 */
@Transactional
@RequestScoped
public class ClassInterceptedClass extends SomeBaseClass
{
    private boolean defaultCtInvoked = false;

    private int meaningOfLife;
    private float f;
    private char c;

    public ClassInterceptedClass()
    {
        defaultCtInvoked = true;
    }

    @PostConstruct
    public void postConstruct()
    {
        System.out.println("postConstruct invoked");
    }

    @PreDestroy
    private void preDestroy()
    {
        System.out.println("preDestroy invoked");
    }

    public void init()
    {
        f = 2.4f;
        c = 'c';
        meaningOfLife = 42;
    }

    public int getMeaningOfLife() throws NumberFormatException
    {
        System.out.println("answeringowb-arquillian-parent the question about life, the universe and everything!");
        System.out.println("and being in " + this.getClass());
        return meaningOfLife;
    }


    public void setMeaningOfLife(int meaningOfLife)
    {
        this.meaningOfLife = meaningOfLife;
    }

    public float getFloat()
    {
        return f;
    }

    public ClassInterceptedClass getSelf()
    {
        return this;
    }

    public char getChar()
    {
        return c;
    }

    public String doThaBlowup() throws NumberFormatException
    {
        throw new NumberFormatException("should fit");
    }

    protected int protectedMethod()
    {
        return 21;
    }

    int packagePrivateMethod()
    {
        return 84;
    }


    public DummyBean.SomeInnerClass getValueHolder() throws DummyBean.SomeInnerException
    {
        return new DummyBean.SomeInnerClass("hello");
    }

}
