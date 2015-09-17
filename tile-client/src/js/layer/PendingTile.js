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
 * An overridden OpenLayers.Tile object to create DOM elements based on
 * tile data. Used by HtmlTileLayers for client rendered layers. Uses
 * either Renderer objects or html functions to generate the DOM elements
 * or html strings.
 */

 ( function() {

    "use strict";

    var LayerUtil = require('./LayerUtil');

    function PendingTile() {
        OpenLayers.Tile.apply( this, arguments );
    }

    PendingTile.prototype = Object.create( OpenLayers.Tile.HTML.prototype );

    PendingTile.prototype.getURL = function() {
        return LayerUtil.getTilekey( this.layer, this.bounds ).replace( /,/g, "-" );
    };

    PendingTile.prototype.setImgSrc = function( url ) {
        var that  = this,
            img = this.imgDiv;
        if ( url ) {
            img.setAttribute( "data-url", url );
            that.onImageLoad.apply( that );
        } else {
            this.stopLoading();
            this.imgDiv = null;
            if ( img.parentNode ) {
                img.parentNode.removeChild( img );
            }
        }
    };

    PendingTile.prototype.onImageLoad = function() {
        var img = this.imgDiv;
        this.stopLoading();
        if ( img ) {
            // render the data
            this.renderHtml();
            // trigger load end
            this.events.triggerEvent("loadend");
            this.isLoading = false;
        }
    };

    PendingTile.prototype.getImage = function() {
        if ( !this.imgDiv ) {
            this.imgDiv = document.createElement( 'div' );
            this.imgDiv.className = 'olTileHtml olTilePending';
            this.imgDiv.style.position = "absolute";
        }
        return this.imgDiv;
    };

    PendingTile.prototype.renderHtml = function() {
        var imgDiv = this.imgDiv,
            url = imgDiv.getAttribute( "data-url", url );
        // clear tile contents
        imgDiv.className = 'olTileHtml olTilePending olTilePending_' + url;
        imgDiv.innerHTML = '<div class="cssload-loader">' +
        	'<ul>' +
        		'<li></li>' +
        		'<li></li>' +
        		'<li></li>' +
        		'<li></li>' +
        		'<li></li>' +
        		'<li></li>' +
        	'</ul>' +
        '</div>';
    };

    module.exports = PendingTile;

}());
