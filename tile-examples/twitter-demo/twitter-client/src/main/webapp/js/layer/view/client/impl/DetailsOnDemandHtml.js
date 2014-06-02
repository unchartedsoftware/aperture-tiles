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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines the base class for a client render layer. Must be
 * inherited from for any functionality.
 */
define(function (require) {
    "use strict";

     var TwitterUtil = require('./TwitterUtil');

    return {

        create: function( tagData ) {

            var html = '',
                time, day, tweetsByDay, key, lightOrDark,
                i;

            // top half
            html += '<div class="details-on-demand-tophalf">'

            html +=     '<div class="sentiment-summaries">';
            html +=         '<div class="positive-summaries"> +'+tagData.positive+'</div>';
            html +=         '<div class="neutral-summaries">'+tagData.neutral+'</div>';
            html +=         '<div class="negative-summaries"> -'+tagData.negative+'</div>';
            html +=     '</div>';

            html +=     '<div class="details-on-demand-title large-title">'+TwitterUtil.trimLabelText(tagData.tag)+'</div>';
            html +=     '<div class="details-on-demand-title small-title">Last 24 Hours</div>';

            html +=     '<div class="details-on-demand-chart">';
            html +=         '<div class="details-on-demand-positive-label">Positive Tweets</div>';
            html +=         '<div class="details-on-demand-chart-vizlet"></div>';
            html +=         '<div class="details-on-demand-negative-label">Negative Tweets</div>';
            html +=     '</div>';

            html += '</div>';

            // bottom half
            html += '<div class="details-on-demand-bottomhalf">'

            html +=     '<div class="details-on-demand-title small-title">Most Recent</div>';
            html +=     '<div class="details-on-demand-recent-tweets">';

            // bucket tweets by day
            tweetsByDay = TwitterUtil.getRecentTweetsByDay( tagData );

            for ( key in tweetsByDay ) {
                if( tweetsByDay.hasOwnProperty( key ) ) {
                    day = tweetsByDay[key];
                    lightOrDark = 'light';
                    html += '<div class="details-on-demand-tweet-day">'+key+'</div>';
                    for (i=0; i<day.length; i++) {
                        html += '<div class="details-on-demand-tweet tweet-'+lightOrDark+'">';
                        html +=     '<div class="details-on-demand-tweet-text">'+day[i].tweet+'</div>';
                        html +=     '<div class="details-on-demand-tweet-time">'+day[i].time+'</div>';
                        html += '</div>';
                        lightOrDark = (lightOrDark === 'light') ? 'dark' : 'light';
                    }
                }
            }

            html +=     '</div>';

            html += '</div>';

            this.destroy(); // destroy any previous DoD
            return html;
        },


        destroy : function() {

            $('.details-on-demand').remove();

        }

    };

});