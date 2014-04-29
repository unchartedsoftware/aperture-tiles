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
        MARKER_LABEL_SPACING = 5,
        AXIS_LABEL_SPACING = 10,
        Axis;



    Axis = Class.extend({
        /**
         * Construct an axis
         * @param spec Axis specification object:
         */
        init: function (spec) {

            var that = this,
                temp,
                defaults = {
                    title : "",
                    position : "bottom",
                    repeat: false,
                    intervalSpec : {
                        type: "percentage", // or "fixed"
                        increment: 10,
                        pivot: 0,
                        allowScaleByZoom: true
                    },
                    unitSpec : {
                        type: 'decimal',
                        divisor: 1000,
                        decimals: 2,
                        allowStepDown: true
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
            this.map = spec.map;

            this.position = spec.position || defaults.position;
            this.id = spec.id || this.parentId + "-" + this.position + "-axis";

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

            // generate more attributes
            this.isXAxis = (this.position === 'top' || this.position === 'bottom');
            this.axisWidthOrHeight = this.isXAxis ? "width" : "height";
            this.markerWidthOrHeight = this.isXAxis ? "height" : "width";
            this.leftOrTop = this.isXAxis ? "left" : "top";
            this.horizontalOrVertical = (this.isXAxis) ? 'horizontal' : 'vertical';
            this.maxLabelLength = 0;

            this.map.on('mousemove', function(event) {
                that.redraw();
            });

            /* 'mousemove' triggers on zoom, so this is unnecessary
            this.map.on('zoomend', function(event) {
                that.redraw();
            });
            */

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

                var rotation = "";

                if (!that.isXAxis) {
                    if (that.position === "left") {
                        rotation = "rotate(" + (-90) + "deg)";
                    } else {
                        rotation = "rotate(" + 90 + "deg)";
                    }
                }

                return $('<div class="axis-title-label"'
                    + 'style="position:absolute;'
                    + that.leftOrTop + ':' + (that.axisLength*0.5) + 'px;'
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
                    axis.container = $('<div class="axis-container" id="' + that.id + '"></div>');
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

                return $('<div class="axis-marker-label ' + that.horizontalOrVertical + '-axis-marker-label"'
                       +    'style="position:absolute;">'
                       +    AxisUtil.formatText( marker.label, that.unitSpec )
                       + '</div>');
            }

            /**
             * Creates and returns a major marker element with proper CSS
             */
            function createMajorMarker(marker) {

                var axisClass = (that.isXAxis) ? 'horizontal' : 'vertical';

                return $('<div class="' + axisClass + '-axis-marker ' + that.position + '-axis"'
                       +    'style="position:absolute;">'
                       + '</div>');
            }

            /**
             * Creates the axis marker elements with proper CSS
             */
            function addAxisMarkerElements() {

                var majorMarker,
                    markerLabel,
                    markerLength,
                    markerWidth,
                    labelLength,
                    labelOffset,
                    markerLabelCSS = {},
                    markerCSS = {},
                    i;

                for( i = 0; i < markers.length; i++ ) {

                    // create a major marker
                    majorMarker = createMajorMarker(markers[i]);

                    // create marker label
                    markerLabel = createMarkerLabel(markers[i]);

                    // append marker to axis container
                    // append here to query the width and height
                    axis.container.append(majorMarker);
                    axis.container.append(markerLabel);

                    // get marker length and width
                    markerLength = majorMarker[that.markerWidthOrHeight]();
                    markerWidth = majorMarker.css("border-"+that.leftOrTop+"-width").replace('px', ''); // strip px suffix

                    // get label position
                    markerLabelCSS[that.position] = (-markerLabel[that.markerWidthOrHeight]() - (markerLength + MARKER_LABEL_SPACING)) + "px";

                    // centre marker and label
                    markerCSS[that.leftOrTop] = (markers[i].pixel - markerWidth*0.5) + "px";
                    markerLabelCSS[that.leftOrTop] = (markers[i].pixel - (markerLabel[that.axisWidthOrHeight]()*0.5)) +"px";

                    // get the length of the label
                    labelLength = markerLabel[that.markerWidthOrHeight]() + ((that.isXAxis) ? 0 : axis.label.height());

                    // get text alignment
                    markerLabelCSS["text-align"] = (that.isXAxis) ? "left" : ((that.position === "left") ? "right" : "left");

                    // set marker css
                    markerLabel.css(markerLabelCSS);
                    majorMarker.css(markerCSS);
                    // find max label length to position axis title label
                    if (that.maxLabelLength < labelLength) {
                        that.maxLabelLength = labelLength;
                    }
                }

                labelOffset = that.maxLabelLength + 20 // for some reason the spacing is a bit off on the
                            + MARKER_LABEL_SPACING
                            + markerLength
                            + AXIS_LABEL_SPACING
                            + axis.label.height()
                            + ((that.isXAxis) ? 20 : 0);

                // position axis label
                axis.label.css( that.position, -labelOffset + 'px' );
                // add margin space for axis
                axis.marginContainer.css('margin-' + that.position, labelOffset + 'px');
                // div container may change size, this updates properties accordingly
                that.map.updateSize();
            }

            // ensure axis length is correct
            this.axisLength = $("#" + this.parentId).css(this.axisWidthOrHeight).replace('px', ''); // strip px suffix
            // generate array of marker labels and pixel locations
            markers = AxisUtil.getMarkers(this);
            // generate the main axis DOM elements
            addAxisMainElements();
            // add each marker to correct pixel location in axis DOM elements
            addAxisMarkerElements();
        }

    });

    return Axis;
});
