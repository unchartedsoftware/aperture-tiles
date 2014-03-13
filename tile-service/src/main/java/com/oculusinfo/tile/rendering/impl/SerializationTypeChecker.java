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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rendering.impl;

import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurationException;

public class SerializationTypeChecker {
    /**
     * All this method does is check the return type of a serializer
     * programatically, As such, it supercedes the warnings hidden here.
     * 
     * It will only work, of course, if the serializer is set up correctly
     * (i.e., without lying about its type_, and if the pass-ed class and
     * expandedClass actually match; mis-use will, of course, cause errors.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> TileSerializer<T> checkBinClass (TileSerializer<?> serializer, Class<T> expectedBinClass, TypeDescriptor expandedExpectedBinClass) throws ConfigurationException {
        if (null == serializer) {
            throw new ConfigurationException("No serializer given for renderer");
        }
        if (!expandedExpectedBinClass.equals(serializer.getBinTypeDescription())) {
            throw new ConfigurationException("Serialization type does not match rendering type.  Serialization class was "+serializer.getBinTypeDescription()+", renderer type was "+expandedExpectedBinClass);
        }
        return (TileSerializer) serializer;
    }

}
