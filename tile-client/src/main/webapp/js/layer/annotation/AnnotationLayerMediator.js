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

define(function (require) {
    "use strict";



    var LayerMediator = require('../LayerMediator'),
        PubSub = require('../../util/PubSub'),
        AnnotationLayerMediator;



    AnnotationLayerMediator = LayerMediator.extend({
        ClassName: "AnnotationLayerMediator",

         init: function() {
             this._super();
         },


        registerLayers :  function( layers ) {

            var i;

            function register( layer ) {

                var channel = layer.getChannel(),
                    layerSpec = layer.getLayerSpec();

                layerSpec.enabled = ( layerSpec.enabled !== undefined ) ? layerSpec.enabled : true;
                layerSpec.opacity = ( layerSpec.opacity !== undefined ) ? layerSpec.opacity : 1.0;

                PubSub.subscribe( channel, function( message, path ) {

                    var field = message.field,
                        value = message.value;

                    switch ( field ) {

                        case "opacity":

                            layer.setOpacity( value );
                            break;

                        case "enabled":

                            layer.setVisibility( value );
                            break;

                        case "create":

                            layer.createAnnotation( value );
                            break;

                        case "modify":

                            layer.modifyAnnotation( value );
                            break;

                        case "zIndex":

                            layer.setZIndex( value );
                            break;

                        case "remove":

                            layer.removeAnnotation( value );
                            break;

                        case "click":

                            if ( value !== null ) {
                                layer.createDetails( value );
                            } else {
                                layer.destroyDetails();
                            }
                            break;
                    }

                });

                // set client-side layer state properties after binding callbacks
                PubSub.publish( channel, { field: 'zIndex', value: i+500 } );
                PubSub.publish( channel, { field: 'enabled', value: layerSpec.enabled } );
                PubSub.publish( channel, { field: 'opacity', value: layerSpec.opacity } );

                // clear click state if map is clicked
                layer.map.on( 'click', function() {
                    if ( layer.isClicked() ) {
                        // un-select
                        PubSub.publish( channel, { field: 'click', value: null } );
                    } else {
                        // add new annotation
                        PubSub.publish( channel, { field: 'create', value: event.xy } );
                    }
                });
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
