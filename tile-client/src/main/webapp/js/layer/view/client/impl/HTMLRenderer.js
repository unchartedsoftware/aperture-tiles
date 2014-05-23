/*
 * Copyright (c) 2013 Oculus Info Inc.
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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines the base class for a client render layer. Must be 
 * inherited from for any functionality.
 */
define(function (require) {
    "use strict";



    var ClientRenderer = require('../ClientRenderer'),
        HtmlLayer = require('../HtmlLayer'),
        HtmlRenderer;



    HtmlRenderer = ClientRenderer.extend({
        ClassName: "HtmlRenderer",

        /**
         * Constructs a client render layer object
         * @param id the id string for the render layer
         */
        init: function( map) {

            this._super(map);

            this.layers = [];

            this.layers.push( new HtmlLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey',
                html: '<div class="test-renderer">'

                      +'<div class="test-0">'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'</div>'

                      +'<div class="test-1">'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'</div>'

                      +'<div class="test-2">'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'</div>'

                      +'<div class="test-3">'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'<div class="test-renderer-block"></div>'
                      +'</div>'

                      +'</div>',
                css: {
                    'z-index' : 1000
                }
            }));

        },

        redraw: function( data ) {
            var i;
            for (i=0; i< this.layers.length; i++) {
                this.layers[i].all(data);
            }
        }


    });

    return HtmlRenderer;
});
