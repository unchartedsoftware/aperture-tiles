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



define(function (require) {
    "use strict";



    var LayerMediator = require('../LayerMediator'),
        PubSub = require('../../util/PubSub'),
        ClientLayerMediator;



    ClientLayerMediator = LayerMediator.extend({
        ClassName: "ClientLayerMediator",

        init: function() {
            this._super();
        },

        registerLayers: function( layers ) {

            var i;

            function register( layer ) {

                var channel = layer.getChannel(),
                    layerSpec = layer.getLayerSpec()[0],
                    previousMouse = {};

                function updateTileFocus( x, y ) {
                    var tilekey = layer.map.getTileKeyFromViewportPixel( x, y );
                    PubSub.publish( channel, { field: 'previousTileFocus', value: layer.getTileFocus() } );
                    PubSub.publish( channel, { field: 'tileFocus', value: tilekey } );
                }

                layerSpec.enabled = ( layerSpec.enabled !== undefined ) ? layerSpec.enabled : true;
                layerSpec.opacity = ( layerSpec.opacity !== undefined ) ? layerSpec.opacity : 1.0;

                PubSub.subscribe( channel, function( message, path ) {

                    var field = message.field,
                        value = message.value;

                    switch ( field ) {

                        case "opacity":

                            layer.setOpacity( value );
                            break;

                        case "enabled":

                            layer.setVisibility( value );
                            break;

                        case "zIndex":

                            layer.setZIndex( value );
                            break;

                        case "tileFocus":

                            layer.setTileFocus( value );
                            break;

                        case "defaultRendererIndex":

                            layer.setDefaultRendererIndex( value );
                            break;

                        case "rendererByTile":

                            layer.setTileRenderer( layer.getTileFocus(), value );
                            break;

                        case "click":

                            layer.setClick( value );
                            break;
                    }

                });

                // set client-side layer state properties after binding callbacks
                PubSub.publish( channel, { field: 'zIndex', value: i+1000 } );
                PubSub.publish( channel, { field: 'enabled', value: layerSpec.enabled } );
                PubSub.publish( channel, { field: 'opacity', value: layerSpec.opacity } );
                PubSub.publish( channel, { field: 'defaultRendererIndex', value: 0 } );

                // clear click state if map is clicked
                layer.map.on( 'click', function() {
                    PubSub.publish( channel, { field: 'click', value: null } );
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
