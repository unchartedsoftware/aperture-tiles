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

    var Layer = require('./Layer'),
        ClientLayer = require('./ClientLayer'),
        LayerUtil = require('./LayerUtil'),
        Util = require('../util/Util'),
        HtmlTileLayer = require('./HtmlTileLayer'),
        PubSub = require('../util/PubSub');

    /**
     * Returns a function which, based on the carousel index for the particular
     * tile, will use the specification values from the associated layer to
     * pull tile data from the server.
     *
     * @param {Carousel} carousel - The carousel object.
     *
     * @returns {Function} The getURL function for the carousel.
     */
    function getURLFunction( carousel ) {
        return function( bounds ) {
            var tileIndex = LayerUtil.getTileIndex( this, bounds ),
                x = tileIndex.xIndex,
                y = tileIndex.yIndex,
                z = tileIndex.level,
                tilekey = z + "," + x + "," + y,
                layerIndex = carousel.getLayerIndexForTile( tilekey ),
                layerSpec = carousel.layerSpecs[ layerIndex ],
                type = layerSpec.type,
                url = layerSpec.url,
                layername = layerSpec.layername;
            if ( x >= 0 && y >= 0 ) {
                return url + layername + "/" + z + "/" + x + "/" + y + "." + type;
            }
        };
    }

    /**
     * Returns a function which, based on the carousel index for the particular
     * tile, will use the renderer from the associated layer.
     *
     * @param {Carousel} carousel - The carousel object.
     *
     * @returns {Function} The renderer function for the carousel.
     */
    function getRendererFunction( carousel ) {
        return function( bounds ) {
            var tilekey = LayerUtil.getTilekey( this, bounds ),
                layerIndex = carousel.getLayerIndexForTile( tilekey ),
                layerSpec = carousel.layerSpecs[ layerIndex ];
            return layerSpec.renderer;
        };
    }

    /**
     * OpenLayers stores tiles in a double array. To redraw an individual tile we
     * iterate through until we find the matching tilekey ( which is appended to
     * the tile in the HtmlTile class ).
     *
     * @param {OpenLayers.Layer} olLayer - The openlayers layer object.
     * @param {String} tilekey - The tile key string.
     */
    function redrawTile( olLayer, tilekey ) {
        var grid,
            i, j;
        for ( i=0; i<olLayer.grid.length; i++ ) {
            grid = olLayer.grid[i];
            for ( j=0; j<grid.length; j++ ) {
                if ( grid[j].tilekey === tilekey ) {
                    grid[j].draw( true );
                    return;
                }
            }
        }
    }

    /**
     * Patches the get/set functions of the attached layer in order to delegate
     * behaviour to and from the carousel.
     *
     * @param {Carousel} carousel - The carousel object.
     * @param {Layer} layer - The layer object to patch.
     */
    function patchLayer( carousel, layer ) {
        // override the layers opacity functions, setting the opacity of the carousel
        // will update the opacity of ALL layers.
        layer.setOpacity = function( opacity ) {
            var i;
            carousel.setOpacity( opacity );
            for ( i=0; i<carousel.layers.length; i++ ) {
                PubSub.publish( carousel.layers[i].getChannel(), { field: 'opacity', value: opacity } );
            }
        };
        layer.getOpacity = function() {
            return carousel.getOpacity();
        };
        // override the layers enable functions, enabling a particular layer
        // will disable all other layers attached to the carousel.
        layer.isEnabled = function() {
            if ( !carousel.isEnabled() ) {
                return false;
            }
            return carousel.layers.indexOf( layer ) === carousel.defaultIndex;
        };
        layer.setEnabled = function( enabled ) {
            var i;
            if ( enabled ) {
                carousel.setEnabled( true );
                carousel.setTileLayerIndices( carousel.layers.indexOf( layer ) );
                PubSub.publish( layer.getChannel(), { field: 'enabled', value: enabled } );
                for ( i=0; i<carousel.layers.length; i++ ) {
                    if ( carousel.layers[i] !== layer ) {
                        PubSub.publish( carousel.layers[i].getChannel(), { field: 'enabled', value: false } );
                    }
                }
            } else {
                carousel.setEnabled( false );
                PubSub.publish( layer.getChannel(), { field: 'enabled', value: false } );
            }
        };
        // override the layers z index functions
        layer.setZIndex = function( zIndex ) {
            carousel.setZIndex( zIndex );
        };
        layer.getZIndex = function() {
            return carousel.getZIndex();
        };
        // override the layers theme function
        layer.setTheme = function( theme ) {
            carousel.setTheme( theme );
        };
        // override redraw function
        layer.redraw = function() {
            carousel.olLayer.redraw();
        };
    }

        /**
         * Returns the layer to its original state be repairing the previously
         * patched methods.
         *
         * @param {Layer} layer - The layer object to patch.
         * @param {Object} layerSpec - The object containing the original methods.
         */
    function unpatchLayer( layer, layerSpec ) {
        // return all the layers functions to thier previous state
        layer.setOpacity = layerSpec.setOpacity;
        layer.getOpacity = layerSpec.getOpacity;
        layer.isEnabled = layerSpec.isEnabled;
        layer.setEnabled = layerSpec.setEnabled;
        layer.setZIndex = layerSpec.setZIndex;
        layer.setTheme = layerSpec.setTheme;
        layer.redraw = layerSpec.redraw;
    }

    /**
     * Instantiate a Carousel object to allow tile-by-tile control for all layers in
     * the carousel 'bundle'.
     * This object modifies the functionality from its bundled layer objects as follows:
     * <pre>
     *     1) Opacity is shared across all bundled layers.
     *     2) Z-Index is shared across all bundled layers.
     *     3) Theme is shared across all bundled layers.
     *     4) Enabling / disabling a layer will switch all tiles to that particular layer.
     * </pre>
     * @class Carousel
     * @augments Layer
     * @classdesc A carousel object to allow changing individual client renderered tiles.
     */
    function Carousel() {
        // call base constructor
        Layer.call( this, {} );
        // set reasonable defaults
        this.zIndex = 1500;
        this.domain = "carousel";
        this.layers = [];
        this.layerSpecs = [];
        this.indicesByTile = {};
        this.defaultIndex = 0;
    }

    Carousel.prototype = Object.create( ClientLayer.prototype );

    /**
     * Adds a client rendered layer to the carousel object. This involves
     * 'patching' the methods of the layer with those from the carousel to
     * give the required functionality.
     * @memberof Carousel
         *
     * @param {Layer} layer - The client rendered layer object.
     */
    Carousel.prototype.addLayer = function( layer ) {

        if ( !( layer instanceof ClientLayer ) ) {
            console.log( "Only ClientLayers can be added to a carousel object." );
            return;
        }

        if ( !this.map ) {
            // if there is no map it means that the carousel
            // has not been added yet, or the map is not ready
            // store the layer for later adding
            this.deferreds = this.deferreds || [];
            this.deferreds.push( layer );
            return;
        }

        // store the layer specification
        var layerSpec = {
                url: layer.source.tms,
                type: 'json',
                layername: layer.source.id,
                setOpacity: layer.setOpacity,
                getOpacity: layer.getOpacity,
                isEnabled: layer.isEnabled,
                setEnabled: layer.setEnabled,
                setZIndex: layer.setZIndex,
                setTheme: layer.setTheme,
                redraw: layer.redraw
            };
        // set the renderer parent to the carousel
        if ( layer.renderer ) {
            layerSpec.renderer = layer.renderer;
            layerSpec.renderer.meta = layer.source.meta.meta;
            layerSpec.renderer.parent = this;
        }
        layer.carousel = this;
        // store the layer and its specification
        this.layerSpecs.push( layerSpec );
        this.layers.push( layer );
        // patch the layer
        patchLayer( this, layer );
    };

    /**
     * Remove a layer from the carousel. This will 'unpatch' the
     * layers and return them to their original state.
     * @memberof Carousel
         *
     * @param {Layer} layer - The layer object.
     */
    Carousel.prototype.removeLayer = function( layer ) {

        if ( !this.map && this.deferreds ) {
            // if there is no map it means that the carousel
            // has not been added yet, so simply remove it from
            // the deferreds array.
            this.deferreds.splice( this.deferreds.indexOf( layer ), 1 );
            return;
        }

        var index = this.layers.indexOf( layer );
        if ( index !== -1 ) {
            if ( this.layers.length === 1 && this.map ) {
                console.log( 'A carousel must have at least one layer to be ' +
                             'attached to a map, remove the carousel from the map first.' );
                return;
            }
            if ( layer.renderer ) {
                // restore parent property of the renderer
                layer.renderer.parent = layer;
            }
            delete layer.carousel;
            unpatchLayer( layer, this.layerSpecs[ index ] );
            this.layers.splice( index, 1 );
            this.layerSpecs.splice( index, 1 );
            // set the default index accordingly
            if ( index < this.defaultIndex || this.defaultIndex === this.layers.length ) {
                this.defaultIndex--;
            }
            // reset view
            this.setTileLayerIndices( this.defaultIndex );
        }
    };

        /**
     * Activates the carousel object. This should never be called manually.
     * @memberof Carousel
     * @private
     */
    Carousel.prototype.activate = function() {

        if ( this.layers.length === 0 &&
            ( !this.deferreds || this.deferreds.length === 0 ) ) {
            console.log( 'A carousel must have at least one layer attached ' +
                         'before it can be added to a map.');
            return;
        }

        // add the new layer
        this.olLayer = new HtmlTileLayer(
            'Client Rendered Carousel Tile Layer',
            null,
            {
                layername: null,
                type: 'json',
                maxExtent: new OpenLayers.Bounds(-20037500, -20037500,
                    20037500,  20037500),
                isBaseLayer: false,
                getURL: getURLFunction( this ),
                renderer: getRendererFunction( this )
            });

        this.map.olMap.addLayer( this.olLayer );

        this.setZIndex( this.zIndex );
        this.setOpacity( this.opacity );
        this.setEnabled( this.enabled );
        this.setTheme( this.map.getTheme() );

        // if the layers are already attached to the map, remove them first.
        var i;
        for ( i=0; i<this.layers.length; i++ ) {
            this.map.remove( this.layers[i] );
        }

        // add all deferred layers, if they exist
        if ( this.deferreds ) {
            for ( i=0; i<this.deferreds.length; i++ ) {
                this.addLayer( this.deferreds[i] );
            }
            delete this.deferreds;
        }
    };

    /**
     * Increment which layer index the current tile points to. This will
     * redraw the modified tile.
     * @memberof Carousel
         *
     * @param {String} tilekey - The tile key string.
     */
    Carousel.prototype.incrementTileLayerIndex = function( tilekey ) {
        if ( this.indicesByTile[ tilekey ] === undefined ) {
            this.indicesByTile[ tilekey ] = ( this.defaultIndex + 1 ) % this.layers.length;
        } else {
            this.indicesByTile[ tilekey ] = ( this.indicesByTile[ tilekey ] + 1 ) % this.layers.length;
            if ( this.indicesByTile[ tilekey ] === this.defaultIndex ) {
                delete this.indicesByTile[ tilekey ];
            }
        }
        redrawTile( this.olLayer, tilekey );
    };

    /**
     * Decrement which layer index the current tile points to. This will
     * redraw the modified tile.
     * @memberof Carousel
         *
     * @param {String} tilekey - The tile key string.
     */
    Carousel.prototype.decrementTileLayerIndex = function( tilekey ) {
        if ( this.indicesByTile[ tilekey ] === undefined ) {
            this.indicesByTile[ tilekey ] = Util.mod( this.defaultIndex -1, this.layers.length );
        } else {
            this.indicesByTile[ tilekey ] = Util.mod( this.indicesByTile[ tilekey ] - 1,  this.layers.length );
            if ( this.indicesByTile[ tilekey ] === this.defaultIndex ) {
                delete this.indicesByTile[ tilekey ];
            }
        }
        redrawTile( this.olLayer, tilekey );
    };

    /**
     * Set which layer index the current tile points to. This will
     * redraw the modified tile.
     * @memberof Carousel
         *
     * @param {String} tilekey - The tile key string.
     * @param {number} index - The new layer index for the tile.
     */
    Carousel.prototype.setTileLayerIndex = function( tilekey, index ) {
        if ( index === this.defaultIndex ) {
            delete this.indicesByTile[ tilekey ];
        } else {
            this.indicesByTile[ tilekey ] = index;
        }
        redrawTile( this.olLayer, tilekey );
    };

    /**
     * Set the layer index for all tiles. This will redraw the entire
     * layer.
     * @memberof Carousel
         *
     * @param {String} index - The layer index for all tiles.
     */
    Carousel.prototype.setTileLayerIndices = function( index ) {
        this.defaultIndex = Util.mod( index, this.layers.length );
        this.indicesByTile = {};
        this.olLayer.redraw();
    };

    /**
     * Returns the layer index for a particular tile.
     * @memberof Carousel
         *
     * @param {String} tilekey - The tile key string.
     *
     * @returns {number} The layer index for the tile.
     */
    Carousel.prototype.getLayerIndexForTile = function( tilekey ) {
        var layerIndex = this.indicesByTile[ tilekey ];
        return layerIndex !== undefined ? layerIndex : this.defaultIndex;
    };

    /**
     * Returns the renderer for a particular tile.
     * @memberof Carousel
         *
     * @param {String} tilekey - The tile key string.
     *
     * @returns {number} The renderer for the tile.
     */
    Carousel.prototype.getRendererForTile = function( tilekey ) {
        var layerIndex = this.getLayerIndexForTile( tilekey );
        return this.layers[ layerIndex ].renderer;
    };

    module.exports = Carousel;
}());
