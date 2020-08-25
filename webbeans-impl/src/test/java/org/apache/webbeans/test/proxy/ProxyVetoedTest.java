package org.apache.webbeans.test.proxy;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.proxy.beans.ApplicationBean;
import org.junit.Assert;
import org.junit.Test;

import javax.enterprise.inject.Vetoed;
import java.util.ArrayList;
import java.util.Collection;

public class ProxyVetoedTest extends AbstractUnitTest
{
    @Test
    public void testVetoedAnnotation()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(ApplicationBean.class);
        startContainer(beanClasses, null);
        ApplicationBean applicationBean = getInstance(ApplicationBean.class);
        Assert.assertTrue(applicationBean.getClass().getName().contains("Proxy"));
        Assert.assertTrue(applicationBean.getClass().isAnnotationPresent(Vetoed.class));
    }
}
