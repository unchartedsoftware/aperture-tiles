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
	     './ApertureConfig',
         './summary'],

    function (FileLoader,
    		  configureAperture,
              SummaryBuilder) {
        "use strict";

        var apertureConfigFile = "./data/aperture-config.json";
        
        // Load all our UI configuration data before trying to bring up the ui
        FileLoader.loadJSONData(apertureConfigFile, function (jsonDataMap) {


	        // First off, configure aperture.
	        configureAperture(jsonDataMap[apertureConfigFile]);        

	        var dataDirectory = $.getUrlVar('dataDir');
            var datasetName = $.getUrlVar('dataset');
            if(!dataDirectory){
                dataDirectory = "./data";
            }

            var summaryBuilder = new SummaryBuilder({
                dataDir: dataDirectory,
                dataset: datasetName
            });
            summaryBuilder.start();
	        
			$(window).resize( function() {

			});

	        // Trigger the initial resize event to resize everything
	        $(window).resize();

		});
	}
);
