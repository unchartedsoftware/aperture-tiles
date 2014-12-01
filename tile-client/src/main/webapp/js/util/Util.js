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

define( function() {
    "use strict";

    var propagateEvent;

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

    return {

        /**
         * Generates an RFC4122 version 4 compliant UUID string.
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
         * @param elem   {HTMLElement || jQuery} The DOM element.
         * @param events {Array}                 Array of events to propagate through.
         */
        enableEventPropagation: function( elem, events ) {

            var domElement = ( elem instanceof jQuery ) ? elem[0] : elem,
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
                events = ( events instanceof Array ) ? events : [events];
                for ( i=0; i<events.length; i++ ) {
                    domElement.addEventListener( events[i], propagateEvent );
                }
            }
        },

        /**
         * Removes previously enabled event propagation.
         *
         * @param elem   {HTMLElement || jQuery} The DOM element.
         * @param events {Array}                 Array of events to remove.
         */
        disableEventPropagation: function( elem, events ) {

            var domElement = ( elem instanceof jQuery ) ? elem[0] : elem,
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
                events = ( events instanceof Array ) ? events : [events];
                for ( i=0; i<events.length; i++ ) {
                    domElement.removeEventListener( events[i], propagateEvent );
                }
            }
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
        dragSensitiveClick : function( element, callback, threshold ) {
            var dragStart = {x: null, y: null};

            threshold = threshold || 10;

            element.onmousedown = function( evt ) {
                dragStart.x = evt.pageX;
                dragStart.y = evt.pageY;
            };

            element.onclick = function( evt ) {
                if (Math.abs( dragStart.x - evt.pageX ) < threshold &&
                    Math.abs( dragStart.y - evt.pageY ) < threshold ) {
                    callback.call( this, evt );
                }
            };
        },

        /**
         * Return an object containing all parameters and values in the current
         * URL.
         * @returns {Object}
         */
        getURLParameters: function() {
            var url = window.location.search.substring(1),
                urlVars = url.split('&'),
                result = {},
                keyValue,
                i;
            for  (i=0; i<urlVars.length; ++i ) {
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
        }

    };
});
