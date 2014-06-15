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

/*global OpenLayers*/

/**
 * This module defines a ClientLayer class which provides delegation between multiple client renderers
 * and their respective data sources. Each view is composed of a ClientRenderer and DataService. Each view has
 * a View which delegates tile requests from the view and its DataService.
 *
 * Each DataService manages a unique set of data. Upon tile requests, if data for the tile is not held in memory, it is pulled from the server.
 * Each View manages the set of tiles that a currently visible within a particular view.
 * Each ClientRenderer is responsible for drawing only the elements currently visible within a specific view.
 *
 */
define(function (require) {
    "use strict";



    var Layer = require('../Layer'),
        LayerService = require('../../LayerService'),
        TileService = require('./data/TileService'),
        ClientLayer;



    ClientLayer = Layer.extend({


        init: function ( spec, renderer, map ) {

            this._super( spec, map );
            this.renderer = renderer;
            this.tileService = null;
        },


        setOpacity: function( opacity ) {

            this.renderer.setOpacity( opacity );
        },


        setVisibility: function( visible ) {

            this.renderer.setVisibility( visible );
        },


        configure: function( callback ) {

            var that = this;

            LayerService.configureLayer( this.layerSpec, function( layerInfo, statusInfo ) {
                if (statusInfo.success) {
                    if ( that.layerInfo ) {
                        // if a previous configuration exists, release it
                        LayerService.unconfigureLayer( that.layerInfo, function() {
                            return true;
                        });
                    }
                    // set layer info
                    that.layerInfo = layerInfo;
                    that.tileService = new TileService( that.getLayerInfo(), that.map.getPyramid() );
                }

                callback( layerInfo, statusInfo );
            });
        },


        update: function( tiles ) {
            if ( this.tileService ) {
                var tileSetBounds = this.map.getTileBoundsInView();
                this.tileService.requestData( tiles, tileSetBounds, $.proxy( this.redraw, this ) );
            }
        },

        redraw: function() {

            this.renderer.redraw( this.tileService.getDataArray() );
        }

     });

    return ClientLayer;
});
