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



    var Class = require('../../class'),
        ServerLayerState = require('../model/ServerLayerState'),
        setupRampImage,
        ServerLayerMediator;

    /**
     * Asynchronously updates colour ramp image.
     *
     * @param {Object} layerState - The layer state object that contains
     * the parameters to use when generating the image.
     */
    setupRampImage = function ( layerState, layerInfo, level ) {

        var legendData = {
                transform: layerState.getRampFunction(),
                layer: layerState.getId(),  // layer id
                id: layerInfo.id,           // config id
                level: level,
                width: 128,
                height: 1,
                orientation: "horizontal",
                ramp: layerState.getRampType()
            };

        console.log( "req new ramp: " + layerState.getRampType() + ", " + layerState.getRampFunction());
        aperture.io.rest('/legend',
                         'POST',
                         function (legendString, status) {
                            console.log( "got new ramp : " + legendString);
                            layerState.setRampImageUrl(legendString);
                         },
                         {
                             postData: legendData,
                             contentType: 'application/json'
                         });

    };


    ServerLayerMediator = Class.extend({
        ClassName: "ServerLayerMediator",

        init: function() {
            this.layerStateMap = {};
        },


        registerLayers :  function( layers ) {

            var that = this,

                i;

            function register( layer ) {

                var map = layer.map,
                    layerState,
                    layerSpec;

                // A callback to modify map / visual state in response to layer changes.
                function makeLayerStateCallback() {
                    // Create layer state objects from the layer specs provided by the server rendered map layer.
                    return function (fieldName) {
                        if (fieldName === "opacity") {
                            layer.setOpacity( layerState.getOpacity() );
                        } else if (fieldName === "enabled") {
                            layer.setVisibility( layerState.isEnabled() );
                        } else if (fieldName === "rampType") {
                            layer.setRampType( layerState.getRampType() );
                            setupRampImage( layerState, layer.getLayerInfo(), map.getZoom() );
                        } else if (fieldName === "rampFunction") {
                            layer.setRampFunction( layerState.getId(), layerState.getRampFunction() );
                            setupRampImage( layerState, layer.getLayerInfo(), map.getZoom() );
                        } else if (fieldName === "filterRange") {
                            layer.setFilterRange( layerState.getFilterRange(), 0 );
                        } else if (fieldName === "zIndex") {
                            layer.setZIndex( layerState.getZIndex() );
                        }
                    };
                }

                function getLevelMinMax( level ) {
                    var meta =  layer.getLayerInfo().meta,
                        minArray = meta.levelMinFreq || meta.levelMinimums,
                        maxArray = meta.levelMaxFreq || meta.levelMaximums,
                        min = minArray[level],
                        max = maxArray[level];
                    return [ parseFloat(min), parseFloat(max) ];
                }

                // Make a callback to regen the ramp image on map zoom changes
                function makeMapZoomCallback() {
                    return function () {
                        // set ramp image
                        setupRampImage( layerState, layer.getLayerInfo(), map.getZoom() );
                        // set ramp level
                        layerState.setRampMinMax( getLevelMinMax( map.getZoom() ) );
                    };
                }

                layerSpec = layer.getLayerSpec();

                // Create a layer state object.  Values are initialized to those provided
                // by the layer specs, which are defined in the layers.json file, or are
                // defaulted to appropriate starting values.
                layerState = new ServerLayerState( layer.id );
                layerState.setName( layerSpec.name || layer.id );
                layerState.setEnabled( true );
                layerState.setOpacity( layerSpec.renderer.opacity );
                layerState.setRampFunction( layerSpec.transform.name );
                layerState.setRampType( layerSpec.renderer.ramp );
                layerState.setRampMinMax( getLevelMinMax( map.getZoom() ) );
                layerState.setFilterRange( [0.0, 1.0] );
                layerState.setZIndex( i+1 );

                // Register a callback to handle layer state change events.
                layerState.addListener( makeLayerStateCallback() );

                // Request ramp image from server.
                setupRampImage( layerState, layer.getLayerInfo(), 0 );

                // Add the layer to the layer statemap.
                that.layerStateMap[ layerState.getId() ] = layerState;

                // Handle map zoom events - can require a re-gen of the filter image.
                map.on("zoomend", makeMapZoomCallback() );

            }

            // ensure it is an array
            layers = ( $.isArray(layers) ) ? layers : [layers];

            for (i=0; i<layers.length; i++) {
                register( layers[i] );
            }
        },

        getLayerStateMap: function() {
            return this.layerStateMap;
        }

    });

    return ServerLayerMediator;
});
