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
/* global activityLogger */
define(function (require) {
    "use strict";



    var ActivityLogger = activityLogger,
        logger = new ActivityLogger( "./js/libjs/draper.activity_worker-2.1.1.js" )
                        .echo( true )
                            .testing( true )
                                .mute( ['SYS', 'USER'] );


    function DESCRIPTION_MAP( layerState, fieldName ) {
        var value = layerState.get( fieldName ),
            id = layerState.getId();

        switch ( fieldName ) {
            case 'opacity':             return 'Setting opacity to ' + value + ' for layer ' + id;
            case 'enabled':             return 'Setting visibility to ' + value + ' for layer ' + id;
            case 'zIndex':              return 'Setting zIndex to ' + value + ' for layer ' + id;
            case 'filterRange':         return 'Filtering range from ' + value[0] + ' to ' + value[1] + ' for layer ' + id;
            case 'rampType':            return 'Setting ramp type to ' + value + ' for layer ' + id;
            case 'rampFunction':        return 'Setting ramp function to ' + value + ' for layer ' + id;
            case 'tileFocus':           return 'Hover on tile ' + value;
            case 'previousTileFocus':   return 'Hover off of tile ' + value;
            case 'translate':           return 'Translating over tile ' + value;
            case 'baseLayerIndex':      return 'Setting baselayer to ' + layerState.BASE_LAYERS[value];
            case 'carouselEnabled':     return ( value ? 'Enabling' : 'Disabling' ) + ' carousel';
            case 'rendererByTile':      return 'Swapping client rendering for tile ' + value;
            case 'zoom':                return 'Zooming to level ' + value;
            case 'pan':                 return 'Panning map, delta vector [' + value.dx + ', ' + value.dy + ']';
            case 'drag':

                if (value.state === "drag") {
                    return 'Dragging layer ' + value.target + " controls";
                }
                return 'Dropping layer ' + value.target + " controls";

            case 'tooltip':

                if (value.state === "open") {
                    return 'Tooltip open on ' + value.target;
                }
                return 'Tooltip close on ' + value.target;

            case 'click':

                if (!value) {
                    return "Click off";
                }

                if ( value.target === 'client-layer' ) {
                    return "Click on topic '" + value.tag + "' in tile " + value.data.tilekey;
                }
                return "Click on " + value.target;

            case 'hover':

                if (!value) {
                    return "Hover off";
                }

                if ( value.target === 'client-layer' ) {
                    return "Hover on topic '" + value.tag + ' in tile ' + value.data.tilekey;
                }
                return "Hover on " + value.target;

            default:                    return "Processing '" + fieldName + "' event, value of " + JSON.stringify( layerState.get( fieldName ) );
        }

    }

    function ACTION_MAP( fieldName ) {
        switch ( fieldName ) {
            case 'opacity':             return 'filter_data';
            case 'enabled':             return "filter_data";
            case 'zIndex':              return "filter_data";
            case 'filterRange':         return "filter_data";
            case 'rampType':            return "filter_data";
            case 'rampFunction':        return "filter_data";
            case 'tileFocus':           return "filter_data";
            case 'previousTileFocus':   return "filter_data";
            case 'translate':           return "filter_data";
            case 'baseLayerIndex':      return "filter_data";
            case 'carouselEnabled':     return "filter_data";
            case 'rendererByTile':      return "filter_data";
            default:                    return 'missing_action';
        }
    }

    function WORKFLOW_MAP( fieldName ) {
        switch ( fieldName ) {
            case 'opacity':             return logger.WF_EXPLORE;
            case 'enabled':             return logger.WF_EXPLORE;
            case 'zIndex':              return logger.WF_EXPLORE;
            case 'filterRange':         return logger.WF_EXPLORE;
            case 'rampType':            return logger.WF_EXPLORE;
            case 'rampFunction':        return logger.WF_EXPLORE;
            case 'tileFocus':           return logger.WF_EXPLORE;
            case 'previousTileFocus':   return logger.WF_EXPLORE;
            case 'translate':           return logger.WF_EXPLORE;
            case 'baseLayerIndex':      return logger.WF_EXPLORE;
            case 'carouselEnabled':     return logger.WF_EXPLORE;
            case 'rendererByTile':      return logger.WF_EXPLORE;
            default:                    return logger.WF_OTHER;
        }
    }

    function IGNORE_MAP( fieldName ) {
        switch ( fieldName ) {

            case 'previousBaseLayerIndex':
            case 'rampMinMax':
            case 'rampImageUrl':
            case 'rendererCount':
            case 'defaultRendererIndex':

                return true;

            default: return false;
        }
    }


    return {

        start: function() {
            logger.unmute(['SYS', 'USER']);
        },

        stop: function() {
            logger.mute(['SYS', 'USER']);
        },

        getListener: function( layerState ) {

            return function( fieldName ) {

                if ( !IGNORE_MAP( fieldName ) ) {
                    logger.logUserActivity( DESCRIPTION_MAP( layerState, fieldName ), ACTION_MAP( fieldName ), WORKFLOW_MAP( fieldName ) );
                }
            };
        }

    };

});
