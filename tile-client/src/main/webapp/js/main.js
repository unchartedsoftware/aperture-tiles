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
	            layerConfigFile = "data/layers-config.json",
	            apertureDeferred = $.Deferred(),
	            layerDeferred = $.Deferred(),
	            getServerLayers,
	            getClientLayers,
	            getAnnotationLayers,
	            layerControlsContent,
	            mapsDeferred,
	            layersDeferred;

            /**
             * Iterate through server layers, and append zIndex to
             * mirror the top-down ordering set in the config file.
             */
	        getServerLayers = function( layerConfig, layerInfos ) {
	            var Z_INDEX_OFFSET = 1,
	                layers = layerConfig.layers.filter( function( elem ) {
	                    return elem.domain === "server";
	                }),
	                i;
	            for ( i=0; i<layers.length; i++ ) {
	                layers[i].source = layerInfos[ layers[i].source ];
	                layers[i].zIndex = Z_INDEX_OFFSET + ( layers.length - i );
	            }
	            return layers;
            };

            /**
             * Iterate through client layers, and append zIndex to
             * mirror the top-down ordering set in the config file.
             */
            getClientLayers = function( layerConfig, layerInfos ) {
                var Z_INDEX_OFFSET = 1000,
                    layers = layerConfig.layers.filter( function( elem ) {
	                    return elem.domain === "client";
	                }),
                    i, j;
                for ( i=0; i<layers.length; i++ ) {
                    layers[i].zIndex = Z_INDEX_OFFSET + ( layers.length - i );
                    for ( j=0; j<layers[i].views.length; j++ ) {
                        layers[i].views[j].source = layerInfos[ layers[i].views[j].source ];
                    }
                }
                return layers;
            };

            /**
             * Iterate through annotation layers, and append zIndex to
             * mirror the top-down ordering set in the config file.
             */
	        getAnnotationLayers = function( layerConfig, layerInfos ) {
		        var Z_INDEX_OFFSET = 500,
		            layers = layerConfig.layers.filter( function( elem ) {
	                    return elem.domain === "annotation";
	                }),
		            i;
		        for (i=0; i<layers.length; i++) {
		            layers[i].source = layerInfos[ layers[i].source ];
                    layers[i].zIndex = Z_INDEX_OFFSET + ( layers.length - i );
		        }
		        return layers;
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

            $.getJSON( apertureConfigFile, function( apertureConfig ) {
                apertureDeferred.resolve( apertureConfig );
            });

            $.getJSON( layerConfigFile, function( layerConfig ) {
                layerDeferred.resolve( layerConfig );
            });

            $.when( apertureDeferred, layerDeferred ).done( function( apertureConfig, layerConfig ) {

		        // First off, configure aperture.
		        configureAperture( apertureConfig );

		        // Get our list of maps and layers
		        mapsDeferred = MapService.requestMaps();
		        layersDeferred = LayerService.requestLayers();

		        $.when ( mapsDeferred, layersDeferred ).done(

			        function ( maps, layers ) {
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
				            clientLayers,
				            serverLayers,
				            annotationLayers,
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

				        clientLayers = getClientLayers( layerConfig, layers );
				        serverLayers = getServerLayers( layerConfig, layers );
                        annotationLayers = getAnnotationLayers( layerConfig, layers );

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
