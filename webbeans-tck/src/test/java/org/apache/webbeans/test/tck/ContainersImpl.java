package org.apache.webbeans.test.tck;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

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
            //X it is currently not clear when this is called and when it interferes with the functions from BeansImpl!

        }
        
        return MockManager.getInstance();
    }

    public Manager deploy( List<Class<? extends Annotation>> enabledDeploymentTypes, Class<?>... classes ) {
        //X TODO Auto-generated method stub
        return null;
    }

}
