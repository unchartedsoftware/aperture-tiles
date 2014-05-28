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
	
	var AxisUtil = require('./axisUtil');
	
	return {
		
	    createAxis : function(spec){
	        function formatNumber(value, decimals){
	            return parseFloat(value).toFixed(decimals);
	        }
	        function formatBillion(value, decimals, allowStepDown){
	            var truncValue = value/1e9;
	            var numberStr;
	            if (decimals == null ||decimals == 0){
	                if (truncValue % 1 == 0){
	                    return truncValue + 'B';
	                }
	                numberStr = truncValue.toString();
	                if (allowStepDown && Math.abs(numberStr) < 1 && value != 0){
	                    return formatMillion(value, 0, allowStepDown);
	                }
	                return numberStr.substring(0,numberStr.indexOf('.')) + 'B';
	            }
	            else {
	                numberStr = parseFloat(Math.round(truncValue * 100) / 100).toFixed(decimals);
	                if (allowStepDown && Math.abs(numberStr) < 1 && value != 0){
	                    return formatMillion(value, decimals, allowStepDown);
	                }
	                return numberStr + 'B';
	            }
	        }
	        function formatMillion (value, decimals, allowStepDown){
	            var truncValue = value/1e6;
	            var numberStr;
	            if (decimals == null ||decimals == 0){
	                if (truncValue % 1 == 0){
	                    return truncValue + 'M';
	                }
	                numberStr = truncValue.toString();
	                if (allowStepDown && Math.abs(numberStr) < 1 && value != 0){
	                    return formatThousand(value, 0, allowStepDown);
	                }
	                return numberStr.substring(0,numberStr.indexOf('.')) + 'M';
	            }
	            else {
	                numberStr = parseFloat(Math.round(truncValue * 100) / 100).toFixed(decimals);
	                if (allowStepDown && Math.abs(numberStr) < 1 && value != 0){
	                    return formatThousand(value, decimals, allowStepDown);
	                }
	                return numberStr + 'M';
	            }
	        }
	        function formatThousand(value, decimals, allowStepDown){
	            var truncValue = value/1e3;
	            var numberStr;
	            if (decimals == null || decimals == 0){
	                if (truncValue % 1 == 0){
	                    return truncValue + 'K';
	                }
	                numberStr = truncValue.toString();
	                return numberStr.substring(0,numberStr.indexOf('.')) + 'K';
	            }
	            numberStr = parseFloat(Math.round(truncValue * 100) / 100).toFixed(decimals);
	            if (allowStepDown && Math.abs(numberStr) < 1 && value != 0){
	                return formatText(value, {
	                    type : 'decimal',
	                    decimals : decimals,
	                    allowStepDown : allowStepDown
	                });
	            }
	            return numberStr + 'K';
	        }
	
	        function formatTime(value, divisor){
	            var d = new Date(0); // The 0 there is the key, which sets the date to the epoch
	            d.setUTCSeconds(value/(divisor?divisor:1000)); // Assume default of milliseconds
	            var dateStr = (d.getMonth()+1) + '/' + d.getDate() + '/' + (d.getFullYear());
	            return dateStr;
	        }
	        function formatText(value, labelSpec){
	            if (labelSpec){
	                var labelType = labelSpec.type;
	                if (labelType == 'time'){
	                    return formatTime(value, labelSpec.divisor);
	                }
	                // Millions to 2 decimal places
	                else if (labelType == 'decimal'){
	                    return formatNumber(value, labelSpec.decimals, labelSpec.allowStepDown);
	                }
	                else if (labelType == 'B'){
	                    return formatBillion(value, labelSpec.decimals, labelSpec.allowStepDown);
	                }
	                else if (labelType == 'M'){
	                    return formatMillion(value, labelSpec.decimals, labelSpec.allowStepDown);
	                }
	                else if (labelType == 'K'){
	                    return formatThousand(value, labelSpec.decimals, labelSpec.allowStepDown);
	                }
	            }
	            return value;
	        }
	
	
	        var zoomLevel = spec.map.zoom;
	        var min = 0,
	            max = 0;
	        var isXAxis = spec.type == 'xaxis';
	        var xOrY = isXAxis ? 'x' : 'y';
	        var widthOrHeight = isXAxis ? "width" : "height";
	        var parentDiv = spec.parentId;
	
	        if (spec.layer != null){
	            max = spec.layer.bounds.max[xOrY];
	            min = spec.layer.bounds.min[xOrY];
	        }
	
	        //TODO: This should be obsolete now since any overrides
	        // should be put into the spec at spec-creation time.
	
	        // Check if the user has defined override min/max values.
	        if (spec.min != null){
	            min = spec.min;
	        }
	        if (spec.max != null){
	            max = spec.max;
	        }
	
	        //TODO: This needs to be made generic.
	        var pMax = parseInt(spec.map.pixelBounds.max[xOrY]);
	        var pMin = parseInt(spec.map.pixelBounds.min[xOrY]);
	
	        var tileSize = spec.map.tileSize ? spec.map.tileSize : pMax;
	        var newRangeMax = tileSize*(Math.pow(2,zoomLevel));// The size used here is not the display width.
	        var newRangeMin = 0;
	        var rangeDelta = newRangeMax - newRangeMin;
	
	        var axisState = spec.options;
	        var data = AxisUtil.getMarkerPositions(zoomLevel, min, max, axisState.intervals);
	
	        var tickValue = 0;
	        var pixelPosition = 0;
	        var tickCount = 0; //For debugging
	
	        // Create divs to contain the axes lines.
	        var axisDiv = spec.divId;
	        var plotContainer = $("div[id='"+ parentDiv +"']"); // This way works with '.' in the div id.
	        var axisLengthCssVal = plotContainer.css(widthOrHeight);
	        var axisLength = axisLengthCssVal.replace('px', '');
	
	        var axisContainer = $("div[id='"+ axisDiv +"']");
	        if (axisContainer.length == 0){
	            axisContainer = $('<div id="' + axisDiv + '"></div>');
	            axisContainer.addClass(xOrY + '-scale-line');
	        }
	        else {
	            // If an axis exists, clear it out.
	            axisContainer.empty();
	        }
	
	        // Re-apply the size even if div already existed. (map may have re-sized.)
	        axisContainer.css(widthOrHeight, axisLength);
	        plotContainer.append(axisContainer);
	
	        // Create the axis title.
	        var axisLabel = $('<div id="' + xOrY + 'title_' + axisDiv + '"></div>');
	        axisLabel.addClass(xOrY + '-scale-label');
	        if (isXAxis){
	            axisLabel.css('width', axisLength);
	        }
	        else {
	            axisLabel.css('top', axisLength*0.5);
	        }
	        axisLabel.html(axisState.title);
	        axisContainer.append(axisLabel);
	
	        // Create the tick marks.
	        for( var i=0; i < data.length; i++ ) {
	            if (isXAxis){
	                tickValue = (((data[i] - min)*rangeDelta)/(max-min) + newRangeMin);
	                pixelPosition = Math.round(tickValue + (-1)*pMin);
	            }
	            else {
	                tickValue = (((data[i] - min)*rangeDelta)/(max-min) - newRangeMin);
	                pixelPosition = Math.round(tickValue + pMax - newRangeMax);
	            }
	            if (pixelPosition < 0 || pixelPosition > axisLength){
	                continue;
	            }
	
	            var majorMarker = $('<div></div>');
	            majorMarker.addClass(xOrY + '-scale-majorMarker');
	
	            var label = $('<div></div>');
	            var labelText = formatText(data[i], axisState.labelSpec);
	            label.html(labelText);
	            var labelClass = xOrY + '-scale-marker-label';
	            label.addClass(labelClass);
	
	            axisContainer.append(majorMarker);
	            axisContainer.append(label);
	
	            var width = label.width();
	
	            if(width === 0){ // Perhaps the label is currently hidden?
	                // Explanation of these few lines using $.swap:
	                //  The label.getWidth() for this tick returns zero on first call if
	                //  it is hidden by a collapsed table row (i.e. beside a large density strip)
	                //  So we find the ultimately hidden parent and temporarily apply specific
	                //  props that cause the label to have a true width.
	                // Ref: http://www.foliotek.com/devblog/getting-the-width-of-a-hidden-element-with-jquery-using-width/
	                var workaroundProps = {position: "absolute", visibility: "hidden", display: "block"};
	                var tableRowParent = plotContainer.parents().filter(function(){    // Note: ":hidden" will not work if isn't immediate parent.
	                    return $(this).css("display") === "none";
	                });
	                if(tableRowParent[0]){
	                    $.swap(tableRowParent[0], workaroundProps, function(){
	                        width = label.width();
	                    });
	                }
	            }
	
	            if (isXAxis){
	                if (i==0){
	                    majorMarker.css({left : pixelPosition});
	                }  else {
	                    majorMarker.css({left : pixelPosition - 1});
	                }
	                label.css({left : pixelPosition - (width/2)});
	            }
	            else {
	                if (i==0){
	                    majorMarker.css({bottom : pixelPosition});
	                }  else {
	                    majorMarker.css({bottom : pixelPosition - 1});
	                }
	                label.css({
	                    left : -width-15,
	                    bottom : pixelPosition - 7
	                });
	            }
	            tickCount++;
	            //TODO: This shouldn't be necessary.
	            if (tickCount > 1000){
	                console.log("Number of markers exceeded rendering limit.");
	                break;
	            }
	        }
	    },
	
	    getTickMarkLocationForPixelLocation : function(spec, pixelLocation){
	        var zoomLevel = spec.map.zoom;
	        var min = 0,
	            max = 0;
	        var isXAxis = spec.type == 'xaxis';
	        var xOrY = isXAxis ? 'x' : 'y';
	        var widthOrHeight = isXAxis ? "width" : "height";
	        var parentDiv = spec.parentId;
	
	        if (spec.layer != null){
	            max = spec.layer.bounds.max[xOrY];
	            min = spec.layer.bounds.min[xOrY];
	        }
	
	        // Check if the user has defined override min/max values.
	        if (spec.min != null){
	            min = spec.min;
	        }
	        if (spec.max != null){
	            max = spec.max;
	        }
	
	        // Create divs to contain the axes lines.
	        var axisDiv = spec.divId;
	        var plotContainer = $("div[id='"+ parentDiv +"']"); // This way works with '.' in the div id.
	        var axisLengthCssVal = plotContainer.css(widthOrHeight);
	        var axisLength = axisLengthCssVal.replace('px', '');
	
	        var pMax = parseInt(spec.map.pixelBounds.max[xOrY]);
	        var pMin = parseInt(spec.map.pixelBounds.min[xOrY]);
	
	        var tileSize = spec.map.tileSize ? spec.map.tileSize : pMax;
	        var newRangeMax = tileSize*(Math.pow(2,zoomLevel));// The size used here is not the display width.
	        var newRangeMin = 0;
	        var rangeDelta = newRangeMax - newRangeMin;
	
	        var axisState = spec.options;
	
	        var tickValue = 0;
	        var pixelPosition = 0;
	
	        var val;
	        if (isXAxis){
	            tickValue = pixelLocation - (-1)*pMin;
	            val = (tickValue * ((max-min)+newRangeMin) / rangeDelta) + min;
	
	        }
	        else {
	            tickValue = (axisLength - pixelLocation - pMax + newRangeMax);
	            val =  ((tickValue * ((max-min)-newRangeMin)) / rangeDelta) + min;
	        }
	
	        return val;
	    }
	};
});
