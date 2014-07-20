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
                        openFunc();
                    }, 800);
                },
                close: function() {
                    // cancel any pending callback
                    clearTimeout( pendingCallback );
                    if ( tooltipOpened ) {
                        // only call if the tooltip actually opened
                        closeFunc();
                    }
                    // clear flags on close
                    tooltipOpened = false;
                }
            });

        },

        disableTooltip: function( $elem ) {
            $elem.tooltip('disable');
        },

        /**
         * Allows the given DOM element or jQuery object events to propagate through
         * and interact with underlying elements
         */
        enableEventPropagation: function( elem, events ) {

            var domElement = (elem instanceof jQuery) ? elem[0] : elem,
                i;

            function propagateEvent( event ) {
                var newEvent = new event.constructor(event.type, event),
                    below;
                $(elem).css('pointer-events', 'none');
                below = document.elementFromPoint(event.clientX, event.clientY);
                if (below) {
                    below.dispatchEvent(newEvent);
                }
                $(elem).css('pointer-events', 'all');
            }

            if (!events) {
                domElement.onmousedown = propagateEvent;
                domElement.onmouseup = propagateEvent;
                domElement.onmousemove = propagateEvent;
                domElement.onwheel = propagateEvent;
                domElement.onmousewheel = propagateEvent;
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
            if (!events) {
                domElement.onmousedown = null;
                domElement.onmouseup = null;
                domElement.onmousemove = null;
                domElement.onwheel = null;
                domElement.onmousewheel = null;
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

        // Registers a click handler that only fires if the click didn't
        // involve a map drag. Since the map is moving under the mouse cursor
        // the browser will still register a click despite mouse movement. This
        // guards against that.
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
        }

    };
});
