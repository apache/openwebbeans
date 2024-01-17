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
package org.apache.webbeans.el22;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

public class WrappedExpressionFactory extends ExpressionFactory
{
    private ExpressionFactory expressionFactory;

    public WrappedExpressionFactory(ExpressionFactory expressionFactory)
    {
        this.expressionFactory = expressionFactory;
    }
    
    @Override
    public <T> T coerceToType(Object obj, Class<T> targetType) throws ELException
    {
        return expressionFactory.coerceToType(obj, targetType);
    }

    @Override
    public MethodExpression createMethodExpression(ELContext context, String expression,
                                                   Class<?> expectedReturnType, Class<?>[] expectedParamTypes) throws ELException, NullPointerException
    {
        return new WrappedMethodExpression(
                expressionFactory.createMethodExpression(context, expression, expectedReturnType, expectedParamTypes));
    }

    @Override
    public ValueExpression createValueExpression(Object instance, Class<?> expectedType)
    {
        ValueExpression wrapped = expressionFactory.createValueExpression(instance, expectedType);
        
        return new WrappedValueExpression(wrapped);
    }

    @Override
    public ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType) throws NullPointerException, ELException
    {   
        ValueExpression wrapped = expressionFactory.createValueExpression(context, expression, expectedType);
                
        return new WrappedValueExpression(wrapped);
    }
}
