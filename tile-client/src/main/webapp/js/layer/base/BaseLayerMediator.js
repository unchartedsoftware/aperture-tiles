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
        PubSub = require('../../util/PubSub'),
        BaseLayerMediator;



    BaseLayerMediator = LayerMediator.extend({
        ClassName: "BaseLayerMediator",

         init: function() {
             this._super();
         },


        registerLayers: function( map ) {

            var channel = map.getChannel();

            // Register a callback to handle layer state change events.
            PubSub.subscribe( channel, function( message, path ) {

                var field = message.field,
                    value = message.value;

                switch ( field ) {

                    case "opacity":

                        map.setOpacity( value );
                        break;

                    case "enabled":

                        map.setVisibility( value );
                        break;

                    case "zIndex":

                        map.setZIndex( value );
                        break;

                    case "baseLayerIndex":

                        map.setBaseLayerIndex( value );
                        if ( map.BASE_LAYERS[ map.getBaseLayerIndex() ].type !== "BlankBase" ) {
                            // if switching to a non-blank baselayer, ensure opacity and visibility is restored
                            map.setOpacity( map.getOpacity() );
                            map.setVisibility( map.getVisibility() );
                        }
                        break;
                }

            });

            // set client-side layer state properties after binding callbacks
            PubSub.publish( channel, { field: 'zIndex', value: -1 });
            PubSub.publish( channel, { field: 'enabled', value: true });
            PubSub.publish( channel, { field: 'opacity', value: 1.0 });
            PubSub.publish( channel, { field: 'baseLayerIndex', value: 0 });
        }

    });
    return BaseLayerMediator;
});
