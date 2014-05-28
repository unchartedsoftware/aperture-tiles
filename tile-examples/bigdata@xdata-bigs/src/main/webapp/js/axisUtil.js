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
define( function(require) {
    "use strict"
	
	return {
	    getMarkerPositions :  function (zoomLevel, min, max, intervals){
	        var _decimalPlace = 2;
	
	        function fillArrayByIncrement(start, end, increment){
	            var markerData = [];
	
	            var count = 0;
	            for( var i=start, l=end; i<=l;) {
	                var value = i;
	                if (i % 1 != 0){
	                    value = parseFloat(Math.round(i * 100) / 100).toFixed(_decimalPlace);
	                }
	                markerData[count] = value;
	                i = i + increment;
	                count++;
	            }
	
	            return markerData;
	        }
	
	        var markerData;
	
	        var baseIncrement = (max-min)/intervals;
	        var increment = baseIncrement/Math.pow(2,Math.max(zoomLevel-1,0));
	        markerData = fillArrayByIncrement(min, max, increment);
	
	        return markerData;
	    }
	};
});