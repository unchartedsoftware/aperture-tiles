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
        AXIS_TITLE_CLASS = "axis-title-label",
        AXIS_DIV_CLASS_SUFFIX = "-axis",
        AXIS_HEADER_CLASS = "axis-header",
        AXIS_HEADER_CLASS_SUFFIX = "-axis-header",
        AXIS_CONTENT_CLASS = "axis-content",
        AXIS_CONTENT_CLASS_SUFFIX = "-axis-content",
        AXIS_LABEL_CLASS = "axis-marker-label",
        AXIS_POSITIONED_LABEL_CLASS_SUFFIX = "-axis-marker-label" ,
        AXIS_MARKER_CLASS = "axis-marker",
        AXIS_MARKER_SUFFIX = "-axis-marker",
        AXIS_POSITION_SUFFIX = "-axis",
        Axis;



    Axis = Class.extend({

        Z_INDEX_OFFSET : 2000,

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
                    isOpen : true,
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
                    }
                },
                isOpen = (spec.isOpen !== undefined) ? spec.isOpen : defaults.isOpen;

            // enable / disable functions
            function horizontalSlide() {
                that.setEnabled( !that.isEnabled() );
                that.$content.animate({width: 'toggle'});
                that.map.redrawAxes();
            }
            function verticalSlide() {
                that.setEnabled( !that.isEnabled() );
                that.$content.animate({height: 'toggle'});
                that.map.redrawAxes();
            }
            /**
             * Creates and returns the axis label element with proper CSS
             */
            function generateTitle() {

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
                return $('<span class="'+AXIS_TITLE_CLASS+'"'
                    + 'style="position:absolute;'
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
            function generateElements() {

                var marginLeft = 0,
                    marginRight = 0,
                    marginTop = 0,
                    marginBottom = 0;

                // add margins in case other axis exist, this prevents ugly shadow overlaps
                if (that.isXAxis) {
                    marginLeft = that.$map.find('.left' + AXIS_HEADER_CLASS_SUFFIX ).width() || 0;
                    marginRight = that.$map.find('.right' + AXIS_HEADER_CLASS_SUFFIX ).width() || 0;
                } else {
                    marginTop = that.$map.find('.top' + AXIS_HEADER_CLASS_SUFFIX ).height() || 0;
                    marginBottom = that.$map.find('.bottom' + AXIS_HEADER_CLASS_SUFFIX ).height() || 0;
                }

                // create axis header and container
                that.$div = $('<div class="'+ that.position + AXIS_DIV_CLASS_SUFFIX + '"></div>');
                that.$header = $('<div class="'+ AXIS_HEADER_CLASS + " " + that.position + AXIS_HEADER_CLASS_SUFFIX + '"  style="z-index:'+(that.Z_INDEX+1)+'; margin-left:'+marginLeft+'px; margin-right:'+marginRight+'px; margin-bottom:'+marginBottom+'px; margin-top:'+marginTop+'px;">');
                that.$content = $('<div class="'+ AXIS_CONTENT_CLASS + " " + that.position + AXIS_CONTENT_CLASS_SUFFIX + '"  style="z-index:'+that.Z_INDEX+';">');
                // set enable / disable callbacks
                if (that.isXAxis) {
                    that.$header.click(verticalSlide);
                    that.$content.click(verticalSlide);
                } else {
                    that.$header.click(horizontalSlide);
                    that.$content.click(horizontalSlide);
                }
                // append axis div to map, and other elements to that div
                that.$map.append(that.$div);
                that.$div.append(that.$content);
                that.$div.append(that.$header);
                // create new title
                that.$title = generateTitle();
                that.$header.append(that.$title);
            }

            this.mapId = spec.mapId;
            this.map = spec.map;
            this.$map = this.map.getElement();

            this.min = spec.min;
            this.max = spec.max;
            this.repeat = spec.repeat || defaults.repeat;

            this.Z_INDEX = this.Z_INDEX_OFFSET + this.map.getZIndex();

            this.position = spec.position || defaults.position;
            this.id = spec.id || ( this.mapId + "-" + this.position + "-axis" );

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

            // axis will redraw on map movement
            this.map.on('move', function() {
                that.redraw();
            });

            // generate the core html elements
            generateElements();
            // always set enabled to true, as isOpen attr will trigger a click, which toggles the enabled flag
            this.enabled = true;
            // check if axis starts open or closed
            if ( !isOpen ) {
                // trigger close and skip animation;
                this.$header.click();
                this.$content.finish();
            }

            // allow events to propagate below to map
            this.map.enableEventToMapPropagation( this.$div );

            // draw initial axis
            this.redraw();
        },


        isEnabled: function() {
            return this.enabled;
        },


        setEnabled: function( enabled ) {
            this.enabled = enabled;
        },


        /**
         * Checks if the mutable spec attributes have changed, if so, redraws
         * that.
         */
        redraw: function() {

            var markersBySize = [],
                that = this;

            function updateTitle() {

                // update axis length
                var $title = that.$title,
                    axisLength = that.$map.css( that.axisWidthOrHeight ).replace('px', ''); // strip px suffix
                
                // add position offset for vertical axes
                if (!that.isXAxis) {
                    if (that.position === 'left') {
                        $title.css(that.leftOrTop, axisLength + "px");
                    } else {
                        $title.css(that.leftOrTop, -$title.width()*0.5 + "px");
                    }
                }
                // add padding for hover hit box
                $title.css('padding-left', (axisLength*0.5 - $title.width()*0.5) + "px");
                $title.css('padding-right', (axisLength*0.5 - $title.width()*0.5) + "px" );
            }

            /**
             * Creates and returns a dummy marker label element to measure. This function
             * is used for measuring, as the real label func sizes the labels to the current
             * max measurements
             */
            function createDummyMarkerLabelHTML(marker) {

                return '<div class="' + AXIS_LABEL_CLASS + ' ' + that.horizontalOrVertical + AXIS_POSITIONED_LABEL_CLASS_SUFFIX + '"'
                       + 'style="position:absolute;">'
                       + AxisUtil.formatText( marker.label, that.unitSpec )
                       + '</div>';
            }

            /**
             * Creates and returns a marker label element with proper CSS
             */
            function createMarkerLabelHTML(marker) {

                var SPACING_BETWEEN_MARKER_AND_LABEL = 5,
                    primaryPosition,
                    secondaryPosition;

                if (that.isXAxis) {
                    // if x axis, add half of label length as text is anchored from bottom
                    primaryPosition = marker.pixel - that.MAX_LABEL_UNROTATED_WIDTH*0.5;
                    secondaryPosition =  that.LARGE_MARKER_LENGTH
                        + SPACING_BETWEEN_MARKER_AND_LABEL;
                } else {
                    primaryPosition = marker.pixel - that.MAX_LABEL_HEIGHT*0.5;
                    secondaryPosition =  that.LARGE_MARKER_LENGTH
                        + SPACING_BETWEEN_MARKER_AND_LABEL;
                }

                return '<div class="' + AXIS_LABEL_CLASS + ' ' + that.horizontalOrVertical + AXIS_POSITIONED_LABEL_CLASS_SUFFIX + '"'
                    + 'style="position:absolute;'
                    + 'text-align: center; '    // center text horizontally
                    + 'width: ' + that.MAX_LABEL_WIDTH + 'px;'
                    + 'height: ' + that.MAX_LABEL_HEIGHT + 'px;'
                    + 'line-height: ' + that.MAX_LABEL_HEIGHT + 'px;'   // center text vertically
                    + that.leftOrTop + ":" + primaryPosition + 'px;'
                    + that.oppositePosition + ":" + secondaryPosition + 'px;">'
                    + AxisUtil.formatText( marker.label, that.unitSpec )
                    +'</div>';
            }

            /**
             * Creates and returns a large marker element with proper CSS
             */
            function createLargeMarkerHTML(marker) {

                return '<div class="' + AXIS_MARKER_CLASS + ' large-' + that.horizontalOrVertical + AXIS_MARKER_SUFFIX + ' ' + that.position + AXIS_POSITION_SUFFIX + '"'
                       + 'style="position:absolute;'
                       + that.leftOrTop + ":" + (marker.pixel - that.LARGE_MARKER_HALF_WIDTH) + 'px;">'
                       + '</div>';
            }

            /**
             * Creates and returns a major marker element with proper CSS
             */
            function createMediumMarkerHTML(marker) {

                return '<div class="' + AXIS_MARKER_CLASS + ' medium-' + that.horizontalOrVertical + AXIS_MARKER_SUFFIX + ' ' + that.position + AXIS_POSITION_SUFFIX + '"'
                       + 'style="position:absolute;'
                       + that.leftOrTop + ":" + (marker.pixel - that.MEDIUM_MARKER_HALF_WIDTH) + 'px;">'
                       + '</div>';
            }


            /**
             * Creates and returns a major marker element with proper CSS
             */
            function createSmallMarkerHTML(marker) {

                return '<div class="' + AXIS_MARKER_CLASS + ' small-' + that.horizontalOrVertical + AXIS_MARKER_SUFFIX + ' ' + that.position + AXIS_POSITION_SUFFIX + '"'
                       + 'style="position:absolute;'
                       + that.leftOrTop + ":" + (marker.pixel - that.SMALL_MARKER_HALF_WIDTH) + 'px;">'
                       + '</div>';
            }


            /**
             * This function is used to create temporary elements to determine the required run-time
             * dimensions. This should only be called once as these dimensions will never change
             */
            function calcElementDimensions() {

                var $temp;

                function measureLabelRotation( $label ) {

                    var matrix, values, angle;
                    matrix = $label.css("-webkit-transform") ||
                             $label.css("-moz-transform")    ||
                             $label.css("-ms-transform")     ||
                             $label.css("-o-transform")      ||
                             $label.css("transform") || 'none';
                    if(matrix !== 'none') {
                        values = matrix.split('(')[1].split(')')[0].split(',');
                        angle = Math.atan2(values[1], values[0]);
                    } else {
                        angle = 0;
                    }
                    that.ROTATION_RADIANS = Math.abs(angle);
                }

                function measureLabelMaxDimensions( $label ) {

                    var sinW = $label.width() * Math.sin(that.ROTATION_RADIANS),
                        sinH = $label.height() * Math.sin(that.ROTATION_RADIANS),
                        cosW = $label.width() * Math.cos(that.ROTATION_RADIANS),
                        cosH = $label.height() * Math.cos(that.ROTATION_RADIANS);

                    that.MAX_LABEL_WIDTH = Math.max( sinH + cosW, that.MAX_LABEL_WIDTH );
                    that.MAX_LABEL_HEIGHT = Math.max( cosH + sinW, that.MAX_LABEL_HEIGHT );
                    that.MAX_LABEL_UNROTATED_WIDTH = $label.width();
                }

                // initialized all measurements to zero
                that.LARGE_MARKER_LENGTH = 0;
                that.LARGE_MARKER_HALF_WIDTH = 0;
                that.MEDIUM_MARKER_HALF_WIDTH = 0;
                that.SMALL_MARKER_HALF_WIDTH = 0;
                that.MAX_LABEL_WIDTH = 0;
                that.MAX_LABEL_HEIGHT = 0;
                that.MAX_LABEL_UNROTATED_WIDTH = 0;
                that.ROTATION_RADIANS = 0;

                // measure large markers
                $temp = $(createLargeMarkerHTML({pixel:0})).hide().appendTo(that.$content);
                that.LARGE_MARKER_LENGTH = $temp[that.markerWidthOrHeight]();
                that.LARGE_MARKER_HALF_WIDTH = Math.floor( $temp[that.axisWidthOrHeight]()*0.5 );
                $temp.remove();
                // measure medium markers
                $temp = $(createMediumMarkerHTML({pixel:0})).hide().appendTo(that.$content);
                that.MEDIUM_MARKER_HALF_WIDTH = Math.floor( $temp[that.axisWidthOrHeight]() * 0.5);
                $temp.remove();
                // measure small markers
                $temp = $(createSmallMarkerHTML({pixel:0})).hide().appendTo(that.$content);
                that.SMALL_MARKER_HALF_WIDTH = Math.floor( $temp[that.axisWidthOrHeight]() * 0.5);
                $temp.remove();

                // label measurements
                $temp = $(createDummyMarkerLabelHTML({pixel:0, label:that.max })).appendTo(that.$content);
                // get angle first, it is used in label measurements
                measureLabelRotation( $temp );
                // measure max label
                measureLabelMaxDimensions( $temp );
                $temp.remove();
                // measure min label
                $temp = $(createDummyMarkerLabelHTML({pixel:0, label:that.min})).appendTo(that.$content);
                measureLabelMaxDimensions( $temp );
                $temp.remove();

                // set flag so these do not need to be calculated again
                that.elementDimensionsCalculated = true;
            }


            /**
             * Creates the axis marker elements with proper CSS
             */
            function addAxisMarkerElements() {

                var marker,
                    markers,
                    markerSize,
                    markersHTML = "",
                    i;

                if (that.elementDimensionsCalculated !== true) {
                    // only call this once per axis
                    calcElementDimensions();
                }

                // iterate through markers, by marker type
                for ( markerSize in markersBySize ) {
                    if (markersBySize.hasOwnProperty(markerSize)) {

                        markers = markersBySize[markerSize];

                        for (i = 0; i < markers.length; i++) {

                            marker = markers[i];

                            switch (markerSize) {
                                case 'large':
                                    markersHTML += createLargeMarkerHTML(marker);
                                    markersHTML += createMarkerLabelHTML(marker);
                                    break;
                                case 'medium':
                                    markersHTML += createMediumMarkerHTML(marker);
                                    break;
                                default:
                                    markersHTML += createSmallMarkerHTML(marker);
                                    break;
                            }

                        }
                    }
                }
                // append all markers and labels at once
                that.$content[0].innerHTML = markersHTML;
            }

            // always update title position (in case of window resize)
            updateTitle();

            // exit early if no markers are visible
            if ( !this.isEnabled() ) {
                return;
            }

            // empty elements of axis container
            that.$content.empty();
            // generate array of marker labels and pixel locations
            markersBySize = AxisUtil.getMarkers(this);
            // add each marker to correct pixel location in axis DOM elements
            addAxisMarkerElements();
        }

    });

    return Axis;
});
