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
         './rest/LayerService',
         './layer/LayerUtil',
         './layer/BaseLayer',
         './layer/ServerLayer',
         './layer/ClientLayer',
         './layer/renderer/GraphLabelRenderer',
         './layer/renderer/GraphNodeRenderer',
         './layer/renderer/RenderTheme'],

        function( Util,
                  Map,
                  LayerService,
                  LayerUtil,
                  BaseLayer,
                  ServerLayer,
                  ClientLayer,
                  GraphLabelRenderer,
                  GraphNodeRenderer,
                  RenderTheme ) {

	        "use strict";

            // request layers from server
            LayerService.requestLayers( function( layers ) {

                // parse layers into nicer format
                layers = LayerUtil.parse( layers );

                var map,
                    baseLayer,
                    clientLayer0,
                    clientLayer1,
                    serverLayer0,
                    serverLayer1,
                    serverLayer2;

                baseLayer = new BaseLayer();

                serverLayer0 = new ServerLayer({
                    source: layers["graph-nodes-xy"],
                    valueTransform: {
                        type: "log10"
                    },
                    renderer : {
                        ramp: "flat"
                    }
                });

                serverLayer1 = new ServerLayer({
                    source: layers["graph-intra-edges"],
                    valueTransform: {
                        type: "log10"
                    },
                    renderer : {
                        ramp: "hot"
                    }
                });

                serverLayer2 = new ServerLayer({
                    source: layers["graph-inter-edges"],
                    valueTransform: {
                        type: "log10"
                    },
                    renderer : {
                        ramp: "hot",
                        rangeMin: 60,
                        rangeMax: 100
                    }
                });

                clientLayer0 = new ClientLayer({
                    source: layers["graph-labels"],
                    renderer: new GraphLabelRenderer({
                        text: {
                            idKey: "id",
                            x : "x",
                            y : "y",
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

                clientLayer1 = new ClientLayer({
                    source: layers["graph-nodes"],
                    renderer: new GraphNodeRenderer({
                        node : {
                            x : "x",
                            y : "y",
                            radius: "r",
                            themes: [
                                new RenderTheme( ",dark-theme", {
                                    'background-color' : "rgba(0,0,0,0)",
                                    'border' : "rgb(78,205,196)",
                                    'background-color:hover' : "rgba(78,205,196,0.2)"
                                })
                            ]
                        },
                        criticalNode : {
                            flag : "isPrimaryNode",
                            y : "y",
                            themes: [
                                new RenderTheme( ".dark-theme", {
                                    'background-color' : "rgba(0,0,0,0)",
                                    'border' : "rgb(255,255,255)",
                                    'background-color:hover' : "rgba(255,255,255,0.2)"
                                })
                            ]
                        },
                        parentNode : {
                            id: "parentID",
                            radius: "parentR",
                            x: "parentX",
                            y: "parentY",
                            themes: [
                                new RenderTheme( ".dark-theme", {
                                    'background-color' : "rgba(0,0,0,0)",
                                    'border' : "#555",
                                    'background-color:hover' : "rgba(0,0,0,0)"
                                })
                            ]
                        }
                    })
                });

                map = new Map( "map", {
                    pyramid : {
                        type : "AreaOfInterest",
                        minX : 0,
                        maxX : 256,
                        minY : 0,
                        maxY : 256
                    }
                });
                map.add( baseLayer );
                map.add( serverLayer2 );
                map.add( serverLayer1 );
                map.add( serverLayer0 );
                map.add( clientLayer1 );
                map.add( clientLayer0 );
            });
        });
