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
                    isOpen : true,
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
                },
                isOpen = (spec.isOpen !== undefined) ? spec.isOpen : defaults.isOpen;

            // enable / disable functions
            function horizontalSlide() {
                that.setEnabled( !that.isEnabled() );
                that.$container.animate({width: 'toggle'});
                that.$containerBorder.animate({width: 'toggle'});
                that.map.redrawAxes();
            }
            function verticalSlide() {
                that.setEnabled( !that.isEnabled() );
                that.$container.animate({height: 'toggle'});
                that.$containerBorder.animate({height: 'toggle'});
                that.map.redrawAxes();
            }
            function generateBorder ( $elem ) {

                // standard css borders will cause an ugly overlap, this creates a separate element that is hidden
                // behind to ensure that the borders are created properly
                var $border = $('<div class="'+ that.position +'-axis-border">'),
                    elemCSS = {
                        position : $elem.css('position'),
                        'border-style' : $elem.css('border-style'),
                        'border-color' : $elem.css('border-color'),
                        'border-width' : $elem.css('border-width'),
                        width : $elem.css('width'),
                        height : $elem.css('height'),
                        top : $elem.css('top'),
                        left : $elem.css('left'),
                        right : $elem.css('right'),
                        bottom : $elem.css('bottom'),
                        'z-index' : $elem.css('z-index') - 1
                    };
                that.$div.append($border);
                $border.css(elemCSS);

                if (that.isXAxis) {
                    $border.height( parseInt(elemCSS.height, 10) - parseInt(elemCSS['border-width'], 10) );
                    $border.css('width', '100%');
                } else {
                    $border.width(  parseInt(elemCSS.width, 10) - parseInt(elemCSS['border-width'], 10) );
                    $border.css('height', '100%');
                }

                // remove border from original element since we created a new one
                $elem.css('border-style', 'none');

                return $border;
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

                return $('<span class="axis-title-label"'
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

                // create axis header and container
                that.$div = $('<div class="'+ that.position +'-axis"></div>');
                that.$header = $('<div class="'+ that.position +'-axis-header">');
                that.$container = $('<div class="'+ that.position +'-axis-container">');

                // set enable / disable callbacks
                if (that.isXAxis) {
                    that.$header.click(verticalSlide);
                    that.$container.click(verticalSlide);
                } else {
                    that.$header.click(horizontalSlide);
                    that.$container.click(horizontalSlide);
                }

                // append axis div to map parent, and other elements tot hat div
                that.$map.parent().append(that.$div);
                that.$div.append(that.$container);
                that.$div.append(that.$header);

                // generate borders
                that.$headerBorder = generateBorder(that.$header);
                that.$containerBorder = generateBorder(that.$container);

                // create new title
                that.$title = generateTitle();
                that.$header.append(that.$title);
            }

            this.mapId = spec.mapId;
            this.$map = $("#" + this.mapId);

            // ensure min is < max
            this.min = spec.min;
            this.max = spec.max;
            this.repeat = spec.repeat || defaults.repeat;
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

            // axis will redraw on map movement
            this.map.on('move', function() {
                that.redraw();
            });

            // generate the core html elements
            generateElements();

            this.enabled = true;
            this.containerWidth = 0;

            this.redraw();

            if ( !isOpen ) {
                // trigger close and skip animation;
                this.$header.click();
                this.$container.finish();
            }
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
             * Creates the axis main div elements with proper CSS
             */
            function setupMainElements() {

                // empty elements
                that.$container.empty();
                // add position offset for vertical axes
                if (!that.isXAxis) {
                    if (that.position === 'left') {
                        that.$title.css(that.leftOrTop, that.axisLength + "px");
                    } else {
                        that.$title.css(that.leftOrTop, -that.axisLength + "px");
                    }
                }
                // add padding for hover hit box
                that.$title.css('padding-left', (that.axisLength*0.5 - that.$title.width()*0.5) + "px");
                that.$title.css('padding-right', (that.axisLength*0.5 - that.$title.width()*0.5) + "px" );

            }

            /**
             * Creates and returns a marker label element with proper CSS
             */
            function createMarkerLabelHTML(marker, css) {

                return $('<div class="axis-marker-label ' + that.horizontalOrVertical + '-axis-marker-label"'
                       + 'style="position:absolute;">'
                       + AxisUtil.formatText( marker.label, that.unitSpec )
                       + '</div>');
            }

            /**
             * Creates and returns a large marker element with proper CSS
             */
            function createLargeMarkerHTML(marker) {

                var axisClass = (that.isXAxis) ? 'horizontal' : 'vertical';

                return '<div class="large-' + axisClass + '-axis-marker ' + that.position + '-axis"'
                       + 'style="position:absolute;'
                       + that.leftOrTop + ":" + (marker.pixel - (that.LARGE_MARKER_WIDTH*0.5)) + 'px;">'
                       + '</div>';
            }

            /**
             * Creates and returns a major marker element with proper CSS
             */
            function createMediumMarkerHTML(marker) {

                var axisClass = (that.isXAxis) ? 'horizontal' : 'vertical';

                return '<div class="medium-' + axisClass + '-axis-marker ' + that.position + '-axis"'
                       + 'style="position:absolute;'
                       + that.leftOrTop + ":" + (marker.pixel - (that.MEDIUM_MARKER_WIDTH*0.5)) + 'px;">'
                       + '</div>';
            }


            /**
             * Creates and returns a major marker element with proper CSS
             */
            function createSmallMarkerHTML(marker) {

                var axisClass = (that.isXAxis) ? 'horizontal' : 'vertical';

                return '<div class="small-' + axisClass + '-axis-marker ' + that.position + '-axis"'
                       + 'style="position:absolute;'
                       + that.leftOrTop + ":" + (marker.pixel - (that.SMALL_MARKER_WIDTH*0.5)) + 'px;">'
                       + '</div>';
            }


            /**
             * This function is used to create temporary elements to determine the required run-time
             * dimensions. This should only be called once as these dimensions will never change
             */
            function calcElementDimensions() {

                var $temp,
                    matrix, values, angle;

                that.LARGE_MARKER_LENGTH = 0;
                that.LARGE_MARKER_WIDTH = 0;
                that.MEDIUM_MARKER_WIDTH = 0;
                that.MEDIUM_MARKER_WIDTH = 0;
                that.ROTATION_RADIANS = 0;

                $temp = $(createLargeMarkerHTML({pixel:0})).hide().appendTo(that.$container);
                that.LARGE_MARKER_LENGTH = $temp[that.markerWidthOrHeight](); //).replace('px', ''); // strip px suffix
                that.LARGE_MARKER_WIDTH = $temp.css("border-"+that.leftOrTop+"-width").replace('px', ''); // strip px suffix
                $temp.remove();

                $temp = $(createMediumMarkerHTML({pixel:0})).hide().appendTo(that.$container);
                that.MEDIUM_MARKER_WIDTH = $temp.css("border-"+that.leftOrTop+"-width").replace('px', ''); // strip px suffix
                $temp.remove();

                $temp = $(createSmallMarkerHTML({pixel:0})).hide().appendTo(that.$container);
                that.SMALL_MARKER_WIDTH = $temp.css("border-"+that.leftOrTop+"-width").replace('px', ''); // strip px suffix
                $temp.remove();

                $temp = createMarkerLabelHTML({label:'temp'}).hide().appendTo(that.$container);
                matrix = $temp.css("-webkit-transform") ||
                    $temp.css("-moz-transform")    ||
                    $temp.css("-ms-transform")     ||
                    $temp.css("-o-transform")      ||
                    $temp.css("transform") || 'none';
                $temp.remove();

                if(matrix !== 'none') {
                    values = matrix.split('(')[1].split(')')[0].split(',');
                    angle = Math.atan2(values[1], values[0]);
                } else {
                    angle = 0;
                }
                that.ROTATION_RADIANS = Math.abs(angle);

                that.elementDimensionsCalculated = true;
            }


            /**
             * Creates the axis marker elements with proper CSS
             */
            function addAxisMarkerElements() {

                var marker,
                    $markerLabel,
                    labelLength,
                    labelOffset,
                    markerLabelCSS = {},
                    markerSize,
                    markersHTML = "",
                    i;

                if (that.elementDimensionsCalculated !== true) {
                    // only call this once per axis
                    calcElementDimensions();
                }

                for ( markerSize in markers) {
                    if (markers.hasOwnProperty(markerSize)) {

                        for( i = 0; i < markers[markerSize].length; i++ ) {

                            marker = markers[markerSize][i];

                            // create a major marker
                            if (markerSize === 'large') {
                                markersHTML += createLargeMarkerHTML(marker);
                            } else if (markerSize === 'medium') {
                                markersHTML += createMediumMarkerHTML(marker);
                            } else {
                                markersHTML += createSmallMarkerHTML(marker);
                            }

                            // only put labels on large markers
                            if (markerSize === 'large') {

                                // create marker label
                                $markerLabel = createMarkerLabelHTML(marker);

                                // append here to query the width and height
                                that.$container.append($markerLabel);
                                // get rotation in radians

                                // get the length of the label, after rotation
                                if (that.isXAxis) {
                                    // get rotated height of the labels and add half to offset
                                    labelLength = $markerLabel.width() * Math.sin(that.ROTATION_RADIANS) + $markerLabel.height() * Math.cos(that.ROTATION_RADIANS);
                                } else {
                                    labelLength = $markerLabel.width() * Math.cos(that.ROTATION_RADIANS) + $markerLabel.height() * Math.sin(that.ROTATION_RADIANS);
                                }

                                // get label position
                                labelOffset = that.LARGE_MARKER_LENGTH + MARKER_LABEL_SPACING;
                                if (that.isXAxis) {
                                    // if x axis, add half of label length as text is anchored from bottom
                                    labelOffset += labelLength * 0.5;
                                }
                                markerLabelCSS[that.oppositePosition] = labelOffset + "px";
                                // set marker label position
                                markerLabelCSS[that.leftOrTop] = (marker.pixel - ($markerLabel[that.axisWidthOrHeight]()*0.5)) +"px";
                                // get text alignment
                                markerLabelCSS["text-align"] = (that.isXAxis) ? "left" : ((that.position === "left") ? "right" : "left");
                                // set marker css
                                $markerLabel.css(markerLabelCSS);

                            }

                        }
                    }
                }

                // append all markers at once
                that.$container.append(markersHTML);

                // div container may change size, this updates properties accordingly
                that.map.updateSize();
            }

            // ensure axis length is correct
            this.axisLength = this.$map.css(this.axisWidthOrHeight).replace('px', ''); // strip px suffix
            // generate array of marker labels and pixel locations
            markers = AxisUtil.getMarkers(this);
            // generate the main axis DOM elements
            setupMainElements();
            // add each marker to correct pixel location in axis DOM elements
            addAxisMarkerElements();
        }

    });

    return Axis;
});
