package org.apache.webbeans.test.unittests.config;

import java.util.Map;
import java.util.Set;

import javax.annotation.Named;
import javax.context.RequestScoped;
import javax.servlet.ServletContext;

import junit.framework.Assert;

import org.apache.webbeans.config.WebBeansScanner;
import org.apache.webbeans.test.mock.MockServletContext;
import org.apache.webbeans.test.servlet.TestContext;
import org.apache.webbeans.util.ArrayUtil;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link WebBeansScanner}.
 */
public class WebBeansScannerTest extends TestContext
{

    public WebBeansScannerTest()
    {
        super(WebBeansScannerTest.class.getName());
    }

    @Before
    public void init()
    {
        super.init();
    }

    @Test
    public void testWebBeansScanner() throws Exception
    {
        WebBeansScanner scanner = new WebBeansScanner();

        ServletContext servletContext = new MockServletContext();
        scanner.scan(servletContext);

        // try to re-run the scan
        scanner.scan(servletContext);

        Map<String, Set<String>> classMap = scanner.getANNOTATION_DB().getClassIndex();
        Assert.assertNotNull(classMap);
        Assert.assertFalse(classMap.isEmpty());
        Set<String> testBeanAnnotations = classMap.get(ScannerTestBean.class.getName());

        String[] expectedAnnotations = new String[] { RequestScoped.class.getName(), Named.class.getName() };

        Assert.assertTrue(ArrayUtil.equalsIgnorePosition(testBeanAnnotations.toArray(), expectedAnnotations));
    }
}
