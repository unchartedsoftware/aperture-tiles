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



    var HtmlNodeLayer = require('../../HtmlNodeLayer'),
        HtmlLayer = require('../../HtmlLayer'),
        HtmlRenderer = require('./HtmlRenderer'),
        PubSub = require('../../../util/PubSub'),
        TopTagsHtml;



    TopTagsHtml = HtmlRenderer.extend({
        ClassName: "TopTagsHtml",

        init: function( map ) {

            this._super( map );
            this.createNodeLayer(); // instantiate the node layer data object
            this.createLayer();     // instantiate the html visualization layer
        },


        subscribeRenderer: function() {

            var that = this; // preserve 'this' context

            PubSub.subscribe( this.parent.getChannel(), function( message, path ) {

                var field = message.field,
                    value = message.value;

                if ( field === "click" ) {
                    // if a click occurs, lets remove styling from any previous label
                    $(".topic-label, .clicked").removeClass('clicked');
                    // in this demo we only want to style this layer if the click comes from this layer
                    if ( value && value.type === "html" ) {
                        // add class to the object to adjust the styling
                        value.$elem.addClass('clicked');
                    }
                }
            });

        },


        createNodeLayer: function() {

            /*
                 Instantiate the html node layer. This holds the tile data as it comes in from the tile service. Here
                 we set the x and y coordinate mappings that are used to position the individual nodes on the map. In this
                 example, the data is geospatial and is under the keys 'latitude' and 'longitude'. The idKey
                 attribute is used as a unique identification key for internal managing of the data. In this case, it is
                 the tilekey.
             */
            this.nodeLayer = new HtmlNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey'
            });
        },


        createLayer : function() {

            var that = this;

            /*
                Utility function for positioning the labels
            */
            function getYOffset( numTopics, index ) {
                var SPACING =  36;
                return 108 - ( (( numTopics - 1) / 2 ) - index ) * SPACING;
            }

            /*
                Here we create and attach an individual html layer to the html node layer. For every individual node
                of data in the node layer, the html function will be executed with the 'this' context that of the node.
             */
            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var MAX_TOPICS = 5,             // we only want to display a maximum of 5 topics
                        values = this.values,    // the values associated with the bin (in this example there is only
                                                    // one bin per tile)
                        numTopics = Math.min( values.length, MAX_TOPICS ),
                        $html = $('<div class="tile"></div>'), // this isn't necessary, but wrapping the tile html in a
                                                               // 256*256 div lets us use 'bottom' and 'right' positioning
                                                               // if we so choose
                        topic,
                        $topic,
                        i;

                    /*
                        Iterate over the top 5 available topics and create the html elements.
                    */
                    for (i=0; i<numTopics; i++) {

                        topic = values[i].topic;
                        $topic = $('<div class="topics-label" style=" top:' +  getYOffset( numTopics, i ) + 'px;">' + topic + '</div>');

                        /*
                            Attach a mouse click event listener
                        */
                        $topic.click( function() {
                            var click = {
                                $elem: $(this),
                                type: "html"
                            };
                            that.parent.setClick( click );
                            event.stopPropagation(); // stop the click from propagating deeper
                        });

                        $html.append( $topic );
                    }

                    // return the jQuery object. You can also return raw html as a string.
                    return $html;
                }
            }));

        }


    });

    return TopTagsHtml;
});