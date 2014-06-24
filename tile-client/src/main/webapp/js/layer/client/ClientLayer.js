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


define(function (require) {
    "use strict";



    var Layer = require('../Layer'),
        LayerService = require('../LayerService'),
        TileService = require('./TileService'),
        makeRedrawFunc,
        ClientLayer;



    makeRedrawFunc = function( renderer, tileService ) {
        return function() {
            renderer.redraw( tileService.getDataArray() );
        };
    };

    ClientLayer = Layer.extend({


        init: function ( spec, renderers, map ) {

            this._super( spec, map );
            this.renderers = renderers;
            this.tileServices = [];
            this.renderersByTile = {};
            this.defaultRendererIndex = 0;
            this.map.on('move', $.proxy(this.update, this));
        },


        setOpacity: function( opacity ) {

            var i;
            for (i=0; i<this.renderers.length; i++) {
                this.renderers[i].setOpacity( opacity );
            }
        },


        setVisibility: function( visible ) {
            var i;
            for (i=0; i<this.renderers.length; i++) {
                this.renderers[i].setVisibility( visible );
            }
        },


        setZIndex: function( zIndex ) {
            var i;
            for (i=0; i<this.renderers.length; i++) {
                this.renderers[i].setZIndex( zIndex );
            }
        },


        setTileFocus: function( tilekey ) {
            return true;
        },


        setDefaultRendererIndex: function( index ) {
            this.defaultRendererIndex = index;
            this.update();
        },


        getTileRenderer: function( tilekey ) {

            var index = this.renderersByTile[tilekey];
            return (index !== undefined) ? index : this.defaultRendererIndex;
        },


        configure: function( callback ) {

            var that = this;

            LayerService.configureLayer( this.layerSpec, function( layerInfo, statusInfo ) {

                var i;
                if (statusInfo.success) {
                    if ( that.layerInfo ) {
                        // if a previous configuration exists, release it
                        LayerService.unconfigureLayer( that.layerInfo, function() {
                            return true;
                        });
                    }
                    // set layer info
                    that.layerInfo = layerInfo;
                    // create tile service for each renderer
                    // TODO: add support for different services (multiple layerInfos)
                    for (i=0; i<that.renderers.length; i++) {
                        that.tileServices[i] = new TileService( that.getLayerInfo(), that.map.getPyramid() );
                    }
                    that.update();
                }

                callback( layerInfo, statusInfo );
            });
        },


        setTileRenderer: function( tilekey, newIndex ) {

            var oldIndex = this.getTileRenderer( tilekey ),
                oldRenderer = this.renderers[oldIndex],
                newRenderer = this.renderers[newIndex],
                oldService = this.tileServices[oldIndex],
                newService = this.tileServices[newIndex];

            // update internal state
            if ( newIndex === this.defaultRendererIndex ) {
                delete this.renderersByTile[tilekey];
            } else {
                this.renderersByTile[tilekey] = newIndex;
            }

            if ( newService.layerInfo.layer === oldService.layerInfo.layer ) {
                // both renderers share the same data source, swap tile data
                // give tile to new view
                newService.data[tilekey] = oldService.data[tilekey];
                makeRedrawFunc( newRenderer, newService )();
            } else {
                // otherwise request new data
                newService.getRequest( tilekey, {}, makeRedrawFunc( newRenderer, newService ));
            }

            // release and redraw to remove old data
            oldService.releaseData( tilekey );
            makeRedrawFunc( oldRenderer, oldService )();
        },


        /**
         * Map update callback, this function is called when the map view state is updating. Requests
         * and draws any new tiles
         */
        update: function() {

            var tiles, tilekey,
                rendererIndex,
                tilesByRenderer = [],
                tileViewBounds, i;

            if (this.renderers.length === 0 || this.tileServices.length === 0) {
                return;
            }

            for (i=0; i<this.renderers.length; ++i) {
                tilesByRenderer[i] = [];
            }

            // determine all tiles in view
            tiles = this.map.getTilesInView();
            tileViewBounds = this.map.getTileBoundsInView();

            // group tiles by view index
            for (i=0; i<tiles.length; ++i) {
                tilekey = tiles[i].level+','+tiles[i].xIndex+','+tiles[i].yIndex;
                rendererIndex = this.getTileRenderer( tilekey );
                tilesByRenderer[rendererIndex].push( tiles[i] );
            }

            for (i=0; i<this.renderers.length; ++i) {
                // find which tiles we need for each view from respective
                this.tileServices[i].requestData( tilesByRenderer[i],
                                                  tileViewBounds,
                                                  makeRedrawFunc( this.renderers[i], this.tileServices[i] ) );
            }
        }

     });

    return ClientLayer;
});
