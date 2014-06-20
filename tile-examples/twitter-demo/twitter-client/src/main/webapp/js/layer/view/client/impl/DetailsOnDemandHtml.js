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



    var TwitterUtil = require('./TwitterSentimentUtil');



    return {

        create: function( position, value, closeCallback ) {

            var html = '',
                day, tweetsByDay, key, lightOrDark, visibility,
                maxPercentage = TwitterUtil.getMaxCountByTimePercentage( value ),
                sentimentPercentages = TwitterUtil.getSentimentPercentagesByTime( value ),
                relativePercent, cumulativePercentages = [], $details,
                i;

            html += '<div class="details-on-demand" style="left:'+position.x+'px; top:'+position.y+'px;">';

            // top half
            html += '<div class="details-on-demand-half">';

            // summaries
            html +=     '<div class="sentiment-summaries">';
            html +=         '<div class="positive-summaries"> +'+value.positive+'</div>';
            html +=         '<div class="neutral-summaries">'+value.neutral+'</div>';
            html +=         '<div class="negative-summaries"> -'+value.negative+'</div>';
            html +=     '</div>';


            // title
            html +=     '<div class="details-on-demand-title large-title">'+TwitterUtil.trimLabelText(value.tag)+'</div>';

            // last 24 hours
            html +=     '<div class="details-on-demand-title small-title">Last 24 Hours</div>';

            html +=     '<div class="details-on-demand-chart">';
            html +=         '<div class="details-positive-label">Positive Tweets</div>';
            html +=         '<div class="details-chart-content">';

            html +=             '<div class="details-chart-bars">';

            for (i=0; i<value.countByTime.length; i++) {
                relativePercent = (TwitterUtil.getCountByTimePercentage( value, i ) / maxPercentage) * 100;
                cumulativePercentages[0] = sentimentPercentages.negative[i]*relativePercent;
                cumulativePercentages[1] = cumulativePercentages[0] + sentimentPercentages.neutral[i]*relativePercent;
                cumulativePercentages[2] = cumulativePercentages[1] + sentimentPercentages.positive[i]*relativePercent;

                visibility = (relativePercent > 0) ? 'visible' : 'hidden';
                html +=            '<div class="details-chart-bar" style="visibility:'+visibility+';">';
                html +=            '<div class="details-chart-positive-bar" style="height:'+cumulativePercentages[2]+'%;"></div>';
                html +=            '<div class="details-chart-neutral-bar" style="height:'+cumulativePercentages[1]+'%;"></div>';
                html +=            '<div class="details-chart-negative-bar" style="height:'+cumulativePercentages[0]+'%;"></div>';
                html +=            '</div>';
            }
            html +=            '</div>';

            html +=             '<div class="details-chart-axis">';
            html +=                 '<div class="details-axis-markers">';
            html +=                     '<div class="details-axis-marker details-major-marker"></div>';
            html +=                     '<div class="details-axis-marker details-minor-marker"></div>';

            html +=                     '<div class="details-axis-marker details-major-marker"></div>';
            html +=                     '<div class="details-axis-marker details-minor-marker"></div>';

            html +=                     '<div class="details-axis-marker details-major-marker"></div>';
            html +=                     '<div class="details-axis-marker details-minor-marker"></div>';

            html +=                     '<div class="details-axis-marker details-major-marker"></div>';
            html +=                     '<div class="details-axis-marker details-minor-marker"></div>';
            html +=                     '<div class="details-axis-marker details-last-major-marker"></div>';
            html +=                 '</div>';

            html +=                 '<div class="details-axis-labels">';
            html +=                     '<div class="details-axis-label">12am</div>';
            html +=                     '<div class="details-axis-label">6am</div>';
            html +=                     '<div class="details-axis-label">12pm</div>';
            html +=                     '<div class="details-axis-label">6am</div>';
            html +=                     '<div class="details-axis-label">12am</div>';
            html +=                 '</div>';

            html +=             '</div>';
            html +=         '</div>';
            html +=         '<div class="details-negative-label">Negative Tweets</div>';
            html +=     '</div>';

            html += '</div>';

            // bottom half
            html += '<div class="details-on-demand-half">';

            // most recent tweets
            html +=     '<div class="details-on-demand-title small-title">Most Recent</div>';
            html +=     '<div class="details-on-demand-recent-tweets">';

            // bucket tweets by day
            tweetsByDay = TwitterUtil.getRecentTweetsByDay( value );

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

            html += '<div class="details-on-demand-close-button"></div>';
            html += '</div>';

            this.destroy(); // destroy any previous DoD

            // create element
            $details = $(html).draggable().resizable({
                minHeight: 257,
                minWidth: 257,
                handles: 'se'
            });
            $details.find('.details-on-demand-close-button').click( closeCallback );
            return $details;

        },


        destroy : function() {

            $('.details-on-demand').remove();
        }

    };

});