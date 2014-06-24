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
        TwitterUtil = require('./TwitterUtil'),
        DetailsOnDemand = require('./DetailsOnDemandHtml'),
        activityLogger = require('../../../logging/DraperActivityLogger'),
        TwitterHtmlRenderer;


    TwitterHtmlRenderer = HtmlRenderer.extend({
        ClassName: "TwitterHtmlRenderer",

        init: function( map) {

            this._super( map );
        },


        registerLayer: function( layerState ) {

            var that = this;

            this._super( layerState );

            this.layerState.addListener( function( fieldName ) {

                var tilekey, previousTilekey;

                switch (fieldName) {

                    case "clickState":

                        if ( layerState.hasClickState() ) {
                            // add click state classes
                            that.addClickStateClassesGlobal();
                        } else {
                            // remove click state classes
                            that.removeClickStateClassesGlobal();
                            DetailsOnDemand.destroy();
                        }
                        break;

                    case "translate":

                        // redraw node based on translation
                        tilekey = that.layerState.getTileFocus();
                        $('#'+that.getTranslateLabelId( tilekey ) ).css('visibility', 'visible');
                        that.nodeLayer.where( tilekey ).redraw();
                        break;

                    case "tileFocus":

                        tilekey = that.layerState.getTileFocus();
                        previousTilekey = that.layerState.getPreviousTileFocus();
                        // set this translate label visible, if layer is enabled
                        if ( that.layerState.isEnabled() ) {
                            $('#'+that.getTranslateLabelId( tilekey ) ).css('visibility', 'visible');
                        }
                        // set previous tile translate label invisible
                        if ( previousTilekey ) {
                            $('#'+that.getTranslateLabelId( previousTilekey ) ).css('visibility', 'hidden');
                        }
                        break;
                }

            });

        },


        addClickStateClasses: function( $elem, tag ) {

            // if user has clicked a tag entry, ensure newly created nodes are styled accordingly
            var selectedTag = this.layerState.getClickState().tag;
            if ( selectedTag ) {
                if ( selectedTag !== tag ) {
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


        getTopic: function( value, tilekey ) {
            var translation = this.layerState.getCustomObject('translate', tilekey);
            return translation ? value.topicEnglish : value.topic;
        },


        getTranslateLabelId : function( tilekey ) {
            var parsedValues = tilekey.split(','),
                level = parseInt(parsedValues[0], 10),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10);

            return 'translate-label-'+level+'-'+xIndex+'-'+yIndex;
        },


        createTranslateLabel: function( tilekey ) {
            var that = this,
                translation = that.layerState.getCustomObject( 'translate', tilekey ),
                $translate = $('<div id="'+this.getTranslateLabelId(tilekey)+'" class="translate-label">translate</div>');

            if ( !translation ) {
                $translate.addClass('translate-label-greyed');
            }

            if ( tilekey === this.layerState.getTileFocus() ) {
                $translate.css('visibility', 'visible');
            }

            $translate.click( function( event) {

                var translation = that.layerState.getCustomObject( 'translate', tilekey );
                if (translation) {
                    that.layerState.removeCustomObject( 'translate', tilekey );
                } else {
                    that.layerState.setCustomObject( 'translate', tilekey, true );
                }
                event.stopPropagation();
            });

            return $translate;
        },


        clickOn: function( data, value ) {

            this.layerState.setClickState({
                tag: value.topic,
                translatedTag: value.topicEnglish,
                data: data,
                value : value
            });
        },


        clickOff: function() {

            this.layerState.setClickState({});
        },


        hoverOn: function( data, value ) {
            this.layerState.setHoverState({
                tag: value.topic,
                translatedTag: value.topicEnglish,
                data: data,
                value : value
            });
        },


        hoverOff: function() {
            this.layerState.setHoverState({});
        },


        setMouseEventCallbacks: function( $element, data, value ) {

            var that = this;

            // moving mouse disables click event
            $element.mousemove( function( event ) {
                 $element.off('click');
            });

            // mouse down enables click event
            $element.mousedown( function( event ) {

                // set click handler
                $element.click( function( event ) {
                    // process click
                    that.clickOn( data, value );
                    // create details here so that only 1 is created
                    that.createDetailsOnDemand();
                    // prevent event from going further
                    event.stopPropagation();
                 });
            });

            $element.mouseover( function( event ) {
                that.hoverOn( data, value );
            });

            $element.mouseout( function( event ) {
                that.hoverOff();
            });

        },


        createDetailsOnDemand: function() {

            var clickState = this.layerState.getClickState(),
                map = this.map,
                data = clickState.data,
                value = clickState.value,
                tilePos = map.getMapPixelFromCoord( data.longitude, data.latitude ),
                detailsPos = {
                    x: tilePos.x + 256,
                    y: map.getMapHeight() - tilePos.y
                },
                $details;

            $details = DetailsOnDemand.create( detailsPos, value, this.getTopic( value, data.tilekey ), $.proxy( this.clickOff, this ) );

            map.enableEventToMapPropagation( $details, ['onmousemove', 'onmouseup'] );
            map.getRootElement().append( $details );

            TwitterUtil.centreForDetails( map, data );
        }

    });

    return TwitterHtmlRenderer;

});