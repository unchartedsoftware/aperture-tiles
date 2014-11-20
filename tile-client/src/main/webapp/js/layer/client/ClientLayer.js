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
        TileService = require('./TileService'),
        PubSub = require('../../util/PubSub'),
        makeRedrawFunc;

    makeRedrawFunc = function( layer, renderer, conditional ) {
        return function( tile ) {
            // if conditional function is provided, it must return true to execute layer redraw
            if ( !conditional || conditional() ) {
                if ( tile && layer.pendingTiles[ tile.tilekey ] ) {
                    renderer.draw(tile);
                    delete layer.pendingTiles[ tile.tilekey ];
                }
            }
        };
    };

    function ClientLayer( spec ) {
        // set reasonable defaults
        spec.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
        spec.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;
        spec.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 1000;
        spec.domain = "client";
        // call base constructor    
        Layer.call( this, spec );
        this.views = spec.views;
        this.renderersByTile = {};
        this.defaultViewIndex = 0;
        this.pendingTiles = {};
    }

    ClientLayer.prototype = Object.create( Layer.prototype );

    ClientLayer.prototype.activate = function() {
        var that = this,
            view, i;
        // set callback to update tiles on map move
        this.map.on( 'tile', function( event ) {
            that.update( event );
        });
        for (i=0; i<this.views.length; i++) {
            view = this.views[i];
            view.renderer.parent = that;
            view.renderer.map = this.map;
            view.renderer.activate();
        }
        this.setZIndex( this.spec.zIndex );
        this.setOpacity( this.spec.opacity );
        this.setVisibility( this.spec.enabled );
    };

    ClientLayer.prototype.deactivate = function() {
        // TODO:
        return true;
    };

    /**
     * Set whether the carousel controls is enabled or not.
     */
    ClientLayer.prototype.setCarouselEnabled = function( isEnabled ) {
        this.carouselEnabled = isEnabled;
        PubSub.publish( this.getChannel(), { field: 'carouselEnabled', value: isEnabled });
    };

    /**
     * Check if the carousel controls is enabled.
     */
    ClientLayer.prototype.isCarouselEnabled = function() {
        return this.carouselEnabled;
    };

    /**
     * Returns the number of views in the layer.
     */
    ClientLayer.prototype.getNumViews = function() {
        return this.views.length;
    };

    /**
     * Set the layers opacity.
     */
    ClientLayer.prototype.setOpacity = function( opacity ) {
        var i;
        this.spec.opacity = opacity;
        for (i=0; i<this.views.length; i++) {
            this.views[i].renderer.setOpacity( opacity );
        }
        PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity });
    };

    /**
     * Get the layers opacity.
     */
    ClientLayer.prototype.getOpacity = function() {
        return this.spec.opacity;
    };

    /**
     * Set the layers visibility.
     */
    ClientLayer.prototype.setVisibility = function( visible ) {
        var i;
        this.spec.enabled = visible;
        for (i=0; i<this.views.length; i++) {
            this.views[i].renderer.setVisibility( visible );
        }
        this.update({ added: this.map.getTilesInView(), removed: []});  // pull missing tiles
        PubSub.publish( this.getChannel(), { field: 'enabled', value: visible });
    };

    /**
     * Get the layers visibility.
     */
    ClientLayer.prototype.getVisibility = function() {
        return this.spec.enabled;
    };

    /**
     * Set the layers z index.
     */
    ClientLayer.prototype.setZIndex = function( zIndex ) {
        var i;
        this.spec.zIndex = zIndex;
        for (i=0; i<this.views.length; i++) {
            this.views[i].renderer.setZIndex( zIndex );
        }
        PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
    };

    /**
     * Get the layers z index.
     */
    ClientLayer.prototype.getZIndex = function() {
        return this.spec.zIndex;
    };

    /**
     * Set the carousel view index for a particular tile
     */
    ClientLayer.prototype.setTileViewIndex = function( tilekey, newIndex ) {

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
    };

    /**
     * Get the carousel view index for a particular tile
     */
    ClientLayer.prototype.getTileViewIndex = function( tilekey ) {
        var index = this.renderersByTile[tilekey];
        return ( index !== undefined ) ? index : this.defaultViewIndex;
    };

    /**
     * Map update callback, this function is called when the map view state is updating. Requests
     * and draws any new tiles
     */
    ClientLayer.prototype.update = function( tiles ) {

        var added = tiles.added,
            removed = tiles.removed,
            tilekey,
            view, renderer,
            viewIndex,
            tilesByView = [], i;

        if ( !this.getVisibility() ) {
            return;
        }

        for (i=0; i<this.views.length; ++i) {
            tilesByView[i] = {
                added: [],
                removed: []
            };
        }

        // group tiles by view index
        for (i=0; i<added.length; ++i) {
            tilekey = added[i];
            viewIndex = this.getTileViewIndex( tilekey );
            tilesByView[ viewIndex ].added.push( tilekey );
            this.pendingTiles[ tilekey ] = true;
        }
        for (i=0; i<removed.length; ++i) {
            tilekey = removed[i];
            viewIndex = this.getTileViewIndex( tilekey );
            tilesByView[ viewIndex ].removed.push( tilekey );
            delete this.pendingTiles[ tilekey ];
        }

        for (i=0; i<this.views.length; ++i) {
            view = this.views[i];
            renderer = view.renderer;
            renderer.erase( tilesByView[ i ].removed );
            TileService.getTiles( view.source,
                                  this.map.getPyramid(),
                                  tilesByView[ i ].added,
                                  makeRedrawFunc( this, renderer ) );
        }
    };

    return ClientLayer;
});
