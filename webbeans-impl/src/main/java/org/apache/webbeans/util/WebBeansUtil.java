/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.util;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.annotation.NewLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.BeanManagerBean;
import org.apache.webbeans.component.BeanMetadataBean;
import org.apache.webbeans.component.ConversationBean;
import org.apache.webbeans.component.DecoratorMetadataBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.EventBean;
import org.apache.webbeans.component.EventMetadataBean;
import org.apache.webbeans.component.ExtensionBean;
import org.apache.webbeans.component.InjectionPointBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.InstanceBean;
import org.apache.webbeans.component.InterceptedOrDecoratedBeanMetadataBean;
import org.apache.webbeans.component.InterceptorMetadataBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.NewManagedBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.PrincipalBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.ResourceBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.creation.BeanAttributesBuilder;
import org.apache.webbeans.component.creation.ExtensionBeanBuilder;
import org.apache.webbeans.component.creation.ManagedBeanBuilder;
import org.apache.webbeans.component.creation.ObserverMethodsBuilder;
import org.apache.webbeans.component.creation.ProducerFieldBeansBuilder;
import org.apache.webbeans.component.creation.ProducerMethodBeansBuilder;
import org.apache.webbeans.config.OwbParametrizedTypeImpl;
import org.apache.webbeans.config.OwbWildcardTypeImpl;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.AnnotatedTypeWrapper;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.ExternalScope;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.exception.WebBeansConfigurationException;


import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.AbstractProducer;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.portable.events.ProcessBeanAttributesImpl;
import org.apache.webbeans.portable.events.discovery.ErrorStack;
import org.apache.webbeans.portable.events.generics.GProcessAnnotatedType;
import org.apache.webbeans.portable.events.generics.GProcessBean;
import org.apache.webbeans.portable.events.generics.GProcessBeanAttributes;
import org.apache.webbeans.portable.events.generics.GProcessInjectionPoint;
import org.apache.webbeans.portable.events.generics.GProcessInjectionTarget;
import org.apache.webbeans.portable.events.generics.GProcessManagedBean;
import org.apache.webbeans.portable.events.generics.GProcessObservableMethod;
import org.apache.webbeans.portable.events.generics.GProcessProducer;
import org.apache.webbeans.portable.events.generics.GProcessProducerField;
import org.apache.webbeans.portable.events.generics.GProcessProducerMethod;
import org.apache.webbeans.portable.events.generics.GProcessSessionBean;
import org.apache.webbeans.portable.events.generics.GProcessSyntheticAnnotatedType;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.spi.plugins.OpenWebBeansPlugin;

import javax.decorator.Decorator;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.inject.spi.ProcessSyntheticAnnotatedType;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Named;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Contains some utility methods used in the all project.
 */
@SuppressWarnings("unchecked")
public final class WebBeansUtil
{
    private static final Logger logger = WebBeansLoggerFacade.getLogger(WebBeansUtil.class);

    private final WebBeansContext webBeansContext;

    // cache to skip some validations
    private final ConcurrentMap<Type, Boolean> noTypeVariables = new ConcurrentHashMap<Type, Boolean>();
    private final ConcurrentMap<EventCacheKey, Boolean> validEventType = new ConcurrentHashMap<EventCacheKey, Boolean>();
    private final ConcurrentMap<Type, Boolean> notContainerEvents = new ConcurrentHashMap<Type, Boolean>();

    public WebBeansUtil(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * Gets current classloader with current thread.
     *
     * @return Current class loader instance
     */
    public static ClassLoader getCurrentClassLoader()
    {
        ClassLoader loader =  Thread.currentThread().getContextClassLoader();

        if (loader == null)
        {
            loader = WebBeansUtil.class.getClassLoader();
        }

        return loader;
    }

    /**
     * Checks the generic type requirements.
     */
    public static void checkGenericType(Class<?> clazz, Class<? extends Annotation> scope)
    {
        Asserts.assertNotNull(clazz);

        if (clazz.getTypeParameters().length > 0)
        {
            if(!scope.equals(Dependent.class))
            {
                throw new WebBeansConfigurationException("Generic type may only defined with scope @Dependent " +
                        "for ManagedBean class : " + clazz.getName());
            }
        }
    }


    /**
     * Check producer method/field bean return type.
     * @param bean producer bean instance
     * @param member related member instance
     */
    public static void checkProducerGenericType(Bean<?> bean,Member member)
    {
        Asserts.assertNotNull(bean,"Bean is null");

        Type type;

        if(bean instanceof ProducerMethodBean)
        {
            type = ((ProducerMethodBean<?>)bean).getCreatorMethod().getGenericReturnType();
        }
        else if(bean instanceof ProducerFieldBean)
        {
            type = ((ProducerFieldBean<?>)bean).getCreatorField().getGenericType();
        }
        else
        {
            throw new IllegalArgumentException("Bean must be Producer Field or Method Bean instance : " + bean);
        }

        String messageTemplate = "Producer Field/Method Bean with name : %s" + 
                         " in bean class : %s"; 

        String memberName = member.getName();
        String declaringClassName = member.getDeclaringClass().getName();
        if(checkGenericForProducers(type, messageTemplate, memberName, declaringClassName))
        {
            if(!bean.getScope().equals(Dependent.class))
            {
                String message = format(messageTemplate, memberName, declaringClassName);
                throw new WebBeansConfigurationException(message + " scope must bee @Dependent");
            }
        }
    }

    /**
     * Check generic types for producer method and fields.
     * @param type generic return type
     * @param messageTemplate error message
     * @return true if parametrized type argument is TypeVariable
     */
    //Helper method
    private static boolean checkGenericForProducers(Type type, String messageTemplate, Object... errorMessageArgs)
    {
        boolean result = false;

        if(type instanceof TypeVariable)
        {
            String message = format(messageTemplate, errorMessageArgs);
            throw new WebBeansConfigurationException(message + " return type can not be type variable");
        }

        if(ClassUtil.isParametrizedType(type))
        {
            Type[] actualTypes = ClassUtil.getActualTypeArguments(type);

            if(actualTypes.length == 0)
            {
                String message = format(messageTemplate, errorMessageArgs);
                throw new WebBeansConfigurationException(message +
                        " return type must define actual type arguments or type variable");
            }

            for(Type actualType : actualTypes)
            {
                if(ClassUtil.isWildCardType(actualType))
                {
                    String message = format(messageTemplate, errorMessageArgs);
                    throw new WebBeansConfigurationException(message +
                            " return type can not define wildcard actual type argument");
                }

                if(ClassUtil.isTypeVariable(actualType))
                {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Returns true if this class can be candidate for simple web bean, false otherwise.
     *
     * @param clazz implementation class
     * @throws WebBeansConfigurationException if any configuration exception occurs
     */
    public void checkManagedBean(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz, "Class is null");

        int modifier = clazz.getModifiers();

        if (!Modifier.isStatic(modifier) && ClassUtil.isInnerClazz(clazz))
        {
            throw new WebBeansConfigurationException("Skipping CDI bean detection for non-static inner class: "
                                                     + clazz.getName() );
        }

        if(Extension.class.isAssignableFrom(clazz))
        {
            throw new WebBeansConfigurationException("Skipping CDI bean detection for CDI Extension class"
                                                     + clazz.getName());
        }

        // and finally call all checks which are defined in plugins like JSF, JPA, etc
        List<OpenWebBeansPlugin> plugins = webBeansContext.getPluginLoader().getPlugins();
        for (OpenWebBeansPlugin plugin : plugins)
        {
            try
            {
                plugin.isManagedBean(clazz);
            }
            catch (Exception e)
            {
                PluginLoader.throwsException(e);
            }
        }
    }

    public void checkManagedBeanCondition(Class<?> clazz) throws WebBeansConfigurationException
    {
        if (AnnotationUtil.hasClassAnnotation(clazz, Decorator.class) && AnnotationUtil.hasClassAnnotation(clazz, javax.interceptor.Interceptor.class))
        {
            throw new WebBeansConfigurationException("ManagedBean implementation class : " + clazz.getName()
                                                     + " may not annotated with both @Interceptor and @Decorator annotation");
        }

        if (!AnnotationUtil.hasClassAnnotation(clazz, Decorator.class) && !AnnotationUtil.hasClassAnnotation(clazz, javax.interceptor.Interceptor.class))
        {
            webBeansContext.getInterceptorUtil().checkSimpleWebBeansInterceptorConditions(clazz);
        }
    }

    /**
     * Returns true if given class supports injections,
     * false otherwise.
     * <p>
     * Each plugin is asked with given class that supports
     * injections or not.
     * </p>
     * @param clazz scanned class
     * @return  true if given class supports injections
     */
    public boolean supportsJavaEeComponentInjections(Class<?> clazz)
    {
        if (clazz.isInterface() || clazz.isAnnotation() || clazz.isEnum())
        {
            // interfaces, annotations and enums are no subject of injection
            return false;
        }

        // and finally call all checks which are defined in plugins like JSF, JPA, etc
        List<OpenWebBeansPlugin> plugins = webBeansContext.getPluginLoader().getPlugins();
        for (OpenWebBeansPlugin plugin : plugins)
        {
            //Ejb plugin handles its own events
            //Also EJb beans supports injections
            if(!(plugin instanceof OpenWebBeansEjbPlugin))
            {
                if(plugin.supportsJavaEeComponentInjections(clazz))
                {
                    return true;
                }
            }
            else
            {
                return ((OpenWebBeansEjbPlugin) plugin).isSessionBean(clazz);
            }
        }

        return false;
    }


    /**
     * Check that simple web beans class has compatible constructor.
     * @param annotatedType web beans annotatedType
     * @throws WebBeansConfigurationException if the web beans has incompatible
     *             constructor
     */
    public boolean isConstructorOk(AnnotatedType<?> annotatedType) throws WebBeansConfigurationException
    {
        Class<?> clazz = annotatedType.getJavaClass();

        if (getNoArgConstructor(clazz) != null)
        {
            // if we have a default ct, then all is fine in any case
            return true;
        }

        Set<? extends AnnotatedConstructor<?>> constructors = annotatedType.getConstructors();
        for (AnnotatedConstructor<?> constructor : constructors)
        {
            if (constructor.getAnnotation(Inject.class) != null)
            {
                return true;
            }
        }
        return false;
    }

    private boolean isNormalScoped(final AnnotatedType<?> type)
    {
        final Set<Annotation> annotations = type.getAnnotations();
        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();
        if (annotations != null)
        {
            for (final Annotation a : annotations)
            {
                if (AnnotationUtil.hasMetaAnnotation(a.annotationType().getAnnotations(), NormalScope.class)
                    || AnnotationUtil.hasAnnotation(a.annotationType().getAnnotations(), NormalScope.class))
                {
                    return true;
                }
                for (ExternalScope externalScope : beanManager.getAdditionalScopes())
                {
                    if (externalScope.isNormal() && externalScope.getScope().equals(a))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    public <T> Bean<T> createNewComponent(Class<T> type)
    {
        Asserts.nullCheckForClass(type);

        final OpenWebBeansEjbPlugin ejbPlugin = webBeansContext.getPluginLoader().getEjbPlugin();
        if (ejbPlugin != null && ejbPlugin.isNewSessionBean(type))
        {
            return ejbPlugin.defineNewSessionBean(type);
        }

        AnnotatedType<T> annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(type);
        BeanAttributesImpl<T> defaultBeanAttributes = BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(annotatedType).build();
        BeanAttributesImpl<T> newBeanAttributes = new BeanAttributesImpl<T>(defaultBeanAttributes.getTypes(), Collections.<Annotation>singleton(new NewLiteral(type)));
        // TODO replace this by InjectionPointBuilder
        ManagedBeanBuilder<T, ManagedBean<T>> beanBuilder = new ManagedBeanBuilder<T, ManagedBean<T>>(webBeansContext, annotatedType, newBeanAttributes);
        NewManagedBean<T> newBean
            = new NewManagedBean<T>(webBeansContext, WebBeansType.MANAGED, annotatedType, newBeanAttributes, type, beanBuilder.getBean().getInjectionPoints());
        return newBean;
    }
    
    /**
     * New WebBeans component class.
     *
     * @return the new component
     */
    public <T> NewManagedBean<T> createNewComponent(OwbBean<T> bean, Class<T> type)
    {
        Asserts.assertNotNull(bean, "bean may not be null");
        if (!EnumSet.of(WebBeansType.MANAGED, WebBeansType.ENTERPRISE, WebBeansType.PRODUCERMETHOD, WebBeansType.PRODUCERFIELD).contains(bean.getWebBeansType()))
        {
            throw new WebBeansConfigurationException("@New annotation on type : " + bean.getBeanClass()
                    + " must defined as a simple or an enterprise web bean");
        }

        AnnotatedType<T> annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(type);
        BeanAttributesImpl<T> newBeanAttributes = new BeanAttributesImpl<T>(bean.getTypes(), Collections.<Annotation>singleton(new NewLiteral(type)));
        NewManagedBean<T> newBean = new NewManagedBean<T>(bean.getWebBeansContext(), bean.getWebBeansType(), annotatedType, newBeanAttributes, type, bean.getInjectionPoints());
        //TODO XXX set producer
        return newBean;
    }

    /**
     * Creates a new extension bean.
     *
     * @param <T> extension service class
     * @param clazz impl. class
     * @return a new extension service bean
     */
    public <T> ExtensionBean<T> createExtensionComponent(Class<T> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        ExtensionBeanBuilder<T> extensionBeanBuilder = new ExtensionBeanBuilder<T>(webBeansContext, clazz);
        ExtensionBean<T> bean = extensionBeanBuilder.getBean();
        new ObserverMethodsBuilder<T, InjectionTargetBean<T>>(webBeansContext, extensionBeanBuilder.getAnnotatedType()).defineObserverMethods(bean);
        return bean;
    }


    /**
     * Creates a new manager bean instance.
     * @return new manager bean instance
     */
    public BeanManagerBean getManagerBean()
    {
        return new BeanManagerBean(webBeansContext);
    }

    /**
     * Creates a new instance bean.
     * @return new instance bean
     */
    public <T> InstanceBean<T> getInstanceBean()
    {
        return new InstanceBean<T>(webBeansContext);
    }

    /**
     * Creates a new event bean.
     * @return new event bean
     */
    public <T> EventBean<T> getEventBean()
    {
        return new EventBean<T>(webBeansContext);
    }

    /**
     * Creates a new event bean.
     * @return new event bean
     */
    public EventMetadataBean getEventMetadataBean()
    {
        return new EventMetadataBean(webBeansContext);
    }

    /**
     * Creates a new bean metadata bean.
     * @return new  bean
     */
    public <T> BeanMetadataBean<T> getBeanMetadataBean()
    {
        return new BeanMetadataBean<T>(webBeansContext);
    }

    /**
     * Creates a new bean metadata bean.
     * @return new  bean
     */
    public PrincipalBean getPrincipalBean()
    {
        return new PrincipalBean(webBeansContext);
    }

    /**
     * Creates a new interceptor metadata bean.
     * @return new bean
     */
    public <T> InterceptorMetadataBean<T> getInterceptorMetadataBean()
    {
        return new InterceptorMetadataBean<T>(webBeansContext);
    }

    /**
     * Creates a new decorator metadata bean.
     * @return new bean
     */
    public <T> DecoratorMetadataBean<T> getDecoratorMetadataBean()
    {
        return new DecoratorMetadataBean<T>(webBeansContext);
    }

    /**
     * Creates a new metadata bean.
     * @return new bean
     */
    public <T> InterceptedOrDecoratedBeanMetadataBean<T> getInterceptedOrDecoratedBeanMetadataBean()
    {
        return new InterceptedOrDecoratedBeanMetadataBean<T>(webBeansContext);
    }

    /**
     * Returns new conversation bean instance.
     * The name is explicitly specified in 6.7.2 and is not the normal default name.
     * @return new conversation bean
     */
    public ConversationBean getConversationBean()
    {
        ConversationBean conversationComp = new ConversationBean(webBeansContext);

        return conversationComp;
    }

    /**
     * Returns a new injected point bean instance.
     * @return new injected point bean
     */
    public InjectionPointBean getInjectionPointBean()
    {
        return new InjectionPointBean(webBeansContext);
    }

    public static String getManagedBeanDefaultName(String clazzName)
    {
        Asserts.assertNotNull(clazzName);

        if(clazzName.length() > 0)
        {
            StringBuilder name = new StringBuilder(clazzName);
            name.setCharAt(0, Character.toLowerCase(name.charAt(0)));

            return name.toString();
        }

        return clazzName;
    }

    public static String getProducerDefaultName(String methodName)
    {
        StringBuilder buffer = new StringBuilder(methodName);

        if (buffer.length() > 3 &&  (buffer.substring(0, 3).equals("get") || buffer.substring(0, 3).equals("set")))
        {

            if(Character.isUpperCase(buffer.charAt(3)))
            {
                buffer.setCharAt(3, Character.toLowerCase(buffer.charAt(3)));
            }

            return buffer.substring(3);
        }
        else if ((buffer.length() > 2 &&  buffer.substring(0, 2).equals("is")))
        {
            if(Character.isUpperCase(buffer.charAt(2)))
            {
                buffer.setCharAt(2, Character.toLowerCase(buffer.charAt(2)));
            }

            return buffer.substring(2);
        }

        else
        {
            buffer.setCharAt(0, Character.toLowerCase(buffer.charAt(0)));
            return buffer.toString();
        }
    }


    /**
     * Configure a list of producer method beans, which override the same method
     * and the bean classes are directly extended each other.
     *
     * @param sortedProducerBeans
     */
    protected void configSpecializedProducerMethodBeans(List<ProducerMethodBean> sortedProducerBeans)
    {
        if (sortedProducerBeans.isEmpty())
        {
            return;
        }

        AlternativesManager altManager = webBeansContext.getAlternativesManager();
        Method superMethod = sortedProducerBeans.get(0).getCreatorMethod();

        for(int i=1; i<sortedProducerBeans.size(); i++)
        {
            ProducerMethodBean bean = sortedProducerBeans.get(i);
            ProducerMethodBean superBean = sortedProducerBeans.get(i - 1);

            // inherit name is super class has name
            boolean isSuperHasName = isSuperMethodNamed(bean, bean.getCreatorMethod(), superMethod);

            // disable super bean if needed
            if (bean.getCreatorMethod().getAnnotation(Alternative.class) == null)
            {
                //disable superbean if the current bean is not an alternative
                superBean.setEnabled(false);
            }
            else if(altManager.isAlternative(bean))
            {
                //disable superbean if the current bean is an enabled alternative
                superBean.setEnabled(false);
            }

            //if no name defined, set superMethod to this bean since this
            //bean's method might have name defined.
            if (!isSuperHasName)
            {
                superMethod = bean.getCreatorMethod();
            }
        }
    }

    /**
     * Configure direct/indirect specialized producer method beans.
     * @deprecated remove
     */
    public void configureProducerMethodSpecializations()
    {
        Method method;
        ProducerMethodBean pbean;
        ProducerMethodBean pLeft;
        ProducerMethodBean pRight;

        // collect all producer method beans
        Set<Bean<?>> beans = webBeansContext.getBeanManagerImpl().getBeans();
        List<ProducerMethodBean> producerBeans = new ArrayList<ProducerMethodBean>();
        Set<Class> classesDisabledDueToSpecialization = new HashSet<Class>();

        for(Bean b : beans)
        {
            if (b instanceof ProducerMethodBean)
            {
                producerBeans.add((ProducerMethodBean)b);

                if (((ProducerMethodBean) b).isSpecializedBean())
                {
                    Class superClass = b.getBeanClass().getSuperclass();
                    if (classesDisabledDueToSpecialization.contains(superClass))
                    {
                        throw new DeploymentException("Multiple specializations for the same producer method got detected for type"
                                + b.toString());
                    }
                    classesDisabledDueToSpecialization.add(superClass);
                }
            }
        }
        classesDisabledDueToSpecialization.clear(); // not needed any longer

        // create sorted bean helper.
        SortedListHelper<ProducerMethodBean> producerBeanListHelper = new
                SortedListHelper<ProducerMethodBean>(new ArrayList<ProducerMethodBean>(),
                new Comparator<ProducerMethodBean>()
                {
                    @Override
                    public int compare(ProducerMethodBean e1, ProducerMethodBean e2)
                    {
                        if (e1.getBeanClass().isAssignableFrom(e2.getBeanClass()))
                        {
                            return -1;
                        }
                        else if (e1.equals(e2))
                        {
                            return 0;
                        }
                        return 1;
                    }
                });

        Set<Method> disabledProducerMethods = new HashSet<Method>();

        while(true)
        {
            pbean = null;
            method = null;
            producerBeanListHelper.clear();

            //locate the first specialized bean
            for(ProducerMethodBean pb : producerBeans)
            {
                if (pb.isSpecializedBean())
                {
                    pbean = pb;
                    method = pb.getCreatorMethod();
                    producerBeanListHelper.add(pb);
                    break;
                }
            }
            if (pbean == null)
            {
                break;
            }

            pRight = pbean;
            pLeft = pRight;
            boolean pLeftContinue = true;
            boolean pRightContinue = true;

            // find all pbean's super beans and sub sub beans
            while(pLeftContinue || pRightContinue)
            {
                pRightContinue = false;
                pLeftContinue = false;
                for(ProducerMethodBean pb : producerBeans)
                {
                    //left
                    if (pLeft!= null &&
                        pLeft.getBeanClass().getSuperclass().equals(pb.getBeanClass()))
                    {
                        Method superMethod = webBeansContext.getSecurityService().doPrivilegedGetDeclaredMethod(pb.getBeanClass(), method.getName(), method.getParameterTypes());

                        //Added by GE, method check is necessary otherwise getting wrong method qualifier annotations
                        if (superMethod != null && superMethod.equals(pb.getCreatorMethod()))
                        {
                            if (disabledProducerMethods.contains(superMethod))
                            {
                                throw new DefinitionException("Multiple specializations for the same producer method got detected for type"
                                        + pbean.toString());
                            }
                            disabledProducerMethods.add(superMethod);
                            producerBeanListHelper.add(pb);
                            pLeft = (pb.isSpecializedBean()) ? pb : null;
                        }
                        else
                        {
                            pLeft = null;
                        }
                        if (pLeft != null)
                        {
                            pLeftContinue = true;
                        }
                    }
                    //right
                    if (pRight != null &&
                        pb.getBeanClass().getSuperclass().equals(pRight.getBeanClass()))
                    {
                        if (!pb.isSpecializedBean())
                        {
                            pRight = null;
                        }
                        else
                        {
                            Method superMethod = webBeansContext.getSecurityService().doPrivilegedGetDeclaredMethod(pb.getBeanClass(),
                                                                                                                    method.getName(), method.getParameterTypes());
                            //Added by GE, method check is necessary otherwise getting wrong method qualifier annotations
                            if (superMethod != null && superMethod.equals(pb.getCreatorMethod()))
                            {
                                producerBeanListHelper.add(pb);
                                pRight = pb;
                            }
                            else
                            {
                                pRight = null;
                            }
                        }
                        if (pRight != null)
                        {
                            pRightContinue = true;
                        }
                    }
                } // for
            } // while

            //remove the group from producer bean list
            for(ProducerMethodBean pb : producerBeanListHelper.getList())
            {
                producerBeans.remove(pb);
            }
            //configure the directly extended producer beans
            configSpecializedProducerMethodBeans(producerBeanListHelper.getList());
        }
    }



    public <T> Constructor<T> getNoArgConstructor(Class<T> clazz)
    {
        return webBeansContext.getSecurityService().doPrivilegedGetDeclaredConstructor(clazz);
    }

    /**
     * Configures the name of the producer method for specializing the parent.
     *
     * @param component producer method component
     * @param method specialized producer method
     * @param superMethod overriden super producer method
     */
    public boolean isSuperMethodNamed(AbstractOwbBean<?> component, Method method, Method superMethod)
    {
        return webBeansContext.getAnnotationManager().isSuperMethodNamed(component, method, superMethod);
    }

    /**
     * Returns true if instance injection point false otherwise.
     *
     * @param injectionPoint injection point definition
     * @return true if instance injection point
     */
    public static boolean checkObtainsInjectionPointConditions(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();

        Class<?> candidateClazz = ClassUtil.getClass(type);

        if(!candidateClazz.isAssignableFrom(Instance.class))
        {
            return false;
        }

        Class<?> rawType;

        if(ClassUtil.isParametrizedType(injectionPoint.getType()))
        {
            ParameterizedType pt = (ParameterizedType)injectionPoint.getType();

            rawType = (Class<?>) pt.getRawType();

            Type[] typeArgs = pt.getActualTypeArguments();

            if(!(rawType.isAssignableFrom(Instance.class)))
            {
                throw new WebBeansConfigurationException("<Instance> field injection " + injectionPoint.toString()
                        + " must have type javax.inject.Instance");
            }
            else
            {
                if(typeArgs.length != 1)
                {
                    throw new WebBeansConfigurationException("<Instance> field injection " + injectionPoint.toString()
                            + " must not have more than one actual type argument");
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("<Instance> field injection " + injectionPoint.toString()
                    + " must be defined as ParameterizedType with one actual type argument");
        }

        return true;
    }

    public static void checkNullInstance(Object instance, Class<? > scopeType, String errorMessage, 
            Object... errorMessageArgs)
    {
        if (instance == null)
        {
            if (!scopeType.equals(Dependent.class))
            {
                String message = format(errorMessage, errorMessageArgs);
                throw new IllegalProductException(message);
            }
        }
    }

    public void checkSerializableScopeType(Class<? extends Annotation> scopeType, boolean isSerializable, String errorMessage,
            Object... errorMessageArgs)
    {
        if (webBeansContext.getBeanManagerImpl().isPassivatingScope(scopeType))
        {
            if (!isSerializable)
            {
                String message = format(errorMessage, errorMessageArgs);
                throw new IllegalProductException(message);
            }
        }
    }

    public static Bean<?> getMostSpecializedBean(BeanManager manager, Bean<?> component)
    {
         Set<Bean<?>> beans;

         if (component instanceof EnterpriseBeanMarker)
         {
             beans = new HashSet<Bean<?>>();
             Set<Bean<?>> allBeans = manager.getBeans(Object.class, AnnotationUtil.asArray(component.getQualifiers()));

             for(Bean<?> candidateBean : allBeans)
             {
                 if (candidateBean instanceof EnterpriseBeanMarker)
                 {
                     /*
                      * If a bean class of a session bean X is annotated @Specializes, then the bean class of X must directly extend
                      * the bean class of another session bean Y. Then X directly specializes Y, as defined in Section 4.3, ‚"Specialization".
                      */
                     Class<?> candidateSuperClass = candidateBean.getBeanClass().getSuperclass();
                     if (candidateSuperClass.equals(component.getBeanClass()))
                     {
                         beans.add(candidateBean);
                     }
                 }
             }
         }
         else
         {
             beans = manager.getBeans(component.getBeanClass(),
                     AnnotationUtil.asArray(component.getQualifiers()));
         }

        for(Bean<?> bean : beans)
        {
            Bean<?> find = bean;

            if(!find.equals(component))
            {
                if(AnnotationUtil.hasClassAnnotation(find.getBeanClass(), Specializes.class))
                {
                    return getMostSpecializedBean(manager, find);
                }
            }
        }

        return component;
    }

    /**
     * Returns <code>ProcessAnnotatedType</code> event.
     * @param <T> bean type
     * @param annotatedType bean class
     * @return event
     */
    public <T> GProcessAnnotatedType fireProcessAnnotatedTypeEvent(AnnotatedType<T> annotatedType)
    {
        GProcessAnnotatedType processAnnotatedEvent = new GProcessAnnotatedType(annotatedType);

        //Fires ProcessAnnotatedType
        webBeansContext.getBeanManagerImpl().fireEvent(processAnnotatedEvent, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);

        if (processAnnotatedEvent.isModifiedAnnotatedType())
        {
            webBeansContext.getAnnotatedElementFactory().setAnnotatedType(processAnnotatedEvent.getAnnotatedType());
        }

        return processAnnotatedEvent;
    }

    /**
     * Returns <code>ProcessAnnotatedType</code> event.
     * @param <T> bean type
     * @param annotatedType bean class
     * @return event
     */
    public <T> GProcessSyntheticAnnotatedType fireProcessSyntheticAnnotatedTypeEvent(AnnotatedType<T> annotatedType)
    {
        Extension source = AnnotatedTypeWrapper.class.isInstance(annotatedType) ? AnnotatedTypeWrapper.class.cast(annotatedType).getSource() : null;
        GProcessSyntheticAnnotatedType gProcessSyntheticAnnotatedType = new GProcessSyntheticAnnotatedType(source, annotatedType);

        //Fires ProcessSyntheticAnnotatedType
        webBeansContext.getBeanManagerImpl().fireEvent(gProcessSyntheticAnnotatedType, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);

        if (gProcessSyntheticAnnotatedType.isModifiedAnnotatedType())
        {
            webBeansContext.getAnnotatedElementFactory().setAnnotatedType(gProcessSyntheticAnnotatedType.getAnnotatedType());
        }

        return gProcessSyntheticAnnotatedType;
    }

    /**
     * @param injectionPoint the original {@link InjectionPoint}
     * @return fired {@link ProcessInjectionPoint} event
     */
    public GProcessInjectionPoint fireProcessInjectionPointEvent(InjectionPoint injectionPoint)
    {
        GProcessInjectionPoint event = new GProcessInjectionPoint(injectionPoint);
        webBeansContext.getBeanManagerImpl().fireEvent(event, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        return event;
    }

    /**
     * Returns <code>ProcessInjectionTarget</code> event.
     * @param <T> bean type
     * @return event
     */
    public <T> GProcessInjectionTarget fireProcessInjectionTargetEvent(InjectionTargetImpl<T> injectionTarget, AnnotatedType<T> annotatedType)
    {
        final GProcessInjectionTarget processInjectionTargetEvent = new GProcessInjectionTarget(injectionTarget, annotatedType);
        return fireProcessInjectionTargetEvent(processInjectionTargetEvent);
    }

    private GProcessInjectionTarget fireProcessInjectionTargetEvent(GProcessInjectionTarget processInjectionTargetEvent)
    {
        //Fires ProcessInjectionTarget
        webBeansContext.getBeanManagerImpl().fireEvent(processInjectionTargetEvent, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        return processInjectionTargetEvent;
    }

    /**
     * Returns <code>ProcessInjectionTarget</code> event.
     * @param <T> bean type
     * @return event
     */
    public <T> GProcessInjectionTarget fireProcessInjectionTargetEventForJavaEeComponents(Class<T> componentClass)
    {
        final AnnotatedType<T> annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(componentClass);
        final InjectionTargetImpl<T> injectionTarget = InjectionTargetImpl.class.cast(webBeansContext.getBeanManagerImpl().createInjectionTarget(annotatedType));
        final GProcessInjectionTarget processInjectionTargetEvent = new GProcessInjectionTarget(injectionTarget,annotatedType);

        //Fires ProcessInjectionTarget
        return fireProcessInjectionTargetEvent(processInjectionTargetEvent);

    }

    public <T> Producer<T> fireProcessProducerEvent(Producer<T> producer, AnnotatedMember<?> annotatedMember)
    {
        GProcessProducer processProducerEvent = new GProcessProducer(producer, annotatedMember);
        //Fires ProcessProducer
        webBeansContext.getBeanManagerImpl().fireEvent(processProducerEvent, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessProducer event observers. Look at logs for further details");
        return processProducerEvent.getProducer();
    }

    public void fireProcessProducerMethodBeanEvent(Map<ProducerMethodBean<?>, AnnotatedMethod<?>> annotatedMethods, AnnotatedType<?> annotatedType)
    {
        WebBeansContext webBeansContext = this.webBeansContext;
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        for(Map.Entry<ProducerMethodBean<?>, AnnotatedMethod<?>> beanEntry : annotatedMethods.entrySet())
        {
            ProducerMethodBean<?> bean = beanEntry.getKey();
            AnnotatedMethod<?> annotatedMethod = beanEntry.getValue();
            Annotation[] annotationsFromSet = AnnotationUtil.asArray(bean.getQualifiers());
            Method disposal = annotationManager.getDisposalWithGivenAnnotatedMethod(annotatedType, bean.getReturnType(), annotationsFromSet);

            AnnotatedMethod<?> disposalAnnotated;
            GProcessProducerMethod processProducerMethodEvent;
            if(disposal != null)
            {
                disposalAnnotated = webBeansContext.getAnnotatedElementFactory().newAnnotatedMethod(disposal, annotatedType);
                processProducerMethodEvent = new GProcessProducerMethod(bean,annotatedMethod,
                                                                        disposalAnnotated.getParameters().get(0));
            }
            else
            {
                processProducerMethodEvent = new GProcessProducerMethod(bean,annotatedMethod,null);
            }


            //Fires ProcessProducer
            webBeansContext.getBeanManagerImpl().fireEvent(processProducerMethodEvent, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        }
    }

    public void fireProcessObservableMethodBeanEvent(Map<ObserverMethod<?>,AnnotatedMethod<?>> annotatedMethods)
    {
        for(Map.Entry<ObserverMethod<?>, AnnotatedMethod<?>> observableMethodEntry : annotatedMethods.entrySet())
        {
            ObserverMethod<?> observableMethod = observableMethodEntry.getKey();
            AnnotatedMethod<?> annotatedMethod = observableMethodEntry.getValue();

            GProcessObservableMethod event = new GProcessObservableMethod(annotatedMethod, observableMethod);

            //Fires ProcessProducer
            webBeansContext.getBeanManagerImpl().fireEvent(event, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        }
    }


    public void fireProcessProducerFieldBeanEvent(Map<ProducerFieldBean<?>,AnnotatedField<?>> annotatedFields)
    {
        for(Map.Entry<ProducerFieldBean<?>, AnnotatedField<?>> beanEntry : annotatedFields.entrySet())
        {
            ProducerFieldBean<?> bean = beanEntry.getKey();
            AnnotatedField<?> field = beanEntry.getValue();

            GProcessProducerField processProducerFieldEvent = new GProcessProducerField(bean, field, null);

            //Fire ProcessProducer
            webBeansContext.getBeanManagerImpl().fireEvent(processProducerFieldEvent, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        }
    }

    public static void checkInjectionPointNamedQualifier(InjectionPoint injectionPoint)
    {
        Set<Annotation> qualifierset = injectionPoint.getQualifiers();
        Named namedQualifier = null;
        for(Annotation qualifier : qualifierset)
        {
            if(qualifier.annotationType().equals(Named.class))
            {
                namedQualifier = (Named)qualifier;
                break;
            }
        }

        if(namedQualifier != null)
        {
            String value = namedQualifier.value();

            if(value == null || value.equals(""))
            {
                Member member = injectionPoint.getMember();
                if(!(member instanceof Field))
                {
                    throw new WebBeansConfigurationException("Injection point type : " + injectionPoint
                                                             + " can not define @Named qualifier without value!");
                }
            }
        }

    }

    /**
     * Sets bean enabled flag.
     * @param bean bean instance
     */
    public void setInjectionTargetBeanEnableFlag(InjectionTargetBean<?> bean)
    {
        bean.setEnabled(isBeanEnabled(bean.getAnnotatedType(), bean.getStereotypes()));
    }

    public boolean isBeanEnabled(AnnotatedType<?> at, Set<Class<? extends Annotation>> stereotypes)
    {
        boolean isAlternative = isAlternative(at, stereotypes);

        return !isAlternative || webBeansContext.getAlternativesManager().isAlternative(at.getJavaClass(), stereotypes);
    }

    public static boolean isAlternative(AnnotatedType<?> beanType, Set<Class<? extends Annotation>> stereotypes)
    {
        Asserts.assertNotNull(beanType, "bean type can not be null");
        Asserts.assertNotNull(stereotypes, "stereotypes can not be null");

        boolean alternative = false;

        if(beanType.getAnnotation(Alternative.class) != null)
        {
            alternative = true;
        }

        if(!alternative)
        {
            for(Class<? extends Annotation> stereoType : stereotypes)
            {
                if(AnnotationUtil.hasClassAnnotation(stereoType, Alternative.class))
                {
                    alternative = true;
                    break;
                }
            }

        }

        return alternative;

    }

    public void setBeanEnableFlagForProducerBean(InjectionTargetBean<?> parent, AbstractProducerBean<?> producer, Annotation[] annotations)
    {
        Asserts.assertNotNull(parent, "parent can not be null");
        Asserts.assertNotNull(producer, "producer can not be null");
        Set<Class<? extends Annotation>> stereotypes = producer.getStereotypes();

        boolean alternative = false;

        if (AnnotationUtil.hasAnnotation(annotations, Alternative.class))
        {
            alternative = true;
        }

        if (!alternative)
        {
            for (Class<? extends Annotation> stereoType : stereotypes)
            {
                if (AnnotationUtil.hasClassAnnotation(stereoType, Alternative.class))
                {
                    alternative = true;
                    break;
                }
            }
        }

        if (alternative)
        {
            // either the parent class is an enabled Alternative
            // or the stereotype directly on the producer field or method is an enabled Alternative
            producer.setEnabled(isAlternative(parent.getAnnotatedType(), parent.getStereotypes()) && webBeansContext.getAlternativesManager().isAlternative(parent) ||
                                isAlternative(parent.getAnnotatedType(), stereotypes) && webBeansContext.getAlternativesManager().isAlternative(producer));
        }
        else
        {
            producer.setEnabled(parent.isEnabled());
        }
    }

    public static boolean isExtensionBeanEventType(Type type)
    {
        return type.equals(GProcessAnnotatedType.class) ||
               type.equals(GProcessSyntheticAnnotatedType.class) ||
               type.equals(GProcessInjectionPoint.class) ||
               type.equals(GProcessInjectionTarget.class) ||
               type.equals(GProcessBeanAttributes.class) ||
               type.equals(GProcessManagedBean.class) ||
               type.equals(GProcessSessionBean.class) ||
               type.equals(GProcessBean.class);
    }

    public static boolean isDefaultExtensionBeanEventType(Class<?> clazz)
    {
        return clazz.equals(ProcessAnnotatedType.class) ||
               clazz.equals(ProcessSyntheticAnnotatedType.class) ||
               clazz.equals(ProcessInjectionPoint.class) ||
               clazz.equals(ProcessInjectionTarget.class) ||
               clazz.equals(ProcessBeanAttributes.class) ||
               clazz.equals(ProcessManagedBean.class) ||
               clazz.equals(ProcessBean.class) ||
               clazz.equals(ProcessSessionBean.class);
    }

    public static boolean isExtensionProducerOrObserverEventType(Type type)
    {
        return type.equals(GProcessProducer.class) ||
               type.equals(GProcessProducerField.class) ||
               type.equals(GProcessProducerMethod.class) ||
               type.equals(GProcessObservableMethod.class);
    }

    public static boolean isDefaultExtensionProducerOrObserverEventType(Class<?> clazz)
    {
        return clazz.equals(ProcessProducer.class) ||
               clazz.equals(ProcessProducerField.class) ||
               clazz.equals(ProcessProducerMethod.class) ||
               clazz.equals(ProcessObserverMethod.class);

    }

    public static boolean isDependent(Bean<?> bean)
    {
        if(!(bean instanceof OwbBean))
        {
            return bean.getScope().equals(Dependent.class);
        }

        return ((OwbBean) bean).isDependent();
    }

    public void inspectErrorStack(String logMessage)
    {
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        //Looks for errors
        ErrorStack stack = manager.getErrorStack();
        try
        {
            if(stack.hasErrors())
            {
                stack.logErrors();
                throw new WebBeansConfigurationException(logMessage);
            }
        }
        finally
        {
            stack.clear();
        }
    }

    /**
     *
     * @param contextual the {@link Bean} to check
     * @return the uniqueId if it is {@link PassivationCapable} and enabled
     */
    public static String getPassivationId(Contextual<?> contextual)
    {
        if(contextual instanceof Bean)
        {
            if(contextual instanceof AbstractOwbBean)
            {
                if( ((AbstractOwbBean<?>)contextual).isPassivationCapable())
                {
                    return ((AbstractOwbBean<?>)contextual).getId();
                }
            }

            else if(contextual instanceof PassivationCapable)
            {
                PassivationCapable pc = (PassivationCapable)contextual;

                return pc.getId();
            }
        }
        else
        {
            if((contextual instanceof PassivationCapable) && (contextual instanceof Serializable))
            {
                PassivationCapable pc = (PassivationCapable)contextual;

                return pc.getId();
            }
        }

        return null;
    }

    /**
     * This method will be used in {@link AfterBeanDiscovery#addBean(javax.enterprise.inject.spi.Bean)}}
     */
    public <T> ManagedBean<T> defineManagedBeanWithoutFireEvents(AnnotatedType<T> type)
    {
        BeanAttributesImpl<T> beanAttributes = BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(type).build();
        ManagedBeanBuilder<T, ManagedBean<T>> managedBeanCreator = new ManagedBeanBuilder<T, ManagedBean<T>>(webBeansContext, type, beanAttributes);

        //Check for Enabled via Alternative
        setInjectionTargetBeanEnableFlag(managedBeanCreator.getBean());
        ManagedBean<T> managedBean = managedBeanCreator.getBean();
        new ProducerMethodBeansBuilder(managedBean.getWebBeansContext(), managedBean.getAnnotatedType()).defineProducerMethods(managedBean);
        new ProducerFieldBeansBuilder(managedBean.getWebBeansContext(), managedBean.getAnnotatedType()).defineProducerFields(managedBean);
        new ObserverMethodsBuilder<T, InjectionTargetBean<T>>(webBeansContext, managedBean.getAnnotatedType()).defineObserverMethods(managedBean);

        if (managedBean.getProducer() instanceof AbstractProducer)
        {
            AbstractProducer<T> producer = (AbstractProducer<T>)managedBean.getProducer();
            producer.defineInterceptorStack(managedBean, managedBean.getAnnotatedType(), webBeansContext);
        }

        return managedBean;
    }

    public boolean isPassivationCapableDependency(InjectionPoint injectionPoint)
    {
        //Don't attempt to get an instance of the delegate injection point
        if (injectionPoint.isDelegate())
        {
            return true;
        }
        InjectionResolver instance = webBeansContext.getBeanManagerImpl().getInjectionResolver();

        Bean<?> bean = instance.getInjectionPointBean(injectionPoint);
        if((bean instanceof EnterpriseBeanMarker) ||
                (bean instanceof ResourceBean) ||
                (bean instanceof InstanceBean) ||
                (bean instanceof EventBean) ||
                (bean instanceof InjectionPointBean) ||
                (bean instanceof BeanManagerBean)
                )
        {
            return true;
        }

        else if(webBeansContext.getBeanManagerImpl().isNormalScope(bean.getScope()))
        {
            return true;
        }
        else
        {
            if(getPassivationId(bean) != null)
            {
                return true;
            }
        }
        return false;
    }

    public static void throwRuntimeExceptions(Exception e)
    {
        if(RuntimeException.class.isAssignableFrom(e.getClass()))
        {
            throw (RuntimeException)e;
        }

        throw new RuntimeException(e);
    }

    /**
     * @return <code>true</code> if this annotated type represents a decorator.
     */
    public static boolean isDecorator(AnnotatedType<?> annotatedType)
    {
        return annotatedType.isAnnotationPresent(Decorator.class);
    }

    /**
     * Return true if this annotated type represents a decorator.
     * @param annotatedType annotated type
     * @return true if decorator
     */
    public boolean isAnnotatedTypeDecoratorOrInterceptor(AnnotatedType<?> annotatedType)
    {
        if(isDecorator(annotatedType) ||
           isCdiInterceptor(annotatedType))
        {
            return true;
        }
        else if(webBeansContext.getInterceptorsManager().isInterceptorClassEnabled(annotatedType.getJavaClass()))
        {
            return true;
        }
        else if(webBeansContext.getDecoratorsManager().isDecoratorEnabled(annotatedType.getJavaClass()))
        {
            return true;
        }


        return false;
    }

    /**
     * @return <code>true</code> if this AnnotatedType represents a CDI Interceptor
     *         defined via a {@link javax.interceptor.Interceptor} annotation
     */
    public static boolean isCdiInterceptor(AnnotatedType<?> annotatedType)
    {
        return annotatedType.isAnnotationPresent(javax.interceptor.Interceptor.class);
    }

    /**
     * Checks the implementation class for checking conditions.
     *
     * @param type implementation class
     * @throws org.apache.webbeans.exception.WebBeansConfigurationException if any configuration exception occurs
     */
    public <X> void checkManagedBeanCondition(AnnotatedType<X> type) throws WebBeansConfigurationException
    {
        int modifier = type.getJavaClass().getModifiers();

        if (type.isAnnotationPresent(Decorator.class) && type.isAnnotationPresent(javax.interceptor.Interceptor.class))
        {
            throw new WebBeansConfigurationException("Annotated type "+ type +  " may not annotated with both @Interceptor and @Decorator annotation");
        }

        if (!type.isAnnotationPresent(Decorator.class) && !type.isAnnotationPresent(javax.interceptor.Interceptor.class))
        {
            checkManagedWebBeansInterceptorConditions(type);
        }

        if (Modifier.isInterface(modifier))
        {
            throw new WebBeansConfigurationException("ManagedBean implementation class : " + type.getJavaClass().getName() + " may not defined as interface");
        }
    }

    private <X> void checkManagedWebBeansInterceptorConditions(AnnotatedType<X> type)
    {
        Annotation[] anns = AnnotationUtil.asArray(type.getAnnotations());

        Class<?> clazz = type.getJavaClass();
        boolean hasClassInterceptors = false;
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        if (annotationManager.getInterceptorBindingMetaAnnotations(anns).length > 0)
        {
            hasClassInterceptors = true;
        }
        else
        {
            Annotation[] stereoTypes = annotationManager.getStereotypeMetaAnnotations(anns);
            for (Annotation stero : stereoTypes)
            {
                if (annotationManager.hasInterceptorBindingMetaAnnotation(stero.annotationType().getDeclaredAnnotations()))
                {
                    hasClassInterceptors = true;
                    break;
                }
            }
        }

        if(Modifier.isFinal(clazz.getModifiers()) && hasClassInterceptors)
        {
            throw new WebBeansConfigurationException("Final managed bean class with name : " + clazz.getName() + " can not define any InterceptorBindings");
        }

        Set<AnnotatedMethod<? super X>> methods = webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(type);
        for(AnnotatedMethod<? super X> methodA : methods)
        {
            Method method = methodA.getJavaMember();
            int modifiers = method.getModifiers();
            if (!method.isSynthetic() && !method.isBridge() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && Modifier.isFinal(modifiers))
            {
                if (hasClassInterceptors)
                {
                    throw new WebBeansConfigurationException("Maanged bean class : " + clazz.getName()
                                                    + " can not define non-static, non-private final methods. Because it is annotated with at least one @InterceptorBinding");
                }

                if (annotationManager.hasInterceptorBindingMetaAnnotation(
                    AnnotationUtil.asArray(methodA.getAnnotations())))
                {
                    throw new WebBeansConfigurationException("Method : " + method.getName() + "in managed bean class : " + clazz.getName()
                                                    + " can not be defined as non-static, non-private and final . Because it is annotated with at least one @InterceptorBinding");
                }
            }

        }
    }

    // Note: following code for method 'format' is taken from google guava - apache 2.0 licenced library
    // com.google.common.base.Preconditions.format(String, Object...)
    /**
     * Substitutes each {@code %s} in {@code template} with an argument. These
     * are matched by position - the first {@code %s} gets {@code args[0]}, etc.
     * If there are more arguments than placeholders, the unmatched arguments will
     * be appended to the end of the formatted message in square braces.
     *
     * @param template a non-null string containing 0 or more {@code %s}
     *     placeholders.
     * @param args the arguments to be substituted into the message
     *     template. Arguments are converted to strings using
     *     {@link String#valueOf(Object)}. Arguments can be null.
     */
    private static String format(String template,
            Object... args)
    {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        StringBuilder builder = new StringBuilder(
                template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;
        while (i < args.length)
        {
            int placeholderStart = template.indexOf("%s", templateStart);
            if (placeholderStart == -1)
            {
                break;
            }
            builder.append(template.substring(templateStart, placeholderStart));
            builder.append(args[i++]);
            templateStart = placeholderStart + 2;
        }
        builder.append(template.substring(templateStart));

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length)
        {
            builder.append(" [");
            builder.append(args[i++]);
            while (i < args.length)
            {
                builder.append(", ");
                builder.append(args[i++]);
            }
            builder.append(']');
        }

        return builder.toString();
    }

    public void validate(final Set<InjectionPoint> injectionPoints, final boolean isDecorator)
    {
        boolean delegateFound = false;
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            if (!injectionPoint.isDelegate())
            {
                webBeansContext.getBeanManagerImpl().validate(injectionPoint);
            }
            else
            {
                if (!isDecorator)
                {
                    throw new WebBeansConfigurationException(
                            "Delegate injection points can not defined by beans that are not decorator. Injection point : "
                                    + injectionPoint);
                }
                else if (delegateFound)
                {
                    throw new WebBeansConfigurationException(
                            "Only one Delegate injection point can be defined by decorator. Decorator : "
                                    + injectionPoint.getBean());
                }
                else
                {
                    delegateFound = true;
                }
            }
        }
    }

    public void checkTypeVariables(final TypeLiteral<?> subtype)
    {
        final Type t = subtype.getType();
        Boolean result = noTypeVariables.get(t);
        if (result != null)
        {
            if (!result)
            {
                throw new IllegalArgumentException(t + " has a TypeVariable which is forbidden");
            }
            return;
        }

        if (ParameterizedType.class.isInstance(t))
        {
            for (final Type arg : ParameterizedType.class.cast(t).getActualTypeArguments())
            {
                if (TypeVariable.class.isInstance(arg))
                {
                    noTypeVariables.putIfAbsent(t, false);
                    throw new IllegalArgumentException(arg + " is a TypeVariable which is forbidden");
                }
            }
        }
        noTypeVariables.putIfAbsent(t, true);
    }

    public void validEventType(final Type eventType, final Type metadataType)
    {
        final EventCacheKey key = new EventCacheKey(eventType, metadataType);
        if (validEventType.containsKey(key))
        {
            return;
        }

        if (GenericsUtil.hasTypeParameters(eventType))
        {
            final Type et = GenericsUtil.resolveType(GenericsUtil.getParameterizedType(eventType), metadataType);
            if (OwbParametrizedTypeImpl.class.isInstance(et))
            {
                for (final Type t : OwbParametrizedTypeImpl.class.cast(et).getActualTypeArguments())
                {
                    if (OwbWildcardTypeImpl.class.isInstance(t))
                    {
                        throw new IllegalArgumentException("TypeVariable forbidden for events");
                    }
                }
            }
        }
        validEventType.putIfAbsent(key, true); // we don't care about the value but that's thread safe to use this map
    }

    public boolean isContainerEventType(final Object event)
    {
        if (event == null)
        {
            return false;
        }

        final Class<?> eventType = event.getClass();
        if (notContainerEvents.containsKey(eventType))
        {
            return false;
        }

        if (AfterBeanDiscovery.class.isInstance(event)
                || AfterDeploymentValidation.class.isInstance(event)
                || BeforeShutdown.class.isInstance(event)
                || ProcessAnnotatedType.class.isInstance(event)
                || ProcessInjectionPoint.class.isInstance(event)
                || ProcessInjectionTarget.class.isInstance(event)
                || ProcessBeanAttributes.class.isInstance(event)
                || ProcessBean.class.isInstance(event)
                || ProcessObserverMethod.class.isInstance(event)
                || ProcessSessionBean.class.isInstance(event)
                || ProcessProducer.class.isInstance(event)
                || ProcessProducerField.class.isInstance(event)
                || ProcessProducerMethod.class.isInstance(event)
                || BeforeBeanDiscovery.class.isInstance(event))
        {
            return true;
        }

        notContainerEvents.putIfAbsent(eventType, true);
        return false;
    }

    public <T> BeanAttributes<T> fireProcessBeanAttributes(final AnnotatedType<T> annotatedType, final Class<?> type, final BeanAttributes<T> ba)
    {
        // we don't use bm stack since it is actually quite useless
        final ProcessBeanAttributesImpl event = new GProcessBeanAttributes(type, annotatedType, ba);
        try
        {
            webBeansContext.getBeanManagerImpl().fireEvent(event, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);
        }
        catch (final Exception e)
        {
            throw new DefinitionException("event ProcessBeanAttributes thrown an exception for " + annotatedType, e);
        }

        if (event.getDefinitionError() != null)
        {
            throw new DefinitionException(event.getDefinitionError());
        }

        final BeanAttributes<T> beanAttributes;
        if (event.getAttributes() != ba)
        {
            beanAttributes = event.getAttributes();
            if (!webBeansContext.getBeanManagerImpl().isScope(beanAttributes.getScope()))
            {
                throw new DefinitionException(beanAttributes.getScope() + " is not a scope");
            }
        }
        else
        {
            beanAttributes = ba;
        }
        if (event.isVeto())
        {
            return null;
        }
        return beanAttributes;
    }

    private static final class EventCacheKey
    {
        private final Type event;
        private final Type metadata;
        private final int hashCache;

        private EventCacheKey(final Type event, final Type metadata)
        {
            this.event = event;
            this.metadata = metadata;

            int result = event != null ? event.hashCode() : 0;
            result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
            this.hashCache = result;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || EventCacheKey.class != o.getClass())
            {
                return false;
            }

            final EventCacheKey that = EventCacheKey.class.cast(o);
            if (event != null ? !event.equals(that.event) : that.event != null)
            {
                return false;
            }
            if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null)
            {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            return hashCache;
        }
    }
}
