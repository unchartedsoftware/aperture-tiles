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

/*global OpenLayers*/

/**
 * This module defines a ClientState class which is to be held by a ViewController and shared with the individual client
 * render layers. This is to guarantee that interface states can be shared / integrated between separate client layers
 */
define(function (require) {
    "use strict";



    var Class = require('../../../class'),
        ClientState;



    ClientState = Class.extend({

        /**
         * Construct a mouse state
         */
        init: function () {

			// holds the state of mouse hovers
            this.hoverState = {
                    userData : {},
                    tilekey : '',
                    level : -1,
                    xIndex : -1,
                    yIndex : -1
            };

			// holds the state of mouse clicks
            this.clickState = {
                    userData : {},
                    tilekey : '',
                    level : -1,
                    xIndex : -1,
                    yIndex : -1
            };

            this.isVisible = true;
            this.opacity = 1.0;
        },


        setHoverState: function(tilekey, userData) {
            var splitKey = tilekey.split(',');
            this.hoverState.userData = userData;
            this.hoverState.tilekey = tilekey;
            this.hoverState.level = parseInt(splitKey[0], 10);
            this.hoverState.xIndex = parseInt(splitKey[1], 10);
            this.hoverState.yIndex = parseInt(splitKey[2], 10);
        },


        setClickState: function(tilekey, userData) {
            var splitKey = tilekey.split(',');
            this.clickState.userData = userData;
            this.clickState.tilekey = tilekey;
            this.clickState.level = parseInt(splitKey[0], 10);
            this.clickState.xIndex = parseInt(splitKey[1], 10);
            this.clickState.yIndex = parseInt(splitKey[2], 10);
        },


        clear: function() {
            this.clearClickState();
            this.clearHoverState();
        },


        clearClickState: function() {
            this.clickState = {
                userData : {},
                tilekey : '',
                level : -1,
                xIndex : -1,
                yIndex : -1
            };
        },


        clearHoverState: function() {
            this.hoverState = {
                userData : {},
                tilekey : '',
                level : -1,
                xIndex : -1,
                yIndex : -1
            };
        }
		

     });

    return ClientState;
});
