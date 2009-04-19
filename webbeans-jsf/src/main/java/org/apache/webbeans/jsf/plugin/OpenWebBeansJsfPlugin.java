package org.apache.webbeans.jsf.plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.faces.component.UIComponent;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.plugins.OpenWebBeansPlugin;
import org.apache.webbeans.util.ClassUtil;

public class OpenWebBeansJsfPlugin implements OpenWebBeansPlugin 
{

    /** {@inheritDoc} */
    public void startUp() throws WebBeansConfigurationException 
    {
        // nothing to do 
    }

    /** {@inheritDoc} */
    public void shutDown() throws WebBeansConfigurationException 
    {
        // nothing to do
    }

    /** {@inheritDoc} */
    public void isSimpleBeanClass( Class<?> clazz ) throws WebBeansConfigurationException 
    {
        if (ClassUtil.isAssignable(UIComponent.class, clazz))
        {
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() 
                                                     + " can not implement JSF UIComponent");
        }
    }

    /** {@inheritDoc} */
    public void checkForValidResources(Type type, Class<?> clazz, String name, Annotation[] annotations)
    {
        // nothing to do
    }

    /** {@inheritDoc} */
    public Object injectResource(Type type, Annotation[] annotations)
    {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isResourceAnnotation(Class<? extends Annotation> annotationClass)
    {
        return false;
    }

}
