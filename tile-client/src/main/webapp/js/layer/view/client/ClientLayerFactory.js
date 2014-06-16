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
	    ClientLayerFactory;

    ClientLayerFactory = LayerFactory.extend({
        ClassName: "ClientLayerFactory",



        createLayer: function(layerJSON, map) {

            var renderers = layerJSON.renderers,
                rendererDeferreds = [],
                clientLayer,
                clientLayerDeferred = $.Deferred(),
                i;

            function loadModule( arg ) {
                var requireDeferred = $.Deferred();
                require( [arg], function( RendererModule ) {
                    requireDeferred.resolve( new RendererModule( map ) );
                });
                return requireDeferred;
            }

            for (i=0; i<renderers.length; i++) {
                rendererDeferreds.push( loadModule( "./impl/" + renderers[i] ) );
            }

            $.when.apply( $, rendererDeferreds ).done( function() {

                // once everything has loaded
                var renderers = Array.prototype.slice.call( arguments, 0 );
                // create the layer
                clientLayer = new ClientLayer( layerJSON, renderers, map );
                // send configuration request
                clientLayer.configure( function( layerInfo ) {
                    // resolve deferred
                    clientLayerDeferred.resolve( clientLayer );
                });
            });

            return clientLayerDeferred;
        }


        /*

        createLayer: function(layerJSON, map) {

            var clientLayer,
                clientLayerDeferred = $.Deferred();

            // load renderer module
            require( ["./impl/" + layerJSON.renderer], function( RendererModule ) {
                // create the layer
                clientLayer = new ClientLayer( layerJSON,  new RendererModule( map ), map );
                // send configuration request
                clientLayer.configure( function( layerInfo ) {
                    // update layer and resolve deferred
                    clientLayer.update( layerInfo );
                    clientLayerDeferred.resolve( clientLayer );
                });

            });

            return clientLayerDeferred;
        }
        */


    });

    return ClientLayerFactory;

});
