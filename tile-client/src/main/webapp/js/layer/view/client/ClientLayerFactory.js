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
		
	var TileService = require('./data/TileService'),
        ClientLayer = require('./CarouselLayer'),
		DataLayer = require('../../DataLayer');	
		
	return {
	
		/**
		 * Given a layer JSON specification object and a map, this function will pull data information from the server and load
		 * required layer and renderer class modules using require.js. Once everything is ready, constructs individual layers.
		 * @param layerJSON	 	layer specification JSON object loaded from layers.json
		 * @param map			map object from map.js
		 */
		createLayers: function(layerJSON, uiMediator, map) {
			var i;
			for (i=0; i<layerJSON.length; i++) {   
				this.createLayer(layerJSON[i], uiMediator, map);
			}		
		},
	

		createLayer: function(layerJSON, uiMediator, map) {

			var layer = {
                    views : [],
                    controller : {}
                },
                tasks = [],
                clientLayer,
                d,
                i;
	
			// load module func
			function loadRequireJsModule(arg, callback) {
				require([arg], callback);
			}
			
			// get layer info from server func	
			function getLayerInfoFromServer(arg, callback) {
				var layerInfoListener = new DataLayer([arg]);
				layerInfoListener.addRetrievedCallback(callback);
				layerInfoListener.retrieveLayerInfo();
			}

            function onRetrieveController( deferred ){
                return function( module ) {
                    layer.controller = module;
                    deferred.resolve();
                };
            }

            function onRetrieveRenderer( index, deferred ){
                return function( Module ) {
                    layer.views[index].renderer = new Module();
                    deferred.resolve();
                };
            }

            function onRetrieveLayerInfo( index, deferred ){
                return function( dataLayer, layerInfo ) {
                    layer.views[index].dataService = new TileService( layerInfo );
                    deferred.resolve();
                };
            }

            /*
            d = $.Deferred();
            tasks.push( d );

            loadRequireJsModule( "./" + layerJSON.type, onRetrieveController( d ) );
            */

            // add view dependencies to requirements
            for (i=0; i<layerJSON.views.length; i++) {

                layer.views[i] = {};

                d = $.Deferred();
                tasks.push( d );

                // get renderer class from require.js
                loadRequireJsModule( "./impl/" + layerJSON.views[i].renderer, onRetrieveRenderer( i ,d ) );

                d = $.Deferred();
                tasks.push( d );

                // POST request for layerInfo
                getLayerInfoFromServer( {
                    request: "configure",
                    layer:  layerJSON.layer,
                    configuration: layerJSON.views[i]
                }, onRetrieveLayerInfo(i, d ) );
            }

            clientLayer = new ClientLayer(map);

            // instantiate layer object
            uiMediator.addClientLayer( clientLayer, map );

            $.when.apply( $, tasks ).done( function() {

                // once everything has loaded
                var views = [],
                    i;

                // add views to layer spec object
                for (i=0; i<layerJSON.views.length; i++) {
                    views.push({
                        renderer: layer.views[i].renderer,
                        dataService: layer.views[i].dataService
                    });
                }

               clientLayer.setViews( views ); // = new ClientLayer(map);
            });

		}


    };	
	

});
