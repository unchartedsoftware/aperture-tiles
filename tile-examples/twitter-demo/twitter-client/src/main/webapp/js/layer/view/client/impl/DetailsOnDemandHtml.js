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
                count = tweetCount( tagData ),
                time, tweet,
                i;

            function getTime( timestamp ) {

                function getMonth( date ) {

                    var month = date.getMonth();
                    switch(month) {
                        case 0: return "Jan";
                        case 1: return "Feb";
                        case 2: return "Mar";
                        case 3: return "Apr";
                        case 4: return "May";
                        case 5: return "Jun";
                        case 6: return "Jul";
                        case 7: return "Aug";
                        case 8: return "Sep";
                        case 9: return "Oct";
                        case 10: return "Nov";
                        default: return "Dec";
                    }
                }

                function padZero( num ) {
                    return ("0" + num).slice(-2);
                }

                var date = new Date( timestamp*1000 ),
                    month = getMonth( date ),
                    day = date.getDay(),
                    hours = date.getHours(),
                    minutes = padZero( date.getMinutes() ),
                    seconds = padZero( date.getSeconds() ),
                    suffix = (hours >= 12) ? 'pm' : 'am';

                hours = ( hours === 0 || hours === 12 ) ? 12 : hours % 12;

                return month + " " + day + ", " + hours + ':' + minutes + ':' + seconds + " " + suffix;
            }

            function tweetCount( data ) {
                var MAX_TWEETS = 10;
                return Math.min( data.recent.length, MAX_TWEETS );
            }

            // top half
            html += '<div class="details-on-demand-tophalf">'

            html +=     '<div class="sentiment-summaries">';
            html +=         '<div class="positive-summaries"> +'+tagData.positive+'</div>';
            html +=         '<div class="neutral-summaries">'+tagData.neutral+'</div>';
            html +=         '<div class="negative-summaries"> -'+tagData.negative+'</div>';
            html +=     '</div>';

            html +=     '<div class="details-on-demand-title">'+TwitterUtil.trimLabelText(tagData.tag)+'</div>';
            html +=     '<div class="details-on-demand-subtitle">Last 24 Hours</div>';

            html +=     '<div class="details-on-demand-chart">';
            html +=         '<div class="details-on-demand-positive-label">Positive Tweets</div>';
            html +=         '<div class="details-on-demand-chart-vizlet"></div>';
            html +=         '<div class="details-on-demand-negative-label">Negative Tweets</div>';
            html +=     '</div>';

            html += '</div>';

            // bottom half
            html += '<div class="details-on-demand-bottomhalf">'

            html +=     '<div class="details-on-demand-subtitle">Most Recent</div>';
            html +=     '<div class="details-on-demand-recent-tweets">';
            for (i=0; i<count; i++) {
                time = getTime( tagData.recent[i].time );
                tweet = tagData.recent[i].tweet;
                html +=         '<div class="details-on-demand-tweet-time">'+time+'</div>';
                html +=         '<div class="details-on-demand-tweet-text">'+tweet+'</div>';
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