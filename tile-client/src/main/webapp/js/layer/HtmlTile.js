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

    OpenLayers.Tile.Html = function() {
        OpenLayers.Tile.apply( this, arguments );
    };

    OpenLayers.Tile.Html.prototype = Object.create( OpenLayers.Tile.prototype );

    OpenLayers.Tile.Html.prototype.draw = function() {
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

    OpenLayers.Tile.Html.prototype.positionTile = function() {

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

    OpenLayers.Tile.Html.prototype.clear = function() {
        OpenLayers.Tile.prototype.clear.apply( this, arguments );
        this.tileData = null;
        this.url = null;
        if ( this.div ) {
            this.layer.div.removeChild( this.div );
            this.div = null;
        }
    };

    OpenLayers.Tile.Html.prototype.renderTile = function(container, data) {
        var renderer = this.layer.renderer,
            html = this.layer.html,
            entry = this.layer.entry,
            entries,
            i;
        if ( this.div && data.tile ) {
            if ( renderer ) {
                // if renderer is attached, use it
                html = renderer.createHtml( data );
            } else {
                // else execute html
                if ( typeof html === "function" ) {
                    html = html( data );
                }
            }
            // if generated a jquery object, get html text
            if ( html instanceof jQuery ) {
                html = html[0].outerHTML;
            }
            // if generated an HTMLElement, get html text
            if ( html instanceof HTMLElement ) {
                html = html.outerHTML;
            }
            this.div.innerHTML = html;
            this.div.style.visibility = 'inherit';
            this.div.style.opacity = 'inherit';
            this.div.style['pointer-events'] = 'none';

            // set pointer-events on tile elements to 'all'
            entries = this.div.children;
            for ( i=0; i<entries.length; i++ ) {
                entries[i].style['pointer-events'] = 'all';
            }

            if ( entry && typeof entry === "function" ) {
                entries = $( this.div.children );
                if ( renderer.getEntrySelector() ) {
                    entries = entries.find( renderer.getEntrySelector() );
                }
                entries.each( function( index, elem ) {
                    entry( data, elem );
                });
            }
        }
    };

    return OpenLayers.Tile.Html;
});
