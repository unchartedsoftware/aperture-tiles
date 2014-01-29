/**
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



    var Class = require('../class'),
        AxisUtil = require('./AxisUtil'),
        updateMutableAttributes,
        checkInputSpec,
        MARKER_LABEL_SPACING = 5,
        AXIS_LABEL_SPACING = 10,
        Axis;



    /** Private function
     * Updates mutable spec attributes and set change flag to true if they are different
     * from previous call. Change flag is used to determine whether or not to redraw the axis
     */
    updateMutableAttributes = function(axis) {

        var totalPixelSpan,
            newPixelMin,
            newPixelMax,
            newZoom,
            newAxisLength;

        // update zoom
        newZoom = axis.olMap.getZoom();
        if ( newZoom !== axis.zoom ) {
            // zoom changed
            axis.zoom = newZoom;
            axis.changeFlag = true;
        }

        totalPixelSpan = {
            x: axis.tileSize * Math.pow(2, axis.zoom),
            y: axis.olMap.viewPortDiv.clientHeight
        };

        // update pixel min
        newPixelMin = totalPixelSpan[axis.xOrY] - axis.olMap.maxPx[axis.xOrY];
        if ( newPixelMin !== axis.pixelMin ) {
            // zoom changed
            axis.pixelMin = newPixelMin;
            axis.changeFlag = true;
        }

        // update pixel max
        newPixelMax = totalPixelSpan[axis.xOrY] - axis.olMap.minPx[axis.xOrY];
        if ( newPixelMax !== axis.pixelMax ) {
            // zoom changed
            axis.pixelMax = newPixelMax;
            axis.changeFlag = true;
        }

        // update axis length
        newAxisLength = $("#" + axis.parentId).css(axis.widthOrHeight).replace('px', ''); // strip px suffix
        if ( newAxisLength !== axis.axisLength ) {
            // zoom changed
            axis.axisLength = newAxisLength;
            axis.changeFlag = true;
        }
    };

    /** Private function
     * Check spec object for all parameters, if any are missing, fill with defaults
     */
     checkInputSpec = function(spec) {

        var temp;

        // must have a parent id
        if (spec.parentId === undefined ) {
            return false;
        }
        // must have an open layers map
        if (spec.olMap === undefined) {
            return false;
        }
        // must have a min and max value
        if (spec.min === undefined || spec.max === undefined) {
            return false;
        }
        // ensure max is greater than min
        if (spec.min > spec.max) {
            // swap values
            temp = spec.min;
            spec.min = spec.max;
            spec.max = temp;
        }
        // ensure position is valid
        if ( (spec.position).toLowerCase() !== "top" &&
             (spec.position).toLowerCase() !== "bottom" &&
             (spec.position).toLowerCase() !== "left" &&
             (spec.position).toLowerCase() !== "right") {

            spec.position = "bottom";
        }
        // set id if none is specified
        if (spec.id === undefined ) {
            spec.id = spec.parentId + "-" + spec.position + "-axis";
        }
        // set default title if none is supplied
        if (spec.title === undefined) {
            spec.title = "";
        }
        // set default interval spec if none supplied
        if (spec.intervalSpec === undefined) {
            spec.intervalSpec = {
                type: "percentage",
                value: 10,
                pivot: 0,
                allowScaleByZoom: true,
                isMercatorProjected: false
            };
        }
        if (spec.intervalSpec.type === undefined) {
            spec.intervalSpec.type = "percentage";
        }
        if (spec.intervalSpec.value === undefined || spec.intervalSpec.value === 0) {
            spec.intervalSpec.value = 10;
        }
        if (spec.intervalSpec.value < 0 ) {
             spec.intervalSpec.value = Math.abs(spec.intervalSpec.value);
        }
        if (spec.intervalSpec.pivot === undefined) {
            spec.intervalSpec.pivot = 0;
        }
        if (spec.intervalSpec.allowScaleByZoom === undefined) {
            spec.intervalSpec.allowScaleByZoom = true;
        }
        if (spec.intervalSpec.isMercatorProjected === undefined) {
            spec.intervalSpec.isMercatorProjected = false;
        }

        // set default unit spec if none supplied
        if (spec.unitSpec === undefined) {
            spec.unitSpec = {
                type: 'decimal',
                divisor: 1000,
                decimals: 2,
                allowStepDown: true
            };
        }
        if (spec.unitSpec.type === undefined) {
            spec.unitSpec.type = 'decimal';
        }
        if (spec.unitSpec.divisor === undefined) {
            spec.unitSpec.divisor = 1000;
        }
        if (spec.unitSpec.decimals === undefined) {
            spec.unitSpec.decimals = 2;
        }
        if (spec.unitSpec.allowStepDown === undefined) {
            spec.unitSpec.allowStepDown = true;
        }

        return true;
    };


    Axis = Class.extend({
        /**
         * Construct an axis
         * @param spec Axis specification object:
         *             {
         *
         *                  title:      axis label text,        ex. "Longitude"
         *                  type:       axis label type,        ex. 'x' or 'y'
         *                  parentId:   container for axis,     ex. worldMap.mapSpec.id
         *                  id:         id for axis,            ex. "map-x-axis"
         *                  olMap:      OpenLayers map binding  ex. worldMap.map.olMap_
         *                  min:        minimum axis value      ex. worldMap.mapSpec.options.mapExtents[0]
         *                  max:        maximum axis value      ex. worldMap.mapSpec.options.mapExtents[2]
         *                  intervalSpec: {
         *                      type:   type of interval        ex. "fixed" or "percentage",
         *                      value:  fixed / percentage increment of the axis
         *                                                      ex. 10
         *                      pivot:  the fixed value / percentage that all other values are incremented from
         *                                                      ex. 0
         *                      allowScaleByZoom: if the axis should be scaled by the zoom factor
         *                                                      ex. false
         *                      isMercatorProjected: whether or not to project value by mercator projection
         *                                                      ex. true
         *                  },
         *                  intervals:  number of ticks between min and max value at zoom level 1
         *                                                      ex. 6
         *                  unitSpec: {
         *                      type:       type of units parse in FormatAxis.js
         *                                                      ex.'degrees', 'time', 'decimal', 'B', 'M', 'K'
         *                      divisor:    unit divisor        ex. undefined or 1000
         *                      decimals:   number of decimals  ex. 2
         *                      allowStepDown: if the units can step down if they are below range
         *                                                  ex. true .001M => 1K
         *                  },
         *                  repeat:     whether or not the axis will repeat
         *                                                  ex. true or false
         *
         */
        init: function (spec) {

            var that = this,
                key;

            // check input spec, fill in any missing values
            this.good = checkInputSpec(spec);

            // copy properties to the axis object
            for (key in spec) {
                if (spec.hasOwnProperty(key)) {
                    this[key] = spec[key];
                }
            }

            // generate more attributes
            this.isXAxis = (spec.position === 'top' || spec.position === 'bottom');
            this.xOrY = this.isXAxis ? 'x' : 'y';
            this.widthOrHeight = this.isXAxis ? "width" : "height";
            this.tileSize = this.isXAxis ? spec.olMap.getTileSize().w : spec.olMap.getTileSize().h;

            this.majorMarkerLength = 10;
            this.majorMarkerWidth = 1;

            this.majorMarkerColour = "#7E7E7E";
            this.markerLabelColour = "#7E7E7E";
            this.axisLabelColour = "#7E7E7E";

            this.fontFamily = "Tahoma, Verdana, Segoe, sans-serif";

            this.markerLabelFontSize = "0.75em";
            this.axisLabelFontSize = "0.95em";

            this.maxLabelLength = 0;

            this.olMap.events.register('mousemove', this.olMap, function(event) {
                that.redraw();
            });

            this.olMap.events.register('panend', this.olMap, function(event) {
                that.redraw();
            });

            this.olMap.events.register('zoomend', this.olMap, function(event) {
                that.redraw();
            });

            this.redraw();
        },

        /**
         * Checks if the mutable spec attributes have changed, if so, redraws
         * axis.
         */
        redraw: function() {

            var axis = {},
                markers = [],
                that = this;

            /**
             * Creates and returns the axis label element with proper CSS
             */
            function createAxisLabel() {

                var positionDir,
                    rotation;

                if (that.isXAxis) {
                    positionDir = "left";
                } else {
                    positionDir = "top";
                    if (that.position === "left") {
                        rotation = "rotate(" + (-90) + "deg)";
                    } else {
                        rotation = "rotate(" + 90 + "deg)";
                    }
                }

                return $('<div id="' + that.id + '-label"'
                    + 'style="position:absolute;'
                    + 'font-family: ' + that.fontFamily + ';'
                    + 'font-size:' + that.axisLabelFontSize + ';'
                    + 'color:' + that.axisLabelColour + ';'
                    + positionDir + ':' + (that.axisLength*0.5) + 'px;'
                    + '-webkit-transform: ' + rotation + ";"
                    + '-moz-transform: ' + rotation + ";"
                    + '-ms-transform: ' + rotation + ";"
                    + '-o-transform: ' + rotation + ";"
                    + 'transform: ' + rotation + ";"
                    + '">' + that.title + '</div>');
            }

            /**
             * Creates the axis main div elements with proper CSS
             */
            function addAxisMainElements() {

                // get parent element
                axis.parentContainer = $("#" + that.parentId);

                // ensure parent container has a div to add margins for the axes
                if ( axis.parentContainer.parent('.axis-margin-container').length === 0 ) {
                   axis.parentContainer.wrap("<div class='axis-margin-container' style='position:relative; width:" + that.axisLength + "px';></div>");
                }
                axis.marginContainer = $('.axis-margin-container');

                // get axis container
                axis.container = $("#"+ that.id);
                if (axis.container.length === 0) {
                    // if container does not exist, make it
                    axis.container = $('<div id="' + that.id + '"></div>');
                } else {
                    // if the axis exists, clear it out, we need to redraw the markers
                    axis.container.empty();
                }

                // add axis to parent container
                axis.parentContainer.append(axis.container);

                // create the axis title label
                axis.label = createAxisLabel();

                // add axis label to container
                axis.container.append(axis.label);
            }

            /**
             * Creates and returns a marker label element with proper CSS
             */
            function createMarkerLabel(marker) {

                return $('<div id="' + that.id + '-marker-label"'
                       + 'style="position:absolute;'
                       + 'font-family: ' + that.fontFamily + ';'
                       + 'font-size:' + that.markerLabelFontSize + ';'
                       + 'color:' + that.markerLabelColour + ';'
                       + '">' + AxisUtil.formatText( marker.label, that.unitSpec ) + '</div>');
            }

            /**
             * Creates and returns a major marker element with proper CSS
             */
            function createMajorMarker(marker) {

                var lengthDim,
                    positionDir;

                if (that.isXAxis) {
                    lengthDim = 'height';
                    positionDir = 'left';
                } else {
                    lengthDim = 'width';
                    positionDir = 'bottom';
                }

                return $('<div id="' + that.id + 'major-marker"'
                    + 'style="position:absolute;'
                    + 'color:' + that.majorMarkerColour + ';'
                    + that.position + ":" + (-that.majorMarkerLength) + "px;"
                    + lengthDim + ":" + that.majorMarkerLength + "px;"
                    + 'border-' + positionDir + ":" + that.majorMarkerWidth + "px solid" + that.majorMarkerColour + ";"
                    + positionDir + ":" + (marker.pixel - that.majorMarkerWidth*0.5) + "px;"
                    + '"></div>');

            }

            /**
             * Creates the axis marker elements with proper CSS
             */
            function addAxisMarkerElements() {

                var majorMarker,
                    markerLabel,
                    labelLength,
                    labelOffset,
                    markerLabelCSS = {},
                    i;

                for( i = 0; i < markers.length; i += 1 ) {

                    // create a major marker
                    majorMarker = createMajorMarker(markers[i]);

                    // create marker label
                    markerLabel = createMarkerLabel(markers[i]);

                    // append marker to axis container
                    axis.container.append(majorMarker);
                    axis.container.append(markerLabel);

                    // append before this code as it needs to know the width and height
                    // of the marker labels
                    if (that.isXAxis) {
                        // get label position
                        markerLabelCSS[that.position] = (-markerLabel.height() - (that.majorMarkerLength + MARKER_LABEL_SPACING)) + "px";
                        // centre label under tick
                        markerLabelCSS.left = (markers[i].pixel - (markerLabel.width()*0.5)) +"px";
                        labelLength = markerLabel.height();
                    }
                    else {
                        // get label position
                        markerLabelCSS[that.position] = (-markerLabel.width() - (that.majorMarkerLength + MARKER_LABEL_SPACING)) + "px";
                        // centre label on tick
                        markerLabelCSS.bottom = (markers[i].pixel - (markerLabel.height()*0.5)) + "px";
                        // get text alignment
                        if (that.position === "left") {
                            markerLabelCSS["text-align"] = "right";
                        } else {
                            markerLabelCSS["text-align"] = "left";
                        }
                        labelLength = markerLabel.width() + axis.label.height();
                    }

                    // get marker label css
                    markerLabel.css(markerLabelCSS);
                    // find max label length to position axis title label
                    if (that.maxLabelLength < labelLength) {
                        that.maxLabelLength = labelLength;
                    }
                }

                labelOffset = that.maxLabelLength + 20 // for some reason the spacing is a bit off on the
                            + MARKER_LABEL_SPACING
                            + that.majorMarkerLength
                            + AXIS_LABEL_SPACING
                            + axis.label.height();

                if (that.isXAxis) {
                    labelOffset += 20;
                }

                // position axis label
                axis.label.css( that.position, -labelOffset + 'px' );

                // add margin space for axis
                axis.marginContainer.css('margin-' + that.position, labelOffset + 'px');

                // div container may change size, this updates properties accordingly
                that.olMap.updateSize();
            }

            if (this.good) {
                // update mutable spec attributes
                updateMutableAttributes(this);
                // only redraw if it has changed since last redraw
                if (this.changeFlag) {
                    // generate array of marker labels and pixel locations
                    markers = AxisUtil.getMarkers(this);
                    // generate the main axis DOM elements
                    addAxisMainElements();
                    // add each marker to correct pixel location in axis DOM elements
                    addAxisMarkerElements();
                    // reset change flag
                    this.changeFlag = false;
                }
            }

        },

        /**
         * Given an axis spec and pixel location, returns axis value.
         *
         * @param pixelLocation     Pixel location of axis query
         */
        getAxisValueForPixel : function(pixelLocation) {

            var mapPixelSpan,
                tickValue,
                value;

            if (this.good) {

                updateMutableAttributes(this);

                // The size used here is not the display width.
                mapPixelSpan = this.tileSize*(Math.pow(2, this.zoom));

                if (this.isXAxis){
                    tickValue = pixelLocation + this.pixelMin;
                    value = (tickValue * ((this.max-this.min)) / mapPixelSpan) + this.min;
                }
                else {
                    tickValue = (this.axisLength - pixelLocation - this.pixelMax + mapPixelSpan);
                    value = ((tickValue * ((this.max-this.min))) / mapPixelSpan) + this.min;
                }

                if (!this.isXAxis && this.intervalSpec.isMercatorProjected) {
                    // find the gudermannian value where this current linear value is
                    value = AxisUtil.scaleLinearToGudermannian( value, this );
                }

                // return axis value, rollover if necessary
                return AxisUtil.formatText( AxisUtil.getMarkerRollover( this, value ), this.unitSpec );
            }
        }

    });

    return Axis;
});
