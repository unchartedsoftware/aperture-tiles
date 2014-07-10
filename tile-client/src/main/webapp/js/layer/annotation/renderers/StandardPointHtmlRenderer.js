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



    var HtmlAnnotationRenderer = require('./HtmlAnnotationRenderer'),
        HtmlNodeLayer = require('../../HtmlNodeLayer'),
        HtmlLayer = require('../../HtmlLayer'),
        ANNOTATION_BIN_CLASS = "annotation-bin",
        ANNOTATION_POINT_CLASS = "point-annotation",
        ANNOTATION_AGGREGATE_POINT_CLASS = "point-annotation-aggregate",
        ANNOTATION_POINT_FILL_CLASS = "point-annotation-front",
        ANNOTATION_POINT_BORDER_CLASS = "point-annotation-back",
        CLICKED_ANNOTATION_CLASS = "clicked-annotation",
        CLICKED_AGGREGATE_CLASS = "clicked-aggregate",
        StandardPointHtmlRenderer;



    StandardPointHtmlRenderer = HtmlAnnotationRenderer.extend({

        init: function( spec, map ) {

            this._super( spec, map );
            this.createLayer();
        },


        registerLayer: function( layerState ) {

            var that = this;
            this._super( layerState );
            this.layerState.addListener( function(fieldName) {

                var value = that.layerState.get( fieldName );

                if ( fieldName === "click" ) {

                    // remove any previous click classes
                    $( '.'+ANNOTATION_AGGREGATE_POINT_CLASS ).removeClass( CLICKED_AGGREGATE_CLASS );
                    $( '.'+ANNOTATION_POINT_FILL_CLASS ).removeClass( CLICKED_ANNOTATION_CLASS );

                    if (value) {
                        // add click classes
                        value.$annotation.addClass( CLICKED_AGGREGATE_CLASS );
                        value.$annotation.find( '.'+ANNOTATION_POINT_FILL_CLASS ).addClass( CLICKED_ANNOTATION_CLASS );
                    }
                }
            });
        },


        getAnnotationHtml : function ( bin, tilePosition ) {

            var html = '',
                position,
                offset,
                i;

            // aggregation div
            html += '<div class="'+ANNOTATION_AGGREGATE_POINT_CLASS+'">';

            // for each annotation in the bin
            for (i=0; i<bin.length; i++) {

                // get annotations position in viewport space
                position = this.map.getViewportPixelFromCoord( bin[i].x, bin[i].y );
                // get relative position from tile top left
                offset = {
                    x : position.x - tilePosition.x,
                    y : position.y - tilePosition.y
                };

                html += '<div class="'+ANNOTATION_POINT_CLASS+' '+ANNOTATION_POINT_FILL_CLASS+'" style="left:'+offset.x+'px; top:'+offset.y+'px;"></div>' +
                        '<div class="'+ANNOTATION_POINT_CLASS+' '+ANNOTATION_POINT_BORDER_CLASS+'" style="left:'+offset.x+'px; top:'+offset.y+'px;"></div>';
            }

            html += '</div>';

            return $( html );
        },


        createLayer : function() {

            var that = this;

            this.nodeLayer = new HtmlNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'uuid',
                propagate: false
            });

            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var data = this,
                        $html = $(''),
                        key,
                        tilePos;

                    // get tile position
                    tilePos = that.map.getViewportPixelFromCoord( data.longitude, data.latitude );

                    function createAnnotationBin( bin ) {

                        var $bin = $('<div class="'+ANNOTATION_BIN_CLASS+'"></div>'),
                            $annotation = that.getAnnotationHtml( bin, tilePos );

                        // add details click event
                        $annotation.click( function( event ) {

                            var offset = $bin.offset(),
                                position = {
                                    x: event.pageX - offset.left,
                                    y: event.pageY - offset.top
                                };

                            that.layerState.set('click', {
                                bin : bin,
                                $bin : $bin,
                                $annotation : $annotation,
                                position : position
                            });
                            event.stopPropagation();
                        });

                        // add drag modify event
                        /*
                        if ( bin.length === 1 ) {

                            $bin.draggable({

                                stop: function( event ) {

                                    var annotation = bin[0],
                                        offset = that.map.getElement().offset(),
                                        pos = that.map.getCoordFromViewportPixel( event.clientX - offset.left,
                                                                                  event.clientY - offset.top );

                                    annotation.x = pos.x;
                                    annotation.y = pos.y;

                                    that.layerState.set('modify', annotation );

                                    // prevent click from firing
                                    $( event.toElement ).one('click', function(e) {
                                        e.stopImmediatePropagation();
                                    });
                                }
                            });
                        }
                        */

                        $bin.append( $annotation );
                        return $bin;
                    }

                    // for each bin
                    for (key in data.bins) {
                        if (data.bins.hasOwnProperty(key)) {
                            $html = $html.add( createAnnotationBin( data.bins[key] ) );
                        }
                    }

                    return $html;
                }

            }));
        }


     });

    return StandardPointHtmlRenderer;
});
