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


        init: function ( spec, views, map ) {
            this.id = spec[0].layer;
            this.name = spec[0].name || spec[0].layer;
            this.map = map;
            this.layerSpec = spec;
            this.layerInfo = {};
            this.views = views;
            this.renderersByTile = {};
            this.defaultRendererIndex = 0;
        },


        setOpacity: function( opacity ) {
            var i;
            for (i=0; i<this.views.length; i++) {
                this.views[i].renderer.setOpacity( opacity );
            }
        },


        setVisibility: function( visible ) {
            var i;
            for (i=0; i<this.views.length; i++) {
                this.views[i].renderer.setVisibility( visible );
            }
        },


        setZIndex: function( zIndex ) {
            var i;
            for (i=0; i<this.views.length; i++) {
                this.views[i].renderer.setZIndex( zIndex );
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

            var that = this,
                layerSpecs = this.layerSpec,
                layerInfos = this.layerInfo,
                deferreds = [],
                i;

            function configureView( layerSpec ) {

                var layerDeferred = $.Deferred();

                LayerService.configureLayer( layerSpec, function( layerInfo, statusInfo ) {

                    var layerId = layerInfo.layer;

                    if (statusInfo.success) {
                        if ( layerInfos[layerId] ) {
                            // if a previous configuration exists, release it
                            LayerService.unconfigureLayer( layerInfos[layerId], function() {
                                return true;
                            });
                        }
                        // set layer info
                        layerInfos[ layerId ] = layerInfo;
                        // resolve deferred
                        layerDeferred.resolve();
                    }
                });
                return layerDeferred;
            }

            for ( i=0; i<layerSpecs.length; i++ ) {
                deferreds.push( configureView( layerSpecs[i] ) );
            }

            $.when.apply( $, deferreds ).done( function() {
                var i, view;
                for (i=0; i<that.views.length; i++) {
                    view = that.views[i];
                    view.service = new TileService( layerInfos[ view.id ], that.map.getPyramid() );
                }
                // attach callback now
                that.map.on('move', $.proxy(that.update, that));
                that.update();
                callback( layerInfos );
            });

        },


        setTileRenderer: function( tilekey, newIndex ) {

            var oldIndex = this.getTileRenderer( tilekey ),
                oldRenderer = this.views[oldIndex].renderer,
                newRenderer = this.views[newIndex].renderer,
                oldService = this.views[oldIndex].service,
                newService = this.views[newIndex].service;

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
                view, renderer, service,
                rendererIndex,
                tilesByRenderer = [],
                tileViewBounds, i;

            for (i=0; i<this.views.length; ++i) {
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

            for (i=0; i<this.views.length; ++i) {

                view = this.views[i];
                renderer = view.renderer;
                service = view.service;
                // find which tiles we need for each view from respective
                service.requestData( tilesByRenderer[i],
                                     tileViewBounds,
                                     makeRedrawFunc( renderer, service ) );
                // force a redraw here, this will ensure that all removed nodes are erased
                renderer.redraw( service.getDataArray() );
            }
        }

     });

    return ClientLayer;
});
