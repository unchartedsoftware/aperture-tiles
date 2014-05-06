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
            var majorCitiesFile = "./data/majorCities.json";

            FileLoader.loadJSONData(majorCitiesFile, function (jsonDataMap) {
                var majorCitiesDropDown;

                // Add major cities entries to zoom select box
                majorCitiesDropDown = $("#select-city-zoom");
                $.each(jsonDataMap[majorCitiesFile], function(key) {
                    majorCitiesDropDown.append(
                        $('<option></option>').val(key).html(this.text)
                    );
                });
                // Set city zoom callback function
                majorCitiesDropDown.change( function() {
                    var majorCitiesMap = jsonDataMap[majorCitiesFile];
                    worldMap.map.zoomTo( majorCitiesMap[this.value].lat,
                                         majorCitiesMap[this.value].long, 8);
                });

                // Zoom to the area of the world with the data.
                worldMap.map.zoomTo( -15, -60, 4 );
            });
        }
    };
});
