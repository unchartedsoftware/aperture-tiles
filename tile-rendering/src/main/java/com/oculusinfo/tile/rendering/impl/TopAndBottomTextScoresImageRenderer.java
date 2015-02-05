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

import com.oculusinfo.factory.util.Pair;

import java.util.List;

/**
 * A server side to render Map<String, Double> (well, technically,
 * List<Pair<String, Double>>) tiles.
 * 
 * This renderer modifies {@link TopTextScoresImageRenderer} to render the top
 * and bottom few scores, up to 5 of each.
 * 
 * @author nkronenfeld
 */
public class TopAndBottomTextScoresImageRenderer extends TopTextScoresImageRenderer {
	@Override
	protected int[] getTextsToDraw(List<Pair<String, Double>> cellData) {
		int n = cellData.size();
		int t = Math.min(10, n);
		int[] result = new int[t];

		if (n < 10) {
			for (int i=0; i<t; ++i) {
				result[t-1-i] = i;
			}
		} else {
			for (int i=0; i<5; ++i) {
				result[9-i] = i;
				result[i] = n-1-i;
			}
		}

		return result;
	}
}
