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
 * A utility namespace containing commonly used functionality.
 */
( function() {

    "use strict";

    var propagateEvent;

    /**
     * Private: A propagation handler that will temporarily de-activate the
     * DOM element and propagate the event through to any underlying element.
     * @param event {Event} The event object.
     */
    propagateEvent = function( event ) {
        var newEvent = new event.constructor( event.type, event ),
            element,
            before,
            below;
        element = event.currentTarget;
        before = element.style['pointer-events'];
        element.style['pointer-events'] = 'none';
        below = document.elementFromPoint( event.clientX, event.clientY );
        if ( below ) {
            below.dispatchEvent( newEvent );
        }
        element.style['pointer-events'] = before;
    };

    module.exports = {

        /**
         * Generates an RFC4122 version 4 compliant UUID string.
         *
         * @returns {string}
         */
        generateUuid: function() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random()*16|0, v = (c === 'x') ? r : (r&0x3|0x8);
                return v.toString(16);
            });
        },

        /**
         * Allows the given DOM element or jQuery object events to propagate through
         * and interact with underlying elements
         *
         * @param elem     {HTMLElement | jQuery} The DOM element.
         * @param [events] {Array}                 Array of events to propagate through (optional).
         */
        enableEventPropagation: function( elem, events ) {
            var domElement = ( elem instanceof $ ) ? elem[0] : elem,
                i;
            if ( !events ) {
                domElement.addEventListener( 'mousedown', propagateEvent );
                domElement.addEventListener( 'mouseup', propagateEvent );
                domElement.addEventListener( 'mousemove', propagateEvent );
                domElement.addEventListener( 'wheel', propagateEvent );
                domElement.addEventListener( 'scroll', propagateEvent );
                domElement.addEventListener( 'click', propagateEvent );
                domElement.addEventListener( 'dblclick', propagateEvent );
            } else {
                events = ( events instanceof Array ) ? events : [ events ];
                for ( i=0; i<events.length; i++ ) {
                    domElement.addEventListener( events[i], propagateEvent );
                }
            }
        },

        /**
         * Removes previously enabled event propagation.
         *
         * @param elem     {HTMLElement | jQuery} The DOM element.
         * @param [events] {Array}               Array of events to remove (optional).
         */
        disableEventPropagation: function( elem, events ) {
            var domElement = ( elem instanceof $ ) ? elem[0] : elem,
                i;
            if ( !events ) {
                domElement.removeEventListener( 'mousedown', propagateEvent );
                domElement.removeEventListener( 'mouseup', propagateEvent );
                domElement.removeEventListener( 'mousemove', propagateEvent );
                domElement.removeEventListener( 'wheel', propagateEvent );
                domElement.removeEventListener( 'scroll', propagateEvent );
                domElement.removeEventListener( 'click', propagateEvent );
                domElement.removeEventListener( 'dblclick', propagateEvent );
            } else {
                events = ( events instanceof Array ) ? events : [ events ];
                for ( i=0; i<events.length; i++ ) {
                    domElement.removeEventListener( events[i], propagateEvent );
                }
            }
        },

        /**
         * Returns a proper modulo as the % operator in javascript is the 'remainder' operator.
         * @param value {number} the value
         * @param n     {number} the divisor
         * @returns {number}
         */
        mod: function( value, n ) {
           return ( (value % n) + n) % n;
        },

        /**
         * Rounds a number towards another number.
         * @param value {number} the value
         * @returns {number}
         */
         roundTowards: function( value, num ) {
            if ( value < num ) {
                return Math.ceil( value );
            }
            return Math.floor( value );
        },

        /**
         * Rounds a given value to a set number of decimals. Defaults to 2.
         *
         * @param value    {number} value to round
         * @param decimals {int}    number of decimals
         * @returns {string} rounded value
         */
        roundToDecimals: function( value, decimals ) {
            var numDec = decimals || 2,
                pow10 = Math.pow( 10, numDec );
            return parseFloat( Math.round( value * pow10 ) / pow10 ).toFixed( decimals );
        },

        /**
         * Converts a value from the range [0, 1] to a value from the
         * range [minMax.min, minMax.max] based on a value transform function.
         *
         * @param {number} percentage - The range percentage in range [0, 1].
         * @param {Object} minMax - The min and max values of the range.
         * @param {String} transform - The transform type, either 'log10' or 'linear'.
         *
         * @returns {number} The value from [minMax.min, minMax.max].
         */
        denormalizeValue: function( percentage, minMax, transform ) {
            var range = minMax.maximum- minMax.minimum,
                val;
            function log10(val) {
                return Math.log(val) / Math.LN10;
            }
            //iterate over the inner labels
            if ( transform === "log10" ) {
                var logMin = (minMax.minimum === 0) ? 0 : log10(minMax.minimum);
                var logMax = (minMax.maximum === 0) ? 0 : log10(minMax.maximum);
                val =  Math.pow(10, logMin + (logMax - logMin) * percentage);
            } else {
                val = percentage * range + minMax.minimum;
            }
            return val;
        },

        /**
         * Converts a value from the range [minMax.min, minMax.max] to a value in
         * the range [0, 1] based on a value transform function.
         *
         * @param {number} value - The range percentage in range [minMax.min, minMax.max].
         * @param {Object} minMax - The min and max values of the range.
         * @param {String} transform - The transform type, either 'log10' or 'linear'.
         *
         * @returns {number} The value percentage from [0, 1].
         */
        normalizeValue: function( value, minMax, transform ) {
            var range = minMax.maximum - minMax.minimum,
                val;
            function log10(val) {
                return Math.log(val) / Math.LN10;
            }
            function checkLogInput( value ) {
                return ( value === 0 || value === 1 ) ? 0 : log10( value ) / log10( value );
            }
            //iterate over the inner labels
            if ( transform === "log10" ) {
                val = checkLogInput( minMax.maximum );
            } else {
                val = ( ( value - minMax.minimum ) / range );
            }
            return val;
        },

        /**
         * Registers a click handler that only fires if the click isn't part of
         * a double click.
         *
         * @param element  {HTMLElement} The DOM element to attach the event.
         * @param callback {Function}    The callback function.
         * @param [timout] {int}            The timeout in ms (optional).
         */
        timeSensitiveClick: function( element, callback, timeout ) {
            var clicks = 0;
            element.addEventListener( "click", function( event ) {
		    	clicks++;
		    	if ( clicks === 1 ) {
		        	setTimeout( function() {
		        		if ( clicks === 1 ) {
							callback.call( element, event );
				        }
		        		clicks = 0;
		        	}, timeout || 300);
			    }
		    });
        },

        /**
         * Registers a click handler that only fires if the click didn't
         * involve a map drag. Since the map is moving under the mouse cursor
         * the browser will still register a click despite mouse movement. This
         * guards against that.
         *
         * @param element     {HTMLElement} The DOM element to attach the event.
         * @param callback    {Function}    The callback function.
         * @param [threshold] {int}         The movement threshold (optional).
         */
        dragSensitiveClick: function( element, callback, threshold ) {
            var dragStart = {x: null, y: null};
            threshold = threshold || 10;
            element.addEventListener( "mousedown", function( evt ) {
                dragStart.x = evt.pageX;
                dragStart.y = evt.pageY;
            });
            element.addEventListener( "click", function( evt ) {
                if (Math.abs( dragStart.x - evt.pageX ) < threshold &&
                    Math.abs( dragStart.y - evt.pageY ) < threshold ) {
                    callback.call( this, evt );
                }
            });
        },

        /**
         * Registers a click handler that only fires if the click didn't
         * involve a map drag and was not part of a double click.
         *
         * @param element     {HTMLElement} The DOM element to attach the event.
         * @param callback    {Function}    The callback function.
         */
        dragAndTimeSensitiveClick: function( element, callback ) {
            var dragStart = {x: null, y: null},
                threshold = threshold || 10;
            element.addEventListener( "mousedown", function( evt ) {
                dragStart.x = evt.pageX;
                dragStart.y = evt.pageY;
            });
            this.timeSensitiveClick( element, function( evt ) {
                if (Math.abs( dragStart.x - evt.pageX ) < threshold &&
                    Math.abs( dragStart.y - evt.pageY ) < threshold ) {
                    callback.call( this, evt );
                }
            });
        },

        /**
         * Return an object containing all parameters and values in the current
         * URL.
         *
         * @returns {Object}
         */
        getURLParameters: function() {
            var url = window.location.search.substring(1),
                urlVars = url.split('&'),
                result = {},
                keyValue,
                i;
            for ( i=0; i<urlVars.length; ++i ) {
                keyValue = urlVars[i].split('=');
                result[ keyValue[0] ] = keyValue[1];
            }
            return result;
        },

        /**
         * Return the value of a specific parameters in the current URL.
         *
         * @param key {string} The url parameter key.
         * @returns {string}
         */
        getURLParameter: function( key ) {
            return this.getURLParameters()[ key ];
        },

        /**
         * HTTP REST Error handling function.
         *
         * @param xhr {XmlHttpRequest} XmlHttpRequest object
         */
        handleHTTPError: function( xhr ) {
            console.error( xhr.responseText );
            console.error( xhr );
        },

        /**
         * Returns a string of an object in query parameter dot notation.
         *
         * @param params {Object} The query parameter object.
         * @returns {string}
         */
        encodeQueryParams: function( params ) {
            var query;
            function traverseParams( params, query ) {
                var result = "";
                _.forIn( params, function( value, key ) {
                    if ( typeof value !== "object" ) {
                        result += query + key + '=' + value + "&";
                    } else {
                        result += traverseParams( params[ key ], query + key + "." );
                    }
                });
                // if string ends in ".", it resulted in an empty leaf node, return
                // nothing in that case
                return ( result[0] !== "." ) ? result : "";
            }
            query = "?" + traverseParams( params, '' );
            // remove last '&' character
            return query.slice( 0, query.length - 1 );
        },

        /**
         * Creates and fills an array with the provided value.
         *
         * @param {number} length - The length of the array.
         * @param {*} value - The value to fill. Defaults to 0.
         *
         * @returns {Array} The filled array.
         */
        fillArray: function( length, value ) {
            value = value !== undefined ? value : 0;
            var arr = [],
                i;
            for ( i=0; i<length; i++ ) {
                arr.push( value );
            }
            return arr;
        },

			    /**
			     * Creates and fills an array with the provided value.
			     *
			     * @param {number} length - The length of the array.
			     * @param {*} value - The value to fill. Defaults to 0.
			     *
			     * @returns {Array} The filled array.
			     */
			    fillArrayByFunc: function( length, func ) {
				    var arr = [],
					    i;
				    for ( i=0; i<length; i++ ) {
					    arr.push( func() );
				    }
				    return arr;
			    },

        /**
         * Returns true if an object has no parameters.
         *
         * @param obj {Object} The object.
         * @returns {boolean}
         */
        isEmpty: function( obj ) {
            var prop;
            for ( prop in obj ) {
                if ( obj.hasOwnProperty( prop ) ) {
                    return false;
                }
            }
            return true;
        },

        /**
         * Extend class a by class b. Does not recurse, simply overlays top attributes.
         *
         * @param {Object} a - Object a which is extended.
         * @param {Object} b - Object b which extends a.
         *
         * @returns {Object} The extended object.
         */
        extend: function( a, b ) {
            var key;
            for( key in b ) {
                if( b.hasOwnProperty( key ) ) {
                    a[ key ] = b[ key ];
                }
            }
            return a;
        },


    };
}());
