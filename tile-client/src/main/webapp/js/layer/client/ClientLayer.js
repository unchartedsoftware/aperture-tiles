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
        makeRedrawFunc,
        ClientLayer;



    makeRedrawFunc = function( renderer, tileService, conditional ) {
        return function() {
            // if conditional function is provided, it must return true to execute layer redraw
            if ( !conditional || conditional() ) {
                renderer.redraw( tileService.getDataArray() );
            }
        };
    };



    ClientLayer = Layer.extend({


        init: function ( spec, views, map ) {

            var that = this,
                view,
                i;

            // set reasonable defaults
            spec.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
            spec.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;

            // call base constructor
            this._super( spec, map );

            this.views = views;
            this.renderersByTile = {};
            this.defaultViewIndex = 0;
            this.click = null;
            this.hover = null;

            for (i=0; i<this.views.length; i++) {
                view = this.views[i];
                // pass parent layer (this) along with meta data to the renderer / details
                view.renderer.parent = that;
                if ( view.details ) {
                    view.details.parent = that;
                }
                // subscribe renderer to pubsub AFTER it has its parent reference
                view.renderer.subscribeRenderer();
            }

            // update tiles on map move
            this.map.on('move', function() {
                that.update();
            });
            // clear click state if map is clicked
            this.map.on( 'click', function() {
                that.setClick( null );
            });
            this.setZIndex( spec.zIndex );
            this.setVisibility( spec.enabled );
            this.setOpacity( spec.opacity );
            this.update();
        },


        /**
         * Store click event object.
         */
        setClick: function( value ) {
            this.click = value;
            PubSub.publish( this.getChannel(), { field: 'click', value: value });
        },


        /**
         * Get click event object.
         */
        getClick: function() {
            return this.click;
        },


        /**
         * Check if click event object exists.
         */
        isClicked: function() {
            return this.click !== null && this.click !== undefined;
        },


        /**
         * Store hover event object.
         */
        setHover: function( value ) {
            this.hover = value;
            PubSub.publish( this.getChannel(), { field: 'hover', value: value });
        },


        /**
         * Get hover event object.
         */
        getHover: function() {
            return this.hover;
        },


        /**
         * Set whether the carousel conttrols is enabled or not.
         */
        setCarouselEnabled: function( isEnabled ) {
            this.carouselEnabled = isEnabled;
            PubSub.publish( this.getChannel(), { field: 'carouselEnabled', value: isEnabled });
        },


        /**
         * Check if the carousel controls is enabled.
         */
        isCarouselEnabled: function() {
            return this.carouselEnabled;
        },


        /**
         * Returns the number of views in the layer.
         */
        getNumViews: function() {
            return this.views.length;
        },


        /**
         * Set the layers opacity.
         */
        setOpacity: function( opacity ) {
            var i;
            this.opacity = opacity;
            for (i=0; i<this.views.length; i++) {
                this.views[i].renderer.setOpacity( opacity );
            }
            PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity });
        },


        /**
         * Get the layers opacity.
         */
        getOpacity: function() {
            return this.opacity;
        },


        /**
         * Set the layers visibility.
         */
        setVisibility: function( visible ) {
            var i;
            this.visibility = visible;
            for (i=0; i<this.views.length; i++) {
                this.views[i].renderer.setVisibility( visible );
            }
            this.update();  // pull missing tiles
            PubSub.publish( this.getChannel(), { field: 'enabled', value: visible });
        },


        /**
         * Get the layers visibility.
         */
        getVisibility: function() {
            return this.visibility;
        },


        /**
         * Set the layers z index.
         */
        setZIndex: function( zIndex ) {
            var i;
            this.zIndex = zIndex;
            for (i=0; i<this.views.length; i++) {
                this.views[i].renderer.setZIndex( zIndex );
            }
            PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
        },


        /**
         * Get the layers z index.
         */
        getZIndex: function() {
            return this.zIndex;
        },


        /**
         * Set the default view index
         */
        setDefaultViewIndex: function( index ) {
            this.defaultViewIndex = index;
            this.update();
            PubSub.publish( this.getChannel(), { field: 'defaultViewIndex', value: index });
        },


        /**
         * Get the default view index
         */
        getDefaultViewIndex: function() {
            return this.defaultViewIndex;
        },


        /**
         * Set the carousel view index for a particular tile
         */
        setTileViewIndex: function( tilekey, newIndex ) {

            var that = this,
                oldIndex = this.getTileViewIndex( tilekey ),
                oldView = this.views[oldIndex],
                oldSource = oldView.source,
                oldRenderer = oldView.renderer,
                oldService = oldView.service,
                newView = this.views[newIndex],
                newSource = newView.source,
                newRenderer = newView.renderer,
                newService = newView.service;

            if ( newIndex === oldIndex ) {
                return;
            }

            // update internal state
            if ( newIndex === this.defaultViewIndex ) {
                delete this.renderersByTile[tilekey];
            } else {
                this.renderersByTile[tilekey] = newIndex;
            }

            if ( newSource.id === oldSource.id ) {
                // both renderers share the same data source, swap tile data
                // give tile to new view
                newService.data[tilekey] = oldService.data[tilekey];
                makeRedrawFunc( newRenderer, newService )();
            } else {
                // otherwise request new data
                newService.getRequest( tilekey, {}, makeRedrawFunc( newRenderer, newService, function() {
                    // on redraw check if the renderer index is still the same, it may be the case that
                    // the server took so long to respond that the this view is no longer active
                    return that.getTileViewIndex(tilekey) === newIndex;
                }));
            }

            // release and redraw to remove old data
            oldService.releaseData( tilekey );
            makeRedrawFunc( oldRenderer, oldService )();
            PubSub.publish( this.getChannel(), { field: 'tileViewIndex', value: { tilekey: tilekey, index: newIndex } } );
        },


        /**
         * Get the carousel view index for a particular tile
         */
        getTileViewIndex: function( tilekey ) {
            var index = this.renderersByTile[tilekey];
            return ( index !== undefined ) ? index : this.defaultViewIndex;
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

            if ( !this.getVisibility() ) {
                return;
            }

            for (i=0; i<this.views.length; ++i) {
                tilesByRenderer[i] = [];
            }

            // determine all tiles in view
            tiles = this.map.getTilesInView();
            tileViewBounds = this.map.getTileBoundsInView();

            // group tiles by view index
            for (i=0; i<tiles.length; ++i) {
                tilekey = tiles[i].level+','+tiles[i].xIndex+','+tiles[i].yIndex;
                rendererIndex = this.getTileViewIndex( tilekey );
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
