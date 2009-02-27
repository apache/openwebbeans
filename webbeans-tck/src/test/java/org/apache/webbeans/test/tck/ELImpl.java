package org.apache.webbeans.test.tck;

import org.jboss.jsr299.tck.spi.EL;

public class ELImpl implements EL
{

    public <T> T evaluateMethodExpression(String expression, Class<T> expectedType, Class<?>[] expectedParamTypes, Object[] expectedParams)
    {
        
        return null;
    }

    public <T> T evaluateValueExpression(String expression, Class<T> expectedType)
    {
        return null;
    }

}
