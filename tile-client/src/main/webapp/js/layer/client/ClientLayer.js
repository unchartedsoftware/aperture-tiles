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
        PubSub = require('../../util/PubSub'),
        Util = require('../../util/Util'),
        LayerService = require('../LayerService'),
        TileService = require('./TileService'),
        makeRedrawFunc,
        updateTileFocus,
        ClientLayer;



    makeRedrawFunc = function( renderer, tileService, conditional ) {
        return function() {
            // if conditional function is provided, it must return true to execute layer redraw
            if ( conditional === undefined ||
                ( conditional && typeof conditional === 'function' && conditional() ) ) {
                renderer.redraw( tileService.getDataArray() );
            }
        };
    };


    updateTileFocus = function ( layer, x, y ) {
        var tilekey = layer.map.getTileKeyFromViewportPixel( x, y );
        if ( tilekey !== layer.getTileFocus() ) {
            // only update tilefocus if it actually changes
            layer.setTileFocus( tilekey );
        }
    };



    ClientLayer = Layer.extend({


        init: function ( spec, views, map ) {

            var that = this,
                previousMouse = {};

            // set reasonable defaults
            spec[0].enabled = ( spec[0].enabled !== undefined ) ? spec[0].enabled : true;
            spec[0].opacity = ( spec[0].opacity !== undefined ) ? spec[0].opacity : 1.0;

            this.id = spec[0].layer;
            this.uuid = Util.generateUuid();
            this.domain = 'client';
            this.name = spec[0].name || spec[0].layer;
            this.map = map;
            this.layerSpec = spec;
            this.layerInfo = {};
            this.views = views;
            this.renderersByTile = {};
            this.defaultRendererIndex = 0;
            this.click = null;
            this.hover = null;

            // clear click state if map is clicked
            this.map.on( 'click', function() {
                that.setClick( null );
            });

            // set tile focus callbacks
            this.map.on('mousemove', function(event) {
                updateTileFocus( that, event.xy.x, event.xy.y );
                previousMouse.x = event.xy.x;
                previousMouse.y = event.xy.y;
            });
            this.map.on('zoomend', function(event) {
                updateTileFocus( that, previousMouse.x, previousMouse.y );
            });

            this.setZIndex( spec[0].zIndex );
            this.setVisibility( spec[0].enabled );
            this.setOpacity( spec[0].opacity );
        },


        setClick: function( value ) {
            this.click = value;
            PubSub.publish( this.getChannel(), { field: 'click', value: value });
        },


        getClick: function() {
            return this.click;
        },


        isClicked: function() {
            return this.click !== null && this.click !== undefined;
        },


        setHover: function( value ) {
            this.hover = value;
            PubSub.publish( this.getChannel(), { field: 'hover', value: value });
        },


        getHover: function() {
            return this.hover;
        },


        setCarouselEnabled: function( isEnabled ) {
            this.carouselEnabled = isEnabled;
            PubSub.publish( this.getChannel(), { field: 'carouselEnabled', value: isEnabled });
        },


        isCarouselEnabled: function() {
            return this.carouselEnabled;
        },


        getRendererCount: function() {
            return this.views.length;
        },


        setOpacity: function( opacity ) {
            var i;
            this.opacity = opacity;
            for (i=0; i<this.views.length; i++) {
                this.views[i].renderer.setOpacity( opacity );
            }
            PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity });
        },


        getOpacity: function() {
            return this.opacity;
        },


        setVisibility: function( visible ) {
            var i;
            this.visibility = visible;
            for (i=0; i<this.views.length; i++) {
                this.views[i].renderer.setVisibility( visible );
            }
            PubSub.publish( this.getChannel(), { field: 'enabled', value: visible });
        },


        getVisibility: function() {
            return this.visibility;
        },


        setZIndex: function( zIndex ) {
            var i;
            this.zIndex = zIndex;
            for (i=0; i<this.views.length; i++) {
                this.views[i].renderer.setZIndex( zIndex );
            }
            PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
        },


        getZIndex: function() {
            return this.zIndex;
        },


        setTileFocus: function( tilekey ) {
            this.previousTileFocus = this.tileFocus;
            this.tileFocus = tilekey;
            PubSub.publish( this.getChannel(), { field: 'tileFocus', value: tilekey });
        },


        getTileFocus: function() {
            return this.tileFocus;
        },


        getPreviousTileFocus: function() {
            return this.previousTileFocus;
        },


        setDefaultRendererIndex: function( index ) {
            this.defaultRendererIndex = index;
            this.update();
            PubSub.publish( this.getChannel(), { field: 'defaultRendererIndex', value: index });
        },


        getDefaultRendererIndex: function() {
            return this.defaultRendererIndex;
        },


        setTileRenderer: function( tilekey, newIndex ) {

            var that = this,
                oldIndex = this.getTileRenderer( tilekey ),
                oldRenderer = this.views[oldIndex].renderer,
                newRenderer = this.views[newIndex].renderer,
                oldService = this.views[oldIndex].service,
                newService = this.views[newIndex].service;

            if ( newIndex === oldIndex ) {
                return;
            }

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
                newService.getRequest( tilekey, {}, makeRedrawFunc( newRenderer, newService, function() {
                    // on redraw check if the renderer index is still the same, it may be the case that
                    // the server took so long to respond that the this view is no longer active
                    return that.getTileRenderer(tilekey) === newIndex;
                }));
            }

            // release and redraw to remove old data
            oldService.releaseData( tilekey );
            makeRedrawFunc( oldRenderer, oldService )();
            PubSub.publish( this.getChannel(), { field: 'rendererByTile', value: newIndex });
        },


        getTileRenderer: function( tilekey ) {
            var index = this.renderersByTile[tilekey];
            return ( index !== undefined ) ? index : this.defaultRendererIndex;
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
                    // pass parent layer (this) along with meta data to the renderer
                    view.renderer.parent = that;
                    view.renderer.meta = layerInfos[ view.id ].meta;
                    // subscribe renderer to pubsub AFTER it has its parent reference
                    view.renderer.subscribeRenderer();
                }
                // attach callback now
                that.map.on('move', $.proxy(that.update, that));
                that.update();
                callback( layerInfos );
            });

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
