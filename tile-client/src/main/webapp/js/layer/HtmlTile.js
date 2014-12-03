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

/*global HTMLElement */
define( function() {
    "use strict";

    OpenLayers.Tile.HTML = function() {
        OpenLayers.Tile.apply( this, arguments );
    };

    OpenLayers.Tile.HTML.prototype = Object.create( OpenLayers.Tile.prototype );

    OpenLayers.Tile.HTML.prototype.draw = function() {
        var that = this,
            shouldDraw = OpenLayers.Tile.prototype.draw.apply( this, arguments),
            dataUrl;
        if ( shouldDraw ) {
            this.positionTile();
            dataUrl = this.layer.getURL( this.bounds );
            if ( dataUrl !== this.url ) {

                this.url = dataUrl;
                this.tileData = null;

                // new url to render
                if (this.isLoading) {
                    this.dataRequest.abort();
                    this.dataRequest = null;
                    this.isLoading = false;
                }

                if (!this.url) {
                    this.unload();
                    return;
                }

                // hide tile contents until have data
                this.div.style.visibility = 'hidden';

                this.dataRequest = $.ajax({
                    url: this.url
                }).then(
                    function( data ) {
                        that.tileData = data;
                        that.renderTile( that.div, that.tileData );
                    },
                    function( xhr, status, error ) {
                        // TODO handle error
                        return true;
                    }
                ).always( function() {
                    that.isLoading = false;
                    that.dataRequest = null;
                });

            } else {
                // already have right data, just render
                this.renderTile( this.div, this.tileData );
            }

        } else if ( shouldDraw === false ) {
            this.unload();
        }
        return shouldDraw;
    };

    OpenLayers.Tile.HTML.prototype.positionTile = function() {

        if ( !this.div ) {
            this.div = document.createElement( 'div' );
            this.div.style.position = 'absolute';
            this.div.className = 'olTileHtml';
            this.layer.div.appendChild( this.div );
        }

        var style = this.div.style,
            size = this.layer.getImageSize( this.bounds ),
            ratio = 1;

        if ( this.layer instanceof OpenLayers.Layer.Grid ) {
            ratio = this.layer.getServerResolution() / this.layer.map.getResolution();
        }

        style.left = this.position.x + 'px';
        style.top = this.position.y + 'px';
        style.width = Math.round( ratio * size.w ) + 'px';
        style.height = Math.round( ratio * size.h ) + 'px';
    };

    OpenLayers.Tile.HTML.prototype.clear = function() {
        OpenLayers.Tile.prototype.clear.apply( this, arguments );
        this.tileData = null;
        this.url = null;
        if ( this.div ) {
            this.layer.div.removeChild( this.div );
            this.div = null;
        }
    };

    OpenLayers.Tile.HTML.prototype.renderTile = function(container, data) {

        if ( !this.layer || !this.div || !data.tile ) {
            return;
        }

        var renderer = this.layer.renderer,
            html = this.layer.html,
            render,
            entries,
            elements,
            i;

        if ( renderer ) {
            // if renderer is attached, use it
            render = renderer.render( data );
            html = render.html;
            entries = render.entries;
        } else {
            // else execute html
            if ( typeof html === "function" ) {
                html = html( data );
            }
        }

        if ( html instanceof jQuery ) {
            // if generated a jquery object, append it
            $( this.div ).append( html );
        } else if ( html instanceof HTMLElement ) {
            // if generated an HTMLElement, get html text
            this.div.appendChild( html );
        } else {
            // if generated string, set inner html
            this.div.innerHTML = html;
        }

        this.div.style.visibility = 'inherit';
        this.div.style.opacity = 'inherit';
        this.div.style['pointer-events'] = 'none';

        // set pointer-events on tile elements to 'all'
        elements = this.div.children;
        for ( i=0; i<elements.length; i++ ) {
            elements[i].style['pointer-events'] = 'all';
        }

        if ( renderer ) {
            // if renderer is attached, call hook function
            renderer.hook( elements, entries, data );
        }
    };

    return OpenLayers.Tile.HTML;
});
