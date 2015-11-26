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

    OpenLayers.Tile.HTML = function() {
        this.url = null;
        this.imgDiv = null;
        this.imageReloadAttempts = null;
        OpenLayers.Tile.apply( this, arguments );
    };

    OpenLayers.Tile.HTML.prototype = Object.create( OpenLayers.Tile.prototype );

    OpenLayers.Tile.HTML.prototype.destroy = function() {
        if ( this.imgDiv )  {
            this.clear();
            this.imgDiv = null;
        }
        OpenLayers.Tile.prototype.destroy.apply( this, arguments );
    };

    OpenLayers.Tile.HTML.prototype.draw = function() {
        var shouldDraw = OpenLayers.Tile.prototype.draw.apply( this, arguments );
        if ( shouldDraw ) {
            if ( this.isLoading ) {
                //if we're already loading, send 'reload' instead of 'loadstart'.
                this._loadEvent = "reload";
            } else {
                this.isLoading = true;
                this._loadEvent = "loadstart";
            }
            this.renderTile();
            this.positionTile();
        } else if ( shouldDraw === false ) {
            this.unload();
        }
        return shouldDraw;
    };

    OpenLayers.Tile.HTML.prototype.getURL = function() {
        var url = this.layer.getURL( this.bounds );
        if ( url instanceof Array ) {
            url = url.join('|');
        }
        return url;
    };

    OpenLayers.Tile.HTML.prototype.renderTile = function() {
        this.url = this.getURL();
        this.initImage();
    };

    OpenLayers.Tile.HTML.prototype.positionTile = function() {
        var style = this.getTile().style,
            size = this.layer.getImageSize( this.bounds ),
            ratio = 1;
        if ( this.layer instanceof OpenLayers.Layer.Grid ) {
            ratio = this.layer.getServerResolution() / this.layer.map.getResolution();
        }
        style.left = this.position.x + "px";
        style.top = this.position.y + "px";
        style.width = Math.round( ratio * size.w ) + "px";
        style.height = Math.round( ratio * size.h ) + "px";
    };

    OpenLayers.Tile.HTML.prototype.clear = function() {
        OpenLayers.Tile.prototype.clear.apply( this, arguments );
        var img = this.imgDiv;
        if ( img ) {
            var tile = this.getTile();
            if ( tile.parentNode === this.layer.div ) {
                this.layer.div.removeChild( tile );
            }
            this.setImgSrc();
            OpenLayers.Element.removeClass(img, "olImageLoadError");
        }
    };

    OpenLayers.Tile.HTML.prototype.getImage = function() {
        if ( !this.imgDiv ) {
            this.imgDiv = document.createElement( 'div' );
            this.imgDiv.className = 'olTileHtml';
            var style = this.imgDiv.style;
            style.visibility = "hidden";
            style.opacity = 0;
            if ( this.layer.opacity < 1 ) {
                style.filter = 'alpha(opacity=' + (this.layer.opacity * 100) + ')';
            }
            style.position = "absolute";
        }
        return this.imgDiv;
    };

    OpenLayers.Tile.HTML.prototype.setImage = function( img ) {
        this.imgDiv = img;
    };

    OpenLayers.Tile.HTML.prototype.initImage = function() {
        if ( !this.url && !this.imgDiv ) {
            // fast path out - if there is no tile url and no previous image
            this.isLoading = false;
            return;
        }
        this.events.triggerEvent('beforeload');
        this.layer.div.appendChild( this.getTile() );
        this.events.triggerEvent( this._loadEvent );
        var img = this.getImage();
        var dataUrl = img.getAttribute('data-url') || '';
        if ( dataUrl === this.url ) {
            this._loadTimeout = window.setTimeout(
                OpenLayers.Function.bind( this.onImageLoad, this ),
                0 );
        } else {
            this.stopLoading();
            this.imageReloadAttempts = 0;
            this.setImgSrc( this.url );
        }
    };

    OpenLayers.Tile.HTML.prototype.setImgSrc = function( url ) {
        var that  = this,
            img = this.imgDiv;
        if ( url ) {
            var urls = url.split('|');
            img.style.visibility = 'hidden';
            img.style.opacity = 0;
            img.setAttribute( "data-url", url );
            var pendingRequests = urls.map( function( url ) {
                var deferred = $.Deferred();
                $.ajax({
                    url: url
                }).then(
                    function( results ) {
                        deferred.resolve( results );
                    },
                    function( xhr ) {
                        deferred.reject();
                        console.error( xhr.responseText );
                        console.error( xhr );
                    }
                );
                return deferred;
            });
            $.when.apply( $, pendingRequests ).then(
                function() {
                    if ( url === that.url ) {
                        that.onImageLoad.apply( that, arguments );
                    }
                },
                function() {
                    if ( url === that.url ) {
                        that.onImageError();
                    }
                }
            );
        } else {
            // Remove reference to the image, and leave it to the browser's
            // caching and garbage collection.
            this.stopLoading();
            this.imgDiv = null;
            if ( img.parentNode ) {
                img.parentNode.removeChild( img );
            }
        }
    };

    OpenLayers.Tile.HTML.prototype.getTile = function() {
        return this.getImage();
    };

    OpenLayers.Tile.HTML.prototype.createBackBuffer = function() {
        return;
    };

    OpenLayers.Tile.HTML.prototype.renderHtml = function() {
        var imgDiv = this.imgDiv,
            tileData = imgDiv._tileData,
            renderer = this.getRenderer(),
            aggregator,
            html,
            render;
        // clear tile contents
        imgDiv.innerHTML = "";
        // hide standard tile hover interaction
        if ( renderer.spec.hideTile ) {
            imgDiv.className = imgDiv.className + " hideTile";
        }
        if ( !tileData || tileData.length === 0 ) {
            // no data, exit
            return;
        }
        // ensure there is at least one set of data
        // tile data is under 'tile', elasticsearch is under 'hits'
        var hasTileOrHits = false;
        tileData.forEach( function( datum ) {
            if ( datum.tile || datum.hits ) {
                hasTileOrHits = true;
            }
        });
        // no content in data, exit
        if ( !hasTileOrHits ) {
            return;
        }
        // if aggregator, aggregate the data
        aggregator = renderer.aggregator;
        if ( aggregator ) {
            var outOfRangeCount = 0;
            tileData.forEach( function( datum ) {
                if ( datum.tile ) {
                    // only aggregate the data if it hasn't already been aggregated
                    if ( !datum.tile.meta.aggregated || datum.tile.meta.aggregationType !== aggregator ) {
                        var rawData = datum.tile.meta.map ? datum.tile.meta.map.bins : datum.tile.meta.raw;
                        datum.tile.meta = {
                            raw: rawData,
                            aggregated: aggregator.aggregate( rawData )
                        };
                        datum.tile.meta.aggregationType = aggregator;
                    }
                    // check if requested range is outside of available range
                    if ( datum.tile.meta.aggregated.length === 0 ) {
                        // no data, flag as out of range
                        outOfRangeCount++;
                    }
                }
            });
            // if all data is outside the range, exit early
            if ( outOfRangeCount === tileData.length ) {
                // data outside of range, exit
                return;
            }
        }
        var data = tileData.length === 1 ? tileData[0] : tileData;
        // render data
        render = renderer.render( data );
        html = render.html;
        this.entries = render.entries;
        // add html to the tile div
        if ( html instanceof $ ) {
            // if generated a jquery object, append it
            $( imgDiv ).append( html );
        } else if ( html instanceof HTMLElement ) {
            // if generated an HTMLElement, get html text
            imgDiv.appendChild( html );
        } else {
            // if generated string, set inner html
            imgDiv.innerHTML = html;
        }
        // inject selected entry classes
        renderer.injectEntries( imgDiv.children, this.entries );
        // call renderer hook function
        renderer.executeHooks( imgDiv.children, this.entries, data );
    };

    OpenLayers.Tile.HTML.prototype.getRenderer = function() {
        var renderer = this.layer.renderer;
        if ( typeof renderer === "function" ) {
            renderer = renderer.call( this.layer, this.bounds );
        }
        return renderer;
    };

    OpenLayers.Tile.HTML.prototype.onImageLoad = function() {
        var img = this.imgDiv;
        this.stopLoading();
        if ( img ) {
            img.style.visibility = 'inherit';
            img.style.opacity = this.layer.opacity;
            if ( arguments.length > 0 ) {
                // only set the data if there is new data to set
                var data = [],
                    i;
                for ( i=0; i<arguments.length; i++ ) {
                    data.push( arguments[i] );
                }
                img._tileData = data;
            }
            // render the data
            this.renderHtml();
            // trigger load end
            this.isLoading = false;
            this.events.triggerEvent("loadend");
        }
    };

    OpenLayers.Tile.HTML.prototype.onImageError = function() {
        var img = this.imgDiv;
        if ( img && img.getAttribute('data-url') ) {
            this.imageReloadAttempts++;
            if ( this.imageReloadAttempts <= OpenLayers.IMAGE_RELOAD_ATTEMPTS ) {
                this.setImgSrc( this.getURL() );
            } else {
                OpenLayers.Element.addClass( img, "olImageLoadError" );
                this.events.triggerEvent("loaderror");
                this.onImageLoad();
            }
        }
    };

    OpenLayers.Tile.HTML.prototype.stopLoading = function() {
        window.clearTimeout( this._loadTimeout );
        delete this._loadTimeout;
    };

    OpenLayers.Tile.HTML.prototype.getCanvasContext = function() {
        return undefined;
    };

    /**
     * OpenLayers overrides
     */

    if ( OpenLayers.TileManager ) {

        OpenLayers.TileManager.prototype.addTile = function(evt) {
            if ( evt.tile instanceof OpenLayers.Tile.Image ||
                evt.tile instanceof OpenLayers.Tile.HTML ||
                evt.tile instanceof OpenLayers.Tile.Univariate ) {
                if ( !evt.tile.layer.singleTile ) {
                    evt.tile.events.on({
                        beforedraw: this.queueTileDraw,
                        beforeload: this.manageTileCache,
                        loadend: this.addToCache,
                        unload: this.unloadTile,
                        scope: this
                    });
                }
            } else {
                // Layer has the wrong tile type, so don't handle it any longer
                this.removeLayer({layer: evt.tile.layer});
            }
        };

        OpenLayers.TileManager.prototype.queueTileDraw = function( evt ) {
            var tile = evt.object;
            var queued = false;
            var layer = tile.layer;
            var url = tile.getURL ? tile.getURL() : layer.getURL( tile.bounds );
            var img = this.tileCache[url];
            if ( img &&
                !OpenLayers.Element.hasClass( img, 'olTileImage' ) &&
                !OpenLayers.Element.hasClass( img, 'olTileHtml' ) &&
                !OpenLayers.Element.hasClass( img, 'olTileUnivariate' ) ) {
                // cached image no longer valid, e.g. because we're olTileReplacing
                delete this.tileCache[ url ];
                OpenLayers.Util.removeItem( this.tileCacheIndex, url );
                img = null;
            }
            // queue only if image with same url not cached already
            if ( layer.url && ( layer.async || !img ) ) {
                // add to queue only if not in queue already
                var tileQueue = this.tileQueue[ layer.map.id ];
                if ( !~OpenLayers.Util.indexOf( tileQueue, tile ) ) {
                    tileQueue.push(tile);
                }
                queued = true;
            }
            return !queued;
        };

    }

    OpenLayers.Tile.Image.prototype.createBackBuffer = function() {
        return;
    };

    module.exports = OpenLayers.Tile.HTML;
}());
