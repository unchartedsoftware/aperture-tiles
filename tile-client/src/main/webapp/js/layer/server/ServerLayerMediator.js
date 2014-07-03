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
        SharedObject = require('../../util/SharedObject'),
        requestRampImage,
        ServerLayerMediator;

    /**
     * Asynchronously requests colour ramp image.
     *
     * @param {Object} layerState - The layer state object that contains
     * the parameters to use when generating the image.
     */
    requestRampImage = function ( layerState, layerInfo, level ) {

        var legendData = {
                layer: layerState.get( 'id' ),  // layer id
                id: layerInfo.id,           // config id
                level: level,
                width: 128,
                height: 1,
                orientation: "horizontal"
            };

        aperture.io.rest('/legend',
                         'POST',
                         function (legendString, status) {
                            layerState.set( 'rampImageUrl', legendString);
                         },
                         {
                             postData: legendData,
                             contentType: 'application/json'
                         });

    };


    ServerLayerMediator = LayerMediator.extend({
        ClassName: "ServerLayerMediator",

        init: function() {
            this._super();
        },


        registerLayers :  function( layers ) {

            var that = this,
                i;

            function register( layer ) {

                var map = layer.map,
                    layerState,
                    layerSpec;

                function getLevelMinMax( level ) {
                    var meta =  layer.getLayerInfo().meta,
                        minArray = meta.levelMinFreq || meta.levelMinimums,
                        maxArray = meta.levelMaxFreq || meta.levelMaximums,
                        min = minArray ? minArray[level] : 0,
                        max = maxArray[level];
                    return [ parseFloat(min), parseFloat(max) ];
                }

                // Make a callback to regen the ramp image on map zoom changes
                function mapZoomCallback() {
                    // set ramp image
                    requestRampImage( layerState, layer.getLayerInfo(), map.getZoom() );
                    // set ramp level
                    layerState.set( 'rampMinMax', getLevelMinMax( map.getZoom() ) );
                }

                layerSpec = layer.getLayerSpec();
                layerSpec.renderer.opacity  = layerSpec.renderer.opacity || 1.0;
                layerSpec.renderer.enabled = ( layerSpec.renderer.enabled !== undefined ) ? layerSpec.renderer.enabled : true;
                layerSpec.renderer.ramp = layerSpec.renderer.ramp || "ware";
                layerSpec.transform.name = layerSpec.transform.name || 'linear';
                layerSpec.legendrange = layerSpec.legendrange || [0,100];

                // Create a layer state object.  Values are initialized to those provided
                // by the layer specs, which are defined in the layers.json file, or are
                // defaulted to appropriate starting values.
                layerState = new SharedObject();
                layerState.set( 'id', layer.id );
                layerState.set( 'name', layer.name );
                layerState.set( 'domain', 'server' );
                layerState.set( 'enabled', layerSpec.renderer.enabled );
                layerState.set( 'opacity', layerSpec.renderer.opacity );
                layerState.set( 'rampFunction', layerSpec.transform.name );
                layerState.set( 'rampType', layerSpec.renderer.ramp );
                layerState.set( 'rampMinMax', getLevelMinMax( map.getZoom() ) );
                layerState.set( 'rampImageUrl', "" );
                layerState.set( 'filterRange', layerSpec.legendrange );

                /**
                 * Valid ramp type strings.
                 */
                layerState.RAMP_TYPES = [
                    {id: "ware", name: "Ware"},
                    {id: "inv-ware", name: "Inverse Ware"},
                    {id: "br", name: "Blue/Red"},
                    {id: "inv-br", name: "Inverse Blue/Red"},
                    {id: "grey", name: "Grey"},
                    {id: "inv-grey", name: "Inverse Grey"},
                    {id: "flat", name: "Flat"},
                    {id: "single-gradient", name: "Single Gradient"}
                ];

                /**
                 * Valid ramp function strings.
                 */
                layerState.RAMP_FUNCTIONS = [
                    {id: "linear", name: "Linear"},
                    {id: "log10", name: "Log 10"}
                ];

                // Register a callback to handle layer state change events.
                layerState.addListener( function (fieldName) {

                    switch (fieldName) {

                        case "opacity":

                            layer.setOpacity( layerState.get('opacity') );
                            break;

                        case "enabled":

                            layer.setVisibility( layerState.get('enabled') );
                            break;

                        case "rampType":

                            layer.setRampType( layerState.get('rampType'), function() {
                                // once configuration is received that the server has been re-configured, request new image
                                requestRampImage( layerState, layer.getLayerInfo(), map.getZoom() );
                            });
                            break;

                        case"rampFunction":

                            layer.setRampFunction( layerState.get('rampFunction') );
                            break;

                        case "filterRange":

                            layer.setFilterRange( layerState.get('filterRange') );
                            break;

                        case "zIndex":

                            layer.setZIndex( layerState.get('zIndex') );
                            break;

                    }
                });

                // set z index here so callback is executed
                layerState.set( 'zIndex', i+1 );

                // Request ramp image from server.
                requestRampImage( layerState, layer.getLayerInfo(), 0 );

                // Add the layer to the layer state array.
                that.layerStates.push( layerState );

                // Handle map zoom events - can require a re-gen of the filter image.
                map.on("zoomend", mapZoomCallback );

            }

            // ensure it is an array
            layers = ( $.isArray(layers) ) ? layers : [layers];

            for (i=0; i<layers.length; i++) {
                register( layers[i] );
            }
        }

    });

    return ServerLayerMediator;
});
