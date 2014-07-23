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

/* JSLint global declarations: these objects don't need to be declared. */
/*global define, console, $, aperture*/

/**
 * Populates the LayerState model based on the contents of a the layer, and makes the appropriate
 * modifications to it as the LayerState model changes.
 */
define(function (require) {
    "use strict";



    var LayerMediator = require('../LayerMediator'),
        SharedObject = require('../../util/SharedObject'),
        AnnotationLayerMediator;



    AnnotationLayerMediator = LayerMediator.extend({
        ClassName: "AnnotationLayerMediator",

         init: function() {
             this._super();
         },


        registerLayers :  function( layers ) {

            var that = this,

                i;

            function register( layer ) {

                var layerState;

                layerState = new SharedObject();
                layerState.set( 'id', layer.id );
                layerState.set( 'name', layer.name );
                layerState.set( 'domain', 'annotation' );
                layerState.set( 'enabled', true );
                layerState.set( 'opacity', 1.0 );
                layerState.set( 'zIndex', 500+i );

                // register layerstate with renderer and details implementations
                layer.renderer.registerLayer( layerState );
                layer.details.registerLayer( layerState );

                // Register a callback to handle layer state change events.
                layerState.addListener( function( fieldName ) {

                    switch (fieldName) {

                        case "opacity":

                            layer.setOpacity( layerState.get('opacity') );
                            break;

                        case "enabled":

                            layer.setVisibility( layerState.get('enabled') );
                            break;

                        case "create":

                            layer.createAnnotation( layerState.get('create') );
                            break;

                        case "modify":

                            layer.modifyAnnotation( layerState.get('modify') );
                            break;

                        case "remove":

                            layer.removeAnnotation( layerState.get('remove') );
                            break;

                        case "click":

                            if ( layerState.has('click') ) {
                                layer.createDetails( layerState.get('click') );
                            } else {
                                layer.destroyDetails();
                            }
                            break;
                    }

                });

                // clear click state if map is clicked
                layer.map.on( 'click', function() {
                    if ( layerState.has('click') ) {
                        layerState.set( 'click', null );
                    } else {
                        layerState.set( 'create', event.xy );
                    }
                });

                // Add the layer to the layer state array.
                that.layerStates.push( layerState );
            }

            // ensure it is an array
            layers = ( $.isArray(layers) ) ? layers : [layers];

            for (i=0; i<layers.length; i++) {
                register( layers[i] );
            }

        }

    });

    return AnnotationLayerMediator;
});
