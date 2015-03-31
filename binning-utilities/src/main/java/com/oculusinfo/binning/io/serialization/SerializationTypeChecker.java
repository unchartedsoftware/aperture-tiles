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
package com.oculusinfo.binning.io.serialization;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
		if (   !expandedExpectedBinClass.equals(serializer.getBinTypeDescription())
			&& !serializerTypeCompatibleToNumber( serializer, expandedExpectedBinClass )) {
			throw new ConfigurationException("Serialization type does not match rendering type.  Serialization class was "+serializer.getBinTypeDescription()+", renderer type was "+expandedExpectedBinClass);
		}
		return (TileSerializer) serializer;
	}
	
	/**
	 * If the renderer is of Type java.lang.Number, this method will determine if the serializer type is compatible 
	 *  to Number type.  
	 * 
	 * @param serializer
	 * @param expandedExpectedBinClass
	 * @return boolean - true if serializer type is compatible with java.lang.Number
	 */
	private static <T> boolean serializerTypeCompatibleToNumber (TileSerializer<?> serializer, TypeDescriptor expandedExpectedBinClass) {
		boolean result = false;
		
		TypeDescriptor serializerBinTypeDescriptor = serializer.getBinTypeDescription();
		TypeDescriptor numberDescriptor = new TypeDescriptor(Number.class);
		TypeDescriptor listNumberDescriptor = new TypeDescriptor(List.class, numberDescriptor);
		
		// descriptors for all types that can be represented by java.lang.Number
		TypeDescriptor byteDescriptor 		= new TypeDescriptor(Byte.class);
		TypeDescriptor shortDescriptor 		= new TypeDescriptor(Short.class);
		TypeDescriptor integerDescriptor 	= new TypeDescriptor(Integer.class);
		TypeDescriptor longDescriptor 		= new TypeDescriptor(Long.class);
		TypeDescriptor atomicIntegerDescriptor = new TypeDescriptor(AtomicInteger.class);
		TypeDescriptor atomicLongDescriptor = new TypeDescriptor(AtomicLong.class);
		TypeDescriptor floatDescriptor 		= new TypeDescriptor(Float.class);
		TypeDescriptor doubleDescriptor 	= new TypeDescriptor(Double.class);
		TypeDescriptor bigIntegerDescriptor = new TypeDescriptor(BigInteger.class);
		TypeDescriptor bigDecimalDescriptor = new TypeDescriptor(BigDecimal.class);
		
		// if the renderer type is Number then we compare the serializer type to all types that can be represented by Nunber
		if ( expandedExpectedBinClass.equals(numberDescriptor) ) {		
			result =   serializerBinTypeDescriptor.equals(byteDescriptor)
					|| serializerBinTypeDescriptor.equals(shortDescriptor)
					|| serializerBinTypeDescriptor.equals(integerDescriptor)
					|| serializerBinTypeDescriptor.equals(longDescriptor)
					|| serializerBinTypeDescriptor.equals(atomicIntegerDescriptor)
					|| serializerBinTypeDescriptor.equals(atomicLongDescriptor)
					|| serializerBinTypeDescriptor.equals(floatDescriptor)
					|| serializerBinTypeDescriptor.equals(doubleDescriptor)
					|| serializerBinTypeDescriptor.equals(bigIntegerDescriptor)
					|| serializerBinTypeDescriptor.equals(bigDecimalDescriptor);
		} else if ( expandedExpectedBinClass.equals(listNumberDescriptor) ) {
			// descriptors for all types that can be represented by java.lang.Number in list
			TypeDescriptor byteListDescriptor 		= new TypeDescriptor(List.class, byteDescriptor);
			TypeDescriptor shortListDescriptor 		= new TypeDescriptor(List.class, shortDescriptor);
			TypeDescriptor integerListDescriptor 	= new TypeDescriptor(List.class, integerDescriptor);
			TypeDescriptor longListDescriptor 		= new TypeDescriptor(List.class, longDescriptor);
			TypeDescriptor atomicIntegerListDescriptor = new TypeDescriptor(List.class, atomicIntegerDescriptor);
			TypeDescriptor atomicLongListDescriptor = new TypeDescriptor(List.class, atomicLongDescriptor);
			TypeDescriptor floatListDescriptor 		= new TypeDescriptor(List.class, floatDescriptor);
			TypeDescriptor doubleListDescriptor 	= new TypeDescriptor(List.class, doubleDescriptor);
			TypeDescriptor bigIntegerListDescriptor = new TypeDescriptor(List.class, bigIntegerDescriptor);
			TypeDescriptor bigDecimalListDescriptor = new TypeDescriptor(List.class, bigDecimalDescriptor);
			
			result =   serializerBinTypeDescriptor.equals(byteListDescriptor)
					|| serializerBinTypeDescriptor.equals(shortListDescriptor)
					|| serializerBinTypeDescriptor.equals(integerListDescriptor)
					|| serializerBinTypeDescriptor.equals(longListDescriptor)
					|| serializerBinTypeDescriptor.equals(atomicIntegerListDescriptor)
					|| serializerBinTypeDescriptor.equals(atomicLongListDescriptor)
					|| serializerBinTypeDescriptor.equals(floatListDescriptor)
					|| serializerBinTypeDescriptor.equals(doubleListDescriptor)
					|| serializerBinTypeDescriptor.equals(bigIntegerListDescriptor)
					|| serializerBinTypeDescriptor.equals(bigDecimalListDescriptor);
		}
		
		return result;
	}
}
