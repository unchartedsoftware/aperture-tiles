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
         './layer/LayerService',
         './layer/BaseLayer',
         './layer/ServerLayer',
         './layer/ClientLayer',
         './layer/AnnotationLayer',
         './layer/renderer/TextScoreRenderer',
         './layer/renderer/WordCloudRenderer',
         './layer/renderer/TextByFrequencyRenderer',
         './layer/renderer/PointRenderer',
         './layer/renderer/RenderTheme'],

        function( Util,
                  Map,
                  LayerService,
                  BaseLayer,
                  ServerLayer,
                  ClientLayer,
                  AnnotationLayer,
                  TextScoreRenderer,
                  WordCloudRenderer,
                  TextByFrequencyRenderer,
                  PointRenderer,
                  RenderTheme ) {

	        "use strict";

            // request layers from server
            LayerService.requestLayers( function( layers ) {

                var map,
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
                    html: new WordCloudRenderer({
                        textKey: "topic",
                        countKey : "countMonthly",
                        themes: [
                            new RenderTheme({
                                id: "dark-theme",
                                color: "#FFFFFF",
                                hoverColor: "#09CFFF",
                                outline: "#000"
                            })
                        ]
                    })
                });

                annotationLayer0 = new AnnotationLayer({
                    source: layers["parlor-annotations"],
                    html: new PointRenderer({})
                });

                /*
                clientLayer0 = new ClientLayer({
                    source: layers["top-tweets"],
                    html: new TextScoreRenderer({
                        textKey: "topic",
                        countKey : "countMonthly",
                        themes: [
                            new RenderTheme({
                                id: "dark-theme",
                                color: "#FFFFFF",
                                hoverColor: "#09CFFF",
                                outline: "#000"
                            })
                        ]
                    })
                });

                clientLayer0 = new ClientLayer({
                    source: layers["top-tweets"],
                    html: new TextByFrequencyRenderer({
                        textKey: "topic",
                        countKey : "countPerHour",
                        themes: [
                            new RenderTheme({
                                id: "dark-theme",
                                color: "#FFFFFF",
                                hoverColor: "#09CFFF",
                                outline: "#000"
                            })
                        ]
                    })
                });
                */

                map = new Map( "map" );
                map.add( baseLayer );
                map.add( serverLayer0 );
                map.add( annotationLayer0 );
                map.add( clientLayer0 );
            });
        });
