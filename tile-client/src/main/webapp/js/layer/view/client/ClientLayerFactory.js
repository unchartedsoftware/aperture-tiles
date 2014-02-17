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
 * This module when given a client layer json object contains all layer data, will load the required classes and build
 * the layers
 */
define( function (require) {
    "use strict";
		
	var LayerDataLoader = require('./data/LayerDataLoader'),
		DataLayer = require('../../DataLayer');	
		
	return {
	
		/**
		 * Given a layer JSON specification object and a map, this function will pull data information from the server and load
		 * required layer and renderer class modules using require.js. Once everything is ready, constructs individual layers.
		 * @param layerJSON	 	layer specification JSON object loaded from layers.json
		 * @param map			map object from map.js
		 */
		createLayers: function(layerJSON, map) {
			var i = 0;
			for (i=0; i<layerJSON.length; i++) {   
				this.createLayer(layerJSON[i], map);
			}		
		},
	

		createLayer: function(layerJSON, map) {

			var requirements = [],	// this is an array of requirement spec objects. Each entry is used to load the individual requirements	
				i;
	
			// load module func
			function loadModule(arg, callback) {
				require([arg], callback);
			}
			
			// get layer info from server func	
			function getLayerInfoFromServer(arg, callback) {
				var layerInfoListener = new DataLayer([arg]);
				layerInfoListener.setRetrievedCallback(callback);
				layerInfoListener.retrieveLayerInfo();
			}	
			
			// add layer view controller to requirements
			requirements.push({
				type : "view-controller",
				id : layerJSON.type,
				spec : "./" + layerJSON.type,
				func : loadModule				
				});	
				
			// add view dependencies to requirements
			for (i=0; i<layerJSON.views.length; i++) {   
			
				// get renderer class from require.js
				requirements.push({
					type : "renderer",
					id : layerJSON.views[i].renderer,
					spec : "./impl/" + layerJSON.views[i].renderer,
					func : loadModule
					});
				// get data tracker from server
				requirements.push({
					type : "data-tracker",
					id : layerJSON.views[i].layer,
					spec : layerJSON.views[i],
					func : getLayerInfoFromServer
					});				
			}
			
			// send load request to layer data loader, once all requirements are in memory, the
			// following callback function will execute.
			LayerDataLoader.get(requirements, function(layerDataMap) {
			
				// once everything is in memory, construct layer
				var spec =  {
						map: map.map,
						views: []
					}, 
					i;
			
				// add views to layer spec object
				for (i=0; i<layerJSON.views.length; i++) {
					spec.views.push({
						renderer: new layerDataMap[layerJSON.views[i].renderer](),
						dataTracker: layerDataMap[layerJSON.views[i].layer]
					});
				}
				
				// instantiate layer object
				return new layerDataMap[layerJSON.type](spec);
			});	

		}


    };	
	

});
