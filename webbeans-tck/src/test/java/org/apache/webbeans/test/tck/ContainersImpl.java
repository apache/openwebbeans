package org.apache.webbeans.test.tck;

import java.util.Arrays;

import javax.webbeans.manager.Manager;

import org.apache.log4j.Logger;
import org.apache.webbeans.test.mock.MockManager;
import org.jboss.webbeans.tck.api.Containers;

public class ContainersImpl implements Containers {

    private Logger logger = Logger.getLogger(ContainersImpl.class);

    /** {@inheritDoc} */
    public Manager deploy( Class<?>... classes ) {
        Iterable<Class<?>> webbeanClasses = Arrays.asList(classes);
        
        for (Class<?> webbeanClass : webbeanClasses)
        {
            logger.debug("registering WebBean class " + webbeanClass);
            
            //X TODO create beans!

        }
        
        return MockManager.getInstance();
    }

}
