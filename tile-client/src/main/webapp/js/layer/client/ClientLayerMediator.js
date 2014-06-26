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

/* JSLint global declarations: these objects don't need to be declared. */
/*global define, console, $, aperture*/

/**
 * Populates the LayerState model based on the contents of a the layer, and makes the appropriate
 * modifications to it as the LayerState model changes.
 */
define(function (require) {
    "use strict";



    var LayerMediator = require('../LayerMediator'),
        LayerState = require('../LayerState'),
        ClientLayerMediator;



    ClientLayerMediator = LayerMediator.extend({
        ClassName: "ClientLayerMediator",

        init: function() {
            this._super();
        },

        registerLayers: function( layers ) {

            var that = this,
                i;

            function register( layer ) {

                var layerState,
                    previousMouse = {},
                    j;

                function updateTileFocus( x, y ) {

                    var tilekey = layer.map.getTileKeyFromViewportPixel( x, y );
                    layerState.set( 'previousTileFocus', layerState.get('tileFocus') );
                    layerState.set( 'tileFocus', tilekey );
                }

                // Create a layer state object for the base map.
                layerState = new LayerState( layer.id, layer.name, 'client' );
                layerState.set( 'enabled', true );
                layerState.set( 'opacity', 1.0 );
                layerState.set( 'zIndex', 1000+i );
                layerState.set( 'rendererCount', layer.renderers.length );
                layerState.set( 'defaultRendererIndex', 0 );
                // register layer state with each renderer
                for (j=0; j< layer.renderers.length; j++) {
                    layer.renderers[j].registerLayer( layerState );
                }

                // Register a callback to handle layer state change events.
                layerState.addListener( function( fieldName ) {
                    var tilekey;


                    switch (fieldName) {

                        case "opacity":

                            layer.setOpacity( layerState.get( 'opacity' ) );
                            break;

                        case "enabled":

                            layer.setVisibility( layerState.get( 'enabled' ) );
                            break;

                        case "z-index":

                            layer.setZIndex( layerState.get( 'zIndex' ) );
                            break;

                        case "tileFocus":

                            layer.setTileFocus( layerState.get( 'tileFocus' ) );
                            break;

                        case "defaultRendererIndex":

                            layer.setDefaultRendererIndex( layerState.get( 'defaultRendererIndex' ) );
                            break;

                        case "rendererByTile":

                            tilekey = layerState.get( 'tileFocus' );
                            layer.setTileRenderer( tilekey, layerState.get( 'rendererByTile', tilekey ) );
                            break;
                    }

                });

                // clear click state if map is clicked
                layer.map.on( 'click', function() {
                    layerState.set( 'click', {} );
                });

                // set tile focus callbacks
                layer.map.on('mousemove', function(event) {
                    updateTileFocus( event.xy.x, event.xy.y );
                    previousMouse.x = event.xy.x;
                    previousMouse.y = event.xy.y;
                });
                layer.map.on('zoomend', function(event) {
                    updateTileFocus( previousMouse.x, previousMouse.y );
                });

                // Add the layer to the layer state array.
                that.layerStates.push( layerState );
            }

            // ensure it is an array
            layers = ( $.isArray(layers) ) ? layers : [layers];

            for (i=0; i<layers.length; i++) {
                register( layers[i] );
            }

        }

    });
    return ClientLayerMediator;
});
