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
package org.apache.webbeans.test.tck;


import jakarta.el.ArrayELResolver;
import jakarta.el.BeanELResolver;
import jakarta.el.CompositeELResolver;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.ListELResolver;
import jakarta.el.MapELResolver;
import jakarta.el.ResourceBundleELResolver;
import jakarta.el.VariableMapper;
import jakarta.enterprise.inject.spi.BeanManager;

import org.apache.el.ExpressionFactoryImpl;
import org.apache.el.lang.FunctionMapperImpl;
import org.apache.el.lang.VariableMapperImpl;
import org.apache.webbeans.el22.WrappedExpressionFactory;
import org.jboss.cdi.tck.spi.EL;

public class ELImpl implements EL
{
    private static final ExpressionFactory EXPRESSION_FACTORY = new WrappedExpressionFactory(new ExpressionFactoryImpl());
    
    public ELImpl()
    {
    }
    
    public static ELResolver getELResolver()
    {
        CompositeELResolver composite = new CompositeELResolver();
        composite.add(new BeanELResolver());
        composite.add(new ArrayELResolver());
        composite.add(new MapELResolver());
        composite.add(new ListELResolver());
        composite.add(new ResourceBundleELResolver());
        composite.add(new OwbTckElResolver());
        
        return composite;
    }
    
    public static class ELContextImpl extends ELContext
    {        

        @Override
        public ELResolver getELResolver()
        {
            return ELImpl.getELResolver();
        }

        @Override
        public FunctionMapper getFunctionMapper()
        {
            return new FunctionMapperImpl();
        }

        @Override
        public VariableMapper getVariableMapper()
        {
            return new VariableMapperImpl();
        }
        
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluateMethodExpression(BeanManager beanManager, String expression, Class<T> expectedType, Class<?>[] expectedParamTypes, Object[] expectedParams)
    {   
        ELContext context = createELContext(beanManager);        
        Object object = EXPRESSION_FACTORY.createMethodExpression(context, expression, expectedType, expectedParamTypes).invoke(context, expectedParams);
        
        return (T)object;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluateValueExpression(BeanManager beanManager, String expression, Class<T> expectedType)
    {
        ELContext context = createELContext(beanManager);        
        Object object = EXPRESSION_FACTORY.createValueExpression(context, expression, expectedType).getValue(context);
        
        return (T)object;
    }

    public ELContext createELContext(BeanManager beanManager)
    {   
        ELContext context = new ELContextImpl();

        return context;
    }
}
