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
 * A utility namespace containing axis related functionality.
 */
( function() {

    "use strict";

    var Util = require('../util/Util'),
        MapUtil = require('./MapUtil'),
        MARKER_TYPE_ORDER = ['large', 'small', 'medium', 'small'];

    /**
     * Private: Given a value that is outside of the min and max of axis,
     * ensure the values rollover properly
     *
     * @param axis  {Axis}   the axis object.
     * @param value {number} original value.
     */
    function getMarkerRollover( axis, value ) {
        var rollover;
        if ( axis.repeat ) {
            // if repeat enabled ensure label value wraps past min/max properly
            if ( value > axis.max ) {
                rollover = value - axis.max + axis.min;
            } else if ( value < axis.min ) {
                rollover = value + axis.max - axis.min;
            } else {
                rollover = value;
            }
        } else {
            // non-repeat label is always value as there is no wrap around
            rollover = value;
        }
        return rollover;
    }

    /**
     * Private: Returns the pixel position for the value in viewport coordinates.
     *
     * @param axis  {Axis}   axis object.
     * @param value {number} the coordinate value.
     * @returns {int} pixel position
     */
    function getPixelPosition( axis, value ) {
        // given an axis value, get the pixel position on the page
        if ( axis.isXAxis ) {
            return MapUtil.getViewportPixelFromCoord( axis.map, value, 0 ).x;
        }
        return MapUtil.getViewportPixelFromCoord( axis.map, 0, value ).y;
    }

    /**
     * Private: Fills an array of markers by sub increments.
     *
     * @param axis      {Axis}   axis object.
     * @param start     {number} start increment.
     * @param end       {number} end increment
     * @param intervals {Object} intervals specification object.
     * @returns {{large: Array, medium: Array, small: Array}}
     */
    function fillArrayByIncrement( axis, start, end, intervals ) {
        var EPSILON = ( end - start ) * 0.000001,
            subIncrement = intervals.subIncrement,
            startingMarkerTypeIndex = intervals.startingMarkerTypeIndex,
            markers = {
                large: [],
                medium: [],
                small: []
            },
            i = Util.mod( startingMarkerTypeIndex, MARKER_TYPE_ORDER.length ),
            value;
        // reduce sub increment by epsilon to prevent precision errors culling max point
        subIncrement -= EPSILON;

        for ( value=start; value<=end; value+=subIncrement ) {
            markers[ MARKER_TYPE_ORDER[i] ].push({
                label: getMarkerRollover( axis, value ),
                pixel: getPixelPosition( axis, value )
            });
            i = (i + 1) % MARKER_TYPE_ORDER.length;
        }
        return markers;
    }

    /**
     * Private: Returns the minimum visible increment on the axis. Also sets the
     * 'startingMarkerTypeIndex' attribute on the intervals object to distinguish
     * which type of marker to start on.
     *
     * @param axis      {Axis}   axis object.
     * @param intervals {Object} intervals specification object.
     * @returns {number} minimum increment.
     */
    function getMinIncrement( axis, intervals ) {
        var minCull, // exact value of cull point, any value less will be culled from view
            incrementCount; // number of sub increments from the pivot
        if ( axis.isXAxis ) {
            minCull = MapUtil.getCoordFromViewportPixel( axis.map, 0, 0 ).x;
        } else {
            minCull = MapUtil.getCoordFromViewportPixel( axis.map, 0, axis.map.getViewportHeight() ).y;
        }
        if ( !axis.repeat && minCull < axis.min ) {
            // prevent roll-over
            minCull = axis.min;
        }
        // determine how many sub increments from the pivot to the minimum culling point
        incrementCount = Math.floor( ( minCull - intervals.pivot ) / intervals.subIncrement );
        intervals.startingMarkerTypeIndex = incrementCount;
        // return the minimum increment that is still in view
        return intervals.pivot + intervals.subIncrement * incrementCount;
    }

    /**
     * Private: Returns the maximum visible increment on the axis.
     *
     * @param axis      {Axis}   axis object.
     * @param intervals {Object} intervals specification object.
     * @returns {number} maximum increment.
     */
    function getMaxIncrement( axis, intervals ) {
        var maxCull, // exact value of cull point, any value greater will be culled from view
            incrementCount; // number of sub increments from the pivot
        if ( axis.isXAxis ) {
            maxCull = MapUtil.getCoordFromViewportPixel( axis.map, axis.map.getViewportWidth(), 0 ).x;
        } else {
            maxCull = MapUtil.getCoordFromViewportPixel( axis.map, 0, 0 ).y;
        }
        if ( !axis.repeat && maxCull > axis.max ) {
            // prevent roll-over
            maxCull = axis.max;
        }
        // determine how many sub increments from the pivot to the maximum culling point
        incrementCount = Math.floor( ( maxCull - intervals.pivot ) / intervals.subIncrement );
        // return the maximum increment that is still in view
        return intervals.pivot + intervals.subIncrement * incrementCount;
    }

    /**
     * Private: Rounds a value to an integer representation.
     *
     * @param value {number} the value.
     * @returns {int}
     */
    function formatInteger( value ) {
        return Math.round( value );
    }

    /**
     * Private: Rounds a value to an floating point representation with specific decimal places.
     *
     * @param value    {number} the value.
     * @param decimals {int}    the number of decimal places.
     * @returns {string}
     */
    function formatNumber( value, decimals ) {
        return Util.roundToDecimals( value, decimals );
    }

    /**
     * Private: Rounds a value to an floating point representation of thousands with a 'K' appended
     * onto the end.
     *
     * @param value         {number}  the value.
     * @param decimals      {int}     the number of decimal places.
     * @param allowStepDown {boolean} whether or not to step down units if < 1
     * @returns {string}
     */
    function formatThousand( value, decimals, allowStepDown ) {
        var truncValue = value / 1e3;
        if ( allowStepDown && Math.abs( truncValue ) < 1 ) {
            return formatNumber( value, decimals );
        }
        return Util.roundToDecimals( truncValue, decimals ) + 'K';
    }

    /**
     * Private: Rounds a value to an floating point representation of millions with an 'M' appended
     * onto the end.
     *
     * @param value         {number}  the value.
     * @param decimals      {int}     the number of decimal places.
     * @param allowStepDown {boolean} whether or not to step down units if < 1
     * @returns {string}
     */
    function formatMillion( value, decimals, allowStepDown ) {
        var truncValue = value / 1e6;
        if ( allowStepDown && Math.abs( truncValue ) < 1 ) {
            return formatThousand( value, decimals, true );
        }
        return Util.roundToDecimals( truncValue, decimals ) + 'M';
    }

    /**
     * Private: Rounds a value to an floating point representation of billions with a 'B' appended
     * onto the end.
     *
     * @param value         {number}  the value.
     * @param decimals      {int}     the number of decimal places.
     * @param allowStepDown {boolean} whether or not to step down units if < 1
     * @returns {string}
     */
    function formatBillion( value, decimals, allowStepDown ) {
        var truncValue = value / 1e9;
        if ( allowStepDown && Math.abs( truncValue ) < 1 ) {
            return formatMillion( value, decimals, true );
        }
        return Util.roundToDecimals( truncValue, decimals ) + 'B';
    }

    /**
     * Private: Formats a timestamp into a date.
     *
     * @param value {int} unix timestamp.
     * @returns {string}
     */
    function formatTime( value ) {
        var d = new Date( value ); // The 0 there is the key, which sets the date to the epoch
        return d.getFullYear() + "-" + ( d.getMonth() + 1 ) + '-' + d.getDate() + " " +
            "<br>" + ("0" + d.getHours() ).slice(-2) + ":" + ("0" + d.getMinutes() ).slice(-2);
    }

    /**
     * Private: Formats a floating point number and appends a 'degrees' symbol.
     *
     * @param value    {float} degrees value.
     * @param decimals {int}   the number of decimal places.
     * @returns {string}
     */
    function formatDegrees( value, decimals ) {
        return formatNumber( value, decimals ) + "\u00b0";
    }

    module.exports = {

        /**
         * Formats axis marker label text.
         *
         * @param value {number} value of the label
         * @param units {Object} unit specification of the axis.
         */
        formatText: function( value, units ) {

            if ( !units ) {
                return formatNumber( value, 2 );
            }

            switch ( units.type ) {

                case 'degrees':
                case 'degree':
                case 'deg':

                    return formatDegrees( value, units.decimals );

                case 'time':
                case 'date':

                    return formatTime( value );

                case 'k':
                case 'thousand':
                case 'thousands':

                    return formatThousand( value, units.decimals, units.stepDown );

                case 'm':
                case 'million':
                case 'millions':

                    return formatMillion( value, units.decimals, units.stepDown );

                case 'b':
                case 'billion':
                case 'billions':

                    return formatBillion( value, units.decimals, units.stepDown );

                case 'i':
                case 'int':
                case 'integer':

                    return formatInteger(value);

                default:

                    return formatNumber( value, units.decimals );
            }
        },

        /**
         * Generates a marker value for the given viewport pixel coords.
         *
         * @param axis {Axis} the axis object.
         * @param vx   {int}  x viewport pixel coordinate
         * @param vy   {int}  y viewport pixel coordinate
         */
        getMarker: function( axis, vx, vy ) {
            var xOrY = axis.isXAxis ? 'x' : 'y',
                pixel = ( axis.isXAxis ) ? vx : vy,
                value = MapUtil.getCoordFromViewportPixel( axis.map, vx, vy )[ xOrY ];
            return {
                label: getMarkerRollover(axis, value),
                pixel: pixel
            };
        },

        /**
         * Generates all visible marker values, returns array of objects, containing
         * labels and pixel locations
         *
         * @param axis {Axis} the axis object.
         */
        getMarkers: function( axis ) {
            var intervals = {},
                increment,
                minIncrement,
                maxIncrement;

            switch ( axis.intervals.type ) {

                case "value":
                case "fixed":
                case "#":
                    // use fixed interval
                    increment = axis.intervals.increment;
                    // set pivot by value
                    intervals.pivot = axis.intervals.pivot;
                    break;

                default:
                    // use percentage
                    increment = axis.intervals.increment;
                    increment = ( increment > 1 ) ? increment * 0.01 : increment;
                    increment = ( axis.max - axis.min ) * increment;
                    // normalize percentages to [0-1] not [0-100]
                    if ( axis.intervals.pivot > 1 ) {
                        axis.intervals.pivot = axis.intervals.pivot / 100;
                    }
                    // calc pivot value by percentage
                    intervals.pivot = axis.min + ( axis.intervals.pivot * ( axis.max - axis.min ) );
                    break;
            }

            // scale increment if specified
            if ( axis.intervals.scaleByZoom ) {
                // scale increment by zoom
                increment = increment / Math.pow(2, Math.max( axis.map.getZoom() - 1, 0) );
            }

            // get sub increment for small / medium label-less ticks
            intervals.subIncrement = increment / MARKER_TYPE_ORDER.length;

            // get minimum and maximum visible increments on the axis
            minIncrement = getMinIncrement( axis, intervals );
            maxIncrement = getMaxIncrement( axis, intervals );

            // add all points between minimum visible value and maximum visible value
            return fillArrayByIncrement( axis, minIncrement, maxIncrement, intervals );
        }
    };
}());
