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
        ClientLayerState = require('./ClientLayerState'),
        ClientLayerMediator;



    ClientLayerMediator = LayerMediator.extend({
        ClassName: "ClientLayerMediator",

        init: function() {
            this._super();
        },

        registerLayers: function( layers ) {

            var that = this,
                i;

            function register( layer, index ) {

                var layerState,
                    previousMouse = {},
                    i;

                function updateTileFocus( x, y ) {

                    var tilekey = layer.map.getTileKeyFromViewportPixel( x, y );
                    layerState.setTileFocus( tilekey );
                }

                // Create a layer state object for the base map.
                layerState = new ClientLayerState( layer.id );
                layerState.setName( layer.getLayerSpec().name || layer.id );
                layerState.setEnabled( true );
                layerState.setOpacity( 1.0 );
                layerState.setZIndex( 1000+i );
                layerState.setRendererCount( layer.renderers.length );
                layerState.setDefaultRendererIndex( 0 );
                // register layer state with each renderer
                for (i=0; i< layer.renderers.length; i++) {
                    layer.renderers[i].registerLayer( layerState );
                }

                // Register a callback to handle layer state change events.
                layerState.addListener( function( fieldName ) {
                    var tilekey;


                    switch (fieldName) {

                        case "opacity":

                            layer.setOpacity( layerState.getOpacity() );
                            break;

                        case "enabled":

                            layer.setVisibility( layerState.isEnabled() );
                            break;

                        case "tileFocus":

                            layer.setTileFocus( layerState.getTileFocus() );
                            break;

                        case "defaultRendererIndex":

                            layer.setDefaultRendererIndex( layerState.getDefaultRendererIndex() );
                            break;

                        case "tileRendererIndex":

                            tilekey = layerState.getTileFocus();
                            layer.setTileRenderer( tilekey, layerState.getRendererByTile( tilekey ) );
                            break;
                    }

                });

                // clear click state if map is clicked
                layer.map.on( 'click', function() {
                    layerState.setClickState( {} );
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
                register( layers[i], i );
            }

        }

    });
    return ClientLayerMediator;
});
