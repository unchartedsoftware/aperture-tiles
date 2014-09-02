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


define( function (require) {
    "use strict";



	var LayerFactory = require('../LayerFactory'),
	    ClientLayer = require('./ClientLayer'),
	    loadModule,
	    loadAllModules,
	    assembleViews,
	    loadedModules = {},
	    ClientLayerFactory;


    loadModule = function( path, module, spec ) {

        var deferred = $.Deferred();
        require( [path+module], function( Module ) {
            loadedModules[ module ] = Module;
            deferred.resolve();
        });
        return deferred;
    };


    loadAllModules = function( layerJSON ) {

        var renderer,
            renderers,
            //det,
            //details,
            deferreds = [],
            i, j;

        for (i=0; i<layerJSON.length; i++) {
            renderers = layerJSON[i].renderers;
            for (j=0; j<renderers.length; j++) {

                renderer = renderers[j];
                if ( !loadedModules[ renderer.type ] ) {
                    // only load each module once
                    loadedModules[ renderer.type ] = "pending";
                    deferreds.push( loadModule( "./renderers/", renderer.type, renderer.spec ) );
                }
            }

            /*
            details = layerJSON[i].details;
            if ( details ) {
                for (j=0; j<details.length; j++) {
                    det = details[j];
                    if ( !loadedModules[ det.type ] ) {
                        // only load each module once
                        loadedModules[ det.type ] = "pending";
                        deferreds.push( loadModule( "./details/", det.type, det.spec ) );
                    }
                }
            }
            */
        }
        return deferreds;
    };


    assembleViews = function( layerJSON, map ) {

        var layerId,
            renderers,
            renderer,
            views = [],
            i, j;

        for (i=0; i<layerJSON.length; i++) {

            layerId = layerJSON[i].layer;
            renderers = layerJSON[i].renderers;

            for (j=0; j<renderers.length; j++) {

                renderer = renderers[j];
                views.push({
                    id: layerId,
                    renderer: new loadedModules[ renderer.type ]( map, renderer.spec )
                });
            }
        }
        return views;
    };


    ClientLayerFactory = LayerFactory.extend({
        ClassName: "ClientLayerFactory",


        // override base function so that all client rendering
        // instances in config file are group into a single layer
        // TODO: have this factory group by layer name, so it will be possible to have multiple client layers
        createLayers: function( layerJSON, map, layerMediator ) {

            var moduleDeferreds,
                factoryDeferred = $.Deferred();

            if ( layerJSON.length === 0 ) {
                factoryDeferred.resolve([]);
                return factoryDeferred;
            }

            // load all renderer modules and grab deferreds
            moduleDeferreds = loadAllModules( layerJSON );

            // once all modules are loaded
            $.when.apply( $, moduleDeferreds ).done( function() {

                var clientLayer,
                    views = [];

                // assemble views from renderer objects
                views = assembleViews( layerJSON, map );
                // create the layer
                clientLayer = new ClientLayer( layerJSON, views, map );
                // send configuration request
                clientLayer.configure( function() {
                    // once layer is configured
                    layerMediator.registerLayers( [clientLayer] );
                    // resolve deferred
                    factoryDeferred.resolve( [clientLayer] );
                });

            });

            return factoryDeferred;
        }

    });


    return ClientLayerFactory;

});
