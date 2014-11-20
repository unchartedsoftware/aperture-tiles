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
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* global OpenLayers */
require(['./ApertureConfig',
         './util/Util',
         './map/Map',
         './layer/LayerService',
        './layer/base/BaseLayer',
         './layer/server/ServerLayer'],

        function( configureAperture,
                  Util,
                  Map,
                  LayerService,
                  BaseLayer,
                  ServerLayer ) {

	        "use strict";

	        var apertureConfigFile = "data/aperture-config.json",
	            layerConfigFile = "data/layer-config.json",
	            mapConfigFile = "data/map-config.json",
	            viewConfigFile = "data/view-config.json",
	            apertureDeferred,
	            layerDeferred,
	            mapDeferred,
	            viewDeferred,
	            map;

            /**
             * Iterate through server layers, and append zIndex to
             * mirror the top-down ordering set in the config file.
             */
	        function getServerLayers( layerConfig, layerInfos ) {
	            var Z_INDEX_OFFSET = 1,
	                layers = layerConfig.filter( function( elem ) {
	                    return elem.domain === "server";
	                }),
	                i;
	            for ( i=0; i<layers.length; i++ ) {
	                layers[i].source = layerInfos[ layers[i].source ];
	                layers[i].zIndex = Z_INDEX_OFFSET + ( layers.length - i );
	            }
	            return layers;
            }

            /**
             * GET a configuration file from server and resolve the returned
             * $.Deferred when it is received.
             */
	        function getConfigFile( file ) {
	            var deferred = $.Deferred();
	            $.getJSON( file )
                    .done( function( config ) {
                        deferred.resolve( config );
                    })
                    .fail( function( jqxhr, textStatus, error ) {
                        var err = textStatus + ", " + error;
                        console.log( "Request to GET "+file+" failed: " + err );
                    });
                return deferred;
	        }

            /**
             * Assemble current view from view configuration. If there are multiple view,
             * create the corresponding overlay button.
             */
	        function assembleView( layerConfig, mapConfig, viewConfig ) {

                var currentView,
                    map,
                    layers,
                    i;

                currentView = viewConfig[ 0 ];

                layers = [];
                for ( i=0; i<currentView.layers.length; i++ ) {
                    layers.push( layerConfig[ currentView.layers[i] ] );
                }
                map = mapConfig[ currentView.map ];

                return {
                    layers: layers,
                    map: map
                };
	        }

            /*
             * GET all client-side configuration files
             */
            apertureDeferred = getConfigFile( apertureConfigFile );
            layerDeferred = getConfigFile( layerConfigFile );
            mapDeferred = getConfigFile( mapConfigFile );
            viewDeferred = getConfigFile( viewConfigFile );

            $.when( apertureDeferred,
                    layerDeferred,
                    mapDeferred,
                    viewDeferred ).done( function( apertureConfig, layerConfig, mapConfig, viewConfig ) {

                var layersDeferred = $.Deferred();

		        // First off, configure aperture.
		        configureAperture( apertureConfig );

		        // Get our list of layers
		        LayerService.requestLayers( function( layers ) {
		            layersDeferred.resolve( layers );
		        });

		        $.when( layersDeferred ).done( function ( layers ) {
				        var view,
				            baseLayer,
				            serverLayer;

				        view = assembleView( layerConfig, mapConfig, viewConfig );

                        baseLayer = new BaseLayer();

                        map = new Map( mapConfig );
                        map.add( baseLayer );

				        serverLayer = new ServerLayer( getServerLayers( view.layers, layers )[0] );
                        map.add( serverLayer );
			        }
		        );
	        }, 'json');
        });
