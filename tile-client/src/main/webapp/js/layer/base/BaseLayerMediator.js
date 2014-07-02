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
        BaseLayerMediator;



    BaseLayerMediator = LayerMediator.extend({
        ClassName: "BaseLayerMediator",

         init: function() {
             this._super();
         },


        registerLayers: function( map ) {

            var layerState,
                zoomOrPan,
                prevState;

            // Create a layer state object for the base map.
            layerState = new LayerState( map.id, "Base Layer", 'base' );
            layerState.BASE_LAYERS = map.baseLayers;
            layerState.set( 'enabled', true );
            layerState.set( 'opacity', 1.0 );
            layerState.set( 'zIndex', -1 );
            layerState.set( 'baseLayerIndex', 0 );

            // Register a callback to handle layer state change events.
            layerState.addListener( function( fieldName ) {

                switch (fieldName) {

                    case "opacity":

                        map.setOpacity( layerState.get('opacity') );
                        break;

                    case "enabled":

                        map.setVisibility( layerState.get('enabled') );
                        break;

                    case "baseLayerIndex":

                        map.setBaseLayerIndex( layerState.get('baseLayerIndex') );
                        if ( layerState.BASE_LAYERS[ layerState.get('baseLayerIndex') ].type !== "BlankBase" ) {
                            map.setOpacity( layerState.get('opacity') );
                            map.setVisibility( layerState.get('enabled') );
                        }
                        break;
                }

            });

            map.on('movestart', function( event ) {
                zoomOrPan = ( event.object.dragging ) ? 'pan' : 'zoom';
                if ( zoomOrPan === 'pan' ) {
                    prevState = {
                        x: event.object.maxPx.x,
                        y: event.object.maxPx.y
                    };
                }
            });

            map.on('moveend', function( event ) {
                var obj;
                if ( zoomOrPan === 'zoom' ) {
                    obj = map.getZoom();
                } else {
                    obj = {
                        dx: event.object.maxPx.x - prevState.x,
                        dy: event.object.maxPx.y - prevState.y
                    };
                }
                layerState.set( zoomOrPan, obj );
            });

            // Add the layer to the layer state array.
            this.layerStates.push( layerState );
        }

    });
    return BaseLayerMediator;
});
