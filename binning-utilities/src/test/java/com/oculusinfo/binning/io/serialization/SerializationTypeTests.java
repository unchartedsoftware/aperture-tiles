/*
 * Copyright (c) 2014 Oculus Info Inc. 
 * http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.io.serialization;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.io.serialization.impl.DoubleArrayAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringIntPairArrayAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.binning.util.Pair;

// Test that serializers can check their bin types somehow
public class SerializationTypeTests {
    @Test
    public void testSerializerTypeing () {
        Assert.assertEquals(new TypeDescriptor(Double.class),
                            new DoubleAvroSerializer(null).getBinTypeDescription());
        Assert.assertEquals(new TypeDescriptor(List.class,
                                                       new TypeDescriptor(Double.class)),
                            new DoubleArrayAvroSerializer(null).getBinTypeDescription());
        Assert.assertEquals(new TypeDescriptor(List.class,
                                                       new TypeDescriptor(Pair.class,
                                                                                  new TypeDescriptor(String.class),
                                                                                  new TypeDescriptor(Integer.class))),
                            new StringIntPairArrayAvroSerializer(null).getBinTypeDescription());
    }
}
