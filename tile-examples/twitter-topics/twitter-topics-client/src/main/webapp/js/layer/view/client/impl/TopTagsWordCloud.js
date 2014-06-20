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



    var TwitterApertureRenderer = require('./TwitterApertureRenderer'),
        //TwitterUtil = require('./TwitterUtil'),
        WordCloudLayer = require('./WordCloudLayer'),
        TopTagsWordCloud;



    TopTagsWordCloud = TwitterApertureRenderer.extend({
        ClassName: "TopTagsWordCloud",

        init: function( map ) {

            this._super( map );
            this.createLayer();
        },


        /**
         * Create our layer visuals, and attach them to our node layer.
         */
        createLayer: function() {

            this.nodeLayer = this.map.addApertureLayer( aperture.geo.MapNodeLayer );
            this.nodeLayer.map('latitude').from('latitude');
            this.nodeLayer.map('longitude').from('longitude');
            this.createLabels();
            this.createTranslateLabel();
        },


        createLabels: function () {

            var that = this,
                MAX_LABEL_CHAR_COUNT = 12;

            this.wordCloudLabel = this.nodeLayer.addLayer(WordCloudLayer);
            this.wordCloudLabel.map('offset-x').asValue(this.X_CENTRE_OFFSET);
            this.wordCloudLabel.map('offset-y').asValue(this.Y_CENTRE_OFFSET);
            this.wordCloudLabel.map('cursor').asValue('pointer');
            this.wordCloudLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.wordCloudLabel.map('font-outline-width').asValue(3);
            this.wordCloudLabel.map('width').asValue( 228 );
            this.wordCloudLabel.map('height').asValue( 208 );
            this.wordCloudLabel.map('min-font-size').asValue(9);
            this.wordCloudLabel.map('max-font-size').asValue(32);

            this.wordCloudLabel.map('visible').from(function() {
                return that.visibility;
            });

            this.wordCloudLabel.map('fill').from(function(index) {

                var layerState = that.layerState,
                    clickState = layerState.getClickState(),
                    hoverState = layerState.getHoverState(),
                    hasClickState = layerState.hasClickState(),
                    hasHoverState = layerState.hasHoverState(),
                    selectedTag = clickState.tag,
                    hoveredTag = hoverState.tag,
                    tag = this.bin.value[index].topic;

                if ( (hasClickState && selectedTag === tag) ||
                     (hasHoverState && hoveredTag === tag) ) {
                    return that.BLUE_COLOUR;
                }
                if ( hasClickState && selectedTag !== tag  ) {
                    return that.GREY_COLOUR;
                }
                return that.WHITE_COLOUR;
            });

            this.wordCloudLabel.map('words').from(function() {
                var numWords = this.bin.value.length,
                    wordList = [],
                    word,
                    i;
                for (i=0; i<numWords; i++) {
                    word = that.getTopic( this.bin.value[i], this.tilekey );
                    word = (word.length > MAX_LABEL_CHAR_COUNT) ? word.substr(0, MAX_LABEL_CHAR_COUNT) + "..." : word;
                    wordList.push( word );
                }
                return wordList;
            });

            this.wordCloudLabel.map('frequencies').from(function() {
                var numWords = this.bin.value.length,
                    frequencies = [],
                    i;
                for (i=0; i<numWords; i++) {      
                    frequencies.push( this.bin.value[i].countMonthly );
                }
                return frequencies;
            });

            this.wordCloudLabel.map('opacity').from( function() {
                return that.opacity;
            });

            this.wordCloudLabel.on('click', function(event) {
                var data = event.data,
                    value = data.bin.value[ event.index[0] ];
                that.clickOn( data, value );
                return true; // swallow event
            });
            this.wordCloudLabel.on('mouseover', function(event) {
                var data = event.data,
                    value = data.bin.value[ event.index[0] ];
                that.hoverOn( data, value );
                that.nodeLayer.all().where(data).redraw( new aperture.Transition( 100 ) );
            });
            this.wordCloudLabel.on('mouseout', function(event) {

                that.hoverOff();
                that.nodeLayer.all().where(event.data).redraw( new aperture.Transition( 100 ) );
            });

        }

    });

    return TopTagsWordCloud;
});
