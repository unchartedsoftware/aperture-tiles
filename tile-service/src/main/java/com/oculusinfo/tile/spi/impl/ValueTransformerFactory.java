/**
 * Copyright (c) 2013 Oculus Info Inc.
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
package com.oculusinfo.tile.spi.impl;

import com.oculusinfo.tile.util.TransformParameter;

public class ValueTransformerFactory {

	public static IValueTransformer create(TransformParameter transform, double levelMin, double levelMax) {
		IValueTransformer t;
		String name = transform.getName();
		
		if(name.equalsIgnoreCase("log10")){ 
			t = new Log10ValueTransformer(levelMax);
		}else if (name.equalsIgnoreCase("minmax")) {
			double min = transform.getDoubleOrElse("min", levelMin);
			double max = transform.getDoubleOrElse("max", levelMax);
			t = new LinearCappedValueTransformer(min, max);
		}else { //if 'linear'
			t = new LinearCappedValueTransformer(levelMin, levelMax);
		}
		return t;
	}

}
