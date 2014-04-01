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
         './map/Map',
         './layer/view/server/ServerLayerFactory',
         './layer/view/client/ClientLayerFactory',
         './annotation/AnnotationLayerFactory',
         './annotation/AnnotationTracker'
        ],

        function (FileLoader, 
        	      Map,
                  ServerLayerFactory,
                  ClientLayerFactory,
                  AnnotationLayerFactory,
                  AnnotationTracker) {
            "use strict";

            var mapFile = "./data/map.json",
                layersFile = "./data/layers.json";

            // Load all our UI configuration data before trying to bring up the ui
            FileLoader.loadJSONData(mapFile, layersFile, function (jsonDataMap) {
                // We have all our data now; construct the UI.
                var worldMap,
                    /*******************/
                    /*
                    defaultStyle,
                    hoverStyle,
                    selectStyle,
                    temporaryStyle,
                    
                    vector,
                    addPointControl,
                    selectControl,
                    dragControl,
                    highlightControl,
                    */
                    annotation, 
                    a;
                    /*******************/

                // Create world map and axes from json file under mapFile
                worldMap = new Map("map", jsonDataMap[mapFile]);

                // Create client and server layers
                annotation = AnnotationLayerFactory.createLayers(jsonDataMap[layersFile].AnnotationLayers, worldMap);
                ClientLayerFactory.createLayers(jsonDataMap[layersFile].ClientLayers, worldMap);
                ServerLayerFactory.createLayers(jsonDataMap[layersFile].ServerLayers, worldMap);

                // Trigger the initial resize event to resize everything
                $(window).resize();

                annotation = annotation[0];

                a = {
                        x: "12.45",
                        y: "56.78",
                        priority : "P0",
                        data: {
                            comment: "derpderp"
                        }
                    };

                annotation.tracker.postAnnotation( a );

/*
                AnnotationTracker.getData( "0,0,0", function( anno ) {
                    console.log( anno );
                });
*/
                /*****************************************************************/
                /*
                defaultStyle = new OpenLayers.Style({
                    externalGraphic: 'http://www.openlayers.org/dev/img/marker.png', 
                    graphicWidth: 21, 
                    graphicHeight: 25,
                    graphicYOffset: -24,
                    'cursor': 'pointer'              
                });

                hoverStyle = new OpenLayers.Style({
                    externalGraphic: 'http://www.openlayers.org/dev/img/marker-green.png', 
                    graphicWidth: 21, 
                    graphicHeight: 25,
                    graphicYOffset: -24,
                    'cursor': 'pointer'                 
                });

                selectStyle = new OpenLayers.Style({
                    externalGraphic: 'http://www.openlayers.org/dev/img/marker-blue.png',
                    graphicWidth: 21, 
                    graphicHeight: 25,
                    graphicYOffset: -24,
                    'cursor': 'pointer'                  
                });

                temporaryStyle = new OpenLayers.Style({
                    display:"none"               
                });

                vector = new OpenLayers.Layer.Vector( "Vectors Layer", { 
                    ratio: 2,
                    styleMap: new OpenLayers.StyleMap({ 
                        "default" : defaultStyle,
                        "hover": hoverStyle,
                        "select": selectStyle,
                        "temporary": temporaryStyle
                    }),
                    eventListeners: {
                        "featureselected": function(e) {
                        
                            var latlon,
                                px,
                                size,
                                popup = new OpenLayers.Popup("marker-popup",
                                                         OpenLayers.LonLat.fromString(e.feature.geometry.toShortString()),
                                                         null,
                                                         "<div style='padding-top:5px; padding-left:15px;'>"+
                                                         "<div style='font-weight:bold; padding-bottom:10px'>Title Here</div>"+
                                                         "<div style='width:200px;height:80px;overflow:auto;'>"+
                                                         "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>"+
                                                         "<button style='margin-top:10px; border-radius:5px;'>Button 1</button>"+
                                                         "<button style='margin-top:10px; border-radius:5px;'>Button 2</button>"+
                                                         "<button style='margin-top:10px; border-radius:5px;'>Button 3</button>"+
                                                         "</div>",
                                                         true);

                            
                            popup.backgroundColor = '#222222';
                            popup.autoSize = true;
                            popup.panMapIfOutOfView = true;
                            e.feature.popup = popup;
                            worldMap.map.olMap_.addPopup(popup, false);

                            latlon = OpenLayers.LonLat.fromString(e.feature.geometry.toShortString());
                            px = worldMap.map.olMap_.getLayerPxFromViewPortPx( worldMap.map.olMap_.getPixelFromLonLat(latlon) );
                            size = popup.size;
                            px.x -= size.w / 2;
                            px.y -= size.h + 25;
                            popup.moveTo( px );
                        },
                        "featureunselected": function(e) {
                            worldMap.map.olMap_.removePopup(e.feature.popup);
                            e.feature.popup.destroy();
                            e.feature.popup = null;
                        }
                    }
                });

                worldMap.map.olMap_.addLayer(vector);

                addPointControl = new OpenLayers.Control.DrawFeature(vector, OpenLayers.Handler.Point);
                selectControl = new OpenLayers.Control.SelectFeature(vector, {
                    clickout: true
                });

                dragControl = new OpenLayers.Control.DragFeature(vector, {
                    onStart: function(feature, pixel){
                        selectControl.clickFeature(feature);
                    },
                    onDrag: function(feature, pixel){

                        var latlon = OpenLayers.LonLat.fromString(feature.geometry.toShortString()),
                            px = worldMap.map.olMap_.getLayerPxFromViewPortPx( worldMap.map.olMap_.getPixelFromLonLat(latlon) ),
                            size = feature.popup.size;
                        px.x -= size.w / 2;
                        px.y -= size.h + 25;
                        feature.popup.moveTo( px );
                    }
                                                       
                });
                highlightControl = new OpenLayers.Control.SelectFeature(vector, {
                    hover: true,
                    highlightOnly: true,
                    renderIntent: "hover"

                });

                worldMap.map.olMap_.addControl(addPointControl);
                worldMap.map.olMap_.addControl(dragControl);
                worldMap.map.olMap_.addControl(highlightControl);
                worldMap.map.olMap_.addControl(selectControl);

                addPointControl.activate();
                dragControl.activate();
                highlightControl.activate();
                selectControl.activate();
                */
                /*****************************************************************/
            });
        });
