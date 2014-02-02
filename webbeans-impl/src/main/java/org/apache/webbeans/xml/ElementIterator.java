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
package org.apache.webbeans.xml;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 */
public class ElementIterator implements Iterator<Element>
{
    private final Element parent;
    private final NodeList children;
    private int currentPosition = 0;

    public ElementIterator(Element parent)
    {
        this.parent = parent;
        children = parent.getChildNodes();
    }

    @Override
    public boolean hasNext()
    {
        if (children == null || children.getLength() < currentPosition)
        {
            return false;
        }

        do
        {
            Node nd = children.item(currentPosition);
            if (nd instanceof Element)
            {
                return true;
            }

            currentPosition++;
        } while (currentPosition < children.getLength());

        return false;
    }

    @Override
    public Element next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException("The Element does not have more children");
        }

        return (Element) children.item(currentPosition++);
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("remove is not supported with this DOM Element iterator.");
    }
}
