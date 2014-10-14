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
         './util/Util',
         './binning/PyramidFactory',
         './map/MapService',
         './map/Map',
         './layer/LayerService',
         './layer/LayerControls',
         './layer/server/ServerLayerFactory',
         './layer/client/ClientLayerFactory',
         './layer/client/CarouselControls',
         './layer/annotation/AnnotationService',
         './layer/annotation/AnnotationLayerFactory'],

        function (configureAperture,
                  UICustomization,
                  OverlayButton,
                  Util,
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
	            getLayers,
	            getServerLayers,
	            getClientLayers,
	            groupClientLayers,
	            groupClientLayerViews,
	            getAnnotationLayers,
	            layerControlsContent,
	            mapsDeferred, layersDeferred, annotationsDeferred;

	        groupClientLayerViews = function( subLayers ) {
	            var spec = {},
	                subLayer, i, j;
	            // set base spec from first declared client layer
                spec.domain = subLayers[0].domain;
                spec.name = subLayers[0].name;
                spec.opacity = subLayers[0].opacity;
                spec.enabled = subLayers[0].enabled;
                spec.views = [];
	            for ( i=0; i<subLayers.length; i++ ) {
	                subLayer = subLayers[i];
	                for ( j=0; j<subLayer.renderers.length; j++ ) {
                        spec.views.push({
                            layer : subLayer.layer,
                            renderer : subLayer.renderers[j],
                            details : subLayer.details
                        });
	                }
	            }
                return spec;
            };

	        groupClientLayers = function( clientLayers ) {
                var layer, layerName,
                    layersByName = {}, layersByOrder = [],
                    layerSpecs = [],
                    i;
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
                // iterate over individual client layers and assemble view specs
                for ( i=0; i<layersByOrder.length; i++ ) {
                    layerSpecs.push( groupClientLayerViews( layersByOrder[i] ) );
                }
                return layerSpecs;
            };

	        getLayers = function( rootNode, filter, domain ) {
	            var layers = LayerService.filterLeafLayers( rootNode ),
                    layer, configurations = [], config, renderer,
                    i, j;
                for (i=0; i<layers.length; i++) {
                    layer = layers[i];
                    // filter layer based on function
                    if ( filter( layer ) ) {
                        for ( j=0; j<layer.renderers.length; j++ ) {
                            renderer = layer.renderers[j];
                            // if renderer matches domain
                            if ( renderer.domain === domain ) {
                                // we only needed certain parts of the spec objects
                                config = _.cloneDeep( renderer );
                                if (layer.data.transformer) {
                                    config.transformer = _.cloneDeep( layer.data.transformer );
                                }
                                config.layer = layer.id;
                                config.name = layer.name;
                                configurations.push( config );
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
                // group client layers based on common name
                layers = groupClientLayers( layers );
                for ( i=0; i<layers.length; i++ ) {
                    layers[i].zIndex = zIndex++;
                }
                return layers;
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
	        $.get("description.html", function( descriptionHtml ) {
		        // create the overlay container
		        new OverlayButton({
		            id:'description',
                    header: 'Description',
                    content: descriptionHtml,
                    horizontalPosition: 'right',
                    verticalPosition: 'top'
		        }).getContentElement().append(''); // jslint...
	        });

            // create layer controls
            layerControlsContent = new OverlayButton({
                id:'layer-controls',
                header: 'Controls',
                content: '',
                horizontalPosition: 'right',
                verticalPosition: 'bottom'
            }).getContentElement();

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
				            currentBaseLayer,
				            addBaseLayerToURL,
				            mapConfig,
				            worldMap,
				            viewsOverlay,
				            viewLink,
				            viewEntry,
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

				        // Initialize our view choice panel
				        if (maps.length > 1) {

					        // ... first, create the panel
					        viewsOverlay = new OverlayButton({
                                id:'views',
                                header: 'Views',
                                content: '',
                                horizontalPosition: 'right',
                                verticalPosition: 'bottom'
                            });

                            addBaseLayerToURL = function() {
                                var href = $(this).attr('href');
                                $(this).attr('href', href + "&baselayer="+worldMap.getBaseLayerIndex() );
                            };

                            // ... Next, insert contents
                            for (i=0; i<maps.length; ++i) {
                                viewLink = $('<a/>').attr({
                                    'href': '?map='+i,
	                                'class': 'views-link'
                                });

                                // on click, inject the base layer index
                                viewLink.click( addBaseLayerToURL );

                                viewEntry = $('<div class="views-entry" style="text-align:center;"></div>').append( viewLink );
                                viewLink.append(maps[i].description+'<br>' );
                                viewsOverlay.getContentElement().append( viewEntry );
                            }
				        }

				        // read map and base layer index parameters from url, if present
				        currentMap = Util.getURLParameter('map');
				        currentBaseLayer = Util.getURLParameter('baselayer');

				        if ( !currentMap || !maps[currentMap] ) {
					        currentMap = 0;
				        }

				        mapConfig = maps[currentMap];

                        mapConfig.MapConfig.baseLayerIndex = ( currentBaseLayer !== undefined ) ? currentBaseLayer : 0;

				        worldMap = new Map( "map", mapConfig );   // create map

				        // ... perform any project-specific map customizations ...
				        if ( UICustomization.customizeMap ) {
					        UICustomization.customizeMap( worldMap );
				        }

				        // TODO: Make it so we can pass the pyramid up to the server
				        // in the layers request, and have it return only portions
				        // of the layer tree that match that pyramid.
				        // Eventually, we should let the user choose among them.
				        filter = function (layer) {
					        return PyramidFactory.pyramidsEqual( mapConfig.PyramidConfig, layer.pyramid );
				        };

				        clientLayers = getClientLayers( layers, filter );
				        serverLayers = getServerLayers( layers, filter );
                        annotationLayers = getAnnotationLayers( annotationLayers, filter );

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
                            new LayerControls( layerControlsContent, layers, UICustomization.customizeSettings ).noop();
                            // create the carousel controls
                            new CarouselControls( clientLayers, worldMap ).noop();
                        });

			        }
		        );

		        // Trigger the initial resize event to resize everything
		        $(window).resize();
	        }, 'json');
        });
