package org.apache.webbeans.test.unittests.config;

import javax.annotation.Named;
import javax.context.RequestScoped;

/**
 * Test WebBean for the {@link WebBeansScannerTest}
 */
@RequestScoped
@Named
public class ScannerTestBean
{
    private int myInt = 3;

    public int getMyInt()
    {
        return myInt;
    }

    public void setMyInt(int myInt)
    {
        this.myInt = myInt;
    }

}
