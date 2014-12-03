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
         './map/Map',
         './layer/LayerService',
         './layer/LayerControls',
         './layer/server/ServerLayerFactory',
         './layer/client/ClientLayerFactory',
         './layer/client/CarouselControls',
         './layer/annotation/AnnotationLayerFactory'],

        function( configureAperture,
                  UICustomization,
                  OverlayButton,
                  Util,
                  Map,
                  LayerService,
                  LayerControls,
                  ServerLayerFactory,
                  ClientLayerFactory,
                  CarouselControls,
                  AnnotationLayerFactory) {

	        "use strict";

	        var apertureConfigFile = "data/aperture-config.json",
	            layerConfigFile = "data/layer-config.json",
	            mapConfigFile = "data/map-config.json",
	            viewConfigFile = "data/view-config.json",
	            apertureDeferred,
	            layerDeferred,
	            mapDeferred,
	            viewDeferred,
	            layerControlsContent,
	            worldMap;

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
             * Iterate through client layers, and append zIndex to
             * mirror the top-down ordering set in the config file.
             */
            function getClientLayers( layerConfig, layerInfos ) {
                var Z_INDEX_OFFSET = 1000,
                    layers = layerConfig.filter( function( elem ) {
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
            }

            /**
             * Iterate through annotation layers, and append zIndex to
             * mirror the top-down ordering set in the config file.
             */
	        function getAnnotationLayers( layerConfig, layerInfos ) {
		        var Z_INDEX_OFFSET = 500,
		            layers = layerConfig.filter( function( elem ) {
	                    return elem.domain === "annotation";
	                }),
		            i;
		        for (i=0; i<layers.length; i++) {
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

                var viewsOverlay,
                    viewIndex,
                    viewLink,
                    viewEntry,
                    currentView,
                    baseLayerIndex,
                    map,
                    layers,
                    i;

                function addBaseLayerToURL() {
                    var href = $(this).attr('href');
                    $(this).attr('href', href + "&baselayer="+worldMap.getBaseLayerIndex() );
                }

                // read map and base layer index parameters from url, if present
                viewIndex = Util.getURLParameter('view');
                if ( !viewIndex ) {
                    viewIndex = 0;
                }

                baseLayerIndex = Util.getURLParameter('baselayer');
                if ( !baseLayerIndex ) {
                    baseLayerIndex = 0;
                }

                if ( viewConfig.length > 1 ) {
                    // create the view panel
                    viewsOverlay = new OverlayButton({
                        id:'views',
                        header: 'Views',
                        content: '',
                        horizontalPosition: 'right',
                        verticalPosition: 'bottom'
                    });
                    // insert all view contents into overlay
                    for ( i=0; i<viewConfig.length; ++i ) {
                        viewLink = $('<a/>').attr({
                            'href': '?view='+i,
                            'class': 'views-link'
                        });
                        // on click, inject the base layer index
                        viewLink.click( addBaseLayerToURL );
                        viewEntry = $('<div class="views-entry" style="text-align:center;"></div>').append( viewLink );
                        viewLink.append( viewConfig[i].name + '<br>' );
                        viewsOverlay.getContentElement().append( viewEntry );
                    }
                }

                currentView = viewConfig[ viewIndex ];

                layers = [];
                for ( i=0; i<currentView.layers.length; i++ ) {
                    layers.push( layerConfig[ currentView.layers[i] ] );
                }
                map = mapConfig[ currentView.map ];
                map.map.baseLayerIndex = baseLayerIndex;

                return {
                    layers: layers,
                    map: map
                };
	        }

	        /**
	         * GET the description html and create an overlay button
	         * containing it.
	         */
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

            /**
             * Create an empty overlay button for the layer controls
             */
            layerControlsContent = new OverlayButton({
                id:'layer-controls',
                header: 'Controls',
                content: '',
                horizontalPosition: 'right',
                verticalPosition: 'bottom'
            }).getContentElement();

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
				            clientLayers,
				            serverLayers,
				            annotationLayers,
				            clientLayerFactory,
				            serverLayerFactory,
				            annotationLayerFactory,
                            clientLayerDeferreds,
                            serverLayerDeferreds,
                            annotationLayerDeferreds;

				        view = assembleView( layerConfig, mapConfig, viewConfig );

				        worldMap = new Map( "map", view.map );   // create map

				        // ... perform any project-specific map customizations ...
				        if ( UICustomization.customizeMap ) {
					        UICustomization.customizeMap( worldMap );
				        }

				        clientLayers = getClientLayers( view.layers, layers );
				        serverLayers = getServerLayers( view.layers, layers );
                        annotationLayers = getAnnotationLayers( view.layers, layers );

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
