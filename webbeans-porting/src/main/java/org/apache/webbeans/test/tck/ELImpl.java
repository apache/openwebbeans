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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;

import org.apache.webbeans.el.WebBeansELResolver;
import org.jboss.jsr299.tck.spi.EL;

public class ELImpl implements EL
{
    private WebBeansELResolver resolver = new WebBeansELResolver();
    
    public static class ELContextImpl extends ELContext
    {

        @Override
        public ELResolver getELResolver()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public FunctionMapper getFunctionMapper()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public VariableMapper getVariableMapper()
        {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    @SuppressWarnings("unchecked")
    public <T> T evaluateMethodExpression(String expression, Class<T> expectedType, Class<?>[] expectedParamTypes, Object[] expectedParams)
    {
        int firstDot = expression.indexOf('.');
        String property = expression.substring(expression.indexOf("#"),firstDot) + "}"; //object name
        String methodName = expression.substring(firstDot+1,expression.length()-1);
        
        Object object = evaluateValueExpression(property, expectedType);
        
        try
        {
            Method method = object.getClass().getMethod(methodName, expectedParamTypes);
            return (T)method.invoke(object, expectedParams);
        }
        catch (SecurityException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoSuchMethodException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T evaluateValueExpression(String expression, Class<T> expectedType)
    {
        String property = expression.substring(expression.indexOf("#")+2,expression.length()-1);
        
        T object = (T) resolver.getValue(new ELContextImpl() , null, property);
        
        return object;
    }

    @Override
    public ELContext createELContext()
    {
        // TODO Auto-generated method stub
        return null;
    }



}
