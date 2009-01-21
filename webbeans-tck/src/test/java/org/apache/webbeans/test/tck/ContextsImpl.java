package org.apache.webbeans.test.tck;

import org.apache.webbeans.context.AbstractContext;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.type.ContextTypes;
import org.jboss.webbeans.tck.api.Contexts;

public class ContextsImpl implements Contexts<AbstractContext>
{

    public AbstractContext getRequestContext()
    {
        ContextFactory.initRequestContext(null);
        return (AbstractContext) ContextFactory.getStandartContext(ContextTypes.REQUEST);
    }

    public void setActive(AbstractContext context)
    {
        context.setActive(true);
        
    }

    public void setInactive(AbstractContext context)
    {
        context.setActive(false);
    }

    public AbstractContext getDependentContext() {
        return (AbstractContext) ContextFactory.getStandartContext(ContextTypes.DEPENDENT);
    }

    public void destroyContext(AbstractContext context)
    {
        context.destroy();
    }

}
