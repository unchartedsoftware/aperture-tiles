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
            value,
            marker;
        // reduce sub increment by epsilon to prevent precision errors culling max point
        if ( axis.units.type !== 'time' &&
            axis.units.type !== 'date' &&
            axis.units.type !== 'i' &&
            axis.units.type !== 'int' &&
            axis.units.type !== 'integer' ) {
            subIncrement -= EPSILON;
        }
        for ( value=start; value<=end; value+=subIncrement ) {
            marker = {
                pixel: getPixelPosition( axis, value )
            };
            if ( MARKER_TYPE_ORDER[i] === "large" ) {
                marker.label = getMarkerRollover( axis, value );
            }
            markers[ MARKER_TYPE_ORDER[i] ].push( marker );
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
        incrementCount = Math.ceil( ( minCull - intervals.pivot ) / intervals.subIncrement, minCull );
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
        incrementCount = Math.floor( ( maxCull - intervals.pivot ) / intervals.subIncrement, maxCull );
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
     * Private: Formats a number in scientific notation rounding to nearest decimals.
     *
     * @param value    {number} the value.
     * @param decimals {int}    the number of decimal places.
     * @param allowStepDown {boolean} whether or not to step down units if < 1
     * @returns {string}
     */
    function formatScientificNotation( value, decimals ) {
        return value.toExponential(decimals);
    }

    /**
     * Private: Rounds a value to an floating point representation with specific decimal places.
     *
     * @param value    {number} the value.
     * @param decimals {int}    the number of decimal places.
     * @returns {string}
     */
    function formatNumber( value, decimals, allowStepDown ) {
        var truncValue = Math.pow( 10, -decimals );
        if (allowStepDown && value < truncValue) {
            return formatScientificNotation( value, decimals );
        }
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
            return formatNumber( value, decimals, true );
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
     * @param verbose {boolean} format to a longer more human readable date
     * @returns {string}
     */
    function formatTime( axis, value, verbose ) {
        var time = moment.utc( value );
        if ( verbose ) {
            return time.format( "MMM D, h:mm:ssa" );
        }
        var duration = moment.duration( ( axis.max - axis.min ) / Math.pow( 2, axis.map.getZoom() ) );
        if ( duration.asMonths() > 16 ) {
            return time.format( "YYYY" );
        } else if ( duration.asMonths() > 2 ) {
            return time.format( "MMM YYYY" );
        }
        return time.format( "MMM D, YYYY" );
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

    /**
     * Private: Returns a sub increment for the axis. This is scaled base on a
     * the 'minPixelWidth' interval property.
     * @param axis      {Axis}   axis object.
     * @param increment {number} increment for the axis.
     * @returns {number} sub increment value.
     */
    function getSubIncrement( axis, increment ) {
        var powerOfTwo = 1,
            subIncrement = increment / MARKER_TYPE_ORDER.length;
        if ( axis.intervals.minPixelWidth ) {
            // ensure increment is of minimum width
            while ( Math.abs( getPixelPosition( axis, increment*powerOfTwo ) -
                getPixelPosition( axis, 0 ) ) < axis.intervals.minPixelWidth ) {
                powerOfTwo *= 2;
            }
        }
        return subIncrement * powerOfTwo;
    }

    /**
     * Private: Adds a time based marker to the markers array.
     *
     * @param {Axis} axis - The axis object.
     * @param {number} start - The first visible value on the axis.
     * @param {number} end - The last visible value on the axis.
     * @param {Array} markers - The markers array.
     * @param {number} timestamp - The timestamp value to add.
     * @param {boolean} addLabel - Whether or not to add a label to the marker.
     */
    function addTimeMarker( axis, start, end, markers, timestamp, addLabel ) {
        if ( timestamp >= start && timestamp <= end ) {
            var marker = {
                pixel: getPixelPosition( axis, timestamp )
            };
            if ( addLabel ) {
                marker.label = timestamp;
            }
            markers.push( marker );
        }
    }

    /**
     * Private: Splits the time unit into non floating point units.
     *
     * @param {string} unit - The unit type.
     * @param {number} count - The number of units.
     */
    function splitTimeUnit( unit, count ) {
        // if unit is already a non floating point count, return early
        if ( count % 1 === 0 ) {
            return {
                unit: unit,
                count: count
            };
        }
        // split unit to next subunit
        var result = {};
        switch ( unit ) {
            case "year":
                result = {
                    unit: "month",
                    count: count * 12
                };
                break;
            case "month":
                result = {
                    unit: "week",
                    count: count * 4
                };
                break;
            case "week":
                result = {
                    unit: "day",
                    count: count * 7
                };
                break;
            case "day":
                result = {
                    unit: "hour",
                    count: count * 24
                };
                break;
            default:
                console.error("Cannot split time units evenly");
                return null;
        }
        // if unit still isn't split evenly, recurse further
        if ( result.count % 1 !== 0 ) {
            return splitTimeUnit( result.unit, result.count );
        }
        return result;
    }

    /**
     * Private: Adds time based markers based on some alignment.
     *
     * @param {Axis} axis - The axis object.
     * @param {Array} markers - The markers array.
     * @param {number} start - The first visible value on the axis.
     * @param {number} end - The last visible value on the axis.
     * @param {string} alignBy - The unit to align the markers by.
     * @param {string} unit - The unit by which to step by.
     * @param {number} countPerLabel - The number of units per label.
     */
    function addTimeAlignedMarkers( axis, markers, start, end, alignBy, unit, countPerLabel ) {
        var alignedStart = moment.utc( start ).startOf( alignBy ).valueOf(),
            duration = moment.duration( end - alignedStart ),
            numUnits = duration.as( unit + "s" ),
            step = countPerLabel >= 1 ? countPerLabel : 1,
            subUnit,
            aligned,
            large,
            medium,
            small,
            i, j;
        for ( i=0; i<numUnits; i+=step ) {
            // get timestamp of aligned unit
            aligned = moment.utc( alignedStart ).add( i, unit ).valueOf();
            // determine number of sub divisions
            var numSubDivisions = 1/countPerLabel;
            for ( j=0; j<numSubDivisions; j++ ) {
                var subDivided = splitTimeUnit( unit, countPerLabel );
                large = moment.utc( aligned ).add( j*subDivided.count, subDivided.unit ).valueOf();
                // add large marker
                addTimeMarker( axis, start, end, markers.large, large, true );
                // add medium marker
                subUnit = splitTimeUnit( subDivided.unit, subDivided.count / 2 );
                medium = moment.utc( large ).add( subUnit.count, subUnit.unit + "s" ).valueOf();
                addTimeMarker( axis, start, end, markers.medium, medium );
                // add small marker
                subUnit = splitTimeUnit( subDivided.unit, subDivided.count / 4 );
                small = moment.utc( large ).add( subUnit.count, subUnit.unit + "s" ).valueOf();
                addTimeMarker( axis, start, end, markers.small, small );
                small = moment.utc( large ).add( subUnit.count * 3, subUnit.unit + "s" ).valueOf();
                addTimeMarker( axis, start, end, markers.small, small );
            }
        }
    }

    /**
     * Private: Fills an array of markers by time based sub increments.
     *
     * @param axis      {Axis}   axis object.
     * @param start     {number} start increment.
     * @param end       {number} end increment
     */
    function fillArrayByTimeIncrement( axis, start, end ) {
        var duration = moment.duration( ( axis.max - axis.min ) / Math.pow( 2, axis.map.getZoom() ) ),
            markers = {
                large: [],
                medium: [],
                small: []
            };
        if ( duration.asMonths() > 16 ) {
            addTimeAlignedMarkers( axis, markers, start, end, "year", "year", 1 );
        } else if ( duration.asMonths() > 8 ) {
            addTimeAlignedMarkers( axis, markers, start, end, "year", "month", 4 );
        } else if ( duration.asMonths() > 4 ) {
            addTimeAlignedMarkers( axis, markers, start, end, "year", "month", 2 );
        } else if ( duration.asMonths() > 2 ) {
            addTimeAlignedMarkers( axis, markers, start, end, "year", "month", 1 );
        } else if ( duration.asMonths() > 1 ) {
            addTimeAlignedMarkers( axis, markers, start, end, "year", "month", 0.5 );
        } else if ( duration.asWeeks() > 2 ) {
            addTimeAlignedMarkers( axis, markers, start, end, "year", "month", 0.25 );
        } else if ( duration.asWeeks() > 1 ) {
            addTimeAlignedMarkers( axis, markers, start, end, "year", "day", 4 );
        } else if ( duration.asDays() > 4 ) {
            addTimeAlignedMarkers( axis, markers, start, end, "year", "day", 2 );
        } else {
            addTimeAlignedMarkers( axis, markers, start, end, "year", "day", 1 );
        }
        return markers;
    }

    module.exports = {

        /**
         * Formats axis marker label text.
         *
         * @param value {number} value of the label
         * @param units {Object} unit specification of the axis.
         * @param verbose {boolean} format to a more verbose format, if available.
         */
        formatText: function( axis, value, units, verbose ) {

            if ( !units ) {
                return formatNumber( value, 2 );
            }

            if (units.scale === "log10") {
                value = Math.pow(10, value);
            }

            switch ( units.type ) {

                case 'degrees':
                case 'degree':
                case 'deg':

                    return formatDegrees( value, units.decimals );

                case 'time':
                case 'date':

                    return formatTime( axis, value, verbose );

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

                    return formatInteger( value );

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
                value: value,
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

            // time based axis partitions aren't 'even', they should correspond based
            // on the scale at which the data is currently being shown, therefore these
            // markers will be generated as their own edge case
            if ( axis.units.type === "time" ) {
                minIncrement = getMinIncrement( axis, {
                    pivot: axis.min,
                    subIncrement: 1000 * 60 * 60
                });
                maxIncrement = getMaxIncrement( axis, {
                    pivot: axis.min,
                    subIncrement: 1000 * 60 * 60
                });
                return fillArrayByTimeIncrement( axis, minIncrement, maxIncrement );
            }

            switch ( axis.intervals.type ) {

                case "value":
                case "fixed":
                case "#":
                    // use fixed interval
                    increment = axis.intervals.increment;
                    // set pivot by value
                    intervals.pivot = ( axis.intervals.pivot !== undefined ) ? axis.intervals.pivot : axis.min;
                    break;

                default:
                    // use percentage
                    increment = axis.intervals.increment;
                    increment = ( increment > 1 ) ? increment * 0.01 : increment;
                    increment = ( axis.max - axis.min ) * increment;
                    intervals.pivot = ( axis.intervals.pivot !== undefined ) ? axis.intervals.pivot : 0;
                    // normalize percentages to [0-1] not [0-100]
                    if ( intervals.pivot > 1 ) {
                        intervals.pivot = intervals.pivot / 100;
                    }
                    // calc pivot value by percentage
                    intervals.pivot = axis.min + ( intervals.pivot * ( axis.max - axis.min ) );
                    break;
            }

            // scale increment if specified
            if ( axis.intervals.scaleByZoom ) {
                // scale increment by zoom
                increment = increment / Math.pow(2, Math.max( axis.map.getZoom() - 1, 0) );
            }

            //intervals.increment = increment;
            intervals.subIncrement = getSubIncrement( axis, increment );

            // get minimum and maximum visible increments on the axis
            minIncrement = getMinIncrement( axis, intervals );
            maxIncrement = getMaxIncrement( axis, intervals );

            // add all points between minimum visible value and maximum visible value
            return fillArrayByIncrement( axis, minIncrement, maxIncrement, intervals );
        }
    };
}());
