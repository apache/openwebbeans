package org.apache.webbeans.test.unittests.config;

import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.webbeans.Named;
import javax.webbeans.RequestScoped;

import junit.framework.Assert;

import org.apache.webbeans.config.WebBeansScanner;
import org.apache.webbeans.test.mock.MockServletContext;
import org.apache.webbeans.test.servlet.TestContext;
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

        Assert.assertTrue(equalsIgnorePosition(testBeanAnnotations.toArray(), expectedAnnotations));
    }

    /**
     * TODO please review this function and move to some place where all tests
     * can use it! See also
     * EJBInterceptComponentTest#testMultipleInterceptedComponent and other test
     * functions
     * 
     * Compare two arrays regardless of the position of the elements in the
     * arrays. 
     * The complex handling with temporary flags is necessary due to the
     * possibility of having multiple occurrences of the same element in the
     * arrays. In this case both arrays have to contain the exactly same amount
     * of those elements.
     * 
     * This is only suited for smaller arrays (e.g. count < 100) since the
     * algorithm uses a product of both arrays.
     * 
     * If one likes to use this for larger arrays, we'd have to use hashes.
     * 
     * @param arr1
     * @param arr2
     * @return
     */
    public static boolean equalsIgnorePosition(Object[] arr1, Object[] arr2)
    {
        if (arr1 == null && arr2 == null)
        {
            return true;
        }

        if (arr1 == null || arr2 == null)
        {
            return false;
        }

        if (arr1.length != arr2.length)
        {
            return false;
        }

        boolean[] found1 = new boolean[arr1.length];
        boolean[] found2 = new boolean[arr2.length];

        for (int i1 = 0; i1 < arr1.length; i1++)
        {
            Object o1 = arr1[i1];

            for (int i2 = 0; i2 < arr2.length; i2++)
            {
                Object o2 = arr2[i2];

                // if they are equal and not found already
                if (o1.equals(o2) && found2[i2] == false)
                {
                    // mark the entries in both arrays as found
                    found1[i1] = true;
                    found2[i2] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < found1.length; i++)
        {
            if (!found1[i] || !found2[i])
            {
                return false;
            }
        }
        return true;
    }
}
