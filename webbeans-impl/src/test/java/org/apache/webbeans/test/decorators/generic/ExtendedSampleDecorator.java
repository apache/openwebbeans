package org.apache.webbeans.test.decorators.generic;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

@Decorator
public class ExtendedSampleDecorator implements ExtendedGenericInterface
{

    @Inject
    @Any
    @Delegate
    private ExtendedGenericInterface delegate;

    @Override
    public boolean isDecoratorCalled()
    {
        if (delegate.isDecoratorCalled())
        {
            throw new IllegalStateException();
        }
        return true;
    }
}
