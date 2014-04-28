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

    var FileLoader = require('./FileLoader');

    return {
        customizeMap: function (worldMap) {
            var getDataButton,
                selectAreaLayer,
                drawControl;

	        selectAreaLayer = new OpenLayers.Layer.Vector("Data selection area layer");
	        worldMap.addOLLayer(selectAreaLayer);
	        drawControl =
		        new OpenLayers.Control.DrawFeature(selectAreaLayer,
		                                           OpenLayers.Handler.RegularPolygon,
		                                           {
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
