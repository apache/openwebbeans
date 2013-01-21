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
package org.apache.webbeans.proxy;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Provider;
import java.io.ObjectStreamException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.NormalScopedBeanInterceptorHandler;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.xbean.asm.ClassWriter;
import org.apache.xbean.asm.MethodVisitor;
import org.apache.xbean.asm.Opcodes;
import org.apache.xbean.asm.Type;

/**
 * This factory creates proxies which delegate the
 * method invocations 1:1 to an instance which gets
 * resolved via a {@link javax.inject.Provider}.
 */
public class NormalScopeProxyFactory extends AbstractProxyFactory
{
    /** the name of the field which stores the {@link Provider} for the Contextual Instance */
    public static final String FIELD_INSTANCE_PROVIDER = "owbContextualInstanceProvider";

    private ConcurrentHashMap<Bean<?>, Class<?>> cachedProxyClasses = new ConcurrentHashMap<Bean<?>, Class<?>>();


    public NormalScopeProxyFactory(WebBeansContext webBeansContext)
    {
        super(webBeansContext);
    }

    @Override
    protected Class getMarkerInterface()
    {
        return OwbNormalScopeProxy.class;
    }

    /**
     * @return the internal instance which gets proxied.
     */
    public Provider getInstanceProvider(OwbNormalScopeProxy proxyInstance)
    {
        try
        {
            Field internalInstanceField = proxyInstance.getClass().getDeclaredField(FIELD_INSTANCE_PROVIDER);
            internalInstanceField.setAccessible(true);
            return (Provider) internalInstanceField.get(proxyInstance);
        }
        catch (Exception e)
        {
            ExceptionUtil.throwAsRuntimeException(e);
        }
        return null;
    }

    public <T> T createNormalScopeProxy(Bean<T> bean)
    {
        ClassLoader classLoader = bean.getClass().getClassLoader();

        Class<T> classToProxy;
        if (bean instanceof OwbBean)
        {
            classToProxy = ((OwbBean<T>) bean).getReturnType();
        }
        else
        {
            // TODO: that might be wrong sometimes
            classToProxy = (Class<T>) bean.getBeanClass();
        }



        Class<? extends T> proxyClass = (Class<? extends T>) cachedProxyClasses.get(bean);

        if (proxyClass == null)
        {
            proxyClass = createProxyClass(bean, classLoader, classToProxy);
        }

        return createProxyInstance(proxyClass, getInstanceProvider(classLoader, bean));
    }

    public Provider getInstanceProvider(ClassLoader classLoader, Bean<?> bean)
    {
        String scopeClassName = bean.getScope().getName();
        Class<? extends Provider> instanceProviderClass = null;
        String proxyMappingConfigKey = OpenWebBeansConfiguration.PROXY_MAPPING_PREFIX + scopeClassName;
        String className = webBeansContext.getOpenWebBeansConfiguration().getProperty(proxyMappingConfigKey);
        if (className == null || NormalScopedBeanInterceptorHandler.class.getName().equals(className))
        {
            return new NormalScopedBeanInterceptorHandler(webBeansContext.getBeanManagerImpl(), bean);
        }

        try
        {
            instanceProviderClass = (Class<? extends Provider>) Class.forName(className, true, classLoader);
            Constructor<?> ct = instanceProviderClass.getConstructor(BeanManager.class, Bean.class);
            return (Provider) ct.newInstance(webBeansContext.getBeanManagerImpl(), bean);
        }
        catch (NoSuchMethodException nsme)
        {
            throw new WebBeansConfigurationException("Configured InterceptorHandler " + className +" has wrong constructor" , nsme);
        }
        catch (ClassNotFoundException e)
        {
            throw new WebBeansConfigurationException("Configured InterceptorHandler " + className +" cannot be found", e);
        }
        catch (InvocationTargetException e)
        {
            throw new WebBeansConfigurationException("Configured InterceptorHandler " + className +" creation exception", e);
        }
        catch (InstantiationException e)
        {
            throw new WebBeansConfigurationException("Configured InterceptorHandler " + className +" creation exception", e);
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return null;
    }

    public synchronized <T> Class<T> createProxyClass(Bean<T> bean, ClassLoader classLoader, Class<T> classToProxy)
    {
        Class<T> proxyClass = (Class<T>) cachedProxyClasses.get(bean);

        if (proxyClass == null)
        {
            proxyClass = createProxyClass(classLoader, classToProxy);
            cachedProxyClasses.putIfAbsent(bean, proxyClass);
        }

        return proxyClass;
    }

    @Override
    protected void createSerialisation(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName)
    {
        String[] exceptionTypeNames = {Type.getType(ObjectStreamException.class).getInternalName()};
        MethodVisitor mv = cw.visitMethod(Modifier.PUBLIC, "writeReplace", "()Ljava/lang/Object;", null, exceptionTypeNames);

        // fill method body
        mv.visitCode();

        // load the contextual instance Provider
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassFileName, FIELD_INSTANCE_PROVIDER, Type.getDescriptor(Provider.class));

        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    /**
     * @param classLoader to use for creating the class in
     * @param classToProxy the class for which a subclass will get generated
     * @param <T>
     * @return the proxy class
     */
    public <T> Class<T> createProxyClass(ClassLoader classLoader, Class<T> classToProxy)
            throws ProxyGenerationException
    {
        String proxyClassName = getUnusedProxyClassName(classLoader, classToProxy.getName() + "$OwbNormalScopeProxy");

        Method[] nonInterceptedMethods;
        if (classToProxy.isInterface())
        {
            nonInterceptedMethods = classToProxy.getMethods();
        }
        else
        {
            List<Method> methods = ClassUtil.getNonPrivateMethods(classToProxy, true);
            nonInterceptedMethods = methods.toArray(new Method[methods.size()]);
        }

        Class<T> clazz = createProxyClass(classLoader, proxyClassName, classToProxy, null, nonInterceptedMethods);

        return clazz;
    }

    public <T> T createProxyInstance(Class<T> proxyClass, Provider provider)
            throws ProxyGenerationException
    {
        try
        {
            T proxy = unsafeNewInstance(proxyClass);

            Field delegateField = proxy.getClass().getDeclaredField(FIELD_INSTANCE_PROVIDER);
            delegateField.setAccessible(true);
            delegateField.set(proxy, provider);

            return proxy;
        }
        catch (IllegalAccessException e)
        {
            throw new ProxyGenerationException(e);
        }
        catch (NoSuchFieldException e)
        {
            throw new ProxyGenerationException(e);
        }
    }


    @Override
    protected void createConstructor(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName) throws ProxyGenerationException
    {
        try
        {
            Constructor superDefaultCt;
            String parentClassFileName;
            if (classToProxy.isInterface())
            {
                parentClassFileName = Type.getInternalName(Object.class);
                superDefaultCt = Object.class.getConstructor(null);
            }
            else
            {
                parentClassFileName = classFileName;
                superDefaultCt = classToProxy.getDeclaredConstructor(null);
            }

            final String descriptor = Type.getConstructorDescriptor(superDefaultCt);
            final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parentClassFileName, "<init>", descriptor);

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassFileName, FIELD_INSTANCE_PROVIDER, Type.getDescriptor(Provider.class));

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
        catch (NoSuchMethodException e)
        {
            throw new ProxyGenerationException(e);
        }
    }

    @Override
    protected void createInstanceVariables(ClassWriter cw, Class<?> classToProxy, String classFileName)
    {
        // variable #1, the Provider<?> for the Contextual Instance
        cw.visitField(Opcodes.ACC_PRIVATE,
                FIELD_INSTANCE_PROVIDER, Type.getDescriptor(Provider.class), null, null).visitEnd();
    }

    @Override
    protected void delegateInterceptedMethods(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, Method[] interceptedMethods) throws ProxyGenerationException
    {
        // nothing to do ;)
    }

    @Override
    protected void delegateNonInterceptedMethods(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, Method[] noninterceptedMethods) throws ProxyGenerationException
    {
        for (Method delegatedMethod : noninterceptedMethods)
        {
            if (unproxyableMethod(delegatedMethod))
            {
                continue;
            }

            String methodDescriptor = Type.getMethodDescriptor(delegatedMethod);

            //X TODO handle generic exception types?
            Class[] exceptionTypes = delegatedMethod.getExceptionTypes();
            String[] exceptionTypeNames = new String[exceptionTypes.length];
            for (int i = 0; i < exceptionTypes.length; i++)
            {
                exceptionTypeNames[i] = Type.getType(exceptionTypes[i]).getInternalName();
            }

            int targetModifiers = delegatedMethod.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC);

            MethodVisitor mv = cw.visitMethod(targetModifiers, delegatedMethod.getName(), methodDescriptor, null, exceptionTypeNames);

            // fill method body
            mv.visitCode();

            // load the contextual instance Provider
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassFileName, FIELD_INSTANCE_PROVIDER, Type.getDescriptor(Provider.class));

            // invoke the get() method on the Provider
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Provider.class), "get", "()Ljava/lang/Object;");

            // and convert the Object to the target class type
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(classToProxy));


            // now calculate the parameters
            int offset = 1;
            for (Class<?> aClass : delegatedMethod.getParameterTypes())
            {
                final Type type = Type.getType(aClass);
                mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), offset);
                offset += type.getSize();
            }

            // and finally invoke the target method on the provided Contextual Instance
            final Type declaringClass = Type.getType(delegatedMethod.getDeclaringClass());
            boolean interfaceMethod = Modifier.isAbstract(delegatedMethod.getModifiers());
            mv.visitMethodInsn(interfaceMethod ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                               declaringClass.getInternalName(), delegatedMethod.getName(), methodDescriptor);

            generateReturn(mv, delegatedMethod);

            mv.visitMaxs(-1, -1);

            mv.visitEnd();
        }
    }

}
