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



    var Class = require('../class'),
        AxisUtil = require('./AxisUtil'),
        updateMutableAttributes,
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


    Axis = Class.extend({
        /**
         * Construct an axis
         * @param spec Axis specification object:
         *             {
         *                  title:      axis label text,        ex. "Longitude"
         *                  parentId:   container for axis,     ex. worldMap.mapSpec.id
         *                  id:         id for axis,            ex. "map-x-axis"
         *                  olMap:      OpenLayers map binding  ex. worldMap.map.olMap_
         *                  min:        minimum axis value      ex. worldMap.mapSpec.options.mapExtents[0]
         *                  max:        maximum axis value      ex. worldMap.mapSpec.options.mapExtents[2]
         *                  projection: map projection used     ex. EPSG:900913
         *                  intervalSpec: {
         *                      type:   type of interval        ex. "fixed" or "percentage",
         *                      increment:  fixed / percentage increment of the axis
         *                                                      ex. 10
         *                      pivot:  the fixed value / percentage that all other values are incremented from
         *                                                      ex. 0
         *                      allowScaleByZoom: if the axis should be scaled by the zoom factor
         *                                                      ex. false
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
                temp,
                defaults = {
                    title : "",
                    position : "bottom",
                    projection: "EPSG:4326",
                    repeat: false,
                    intervalSpec : {
                        type: "percentage",
                        increment: 10,
                        pivot: 0,
                        allowScaleByZoom: true
                    },
                    unitSpec : {
                        type: 'decimal',
                        divisor: 1000,
                        decimals: 2,
                        allowStepDown: true
                    },
                    style : {
                        majorMarkerLength : 10,
                        majorMarkerWidth : 1,
                        majorMarkerColour : "#7E7E7E",
                        markerLabelColour : "#7E7E7E",
                        markerLabelRotation : 0,
                        markerLabelFontSize : "0.75em",
                        axisLabelColour : "#7E7E7E",
                        axisLabelFontSize : "0.95em",
                        fontFamily : "Tahoma, Verdana, Segoe, sans-serif"
                    }
                };

            this.parentId = spec.parentId;

            // ensure min is < max
            if (spec.min > spec.max) {
                // swap values
                temp = spec.min;
                spec.min = spec.max;
                spec.max = temp;
            }
            this.min = spec.min;
            this.max = spec.max;
            this.olMap = spec.olMap;

            this.position = spec.position || defaults.position;
            this.id = spec.id || this.parentId + "-" + this.position + "-axis";
            this.projection = spec.projection || defaults.projection;

            this.title = spec.title || defaults.title;

            spec.intervalSpec = spec.intervalSpec || {};
            this.intervalSpec = {};
            this.intervalSpec.type = spec.intervalSpec.type || defaults.intervalSpec.type;
            this.intervalSpec.increment = spec.intervalSpec.increment || defaults.intervalSpec.increment;
            this.intervalSpec.pivot = spec.intervalSpec.pivot || defaults.intervalSpec.pivot;
            this.intervalSpec.allowScaleByZoom = spec.intervalSpec.allowScaleByZoom || defaults.intervalSpec.allowScaleByZoom;

            spec.unitSpec = spec.unitSpec || {};
            this.unitSpec = {};
            this.unitSpec.type = spec.unitSpec.type || defaults.unitSpec.type;
            this.unitSpec.divisor = spec.unitSpec.divisor || defaults.unitSpec.divisor;
            this.unitSpec.decimals = spec.unitSpec.decimals || defaults.unitSpec.decimals;
            this.unitSpec.allowStepDown = spec.unitSpec.allowStepDown || defaults.unitSpec.allowStepDown;

            spec.style = spec.style || {};
            this.style = {};
            this.style.majorMarkerLength = spec.style.majorMarkerLength || defaults.style.majorMarkerLength;
            this.style.majorMarkerWidth = spec.style.majorMarkerWidth || defaults.style.majorMarkerWidth;
            this.style.majorMarkerColour = spec.style.majorMarkerColour || defaults.style.majorMarkerColour;

            this.style.markerLabelRotation = spec.style.markerLabelRotation || defaults.style.markerLabelRotation;
            this.style.markerLabelColour = spec.style.markerLabelColour || defaults.style.markerLabelColour;
            this.style.markerLabelFontSize = spec.style.markerLabelFontSize || defaults.style.markerLabelFontSize;

            this.style.axisLabelFontSize = spec.style.axisLabelFontSize || defaults.style.axisLabelFontSize;
            this.style.axisLabelColour = spec.style.axisLabelColour || defaults.style.axisLabelColour;

            this.style.fontFamily = spec.style.fontFamily || defaults.style.fontFamily;

            // generate more attributes
            this.isXAxis = (this.position === 'top' || this.position === 'bottom');
            this.xOrY = this.isXAxis ? 'x' : 'y';
            this.widthOrHeight = this.isXAxis ? "width" : "height";
            this.tileSize = this.isXAxis ? this.olMap.getTileSize().w : this.olMap.getTileSize().h;

            this.maxLabelLength = 0;

            this.olMap.events.register('mousemove', this.olMap, function(event) {
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

                return $('<div class="' + that.id + '-label"'
                    + 'style="position:absolute;'
                    + 'font-family: ' + that.style.fontFamily + ';'
                    + 'font-size:' + that.style.axisLabelFontSize + ';'
                    + 'color:' + that.style.axisLabelColour + ';'
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
                    axis.container = $('<div class="axis container" id="' + that.id + '"></div>');
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

                var rotation = "";
                if (that.style.markerLabelRotation !== undefined) {
                    rotation = '-webkit-transform: rotate(' + that.style.markerLabelRotation + 'deg);'
                            + '-moz-transform: rotate(' + that.style.markerLabelRotation + 'deg);'
                            + '-ms-transform: rotate(' + that.style.markerLabelRotation + 'deg);'
                            + '-o-transform: rotate(' + that.style.markerLabelRotation + 'deg);'
                            + 'transform: rotate(' + that.style.markerLabelRotation + 'deg);';
                }

                return $('<div class="' + that.id + '-marker-label"'
                       + 'style="position:absolute;'
                       + 'font-family: ' + that.style.fontFamily + ';'
                       + 'font-size:' + that.style.markerLabelFontSize + ';'
                       + 'color:' + that.style.markerLabelColour + ';'
                       + rotation
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

                return $('<div class="' + that.id + 'major-marker"'
                    + 'style="position:absolute;'
                    + 'color:' + that.style.majorMarkerColour + ';'
                    + that.position + ":" + (-that.style.majorMarkerLength) + "px;"
                    + lengthDim + ":" + that.style.majorMarkerLength + "px;"
                    + 'border-' + positionDir + ":" + that.style.majorMarkerWidth + "px solid" + that.style.majorMarkerColour + ";"
                    + positionDir + ":" + (marker.pixel - that.style.majorMarkerWidth*0.5) + "px;"
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
                        markerLabelCSS[that.position] = (-markerLabel.height() - (that.style.majorMarkerLength + MARKER_LABEL_SPACING)) + "px";
                        // centre label under tick
                        markerLabelCSS.left = (markers[i].pixel - (markerLabel.width()*0.5)) +"px";
                        labelLength = markerLabel.height();
                    }
                    else {
                        // get label position
                        markerLabelCSS[that.position] = (-markerLabel.width() - (that.style.majorMarkerLength + MARKER_LABEL_SPACING)) + "px";
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
                            + that.style.majorMarkerLength
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

            if (!this.isXAxis && this.projection === 'EPSG:900913') {
                // find the gudermannian value where this current linear value is
                value = AxisUtil.scaleLinearToGudermannian( value, this );
            }

            // return axis value, rollover if necessary
            return AxisUtil.formatText( AxisUtil.getMarkerRollover( this, value ), this.unitSpec );
        }

    });

    return Axis;
});
