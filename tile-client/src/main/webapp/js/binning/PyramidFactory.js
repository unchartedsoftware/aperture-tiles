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


define(function (require) {
    "use strict";

    var AoIPyramid = require('./AoITilePyramid'),
        WebMercatorPyramid = require('./WebTilePyramid');

    return {
        createPyramid: function (specification) {
	        var pyramid;
	        if ("AreaOfInterest" === specification.type) {
		        pyramid = new AoIPyramid(specification.minX,
		                                 specification.minY,
		                                 specification.maxX,
		                                 specification.maxY);
	        } else if ("WebMercator" === specification.type) {
		        pyramid = new WebMercatorPyramid();
	        }
	        return pyramid;
        },
		// Check if two pyramid specs represent the same pyramid
		pyramidsEqual: function (pyramidA, pyramidB) {
			if (pyramidA && pyramidA.ClassName) {
				pyramidA = pyramidA.toJSON();
			}
			if (pyramidB && pyramidB.ClassName) {
				pyramidB = pyramidB.toJSON();
			}
			var result = false;
			if (pyramidA && pyramidA.type && pyramidB && pyramidB.type) {
				if ("AreaOfInterest" === pyramidA.type) {
					result = ("AreaOfInterest" === pyramidB.type &&
							  pyramidA.minX === pyramidB.minX &&
							  pyramidA.maxX === pyramidB.maxX &&
							  pyramidA.minY === pyramidB.minY &&
							  pyramidA.maxY === pyramidB.maxY);
				} else if ("WebMercator" === pyramidA.type) {
					result = ("WebMercator" === pyramidB.type);
				}
			}

			return result;
		}
    };
});
