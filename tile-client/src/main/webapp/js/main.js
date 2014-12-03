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

require(['./util/Util',
         './map/Map',
         './map/Axis',
         './rest/LayerService',
         './layer/LayerUtil',
         './layer/BaseLayer',
         './layer/ServerLayer',
         './layer/ClientLayer',
         './layer/AnnotationLayer',
         './layer/renderer/TextScoreRenderer',
         './layer/renderer/WordCloudRenderer',
         './layer/renderer/TextByFrequencyRenderer',
         './layer/renderer/PointRenderer',
         './layer/renderer/PointAggregateRenderer',
         './layer/renderer/RenderTheme'],

        function( Util,
                  Map,
                  Axis,
                  LayerService,
                  LayerUtil,
                  BaseLayer,
                  ServerLayer,
                  ClientLayer,
                  AnnotationLayer,
                  TextScoreRenderer,
                  WordCloudRenderer,
                  TextByFrequencyRenderer,
                  PointRenderer,
                  PointAggregateRenderer,
                  RenderTheme ) {

	        "use strict";

            // request layers from server
            LayerService.getLayers( function( layers ) {

                // parse layers into nicer format
                layers = LayerUtil.parse( layers.layers );

                var map,
                    axis,
                    baseLayer,
                    clientLayer0,
                    annotationLayer0,
                    serverLayer0;

                baseLayer = new BaseLayer({
                    "type": "Google",
                    "theme" : "dark",
                    "options" : {
                        "name" : "Dark",
                        "type" : "styled",
                        "style" : [
                            {
                                'featureType': 'all',
                                'stylers': [
                                    { 'saturation': -100 },
                                    { 'invert_lightness' : true },
                                    { 'visibility' : 'simplified' }
                                ]
                            },
                            {
                                'featureType': 'landscape.natural',
                                'stylers': [
                                    { 'lightness': -50 }
                                ]
                            },
                            {
                                'featureType': 'poi',
                                'stylers': [
                                    { 'visibility': 'off' }
                                ]
                            },
                            {
                                'featureType': 'road',
                                'stylers': [
                                    { 'lightness': -50 }
                                ]
                            },
                            {
                                'featureType': 'road',
                                'elementType': 'labels',
                                'stylers': [
                                    { 'visibility': 'off' }
                                ]
                            },
                            {
                                'featureType': 'road.highway',
                                'stylers': [
                                    { 'lightness': -60 }
                                ]
                            },
                            {
                                'featureType': 'road.arterial',
                                'stylers': [
                                    { 'visibility': 'off' }
                                ]
                            },
                            {
                                'featureType': 'administrative',
                                'stylers': [
                                    { 'lightness': 10 }
                                ]
                            },
                            {
                                'featureType': 'administrative.province',
                                'elementType': 'geometry',
                                'stylers': [
                                    { 'lightness': 15 }
                                ]
                            },
                            {
                                'featureType' : 'administrative.country',
                                'elementType' : 'geometry',
                                'stylers' : [
                                    { 'visibility' : 'on' },
                                    { 'lightness' : -56 }
                                ]
                            },
                            {
                                'elementType' : 'labels',
                                'stylers' : [
                                    { 'lightness' : -46 },
                                    { 'visibility' : 'on' }
                                ] }
                        ]
                    }
                });

                serverLayer0 = new ServerLayer({
                    source: layers["tweet-heatmap"],
                    valueTransform: {
                        type: "log10"
                    }
                });

                clientLayer0 = new ClientLayer({
                    source: layers["top-tweets"],
                    renderer: new WordCloudRenderer({
                        text: {
                            textKey: "topic",
                            countKey: "countMonthly",
                            themes: [
                                new RenderTheme( ".dark-theme", {
                                    'color': "#FFFFFF",
                                    'color:hover': "#09CFFF",
                                    'text-shadow': "#000"
                                })
                            ]
                        },
                        hook: function( elem, entry, entries, data ) {
                            elem.onclick = function() {
                                console.log( elem );
                                console.log( entry );
                            };
                        }
                    })
                });

                annotationLayer0 = new AnnotationLayer({
                    source: layers["parlor-annotations"],
                    renderer: new PointAggregateRenderer({
                        point: {
                            x: "x",
                            y: "y",
                            themes: [
                                new RenderTheme( ".dark-theme", {
                                    'background-color': "rgba( 9, 207, 255, 0.5 )",
                                    'background-color:hover': "rgba( 9, 207, 255, 0.75 )"
                                })
                            ]
                        },
                        aggregate: {
                            themes: [
                                new RenderTheme( ".dark-theme", {
                                    'background-color': "rgba(0,0,0,0)",
                                    'border': "#000"
                                })
                            ]
                        },
                        hook: function( elem, entry, entries, data ) {
                            elem.onclick = function() {
                                console.log( elem );
                                console.log( entry );
                            };
                        }
                    })
                });

                /*
                clientLayer0 = new ClientLayer({
                    source: layers["top-tweets"],
                    renderer: new TextScoreRenderer({
                        text: {
                            textKey: "topic",
                            countKey : "countMonthly",
                            themes: [
                                new RenderTheme( ".dark-theme", {
                                    'color': "#FFFFFF",
                                    'color:hover': "#09CFFF",
                                    'text-shadow': "#000"
                                })
                            ]
                        }
                    })
                });

                clientLayer0 = new ClientLayer({
                    source: layers["top-tweets"],
                    renderer: new TextByFrequencyRenderer({
                        text: {
                            textKey: "topic",
                            themes: [
                                new RenderTheme( ".dark-theme", {
                                    'color': "#FFFFFF",
                                    'color:hover': "#09CFFF",
                                    'text-shadow': "#000"
                                })
                            ]
                        },
                        frequency: {
                            countKey: "countPerHour",
                            themes: [
                                new RenderTheme( ".dark-theme", {
                                    'background-color': "#FFFFFF",
                                    'background-color:hover': "#09CFFF",
                                    'border': "#000"
                                })
                            ]
                        }
                    })
                });
                */

                /*
                axis = new Axis({
                    position: 'bottom',
                    title: 'Longitude',
                    intervals: {
                        type: 'fixed',
                        increment: 120
                    },
                    units: {
                        type: 'degrees'
                    }
                });
                */

                map = new Map( "map" );
                map.add( baseLayer );
                //map.add( serverLayer0 );
                //map.add( annotationLayer0 );
                //map.add( clientLayer0 );
                //map.addAxis( axis );

            });
        });
