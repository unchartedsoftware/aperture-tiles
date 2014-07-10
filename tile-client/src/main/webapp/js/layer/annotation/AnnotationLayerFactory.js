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



    var AnnotationLayer = require('./AnnotationLayer'),
        LayerFactory = require('../LayerFactory'),
        AnnotationLayerFactory;



	AnnotationLayerFactory = LayerFactory.extend({
        ClassName: "ServerLayerFactory",

		createLayer: function( layerJSON, map ) {

			var annotationLayer,
			    rendererDeferred,
			    detailsDeferred,
			    annotationLayerDeferred = $.Deferred();

            function loadModule( arg ) {
                var requireDeferred = $.Deferred();
                require( [arg], function( RendererModule ) {
                    requireDeferred.resolve( new RendererModule( map ) );
                });
                return requireDeferred;
            }

            layerJSON.layer = layerJSON.id;

            rendererDeferred = loadModule( "./renderers/" +  layerJSON.renderers.layer );
            detailsDeferred = loadModule( "./details/" +  layerJSON.renderers.details );

            $.when( rendererDeferred, detailsDeferred ).done( function( renderer, details ) {

                annotationLayer = new AnnotationLayer( layerJSON, renderer, details, map );
                /*
                annotationLayer.configure( function( layerInfo ) {
                      // update layer and resolve deferred
                      annotationLayer.update( layerInfo );
                      annotationLayer.resolve( annotationLayer );
                });
                */
                annotationLayerDeferred.resolve( annotationLayer );
            });

            return annotationLayerDeferred;

		}
    });

    return AnnotationLayerFactory;
});
