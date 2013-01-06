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

import javax.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * This factory creates proxies which delegate the
 * method invocations 1:1 to an instance which gets
 * resolved via a {@link javax.inject.Provider}.
 */
public class NormalScopeProxyFactory extends AbstractProxyFactory
{
    /** the name of the field which stores the {@link Provider} for the Contextual Instance */
    public static final String FIELD_INSTANCE_PROVIDER = "owbContextualInstanceProvider";


    @Override
    protected Class getMarkerInterface()
    {
        return OwbNormalScopeProxy.class;
    }

    /**
     * @param classLoader to use for creating the class in
     * @param classToProxy the class for which a subclass will get generated
     * @param nonInterceptedMethods all methods which are <b>not</b> intercepted nor decorated and shall get delegated directly
     * @param <T>
     * @return the proxy class
     */
    public synchronized <T> Class<T> createProxyClass(ClassLoader classLoader, Class<T> classToProxy,
                                                      Method[] nonInterceptedMethods)
            throws ProxyGenerationException
    {
        String proxyClassName = classToProxy.getName() + "$OwbNormalScopeProxy";

        Class<T> clazz = createProxyClass(classLoader, proxyClassName, classToProxy, null, nonInterceptedMethods);

        return clazz;
    }

    public <T> T createProxyInstance(Class<T> proxyClass, Provider provider)
            throws ProxyGenerationException
    {
        try
        {
            T proxy = proxyClass.newInstance();

            Field delegateField = proxy.getClass().getDeclaredField(FIELD_INSTANCE_PROVIDER);
            delegateField.setAccessible(true);
            delegateField.set(proxy, provider);

            return proxy;
        }
        catch (InstantiationException e)
        {
            throw new ProxyGenerationException(e);
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
            Constructor superDefaultCt = classToProxy.getConstructor(null);

            final String descriptor = Type.getConstructorDescriptor(superDefaultCt);
            final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classFileName, "<init>", descriptor);

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
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, declaringClass.getInternalName(), delegatedMethod.getName(), methodDescriptor);

            generateReturn(mv, delegatedMethod);

            mv.visitMaxs(-1, -1);

            mv.visitEnd();
        }
    }
}
