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
        Axis;



    Axis = Class.extend({
        /**
         * Construct an axis
         * @param spec Axis specification object:
         */
        init: function (spec) {

            var that = this,
                defaults = {
                    title : "Default Axis Title",
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

            // enable / disable functions
            function horizontalSlide() {
                that.setEnabled( !that.isEnabled() );
                that.$container.animate({width: 'toggle'});
                that.map.redrawAxes();
            }
            function verticalSlide() {
                that.setEnabled( !that.isEnabled() );
                that.$container.animate({height: 'toggle'});
                that.map.redrawAxes();
            }

            this.mapId = spec.mapId;
            this.$map = $("#" + this.mapId);

            // ensure min is < max
            this.min = spec.min;
            this.max = spec.max;
            this.map = spec.map;

            this.position = spec.position || defaults.position;
            this.id = spec.id || this.mapId + "-" + this.position + "-axis";

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
            this.oppositePosition = (this.position === 'left') ? 'right' :
                                        (this.position === 'right') ? 'left' :
                                            (this.position === 'top') ? 'bottom' : 'top';

            this.map.on('mousemove', function(event) {
                that.redraw();
            });

            /* 'mousemove' triggers on zoom, so this is unnecessary
            this.map.on('zoomend', function(event) {
                that.redraw();
            });
            */

            // create axis header and container
            this.$header = $('<div class="'+ that.position +'-axis-header">');
            this.$container = $('<div class="'+ that.position +'-axis-container">');

            // set enable / disable callbacks
            if (this.isXAxis) {
                this.$header.click(verticalSlide);
                this.$container.click(verticalSlide);
            } else {
                this.$header.click(horizontalSlide);
                this.$container.click(horizontalSlide);
            }

            this.$map.parent().append(this.$container);
            this.$map.parent().append(this.$header);
            this.enabled = true;
            this.containerWidth = 0;
            this.redraw();
        },


        isEnabled: function() {
            return this.enabled;
        },


        setEnabled: function( enabled ) {
            this.enabled = enabled;
        },


        getMaxContainerWidth: function() {
            var width;
            if (this.isXAxis) {
                width = this.$container.height();
            } else {
                width = this.$container.width();
            }
            if (this.containerWidth < width) {
                this.containerWidth = width;
            }
            return this.containerWidth;
        },


        /**
         * Checks if the mutable spec attributes have changed, if so, redraws
         * that.
         */
        redraw: function() {

            var markers = [],
                that = this;

            if (!this.enabled) {
                return;
            }

            /**
             * Creates and returns the axis label element with proper CSS
             */
            function createAxisLabel() {

                var rotation = "",
                    transformOrigin ="";

                if (!that.isXAxis) {
                    if (that.position === "left") {
                        rotation = "rotate(" + (-90) + "deg)";
                        transformOrigin = "top left";
                    } else {
                        rotation = "rotate(" + 90 + "deg)";
                        transformOrigin = "bottom left";
                    }
                }

                return $('<span class="axis-title-label"'
                    + 'style="position:absolute;'
                    //+ that.leftOrTop + ':' + (that.axisLength*0.5) + 'px;'

                    + '-webkit-transform: ' + rotation + ";"
                    + '-moz-transform: ' + rotation + ";"
                    + '-ms-transform: ' + rotation + ";"
                    + '-o-transform: ' + rotation + ";"
                    + 'transform: ' + rotation + ";"

                    + '-webkit-transform-origin: ' + transformOrigin + ";"
                    + '-moz-transform-origin: ' + transformOrigin + ";"
                    + '-ms-transform-origin: ' + transformOrigin + ";"
                    + '-o-transform-origin: ' + transformOrigin + ";"
                    + 'transform-origin: ' + transformOrigin + ";"

                    + '">' + that.title + '</div>');
            }

            /**
             * Creates the axis main div elements with proper CSS
             */
            function clearMainElements() {

                // empty elements
                that.$container.empty();
                that.$header.empty();

                that.$title = createAxisLabel();
                that.$header.append(that.$title);

                // position header title AFTER it is appended so we can account for its
                // width
                if (!that.isXAxis) {
                    if (that.position === 'left') {
                        that.$title.css(that.leftOrTop, that.$title.width()*0.5 + that.axisLength*0.5);
                    } else {
                        that.$title.css(that.leftOrTop, -that.$title.width()*0.5 + that.axisLength*0.5);
                    }
                } else {
                    that.$title.css(that.leftOrTop, that.axisLength*0.5);
                }
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
             * Creates and returns a large marker element with proper CSS
             */
            function createLargeMarker(marker) {

                var axisClass = (that.isXAxis) ? 'horizontal' : 'vertical';

                return $('<div class="large-' + axisClass + '-axis-marker ' + that.position + '-axis"'
                       +    'style="position:absolute;">'
                       + '</div>');
            }

            /**
             * Creates and returns a major marker element with proper CSS
             */
            function createMediumMarker(marker) {

                var axisClass = (that.isXAxis) ? 'horizontal' : 'vertical';

                return $('<div class="medium-' + axisClass + '-axis-marker ' + that.position + '-axis"'
                    +    'style="position:absolute;">'
                    + '</div>');
            }


            /**
             * Creates and returns a major marker element with proper CSS
             */
            function createSmallMarker(marker) {

                var axisClass = (that.isXAxis) ? 'horizontal' : 'vertical';

                return $('<div class="small-' + axisClass + '-axis-marker ' + that.position + '-axis"'
                    +    'style="position:absolute;">'
                    + '</div>');
            }


            /**
             * Creates the axis marker elements with proper CSS
             */
            function addAxisMarkerElements() {

                var $marker,
                    marker,
                    markerLabel,
                    markerLength,
                    markerWidth,
                    labelLength,
                    labelOffset,
                    markerLabelCSS = {},
                    markerCSS = {},
                    rotation,
                    markerSize,
                    i;

                function getRotationRadians(obj) {
                    var matrix = obj.css("-webkit-transform") ||
                        obj.css("-moz-transform")    ||
                        obj.css("-ms-transform")     ||
                        obj.css("-o-transform")      ||
                        obj.css("transform") || 'none',
                        values, angle;

                    if(matrix !== 'none') {
                        values = matrix.split('(')[1].split(')')[0].split(',');
                        angle = Math.atan2(values[1], values[0]);
                    } else {
                        angle = 0;
                    }
                    return Math.abs(angle);
                }

                for ( markerSize in markers) {
                    if (markers.hasOwnProperty(markerSize)) {

                        for( i = 0; i < markers[markerSize].length; i++ ) {

                            marker = markers[markerSize][i];

                            // create a major marker
                            if (markerSize === 'large') {
                                $marker = createLargeMarker(marker);
                            } else if (markerSize === 'medium') {
                                $marker = createMediumMarker(marker);
                            } else {
                                $marker = createSmallMarker(marker);
                            }

                            // append here to query the width and height
                            that.$container.append($marker);
                            // get marker length and width
                            markerLength = $marker[that.markerWidthOrHeight]();
                            markerWidth = $marker.css("border-"+that.leftOrTop+"-width").replace('px', ''); // strip px suffix
                            // centre marker
                            markerCSS[that.leftOrTop] = (marker.pixel - markerWidth*0.5) + "px";
                            // set marker css
                            $marker.css(markerCSS);

                            if (markerSize === 'large') {
                                // only put labels on large markers
                                // create marker label
                                markerLabel = createMarkerLabel(marker);
                                // append here to query the width and height
                                that.$container.append(markerLabel);
                                // get rotation in radians
                                rotation = getRotationRadians( markerLabel );
                                // get the length of the label, after rotation
                                if (that.isXAxis) {
                                    // get rotated height of the labels and add half to offset
                                    labelLength = markerLabel.width() * Math.sin(rotation) + markerLabel.height() * Math.cos(rotation);
                                } else {
                                    labelLength = markerLabel.width() * Math.cos(rotation) + markerLabel.height() * Math.sin(rotation);
                                }

                                // get label position
                                labelOffset = markerLength + MARKER_LABEL_SPACING;
                                if (that.isXAxis) {
                                    // if x axis, add half of label length as text is anchored from bottom
                                    labelOffset += labelLength * 0.5;
                                }
                                markerLabelCSS[that.oppositePosition] = labelOffset + "px";
                                // set marker label position
                                markerLabelCSS[that.leftOrTop] = (marker.pixel - (markerLabel[that.axisWidthOrHeight]()*0.5)) +"px";
                                // get text alignment
                                markerLabelCSS["text-align"] = (that.isXAxis) ? "left" : ((that.position === "left") ? "right" : "left");
                                // set marker css
                                markerLabel.css(markerLabelCSS);
                            }

                        }
                    }
                }


                // div container may change size, this updates properties accordingly
                that.map.updateSize();
            }

            // ensure axis length is correct

            this.axisLength = this.$map.css(this.axisWidthOrHeight).replace('px', ''); // strip px suffix
            // generate array of marker labels and pixel locations
            markers = AxisUtil.getMarkers(this);
            // generate the main axis DOM elements
            clearMainElements();
            // add each marker to correct pixel location in axis DOM elements
            addAxisMarkerElements();
        }

    });

    return Axis;
});
