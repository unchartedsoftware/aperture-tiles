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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * Performs cusomizations on the main window.
 */
define(function (require) {
    "use strict";

    var FileLoader = require('./FileLoader'),
        PyramidFactory = require('./binning/PyramidFactory'),
        AvailableLayersTracker = require("./layer/AllLayers"),
        worldMap,
        findMatchingLayers,
        jsonToHTML;

	findMatchingLayers = function (layerConfigs) {
		return AvailableLayersTracker.filterLeafLayers(layerConfigs, function (layer) {
			return PyramidFactory.pyramidsEqual(layer.pyramid, worldMap.pyramid);
		});
	};

	jsonToHTML = function (json) {
		var result = "";
        switch (typeof json) {
	        case 'object': {	
		        var key;

		        result += "<table>";
		        for (key in json) {
			        if (json.hasOwnProperty(key)) {
				        result += "<tr>";
				        if (!(json instanceof Array)) {
					        result += "<td>"+key+"</td>";
				        }
				        result += "<td>"+jsonToHTML(json[key])+"</td>";
				        result += "</tr>";
			        }
		        }
		        result += "</table>";
		        break;
	        }

	        case 'number':
	        	result += json;
	        	break;
	        case 'boolean':
	        	result += json;
	        	break;
	        case 'string':
	        	result += '"'+json+'"';
	        	break;
	        default: {
	        	result += json;
	        	break;
	        }
		}

		return result;
	};

    return {
        customizeMap: function (map) {
	        worldMap = map;
        },

	    customizeLayers: function (layers) {
            var getDataButton,
                selectAreaLayer,
                drawControl,
                onFeatureDrawn,
                onDataReceived,
                dataLayers;
		    dataLayers = findMatchingLayers(layers);

			if (dataLayers.length > 0) {
				selectAreaLayer = new OpenLayers.Layer.Vector("Data selection area layer");
				worldMap.addOLLayer(selectAreaLayer);
				
				onDataReceived = function (data, status) {
					var outputWindow = window.open("", "MsgWindow", "width=500,height=800"),
					    key;
					outputWindow.document.write(jsonToHTML(data));
				};
				onFeatureDrawn = function (feature) {
					var bounds,
					    dataBounds,
					    dataRequest,
					    dataDescription,
					    xField,
					    yField;
					
					bounds = feature.geometry.getBounds();
					selectAreaLayer.removeAllFeatures();
					
					dataBounds = {
						'lowerLeft': worldMap.getCoordFromMap(bounds.left, bounds.bottom),
						'upperRight': worldMap.getCoordFromMap(bounds.right, bounds.top)
					};
					dataDescription = dataLayers[0].data.pyramidio.data;
					xField = dataDescription.oculus.binning.xField;
					yField = dataDescription.oculus.binning.yField;
					dataRequest = {
						'dataset': dataDescription,
						'requestCount': 10,
						'query': {
							'and': [{}, {}]
						}
					};
					dataRequest.query.and[0][xField] = {
						'min': dataBounds.lowerLeft.x,
						'max': dataBounds.upperRight.x
					};
					dataRequest.query.and[1][yField] = {
						'min': dataBounds.lowerLeft.y,
						'max': dataBounds.upperRight.y
					};
					
					aperture.io.rest('/data',
									 'POST',
									 onDataReceived,
									 {
										 postData: dataRequest,
										 contentType: 'application/json'
									 });
				};
				drawControl =
					new OpenLayers.Control.DrawFeature(selectAreaLayer,
													   OpenLayers.Handler.RegularPolygon,
													   {
														   featureAdded: onFeatureDrawn,
														   handlerOptions: {
															   sides: 4,
															   irregular: true
														   }
													   }
													  );
				
				worldMap.addOLControl(new OpenLayers.Control.MousePosition());
				worldMap.addOLControl(drawControl);
				
				getDataButton = $("#retrieve-data");
				getDataButton.click(function () {
					if (getDataButton.checked) {
						getDataButton.checked = false;
						drawControl.deactivate();
					} else {
						getDataButton.checked = true;
						drawControl.activate();
					}
				});
			}
				
            // Model on http://openlayers.org/dev/examples/draw-feature.html
            // Create a button to put it in data mode
            // that should activate the box draw control.
            // can't zoom map anymore.
            // draw a box, make a request.
            // worldMap.getCoordFromViewportPixel(...)

            // To get from rect to lat/lon:
            // latLon = OpenLayers.LonLat.fromString( geometry.toShortString() ),
            // getViewPortPxFromLonLat( latLon )
        }
    };
});
