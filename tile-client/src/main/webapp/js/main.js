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
         './customization',
         './ui/OverlayButton',
         './binning/PyramidFactory',
         './map/MapService',
         './map/Map',
         './layer/LayerService',
         './layer/LayerControls',
         './layer/server/ServerLayerFactory',
         './layer/client/ClientLayerFactory',
         './layer/client/CarouselControls',
         './layer/annotation/AnnotationService',
         './layer/annotation/AnnotationLayerFactory'
        ],

        function (configureAperture,
                  UICustomization,
                  OverlayButton,
                  PyramidFactory,
                  MapService,
                  Map,
                  LayerService,
                  LayerControls,
                  ServerLayerFactory,
                  ClientLayerFactory,
                  CarouselControls,
                  AnnotationService,
                  AnnotationLayerFactory) {

	        "use strict";

	        var apertureConfigFile = "data/aperture-config.json",
	            cloneObject,
	            getLayers,
	            getServerLayers,
	            getClientLayers,
	            groupClientLayers,
	            getAnnotationLayers,
	            getURLParameter,
	            mapsDeferred, layersDeferred, annotationsDeferred;

	        getURLParameter = function (key) {
		        var url = window.location.search.substring(1),
		            urlVars = url.split('&'),
		            i, varKey,
		            result = 0;
		        for (i=0; i<urlVars.length; ++i) {
			        varKey = urlVars[i].split('=');
			        if (key === varKey[0]) {
				        result = varKey[1];
				        break;
			        }
		        }
		        return result;
	        };

	        cloneObject = function (base) {
		        var result, key;
		        if ($.isArray(base)) {
			        result = [];
		        } else {
			        result = {};
		        }
		        for (key in base) {
			        if (base.hasOwnProperty(key)) {
				        if ("object" === typeof(base[key])) {
					        result[key] = cloneObject(base[key]);
				        } else {
					        result[key] = base[key];
				        }
			        }
		        }

		        return result;
	        };

	        groupClientLayers = function( clientLayers ) {
                var layer, layerName, i, layersByName = {}, layersByOrder = [];
                // group client layers by name while maintaining order in config file
                for ( i=0; i<clientLayers.length; i++ ) {
                    layer = clientLayers[i];
                    layerName = layer.name;
                    if ( layersByName[ layerName ] === undefined ) {
                        layersByName[ layerName ] = [];
                        layersByOrder.push( layersByName[ layerName ] );
                    }
                    layersByName[ layerName ].push( layer );
                }
                return layersByOrder;
            };

	        getLayers = function( rootNode, filter, domain ) {
	            var layers = LayerService.filterLeafLayers( rootNode ),
                    layer, configurations = [], config, renderer,
                    i, j;
                for (i=0; i<layers.length; i++) {
                    layer = layers[i];
                    if ( filter( layer ) ) {
                        for ( j=0; j<layer.renderers.length; j++ ) {
                            renderer = layer.renderers[j];
                            if ( renderer.domain === domain ) {
                                config = cloneObject( renderer );
                                if (layer.data.transformer) {
                                    config.transformer = cloneObject( layer.data.transformer );
                                }
                                config.layer = layer.id;
                                config.name = layer.name;
                                configurations.push( config );
                                console.log( config.zIndex );
                            }
                        }
                    }
                }
                return configurations;
	        };

	        getServerLayers = function( rootNode, filter ) {
	            var layers = getLayers( rootNode, filter, 'server' ),
	                zIndex = 1, i;
	            for ( i=0; i<layers.length; i++ ) {
	                layers[i].zIndex = zIndex++;
	            }
	            return layers;
            };

            getClientLayers = function( rootNode, filter ) {
                var layers = getLayers( rootNode, filter, 'client' ),
                    zIndex = 1000, i;
                for ( i=0; i<layers.length; i++ ) {
                    layers[i].zIndex = zIndex++;
                }
                return groupClientLayers( layers );
            };

	        getAnnotationLayers = function( allLayers, filter ) {
		        var i, validLayers =[],
		            zIndex = 500;
		        for (i=0; i<allLayers.length; i++) {
			        if ( filter( allLayers[i] ) ) {
			            allLayers[i].zIndex = zIndex++;
				        validLayers.push( allLayers[i] );
			        }
		        }
		        return validLayers;
	        };

	        // Create description element
	        $.get("description.html", function (descriptionHtml) {
		        // create the overlay container
		        new OverlayButton({
		            id:'description',
                    header: 'Description',
                    content: descriptionHtml
		        }).getContentElement().append(''); // jslint...
	        });

            new OverlayButton({
                id:'layer-controls',
                header: 'Controls',
                content: ''
            }).getContentElement().append(''); // jslint...

	        // Load all our UI configuration data before trying to bring up the ui
	        $.get( apertureConfigFile, function( apertureConfig ) {

		        // First off, configure aperture.
		        configureAperture( apertureConfig );

		        // Get our list of maps and layers
		        mapsDeferred = MapService.requestMaps();
		        layersDeferred = LayerService.requestLayers();
		        annotationsDeferred = AnnotationService.requestLayers();

		        $.when (mapsDeferred, layersDeferred, annotationsDeferred ).done(
			        function ( maps, layers, annotationLayers ) {
				        // For now, just use the first map
				        var currentMap,
				            mapConfig,
				            worldMap,
				            mapPyramid,
				            mapsOverlay,
				            mapButton,
				            i,
				            filter,
				            clientLayers,
				            serverLayers,
				            clientLayerFactory,
				            serverLayerFactory,
				            annotationLayerFactory,
                            clientLayerDeferreds,
                            serverLayerDeferreds,
                            annotationLayerDeferreds;

				        // Initialize our map choice panel
				        if (maps.length > 1) {

					        // ... first, create the panel
					        mapsOverlay = new OverlayButton({
                                id:'maps',
                                header: 'Maps',
                                content: ''
                            });

                            // ... Next, insert contents
                            for (i=0; i<maps.length; ++i) {
                                mapButton = $('<a/>').attr({
                                    'href': '?map='+i,
	                                'class': 'maps-link'
                                });
                                mapButton.append(maps[i].description+'<br>');
                                mapsOverlay.getContentElement().append( mapButton );
                            }
				        } else {
					        // Only one map. Remove the maps div.
					        $("#maps").remove();
				        }

				        // Initialize our map...
				        currentMap = getURLParameter('map');
				        if ( !currentMap || !maps[currentMap] ) {
					        currentMap = 0;
				        }

				        mapConfig = maps[currentMap];

				        worldMap = new Map("map", mapConfig);   // create map
				        worldMap.setAxisSpecs( MapService.getAxisConfig(mapConfig) ); // set axes

				        // ... perform any project-specific map customizations ...
				        if (UICustomization.customizeMap) {
					        UICustomization.customizeMap(worldMap);
				        }
				        // ... and request relevant data layers
				        mapPyramid = mapConfig.PyramidConfig;

				        // TODO: Make it so we can pass the pyramid up to the server
				        // in the layers request, and have it return only portions
				        // of the layer tree that match that pyramid.
				        // Eventually, we should let the user choose among them.
				        filter = function (layer) {
					        return PyramidFactory.pyramidsEqual( mapPyramid, layer.pyramid );
				        };

				        clientLayers = getClientLayers( layers, filter );
				        serverLayers = getServerLayers( layers, filter );
                        annotationLayers = getAnnotationLayers( annotationLayers, filter );

				        if (UICustomization.customizeLayers) {
					        UICustomization.customizeLayers(layers);
				        }

                        clientLayerFactory = new ClientLayerFactory();
                        serverLayerFactory = new ServerLayerFactory();
                        annotationLayerFactory = new AnnotationLayerFactory();

				        // Create client, server and annotation layers
				        clientLayerDeferreds = clientLayerFactory.createLayers( clientLayers, worldMap );
				        serverLayerDeferreds = serverLayerFactory.createLayers( serverLayers, worldMap );
                        annotationLayerDeferreds = annotationLayerFactory.createLayers( annotationLayers, worldMap );

                        $.when( clientLayerDeferreds, serverLayerDeferreds, annotationLayerDeferreds ).done( function( clientLayers, serverLayers, annotationLayers ) {

                            var layers = [];

                            // customize layers
                            if ( UICustomization.customizeLayers ) {
                                UICustomization.customizeLayers( clientLayers, serverLayers, annotationLayers );
                            }

                            // merge all layers into single array
                            $.merge( layers, [ worldMap ] );
                            $.merge( layers, clientLayers );
                            $.merge( layers, serverLayers );
                            $.merge( layers, annotationLayers );

                            // create layer controls
                            new LayerControls( 'layer-controls-content', layers, UICustomization.customizeSettings ).noop();
                            // create the carousel controls
                            new CarouselControls( clientLayers, worldMap ).noop();
                        });

			        }
		        );

		        // Trigger the initial resize event to resize everything
		        $(window).resize();
	        }, 'json');
        });
