/**
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


/*
    A hierarchical publish and subscribe object. Channels consist of strings, sub-channels
    are separated with a period ('.'). Publishing to a parent channel will propagate the message
    to all existing sub-channels.
*/
define(function (require) {

    "use strict";

    return {

        subscribe: function( channelPath, subscriber ) {

            var paths = channelPath.split('.'),
                path,
                currentPath = '',
                channel;

            this.channels = this.channels || {
                subscribers : [],
                children : {}
            };

            channel = this.channels;

            while ( paths.length > 0 ) {
                path = paths.shift();
                currentPath += ( currentPath.length > 0 ) ? '.' + path : path;
                channel.children[ path ] = channel.children[ path ] || {
                    path : currentPath,
                    subscribers : [],
                    children : {}
                };
                channel = channel.children[ path ];
            }
            channel.subscribers.push( subscriber );
        },


        publish: function( channelPath, message ) {

            var paths = channelPath.split('.'),
                children,
                subscribers,
                queue,
                path, i, sub,
                channel = this.channels || {
                    subscribers : [],
                    children : {}
                };

            // find channel
            while ( paths.length > 0 ) {
                path = paths.shift();
                if ( channel.children[ path ] === undefined ) {
                    return;
                }
                channel = channel.children[ path ];
            }
            
            // initialize publishing queue
            queue = [ channel ];

            // breadth first publishing
            while ( queue.length > 0 ) {
                channel = queue.shift();
                subscribers = channel.subscribers;
                children = channel.children;
                // publish to current channel
                for ( i=0; i<subscribers.length; i++ ) {
                    subscribers[i]( message, channelPath );
                }
                // queue up children
                for ( sub in children ) {
                    if ( children.hasOwnProperty( sub ) ) {
                        queue.push( children[ sub ] );
                    }
                }
            }
        }
    };
});
