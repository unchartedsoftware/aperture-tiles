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

define(function (require) {
    "use strict";



    var ClientRenderer = require('./ClientRenderer'),
        ApertureRenderer;



    ApertureRenderer = ClientRenderer.extend({
        ClassName: "ApertureRenderer",

        init: function( map, spec ) {

            var that = this;
            this._super( map, spec );
            this.opacity = 1.0;
            this.visibility = true;
            this.nodeLayer = {};
            this.X_CENTRE_OFFSET = 128;
            this.Y_CENTRE_OFFSET = 128;
            this.map.on('zoomend', function() {
                that.redraw( [] );
            });
        },

        setOpacity: function( opacity ) {
            this.opacity = opacity;
            this.nodeLayer.all().redraw();
        },

        setVisibility: function( visible ) {
            this.visibility = visible;
            this.nodeLayer.all().redraw();
        },

        setZIndex: function( zIndex ) {
            // TODO: find out why this no longer works and fix it
            //this.map.setLayerIndex( this.nodeLayer.olLayer_, zIndex );
            return true;
        },

        redraw: function( data ) {
            this.nodeLayer.all( data ).where( data ).redraw();
        }

    });

    return ApertureRenderer;
});
