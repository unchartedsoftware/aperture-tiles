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
require(['./FileLoader',
         './ApertureConfig',
         './ui/OverlayButton',
         './map/MapService',
         './map/Map',
         './binning/PyramidFactory',
         './layer/LayerService',
         './customization',
         './layer/view/server/ServerLayerFactory',
         './layer/view/client/ClientLayerFactory',
         './annotation/AnnotationLayerFactory',
         './layer/controller/LayerControls',
         './layer/controller/UIMediator'
        ],

        function (FileLoader, 
                  configureAperture,
                  OverlayButton,
                  MapService,
                  Map,
                  PyramidFactory,
                  LayerService,
                  UICustomization,
                  ServerLayerFactory,
                  ClientLayerFactory,
                  AnnotationLayerFactory,
                  LayerControls,
                  UIMediator) {

	        "use strict";

	        var apertureConfigFile = "data/aperture-config.json",
	            cloneObject,
	            getLayers,
	            getAnnotationLayers,
	            uiMediator,
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

	        // Get the layers from a layer tree that match a given filter and 
	        // pertain to a given domain.
	        getLayers = function (domain, rootNode, filterFcn) {
		        // Filter function to filter out all layers not in the desired 
		        // domain (server or client)
		        var domainFilterFcn = function (domain) {
			        return function (layer) {
				        var accepted = false;
				        if (filterFcn(layer)) {
					        layer.renderers.forEach(function (renderer, index, renderers) {
						        if (domain === renderer.domain) {
							        accepted = true;
							        return;
						        }
					        });
				        }
				        return accepted;
			        };
		        };

		        // Run our filter, and on the returned nodes, return a renderer 
		        // configuration appropriate to that domain.
		        return LayerService.filterLeafLayers(
			        rootNode,
			        domainFilterFcn(domain)
		        ).map(function (layer, index, layersList) {
			        // For now, just use the first appropriate configuration we find.
			        var config;

			        layer.renderers.forEach(function (renderer, index, renderers) {
				        if (domain === renderer.domain) {
					        config = cloneObject(renderer);
					        config.layer = layer.id;
					        config.name = layer.name;
					        return;
				        }
			        });

			        return config;
		        });
	        };

	        getAnnotationLayers = function( allLayers, filter ) {
		        var i, validLayers =[];
		        for (i=0; i<allLayers.length; i++) {

			        if ( filter( allLayers[i] ) ) {
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
		        annotationsDeferred = AnnotationLayerFactory.requestLayers();

		        $.when(mapsDeferred, layersDeferred, annotationsDeferred).done(
			        function (maps, layers, annotationLayers) {
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
				            serverLayers;

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
                                    href: '?map='+i
                                });
                                mapButton.append(maps[i].description+'<br>');
                                mapsOverlay.getContentElement().append( mapButton );
                            }
				        }

				        // Initialize our map...
				        currentMap = getURLParameter('map');
				        if (!currentMap || !maps[currentMap]) {
					        currentMap = 0;
				        }
				        mapConfig = maps[currentMap];
				        worldMap = new Map("map", mapConfig);
				        // ... (set up our map axes) ...
				        worldMap.setAxisSpecs(MapService.getAxisConfig(mapConfig));
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
					        return PyramidFactory.pyramidsEqual(mapPyramid, layer.pyramid);
				        };
				        clientLayers = getLayers("client", layers, filter);
				        serverLayers = getLayers("server", layers, filter);
                        annotationLayers = getAnnotationLayers( annotationLayers, filter );

				        if (UICustomization.customizeLayers) {
					        UICustomization.customizeLayers(layers);
				        }

				        uiMediator = new UIMediator();

				        // Create client, server and annotation layers
				        ClientLayerFactory.createLayers(clientLayers, uiMediator, worldMap);
                        AnnotationLayerFactory.createLayers( annotationLayers, worldMap );

                        $.when(ServerLayerFactory.createLayers(serverLayers, uiMediator, worldMap)).done( function( layersFromServer ) {

                            layerInfo = layersFromServer.getSubLayerInfosById();
                            var layer,
                                layerInfo,
                                filterAxisConfig;

                            for(layer in layerInfo){
                                if(layerInfo.hasOwnProperty( layer )){
                                    filterAxisConfig = layerInfo[ layer ].meta;
                                }
                            }
                            filterAxisConfig.worldMap = worldMap;
                            new LayerControls('layer-controls-content', uiMediator.getLayerStateMap(), filterAxisConfig).noop();
                        });
			        }
		        );

		        // Trigger the initial resize event to resize everything
		        $(window).resize();
	        }, 'json');
        });
