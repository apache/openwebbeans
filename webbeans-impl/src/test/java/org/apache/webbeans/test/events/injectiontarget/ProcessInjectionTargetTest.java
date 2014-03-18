package org.apache.webbeans.test.events.injectiontarget;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.inject.Inject;

import junit.framework.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

/**
 * Checks that the InjectionTarget in ProcessInjectionTarget
 * is correctly filled.
 */
public class ProcessInjectionTargetTest extends AbstractUnitTest
{
    @Test
    public void testInjectionTargetIsValid() throws Exception
    {
        InjectionTargetExtension injectionTargetExtension = new InjectionTargetExtension();
        addExtension(injectionTargetExtension);
        startContainer(SomeBean.class, SomeOtherBean.class);

        Assert.assertNotNull(injectionTargetExtension.getProcessInjectionTarget());
        InjectionTarget injectionTarget = injectionTargetExtension.getProcessInjectionTarget().getInjectionTarget();
        Assert.assertNotNull(injectionTarget);
        Assert.assertNotNull(injectionTarget.getInjectionPoints());
        Assert.assertEquals(1, injectionTarget.getInjectionPoints().size());
    }

    public static class InjectionTargetExtension implements Extension
    {
        private ProcessInjectionTarget processInjectionTarget;


        public void observePit(@Observes ProcessInjectionTarget<SomeBean> pit)
        {
            this.processInjectionTarget = pit;
        }

        public ProcessInjectionTarget getProcessInjectionTarget()
        {
            return processInjectionTarget;
        }
    }

    @RequestScoped
    public static class SomeBean
    {
        private @Inject SomeOtherBean someOtherBean;

        public SomeOtherBean getSomeOtherBean()
        {
            return someOtherBean;
        }
    }

    @RequestScoped
    public static class SomeOtherBean
    {
        private int meaningOfLife = 42;

        public int getMeaningOfLife()
        {
            return meaningOfLife;
        }

        public void setMeaningOfLife(int meaningOfLife)
        {
            this.meaningOfLife = meaningOfLife;
        }
    }
}
