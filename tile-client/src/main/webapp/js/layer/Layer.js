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
 * This modules defines a basic layer class that can be added to maps.
 */
define(function (require) {
    "use strict";



    var Class = require('../class'),
        Layer;



    Layer = Class.extend({
        ClassName: "Layer",


        init: function ( spec, map ) {

            this.id = spec.layer;
            this.name = spec.name || spec.layer;
            this.map = map;
            this.layerSpec = spec;
            this.layerInfo = null;
        },

        /**
         *  Returns the layer's specification object
         */
        getLayerSpec: function () {
            return this.layerSpec;
        },

        /**
         *  Returns the layer's information object
         */
        getLayerInfo: function () {
            return this.layerInfo;
        }

    });

    return Layer;
});
