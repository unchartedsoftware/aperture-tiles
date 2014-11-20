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

define( function(require) {
    "use strict";

    var HtmlNodeLayer = require('../../HtmlNodeLayer'),
        HtmlLayer = require('../../HtmlLayer');

    function HtmlRenderer( spec ) {
        this.spec = spec;
    }

    HtmlRenderer.prototype.activate = function() {
        var that = this;
        this.nodeLayer = new HtmlNodeLayer({
            map: this.map,
            xAttr: 'longitude',
            yAttr: 'latitude',
            idKey: 'tilekey'
        });
        this.nodeLayer.addLayer( new HtmlLayer({
            html: function() {
                var $tile = $('<div class="aperture-tile aperture-tile-'+this.tilekey+'""></div>'),
                    $content = that.spec.html || "";
                return $tile.append( $content );
            }
        }));
    };

    HtmlRenderer.prototype.deactivate = function() {
        // TODO:
        return true;
    };

    HtmlRenderer.prototype.setOpacity = function( opacity ) {
        this.nodeLayer.getRootElement().css( 'opacity', opacity );
    };

    HtmlRenderer.prototype.setVisibility = function( visible ) {
        var visibility = visible ? 'visible' : 'hidden';
        this.nodeLayer.getRootElement().css( 'visibility', visibility );
    };

    HtmlRenderer.prototype.setZIndex = function( zIndex ) {
        this.nodeLayer.getRootElement().css( 'zIndex', zIndex );
    };

    HtmlRenderer.prototype.redraw = function( data ) {
        this.nodeLayer.all( data ).redraw();
    };

    HtmlRenderer.prototype.draw = function( data ) {
        this.nodeLayer.join( [ data ] ).redraw();
    };

    HtmlRenderer.prototype.erase = function( tilekeys ) {
        this.nodeLayer.remove( tilekeys );
    };

    return HtmlRenderer;
});
