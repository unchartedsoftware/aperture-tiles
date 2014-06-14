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



    var Class = require('../../class'),
        BaseLayerState = require('../model/BaseLayerState'),
        BaseLayerMediator;



    BaseLayerMediator = Class.extend({
        ClassName: "BaseLayerMediator",

         init: function() {
             this.layerStateMap = {};
         },


        registerLayers: function( map ) {

            var layerState;
            // Create a layer state object for the base map.
            layerState = new BaseLayerState("Base Layer");
            layerState.setName("Base Layer");
            layerState.setEnabled( map.isEnabled() );
            layerState.setOpacity( map.getOpacity() );
            layerState.setZIndex( -1 );

            // Register a callback to handle layer state change events.
            layerState.addListener( function( fieldName ) {
                if (fieldName === "opacity") {
                    map.setOpacity( layerState.getOpacity() );
                } else if (fieldName === "enabled") {
                    map.setEnabled( layerState.isEnabled( ));
                }
            });

            // Add the layer to the layer statemap.
            this.layerStateMap[ layerState.getId() ] = layerState;
        },


        getLayerStateMap: function() {
            return this.layerStateMap;
        }

    });
    return BaseLayerMediator;
});
