/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.portable.events.extensions;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

public class ParameterizedTypeWithTypeVariableExtension implements Extension
{

    public static boolean CALLED = false;

    <T extends PaintToolFactory<?>> void processClasses(@Observes ProcessAnnotatedType<T> event)
    {
        CALLED = true;
    }

    public static class PaintToolFactoryImpl implements PaintToolFactory<PaintBrush<Red>>
    {
        @Override
        public PaintBrush<Red> createPaintTool()
        {
            return new PaintBrush<Red>();
        }
    }

    public static class PaintBrush<T extends Color> implements PaintTool<T>
    {
        @Override
        public void paint(T color)
        {
            // no-op
        }
    }

    public interface PaintTool<T extends Color>
    {
        void paint(T color);
    }

    public interface PaintToolFactory<T extends PaintTool<?>>
    {
        T createPaintTool();
    }

    public interface Color
    {
        void getColor();
    }

    public static class Red implements Color {

        @Override
        public void getColor() {

        }
    }

    public static class Green implements Color {

        @Override
        public void getColor() {

        }
    }
}
