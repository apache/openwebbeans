package org.apache.webbeans.component;

import java.lang.reflect.Modifier;

import javax.inject.Provider;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.spi.api.ResourceReference;

public class ResourceProvider<T> implements Provider<T> {
    
    private ResourceReference<T, ?> resourceReference = null;
    private WebBeansContext webBeansContext;

    public ResourceProvider(ResourceReference<T, ?> resourceReference, WebBeansContext webBeansContext) {
        this.resourceReference = resourceReference;
        this.webBeansContext = webBeansContext;
    }
    
    @Override
    public T get() {
        try
        {
            ResourceInjectionService resourceService = webBeansContext.getService(ResourceInjectionService.class);
            return resourceService.getResourceReference(resourceReference);
        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
    }
}
