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
/*global OpenLayers */

require(['./FileLoader',
         './ApertureConfig',
         './ui/OverlayButton',
         './map/MapTracker',
         './map/Map',
         './binning/PyramidFactory',
         './layer/AllLayers',
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
                  MapTracker,
                  Map,
                  PyramidFactory,
                  AvailableLayersTracker,
                  UICustomization,
                  ServerLayerFactory,
                  ClientLayerFactory,
                  AnnotationLayerFactory,
                  LayerControls,
                  UIMediator) {
	        "use strict";

	        var apertureConfigFile = "./data/aperture-config.json",
	            cloneObject,
	            getLayers,
	            getAnnotationLayers,
	            uiMediator;

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
		        return AvailableLayersTracker.filterLeafLayers(
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
	        $.get("description.html", function (data) {
		        // create the overlay container
		        new OverlayButton({
			        id:'description',
			        active: false,
			        activeWidth: '50%',
			        text: 'Description',
			        css: {
				        right: '10px',
				        top: '10px'
			        }
		        }).append(data);
		        // append description html

	        });
	        
	        // Load all our UI configuration data before trying to bring up the ui
	        FileLoader.loadJSONData(apertureConfigFile, function (jsonDataMap) {

		        // First off, configure aperture.
		        configureAperture(jsonDataMap[apertureConfigFile]);

		        // Get our list of maps
		        MapTracker.requestMaps(function (maps) {
			        // For now, just use the first map
			        var mapConfig = maps[0],
			            worldMap,
			            mapPyramid;

			        // Initialize our map...
			        worldMap = new Map("map", mapConfig);
			        // ... (set up our map axes) ...
			        worldMap.setAxisSpecs(MapTracker.getAxisConfig(mapConfig));
			        // ... perform any project-specific map customizations ...
			        if (UICustomization.customizeMap) {
				        UICustomization.customizeMap(worldMap);
			        }
			        // ... and request relevant data layers
			        mapPyramid = mapConfig.PyramidConfig;

			        AvailableLayersTracker.requestLayers(
				        function (layers) {
					        // TODO: Make it so we can pass the pyramid up to the server
					        // in the layers request, and have it return only portions
					        // of the layer tree that match that pyramid.
					        // Eventually, we should let the user choose among them.
					        var filter = function (layer) {
						        return PyramidFactory.pyramidsEqual(mapPyramid, layer.pyramid);
					        },
					            clientLayers = getLayers("client", layers, filter),
					            serverLayers = getLayers("server", layers, filter);

					        if (UICustomization.customizeLayers) {
						        UICustomization.customizeLayers(layers);
					        }

					        uiMediator = new UIMediator();
					        
					        // Create client and server layers
					        ClientLayerFactory.createLayers(clientLayers, uiMediator, worldMap);
					        ServerLayerFactory.createLayers(serverLayers, uiMediator, worldMap);

					        new LayerControls().initialize( uiMediator.getLayerStateMap() );

					        AnnotationLayerFactory.requestLayers(
						        function( layers ) {

							        var annotationLayers = getAnnotationLayers(layers, filter);
							        AnnotationLayerFactory.createLayers( annotationLayers, worldMap );

						        }
					        );

					        // Trigger the initial resize event to resize everything
					        $(window).resize();
				        }
			        );

		        });
	        });
        });
