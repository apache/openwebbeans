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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.ClassUtil;
import org.apache.xbean.asm6.ClassWriter;
import org.apache.xbean.asm6.MethodVisitor;
import org.apache.xbean.asm6.Opcodes;
import org.apache.xbean.asm6.Type;

/**
 * This factory creates subclasses for abstract classes.
 * This is being used for Abstract Decorators.
 */
public class SubclassProxyFactory extends AbstractProxyFactory
{

    public SubclassProxyFactory(WebBeansContext webBeansContext)
    {
        super(webBeansContext);
    }

    @Override
    protected Class getMarkerInterface()
    {
        return OwbDecoratorProxy.class;
    }


    public <T> Class<T> createImplementedSubclass(ClassLoader classLoader, Class<T> classToProxy)
    {
        if (!Modifier.isAbstract(classToProxy.getModifiers()))
        {
            throw new WebBeansConfigurationException("Only abstract classes should get subclassed, not " + classToProxy);
        }


        Class<T> proxyClass = tryToLoadClass(classLoader, classToProxy);
        if (proxyClass != null)
        {
            return proxyClass;
        }

        proxyClass = createSubClass(classLoader, classToProxy);

        return proxyClass;
    }

    private <T> Class<T> tryToLoadClass(ClassLoader classLoader, Class<T> classToProxy)
    {
        String proxyClassName = getSubClassName(classToProxy);
        try
        {
            // if the class is already registered, then use this one.
            return (Class<T>) Class.forName(proxyClassName, true, classLoader);
        }
        catch (ClassNotFoundException cnfe)
        {
            // this means we need to generate that class
        }
        return null;
    }

    private <T> String getSubClassName(Class<T> classToProxy)
    {
        return fixPreservedPackages(classToProxy.getName() + "$$OwbSubClass");
    }

    /**
     * @param classLoader to use for creating the class in
     * @param classToProxy the class for which a subclass will get generated
     * @param <T>
     * @return the proxy class
     */
    public synchronized <T> Class<T> createSubClass(ClassLoader classLoader, Class<T> classToProxy)
            throws ProxyGenerationException
    {
        Class<T> clazz = tryToLoadClass(classLoader, classToProxy);
        if (clazz != null)
        {
            return clazz;
        }

        String proxyClassName = getSubClassName(classToProxy);

        List<Method> methods = ClassUtil.getNonPrivateMethods(classToProxy, true);
        Method[] businessMethods = methods.toArray(new Method[methods.size()]);

        clazz = createProxyClass(classLoader, proxyClassName, classToProxy, businessMethods, new Method[0]);

        return clazz;
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
                superDefaultCt = classToProxy.getConstructor(null);
            }

            final String descriptor = Type.getConstructorDescriptor(superDefaultCt);
            final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parentClassFileName, "<init>", descriptor, false);

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
    }

    @Override
    protected void delegateInterceptedMethods(ClassLoader classLoader, ClassWriter cw, String proxyClassFileName, Class<?> classToProxy,
                                              Method[] interceptedMethods) throws ProxyGenerationException
    {
    }

    @Override
    protected void createSerialisation(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName)
    {
        // nothing to do ;)
    }

    @Override
    protected void delegateNonInterceptedMethods(ClassLoader classLoader, ClassWriter cw, String proxyClassFileName, Class<?> classToProxy,
                                                 Method[] noninterceptedMethods) throws ProxyGenerationException
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

            int targetModifiers = delegatedMethod.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC | MODIFIER_VARARGS);

            MethodVisitor mv = cw.visitMethod(targetModifiers, delegatedMethod.getName(), methodDescriptor, null, exceptionTypeNames);

            // fill method body
            mv.visitCode();

            boolean abstractMethod = Modifier.isAbstract(delegatedMethod.getModifiers());

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
            if (abstractMethod)
            {
                // generate an empty return block
            }
            else
            {
                // invoke the method on the super class;
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, declaringClass.getInternalName(), delegatedMethod.getName(), methodDescriptor, false);
            }

            generateReturn(mv, delegatedMethod);

            mv.visitMaxs(-1, -1);

            mv.visitEnd();
        }
    }

}
