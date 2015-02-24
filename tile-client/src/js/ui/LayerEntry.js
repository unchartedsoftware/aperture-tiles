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
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

( function() {

    "use strict";

    var LayerEntryHead = require('./LayerEntryHead'),
        LayerEntryBody = require('./LayerEntryBody'),
        PubSub = require('../util/PubSub');

    module.exports = {

        /**
         * Creates the UI component that holds the controls header and body for a layer.
         *
         * @param {Layer} layer - The layer object.
         *
         * @returns {JQuery} - The JQuery element.
         */
        create: function( layer ) {
            var $layerEntry = $( '<div class="layer-entry"></div>' ),
                $layerHeader = LayerEntryHead.create( layer ),
                $layerBody = LayerEntryBody.create( layer );
            PubSub.subscribe( layer.getChannel(), function( message ) {
                if ( message.field === "enabled" ) {
                    if ( message.value ) {
                        $layerBody.css( 'display', "" );
                    } else {
                        $layerBody.css( 'display', "none" );
                    }
                }
            });
            if ( layer.isEnabled() ) {
                $layerBody.css( 'display', "" );
            } else {
                $layerBody.css( 'display', "none" );
            }
            $layerEntry.append( $layerHeader );
            $layerEntry.append( $layerBody );
            return $layerEntry;
        }

    };

}());