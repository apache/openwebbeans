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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.xbean.asm.ClassWriter;
import org.apache.xbean.asm.MethodVisitor;
import org.apache.xbean.asm.Opcodes;
import org.apache.xbean.asm.Type;

/**
 * Base class for all OWB Proxy factories
 */
public abstract class AbstractProxyFactory
{
    public final static int MAX_CLASSLOAD_TRIES = 10000;

    protected WebBeansContext webBeansContext;

    /**
     * contains the instance of sun.misc.Unsafe.
     * We use it for creating the proxy instance without fully
     * initializing the class.
     */
    private Object unsafe = null;
    private Method unsafeAllocateInstance;


    /**
     * The name of the field which stores the passivationID of the Bean this proxy serves.
     * This is needed in case the proxy gets de-serialized back into a JVM
     * which didn't have this bean loaded yet.
     */
    public static final String FIELD_BEAN_PASSIVATION_ID = "owbBeanPassivationId";


    protected AbstractProxyFactory(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        initializeUnsafe();
    }

    /**
     * @return the marker interface which should be used for this proxy.
     */
    protected abstract Class getMarkerInterface();

    /**
     * generate the bytecode for creating the instance variables of the class
     */
    protected abstract void createInstanceVariables(ClassWriter cw, Class<?> classToProxy, String classFileName);

    /**
     * generate the bytecode for serialization.
     */
    protected abstract void createSerialisation(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName);

    /**
     * Each of our interceptor/decorator proxies has exactly 1 constructor
     * which invokes the super ct + sets the delegation field.
     *
     * @param cw
     * @param classToProxy
     * @param classFileName
     * @throws ProxyGenerationException
     */
    protected abstract void createConstructor(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName)
            throws ProxyGenerationException;

    /**
     * generate the bytecode for invoking all intercepted methods
     */
    protected abstract void delegateInterceptedMethods(ClassLoader classLoader, ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, Method[] interceptedMethods)
            throws ProxyGenerationException;

    /**
     * generate the bytecode for invoking all non-intercepted methods
     */
    protected abstract void delegateNonInterceptedMethods(ClassLoader classLoader, ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, Method[] noninterceptedMethods)
            throws ProxyGenerationException;

    /**
     * Detect a free classname based on the given one
     * @param proxyClassName
     * @return
     */
    protected String getUnusedProxyClassName(ClassLoader classLoader, String proxyClassName)
    {
        proxyClassName = fixPreservedPackages(proxyClassName);

        String finalName = proxyClassName;

        for (int i = 0; i < MAX_CLASSLOAD_TRIES; i++)
        {
            try
            {
                finalName = proxyClassName + i;
                Class.forName(finalName, true, classLoader);
            }
            catch (ClassNotFoundException cnfe)
            {
                // this is exactly what we need!
                return finalName;
            }
            // otherwise we continue ;)
        }

        throw new WebBeansException("Unable to detect a free proxy class name based on: " + proxyClassName);
    }

    protected String fixPreservedPackages(String proxyClassName)
    {
        proxyClassName = fixPreservedPackage(proxyClassName, "java.");
        proxyClassName = fixPreservedPackage(proxyClassName, "javax.");
        proxyClassName = fixPreservedPackage(proxyClassName, "sun.misc.");

        return proxyClassName;
    }
    /**
     * Detect if the provided className is in the forbidden package.
     * If so, move it to org.apache.webbeans.custom.
     * @param forbiddenPackagePrefix including the '.', e.g. 'javax.'
     */
    private String fixPreservedPackage(final String className, final String forbiddenPackagePrefix)
    {
        String fixedClassName = className;

        if (className.startsWith(forbiddenPackagePrefix))
        {
            fixedClassName = "org.apache.webbeans.custom." + className.substring(forbiddenPackagePrefix.length());
        }

        return fixedClassName;
    }


    /**
     * @param classLoader to use for creating the class in
     * @param classToProxy the class for which a subclass will get generated
     * @param interceptedMethods the list of intercepted or decorated business methods.
     * @param nonInterceptedMethods all methods which are <b>not</b> intercepted nor decorated and shall get delegated directly
     * @param <T>
     * @return the proxy class
     */
     protected <T> Class<T> createProxyClass(ClassLoader classLoader, String proxyClassName, Class<T> classToProxy,
                                                      Method[] interceptedMethods, Method[] nonInterceptedMethods)
            throws ProxyGenerationException
    {
        String proxyClassFileName = proxyClassName.replace('.', '/');

        final byte[] proxyBytes = generateProxy(classLoader, classToProxy, proxyClassName, proxyClassFileName, interceptedMethods, nonInterceptedMethods);

        Class<T> clazz = defineAndLoadClass(classLoader, proxyClassName, proxyBytes);

        return clazz;
    }

    private byte[] generateProxy(ClassLoader classLoader, Class<?> classToProxy, String proxyClassName, String proxyClassFileName,
                                 Method[] interceptedMethods, Method[] nonInterceptedMethods)
            throws ProxyGenerationException
    {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String classFileName = classToProxy.getName().replace('.', '/');

        String[] interfaceNames = new String[]{Type.getInternalName(getMarkerInterface())};
        String superClassName = classFileName;

        if (classToProxy.isInterface())
        {
            interfaceNames = new String[]{Type.getInternalName(classToProxy), interfaceNames[0]};
            superClassName = Type.getInternalName(Object.class);
        }

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_SYNTHETIC, proxyClassFileName, null, superClassName, interfaceNames);
        cw.visitSource(classFileName + ".java", null);

        createInstanceVariables(cw, classToProxy, classFileName);
        createSerialisation(cw, proxyClassFileName, classToProxy, classFileName);



        // create a static String Field which contains the passivationId of the Bean or null if not PassivationCapable
        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                FIELD_BEAN_PASSIVATION_ID, Type.getDescriptor(String.class), null, null).visitEnd();

        createConstructor(cw, proxyClassFileName, classToProxy, classFileName);


        if (nonInterceptedMethods != null)
        {
            delegateNonInterceptedMethods(classLoader, cw, proxyClassFileName, classToProxy, nonInterceptedMethods);
        }

        if (interceptedMethods != null)
        {
            delegateInterceptedMethods(classLoader, cw, proxyClassFileName, classToProxy, interceptedMethods);
        }

        return cw.toByteArray();
    }


    /**
     * The 'defineClass' method on the ClassLoader is protected, thus we need to invoke it via reflection.
     * @return the Class which got loaded in the classloader
     */
    private <T> Class<T> defineAndLoadClass(ClassLoader classLoader, String proxyName, byte[] proxyBytes)
            throws ProxyGenerationException
    {
        Class<?> clazz = classLoader.getClass();

        Method defineClassMethod = null;
        do
        {
            try
            {
                defineClassMethod = clazz.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            }
            catch (NoSuchMethodException e)
            {
                // do nothing, we need to search the superclass
            }

            clazz = clazz.getSuperclass();
        } while (defineClassMethod == null && clazz != Object.class);

        if (defineClassMethod == null)
        {
            throw new ProxyGenerationException("could not find 'defineClass' method in the ClassLoader!");
        }

        defineClassMethod.setAccessible(true);
        try
        {
            Class<T> definedClass = (Class<T>) defineClassMethod.invoke(classLoader, proxyName, proxyBytes, 0, proxyBytes.length);

            try
            {
                Class<T> loadedClass = (Class<T>) Class.forName(definedClass.getName(), true, classLoader);
                return loadedClass;
            }
            catch (ClassNotFoundException e)
            {
                throw new ProxyGenerationException(e);
            }
        }
        catch (IllegalAccessException e)
        {
            throw new ProxyGenerationException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new ProxyGenerationException(e);
        }
    }


    protected boolean unproxyableMethod(Method delegatedMethod)
    {
        int modifiers = delegatedMethod.getModifiers();

        //X TODO how to deal with native functions?
        return (modifiers & (Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL | Modifier.NATIVE)) > 0 ||
               "finalize".equals(delegatedMethod.getName());
    }

    /**
     * @return the wrapper type for a primitive, e.g. java.lang.Integer for int
     */
    protected String getWrapperType(final Class<?> type)
    {
        if (Integer.TYPE.equals(type))
        {
            return Integer.class.getCanonicalName().replace('.', '/');
        }
        else if (Boolean.TYPE.equals(type))
        {
            return Boolean.class.getCanonicalName().replace('.', '/');
        }
        else if (Character.TYPE.equals(type))
        {
            return Character.class.getCanonicalName().replace('.', '/');
        }
        else if (Byte.TYPE.equals(type))
        {
            return Byte.class.getCanonicalName().replace('.', '/');
        }
        else if (Short.TYPE.equals(type))
        {
            return Short.class.getCanonicalName().replace('.', '/');
        }
        else if (Float.TYPE.equals(type))
        {
            return Float.class.getCanonicalName().replace('.', '/');
        }
        else if (Long.TYPE.equals(type))
        {
            return Long.class.getCanonicalName().replace('.', '/');
        }
        else if (Double.TYPE.equals(type))
        {
            return Double.class.getCanonicalName().replace('.', '/');
        }
        else if (Void.TYPE.equals(type))
        {
            return Void.class.getCanonicalName().replace('.', '/');
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    /**
     * Returns the appropriate bytecode instruction to load a value from a variable to the stack
     *
     * @param type Type to load
     * @return Bytecode instruction to use
     */
    protected int getVarInsn(final Class<?> type)
    {
        if (type.isPrimitive())
        {
            if (Integer.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Boolean.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Character.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Byte.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Short.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Float.TYPE.equals(type))
            {
                return Opcodes.FLOAD;
            }
            else if (Long.TYPE.equals(type))
            {
                return Opcodes.LLOAD;
            }
            else if (Double.TYPE.equals(type))
            {
                return Opcodes.DLOAD;
            }
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    /**
     * Invokes the most appropriate bytecode instruction to put a number on the stack
     *
     * @param mv
     * @param i
     */
    protected void pushIntOntoStack(final MethodVisitor mv, final int i)
    {
        if (i == 0)
        {
            mv.visitInsn(Opcodes.ICONST_0);
        }
        else if (i == 1)
        {
            mv.visitInsn(Opcodes.ICONST_1);
        }
        else if (i == 2)
        {
            mv.visitInsn(Opcodes.ICONST_2);
        }
        else if (i == 3)
        {
            mv.visitInsn(Opcodes.ICONST_3);
        }
        else if (i == 4)
        {
            mv.visitInsn(Opcodes.ICONST_4);
        }
        else if (i == 5)
        {
            mv.visitInsn(Opcodes.ICONST_5);
        }
        else if (i > 5 && i <= 255)
        {
            mv.visitIntInsn(Opcodes.BIPUSH, i);
        }
        else
        {
            mv.visitIntInsn(Opcodes.SIPUSH, i);
        }
    }

    /**
     * Gets the appropriate bytecode instruction for RETURN, according to what type we need to return
     *
     * @param type Type the needs to be returned
     * @return The matching bytecode instruction
     */
    protected int getReturnInsn(final Class<?> type)
    {
        if (type.isPrimitive())
        {
            if (Void.TYPE.equals(type))
            {
                return Opcodes.RETURN;
            }
            if (Integer.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Boolean.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Character.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Byte.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Short.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Float.TYPE.equals(type))
            {
                return Opcodes.FRETURN;
            }
            else if (Long.TYPE.equals(type))
            {
                return Opcodes.LRETURN;
            }
            else if (Double.TYPE.equals(type))
            {
                return Opcodes.DRETURN;
            }
        }

        return Opcodes.ARETURN;
    }

    /**
     * Gets the string to use for CHECKCAST instruction, returning the correct value for any type, including primitives and arrays
     *
     * @param returnType The type to cast to with CHECKCAST
     * @return CHECKCAST parameter
     */
    protected String getCastType(final Class<?> returnType)
    {
        if (returnType.isPrimitive())
        {
            return getWrapperType(returnType);
        }
        else
        {
            return Type.getInternalName(returnType);
        }
    }

    /**
     * Returns the name of the Java method to call to get the primitive value from an Object - e.g. intValue for java.lang.Integer
     *
     * @param type Type whose primitive method we want to lookup
     * @return The name of the method to use
     */
    protected String getPrimitiveMethod(final Class<?> type)
    {
        if (Integer.TYPE.equals(type))
        {
            return "intValue";
        }
        else if (Boolean.TYPE.equals(type))
        {
            return "booleanValue";
        }
        else if (Character.TYPE.equals(type))
        {
            return "charValue";
        }
        else if (Byte.TYPE.equals(type))
        {
            return "byteValue";
        }
        else if (Short.TYPE.equals(type))
        {
            return "shortValue";
        }
        else if (Float.TYPE.equals(type))
        {
            return "floatValue";
        }
        else if (Long.TYPE.equals(type))
        {
            return "longValue";
        }
        else if (Double.TYPE.equals(type))
        {
            return "doubleValue";
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    protected void generateReturn(MethodVisitor mv, Method delegatedMethod)
    {
        final Class<?> returnType = delegatedMethod.getReturnType();
        mv.visitInsn(getReturnInsn(returnType));
    }

    protected <T> T unsafeNewInstance(Class<T> clazz)
    {
        T instance = null;

        try
        {
            return (T) unsafeAllocateInstance.invoke(unsafe, clazz);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("Failed to allocateInstance of Proxy class " + clazz.getName(), e);
        }
        catch (InvocationTargetException e)
        {
            Throwable throwable = e.getTargetException() != null ? e.getTargetException() : e;
            throw new IllegalStateException("Failed to allocateInstance of Proxy class " + clazz.getName(),
                    throwable);
        }
    }


    private void initializeUnsafe()
    {
        final Class<?> unsafeClass;
        try
        {
            unsafeClass = AccessController.doPrivileged(new PrivilegedAction<Class<?>>()
            {
                public Class<?> run()
                {
                    try
                    {
                        return Thread.currentThread().getContextClassLoader().loadClass("sun.misc.Unsafe");
                    }
                    catch (Exception e)
                    {
                        try
                        {
                            return ClassLoader.getSystemClassLoader().loadClass("sun.misc.Unsafe");
                        }
                        catch (ClassNotFoundException e1)
                        {
                            throw new IllegalStateException("Cannot get sun.misc.Unsafe", e);
                        }
                    }
                }
            });
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Cannot get sun.misc.Unsafe class", e);
        }

        Object unsafe = AccessController.doPrivileged(new PrivilegedAction<Object>()
        {
            public Object run()
            {
                try
                {
                    Field field = unsafeClass.getDeclaredField("theUnsafe");
                    field.setAccessible(true);
                    return field.get(null);
                }
                catch (Exception e)
                {
                    throw new IllegalStateException("Cannot get sun.misc.Unsafe", e);
                }
            }
        });

        this.unsafe = unsafe;

        this.unsafeAllocateInstance = AccessController.doPrivileged(new PrivilegedAction<Method>()
        {
            public Method run()
            {
                try
                {
                    Method mtd = unsafeClass.getDeclaredMethod("allocateInstance", Class.class);
                    mtd.setAccessible(true);
                    return mtd;
                }
                catch (Exception e)
                {
                    throw new IllegalStateException("Cannot get sun.misc.Unsafe.allocateInstance", e);
                }
            }
        });
    }

}
