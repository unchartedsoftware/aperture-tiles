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

define({

    /**
     * Converts a data value from a linear function to its equivolent gudermannian value
     * This converts the linear number to the equivolent gudermannian value where the current
     * linear value is!
     *
     * @param value     data value on between axis.min and axis.max
     * @param axis  axis object
     */
    scaleLinearToGudermannian: function(value, axis) {
        "use strict";
        var temp,
            gudermannian = function(y) {
                // converts a y value from -PI(bottom) to PI(top) into the
                // mercator projection latitude
                var sinh = function (arg) {
                    return (Math.exp(arg) - Math.exp(-arg)) / 2.0;
                };
                return Math.atan(sinh(y)) * (180.0/Math.PI);
            };

        // convert value to between -1  and 1
        temp = ((( value - axis.min ) / (axis.max-axis.min) ) * 2) - 1;
        // convert to -PI to PI
        temp = gudermannian( temp * Math.PI );
        // convert mercator latitude from -85.05 to 85.05 back to data range
        return ( (((temp / 85.05)+1)/2) * (axis.max - axis.min)) + axis.min;
    },

    /**
     * Converts a data value from a gudermannian function to a linear function
     * This converts the gudermannian number to the equivolent linear value where the current
     * gudermannian value is!
     *
     * @param value data value on between axis.min and axis.max
     * @param axis  axis object
     */
    scaleGudermannianToLinear: function(value, axis) {
        "use strict";
        var temp,
            gudermannianInv = function( latitude ) {
                // converts a latitude value from -85.05 to 85.05 into
                // a y value from -PI(bottom) to PI(top)
                var sign = ( latitude !== 0 ) ? latitude / Math.abs(latitude) : 0,
                    sin = Math.sin(latitude * (Math.PI/180.0) * sign);

                return sign * (Math.log((1.0 + sin) / (1.0 - sin)) / 2.0);
            };
        // convert from linear latitude value to mercator projected value
        // convert y value from -PI to PI to -1 to 1
        temp = (gudermannianInv( (((( value - axis.min ) / (axis.max-axis.min) ) * 2) - 1)*85.05 ) / Math.PI);
        // convert value to proper axis data range
        return ( ((temp+1)/2) * (axis.max - axis.min)) + axis.min;
    },

    /**
     * Formats axis marker label text
     *
     * @param value         the value of the label
     * @param labelSpec     format spec of label
     */
    formatText : function(value, labelSpec){
        "use strict";
        function formatNumber (value, decimals) {
            return parseFloat(value).toFixed(decimals);
        }

        function formatThousand (value, decimals, allowStepDown) {
            var truncValue = value/1e3,
                numberStr;

            if (decimals === null || decimals === 0){
                if (truncValue % 1 === 0){
                    return truncValue + 'K';
                }
                numberStr = truncValue.toString();
                return numberStr.substring(0,numberStr.indexOf('.')) + 'K';
            }
            numberStr = parseFloat(Math.round(truncValue * 100) / 100).toFixed(decimals);
            if (allowStepDown && Math.abs(Number(numberStr)) < 1 && value !== 0){
                return formatNumber(value, decimals );
            }
            return numberStr + 'K';
        }

        function formatMillion(value, decimals, allowStepDown) {
            var truncValue = value/1e6,
                numberStr;
            if (decimals === null ||decimals === 0){
                if (truncValue % 1 === 0){
                    return truncValue + 'M';
                }
                numberStr = truncValue.toString();
                if (allowStepDown && Math.abs(Number(numberStr)) < 1 && value !== 0){
                    return formatThousand(value, 0, allowStepDown);
                }
                return numberStr.substring(0,numberStr.indexOf('.')) + 'M';
            }

            numberStr = parseFloat(Math.round(truncValue * 100) / 100).toFixed(decimals);
            if (allowStepDown && Math.abs(Number(numberStr)) < 1 && value !== 0){
                return formatThousand(value, decimals, allowStepDown);
            }
            return numberStr + 'M';
        }

        function formatBillion(value, decimals, allowStepDown) {
            var truncValue = value/1e9,
                numberStr;

            if (decimals === null ||decimals === 0){
                if (truncValue % 1 === 0){
                    return truncValue + 'B';
                }
                numberStr = truncValue.toString();
                if (allowStepDown && Math.abs(Number(numberStr)) < 1 && value !== 0){
                    return formatMillion(value, 0, allowStepDown);
                }
                return numberStr.substring(0,numberStr.indexOf('.')) + 'B';
            }

            numberStr = parseFloat(Math.round(truncValue * 100) / 100).toFixed(decimals);
            if (allowStepDown && Math.abs(Number(numberStr)) < 1 && value !== 0){
                return formatMillion(value, decimals, allowStepDown);
            }
            return numberStr + 'B';

        }

        function formatTime(value, divisor){
            var d = new Date(0); // The 0 there is the key, which sets the date to the epoch

            if (!divisor) {
                divisor = 1000;
            }
            d.setUTCSeconds(value/divisor); // Assume default of milliseconds
            return (d.getMonth()+1) + '/' + d.getDate() + '/' + (d.getFullYear());
        }

        function formatDegrees(value, decimals){
            return parseFloat(value.toFixed(decimals)) + "\u00b0";
        }

        if (labelSpec){

            if (labelSpec.type === 'degrees'){
                return formatDegrees(value, labelSpec.decimals );
            }
            if (labelSpec.type === 'time'){
                return formatTime(value, labelSpec.divisor);
            }
            // Millions to 2 decimal places
            if (labelSpec.type === 'decimal'){
                return formatNumber(value, labelSpec.decimals);
            }
            if (labelSpec.type === 'B'){
                return formatBillion(value, labelSpec.decimals, labelSpec.allowStepDown);
            }
            if (labelSpec.type === 'M'){
                return formatMillion(value, labelSpec.decimals, labelSpec.allowStepDown);
            }
            if (labelSpec.type === 'K'){
                return formatThousand(value, labelSpec.decimals, labelSpec.allowStepDown);
            }
        }
        return value;
    },

    /**
     * Given a value that is outside of the min and max of axis,
     * ensure the values rollover properly
     *
     * @param axis      axis object
     * @param value     original value
     */
    getMarkerRollover: function(axis, value) {
        "use strict";
        var rollover;
        if (axis.repeat) {
            // if repeat enabled ensure label value wraps past min/max properly
            if (value > axis.max) {
                rollover = value - axis.max + axis.min;
            } else if (value < axis.min) {
                rollover = value + axis.max - axis.min;
            } else {
                rollover = value;
            }
        } else {
            // non-repeat label is always value as there is no wrap around
            rollover = value;
        }
        return rollover;
    },

    /**
     * Generates all visible marker values, returns array of objects, containing
     * labels and pixel locations
     *
     * @param axis      axis object
     */
    getMarkers : function(axis) {
        "use strict";

        // generates all increments between min and max using specified interval
        // number and zoom level
        var that = this,
            increment,
            pivot,
            mapPixelSpan = axis.tileSize*(Math.pow(2, axis.zoom));

        function getPixelPosition( value ) {
            // given an axis value, get the pixel position on the page
            var pixelPosition;

            if (axis.isXAxis) {
                pixelPosition = Math.round( (( (value - axis.min)*mapPixelSpan )/(axis.max-axis.min) ) - axis.pixelMin );
            } else {

                if (axis.intervalSpec.isMercatorProjected) {
                    // find the linear value where this current gudermannian value is
                    value = that.scaleGudermannianToLinear( value, axis);
                }

                pixelPosition = Math.round( (( (value - axis.min)*mapPixelSpan )/(axis.max-axis.min) ) + axis.pixelMax - mapPixelSpan );
            }
            return pixelPosition;
        }

        function getMinIncrement() {

            var minCull,      // exact value of cull point, any value less will be culled from view
                minIncrement; // the minimum increment that is visible

            if (axis.isXAxis) {
                minCull = ( ( axis.pixelMin * (axis.max-axis.min) ) / mapPixelSpan ) + axis.min;
            } else {
                minCull = ( ( ( mapPixelSpan - axis.pixelMax ) * (axis.max-axis.min) ) / mapPixelSpan ) + axis.min;
            }
            if ( !axis.repeat && minCull < axis.min ) {
                // prevent roll-over
                minCull = axis.min;
            }

            if (!axis.isXAxis && axis.intervalSpec.isMercatorProjected) {
                // find the gudermannian value where this current linear value is
                minCull = that.scaleLinearToGudermannian( minCull, axis );
            }

            minIncrement = pivot;

            if (pivot < minCull) {
                // cull above pivot
                while (minIncrement < minCull) {
                    minIncrement += increment;
                }
            } else {
                // cull below pivot
                while (minIncrement-increment >= minCull) {
                    minIncrement -= increment;
                }
            }

            return minIncrement;
        }

        function getMaxIncrement() {

            var maxCull,      // exact value of cull point, any value greater will be culled from view
                maxIncrement; // the minimum increment that is visible

            if (axis.isXAxis) {
                maxCull = ( ( ( parseInt( axis.axisLength, 10 ) + axis.pixelMin ) * (axis.max-axis.min) ) / mapPixelSpan ) + axis.min;
            } else {
                maxCull = ( ( ( parseInt( axis.axisLength, 10 ) + mapPixelSpan - axis.pixelMax ) * (axis.max-axis.min) ) / mapPixelSpan ) + axis.min;

            }
            if ( !axis.repeat && maxCull > axis.max ) {
                // prevent roll-over
                maxCull = axis.max;
            }

            if (!axis.isXAxis && axis.intervalSpec.isMercatorProjected) {
                // find the gudermannian value where this current linear value is
                maxCull = that.scaleLinearToGudermannian( maxCull, axis );
            }

            maxIncrement = pivot;

            if (pivot > maxCull) {
                // cull below pivot
                while (maxIncrement > maxCull) {
                    maxIncrement -= increment;
                }
            } else {
                // cull above pivot
                while (maxIncrement+increment <= maxCull) {
                    maxIncrement += increment;
                }
            }

            return maxIncrement;
        }

        function fillArrayByIncrement(start, end) {

            var markers = [],
                i,
                rawValue,// raw value along axis, used for pixel position
                roundedValue;   // value rounded to n decimals

            for (i = start; i <= end; i+=increment) {
                // differentiate between the raw and rounded value, calculate pixel position
                // from raw value or else marks are noticeably non-uniform at high zoom levels
                rawValue = i;
                roundedValue = i;

                if (i % 1 !== 0) {
                    // round to proper decimal place, toFixed converts to string, so convert back
                    roundedValue = parseFloat( (Math.round(i * 100) / 100).toFixed(axis.unitSpec.decimals) );
                }

                markers.push({
                    label : that.getMarkerRollover(axis, roundedValue),
                    pixel : getPixelPosition(rawValue)
                });
            }

            return markers;
        }

        // get increment and pivot value from axis
        if ( axis.intervalSpec.type === "fixed" ) {
            // use fixed interval
            increment = axis.intervalSpec.value;
            pivot = axis.intervalSpec.pivot;
        } else {
            // use percentage
            increment = (axis.max-axis.min)*(axis.intervalSpec.value * 0.01);
            pivot = (axis.max-axis.min)*(axis.intervalSpec.pivot*0.01) + axis.min;
        }
        // scale increment if specified
        if ( axis.intervalSpec.allowScaleByZoom ) {
            // scale increment by zoom
            increment = increment/Math.pow(2,Math.max(axis.zoom-1,0));
        }

        // add all points between minimum visible value and maximum visible value
        return fillArrayByIncrement( getMinIncrement(), getMaxIncrement() );
    }
});
