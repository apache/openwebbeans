/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;

@SuppressWarnings("unchecked")
public class InjectableMethods<T> extends AbstractInjectable
{
	/** Injectable method */
	private Method m;

	/** Component instance that owns the method */
	private Object instance;

	/**
	 * Constructs new instance.
	 * 
	 * @param m
	 *            injectable method
	 * @param instance
	 *            component instance
	 */
	public InjectableMethods(Method m, Object instance, AbstractComponent<?> owner)
	{
		super(owner);
		this.m = m;
		this.instance = instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.webbeans.inject.Injectable#doInjection()
	 */
	public T doInjection()
	{
		Type[] types = m.getGenericParameterTypes();
		Annotation[][] annots = m.getParameterAnnotations();
		List<Object> list = new ArrayList<Object>();
		if (types.length > 0)
		{
			int i = 0;
			for (Type type : types)
			{
				Annotation[] annot = annots[i];
				if (annot.length == 0)
				{
					annot = new Annotation[1];
					annot[0] = new CurrentLiteral();
				}

				Type[] args = new Type[0];
				Class<?> clazz = null;
				if (type instanceof ParameterizedType)
				{
					ParameterizedType pt = (ParameterizedType) type;
					
					checkParametrizedTypeForInjectionPoint(pt);
					args = new Type[1];
					args = pt.getActualTypeArguments();

					clazz = (Class<?>) pt.getRawType();
				} else
				{
					clazz = (Class<?>) type;
				}

				list.add(inject(clazz, args, AnnotationUtil.getBindingAnnotations(annot)));

				i++;

			}

		}

		try
		{
			if (!ClassUtil.isPublic(m.getModifiers()))
			{
				m.setAccessible(true);
			}

			return (T) m.invoke(instance, list.toArray());

		} catch (Throwable e)
		{
			// no-op
			e.printStackTrace();
		}
		return null;
	}

}