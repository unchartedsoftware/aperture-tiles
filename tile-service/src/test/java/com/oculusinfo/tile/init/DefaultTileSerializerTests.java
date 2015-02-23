/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.init;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.io.serialization.DefaultTileSerializerFactoryProvider;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;

public class DefaultTileSerializerTests {
    @Test
    public void testDefaultFactoryProviders () throws ConfigurationException {
        DefaultTileSerializerFactoryProvider[] defaults = DefaultTileSerializerFactoryProvider.values();
        List<TypeDescriptor> typeDescriptors = new ArrayList<>();
        for (DefaultTileSerializerFactoryProvider provider: defaults) {
            ConfigurableFactory<? extends TileSerializer<?>> factory = provider.createFactory(null);
            factory.readConfiguration(new JSONObject());
            typeDescriptors.add(factory.produce(TileSerializer.class).getBinTypeDescription());
        }

        TypeDescriptor tdInt = new TypeDescriptor(Integer.class);
        TypeDescriptor tdLng = new TypeDescriptor(Long.class);
        TypeDescriptor tdFlt = new TypeDescriptor(Float.class);
        TypeDescriptor tdDbl = new TypeDescriptor(Double.class);
        TypeDescriptor tdBoo = new TypeDescriptor(Boolean.class);
        TypeDescriptor tdByB = new TypeDescriptor(ByteBuffer.class);
        TypeDescriptor tdStr = new TypeDescriptor(String.class);
        Assert.assertTrue(typeDescriptors.contains(tdInt));
        Assert.assertTrue(typeDescriptors.contains(tdLng));
        Assert.assertTrue(typeDescriptors.contains(tdFlt));
        Assert.assertTrue(typeDescriptors.contains(tdDbl));
        Assert.assertTrue(typeDescriptors.contains(tdBoo));
        Assert.assertTrue(typeDescriptors.contains(tdByB));
        Assert.assertTrue(typeDescriptors.contains(tdStr));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, tdInt)));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, tdLng)));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, tdFlt)));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, tdDbl)));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, tdBoo)));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, tdByB)));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, tdStr)));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdInt, tdInt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdInt, tdLng))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdInt, tdFlt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdInt, tdDbl))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdInt, tdBoo))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdInt, tdByB))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdInt, tdStr))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdLng, tdInt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdLng, tdLng))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdLng, tdFlt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdLng, tdDbl))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdLng, tdBoo))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdLng, tdByB))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdLng, tdStr))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdFlt, tdInt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdFlt, tdLng))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdFlt, tdFlt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdFlt, tdDbl))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdFlt, tdBoo))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdFlt, tdByB))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdFlt, tdStr))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdDbl, tdInt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdDbl, tdLng))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdDbl, tdFlt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdDbl, tdDbl))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdDbl, tdBoo))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdDbl, tdByB))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdDbl, tdStr))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdBoo, tdInt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdBoo, tdLng))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdBoo, tdFlt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdBoo, tdDbl))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdBoo, tdBoo))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdBoo, tdByB))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdBoo, tdStr))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdByB, tdInt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdByB, tdLng))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdByB, tdFlt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdByB, tdDbl))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdByB, tdBoo))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdByB, tdByB))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdByB, tdStr))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdStr, tdInt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdStr, tdLng))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdStr, tdFlt))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdStr, tdDbl))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdStr, tdBoo))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdStr, tdByB))));
        Assert.assertTrue(typeDescriptors.contains(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, tdStr, tdStr))));
    }
}
