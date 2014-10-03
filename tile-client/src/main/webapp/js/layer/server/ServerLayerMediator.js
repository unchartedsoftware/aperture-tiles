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
        requestRampImage,
        ServerLayerMediator;

    /**
     * Asynchronously requests colour ramp image.
     *
     * @param {Object} layer - The layer object
     */
    requestRampImage = function ( layer, layerInfo, level ) {

        var legendData = {
                layer: layer.id,  // layer id
                id: layerInfo.id, // config uuid
                level: level,
                width: 128,
                height: 1,
                orientation: "horizontal"
            };

        aperture.io.rest('/legend',
                         'POST',
                         function (legendString, status) {
                            PubSub.publish( layer.getChannel(), { field: 'rampImageUrl', value: legendString } );
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

            var i;

            function register( layer ) {

                var map = layer.map,
                    channel = layer.getChannel(),
                    layerSpec;

                function getLevelMinMax() {
                    var zoomLevel = map.getZoom(),
                        coarseness = layer.getLayerSpec().coarseness,
                        adjustedZoom = zoomLevel - (coarseness-1),
                	    meta =  layer.getLayerInfo().meta,
                    	minArray = (meta && (meta.levelMinFreq || meta.levelMinimums || meta[adjustedZoom])),
                    	maxArray = (meta && (meta.levelMaxFreq || meta.levelMaximums || meta[adjustedZoom])),
                    	min = minArray ? ($.isArray(minArray) ? minArray[adjustedZoom] : minArray.minimum) : 0,
                    	max = maxArray ? ($.isArray(maxArray) ? maxArray[adjustedZoom] : maxArray.maximum) : 0;
	                return [ parseFloat(min), parseFloat(max) ];
                }

                // Make a callback to regen the ramp image on map zoom changes
                function mapZoomCallback() {
                    // set ramp image
                    requestRampImage( layer, layer.getLayerInfo(), map.getZoom() );
                    // set ramp level
                    PubSub.publish( channel, { field: 'rampMinMax', value: getLevelMinMax() } );
                }

                // set reasonable defaults
                layerSpec = layer.getLayerSpec();
                layerSpec.renderer.opacity  = layerSpec.renderer.opacity || 1.0;
                layerSpec.renderer.enabled = ( layerSpec.renderer.enabled !== undefined ) ? layerSpec.renderer.enabled : true;
                layerSpec.renderer.ramp = layerSpec.renderer.ramp || "ware";
                layerSpec.transform.name = layerSpec.transform.name || 'linear';
                layerSpec.legendrange = layerSpec.legendrange || [0,100];
                layerSpec.transformer = layerSpec.transformer || {};
                layerSpec.transformer.type = layerSpec.transformer.type || "generic";
                layerSpec.transformer.data = layerSpec.transformer.data || {};
                layerSpec.coarseness = ( layerSpec.coarseness !== undefined ) ?  layerSpec.coarseness : 1;

                /**
                 * Valid ramp type strings.
                 */
                layer.RAMP_TYPES = [
                    {id: "ware", name: "Luminance"},
                    {id: "inv-ware", name: "Inverse Luminance"},
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
                layer.RAMP_FUNCTIONS = [
                    {id: "linear", name: "Linear"},
                    {id: "log10", name: "Log 10"}
                ];

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

                        case "coarseness":

                            layer.setCoarseness( value );
                            PubSub.publish( channel, { field: 'rampMinMax', value: getLevelMinMax() } );
                            break;

                        case "rampType":

                            layer.setRampType( value, function() {
                                // once configuration is received that the server has been re-configured, request new image
                                requestRampImage( layer, layer.getLayerInfo(), map.getZoom() );
                            });
                            break;

                        case "rampFunction":

                            layer.setRampFunction( value );
                            break;

                        case "rampMinMax":

                            layer.setRampMinMax( value );
                            break;

                        case "rampImageUrl":

                            layer.setRampImageUrl( value );
                            break;

                        case "filterRange":

                            layer.setFilterRange( value );
                            break;

                        case "transformerType":

                            layer.setTransformerType( value );
                            break;

                        case "transformerData":

                            layer.setTransformerData( value );
                            break;

                        case "zIndex":

                            layer.setZIndex( value );
                            break;

                        case "theme":

                            if ( value === "light" ) {
                                PubSub.publish( channel, { field: 'rampType', value: "inv-ware" } );
                            } else {
                                PubSub.publish( channel, { field: 'rampType', value: "ware" } );
                            }
                            break;
                    }
                });

                // set client-side layer state properties after binding callbacks
                PubSub.publish( channel, { field: 'zIndex', value: i+1 });
                PubSub.publish( channel, { field: 'enabled', value: layerSpec.renderer.enabled });
                PubSub.publish( channel, { field: 'opacity', value: layerSpec.renderer.opacity });
                PubSub.publish( channel, { field: 'coarseness', value: layerSpec.coarseness });

                // Request ramp image from server
                requestRampImage( layer, layer.getLayerInfo(), 0 );

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
