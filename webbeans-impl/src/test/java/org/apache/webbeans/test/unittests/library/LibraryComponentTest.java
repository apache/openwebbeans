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
package org.apache.webbeans.test.unittests.library;

import junit.framework.Assert;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.test.component.library.Book;
import org.apache.webbeans.test.component.library.BookShop;
import org.apache.webbeans.test.component.library.Shop;
import org.junit.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

public class LibraryComponentTest extends AbstractUnitTest
{
    @Test
    public void testTypedComponent() throws Throwable
    {
        startContainer(BookShop.class, BeanHolder.class);

        BeanHolder beanHolder = getInstance(BeanHolder.class);
        Assert.assertEquals("shop", beanHolder.getBookShop().shop());
    }


    @Dependent
    public static class BeanHolder
    {
        private @Inject Shop<Book> bookShop;

        public Shop<Book> getBookShop() {
            return bookShop;
        }
    }

}
