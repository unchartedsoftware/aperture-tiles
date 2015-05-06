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

    var MapUtil = require('./MapUtil');

    /**
     * Instantiate a Marker object.
     * @class Marker
     * @classdesc A marker object that is pinned to the map.
     */
    function Marker( imgUrl, imgWidth, imgHeight ) {
        this.width = imgWidth || 50;
        this.height = imgHeight || 50;
        this.icon = new OpenLayers.Icon(
            imgUrl,
            new OpenLayers.Size( this.width, this.height ),
            new OpenLayers.Pixel( -this.width/2, -this.height ) );
    }

    Marker.prototype = {

        /**
         * Activates the marker object. This should never be called manually.
         * @memberof Marker
         * @private
         */
        activate: function( x, y ) {
            this.x = x;
            this.y = y;
            var viewportPx = MapUtil.getViewportPixelFromCoord( this.map, x, y ),
                lonlat = this.map.olMap.getLonLatFromViewPortPx( viewportPx );
            this.olMarker = new OpenLayers.Marker( lonlat, this.icon.clone() );
            this.map.olMarkers.addMarker( this.olMarker );
        },

        /**
         * De-activates the marker object. This should never be called manually.
         * @memberof Marker
         * @private
         */
        deactivate: function() {
            if ( this.olMarker && this.map.olMarkers ) {
                this.map.olMarkers.removeMarker( this.olMarker );
                this.olMarker.destroy();
                this.olMarker = null;
                this.x = null;
                this.y = null;
            }
        },

        /**
         * Returns the markers div element.
         * @memberof Map.prototype
         *
         * @returns {HTMLElement} The marker div.
         */
        getElement: function() {
            if ( this.olMarker ) {
                return this.olMarker.icon.imageDiv;
            }
            return null;
        }

    };

    module.exports = Marker;
}());
