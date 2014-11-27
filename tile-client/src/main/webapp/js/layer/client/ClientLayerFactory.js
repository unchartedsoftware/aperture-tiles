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
        TileService = require('./TileService'),
	    loadModule,
	    loadAllModules,
	    assembleViews,
	    loadedModules = {},
	    ClientLayerFactory;

    /**
     * Load a single module via RequireJS and return a jQuery deferred.
     */
    loadModule = function( path, module ) {
        var deferred = $.Deferred();
        require( [path+module], function( Module ) {
            loadedModules[ module ] = Module;
            deferred.resolve();
        });
        return deferred;
    };

    /**
     * Load all modules required for the client layer, storing the deferreds
     * in the loadedModules object.
     */
    loadAllModules = function( layerJSON ) {

        var renderer,
            details,
            deferreds = [],
            deferred,
            i;

        // recursively parse the details spec and load all content components
        function loadDetails( details ) {
            var k;
            if ( details && !$.isEmptyObject( details ) ) {
                if ( !loadedModules[ details.type ] ) {
                    // only load each module once
                    deferred = loadModule( "./details/", details.type );
                    // temporarily store deferred here, so later if duplicate is found, we can grab it
                    loadedModules[ details.type ] = deferred;
                    deferreds.push( deferred );
                } else {
                    // add deferred or module here, if it is a deferred we will wait on it
                    // if it isn't, $.when will ignore it
                    deferreds.push( loadedModules[ details.type ] );
                }
                // recursively load nested detail modules
                if ( details.content ) {
                    for (k=0; k<details.content.length; k++) {
                        loadDetails( details.content[k] );
                    }
                }
            }
        }

        function loadRenderer( renderer ) {
            if ( !loadedModules[ renderer.type ] ) {
                // only load each module once
                deferred = loadModule( "./renderers/", renderer.type );
                // temporarily store deferred here, so later if duplicate is found, we can grab it
                loadedModules[ renderer.type ] = deferred;
                deferreds.push( deferred );
            } else {
                // add deferred or module here, if it is a deferred we will wait on it
                // if it isn't, $.when will ignore it
                deferreds.push( loadedModules[ renderer.type ] );
            }
        }

        for (i=0; i<layerJSON.views.length; i++) {
            renderer = layerJSON.views[i].renderer;
            details = layerJSON.views[i].details;
            loadRenderer( renderer );
            loadDetails( details );
        }
        return deferreds;
    };

    /**
     * Instantiate and returnthe details and renderers for each view from
     * the layer specification object.
     */
    assembleViews = function( layerJSON, map ) {

        var rendererSpec,
            renderer,
            detailsSpec,
            details,
            service,
            view,
            views = [],
            i;

        function assembleDetails( details ) {
            var result = null, k;
            if ( details ) {
                result = new loadedModules[ details.type ]( details.spec );
                result.content = [];
                if ( details.content ) {
                    for (k=0; k<details.content.length; k++) {
                        result.content.push( assembleDetails( details.content[k] ) );
                    }
                }
            }
            return result;
        }

        for (i=0; i<layerJSON.views.length; i++) {
            view = layerJSON.views[i];

            // create details object
            detailsSpec = view.details;
            details = assembleDetails( detailsSpec );
            if ( details ) {
                // if details is available, add meta
                details.meta = view.source.meta.meta;
            }

            // create renderer object
            rendererSpec = view.renderer;
            rendererSpec.spec.details = details;
            renderer = new loadedModules[ rendererSpec.type ]( map, rendererSpec.spec );
            renderer.meta = view.source.meta.meta;

            // create tile service object
            service = new TileService( view.source, map.getPyramid() );

            views.push({
                service: service,
                source: view.source,
                details: details,
                renderer: renderer
            });
        }
        return views;
    };


    ClientLayerFactory = LayerFactory.extend({
        ClassName: "ClientLayerFactory",


        createLayer: function( layerJSON, map ) {

            var clientLayerDeferred = $.Deferred(),
                moduleDeferreds;

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
                clientLayerDeferred.resolve( clientLayer );
            });

            return clientLayerDeferred;
        }

    });


    return ClientLayerFactory;

});
