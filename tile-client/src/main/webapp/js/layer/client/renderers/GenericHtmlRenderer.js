/*
 * Copyright (c) 2013 Oculus Info Inc.
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



    var HtmlRenderer = require('./HtmlRenderer'),
        Util = require('../../../util/Util'),
        GenericHtmlRenderer;



    GenericHtmlRenderer = HtmlRenderer.extend({
        ClassName: "GenericHtmlRenderer",

        init: function( map, spec ) {

            this._super( map, spec );
            //this.details = details;
        },


        registerLayer: function( layerState ) {

            var that = this;

            this._super( layerState );

            this.layerState.addListener( function( fieldName ) {

                switch (fieldName) {

                    case "click":

                        if ( layerState.has('click') ) {
                            // add click state classes
                            that.addClickStateClassesGlobal();
                        } else {
                            // remove click state classes
                            that.removeClickStateClassesGlobal();
                            //that.details.destroy();
                        }
                        break;
                }

            });

        },


        addClickStateClasses: function( $elem, value, valueKey ) {

            // if user has clicked a tag entry, ensure newly created nodes are styled accordingly
            var selectedValue = this.layerState.has('click') ? this.layerState.get('click')[valueKey] : null;
            if ( selectedValue ) {
                if ( selectedValue !== value ) {
                    $elem.addClass('greyed');
                } else {
                    $elem.addClass('clicked');
                }
            }
        },


        addClickStateClassesGlobal: function() {
            // sub-class this
            return true;
        },


        removeClickStateClassesGlobal: function() {
            // sub-class this
            return true;
        },


        clickOn: function( data, value ) {

            this.layerState.set('click', {
                data: data,
                value : value
            });
        },


        clickOff: function() {
            this.layerState.set('click', null);
        },


        setMouseEventCallbacks: function( $element, data, value, countKey ) {

            var that = this;

            if ( countKey ) {
                // set summaries text
                $element.mouseover( function( event ) {
                    $element.closest('.aperture-tile').find(".count-summary").text( value[countKey] );
                });

                // clear summaries text
                $element.mouseout( function( event ) {
                    $element.closest('.aperture-tile').find(".count-summary").text( "" );
                });
            }


            Util.dragSensitiveClick( $element, function( event ) {
                // process click
                that.clickOn( data, value );
                // create details here so that only 1 is created
                //that.createDetailsOnDemand();
                // prevent event from going further
                event.stopPropagation();
            });

        },


        centreForDetails: function( data ) {
            var map = this.map,
                viewportPixel = map.getViewportPixelFromCoord( data.longitude, data.latitude ),
                panCoord = map.getCoordFromViewportPixel( viewportPixel.x + map.getTileSize(),
                                                          viewportPixel.y + map.getTileSize() );
            map.panToCoord( panCoord.x, panCoord.y );
        },


        createDetailsOnDemand: function() {

            var clickState = this.layerState.get('click'),
                map = this.map,
                data = clickState.data,
                value = clickState.value,
                tilePos = map.getMapPixelFromCoord( data.longitude, data.latitude ),
                detailsPos = {
                    x: tilePos.x + 256,
                    y: map.getMapHeight() - tilePos.y
                },
                $details;

            $details = this.details.create( detailsPos, value, $.proxy( this.clickOff, this ) );

            Util.enableEventPropagation( $details, ['onmousemove', 'onmouseup'] );
            map.getRootElement().append( $details );

            this.centreForDetails( data );
        }

    });

    return GenericHtmlRenderer;

});