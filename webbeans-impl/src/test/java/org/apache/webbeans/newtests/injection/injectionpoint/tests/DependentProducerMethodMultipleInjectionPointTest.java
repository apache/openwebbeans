package org.apache.webbeans.newtests.injection.injectionpoint.tests;

import java.util.ArrayList;
import java.util.Collection;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import junit.framework.Assert;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.*;
import org.junit.Test;

public class DependentProducerMethodMultipleInjectionPointTest extends AbstractUnitTest {
    
    @SuppressWarnings("unchecked")
    @Test
    public void testInjectionPointValidity() throws Exception {
        
        Collection<String> beanXmls = new ArrayList<String>();
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(DataTransformer.class);
        beanClasses.add(PropertyEncryptor.class);
        beanClasses.add(MyContainer.class);
        beanClasses.add(PropertyHolder.class);
        beanClasses.add(PropertyHolderFactory.class);
        beanClasses.add(PropertyInjector.class);
        
        startContainer(beanClasses, beanXmls);  

        Bean<PropertyInjector> bean = (Bean<PropertyInjector>) getBeanManager().getBeans(PropertyInjector.class.getName()).iterator().next();
        CreationalContext<PropertyInjector> cc = getBeanManager().createCreationalContext(bean);
        PropertyInjector propertyInjector = (PropertyInjector) getBeanManager().getReference(bean, PropertyInjector.class, cc);

        Assert.assertNotNull(propertyInjector.getDataTransformer());
        
        Assert.assertNotNull(propertyInjector.getAnotherVarName());
        Assert.assertTrue(propertyInjector.getAnotherVarName().equals("Injection is working...Finally"));
        
        Assert.assertNotNull(propertyInjector.getLdapHost());
        Assert.assertTrue(propertyInjector.getLdapHost().equals("Rohit Kelapure LDAP Host"));
        
        Assert.assertNotNull(propertyInjector.getNested().getNestedProperty());
        Assert.assertTrue(propertyInjector.getNested().getNestedProperty().trim().equals("Rohit Kelapure Nested Property"));
        
        shutDownContainer();
        
    }
}
