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


define(function (require) {
    "use strict";



    var LayerMediator = require('../LayerMediator'),
        SharedObject = require('../../util/SharedObject'),
        Util = require('../../util/Util'),
        BaseLayerMediator;



    BaseLayerMediator = LayerMediator.extend({
        ClassName: "BaseLayerMediator",

         init: function() {
             this._super();
         },


        registerLayers: function( map ) {

            var layerState;

            // create a layer state object. Values are initialized to those provided
            // by the layer specs, which are defined in the layers.json file, or are
            // defaulted to appropriate starting values
            layerState = new SharedObject();

            // set immutable layer state properties
            layerState.set( 'id', map.id );
            layerState.set( 'uuid', Util.generateUuid() );
            layerState.set( 'name', "Base Layer" );
            layerState.set( 'domain', 'base' );

            layerState.BASE_LAYERS = map.baseLayers;

            // Register a callback to handle layer state change events.
            layerState.addListener( function( fieldName ) {

                switch (fieldName) {

                    case "opacity":

                        map.setOpacity( layerState.get('opacity') );
                        break;

                    case "enabled":

                        map.setVisibility( layerState.get('enabled') );
                        break;

                    case "baseLayerIndex":

                        map.setBaseLayerIndex( layerState.get('baseLayerIndex') );
                        if ( layerState.BASE_LAYERS[ layerState.get('baseLayerIndex') ].type !== "BlankBase" ) {
                            // if switching to a non-blank baselayer, ensure opacity and visibility is restored
                            map.setOpacity( layerState.get('opacity') );
                            map.setVisibility( layerState.get('enabled') );
                        }
                        break;
                }

            });

            // set client-side layer state properties after binding callbacks
            layerState.set( 'enabled', true );
            layerState.set( 'opacity', 1.0 );
            layerState.set( 'zIndex', -1 );
            layerState.set( 'baseLayerIndex', 0 );

            // Add the layer to the layer state array.
            this.layerStates.push( layerState );
        }

    });
    return BaseLayerMediator;
});
