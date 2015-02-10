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

    var PubSub = require('../util/PubSub');

    module.exports = {

        /**
         * Creates a coarseness dropdown menu and binds it to the provided layer.
         *
         * @param {Layer} layer - The layer object.
         *
         * @returns {JQuery} - The JQuery element.
         */
        create: function( layer ) {
            var $coarsenessControls = $( '<div class="coarseness-controls"></div>' ),
                $coarsenessLabel = $( '<div class="controls-label">Coarseness</div>' ),
                $coarsenessMenu = $( '<div class="control-menu">' ),
                $menuItems = $('<ul class="dropdown-menu"></ul>'),
                setCoarseness = function() {
                    layer.setCoarseness( $( this ).data( 'coarseness' ) );
                    $menuItems.children().each( function() {
                        $( this ).removeClass( 'active' );
                    });
                    $( this ).addClass( 'active' );
                },
                updateMenu = function( coarseness ) {
                    $menuItems.children().each( function() {
                        $( this ).removeClass( 'active' );
                        if ( $( this ).data( 'coarseness' ) === coarseness ) {
                            $( this ).addClass( 'active' );
                        }
                    });
                },
                items = [
                    {
                        id: 1,
                        name: "1x1"
                    },
                    {
                        id: 2,
                        name: "2x2"
                    },
                    {
                        id: 3,
                        name: "4x4"
                    },
                    {
                        id: 4,
                        name: "8x8"
                    }],
                $item,
                i;
            $menuItems.click( function() {
                $menuItems.toggleClass("dropdown-menu-open");
            });
            for ( i=0; i<items.length; i++ ) {
                $item = $( 
                    '<li class="menu-item">'+
                        '<a href="#" class="menu-item-link">'+items[i].name+'</a>'+
                    '</li>' );
                $item.data( 'coarseness', items[i].id );
                $item.click( setCoarseness );
                $menuItems.append( $item );
            }
            updateMenu( layer.getCoarseness() );
            PubSub.subscribe( layer.getChannel(), function( message ) {
                if ( message.field === "coarseness" ) {
                    updateMenu( message.value );
                }
            });
            $coarsenessControls.append( $coarsenessLabel );
            $coarsenessControls.append( $coarsenessMenu.append( $menuItems ) );
            return $coarsenessControls;
        }

    };

}());