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

( function() {

    "use strict";

    var HtmlTile = require('./HtmlTile');

    function clampBounds( bounds, layer ) {
        bounds = bounds.clone();
        if ( layer.dimension === "x" ) {
            bounds.bottom = layer.map.getMaxExtent().bottom;
            bounds.top = layer.map.getMaxExtent().bottom + 1;
        } else {
            bounds.left = layer.map.getMaxExtent().left;
            bounds.right = layer.map.getMaxExtent().left + 1;
        }
        return bounds;
    }

    OpenLayers.Tile.Univariate = function() {
        OpenLayers.Tile.HTML.apply( this, arguments );
    };

    OpenLayers.Tile.Univariate.prototype = Object.create( HtmlTile.prototype );

    /**
     * Override this method to clamp the 'mapExtent' value for the map
     * object.
     */
    OpenLayers.Tile.Univariate.prototype.shouldDraw = function() {
        var withinMaxExtent = false,
            maxExtent = clampBounds( this.layer.maxExtent, this.layer );
        if ( maxExtent ) {
            var map = this.layer.map,
                bounds = map.getMaxExtent();
            var worldBounds = map.baseLayer.wrapDateLine && clampBounds( bounds, this.layer );
            if ( this.bounds.intersectsBounds( maxExtent, {inclusive: false, worldBounds: worldBounds} ) ) {
                withinMaxExtent = true;
            }
        }
        return withinMaxExtent || this.layer.displayOutsideMaxExtent;
    };

    /**
     * Override this method to inject the 'olUnivariateTile' class instead
     * of the usual olHtmlTile.
     */
    OpenLayers.Tile.Univariate.prototype.positionTile = function() {

        if ( !this.div ) {
            this.div = document.createElement( 'div' );
            this.div.style.position = 'absolute';
            this.div.style.opacity = 0;
            this.div.className = 'olTileUnivariate';
            this.layer.div.appendChild( this.div );
        }

        var style = this.div.style,
            size = this.layer.getImageSize( this.bounds ),
            ratio = this.layer.getServerResolution() / this.layer.map.getResolution();

        style.left = this.position.x + 'px';
        style.top = this.position.y + 'px';
        style.width = Math.round( ratio * size.w ) + 'px';
        style.height = Math.round( ratio * size.h ) + 'px';
    };

    module.exports = OpenLayers.Tile.Univariate;
}());
