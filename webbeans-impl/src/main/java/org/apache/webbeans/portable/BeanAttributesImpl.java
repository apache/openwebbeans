package org.apache.webbeans.portable;

import javax.enterprise.inject.spi.BeanAttributes;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * BeanAttributes are new in CDI-1.1. They represent all
 * attributes of a Bean which can be changed via Extensions.
 *
 * @since 1.2.0
 */
public class BeanAttributesImpl<T> implements BeanAttributes<T>
{
    private Set<Type> types;
    private Set<Class<? extends Annotation>> stereotypes;
    private String name;
    private Set<Annotation> qualifiers;
    private Class<Annotation> scope;
    private boolean isAlternative;

    public BeanAttributesImpl(Set<Type> types, boolean alternative, String name, Set<Annotation> qualifiers,
                              Class<Annotation> scope, Set<Class<? extends Annotation>> stereotypes)
    {
        this.types = types;
        isAlternative = alternative;
        this.name = name;
        this.qualifiers = qualifiers;
        this.scope = scope;
        this.stereotypes = stereotypes;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Set<Type> getTypes()
    {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return scope;
    }

    /**
     * @deprecated makes no sense in CDI-1.1
     */
    @Override
    public boolean isNullable()
    {
        return true;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return stereotypes;
    }

    @Override
    public boolean isAlternative()
    {
        return isAlternative;
    }
}
