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
	    loadRendererModule,
	    loadAllRendererModules,
	    assembleViews,
	    ClientLayerFactory;



    loadRendererModule = function( module, spec, map ) {

        var requireDeferred = $.Deferred();
        require( [module], function( RendererModule ) {
            requireDeferred.resolve( new RendererModule( map, spec ) );
        });
        return requireDeferred;
    };


    loadAllRendererModules = function( layerJSON, map ) {

        var renderer,
            renderers,
            rendererPath,
            deferreds = [],
            i, j;

        for (i=0; i<layerJSON.length; i++) {

            renderers = layerJSON[i].renderers;
            for (j=0; j<renderers.length; j++) {

                renderer = renderers[j];
                rendererPath = "./renderers/" + renderer.type;
                deferreds.push( loadRendererModule( rendererPath, renderer.spec, map ) );
            }
        }
        return deferreds;
    };


    assembleViews = function( layerJSON, rendererObjs ) {

        var layerId,
            renderers,
            views = [],
            i, j, k = 0;

        for (i=0; i<layerJSON.length; i++) {

            layerId = layerJSON[i].layer;
            renderers = layerJSON[i].renderers;

            for (j=0; j<renderers.length; j++) {

                views.push({
                    id: layerId,
                    renderer: rendererObjs[k++]
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

            var rendererDeferreds,
                factoryDeferred = $.Deferred();

            if ( layerJSON.length === 0 ) {
                factoryDeferred.resolve([]);
                return factoryDeferred;
            }

            // load all renderer modules and grab deferreds
            rendererDeferreds = loadAllRendererModules( layerJSON, map );

            // once all modules are loaded
            $.when.apply( $, rendererDeferreds ).done( function() {

                var rendererObjs = Array.prototype.slice.call( arguments, 0 ),
                    clientLayer,
                    views = [];

                // assemble views from renderer objects
                views = assembleViews( layerJSON, rendererObjs );
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
