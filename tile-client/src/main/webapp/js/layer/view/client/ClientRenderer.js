/*
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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines the base class for a client render layer. Must be 
 * inherited from for any functionality.
 */
define(function (require) {
    "use strict";



    var Class = require('../../../class'),
		idIncrement = 0,
        ClientRenderer;



    ClientRenderer = Class.extend({
        ClassName: "ClientRenderer",

        /**
         * Constructs a client render layer object
         * @param id the id string for the render layer
         */
        init: function(id, map, avoidIncrement) {
			// ensure each render layer has a unique id for view controller to maintain visibility correctly
			id = id || "renderer-id";
			if (avoidIncrement) {
				this.id = id;	// don't increment id, used for nested renderers such as DetailsOnDemand
			} else {
				this.id = id + "-" + idIncrement++;
			}
            this.map = map;
            this.clientState = null;

            this.TILE_SIZE = 256;
            this.X_CENTRE_OFFSET = this.TILE_SIZE / 2;  // x offset required to centre on tile x-axis
            this.Y_CENTRE_OFFSET = 0;                   // y offset required to centre on tile y-axis

            this.BLACK_COLOUR = '#000000';
            this.DARK_GREY_COLOUR = '#222222';
            this.GREY_COLOUR = '#666666';
            this.LIGHT_GREY_COLOUR = '#999999';
            this.WHITE_COLOUR = '#FFFFFF';
            this.BLUE_COLOUR = '#09CFFF';
            this.DARK_BLUE_COLOUR  = '#069CCC';
            this.PURPLE_COLOUR = '#D33CFF';
            this.DARK_PURPLE_COLOUR = '#A009CC';
            this.YELLOW_COLOUR = '#F5F56F';
        },


        /**
         * Returns the opacity of the layer set in the layer controls
         */
        getOpacity: function() {
            return this.clientState.getSharedState('opacity');
        },


        /**
         * Returns true if the layer is enabled in the layer controls
         */
        isVisible: function() {
            return this.clientState.getSharedState('isVisible');
        },


        /**
         * Returns true if the layer is selected for the respective tile by the view controller,
         * the data.renderer attribute is assigned by the TileTracker
         * @param data aperturejs node data object
         */
        isSelectedView: function(data) {
            return data.rendererId === this.id;
        },


        /**
         * Attaches a mouse state object to be shared by the render layer. A ViewController has
         * primary ownership of this object. It is too be shared with each render layer so that
         * mouse events can be handled across each layer
         * @param clientState the mouse state object
         */
        attachClientState: function(clientState) {
            this.clientState = clientState;
        },


        /**
         * Empty function to create the layer, must be overloaded
         * @param nodeLayer the aperturejs.geo.mapnodelayer
         */
        createLayer: function (nodeLayer) {
			console.log('Warning createLayer function in client renderer has not been overloaded');
        },


        /**
         * Helper function for converting hex code colour to greyscale
         * @param hex the hex code colour
         */
        hexToGreyscale: function(hex) {
            var col, rgb;
            if (hex[0] === '#') {
                hex = hex.substr(1,6);
            }
            col = parseInt(hex, 16);
            rgb = (((((((col >> 16) & 0xff)*76) + (((col >> 8) & 0xff)*150) +
                   ((col & 0xff)*29)) >> 8)) << 16) |

                   (((((((col >> 16) & 0xff)*76) + (((col >> 8) & 0xff)*150) +
                   ((col & 0xff)*29)) >> 8)) << 8) |

                   ((((((col >> 16) & 0xff)*76) + (((col >> 8) & 0xff)*150) +
                   ((col & 0xff)*29)) >> 8));

            return this.rgbToHex( (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
        },


        /**
         * Helper function for converting rgb components to hex code colour
         * @param r red component, should be in range 0-255
         * @param g green component, should be in range 0-255
         * @param b blue component, should be in range 0-255
         */
         rgbToHex : function(r, g, b) {
             function componentToHex(c) {
                 var hex = c.toString(16);
                 return (hex.length === 1) ? "0" + hex : hex;
             }
             return "#" + componentToHex( Math.floor(r)) +
                          componentToHex( Math.floor(g)) +
                          componentToHex( Math.floor(b));
        },


        /**
         * Helper function for converting hex code colour to rgb components
         * @param hex the hex code colour
         */
         hexToRgb: function(hex) {
             var bigint;
             if (hex[0] === '#') {
                 hex = hex.substr(1,6);
             }
             bigint = parseInt(hex, 16);
             return {
                 r: (bigint >> 16) & 255,
                 g: (bigint >> 8) & 255,
                 b: bigint & 255
             };
        },


        createPartitionLine: function( spec ) {

            var that = this,
                bar = this.plotLayer.addLayer(aperture.BarLayer);

            bar.on('click', function() { return true; }); //swallow event
            bar.map('visible').from(spec.isVisibleFunc);
            bar.map('fill').asValue(spec.colour);
            bar.map('orientation').asValue('horizontal');
            bar.map('bar-count').asValue(1);
            bar.map('length').asValue(spec.length);
            bar.map('width').asValue(1);
            bar.map('offset-x').asValue(spec.x - 1);
            bar.map('offset-y').asValue(spec.y - 1);
            bar.map('opacity').from( function() { return that.getOpacity(); });
            return bar;
        },


        createBarChartAxis: function( spec ) {

            var that = this,
                AXIS_OFFSET = 2,    // offset borders
                TICK_LENGTH = 6,
                TICK_WIDTH = 3,
                X_SPACING = spec.length / (spec.numIncs-1),
                LABEL_TICK_OFFSET = 10,
                labels, ticks;

            // axis labels
            labels = this.plotLayer.addLayer(aperture.LabelLayer);
            labels.on('click', function() { return true; }); //swallow event
            labels.map('label-count').asValue(spec.numIncs);
            labels.map('visible').from(spec.isVisibleFunc);
            labels.map('fill').asValue( this.WHITE_COLOUR );
            labels.map('text-anchor').asValue('middle');
            labels.map('font-outline').asValue(this.BLACK_COLOUR);
            labels.map('font-outline-width').asValue(3);
            labels.map('opacity').from( function() { return that.getOpacity(); });
            labels.map('offset-x').from(function(index) { return spec.x + X_SPACING*index; });
            labels.map('offset-y').asValue( spec.y + AXIS_OFFSET + LABEL_TICK_OFFSET);
            labels.map('text').from(spec.labelFunc);

            // axis markers
            ticks = this.plotLayer.addLayer(aperture.BarLayer);
            ticks.map('visible').from(spec.isVisibleFunc);
            ticks.map('orientation').asValue('vertical');
            ticks.map('fill').asValue( this.WHITE_COLOUR );
            ticks.map('length').asValue(TICK_LENGTH);
            ticks.map('width').asValue(TICK_WIDTH);
            ticks.map('stroke').asValue(this.BLACK_COLOUR);
            ticks.map('stroke-width').asValue(1);
            ticks.map('opacity').from( function() { return that.getOpacity(); });
            ticks.map('bar-count').asValue(spec.numIncs);
            ticks.map('offset-x').from( function(index) { return spec.x - AXIS_OFFSET + X_SPACING*index; });
            ticks.map('offset-y').asValue(spec.y  + AXIS_OFFSET);

            return {
                labels : labels,
                markers : ticks
            };
        },

        createBarSeries: function( spec ) {

            var that = this,
                bars;

            bars = this.plotLayer.addLayer(aperture.BarLayer);
            bars.on('click', function() { return true; }); //swallow event
            bars.map('visible').from(spec.isVisibleFunc);
            bars.map('fill').from( spec.colourFunc );
            bars.map('orientation').asValue('vertical');
            bars.map('stroke').asValue(that.BLACK_COLOUR);
            bars.map('stroke-width').asValue(2);
            bars.map('opacity').from( function() { return that.getOpacity(); });
            bars.map('bar-count').from(spec.countFunc);
            bars.map('offset-x').from( spec.xFunc );
            bars.map('offset-y').from( spec.yFunc );
            bars.map('length').from( spec.heightFunc );
            bars.map('width').from(function() { return spec.length / $.proxy( spec.countFunc, this )(); });

            if (spec.mousemove !== undefined) {
                bars.on('mousemove', spec.mousemove );
            }

            if (spec.mouseout !== undefined) {
                bars.on('mouseout', spec.mouseout );
            }

            return bars;
        },


        /**
         * Helper function for break text up into multiple lines
         * @param str the text to format
         * @param charPerLine number of characters allowed per line
         * @param maxNumLines maximum number of alines, rest of text is truncated with '...' appended
         */
        separateTextIntoLines: function(str, charPerLine, maxNumLines) {
            var CHAR_PER_LINE = charPerLine || 35,
                MAX_NUM_LINES = maxNumLines || 3,
                strArray = str.split(" "),
                formatted = '',
                spaceLeft = CHAR_PER_LINE,
                i,
                lineCount = 0;

            for (i=0; i<strArray.length; i++) {
                while (strArray[i].length > spaceLeft) {

                    // if past maximum amount of lines, truncate
                    if (lineCount === MAX_NUM_LINES-1) {
                        // strip space if is als character of string
                        if (formatted[formatted.length-1] === ' ') {
                            formatted = formatted.substring(0, formatted.length - 1);
                        }
                        return formatted + strArray[i].substr(0, spaceLeft-3) + "...";
                    }
                    if (strArray[i].length < CHAR_PER_LINE) {
                        // can fit in next line, put new line
                        formatted += "\n";
                    } else {
                        // cannot fit in next line, hyphenate word
                        formatted += strArray[i].substr(0, spaceLeft);
                        strArray[i] = strArray[i].substr(spaceLeft);
                        if (spaceLeft > 0) {
                            formatted += "-\n";
                        }
                    }
                    spaceLeft = CHAR_PER_LINE;
                    lineCount++;
                }
                formatted += strArray[i] + ' ';
                spaceLeft -= strArray[i].length+1;
            }
            return formatted;
        }


    });

    return ClientRenderer;
});
