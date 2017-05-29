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
package org.apache.webbeans.test.tck;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StreamCorruptedException;

import org.junit.Test;

/**
 * Unit tests for class {@link BeansImpl org.apache.webbeans.test.tck.BeansImpl}.
 *
 * @author Michael Hausegger, hausegger.michael@googlemail.com
 * @date 29.05.2017
 * @see BeansImpl
 **/
public class BeansImplTest {


	@Test
	public void testIsProxy() {

		BeansImpl beansImpl = new BeansImpl();
		Object object = new Object();
		boolean booleanOne = beansImpl.isProxy(object);

		assertFalse(booleanOne);
	}


	@Test
	public void testPassivate() throws IOException {

		BeansImpl beansImpl = new BeansImpl();
		byte[] byteArray = beansImpl.passivate("");

		assertArrayEquals(new byte[]{(byte) (-84), (byte) (-19), (byte) 0, (byte) 5, (byte) 116, (byte) 0, (byte) 0}, byteArray);
	}


	@Test(expected = StreamCorruptedException.class)
	public void testActivateThrowsStreamCorruptedException() throws IOException, ClassNotFoundException {

		BeansImpl beansImpl = new BeansImpl();
		byte[] byteArray = new byte[8];

		beansImpl.activate(byteArray);
	}
}