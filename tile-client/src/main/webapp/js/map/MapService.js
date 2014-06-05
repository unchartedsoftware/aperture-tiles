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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


/**
 * This module handles communications with the server to get the list of 
 * maps the server can support
 */
define(function (require) {
    "use strict";

    var mapDeferred;

	return {
		requestMaps: function () {
            if (!mapDeferred) {
                mapDeferred = $.Deferred();
                aperture.io.rest('/maps',
                                 'GET',
                                 function (maps, status) {
                                     if (status.success) {
                                         mapDeferred.resolve(maps);
                                     } else {
                                         mapDeferred.fail(status);
                                     }
                                 },
                                 {});
            }
            return mapDeferred;
		},

		getAxisConfig: function (mapConfig) {
			var axisConfig, pyramidConfig, i;

			axisConfig = mapConfig.AxisConfig;
			if (!axisConfig.boundsConfigured) {
				// bounds haven't been copied to the axes from the pyramid; copy them.
				axisConfig.boundsConfigured = true;
				pyramidConfig = mapConfig.PyramidConfig;

                for (i=0; i<axisConfig.length; i++) {
                    // TEMPORARY, eventually these bounds wont be needed, the axis will generate
                    // the increments solely based off the Map.pyramid
                    if ( pyramidConfig.type === "AreaOfInterest") {
                        if (axisConfig[i].position === 'top' || axisConfig[i].position === 'bottom' ) {
                            axisConfig[i].min = pyramidConfig.minX;
                            axisConfig[i].max = pyramidConfig.maxX;
                        } else {
                            axisConfig[i].min = pyramidConfig.minY;
                            axisConfig[i].max = pyramidConfig.maxY;
                        }
                    } else {
                        // web mercator
                        if (axisConfig[i].position === 'top' || axisConfig[i].position === 'bottom' ) {
                            axisConfig[i].min = -180.0;
                            axisConfig[i].max = 180.0;
                        } else {
                            axisConfig[i].min = -85.05;
                            axisConfig[i].max = 85.05;
                        }
                    }

                }
			}

			return axisConfig;
		},

        /**
         *
         * @param mapConfig
         * @param plotDiv optional div container id of the plot - useful when multiple maps are present
         * @returns {*}
         */
        setTileBorderConfig: function (mapConfig, plotDiv){
            var olTileImageConfig = mapConfig.TileBorderConfig;

            //if it is not defined, don't set border style
            if(!olTileImageConfig){
                return;
            }

            if(olTileImageConfig === 'default'){
                olTileImageConfig = {
                    "color" : "rgba(255, 255, 255, .5)",
                    "style" : "solid",
                    "weight" : "1px"
                };
            }

            //set individual defaults if they are omitted.
            if(!olTileImageConfig.color){
                olTileImageConfig.color = "rgba(255, 255, 255, .5)";
            }

            if(!olTileImageConfig.style){
                olTileImageConfig.style = "solid";
            }

            if(!olTileImageConfig.weight){
                olTileImageConfig.weight = "1px";
            }

            $(document.body).prepend(
                $('<style type="text/css"> #' + plotDiv + ' .olTileImage {' +
                    'border-left : ' + olTileImageConfig.weight + ' ' + olTileImageConfig.style + ' ' + olTileImageConfig.color +
                    '; border-top : ' + olTileImageConfig.weight + ' ' + olTileImageConfig.style + ' ' + olTileImageConfig.color +';}' +
                    ' </style>')
            );
        }
	};
});
