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
package org.apache.webbeans.web.util;

import static org.junit.Assert.*;

import org.apache.webbeans.web.lifecycle.test.MockServletContext;
import org.junit.Test;

/**
 * Unit tests for class {@link ServletCompatibilityUtil org.apache.webbeans.web.util.ServletCompatibilityUtil}.
 *
 * @author Michael Hausegger, hausegger.michael@googlemail.com
 * @date 29.05.2017
 * @see ServletCompatibilityUtil
 **/
public class ServletCompatibilityUtilTest {


	@Test
	public void testGetServletInfoReturningNullOne() {

		MockServletContext mockServletContext = new MockServletContext();
		String result = ServletCompatibilityUtil.getServletInfo(mockServletContext);

		assertNull(result);
	}


	@Test
	public void testGetServletInfoReturningContextPathOne() {

		MockServletContext mockServletContext = new MockServletContext() {

			@Override
			public int getMajorVersion() {
				return 4;
			}

			@Override
			public String getContextPath() {
				return "a";
			}
		};
		String result = ServletCompatibilityUtil.getServletInfo(mockServletContext);

		assertEquals("a", result);
	}


	@Test
	public void testGetServletInfoReturningNullTwo() {

		MockServletContext mockServletContext = new MockServletContext() {

			@Override
			public int getMajorVersion() {
				return 2;
			}

			@Override
			public String getServletContextName() {
				return "context";
			}
		};
		String result = ServletCompatibilityUtil.getServletInfo(mockServletContext);

		assertEquals("context", result);
	}


	@Test
	public void testGetServletInfoReturningContextPathTwo() {

		MockServletContext mockServletContext = new MockServletContext() {

			@Override
			public int getMajorVersion() {
				return 2;
			}

			@Override
			public int getMinorVersion() {
				return 5;
			}

			@Override
			public String getContextPath() {
				return "a";
			}
		};
		String result = ServletCompatibilityUtil.getServletInfo(mockServletContext);

		assertEquals("a", result);
	}


	@Test
	public void testGetServletInfoReturningContextPathThree() {

		MockServletContext mockServletContext = new MockServletContext() {

			@Override
			public int getMinorVersion() {
				return 10;
			}

			@Override
			public String getContextPath() {
				return "a";
			}

			@Override
			public String getServletContextName() {
				return "context";
			}
		};
		String result = ServletCompatibilityUtil.getServletInfo(mockServletContext);

		assertEquals("context", result);
	}


	@Test
	public void testGetServletInfoReturningNonEmptyString() {

		String string = ServletCompatibilityUtil.getServletInfo(null);

		assertEquals("null", string);
	}
}