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
package com.oculusinfo.tile.rendering.color;

/**
 * Transforms a value into a colour.
 * Each colour ramp will transform a 0-1 scale value to its
 * own domain.
 *
 * @author cregnier
 */
public interface ColorRamp {

	/**
	 * Transform the scale value into a colour.
	 * @param scale
	 * 	A value between 0 - 1.
	 * @return
	 * 	The rgb colour value.
	 */
	int getRGB(double scale);

	/**
	 * Transform the scale and alpha values into a colour
	 * @param scale
	 * 	A value between 0 - 1.
	 * @param alpha
	 *  A value between 0 - 1.
	 * @return
	 * 	The rgb colour value.
	 */
	int getRGBA(double scale, double alpha);
}
