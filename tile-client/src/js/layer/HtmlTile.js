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

    OpenLayers.Tile.HTML = function() {
        OpenLayers.Tile.apply( this, arguments );
    };

    OpenLayers.Tile.HTML.prototype = Object.create( OpenLayers.Tile.prototype );

    OpenLayers.Tile.HTML.prototype.draw = function() {
        var that = this,
            shouldDraw = OpenLayers.Tile.prototype.draw.apply( this, arguments ),
            dataUrl;
        if ( shouldDraw ) {
            this.positionTile();
            dataUrl = this.layer.getURL( this.bounds );
            dataUrl = ( dataUrl instanceof Array ) ? dataUrl : [ dataUrl ];
            if ( !_.isEqual( dataUrl, this.url ) ) {
                // format request
                this.url = dataUrl;
                this.tileData = null;
                this.tileIndex = LayerUtil.getTileIndex( this.layer, this.bounds );
                this.tilekey = this.tileIndex.level + "," + this.tileIndex.xIndex + "," + this.tileIndex.yIndex;

                // new url to render
                if ( this.isLoading ) {
                    this.dataRequests.forEach( function( request ) {
                        request.fail();
                    });
                    this.dataRequests = null;
                    this.isLoading = false;
                }

                if ( !this.url ) {
                    this.unload();
                    return;
                }

                // hide tile contents until have data
                this.div.style.visibility = 'hidden';
                this.isLoading = true;
                var deferreds = [],
                    requestTilekey = this.tilekey;

                this.dataRequests = dataUrl.map( function( url ) {
                    var deferred = $.Deferred();
                    deferreds.push( deferred );
                    return $.ajax({
                        url: url
                    }).then(
                        function( results ) {
                            deferred.resolve( results );
                        },
                        function( xhr ) {
                            console.error( xhr.responseText );
                            console.error( xhr );
                        }
                    ).always(
                        function() {
                            that.isLoading = false;
                            that.dataRequest = null;
                    });
                });
                $.when.apply( $, deferreds ).then(
                    function() {
                        if ( that.tilekey !== requestTilekey ) {
                            // old data, reject it
                            return;
                        }
                        that.tileData = [];
                        var i;
                        for ( i=0; i<arguments.length; i++ ) {
                            that.tileData.push( arguments[i] );
                        }
                        that.renderTile( that.div, that.tileData );
                    },
                    function( xhr ) {
                        console.error( xhr.responseText );
                        console.error( xhr );
                    }
                ).always(
                    function() {
                        that.isLoading = false;
                        that.dataRequest = null;
                    }
                );
            } else {
                // already have right data, just render
                this.renderTile( this.div, this.tileData );
            }
        } else if ( shouldDraw === false ) {
            this.unload();
        }
        return shouldDraw;
    };

    OpenLayers.Tile.HTML.prototype.createBackBuffer = function() {
        return null;
    };

    OpenLayers.Tile.Image.prototype.createBackBuffer = function() {
        return null;
    };

    OpenLayers.Tile.HTML.prototype.positionTile = function() {

        if ( !this.div ) {
            this.div = document.createElement( 'div' );
            this.div.style.position = 'absolute';
            this.div.style.opacity = 0;
            this.div.className = 'olTileHtml';
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

    OpenLayers.Tile.HTML.prototype.clear = function() {
        OpenLayers.Tile.prototype.clear.apply( this, arguments );
        this.tileData = null;
        this.url = null;
        if ( this.div ) {
            this.layer.div.removeChild( this.div );
            this.div = null;
        }
    };

    OpenLayers.Tile.HTML.prototype.renderTile = function( container, data ) {

        if ( !this.layer || !this.div ) {
            // if the div or layer is no longer available, exit gracefully
            return;
        }

        var div = this.div,
            renderer,
            aggregator,
            html,
            render;

        // always style the opacity and visibility of the tile
        div.style.opacity = this.layer.opacity;
        div.style.visibility = 'inherit';
        div.innerHTML = "";

        // get renderer
        renderer = this.layer.renderer;
        if ( typeof renderer === "function" ) {
            renderer = renderer.call( this.layer, this.bounds );
        }

        // hide standard tile hover interaction
        if ( renderer.spec.hideTile ) {
            div.className = div.className + " hideTile";
        }

        if ( !data ) {
            // no data
            return;
        }

        // wrap it in an array for generic handling
        data = ( data instanceof Array ) ? data : [ data ];

        // ensure there is at least one set of data
        // tile data is under 'tile', elasticsearch is under 'hits'
        var hasTileOrHits = false;
        data.forEach( function( datum ) {
            if ( datum.tile || datum.hits ) {
                hasTileOrHits = true;
            }
        });
        if ( !hasTileOrHits ) {
            return;
        }

        // if aggregator, aggregate the data
        aggregator = renderer.aggregator;
        if ( aggregator ) {
            data.forEach( function( datum ) {
                if ( datum.tile ) {
                    datum.tile.meta = {
                        raw: datum.tile.meta.map.bins,
                        aggregated: aggregator.aggregate( datum.tile.meta.map.bins )
                    };
                } else {
                    datum.tile = null;
                }
            });
        }

        // unwrap if only a single entry
        data = ( data.length === 1 ) ? data[0] : data;

        // render data
        render = renderer.render( data );
        html = render.html;
        this.entries = render.entries;

        if ( html instanceof $ ) {
            // if generated a jquery object, append it
            $( div ).append( html );
        } else if ( html instanceof HTMLElement ) {
            // if generated an HTMLElement, get html text
            div.appendChild( html );
        } else {
            // if generated string, set inner html
            div.innerHTML = html;
        }

        // inject selected entry classes
        renderer.injectEntries( div.children, this.entries );
        // call renderer hook function
        renderer.executeHooks( div.children, this.entries, data );
    };

    module.exports = OpenLayers.Tile.HTML;
}());
