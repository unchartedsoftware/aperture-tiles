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

    return {

        generateUuid: function() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random()*16|0, v = (c === 'x') ? r : (r&0x3|0x8);
                return v.toString(16);
            });
        },


        enableTooltip: function( $elem, message, openFunc, closeFunc ) {

            return true;

            /*
            var tooltipOpened = false,
                pendingCallback;

            $elem.attr('title', '');
            $elem.tooltip({
                content: message,
                track: true,
                show: { delay: 800 },
                open: function() {
                    // wait until tooltip actually opens before calling
                    pendingCallback = setTimeout( function() {
                        tooltipOpened = true;
                        if (openFunc) {
                            openFunc();
                        }
                    }, 800);
                },
                close: function() {
                    // cancel any pending callback
                    clearTimeout( pendingCallback );
                    if ( tooltipOpened ) {
                        // only call if the tooltip actually opened
                        if (closeFunc) {
                            closeFunc();
                        }
                    }
                    // clear flags on close
                    tooltipOpened = false;
                }
            });
            */
        },


        disableTooltip: function( $elem ) {
            $elem.tooltip('disable');
        },

        /**
         * Allows the given DOM element or jQuery object events to propagate through
         * and interact with underlying elements
         */
        enableEventPropagation: function( elem, events ) {

            var domElement = ( elem instanceof jQuery ) ? elem[0] : elem,
                i;

            function propagateEvent( event ) {
                var newEvent = new event.constructor( event.type, event ),
                    $elem,
                    before,
                    below;

                $elem = $( event.currentTarget );
                before = $elem.css( 'pointer-events' );
                $elem.css( 'pointer-events', 'none' );
                below = document.elementFromPoint( event.clientX, event.clientY );
                if ( below ) {
                    below.dispatchEvent( newEvent );
                }
                $elem.css( 'pointer-events', before );
            }

            $( domElement ).addClass( 'propagate' );

            if ( !events ) {
                domElement.onmousedown = propagateEvent;
                domElement.onmouseup = propagateEvent;
                domElement.onmousemove = propagateEvent;
                domElement.onwheel = propagateEvent;
                domElement.onscroll = propagateEvent;
                domElement.onclick = propagateEvent;
                domElement.ondblclick = propagateEvent;
            } else {
                events = ($.isArray) ? events : [events];
                for (i=0; i<events.length; i++) {
                    domElement[events[i]] = propagateEvent;
                }
            }
        },


        disableEventPropagation: function( elem, events ) {

            var domElement = (elem instanceof jQuery) ? elem[0] : elem,
                i;
            if ( !events ) {
                domElement.onmousedown = null;
                domElement.onmouseup = null;
                domElement.onmousemove = null;
                domElement.onwheel = null;
                domElement.onscroll = null;
                domElement.onclick = null;
                domElement.ondblclick = null;
            } else {
                events = ($.isArray) ? events : [events];
                for (i=0; i<events.length; i++) {
                    domElement[events[i]] = null;
                }
            }

        },

        /**
         * Registers a click handler that only fires if the click didn't
         * involve a map drag. Since the map is moving under the mouse cursor
         * the browser will still register a click despite mouse movement. This
         * guards against that.
         */
        dragSensitiveClick : function( node, handler, threshold ) {
            var dragStart = {x: null, y: null};

            threshold = threshold || 10;

            node.on('mousedown', function(evt) {
                dragStart.x = evt.pageX;
                dragStart.y = evt.pageY;
            });

            node.on('click', function(evt) {
                if (Math.abs(dragStart.x-evt.pageX) < threshold &&
                    Math.abs(dragStart.y-evt.pageY) < threshold ) {
                    handler.call(this, evt);
                }
            });
        },


        hexToRgb: function(hex) {
            var bigint;

            // remove #
            hex = hex.replace(/#/, '');

            // if only 3 hex values are provided, expand into 6 digit hex code
            if ( hex.length === 3 ) {
                hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
            }

            bigint = parseInt(hex, 16);
            return {
                r: (bigint >> 16) & 255,
                g: (bigint >> 8) & 255,
                b: bigint & 255
            };
        },


        rgbToHex: function( rgb ) {
            var r = rgb.r,
                g = rgb.g,
                b = rgb.b;
            function componentToHex(c) {
                var hex = c.toString(16);
                return (hex.length === 1) ? "0" + hex : hex;
            }
            return "#" + componentToHex( Math.floor(r)) +
                         componentToHex( Math.floor(g)) +
                         componentToHex( Math.floor(b));
        },


        hexBrightness: function( hex, factor ) {
            var rgb = this.hexToRgb( hex );
            return this.rgbToHex( { r: Math.min( Math.max( 0, rgb.r * factor ), 255 ),
                                    g: Math.min( Math.max( 0, rgb.g * factor ), 255 ),
                                    b: Math.min( Math.max( 0, rgb.b * factor ), 255 ) } );
        },


        hexGreyscale: function( hex ) {
            var rgb = this.hexToRgb( hex ),
                avg = ( rgb.r * 0.21 + rgb.g * 0.72 + rgb.b * 0.07 );
            return this.rgbToHex({
                r: avg,
                g: avg,
                b: avg
            });
        },


        hexBlend: function( hex1, hex2 ) {
            var rgb1 = this.hexToRgb( hex1 ),
                rgb2 = this.hexToRgb( hex2 );

            return this.rgbToHex({
                r : ( rgb1.r + rgb2.r ) / 2,
                g : ( rgb1.g + rgb2.g ) / 2,
                b : ( rgb1.b + rgb2.b ) / 2
            });

        },


        hexPercentageBlend: function( colors, percentages ) {
            var rgbs = [], i,
                rgb = {
                    r : 0,
                    g : 0,
                    b : 0
                };
            // convert hex to rgb
            for ( i=0; i<colors.length; i++ ) {
                rgbs.push( this.hexToRgb( colors[i] ) );
            }
            // apply weights
            for ( i=0; i<percentages.length; i++ ) {
                rgb.r += rgbs[i].r * percentages[i];
                rgb.g += rgbs[i].g * percentages[i];
                rgb.b += rgbs[i].b * percentages[i];
            }
            return this.rgbToHex( rgb );
        },


         parseMalformedJson: function( jsonString ) {
            // replace ( and ) with [ and ]
            var squared = jsonString.replace( /\(/g, '[' ).replace( /\)/g, ']'),
                // ensure all attributes are quoted in ""
                quoted = squared.replace(/([a-zA-Z0-9]+)(:)/g,'"$1"$2');
            return JSON.parse( quoted );
        },


        getURLParameter: function (key) {
            var url = window.location.search.substring(1),
                urlVars = url.split('&'),
                i, varKey,
                result = 0;
            for (i=0; i<urlVars.length; ++i) {
                varKey = urlVars[i].split('=');
                if (key === varKey[0]) {
                    result = varKey[1];
                    break;
                }
            }
            return result;
        }

    };
});
