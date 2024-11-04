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

// original header
/*
 * Copyright 2015 Higher Frequency Trading http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.hash;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

// forked from @OpenHFT/Zero-Allocation-Hashing/blob/master/src/main/java/net/openhft/hashing/XxHash.java
// (ASFv2 license)
public final class XxHash64
{
    private static final long PRIME64_1 = 0x9E3779B185EBCA87L;
    private static final long PRIME64_2 = 0xC2B2AE3D27D4EB4FL;
    private static final long PRIME64_3 = 0x165667B19E3779F9L;
    private static final long PRIME64_4 = 0x85EBCA77C2b2AE63L;
    private static final long PRIME64_5 = 0x27D4EB2F165667C5L;

    private XxHash64()
    {
        // no-op
    }

    public static long apply(final String input)
    {
        return apply(ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8)));
    }

    public static long apply(final ByteBuffer input)
    {
        int length = input.remaining();
        long remaining = length;

        long hash;
        int off = 0;
        if (remaining >= 32)
        {
            long v1 = PRIME64_1 + PRIME64_2;
            long v2 = PRIME64_2;
            long v3 = 0;
            long v4 = -PRIME64_1;

            do
            {
                v1 = processChunk(input, off, v1);
                v2 = processChunk(input, off + 8, v2);
                v3 = processChunk(input, off + 16, v3);
                v4 = processChunk(input, off + 24, v4);

                off += 32;
                remaining -= 32;
            }
            while (remaining >= 32);

            hash = Long.rotateLeft(v1, 1)
                    + Long.rotateLeft(v2, 7)
                    + Long.rotateLeft(v3, 12)
                    + Long.rotateLeft(v4, 18);

            hash = finalizeHash(hash, v1);
            hash = finalizeHash(hash, v2);
            hash = finalizeHash(hash, v3);
            hash = finalizeHash(hash, v4);
        }
        else
        {
            hash = PRIME64_5;
        }

        hash += length;

        while (remaining >= 8)
        {
            hash = processRemaining(input, off, hash, 8);
            off += 8;
            remaining -= 8;
        }

        if (remaining >= 4)
        {
            hash ^= (input.getInt(off) & 0xFFFFFFFFL) * PRIME64_1;
            hash = Long.rotateLeft(hash, 23) * PRIME64_2 + PRIME64_3;
            off += 4;
            remaining -= 4;
        }

        while (remaining != 0)
        {
            hash ^= (input.get(off) & 0xFF) * PRIME64_5;
            hash = Long.rotateLeft(hash, 11) * PRIME64_1;
            --remaining;
            ++off;
        }

        return finalize(hash);
    }

    private static long processChunk(ByteBuffer input, int offset, long value)
    {
        value += input.getLong(offset) * PRIME64_2;
        value = Long.rotateLeft(value, 31);
        value *= PRIME64_1;
        return value;
    }

    private static long finalizeHash(long hash, long value)
    {
        value *= PRIME64_2;
        value = Long.rotateLeft(value, 31);
        value *= PRIME64_1;
        hash ^= value;
        return hash * PRIME64_1 + PRIME64_4;
    }

    private static long processRemaining(ByteBuffer input, int offset, long hash, int length)
    {
        long k1 = input.getLong(offset);
        k1 *= PRIME64_2;
        k1 = Long.rotateLeft(k1, 31);
        k1 *= PRIME64_1;
        hash ^= k1;
        return Long.rotateLeft(hash, 27) * PRIME64_1 + PRIME64_4;
    }

    private static long finalize(long hash)
    {
        hash ^= hash >>> 33;
        hash *= PRIME64_2;
        hash ^= hash >>> 29;
        hash *= PRIME64_3;
        hash ^= hash >>> 32;
        return hash;
    }
}