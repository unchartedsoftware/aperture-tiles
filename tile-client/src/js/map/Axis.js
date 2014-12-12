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

/**
 * A map axis object that will attach to a map edge and display coordinates based on
 * supplied interval and unit specification.
 */
( function() {

    "use strict";

    var Util = require('../util/Util'),
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
        SPACING_BETWEEN_MARKER_AND_LABEL = 5,
        Z_INDEX = 2001;

    /**
     * Private: Creates and returns a dummy marker label element to measure. This function
     * is used for measuring, as the real label func sizes the labels to the current
     * max measurements
     *
     * @param axis   {Axis}     the axis object.
     * @param marker {Object} the marker object.
     */
    function createDummyMarkerLabelHTML( axis, marker ) {
        return '<div class="' + AXIS_LABEL_CLASS
            + ' ' + axis.horizontalOrVertical + AXIS_POSITIONED_LABEL_CLASS_SUFFIX + '"'
            + 'style="position:absolute;">'
            + AxisUtil.formatText( marker.label, axis.units )
            + '</div>';
    }

    /**
     * Private: Creates and returns a marker label element with proper CSS
     *
     * @param axis   {Axis}     the axis object.
     * @param marker {Object} the marker object.
     */
    function createMarkerLabelHTML( axis, marker ) {
        var primaryPosition,
            secondaryPosition;
        if ( axis.isXAxis ) {
            // if x axis, add half of label length as text is anchored from bottom
            primaryPosition = marker.pixel - axis.MAX_LABEL_UNROTATED_WIDTH*0.5;
            secondaryPosition =  axis.LARGE_MARKER_LENGTH
                + SPACING_BETWEEN_MARKER_AND_LABEL;
        } else {
            primaryPosition = marker.pixel - axis.MAX_LABEL_HEIGHT*0.5;
            secondaryPosition =  axis.LARGE_MARKER_LENGTH
                + SPACING_BETWEEN_MARKER_AND_LABEL;
        }
        return '<div class="' + AXIS_LABEL_CLASS + ' '
            + axis.horizontalOrVertical + AXIS_POSITIONED_LABEL_CLASS_SUFFIX + '"'
            + 'style="position:absolute;'
            + 'text-align: center; '    // center text horizontally
            + 'width: ' + axis.MAX_LABEL_WIDTH + 'px;'
            + 'height: ' + axis.MAX_LABEL_HEIGHT + 'px;'
            + 'line-height: ' + axis.MAX_LABEL_HEIGHT + 'px;'   // center text vertically
            + axis.leftOrTop + ":" + primaryPosition + 'px;'
            + axis.oppositePosition + ":" + secondaryPosition + 'px;">'
            + AxisUtil.formatText( marker.label, axis.units )
            +'</div>';
    }

    /**
     * Private: Creates and returns a large marker element with proper CSS
     *
     * @param axis   {Axis}     the axis object.
     * @param marker {Object} the marker object.
     */
    function createLargeMarkerHTML( axis, marker ) {
        return '<div class="' + AXIS_MARKER_CLASS
            + ' large-' + axis.horizontalOrVertical + AXIS_MARKER_SUFFIX
            + ' ' + axis.position + AXIS_POSITION_SUFFIX + '"'
            + 'style="position:absolute;'
            + axis.leftOrTop + ":" + (marker.pixel - axis.LARGE_MARKER_HALF_WIDTH) + 'px;">'
            + '</div>';
    }

    /**
     * Private: Creates and returns a major marker element with proper CSS
     *
     * @param axis   {Axis}     the axis object.
     * @param marker {Object} the marker object.
     */
    function createMediumMarkerHTML( axis, marker ) {
        return '<div class="' + AXIS_MARKER_CLASS
            + ' medium-' + axis.horizontalOrVertical + AXIS_MARKER_SUFFIX
            + ' ' + axis.position + AXIS_POSITION_SUFFIX + '"'
            + 'style="position:absolute;'
            + axis.leftOrTop + ":" + (marker.pixel - axis.MEDIUM_MARKER_HALF_WIDTH) + 'px;">'
            + '</div>';
    }

    /**
     * Private: Creates and returns a major marker element with proper CSS
     *
     * @param axis   {Axis}     the axis object.
     * @param marker {Object} the marker object.
     */
    function createSmallMarkerHTML( axis, marker ) {
        return '<div class="' + AXIS_MARKER_CLASS
            + ' small-' + axis.horizontalOrVertical + AXIS_MARKER_SUFFIX
            + ' ' + axis.position + AXIS_POSITION_SUFFIX + '"'
            + 'style="position:absolute;'
            + axis.leftOrTop + ":" + (marker.pixel - axis.SMALL_MARKER_HALF_WIDTH) + 'px;">'
            + '</div>';
    }

    /**
     * Private: This function is used to create temporary elements to determine the required run-time
     * dimensions. This is only be called once per axis as these dimensions will never change.
     *
     * @param axis{Axis} the axis object.
     */
    function calcElementDimensions( axis ) {
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
            axis.ROTATION_RADIANS = Math.abs(angle);
        }
        function measureLabelMaxDimensions( $label ) {
            var sinW = $label.width() * Math.sin(axis.ROTATION_RADIANS),
                sinH = $label.height() * Math.sin(axis.ROTATION_RADIANS),
                cosW = $label.width() * Math.cos(axis.ROTATION_RADIANS),
                cosH = $label.height() * Math.cos(axis.ROTATION_RADIANS);
            axis.MAX_LABEL_WIDTH = Math.max( sinH + cosW, axis.MAX_LABEL_WIDTH );
            axis.MAX_LABEL_HEIGHT = Math.max( cosH + sinW, axis.MAX_LABEL_HEIGHT );
            axis.MAX_LABEL_UNROTATED_WIDTH = Math.max( $label.width(), axis.MAX_LABEL_UNROTATED_WIDTH );
        }
        // initialized all measurements to zero
        axis.LARGE_MARKER_LENGTH = 0;
        axis.LARGE_MARKER_HALF_WIDTH = 0;
        axis.MEDIUM_MARKER_HALF_WIDTH = 0;
        axis.SMALL_MARKER_HALF_WIDTH = 0;
        axis.MAX_LABEL_WIDTH = 0;
        axis.MAX_LABEL_HEIGHT = 0;
        axis.MAX_LABEL_UNROTATED_WIDTH = 0;
        axis.ROTATION_RADIANS = 0;
        axis.HEADER_WIDTH = axis.isXAxis ? axis.$header.height() : axis.$header.width();
        // measure large markers
        $temp = $(createLargeMarkerHTML( axis, {pixel:0} )).hide().appendTo(axis.$content);
        axis.LARGE_MARKER_LENGTH = $temp[axis.markerWidthOrHeight]();
        axis.LARGE_MARKER_HALF_WIDTH = Math.floor( $temp[axis.axisWidthOrHeight]()*0.5 );
        $temp.remove();
        // measure medium markers
        $temp = $(createMediumMarkerHTML( axis, {pixel:0} )).hide().appendTo(axis.$content);
        axis.MEDIUM_MARKER_HALF_WIDTH = Math.floor( $temp[axis.axisWidthOrHeight]() * 0.5);
        $temp.remove();
        // measure small markers
        $temp = $(createSmallMarkerHTML( axis, {pixel:0} )).hide().appendTo(axis.$content);
        axis.SMALL_MARKER_HALF_WIDTH = Math.floor( $temp[axis.axisWidthOrHeight]() * 0.5);
        $temp.remove();
        // label measurements
        $temp = $(createDummyMarkerLabelHTML( axis, {pixel:0, label:axis.max } )).appendTo(axis.$content);
        // get angle first, it is used in label measurements
        measureLabelRotation( $temp );
        // measure max label
        measureLabelMaxDimensions( $temp );
        $temp.remove();
        // measure min label
        $temp = $(createDummyMarkerLabelHTML( axis, {pixel:0, label:axis.min} )).appendTo(axis.$content);
        measureLabelMaxDimensions( $temp );
        $temp.remove();
    }

    /**
     * Private: Creates and returns the axis label element with proper CSS.
     *
     * @param axis {Axis} the axis object.
     */
    function createTitle( axis ) {
        var rotation = "",
            transformOrigin ="";
        if ( !axis.isXAxis ) {
            if (axis.position === "left") {
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
            + '">' + axis.title + '</div>');
    }

    /**
     * Private: Creates and returns the axis header jquery object.
     *
     * @param axis {Axis} the axis object.
     */
    function createHeader( axis ) {
        var marginLeft = 0,
            marginRight = 0,
            marginTop = 0,
            marginBottom = 0;
        // add margins in case other axis exist, this prevents an ugly border overlap
        if ( axis.isXAxis ) {
            marginLeft = axis.$map.find('.left' + AXIS_HEADER_CLASS_SUFFIX ).width() || 0;
            marginRight = axis.$map.find('.right' + AXIS_HEADER_CLASS_SUFFIX ).width() || 0;
        } else {
            marginTop = axis.$map.find('.top' + AXIS_HEADER_CLASS_SUFFIX ).height() || 0;
            marginBottom = axis.$map.find('.bottom' + AXIS_HEADER_CLASS_SUFFIX ).height() || 0;
        }
        return $('<div class="'+ AXIS_HEADER_CLASS + " " + axis.position + AXIS_HEADER_CLASS_SUFFIX + '"'
               + 'style="z-index:'+(Z_INDEX+2)+';'
               + 'margin:'+marginTop+'px '+marginRight+'px '+marginBottom+'px '+marginLeft+'px;"></div>');
    }


    /**
     * Private: Creates and returns the axis header background jquery object. This is used
     * to apply a box-shadow css without ugly overlap.
     *
     * @param axis {Axis} the axis object.
     */
    function createHeaderBack( axis ) {
        return $('<div class="'+ AXIS_HEADER_CLASS + " " + AXIS_HEADER_CLASS + "-back " + axis.position + AXIS_HEADER_CLASS_SUFFIX + '"'
               + 'style="z-index:'+(Z_INDEX+1)+';"></div>' );
    }


    /**
     * Private: Creates and returns the empty axis content jquery object.
     *
     * @param axis {Axis} the axis object.
     */
    function createContent( axis ) {
        return $('<div class="'+ AXIS_CONTENT_CLASS
               + " " + axis.position + AXIS_CONTENT_CLASS_SUFFIX
               + '"  style="z-index:'+Z_INDEX+';"></div>');
    }


    /**
     * Private: Creates and returns the axis parent div jquery object.
     *
     * @param axis {Axis} the axis object.
     */
    function createAxis( axis ) {
        var $axis,
            enableSlide,
            disableSlide,
            horizontalSlide,
            verticalSlide;
        enableSlide = function() {
            // set enable / disable callbacks
            if (axis.isXAxis) {
                axis.$header.click(verticalSlide);
                axis.$content.click(verticalSlide);
            } else {
                axis.$header.click(horizontalSlide);
                axis.$content.click(horizontalSlide);
            }
        };
        disableSlide = function() {
            axis.$header.off('click');
            axis.$content.off('click');
        };
        horizontalSlide = function() {
            axis.setEnabled( !axis.isEnabled() );
            axis.setContentDimension();
            disableSlide();
            axis.$content.animate({width: 'toggle'}, {duration: 300, complete: function(){ enableSlide();} });
            axis.redraw();
        };
        verticalSlide = function() {
            axis.setEnabled( !axis.isEnabled() );
            axis.setContentDimension();
            disableSlide();
            axis.$content.animate({height: 'toggle'}, {duration: 300, complete: function(){ enableSlide();} });
            axis.redraw();
        };
        // create axis title, header, and container and append them to root
        axis.$title = createTitle( axis );
        axis.$header = createHeader( axis ).append( axis.$title );
        axis.$content = createContent( axis );
        $axis = $('<div class="'+ axis.position + AXIS_DIV_CLASS_SUFFIX + '"></div>')
                    .append( axis.$content )
                        .append( axis.$header )
                            .append( createHeaderBack( axis ) );
        // enable callbacks
        enableSlide();
        // return root
        return $axis;
    }

    /**
     * Private: Updates the positon of the axis title of its size changes.
     *
     * @param axis {Axis} the axis object.
     */
    function updateAxisTitle( axis ) {
        // update axis length
        var $title = axis.$title,
            axisLength = axis.$map.css( axis.axisWidthOrHeight ).replace('px', ''), // strip px suffix
            padding;
        // add position offset for vertical axes
        if ( !axis.isXAxis ) {
            if (axis.position === 'left') {
                $title.css( axis.leftOrTop, axisLength + "px" );
            } else {
                $title.css( axis.leftOrTop, -$title.width()*0.5 + "px" );
            }
        }
        // calc padding
        padding = (axisLength*0.5 - $title.width()*0.5);
        // add padding for hover hit box
        $title.css('padding-left', padding + "px");
        $title.css('padding-right', padding + "px" );
    }

    /**
     * Private: Creates the axis marker elements and appends them to the content div.
     *
     * @param axis {Axis} the axis object.
     */
    function updateAxisContent( axis ) {
        var markers,
            markersHTML = "",
            markersBySize,
            i;
        // generate array of marker labels and pixel locations
        markersBySize = AxisUtil.getMarkers( axis );
        // large markers
        markers = markersBySize.large;
        for (i = 0; i < markers.length; i++) {
            markersHTML += createLargeMarkerHTML( axis, markers[i] );
            markersHTML += createMarkerLabelHTML( axis, markers[i] );
        }
        // medium markers
        markers = markersBySize.medium;
        for (i = 0; i < markers.length; i++) {
            markersHTML += createMediumMarkerHTML( axis, markers[i] );
        }
        // small markers
        markers = markersBySize.small;
        for (i = 0; i < markers.length; i++) {
            markersHTML += createSmallMarkerHTML( axis, markers[i] );
        }
        // append all markers and labels at once
        axis.$content[0].innerHTML = markersHTML;
    }

    /**
     * Private: Returns the draw callback function on map 'move' event.
     *
     * @param axis {Axis} The axis object.
     */
    function redrawCallback( axis ) {
        return function() {
            axis.redraw();
        };
    }

    /**
     * Private: Returns the mouse marker callback function on 'mousemove' event.
     *
     * @param axis {Axis} The axis object.
     */
    function mouseMoveCallback( axis ) {
        return function( event ) {
            if ( !axis.enabled ) {
                return;
            }
            var marker = AxisUtil.getMarker( axis, event.xy.x, event.xy.y );
            axis.$content.find( '.mouse-marker' ).remove();
            axis.$content.append( $( createLargeMarkerHTML( axis, marker ) ).addClass( 'mouse-marker' ) );
        };
    }

    /**
     * Instantiate an Axis object.
     *
     * @param spec {Object} The specfication object:
     * {
     *     position {String}  Set the position to the bottom of the map. Default = "bottom"
     *     title    {String}  Set the title of the axis label. Default = "Axis"
     *     isOpen   {boolean} Have the axis initialize to an open or closed state. Default = true
     *     repeat   {boolean} Whether or not the axis repeats. Default = false
     *     intervals: {
     *         type        {String}  Whether the intervals are by "percentage" or by "value". Default = "percentage"
     *         increment   {number}  The interval increment in. Default = 10
     *         pivot       {number}  The value from with increments are generated from. Default = 0
     *         scaleByZoom {boolean} Whether the increments should be scaled by zoom level. Default = true
     *     }
     *     units: {
     *         type     {String}  The type of unit, ["integer", "decimal", "thousands", "millions", "billions", "degrees"]. Default = "decimal"
     *         decimals {number}  The number of decimals to display, if applicable. Default = 2
     *         stepDown {boolean} Whether values should step down by unit type, if applicable. Default = true
     *     }
     * }
     */
    function Axis( spec ) {

        this.position = ( spec.position !== undefined ) ? spec.position.toLowerCase() : 'bottom';
        this.repeat = ( spec.repeat !== undefined ) ? spec.repeat : false;
        this.title = spec.title || 'Axis';
        this.isOpen = ( spec.isOpen !== undefined ) ? spec.isOpen : true;

        spec.intervals = spec.intervals || {};
        this.intervals = {};
        this.intervals.type = ( spec.intervals.type !== undefined ) ? spec.intervals.type.toLowerCase() : 'percentage';
        this.intervals.increment = spec.intervals.increment || 10;
        this.intervals.pivot = ( spec.intervals.pivot !== undefined ) ? spec.intervals.pivot : 0;
        this.intervals.scaleByZoom = ( spec.intervals.scaleByZoom !== undefined ) ? spec.intervals.scaleByZoom : true;

        spec.units = spec.units || {};
        this.units = {};
        this.units.type = ( spec.units.type !== undefined ) ? spec.units.type.toLowerCase() : 'decimal';
        this.units.decimals = spec.units.decimals || 2;
        this.units.stepDown = ( spec.units.stepDown !== undefined ) ? spec.units.stepDown : true;

        // generate more attributes
        this.isXAxis = ( this.position === 'top' || this.position === 'bottom' );
        this.axisWidthOrHeight = this.isXAxis ? "width" : "height";
        this.markerWidthOrHeight = this.isXAxis ? "height" : "width";
        this.leftOrTop = this.isXAxis ? "left" : "top";
        this.horizontalOrVertical = (this.isXAxis) ? 'horizontal' : 'vertical';
        this.oppositePosition = (this.position === 'left') ? 'right' :
                                    (this.position === 'right') ? 'left' :
                                        (this.position === 'top') ? 'bottom' : 'top';
    }

    Axis.prototype.activate = function() {
        // create unique callbacks so they can be removed later
        this.redrawCallback = redrawCallback( this );
        this.mouseMoveCallback = mouseMoveCallback( this );
        // attach callbacks
        this.map.on( 'move', this.redrawCallback );
        this.map.on( 'mousemove', this.mouseMoveCallback );
        // generate the core html elements
        this.$map = $( this.map.getElement() );
        this.$axis = createAxis( this );
        this.$map.append( this.$axis );
        // always set enabled to true, as isOpen attr will trigger a click, which toggles the enabled flag
        this.enabled = true;
        // calculate the dimensions of the individual elements once
        calcElementDimensions( this );
        // check if axis starts open or closed
        if ( !this.isOpen ) {
            // trigger close and skip animation;
            this.$header.click();
            this.$content.finish();
        }
        // allow events to propagate below to map except 'click'
        Util.enableEventPropagation( this.$axis );
        Util.disableEventPropagation( this.$axis, ['onclick', 'ondblclick'] );
    };

    Axis.prototype.deactivate = function() {
        this.map.off( 'move', this.redrawCallback );
        this.map.off( 'mousemove', this.mouseMoveCallback );
        this.$axis.remove();
        this.$axis = null;
        this.$title = null;
        this.$header = null;
        this.$content = null;
        this.redrawCallback = null;
        this.mouseMoveCallback = null;
    };

    /**
     *  Returns true if the axis is currently enabled, false if not
     */
    Axis.prototype.isEnabled = function() {
        return this.enabled;
    };

    /**
     *  Enable or disable the axis
     */
    Axis.prototype.setEnabled = function( enabled ) {
        this.enabled = enabled;
    };

    /**
     *  Returns the dimension of the content div of the axis
     */
    Axis.prototype.getContentDimension = function() {
        var dim = this.isXAxis ? this.MAX_LABEL_HEIGHT : this.MAX_LABEL_WIDTH;
        return dim + SPACING_BETWEEN_MARKER_AND_LABEL*2 + this.LARGE_MARKER_LENGTH + this.HEADER_WIDTH;
    };

    /**
     * Iterates over all axes on the map, determines the max content size, and sets the content dimension
     * to that size.
     */
    Axis.prototype.setContentDimension = function() {
        var dim = this.isXAxis ? 'height' : 'width',
            maxAxisLabelDim = 0;
        _.forIn( this.map.axes, function( value ) {
            maxAxisLabelDim = Math.max( value.getContentDimension() || 0, maxAxisLabelDim );
        });
        this.$content[ dim ]( maxAxisLabelDim );
    };

    /**
     * Checks if the mutable spec attributes have changed, if so, redraws
     * that.
     */
    Axis.prototype.redraw = function() {
        // always update title position (in case of window resize)
        updateAxisTitle( this );
        // exit early if no markers are visible
        if ( !this.isEnabled() ) {
            return;
        }
        // add each marker to correct pixel location in axis DOM elements
        updateAxisContent( this );
    };

    module.exports = Axis;
}());
