package org.apache.webbeans.test.concepts.vetoes;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.concepts.vetoes.vetoedpackage.subpackage.VetoedBean;
import org.junit.Assert;
import org.junit.Test;

import javax.enterprise.inject.spi.Bean;

public class VetoedPackageTest  extends AbstractUnitTest
{
    @Test
    public void testVetoPackageLevel() throws Exception{
        startContainer(VetoedBean.class);

        Bean<VetoedBean> vetoedBean = getBean(VetoedBean.class);
        Assert.assertNull(vetoedBean);

    }
}
