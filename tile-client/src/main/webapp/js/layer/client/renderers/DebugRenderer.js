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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */



/**
 * A simple test layer to test the client side of client rendering.
 * This layer simply puts the tile coordinates in the middle of each tile.
 */
define(function (require) {
    "use strict";
	
	
	
    var HtmlRenderer = require('../HtmlRenderer'),
        HtmlNodeLayer = require('../../HtmlNodeLayer'),
        HtmlLayer = require('../../HtmlLayer'),
        DebugRenderer;

		
		
    DebugRenderer = HtmlRenderer.extend({
        ClassName: "DebugLayer",
		
        init: function ( map, spec ) {

            this._super( map, spec );
            this.createNodeLayer(); // instantiate the node layer data object
            this.createLayer();     // instantiate the html visualization layer
        },


        createNodeLayer: function() {

            // instantiate node layer object
            this.nodeLayer = new HtmlNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey'
            });
        },


        createLayer : function() {

            // instantiate html debug layer
            this.nodeLayer.addLayer( new HtmlLayer({
                html: function () {
                    return '<div>' + this.tilekey + '</div>';
                },
                css: {
                    position: 'relative',
                    color: 'white',
                    width: '100%',
                    height: '100%',
                    'font-size': '40px',
                    'line-height': '256px',
                    'text-align': 'center',
                    'vertical-align': 'middle',
                    'text-shadow': '-2px -2px 2px #000, 2px -2px 2px #000, -2px  2px 2px #000, 2px  5px 5px #000'
                }
            }));
        }

    });

    return DebugRenderer;
});
