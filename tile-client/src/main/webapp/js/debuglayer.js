/**
 * Copyright (c) 2013 Oculus Info Inc.
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
/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */
define(['class', 'maplayer'], function (Class, MapLayer) {
    "use strict";
    var DebugLayer;

    DebugLayer = MapLayer.extend({
        init: function (id) {
            this._super(id);
        },
        createLayer: function(nodeLayer) {
            this._labelLayer = this._nodeLayer.addLayer(aperture.LabelLayer);
            this._labelLayer.map('text').from('text');
            this._labelLayer.map('label-cont').asValue(1);
            this._labelLayer.map('label-visible').asValue(true);
            this._labelLayer.map('offset-y').from('offset-y');
            this._labelLayer.map('fill').from('fill');

            this.setUpdateFcn($.proxy(this.updateData, this));
        },
        updateData: function (event) {
            var bounds = this.map.map.olMap_.getExtent(),
            mapProj = this.map.projection,
            dataProj = new OpenLayers.Projection("EPSG:4326"),
            bottomLeft = new OpenLayers.Pixel(bounds.left, bounds.bottom),
            topRight = new OpenLayers.Pixel(bounds.right, bounds.top),
            pointBL = new OpenLayers.LonLat(bottomLeft.x, bottomLeft.y),
            pointTR = new OpenLayers.LonLat(topRight.x, topRight.y),
            data = [],
            index = 0,
            xAxis, yAxis, lon, correctedLon, lat, getAxisCharacteristics;

            pointBL.transform(mapProj, dataProj);
            pointTR.transform(mapProj, dataProj);

            // Generate data - points in even degree increments 
            // within the bounds
            getAxisCharacteristics = function (min, max) {
                var range, step;

                if (max < min) {
                    min = min - 360;
                }

                range = max-min;
                // Figure out the step size.  Start with the power of 
                // 10 just below the range size
                step = Math.exp(Math.LN10*Math.floor(Math.log(range)/Math.LN10));
                if (range/step < 2.5) {
                    // Way too few steps; use 2*prev power of 10
                    step = step/5;
                } else if (range/step < 5.5) {
                    // Too few steps; use 5*prev power of 10
                    step = step/2;
                }
                return {
                    start: step*Math.ceil(min/step),
                    step: step,
                    end: max
                };
            };

            xAxis = getAxisCharacteristics(pointBL.lon, pointTR.lon);
            yAxis = getAxisCharacteristics(pointBL.lat, pointTR.lat);

            for (lon = xAxis.start; lon < xAxis.end; lon += xAxis.step) {
                correctedLon = lon;
                while (correctedLon >= 180) {
                    correctedLon = correctedLon - 360;
                }
                while (correctedLon < -180) {
                    correctedLon = correctedLon + 360;
                }
                for (lat = yAxis.start; lat < yAxis.end; lat += yAxis.step) {
                    data[index++] = {
                        'text': ("["+correctedLon.toFixed(4)+(correctedLon >= 0 ? "E" : "W")+","+
                                 lat.toFixed(4)+(lat >= 0 ? "N" : "S")+"]"),
                        'longitude': correctedLon,
                        'latitude': lat,
                        'offset-y': -10,
                        'fill': 'blue'
                    };
                    data[index++] = {
                        'text': ("["+xAxis.start.toFixed(1)+" : "+lon.toFixed(1)+" : "+xAxis.end.toFixed(1)+"]"),
                        'longitude': correctedLon,
                        'latitude': lat,
                        'offset-y': 10,
                        'fill': 'red'
                    };
                }
            }

            this._nodeLayer.all(data);
            this._labelLayer.all().redraw();
        }
    });

    return DebugLayer;
});
